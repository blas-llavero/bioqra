package com.blasllavero.bioqra;

import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Read-only live discovery of real GEO study records; no patient data are downloaded. */
public final class PublicDatasetDiscovery {
    private PublicDatasetDiscovery(){}

    public static final class Study {
        public final String accession,title,summary,url;
        public final int relevance;
        Study(String accession,String title,String summary,int relevance){
            this.accession=accession;this.title=title;this.summary=summary;
            this.url="https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc="+accession;
            this.relevance=relevance;
        }
    }
    public static final class Report {
        public final List<Study> studies;
        public final String note;
        Report(List<Study> studies,String note){this.studies=studies;this.note=note;}
        public String forModel(){
            StringBuilder b=new StringBuilder("LIVE PUBLIC DATASET DISCOVERY (NCBI GEO metadata only; no data files downloaded):\n");
            b.append(note).append('\n');
            for(Study study:studies)
                b.append("Verified repository record: ").append(study.accession)
                    .append("\nTitle: ").append(study.title)
                    .append("\nSource: ").append(study.url)
                    .append("\nDescription: ").append(study.summary)
                    .append("\nStatus: public metadata retrieved; raw measurements, permissions and outcome suitability NOT validated.\n\n");
            b.append("Do not claim these studies were analyzed. No patient data or molecular matrices were loaded. ");
            b.append("Review design and source files before selecting any study for computational analysis.");
            return b.toString();
        }
    }

    private static JSONObject json(String url) throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setInstanceFollowRedirects(false);
        c.setRequestProperty("Accept","application/json");
        c.setRequestProperty("User-Agent","BIOQRA public scholarly metadata search");
        try{
            if(c.getResponseCode()!=200)throw new Exception("NCBI returned HTTP "+c.getResponseCode());
            try(InputStream input=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int count;
                while((count=input.read(buffer))!=-1){
                    out.write(buffer,0,count);
                    if(out.size()>220000)throw new Exception("Metadata response exceeded limit");
                }
                return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
            }
        }finally{c.disconnect();}
    }

    private static String query(String title){
        String t=title==null?"":title.toLowerCase(Locale.ROOT);
        if(t.contains("gastro")||t.contains("esoph")||t.contains("oesoph"))
            return "(esophageal OR oesophageal OR gastric) AND (cancer OR adenocarcinoma) AND gse[ETYP]";
        if(t.contains("breast"))return "breast cancer AND gse[ETYP]";
        if(t.contains("lung"))return "lung cancer AND gse[ETYP]";
        if(t.contains("leuk"))return "leukemia AND gse[ETYP]";
        if(t.contains("gliob")||t.contains("brain"))return "glioblastoma AND gse[ETYP]";
        String cleaned=t.replaceAll("[^a-z0-9 ]"," ").trim();
        if(cleaned.length()>65)cleaned=cleaned.substring(0,65).trim();
        return (cleaned.isEmpty()?"cancer":cleaned)+" AND gse[ETYP]";
    }

    private static int relevance(String challenge,String title,String summary){
        String all=(title+" "+summary).toLowerCase(Locale.ROOT);
        String c=challenge==null?"":challenge.toLowerCase(Locale.ROOT);
        int score=0;
        if(all.contains("human")||all.contains("homo sapiens"))score+=3;
        if(all.contains("tissue")||all.contains("biopsy"))score+=2;
        if(all.contains("normal")||all.contains("control"))score+=2;
        if(all.contains("longitudinal")||all.contains("follow-up"))score+=3;
        if(all.contains("cancer")||all.contains("carcinoma"))score+=2;
        if(c.contains("esoph")||c.contains("oesoph")||c.contains("gastro")){
            if(all.contains("esoph")||all.contains("oesoph"))score+=6;
            if(all.contains("gastric"))score+=2;
        }
        return score;
    }

    public static Report discover(String challenge) throws Exception{
        String base="https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
        String terms=Uri.encode(query(challenge));
        JSONObject search=json(base+"esearch.fcgi?db=gds&retmode=json&retmax=12&term="+terms);
        JSONArray ids=search.getJSONObject("esearchresult").optJSONArray("idlist");
        if(ids==null||ids.length()==0)
            return new Report(Collections.emptyList(),"No matching GEO records found by this query.");
        StringBuilder joined=new StringBuilder();
        for(int i=0;i<Math.min(12,ids.length());i++){
            if(i>0)joined.append(',');
            joined.append(ids.optString(i).replaceAll("[^0-9]",""));
        }
        JSONObject response=json(base+"esummary.fcgi?db=gds&retmode=json&id="+joined);
        JSONObject result=response.getJSONObject("result");
        List<Study> studies=new ArrayList<>();
        for(int i=0;i<Math.min(12,ids.length());i++){
            JSONObject item=result.optJSONObject(ids.optString(i));
            if(item==null)continue;
            String acc=item.optString("accession","");
            if(!acc.matches("GSE[0-9]+"))continue;
            String title=item.optString("title","").replaceAll("[\\r\\n]+"," ");
            String description=item.optString("summary","").replaceAll("[\\r\\n]+"," ");
            if(title.length()>240)title=title.substring(0,240);
            if(description.length()>450)description=description.substring(0,450);
            studies.add(new Study(acc,title,description,relevance(challenge,title,description)));
        }
        studies.sort((a,b)->Integer.compare(b.relevance,a.relevance));
        if(studies.size()>5)studies=new ArrayList<>(studies.subList(0,5));
        return new Report(studies,studies.isEmpty()
            ?"No valid study-series accession in live GEO metadata."
            :"These are real GEO study records ranked by keyword relevance, not validated datasets or efficacy findings.");
    }
}
