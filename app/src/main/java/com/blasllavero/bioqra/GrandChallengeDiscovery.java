package com.blasllavero.bioqra;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative discovery of all currently accepting or upcoming Grand Challenge
 * submissions, across all paginated index pages. No API key, AI, or patient data.
 * A listing is not proof of general eligibility, and must be checked with the host.
 */
public final class GrandChallengeDiscovery {
    private GrandChallengeDiscovery() {}
    private static final String BASE="https://grand-challenge.org/challenges/";
    private static final String CACHE="live_grand_challenges.json";
    private static final Pattern PAGE_COUNT=Pattern.compile("Page\\s+1\\s+of\\s+(\\d+)",Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS=Pattern.compile(
        "(?i)(opening submissions[^.]*|accepting submissions[^.]*)");
    private static final Pattern DATE=Pattern.compile(
        "(?i)\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+(\\d{1,2})\\s+(20\\d{2})\\b");
    private static final DateTimeFormatter DATE_FORMAT=
        DateTimeFormatter.ofPattern("MMM d yyyy",Locale.ENGLISH);
    private static final int MAX_BYTES=2_000_000;
    private static final int MAX_PAGES=12;

    private static String dateIn(String text) {
        Matcher m=DATE.matcher(text);
        if (!m.find()) return null;
        String month=m.group(1);
        String input=month.substring(0,1).toUpperCase(Locale.ROOT)+month.substring(1).toLowerCase(Locale.ROOT)
                +" "+m.group(2)+" "+m.group(3);
        try {return LocalDate.parse(input,DATE_FORMAT).toString();}
        catch (DateTimeParseException ex){return null;}
    }

    private static String challengeUrl(Element card, Element heading) {
        // Choose a real organizer or platform link, never synthesize URLs from titles.
        Elements links=new Elements();
        links.addAll(heading.select("a[href]"));
        links.addAll(card.select("a[href]"));
        for(Element a:links) {
            String href=a.absUrl("href");
            try {
                URI uri=URI.create(href);
                String host=uri.getHost();
                if(host==null || !"https".equalsIgnoreCase(uri.getScheme())) continue;
                host=host.toLowerCase(Locale.ROOT);
                if(!(host.equals("grand-challenge.org") || host.endsWith(".grand-challenge.org")))
                    continue;
                if(host.equals("grand-challenge.org") && (uri.getPath().equals("/challenges/")
                       || uri.getPath().equals("/"))) continue;
                return href;
            } catch(Exception ignored){}
        }
        return null;
    }

    private static Element cardFor(Element heading) {
        Element ancestor=heading;
        for(int step=0;step<7 && ancestor!=null;step++,ancestor=ancestor.parent()) {
            String txt=ancestor.text().toLowerCase(Locale.ROOT);
            if((txt.contains("accepting submissions") || txt.contains("opening submissions"))
                    && ancestor.select("h5,h4").size()==1)
                return ancestor;
        }
        return null;
    }

    public static List<ResearchCatalog.Item> parsePage(String html,String checkedDate) {
        Document doc=Jsoup.parse(html,BASE);
        Map<String, ResearchCatalog.Item> output=new LinkedHashMap<>();
        for(Element heading:doc.select("h5,h4")) {
            String title=heading.text().trim();
            if(title.length()<8 || title.length()>180) continue;
            Element card=cardFor(heading);
            if(card==null) continue;
            String full=card.text().replaceAll("\\s+"," ");
            String lower=full.toLowerCase(Locale.ROOT);
            if(lower.contains("not accepting submissions") || lower.contains("challenge completed"))
                continue;
            Matcher status=STATUS.matcher(full);
            if(!status.find()) continue;
            String statusText=status.group(1).trim();
            boolean upcoming=statusText.toLowerCase(Locale.ROOT).startsWith("opening submissions");
            String date=dateIn(statusText);
            if(date!=null) {
                LocalDate time=LocalDate.parse(date);
                if(!upcoming && LocalDate.now().isAfter(time)) continue; // expired deadline
                if(upcoming && !LocalDate.now().isBefore(time)) continue; // older opening date needs live recheck
            }
            String url=challengeUrl(card,heading);
            if(url==null) continue;
            // Grand Challenge's vendor-only clinical device track is not publicly eligible.
            if(title.toLowerCase(Locale.ROOT).contains("lung nodule volumetry")) continue;
            String id="gc-live-"+Integer.toHexString(url.toLowerCase(Locale.ROOT).hashCode());
            String kind=upcoming?"live_upcoming":"live_open";
            String topic=(title.toLowerCase(Locale.ROOT).matches(
                ".*(cancer|tumou?r|lesion|melanoma|prostate|radiotherap|oncolog|pet/ct|nodule).*"))
                    ? "cancer biomedical imaging" : "biomedical imaging";
            output.put(url,new ResearchCatalog.Item(id,title,ResearchCatalog.GRAND,
                "Official index: "+statusText+". Verify registration and eligibility.",
                url,kind,topic,date,checkedDate));
        }
        return new ArrayList<>(output.values());
    }

    private static String fetch(int page) throws Exception {
        String address=BASE+"?page="+page;
        HttpURLConnection connection=(HttpURLConnection)new URL(address).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(16000);
        connection.setRequestProperty("User-Agent","BIOQRA/0.3 scientific challenge directory");
        connection.setInstanceFollowRedirects(false);
        try {
            if(connection.getResponseCode()!=200) throw new Exception("Official index HTTP "+connection.getResponseCode());
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream input=connection.getInputStream()){
                byte[] bytes=new byte[8192];int n;
                while((n=input.read(bytes))!=-1){
                    out.write(bytes,0,n);
                    if(out.size()>MAX_BYTES) throw new Exception("Official index too large");
                }
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }finally{connection.disconnect();}
    }

    public static List<ResearchCatalog.Item> discover() throws Exception {
        String first=fetch(1);
        Document page=Jsoup.parse(first,BASE);
        Matcher count=PAGE_COUNT.matcher(page.text());
        int pages=count.find()?Integer.parseInt(count.group(1)):1;
        if(pages<1 || pages>MAX_PAGES) throw new Exception("Unexpected pagination count "+pages);
        String today=LocalDate.now().toString();
        Map<String,ResearchCatalog.Item> unique=new LinkedHashMap<>();
        for(int i=1;i<=pages;i++) {
            String html=i==1?first:fetch(i);
            for(ResearchCatalog.Item item:parsePage(html,today)) unique.put(item.url,item);
        }
        if(unique.size()<2) throw new Exception("Cannot verify open opportunities; retaining previous cache");
        return new ArrayList<>(unique.values());
    }

    public static void save(Context context,List<ResearchCatalog.Item> items) throws Exception {
        JSONArray json=new JSONArray();
        for(ResearchCatalog.Item i:items) {
            JSONObject x=new JSONObject();
            x.put("id",i.id);x.put("title",i.title);x.put("source",i.source);
            x.put("summary",i.summary);x.put("url",i.url);x.put("kind",i.kind);
            x.put("topic",i.topic);x.put("deadline",i.deadline);
            x.put("verified_on",i.verifiedOn);
            json.put(x);
        }
        java.io.File next=new java.io.File(context.getFilesDir(),CACHE+".tmp");
        java.io.File target=new java.io.File(context.getFilesDir(),CACHE);
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(next)) {
            out.write(json.toString().getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        }
        if(!next.renameTo(target)) throw new Exception("Unable to replace cache");
    }

    public static List<ResearchCatalog.Item> load(Context context) {
        List<ResearchCatalog.Item> result=new ArrayList<>();
        try(java.io.FileInputStream in=new java.io.FileInputStream(
                new java.io.File(context.getFilesDir(),CACHE))) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1) {
                out.write(buffer,0,n);
                if(out.size()>1_000_000) return result;
            }
            JSONArray arr=new JSONArray(out.toString(StandardCharsets.UTF_8.name()));
            for(int i=0;i<arr.length();i++){
                JSONObject x=arr.getJSONObject(i);
                String url=x.getString("url");
                if(!url.startsWith("https://") || !url.contains(".grand-challenge.org")) continue;
                result.add(new ResearchCatalog.Item(x.getString("id"),x.getString("title"),
                    ResearchCatalog.GRAND,x.getString("summary"),url,x.getString("kind"),
                    x.getString("topic"),x.optString("deadline",null),x.getString("verified_on")));
            }
        }catch(Exception ignored){} // Keep bundled reviewed data when offline.
        return result;
    }
}
