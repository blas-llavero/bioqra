package com.blasllavero.bioqra;

import android.content.Context;
import org.jsoup.nodes.Element;
import java.net.URI;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Read-only scientific source discovery. Bounded 2-level crawl of the same
 * organizer only; follows no HTTP redirects and performs no form submission,
 * script execution, authentication or external AI calls while navigating.
 */
public final class ChallengePageResolver {
    private ChallengePageResolver(){}
    private static final int MAX_CANDIDATES=35;
    private static final int MAX_HTML_PAGES=10;
    private static final int MAX_PDFS=2;
    private static final int MIN_CONFIDENCE=60;

    public static final class Outcome {
        public final String text,resolvedUrl,message,deadlineSummary;
        public final boolean specific;
        public final List<DeadlineExtractor.DateFinding> dates;
        public final List<String> documentLinks;
        Outcome(String text,String resolvedUrl,String message,boolean specific,
                List<DeadlineExtractor.DateFinding> dates,List<String> documents){
            this.text=text;this.resolvedUrl=resolvedUrl;this.message=message;
            this.specific=specific;this.dates=dates;this.documentLinks=documents;
            this.deadlineSummary=DeadlineExtractor.display(dates);
        }
    }
    private static final class Candidate {
        final String url,caption;final int rank,depth;final boolean pdf;
        Candidate(String url,String caption,int rank,int depth,boolean pdf){
            this.url=url;this.caption=caption;this.rank=rank;this.depth=depth;this.pdf=pdf;
        }
    }

    private static final String[] DETAIL_WORDS={
        "challenge description","challenge brief","challenge overview","challenge details",
        "problem statement","task description","problem definition","scope of the challenge",
        "challenge scope","scientific question","research question","specific aims",
        "objectives","what to do","requirements","participation","how to participate",
        "participation guidelines","guidelines","application guidelines","instructions",
        "competition rules","official rules","terms and conditions","protocol",
        "evaluation","evaluation criteria","evaluation metric","metrics","scoring",
        "leaderboard","benchmark details","dataset","data access","download data",
        "training data","test data","submission format","submission requirements",
        "submission instructions","submission portal","important dates","key dates",
        "timeline","milestones","registration deadline","submission deadline",
        "application deadline","closing date","deadline","call for proposals",
        "call for applications","request for proposals","research proposal",
        "statement of work","work packages","eligibility","letter of interest",
        "grant application","open calls","details","read more","about the challenge",
        "descripcio del repte","bases","requisits","termini","calendari",
        "convocatoria","termino de entrega","fecha limite","bases de la convocatoria",
        "plazo de inscripcion","criterios de evaluacion","reglas","datos",
        "cahier des charges","appel a projets","date limite"
    };

