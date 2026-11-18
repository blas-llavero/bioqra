package com.blasllavero.bioqra;

import android.content.Context;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.jsoup.nodes.Element;

/**
 * Converts official public challenge pages to a small source-aware, navigable
 * document. No arbitrary browser scripting, submissions, credentials or calls
 * to ChatGPT happen in this class.
 */
public final class ChallengeSectionNavigator {
    private ChallengeSectionNavigator() {}
    public static final String FULL="full",CHALLENGE="challenge",IMPACT="impact",
        SUMMARY="summary",DOCUMENTS="documents",APPLICATIONS="applications",
        DEADLINE="deadline",RELATED="related";

    public static final String[] KEYS={CHALLENGE,IMPACT,SUMMARY,DOCUMENTS,
        APPLICATIONS,DEADLINE,RELATED};

    public static String label(String key){
        switch(key){
            case CHALLENGE:return "Challenge";
            case IMPACT:return "Impact";
            case SUMMARY:return "Summary";
            case DOCUMENTS:return "Documents Útils";
            case APPLICATIONS:return "Applications";
            case DEADLINE:return "Deadline";
            case RELATED:return "Related Content";
            default:return "Full statement";
        }
    }

    public static final class Section {
        public final String key,body,url;
        public final boolean extracted;
        Section(String key,String body,String url,boolean extracted){
            this.key=key;this.body=body;this.url=url;this.extracted=extracted;
        }
    }

    public static final class Snapshot {
        public final String sourceUrl,fullText;
        private final Map<String,Section> parts=new HashMap<>();
        private final Map<String,String> linkedPages=new HashMap<>();
        Snapshot(String sourceUrl,String fullText){
            this.sourceUrl=sourceUrl;this.fullText=fullText;
            parts.put(FULL,new Section(FULL,fullText,sourceUrl,true));
        }
        public Section get(String key){return parts.get(key);}
        public String link(String key){return linkedPages.get(key);}
        void set(String key,String text,String url,boolean extracted){
            if(text!=null && !text.trim().isEmpty())
                parts.put(key,new Section(key,text.trim(),url,extracted));
        }
        void link(String key,String url){linkedPages.putIfAbsent(key,url);}
    }

