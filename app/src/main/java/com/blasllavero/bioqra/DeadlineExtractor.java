package com.blasllavero.bioqra;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts candidate dated milestones from official public challenge pages.
 * These are page claims with a source; never assert they prove a competition is open.
 */
public final class DeadlineExtractor {
    private DeadlineExtractor(){}
    public static final class DateFinding {
        public final String kind,date,quote,url;
        DateFinding(String kind,String date,String quote,String url){
            this.kind=kind;this.date=date;this.quote=quote;this.url=url;
        }
    }
    private static final String MONTHS=
        "January|February|March|April|May|June|July|August|September|October|November|December|"
        +"Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec|"
        +"ener|febrer|març|abril|maig|juny|juliol|agost|setembre|octubre|novembre|desembre|"
        +"enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre";
    private static final Pattern DATE=Pattern.compile(
            "\\b(?:20\\d{2}-\\d{1,2}-\\d{1,2}|\\d{1,2}[/.]\\d{1,2}[/.]20\\d{2}|"
            +"(?:\\d{1,2}\\s+(?:de\\s+)?(?:"+MONTHS+")\\s+(?:de\\s+)?20\\d{2})|"
            +"(?:(?:"+MONTHS+")\\s+\\d{1,2},?\\s+20\\d{2}))\\b",
            Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    private static final Map<String,Integer> MONTH_INDEX=new HashMap<>();
    static{
        String[][] names={
            {"january","jan","ener","enero"},{"february","feb","febrer","febrero"},
            {"march","mar","març","marzo"},{"april","apr","abril"},
            {"may","maig","mayo"},{"june","jun","juny","junio"},
            {"july","jul","juliol","julio"},{"august","aug","agost","agosto"},
            {"september","sep","sept","setembre","septiembre","setiembre"},
            {"october","oct","octubre"},{"november","nov","novembre","noviembre"},
            {"december","dec","desembre","diciembre"}};
        for(int i=0;i<names.length;i++)for(String n:names[i])MONTH_INDEX.put(n,i+1);
    }
    private static String iso(String text){
        try{
            String t=text.toLowerCase(Locale.ROOT).replace(","," ").trim()
                    .replaceAll("\\s+"," ").replaceAll("\\s+de\\s+"," ");
            if(t.matches("20\\d{2}-\\d{1,2}-\\d{1,2}")){
                String[] p=t.split("-");
                return LocalDate.of(Integer.parseInt(p[0]),Integer.parseInt(p[1]),
                        Integer.parseInt(p[2])).toString();
            }
            if(t.matches("\\d{1,2}[/.]\\d{1,2}[/.]20\\d{2}")){
                String[] p=t.split("[/.]");
                // A D/M/Y interpretation is deliberate for EU organizer text;
                // ambiguous dates must still be reviewed on the original page.
                return LocalDate.of(Integer.parseInt(p[2]),Integer.parseInt(p[1]),
                        Integer.parseInt(p[0])).toString();
            }
            String[] p=t.split(" ");
            if(p.length==3){
                Integer month=MONTH_INDEX.get(p[0]);
                if(month!=null)return LocalDate.of(Integer.parseInt(p[2]),month,
                        Integer.parseInt(p[1])).toString();
                month=MONTH_INDEX.get(p[1]);
                if(month!=null)return LocalDate.of(Integer.parseInt(p[2]),month,
                        Integer.parseInt(p[0])).toString();
            }
        }catch(Exception ignored){}
        return null;
    }
    private static String classify(String context){
        String s=context.toLowerCase(Locale.ROOT);
        if(s.matches("(?s).*(?:letter of interest|expression of interest|loi|carta d.inter[eè]s|"
                +"pre.?screen|eligibility check).*"))return "Letter of interest / eligibility";
        if(s.matches("(?s).*(?:submission deadline|application deadline|applications close|"
                +"application closes|deadline to submit|full application|"
                +"final submission|closing date|registration closes|"
                +"fecha l[ií]mite|termini de presentaci[oó]|tanca el termini|"
                +"fecha de cierre).*"))return "Submission / application deadline";
        if(s.matches("(?s).*(?:submission opens|submissions open|registration opens|"
                +"applications open|open for registration|inscripcions obertes|"
                +"opening date).*"))return "Opening date";
        if(s.matches("(?s).*(?:testing phase|test phase|validation phase|"
                +"evaluation window|evaluation period|final test|"
                +"validation period).*"))return "Testing / evaluation phase";
        if(s.matches("(?s).*(?:interview|announcement|result publication|award ceremony|"
                +"shortlisted teams|successful teams).*"))return "Interview / results";
        return null;
    }
    public static List<DateFinding> find(String text,String sourceUrl){
        Map<String,DateFinding> unique=new LinkedHashMap<>();
        if(text==null||text.isEmpty())return new ArrayList<>();
        // Match dates in the same short clause or line as a milestone label.
        // Avoid wrong association with a completely unrelated adjacent date.
        String scan=text.substring(0,Math.min(text.length(),60000))
                .replaceAll("[\\r\\t]+"," ");
        Matcher matcher=DATE.matcher(scan);
        List<int[]> matches=new ArrayList<>();
        List<String> raw=new ArrayList<>();
        while(matcher.find() && matches.size()<120){
            matches.add(new int[]{matcher.start(),matcher.end()});
            raw.add(matcher.group());
        }
        for(int i=0;i<matches.size() && unique.size()<20;i++){
            int[] item=matches.get(i);
            String date=iso(raw.get(i));
            if(date==null)continue;
            int previous=i==0?0:matches.get(i-1)[1];
            int next=i+1==matches.size()?scan.length():matches.get(i+1)[0];
            // Never borrow milestone keywords from the neighboring date.
            int from=Math.max(previous,item[0]-105);
            int to=Math.min(next,item[1]+90);
            String context=scan.substring(from,to).replaceAll("\\s+"," ").trim();
            // Prefer the milestone immediately preceding the date. Adjacent
            // clauses after the date may describe a DIFFERENT requirement.
            int clauseFrom=Math.max(from,Math.max(
                    scan.lastIndexOf('.',item[0]-1)+1,
                    scan.lastIndexOf('\\n',item[0]-1)+1));
            String lead=scan.substring(clauseFrom,item[1]).trim();
            String label=classify(lead);
            if(label==null)label=classify(context);
            if(label==null)continue;
            String key=label+"|"+date+"|"+sourceUrl;
            unique.putIfAbsent(key,new DateFinding(label,date,context,sourceUrl));
        }
        return new ArrayList<>(unique.values());
    }
    public static String display(List<DateFinding> findings){
        if(findings==null || findings.isEmpty())
            return "No participation or delivery dates were confirmed from public page text. Check the organizer's rules.";
        StringBuilder b=new StringBuilder("DATES FOUND IN OFFICIAL PUBLIC TEXT (verify with organizer)\n");
        for(DateFinding f:findings){
            b.append("• ").append(f.kind).append(": ").append(f.date)
                .append("\n  Source: ").append(f.url)
                .append("\n  Page excerpt: ").append(f.quote).append("\n");
        }
        return b.toString();
    }
}