    private static String words(String s){
        return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFKD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();
    }
    private static Set<String> tokens(String s){
        Set<String> result=new HashSet<>();
        String stop="|challenge|research|open|funding|the|and|for|with|into|from|"
            +"project|call|competition|submission|applications|";
        for(String t:words(s).split(" "))if(t.length()>3 && !stop.contains("|"+t+"|"))
            result.add(t);
        return result;
    }
    private static double overlap(String challenge,String page){
        Set<String> terms=tokens(challenge);
        if(terms.isEmpty())return 0;
        Set<String> inPage=tokens(page);
        int found=0;
        for(String t:terms)if(inPage.contains(t))found++;
        return (double)found/terms.size();
    }
    private static String rootHost(String raw){
        try{return URI.create(raw).getHost().toLowerCase(Locale.ROOT);}
        catch(Exception e){return "";}
    }
    private static boolean sameOrganizer(String start,String candidate){
        String a=rootHost(start),b=rootHost(candidate);
        if(a.isEmpty()||b.isEmpty())return false;
        if(a.equals(b))return true;
        if(a.replaceFirst("^www[.]","").equals(b.replaceFirst("^www[.]","")))
            return true;
        // grand-challenge.org hosts unrelated scientific challenges on
        // different subdomains. Never mix their statement or dataset evidence.
        return false;
    }
    private static boolean forbidden(String link){
        String s=link.toLowerCase(Locale.ROOT);
        return s.matches(".*(?:/sign.?in|/log.?in|/register|/signup|/account|"
                +"/logout|/submit|/upload|/admin|/profile|/oauth|"
                +"[.](?:zip|exe|docx|csv|xlsx|mp4|tar|gz)(?:[?#]|$)).*");
    }
    private static String normalized(String url){
        try{
            URI uri=new URI(url);
            if(uri.getRawQuery()!=null && uri.getRawQuery().length()>120)return null;
            return new URI(uri.getScheme(),uri.getAuthority(),uri.getPath(),
                    uri.getQuery(),null).toString();
        }catch(Exception e){return null;}
    }
    private static boolean plausible(String url,String title,String caption){
        String all=words(url+" "+caption);
        return overlap(title,all)>=0.40
                || containsDetailKeywords(all)
                || url.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }
    private static boolean containsDetailKeywords(String s){
        String normalized=words(s);
        for(String term:DETAIL_WORDS)if(normalized.contains(words(term)))return true;
        return false;
    }
    private static int linkScore(String title,String caption,String url,boolean pdf){
        String description=caption+" "+url.replace('/',' ').replace('-',' ');
        String normalized=words(description);
        int score=(int)Math.round(overlap(title,description)*95);
        if(normalized.contains(words(title)) && words(title).length()>8)score+=28;
        for(String term:DETAIL_WORDS)
            if(normalized.contains(words(term)))score+=4;
        String u=url.toLowerCase(Locale.ROOT);
        if(u.contains("/challenge/")||u.contains("/challenges/")
                ||u.contains("/problems/")||u.contains("/tasks/"))score+=14;
        if(u.contains("/how-to-participate")||u.contains("/rules")
                ||u.contains("/timeline")||u.contains("/guidelines"))score+=15;
        if(pdf)score+=18;
        if(u.contains("/apply-for-a-challenge/"))score+=8;
        if(u.endsWith("/challenges")||u.endsWith("/challenges/")
                ||u.endsWith("/apply-for-a-challenge/"))score-=40;
        if(u.contains("/privacy")||u.contains("/terms")||u.contains("/contact")
                ||u.contains("/news/")||u.contains("/blog/"))score-=50;
        return score;
    }
    private static List<Candidate> links(TrustedChallengeReader.Page page,
                                          String startUrl,String title,int depth){
        Map<String,Candidate> found=new LinkedHashMap<>();
        for(Element a:page.document.select("a[href]")){
            String href=a.absUrl("href");
            if(href.isEmpty()||href.length()>650
                    || !TrustedChallengeReader.approved(href)
                    || !sameOrganizer(startUrl,href)||forbidden(href))continue;
            boolean pdf=TrustedChallengeReader.approvedPdf(href);
            String url=normalized(href);
            if(url==null || url.equals(page.url))continue;
            String caption=a.text().trim();
            if(caption.length()>220)caption=caption.substring(0,220);
            if(!plausible(url,title,caption))continue;
            int score=linkScore(title,caption,url,pdf)-depth*7;
            if(score<25)continue;
            Candidate old=found.get(url);
            if(old==null||score>old.rank)found.put(url,
                    new Candidate(url,caption,score,depth,pdf));
        }
        List<Candidate> list=new ArrayList<>(found.values());
        list.sort((a,b)->Integer.compare(b.rank,a.rank));
        if(list.size()>MAX_CANDIDATES)
            return new ArrayList<>(list.subList(0,MAX_CANDIDATES));
        return list;
    }
    private static int detailScore(String text,String title,boolean pdf){
        String normalized=words(text);
        int score=(int)Math.round(overlap(title,normalized.substring(
                0,Math.min(normalized.length(),5000)))*65);
        for(String term:new String[]{"challenge description","problem statement",
                "scientific question","challenge challenge","the challenge is to",
                "this challenge seeks","evaluation criteria","evaluation metric",
                "rules and regulations","objectives","requirements","overview",
                "research aims","task description","submission format"}){
            if(normalized.contains(words(term)))score+=10;
        }
        if(normalized.contains("apply for a challenge challenges currently open")
                ||normalized.contains("explore other challenges")
                ||normalized.contains("challenge directory"))score-=30;
        if(text.length()<350)score-=30;
        if(pdf)score+=8;
        return score;
    }
    private static void addDates(Map<String,DeadlineExtractor.DateFinding> saved,
                                  String text,String url){
        for(DeadlineExtractor.DateFinding finding:DeadlineExtractor.find(text,url)){
            if(saved.size()>=20)break;
            saved.putIfAbsent(finding.kind+"|"+finding.date+"|"+finding.url,finding);
        }
    }