    private static String normalized(String text){
        return text.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9 ]+"," ")
            .replaceAll("\\s+"," ").trim();
    }
    private static String classify(String block){
        if(block.length()>110 || block.contains("\n"))return null;
        String n=normalized(block);
        if(n.matches("(challenge|the challenge|research challenge|the problem|problem statement|scientific challenge|objective|objectives|aims|scope of the challenge)"))
            return CHALLENGE;
        if(n.matches("(impact|expected impact|potential impact|impact on science|scientific impact|outcomes and impact)"))
            return IMPACT;
        if(n.matches("(summary|plain language summary|executive summary|overview|scientific summary|overarching context and unmet need|context|background|background and context)"))
            return SUMMARY;
        if(n.matches("(useful documents|supporting documents|documents|resources|downloads|application guidelines|guidelines and documents)"))
            return DOCUMENTS;
        if(n.matches("(applications|application|how to apply|how to participate|apply|eligibility|application process|apply for a challenge|submitting an application|submission guidelines)"))
            return APPLICATIONS;
        if(n.matches("(deadline|deadlines|key dates|important dates|timeline|submission dates|application deadline|submission deadline|registration dates|schedule)"))
            return DEADLINE;
        if(n.matches("(related content|related contents|related challenges|related research|explore other challenges|related links|further reading)"))
            return RELATED;
        return null;
    }
    private static boolean sameOrganizer(String a,String b){
        try{
            String x=URI.create(a).getHost().toLowerCase(Locale.ROOT).replaceFirst("^www[.]","");
            String y=URI.create(b).getHost().toLowerCase(Locale.ROOT).replaceFirst("^www[.]","");
            return x.equals(y) || (
                (x.equals("grand-challenge.org")||x.endsWith(".grand-challenge.org"))
                && (y.equals("grand-challenge.org")||y.endsWith(".grand-challenge.org")));
        }catch(Exception ex){return false;}
    }
    private static String cleanUrl(String url){
        try{
            URI uri=URI.create(url);
            return new URI(uri.getScheme(),uri.getAuthority(),uri.getPath(),
                    uri.getQuery(),null).toString();
        }catch(Exception e){return "";}
    }
    private static String sectionFromLink(String label,String url){
        String n=normalized(label+" "+url.replace('-',' '));
        if(n.matches(".*(related|other challenges|further reading|read more).*"))return RELATED;
        if(n.matches(".*(application guideline|helpful document|useful document|download pdf|rules pdf|brief pdf|guideline pdf).*"))
            return DOCUMENTS;
        if(n.matches(".*(submission deadline|important dates|key dates|timeline|schedule).*"))
            return DEADLINE;
        if(n.matches(".*(apply|eligibility|letter of interest|application|submission portal|registration).*"))
            return APPLICATIONS;
        if(n.matches(".*(impact|expected outcomes).*"))return IMPACT;
        if(n.matches(".*(summary|overview|plain language).*"))return SUMMARY;
        if(n.matches(".*(challenge brief|problem statement|challenge details|scientific question).*"))
            return CHALLENGE;
        return null;
    }

    public static Snapshot fromPage(TrustedChallengeReader.Page page,
                                    String extractedDates,List<String> pdfs){
        Snapshot snapshot=new Snapshot(page.url,page.text);
        String[] paragraphs=page.text.split("\\n\\s*\\n");
        Map<String,StringBuilder> accumulated=new LinkedHashMap<>();
        String selected=null;
        for(String paragraph:paragraphs){
            String p=paragraph.trim();
            if(p.isEmpty())continue;
            String section=classify(p);
            if(section!=null){
                selected=section;
                accumulated.putIfAbsent(selected,new StringBuilder());
                continue;
            }
            if(selected!=null){
                StringBuilder b=accumulated.get(selected);
                if(b.length()<14500){
                    if(b.length()>0)b.append("\n\n");
                    b.append(p);
                }
            }
        }
        for(Map.Entry<String,StringBuilder> e:accumulated.entrySet()){
            String value=e.getValue().toString().trim();
            if(value.length()>=25)snapshot.set(e.getKey(),value,page.url,true);
        }
        // Dates are not a generated guess; each comes with the organizer link.
        if(extractedDates!=null && !extractedDates.trim().isEmpty()
                && !extractedDates.startsWith("No participation")){
            snapshot.set(DEADLINE,extractedDates,page.url,true);
        }

        Map<String,StringBuilder> collectedLinks=new LinkedHashMap<>();
        Set<String> seen=new HashSet<>();
        for(Element a:page.document.select("a[href]")){
            String href=cleanUrl(a.absUrl("href"));
            if(!TrustedChallengeReader.approved(href)
                    || !sameOrganizer(page.url,href)
                    || href.equals(page.url))continue;
            String caption=a.text().replaceAll("\\s+"," ").trim();
            if(caption.isEmpty()||caption.length()>170)continue;
            String category=sectionFromLink(caption,href);
            if(category==null)continue;
            if(!seen.add(category+"|"+href))continue;
            collectedLinks.putIfAbsent(category,new StringBuilder());
            StringBuilder list=collectedLinks.get(category);
            if(list.length()<5000){
                list.append("• ").append(caption).append("\n  ").append(href).append("\n\n");
                snapshot.link(category,href);
            }
        }
        if(pdfs!=null){
            for(String pdf:pdfs){
                if(!TrustedChallengeReader.approvedPdf(pdf)||!seen.add(DOCUMENTS+"|"+pdf))continue;
                collectedLinks.putIfAbsent(DOCUMENTS,new StringBuilder());
                StringBuilder list=collectedLinks.get(DOCUMENTS);
                if(list.length()<5000)list.append("• Official public PDF\n  ").append(pdf).append("\n\n");
                snapshot.link(DOCUMENTS,pdf);
            }
        }
        for(Map.Entry<String,StringBuilder> e:collectedLinks.entrySet()){
            Section section=snapshot.get(e.getKey());
            String links=e.getValue().toString().trim();
            if(section==null)snapshot.set(e.getKey(),links,page.url,true);
            else snapshot.set(e.getKey(),section.body+"\n\nOfficial links:\n"+links,
                    section.url,section.extracted);
        }
        return snapshot;
    }

    /** Local fallback when refreshing HTML is unavailable. */
    public static Snapshot fromText(String url,String text,String dates,List<String> pdfs){
        Snapshot snapshot=new Snapshot(url,text);
        String current=null;
        Map<String,StringBuilder> sections=new LinkedHashMap<>();
        for(String paragraph:text.split("\\n\\s*\\n")){
            String line=paragraph.trim();
            if(line.isEmpty())continue;
            String label=classify(line);
            if(label!=null){
                current=label;
                sections.putIfAbsent(current,new StringBuilder());
            }else if(current!=null){
                StringBuilder b=sections.get(current);
                if(b.length()<14000){
                    if(b.length()>0)b.append("\n\n");
                    b.append(line);
                }
            }
        }
        for(Map.Entry<String,StringBuilder> item:sections.entrySet())
            if(item.getValue().length()>=25)
                snapshot.set(item.getKey(),item.getValue().toString(),url,true);
        if(dates!=null&&!dates.startsWith("No participation"))
            snapshot.set(DEADLINE,dates,url,true);
        if(pdfs!=null && !pdfs.isEmpty()){
            StringBuilder links=new StringBuilder();
            for(String item:pdfs)if(TrustedChallengeReader.approvedPdf(item)){
                links.append("• ").append(item).append("\n");
                snapshot.link(DOCUMENTS,item);
            }
            if(links.length()>0)snapshot.set(DOCUMENTS,links.toString(),url,true);
        }
        return snapshot;
    }

    /** Retrieves at most one matching, approved public source on user tap. */
    public static Section follow(Context context,Snapshot snapshot,String key) throws Exception{
        if(snapshot==null)return null;
        String target=snapshot.link(key);
        if(target==null || !TrustedChallengeReader.approved(target)
                || !sameOrganizer(snapshot.sourceUrl,target))return null;
        String fetched;
        if(TrustedChallengeReader.approvedPdf(target))
            fetched=TrustedChallengeReader.fetchPdf(context,target);
        else fetched=TrustedChallengeReader.fetchPage(target).text;
        // The document is always identified as a retrieved source, not a proof
        // of eligibility, open registration, or a verified research result.
        String combined=fetched;
        if(fetched.length()>12000)combined=ChallengeTextFormatter.truncate(fetched,12000);
        Section existing=snapshot.get(key);
        if(existing!=null && !existing.body.isEmpty()
                && !existing.url.equals(target)){
            combined=existing.body+"\n\n— ADDITIONAL OFFICIAL SOURCE —\n"
                    +target+"\n\n"+combined;
        }
        Section section=new Section(key,combined,target,true);
        snapshot.set(key,section.body,target,true);
        return section;
    }
}
