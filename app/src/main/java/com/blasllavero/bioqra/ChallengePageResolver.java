package com.blasllavero.bioqra;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URI;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic, limited public-page link discovery. Only follows approved
 * organizer HTTPS links; AI is used solely on the eventual extracted statement.
 * Never submits forms, follows HTTP redirects, executes scripts or downloads data.
 */
public final class ChallengePageResolver {
    private ChallengePageResolver() {}
    private static final int MAX_CANDIDATES=8;
    private static final int MAX_FOLLOW_UP=4;
    private static final int MIN_CONFIDENCE=58;

    public static final class Outcome {
        public final String text, resolvedUrl, message;
        public final boolean specific;
        Outcome(String text,String resolvedUrl,String message,boolean specific){
            this.text=text;this.resolvedUrl=resolvedUrl;this.message=message;this.specific=specific;
        }
    }
    private static final class Candidate {
        final String url, caption;
        final int rank;
        Candidate(String url,String caption,int rank){
            this.url=url;this.caption=caption;this.rank=rank;
        }
    }

    private static String words(String text) {
        return Normalizer.normalize(text,Normalizer.Form.NFKD)
                .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();
    }
    private static Set<String> tokens(String text){
        Set<String> tokens=new HashSet<>();
        for(String token:words(text).split(" ")) if(token.length()>=4 &&
                !token.equals("challenge") && !token.equals("research") &&
                !token.equals("open") && !token.equals("funding")) tokens.add(token);
        return tokens;
    }
    private static double overlap(String a,String b){
        Set<String> ts=tokens(a);
        if(ts.isEmpty())return 0;
        Set<String> candidate=tokens(b);
        int matching=0;
        for(String t:ts)if(candidate.contains(t))matching++;
        return (double)matching / ts.size();
    }
    private static boolean sameOrganizer(String a,String b){
        try{
            String x=URI.create(a).getHost().toLowerCase(Locale.ROOT);
            String y=URI.create(b).getHost().toLowerCase(Locale.ROOT);
            if(x.equals(y))return true;
            return (x.equals("grand-challenge.org") || x.endsWith(".grand-challenge.org"))
                &&(y.equals("grand-challenge.org") || y.endsWith(".grand-challenge.org"));
        }catch(Exception e){return false;}
    }
    private static boolean forbidden(String url,String label){
        String s=(url+" "+label).toLowerCase(Locale.ROOT);
        return s.matches(".*(sign.?in|log.?in|register|apply.?now|submit.?application|letter.of.interest|"
            +"eligibility.form|privacy|cookie|terms.of.service|logout|account|/download|"
            +"\\.pdf(?:[?#]|$)|\\.zip(?:[?#]|$)|\\.csv(?:[?#]|$)).*");
    }
    private static List<Candidate> links(TrustedChallengeReader.Page page,String title) {
        Map<String,Candidate> unique=new LinkedHashMap<>();
        String titleWords=words(title);
        for(Element a:page.document.select("a[href]")) {
            String href=a.attr("abs:href");
            if(!TrustedChallengeReader.approved(href) || !sameOrganizer(page.url,href))continue;
            if(href.length()>650 || forbidden(href,a.text()))continue;
            String url;
            try{
                URI u=URI.create(href);
                URI original=URI.create(page.url);
                if(u.getRawQuery()!=null && u.getRawQuery().length()>150)continue;
                url=new URI(u.getScheme(),u.getAuthority(),u.getPath(),u.getQuery(),null).toString();
                if(url.equals(page.url) || u.getPath().equals(original.getPath()))continue;
            }catch(Exception ex){continue;}
            String caption=a.text().trim();
            String desc=caption+" "+url.replace('/',' ').replace('-',' ');
            int rank=(int)Math.round(overlap(title,desc)*85);
            if(words(desc).contains(titleWords) && titleWords.length()>=9)rank+=35;
            String u=url.toLowerCase(Locale.ROOT);
            if(u.contains("/challenge/") || u.contains("/challenges/") ||
                    u.contains("/problems/") || u.contains("/benchmark"))rank+=12;
            if(caption.equalsIgnoreCase("read more") || caption.equalsIgnoreCase("details"))rank+=4;
            if(u.contains("/apply-for-a-challenge/") && u.split("/").length>=5)rank+=12;
            if(u.contains("/apply-for-a-challenge") && u.split("/").length<5)rank-=25;
            if(u.endsWith("/open-challenges/") || u.endsWith("/challenges/"))rank-=30;
            if(rank<24)continue;
            Candidate old=unique.get(url);
            if(old==null || rank>old.rank)unique.put(url,new Candidate(url,caption,rank));
        }
        List<Candidate> candidates=new ArrayList<>(unique.values());
        candidates.sort((a,b)->Integer.compare(b.rank,a.rank));
        if(candidates.size()>MAX_CANDIDATES)return new ArrayList<>(candidates.subList(0,MAX_CANDIDATES));
        return candidates;
    }
    private static int detailsScore(String text,String title){
        String normal=words(text);
        int score=(int)Math.round(overlap(title,normal.substring(0,Math.min(normal.length(),3500)))*55);
        if(normal.contains("challenge challenge") || normal.contains("this challenge seeks")
                || normal.contains("problem statement") || normal.contains("evaluation metric")
                || normal.contains("the challenge is to"))score+=30;
        if(normal.contains("overarching context") || normal.contains("plain language summary")
                || normal.contains("impact impact"))score+=12;
        if(normal.contains("apply for a challenge challenges currently open")
                || normal.contains("challenge directory") || normal.contains("explore other challenges"))
            score-=25;
        if(text.length()<300)score-=30;
        return score;
    }

    public static Outcome resolve(String startingUrl,String challengeTitle) throws Exception{
        TrustedChallengeReader.Page start=TrustedChallengeReader.fetchPage(startingUrl);
        int sourceScore=detailsScore(start.text,challengeTitle);
        List<Candidate> candidates=links(start,challengeTitle);
        String chosenUrl=start.url,chosenText=start.text;
        int bestScore=sourceScore;
        int runnerUp=-100;
        int scanned=0;
        // Follow only strong, challenge-title-matching links. No general web crawl.
        for(Candidate candidate:candidates){
            if(scanned>=MAX_FOLLOW_UP || candidate.rank<40)break;
            scanned++;
            try{
                TrustedChallengeReader.Page candidatePage=TrustedChallengeReader.fetchPage(candidate.url);
                int score=detailsScore(candidatePage.text,challengeTitle)+candidate.rank/3;
                if(score>bestScore){
                    runnerUp=bestScore;
                    bestScore=score;chosenUrl=candidatePage.url;chosenText=candidatePage.text;
                }else if(score>runnerUp)runnerUp=score;
            }catch(Exception ignored){
                // A candidate may require registration, redirect, or be inaccessible.
            }
        }
        boolean selectedSpecific=bestScore>=MIN_CONFIDENCE
                && (chosenUrl.equals(start.url) || bestScore-runnerUp>=12);
        if(!selectedSpecific){
            return new Outcome(start.text,start.url,
                "A specific problem statement could not be confirmed with confidence. "
                +"The initial public page is shown for manual review. No AI request was sent.",
                false);
        }
        return new Outcome(chosenText,chosenUrl,
                chosenUrl.equals(start.url)
                    ? "The selected URL already contains the detailed public challenge."
                    : "BIOQRA followed an official organizer link to a more specific challenge brief.",
                true);
    }
}