    public static Outcome resolve(Context context,String startingUrl,String title) throws Exception{
        TrustedChallengeReader.Page start=TrustedChallengeReader.fetchPage(startingUrl);
        String bestText=start.text,bestUrl=start.url;
        int bestScore=detailScore(start.text,title,false),runnerUp=-100;
        Map<String,DeadlineExtractor.DateFinding> dates=new LinkedHashMap<>();
        Map<String,Candidate> queue=new LinkedHashMap<>();
        List<String> pdfs=new ArrayList<>();
        addDates(dates,start.text,start.url);
        for(Candidate c:links(start,startingUrl,title,0))queue.put(c.url,c);
        Set<String> seen=new HashSet<>();
        seen.add(start.url);
        int htmlCount=0,pdfCount=0;
        while(!queue.isEmpty()&&(htmlCount<MAX_HTML_PAGES||pdfCount<MAX_PDFS)){
            List<Candidate> ordered=new ArrayList<>(queue.values());
            ordered.sort((a,b)->Integer.compare(b.rank,a.rank));
            Candidate candidate=ordered.get(0);
            queue.remove(candidate.url);
            if(!seen.add(candidate.url))continue;
            if(candidate.rank<30)break;
            try{
                final String text;
                if(candidate.pdf){
                    if(pdfCount>=MAX_PDFS)continue;
                    pdfCount++;
                    text=TrustedChallengeReader.fetchPdf(context,candidate.url);
                    pdfs.add(candidate.url);
                }else{
                    if(htmlCount>=MAX_HTML_PAGES)continue;
                    htmlCount++;
                    TrustedChallengeReader.Page page=
                            TrustedChallengeReader.fetchPage(candidate.url);
                    text=page.text;
                    if(candidate.depth<1){
                        for(Candidate next:links(page,startingUrl,title,1)){
                            if(!seen.contains(next.url)){
                                Candidate old=queue.get(next.url);
                                if(old==null||next.rank>old.rank)
                                    queue.put(next.url,next);
                            }
                        }
                    }
                }
                addDates(dates,text,candidate.url);
                int score=detailScore(text,title,candidate.pdf)+candidate.rank/4;
                if(score>bestScore){
                    runnerUp=bestScore;bestScore=score;
                    bestText=text;bestUrl=candidate.url;
                }else if(score>runnerUp)runnerUp=score;
            }catch(Exception ignored){
                // Some sources require JavaScript, login, larger documents or redirect.
                // Do not bypass those requirements.
            }
        }
        boolean specific=bestScore>=MIN_CONFIDENCE
                &&(bestUrl.equals(start.url)||bestScore-runnerUp>=10);
        List<DeadlineExtractor.DateFinding> findings=
                new ArrayList<>(dates.values());
        String message=!specific
            ? "No confidently identified detailed statement. Manual organizer review needed."
            : bestUrl.equals(start.url)
                ? "The initial public URL appears to contain the detailed challenge."
                : "Detailed source discovered on the same verified organizer site.";
        return new Outcome(specific?bestText:start.text,
                specific?bestUrl:start.url,
                message+" Checked "+htmlCount+" additional HTML pages and "
                    +pdfCount+" small official PDF candidates. Dates are unverified page claims.",
                specific,findings,pdfs);
    }

    /** Kept for compatibility with callers that do not have a Context. */
    public static Outcome resolve(String startingUrl,String title) throws Exception {
        throw new UnsupportedOperationException("Use resolve(Context, URL, title) for safe PDF support.");
    }
}
