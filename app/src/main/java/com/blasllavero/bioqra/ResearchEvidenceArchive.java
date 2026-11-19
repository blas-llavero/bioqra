package com.blasllavero.bioqra;

import android.content.Context;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Evidence package for real, freely downloadable GEO Series Matrix files.
 * Bounded downloads, fixed official host and accession-derived paths, no
 * patient upload, scripts, authentication, redirects or untrusted URLs.
 */
public final class ResearchEvidenceArchive {
    private ResearchEvidenceArchive(){}
    private static final int MAX_FILE=8_000_000;
    private static final int MAX_FILES=2;
    public static final class Outcome {
        public final String summary;
        public final int files;
        Outcome(String summary,int files){this.summary=summary;this.files=files;}
    }
    private static File folder(Context context,String key) throws Exception {
        if(key==null || !key.matches("[a-f0-9]{32}"))throw new Exception("Invalid challenge key");
        File parent=new File(context.getFilesDir(),"research-evidence");
        File target=new File(parent,key);
        if(!target.isDirectory() && !target.mkdirs())throw new Exception("Cannot create evidence folder");
        return target;
    }
    private static String hex(byte[] bytes){
        StringBuilder b=new StringBuilder();
        for(byte v:bytes)b.append(String.format(java.util.Locale.ROOT,"%02x",v&255));
        return b.toString();
    }
    private static String matrixUrl(String accession){
        if(!accession.matches("GSE[0-9]{1,9}"))throw new IllegalArgumentException("Invalid GEO accession");
        String number=accession.substring(3);
        String group=number.length()<=3?"GSEnnn":"GSE"+number.substring(0,number.length()-3)+"nnn";
        return "https://ftp.ncbi.nlm.nih.gov/geo/series/"+group+"/"+accession
            +"/matrix/"+accession+"_series_matrix.txt.gz";
    }
    private static JSONObject tryMatrix(File directory,PublicDatasetDiscovery.Study study) throws Exception {
        String address=matrixUrl(study.accession);
        HttpURLConnection connection=(HttpURLConnection)new URL(address).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(16000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept","application/gzip,application/octet-stream");
        String name=study.accession+"_series_matrix.txt.gz";
        File target=new File(directory,name);
        File pending=new File(directory,name+".partial");
        try{
            if(connection.getResponseCode()!=200)throw new Exception("HTTP "+connection.getResponseCode());
            int total=0;
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            byte[] first=new byte[2];int prefix=0;
            try(InputStream in=connection.getInputStream();OutputStream out=new FileOutputStream(pending)){
                byte[] buffer=new byte[8192];int read;
                while((read=in.read(buffer))!=-1){
                    total+=read;
                    if(total>MAX_FILE)throw new Exception("File larger than 8 MB safe limit");
                    for(int i=0;i<read && prefix<2;i++)first[prefix++]=buffer[i];
                    out.write(buffer,0,read);digest.update(buffer,0,read);
                }
            }
            if(total<100 || prefix<2 || (first[0]&255)!=0x1f || (first[1]&255)!=0x8b)
                throw new Exception("Invalid gzip download");
            // Verify the compressed bytes can be decoded and actually contain GEO
            // sample/series annotations. Do not load full expression matrices in RAM.
            boolean annotated=false;
            try(GZIPInputStream gz=new GZIPInputStream(new FileInputStream(pending))){
                byte[] sample=new byte[32768];
                int n=gz.read(sample);
                String beginning=n>0?new String(sample,0,n,java.nio.charset.StandardCharsets.UTF_8):"";
                annotated=beginning.contains("!Series_")||beginning.contains("!Sample_")
                        ||beginning.contains("series_matrix_table_begin");
            }
            if(!annotated)throw new Exception("File lacks recognizable GEO matrix annotations");
            if(target.exists() && !target.delete())throw new Exception("Cannot replace cached file");
            if(!pending.renameTo(target))throw new Exception("Cannot finish file");
            JSONObject record=new JSONObject();
            record.put("accession",study.accession);
            record.put("title",study.title);
            record.put("url",address);
            record.put("repository_page",study.url);
            record.put("file",name);
            record.put("bytes",total);
            record.put("sha256",hex(digest.digest()));
            record.put("retrieved_utc",Instant.now().toString());
            record.put("status","Downloaded authentic repository file, not scientifically validated");
            record.put("license","Check primary study's reuse terms; free download is not a universal open-source license");
            return record;
        }finally{
            connection.disconnect();
            if(pending.exists())pending.delete();
        }
    }
    public static Outcome acquire(Context context,String key,String title,
            PublicDatasetDiscovery.Report studies,String challengeUrl) throws Exception{
        File folder=folder(context,key);
        JSONObject manifest=new JSONObject();
        manifest.put("challenge",title);
        manifest.put("challenge_url",challengeUrl);
        manifest.put("repository","NCBI GEO");
        manifest.put("generated_utc",Instant.now().toString());
        manifest.put("assessment","Data availability and provenance inventory only. No model training, outcome validation, causal inference or scientific solution performed.");
        JSONArray candidates=new JSONArray();
        JSONArray downloads=new JSONArray();
        java.util.Set<String> prior=new java.util.HashSet<>();
        File previous=new File(folder,"manifest.json");
        if(previous.isFile() && previous.length()<=150000){
            try{
                byte[] bytes=new byte[(int)previous.length()];
                try(FileInputStream in=new FileInputStream(previous)){
                    int offset=0;
                    while(offset<bytes.length){
                        int n=in.read(bytes,offset,bytes.length-offset);
                        if(n<0)throw new Exception("Incomplete existing manifest");
                        offset+=n;
                    }
                }
                JSONObject old=new JSONObject(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));
                JSONArray previousCandidates=old.optJSONArray("candidate_studies");
                if(previousCandidates!=null)for(int i=0;i<previousCandidates.length();i++){
                    JSONObject c=previousCandidates.optJSONObject(i);
                    if(c!=null && prior.add("candidate:"+c.optString("accession")))candidates.put(c);
                }
                JSONArray previousFiles=old.optJSONArray("downloaded_files");
                if(previousFiles!=null)for(int i=0;i<previousFiles.length();i++){
                    JSONObject file=previousFiles.optJSONObject(i);
                    if(file==null)continue;
                    String acc=file.optString("accession");
                    String name=file.optString("file");
                    File actual=new File(folder,name);
                    if(acc.matches("GSE[0-9]+")&&name.equals(acc+"_series_matrix.txt.gz")
                        &&actual.isFile()&&actual.length()==file.optLong("bytes")
                        &&prior.add("download:"+acc))downloads.put(file);
                }
            }catch(Exception ignored){/* discard malformed prior manifest */}
        }
        int attempted=0;
        if(studies!=null)for(PublicDatasetDiscovery.Study study:studies.studies){
            JSONObject candidate=new JSONObject();
            candidate.put("accession",study.accession);
            candidate.put("title",study.title);
            candidate.put("source",study.url);
            candidate.put("relevance_score",study.relevance);
            if(prior.add("candidate:"+study.accession))candidates.put(candidate);
            if(prior.contains("download:"+study.accession)
                    || attempted>=MAX_FILES || downloads.length()>=6)continue;
            attempted++;
            try{
                JSONObject file=tryMatrix(folder,study);
                if(prior.add("download:"+study.accession))downloads.put(file);
            }
            catch(Exception problem){
                candidate.put("download_status","Unavailable or skipped: "+problem.getMessage());
            }
        }
        manifest.put("candidate_studies",candidates);
        manifest.put("downloaded_files",downloads);
        manifest.put("downloaded_count",downloads.length());
        manifest.put("required_data_complete",false);
        manifest.put("next_step","Determine which measurements, labels, consent, protocols and study design are adequate before any analysis.");
        File temp=new File(folder,"manifest.json.partial");
        try(FileOutputStream out=new FileOutputStream(temp)){
            out.write(manifest.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        File existing=new File(folder,"manifest.json");
        if(existing.exists()&&!existing.delete())throw new Exception("Cannot replace manifest");
        if(!temp.renameTo(existing))throw new Exception("Cannot save evidence manifest");
        return new Outcome(downloads.length()+" authentic GEO matrix files retained across searches (max "+6
            +"); evidence manifest saved. Completeness and scientific suitability not verified.",downloads.length());
    }
    /** Counts only downloaded files with an original SHA-256 provenance entry. */
    public static int downloadedCount(Context context,String key){
        try{
            File manifest=new File(folder(context,key),"manifest.json");
            if(!manifest.isFile()||manifest.length()>150000)return 0;
            byte[] raw=new byte[(int)manifest.length()];
            try(FileInputStream in=new FileInputStream(manifest)){
                int offset=0;
                while(offset<raw.length){
                    int n=in.read(raw,offset,raw.length-offset);
                    if(n<0)return 0;
                    offset+=n;
                }
            }
            JSONObject doc=new JSONObject(new String(raw,java.nio.charset.StandardCharsets.UTF_8));
            JSONArray entries=doc.optJSONArray("downloaded_files");
            if(entries==null)return 0;
            int valid=0;
            for(int i=0;i<entries.length();i++){
                JSONObject entry=entries.optJSONObject(i);
                if(entry==null)continue;
                String accession=entry.optString("accession");
                String expected=entry.optString("sha256");
                String name=entry.optString("file");
                if(!accession.matches("GSE[0-9]+")||!expected.matches("[a-f0-9]{64}")||
                    !name.equals(accession+"_series_matrix.txt.gz"))continue;
                File actual=new File(folder(context,key),name);
                if(actual.isFile()&&actual.length()==entry.optLong("bytes"))valid++;
            }
            return valid;
        }catch(Exception e){return 0;}
    }

    /** Local audit trail of each bounded planning decision, not a scientific result. */
    public static void recordPlanning(Context context,String key,int pass,
                                      String query,int before,int after,String outcome){
        try{
            File directory=folder(context,key);
            File audit=new File(directory,"planning-log.jsonl");
            if(audit.length()>120000)return;
            JSONObject event=new JSONObject();
            event.put("utc",Instant.now().toString());
            event.put("pass",pass);
            event.put("query",query==null?"":query.substring(0,Math.min(query.length(),95)));
            event.put("files_before",before);
            event.put("files_after",after);
            event.put("outcome",outcome==null?"":outcome.substring(0,Math.min(300,outcome.length())));
            try(FileOutputStream out=new FileOutputStream(audit,true)){
                out.write((event.toString()+"\\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }catch(Exception ignored){/* audit never changes decision to fabricate results */}
    }

    public static String summary(Context context,String key){
        try{
            File manifest=new File(folder(context,key),"manifest.json");
            if(!manifest.isFile())return "No downloaded datasets or verified evidence package.";
            byte[] bytes=new byte[(int)Math.min(manifest.length(),100000)];
            try(FileInputStream in=new FileInputStream(manifest)){
                int n=in.read(bytes);
                if(n<=0)return "Empty evidence manifest";
                JSONObject j=new JSONObject(new String(bytes,0,n,java.nio.charset.StandardCharsets.UTF_8));
                return "Local real GEO files downloaded: "+j.optInt("downloaded_count")
                    +". Source URLs, checksums and timestamps preserved in manifest.json. No scientific analysis performed.";
            }
        }catch(Exception e){return "Evidence manifest unavailable: "+e.getMessage();}
    }
    private static void put(ZipOutputStream zip,String name,InputStream in) throws Exception{
        zip.putNextEntry(new ZipEntry(name));
        byte[] b=new byte[8192];int n;
        while((n=in.read(b))!=-1)zip.write(b,0,n);
        zip.closeEntry();
    }
    public static void export(Context context,String key,Uri destination,
                              AiPreferences.SavedResult result) throws Exception{
        File directory=folder(context,key);
        File manifest=new File(directory,"manifest.json");
        if(!manifest.isFile())throw new Exception("No downloaded evidence manifest yet");
        try(OutputStream out=context.getContentResolver().openOutputStream(destination,"wt")){
            if(out==null)throw new Exception("Cannot write evidence ZIP");
            try(ZipOutputStream zip=new ZipOutputStream(out)){
                try(InputStream in=new FileInputStream(manifest)){put(zip,"evidence/manifest.json",in);}
                zip.putNextEntry(new ZipEntry("evidence/readiness.txt"));
                zip.write(("BIOQRA dataset readiness assessment\\n"
                    +ResearchReadiness.assess(context,key,null).describe()
                    +"\\nDataset discovery and downloading are not equivalent to analyzing data. "
                    +"This archive contains no independently validated scientific results.\\n")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
                File qc=new File(directory,"analysis-qc.json");
                if(qc.isFile()&&qc.length()<=500000)
                    try(InputStream in=new FileInputStream(qc)){
                        put(zip,"research/analysis-qc.json",in);
                    }
                File comparison=new File(directory,"group-comparison.json");
                if(comparison.isFile()&&comparison.length()<=500000)
                    try(InputStream in=new FileInputStream(comparison)){
                        put(zip,"research/group-comparison.json",in);
                    }
                File audit=new File(directory,"planning-log.jsonl");
                if(audit.isFile() && audit.length()<=120000)
                    try(InputStream in=new FileInputStream(audit)){
                        put(zip,"evidence/planning-log.jsonl",in);
                    }
                File[] files=directory.listFiles();
                if(files!=null)for(File file:files){
                    if(file.isFile()&&file.getName().matches("GSE[0-9]+_series_matrix[.]txt[.]gz")){
                        try(InputStream in=new FileInputStream(file)){put(zip,"evidence/data/"+file.getName(),in);}
                    }
                }
                if(result!=null){
                    zip.putNextEntry(new ZipEntry("research/proposal.pdf"));
                    zip.write(ArchiveFiles.generatedPdf(result));
                    zip.closeEntry();
                    zip.putNextEntry(new ZipEntry("research/proposal.md"));
                    zip.write(result.answer.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
                zip.putNextEntry(new ZipEntry("README.txt"));
                zip.write(("BIOQRA evidence package\\nGEO data files are independent public research files. "
                    +"Do not assume the selected files are sufficient or suitable for this challenge. "
                    +"Review study terms and metadata. No executable analysis is included.\\n")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }
}
