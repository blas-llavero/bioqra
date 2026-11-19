package com.blasllavero.bioqra;

import android.content.Context;
import org.apache.commons.math3.distribution.TDistribution;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/**
 * Optional, conservative exploratory case-vs-control comparison of real GEO
 * series matrices. Only normalized log2 measurement matrices with clear,
 * complete study-provided sample labels and >=3 samples per group qualify.
 * No implicit sample label generation, no RNA count inference, no clinical claims.
 */
public final class GeoGroupStatistics {
    private GeoGroupStatistics(){}
    private static final int MAX_FEATURES=50000;
    private static final long MAX_CELLS=2500000;
    private static final class Feature {
        final String id;
        final double effect,p;
        double adjusted;
        Feature(String id,double effect,double p){this.id=id;this.effect=effect;this.p=p;}
    }
    public static final class Result {
        public final int tested;
        public final String summary;
        Result(int tested,String summary){this.tested=tested;this.summary=summary;}
    }
    private static String clean(String s){return s.replace("\"","").trim();}
    private static String sha(File file)throws Exception{
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        try(FileInputStream in=new FileInputStream(file)){
            byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1)md.update(buffer,0,n);
        }
        StringBuilder hex=new StringBuilder();
        for(byte b:md.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));
        return hex.toString();
    }
    private static int classify(String text){
        String s=clean(text).toLowerCase(Locale.ROOT);
        // Only explicit, isolated phenotype/status labels, not free-text titles.
        if(!s.matches("(?:disease state|disease status|condition|tissue type|phenotype|group|sample type|histology):\\s*.*"))
            return 0;
        String value=s.substring(s.indexOf(':')+1).trim();
        if(value.matches("(normal|healthy|non-tumou?r|non-neoplastic|control|adjacent normal|normal tissue|healthy control)"))
            return -1;
        if(value.matches("(tumou?r|cancer|carcinoma|primary tumou?r|tumou?r tissue|malignant|adenocarcinoma|case)"))
            return 1;
        return 0;
    }
    private static JSONObject runOne(File file,JSONObject entry) throws Exception{
        return runOne(file,entry,null);
    }
    /** Apply candidates chosen from a separate study to a new, untouched matrix. */
    private static JSONObject runOne(File file,JSONObject entry,
                                      java.util.Set<String> preselected) throws Exception{
        String acc=entry.getString("accession");
        JSONObject result=new JSONObject();
        result.put("accession",acc);
        result.put("source_sha256",entry.getString("sha256"));
        String processing="";
        int[] groups=null;int controls=0,cases=0,featureRows=0;
        java.util.List<String> sampleAccessions=new java.util.ArrayList<>();
        boolean metadataConflict=false;
        boolean complete=false,header=false,skipped=false;
        String platformId=null;
        boolean platformConflict=false;
        List<Feature> features=new ArrayList<>();
        long cells=0;
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(file)),StandardCharsets.UTF_8))){
            String line;
            while((line=reader.readLine())!=null){
                if(line.startsWith("!Series_data_processing")||line.startsWith("!Sample_data_processing")){
                    if(processing.length()<8000)processing+=line.toLowerCase(Locale.ROOT)+" ";
                }
                if((line.startsWith("!Series_platform_id") ||
                        line.startsWith("!Sample_platform_id")) && !header){
                    String[] platforms=line.split("\t",-1);
                    for(int i=1;i<platforms.length;i++){
                        String id=clean(platforms[i]);
                        if(!id.matches("GPL[0-9]+")){platformConflict=true;break;}
                        if(platformId!=null && !platformId.equals(id))platformConflict=true;
                        platformId=id;
                    }
                }
                if(line.startsWith("!Sample_geo_accession") && !header){
                    if(!sampleAccessions.isEmpty())throw new Exception("Duplicate GEO sample accession annotations");
                    String[] ids=line.split("\t",-1);
                    if(ids.length<3||ids.length>501)throw new Exception("Invalid GSM sample identifiers");
                    java.util.Set<String> unique=new java.util.HashSet<>();
                    for(int i=1;i<ids.length;i++){
                        String id=clean(ids[i]);
                        if(!id.matches("GSM[0-9]+")||!unique.add(id))
                            throw new Exception("Missing or duplicate GSM sample accession");
                        sampleAccessions.add(id);
                    }
                }
                if(line.startsWith("!Sample_characteristics_ch1") && !header){
                    String lower=line.toLowerCase(Locale.ROOT);
                    if(lower.contains("batch:")||lower.contains("batch id:")||
                       lower.contains("patient id:")||lower.contains("patient_id:")||
                       lower.contains("subject id:")||lower.contains("subject_id:")||
                       lower.contains("donor id:")||lower.contains("donor_id:")||
                       lower.contains("pair id:")||lower.contains("paired:")||
                       lower.contains("treatment:")||lower.contains("timepoint:")||
                       lower.contains("time point:"))metadataConflict=true;

                    String[] parts=line.split("\t",-1);
                    if(parts.length<2||parts.length>501)continue;
                    if(groups==null)groups=new int[parts.length-1];
                    if(groups.length!=parts.length-1){skipped=true;break;}
                    for(int i=1;i<parts.length;i++){
                        int label=classify(parts[i]);
                        if(label!=0){
                            if(groups[i-1]!=0 && groups[i-1]!=label){skipped=true;break;}
                            groups[i-1]=label;
                        }
                    }
                    if(skipped)break;
                }
                if(line.equals("!series_matrix_table_begin")){
                    header=true;break;
                }
            }
            if(!header||skipped)throw new Exception("Missing table or inconsistent group metadata");
            if(groups==null)throw new Exception("No explicit sample phenotype annotations");
            if(platformId==null || platformConflict)
                throw new Exception("No single consistent GEO measurement platform identified");
            if(sampleAccessions.size()!=groups.length)
                throw new Exception("Missing or inconsistent GEO sample identifiers");
            if(metadataConflict)
                throw new Exception("Pairing, batch, treatment, donor or timepoint metadata: unadjusted independent Welch test blocked");

            for(int group:groups){if(group==-1)controls++;else if(group==1)cases++;}
            if(controls<3||cases<3||controls+cases!=groups.length)
                throw new Exception("Requires at least 3 unambiguous, annotated samples in each group");
            String p=processing.toLowerCase(Locale.ROOT);
            if(!(p.contains("log2")||p.contains("log 2")||p.contains("log-transformed")||p.contains("log transformed"))||
                !(p.contains("normaliz")||p.contains("quantile")||p.contains("rma"))||
                p.contains("raw counts")||p.contains("unnormalized"))
                throw new Exception("Matrix lacks explicit log-transformed and normalized processing evidence");
            String tableHeader=reader.readLine();
            if(tableHeader==null || tableHeader.split("\t",-1).length!=groups.length+1)
                throw new Exception("Sample column count differs from phenotype annotations");
            String[] matrixHeader=tableHeader.split("\t",-1);
            if(!clean(matrixHeader[0]).equalsIgnoreCase("ID_REF"))
                throw new Exception("Unexpected matrix identifier column");
            for(int i=0;i<groups.length;i++)
                if(!clean(matrixHeader[i+1]).equals(sampleAccessions.get(i)))
                    throw new Exception("GEO matrix sample order does not match phenotype annotations");

            while((line=reader.readLine())!=null){
                if(line.equals("!series_matrix_table_end")){complete=true;break;}
                if(featureRows>=MAX_FEATURES || cells+groups.length>MAX_CELLS){skipped=true;break;}
                String[] vals=line.split("\t",-1);
                if(vals.length!=groups.length+1)continue;
                // Study A selects candidate probes; in study B evaluate exactly
                // those prespecified IDs, never reselect its top-ranked features.
                if(preselected!=null && !preselected.contains(clean(vals[0])))continue;
                featureRows++;cells+=groups.length;
                double[] controlsData=new double[controls];
                double[] casesData=new double[cases];
                int cc=0,tc=0;
                boolean valid=true;
                for(int i=0;i<groups.length;i++){
                    try{
                        double value=Double.parseDouble(clean(vals[i+1]));
                        if(!Double.isFinite(value)){valid=false;break;}
                        if(groups[i]==-1)controlsData[cc++]=value;
                        else casesData[tc++]=value;
                    }catch(NumberFormatException e){valid=false;break;}
                }
                if(!valid||cc<3||tc<3)continue;
                try{
                    WelchStatistics.TestResult t=WelchStatistics.compare(controlsData,casesData);
                    features.add(new Feature(clean(vals[0]),t.effect,t.p));
                }catch(IllegalArgumentException ignored){ /* incomplete feature */ }
            }
        }
        if(!complete||skipped)throw new Exception("Matrix incomplete or exceeds bounded computational limits");
        if(preselected==null && features.size()<10)
            throw new Exception("Too few analyzable features");
        if(preselected!=null && features.isEmpty())
            throw new Exception("No prespecified discovery probes evaluable on external matrix");
        features.sort(Comparator.comparingDouble(f->f.p));
        final int m=features.size();
        double[] pValues=new double[m];
        for(int i=0;i<m;i++)pValues[i]=features.get(i).p;
        double[] adjusted=WelchStatistics.benjaminiHochberg(pValues);
        for(int i=0;i<m;i++)features.get(i).adjusted=adjusted[i];
        int passed=0;for(Feature f:features)if(f.adjusted<0.05)passed++;
        JSONArray top=new JSONArray();
        for(int i=0;i<Math.min(100,features.size());i++){
            Feature f=features.get(i);
            JSONObject item=new JSONObject();
            item.put("feature_id",f.id);
            item.put("mean_difference_log2",f.effect);
            item.put("welch_p",f.p);
            item.put("bh_fdr",f.adjusted);
            top.put(item);
        }
        result.put("method","Exploratory Welch t-test of normalized log-scale data; Benjamini-Hochberg FDR; no covariates or batch adjustment");
        result.put("platform_accession",platformId);
        result.put("sample_accessions_verified_in_order",true);
        result.put("explicit_pairing_or_batch_metadata",false);
        result.put("sample_identifiers",new JSONArray(sampleAccessions));
        result.put("measurement_type","Reported log-scale normalized measurement; cannot assume gene-level mapping or platform comparability");
        result.put("case_samples",cases);result.put("control_samples",controls);
        result.put("features_tested",m);
        result.put("features_fdr_below_0_05",passed);
        result.put("top_100_features",top);
        result.put("feature_selection",preselected==null
            ?"Within-study exploratory feature ranking"
            :"Fixed from independent discovery study; no validation-set feature selection");
        if(preselected!=null){
            result.put("prespecified_candidates_requested",preselected.size());
            result.put("prespecified_candidates_evaluated",features.size());
        }
        result.put("limitations","Automatic labels from explicit GEO characteristics only. No batch adjustment, pairing model, biological replicate verification, clinical evaluation or study-specific preprocessing validation. Statistical associations must not be interpreted as biomarkers or treatment effects.");
        result.put("scientifically_validated",false);
        return result;
    }
    /**
     * Re-test a frozen set of discovery-selected probe IDs on real validation
     * study data. The same conservative sample/processing checks run again.
     */
    public static JSONObject retest(Context context,String key,String accession,
                                    java.util.Set<String> candidateProbes) throws Exception{
        if(key==null||!key.matches("[a-f0-9]{32}")||
                accession==null||!accession.matches("GSE[0-9]+")||
                candidateProbes==null||candidateProbes.isEmpty()||
                candidateProbes.size()>100)
            throw new Exception("Invalid frozen external validation request");
        File directory=new File(new File(context.getFilesDir(),"research-evidence"),key);
        File manifest=new File(directory,"manifest.json");
        if(!manifest.isFile()||manifest.length()>150000)
            throw new Exception("Evidence manifest unavailable");
        byte[] bytes=new byte[(int)manifest.length()];
        try(FileInputStream in=new FileInputStream(manifest)){
            int off=0;
            while(off<bytes.length){
                int n=in.read(bytes,off,bytes.length-off);
                if(n<0)throw new Exception("Incomplete provenance manifest");
                off+=n;
            }
        }
        JSONArray source=new JSONObject(new String(bytes,StandardCharsets.UTF_8))
            .optJSONArray("downloaded_files");
        if(source==null)throw new Exception("No study downloads available");
        for(int i=0;i<source.length();i++){
            JSONObject entry=source.optJSONObject(i);
            if(entry==null||!accession.equals(entry.optString("accession")))continue;
            String name=accession+"_series_matrix.txt.gz";
            File file=new File(directory,name);
            if(!name.equals(entry.optString("file"))||!file.isFile()||
                    file.length()!=entry.optLong("bytes")||
                    !sha(file).equals(entry.optString("sha256")))
                throw new Exception("External study file provenance invalid");
            return runOne(file,entry,candidateProbes);
        }
        throw new Exception("External validation accession absent from manifest");
    }

    public static Result analyze(Context context,String key) throws Exception{
        if(key==null||!key.matches("[a-f0-9]{32}"))throw new Exception("Invalid challenge key");
        File dir=new File(new File(context.getFilesDir(),"research-evidence"),key);
        File manifest=new File(dir,"manifest.json");
        if(!manifest.isFile()||manifest.length()>150000)throw new Exception("No valid manifest");
        byte[] raw=new byte[(int)manifest.length()];
        try(FileInputStream in=new FileInputStream(manifest)){
            int offset=0;
            while(offset<raw.length){
                int n=in.read(raw,offset,raw.length-offset);
                if(n<0)throw new Exception("Truncated manifest");
                offset+=n;
            }
        }
        JSONArray entries=new JSONObject(new String(raw,StandardCharsets.UTF_8))
                .optJSONArray("downloaded_files");
        JSONArray results=new JSONArray(),rejections=new JSONArray();
        int files=0;
        if(entries!=null)for(int i=0;i<entries.length()&&files<6;i++){
            JSONObject entry=entries.optJSONObject(i);
            if(entry==null)continue;
            String acc=entry.optString("accession");
            if(!acc.matches("GSE[0-9]+"))continue;
            String name=acc+"_series_matrix.txt.gz";
            File file=new File(dir,name);
            files++;
            try{
                if(!name.equals(entry.optString("file"))||!file.isFile()||
                        file.length()!=entry.optLong("bytes")||
                        !sha(file).equals(entry.optString("sha256")))
                    throw new Exception("File missing or checksum mismatch");
                results.put(runOne(file,entry));
            }catch(Exception e){
                JSONObject rejected=new JSONObject();
                rejected.put("accession",acc);
                rejected.put("reason",e.getMessage());
                rejections.put(rejected);
            }
        }
        JSONObject report=new JSONObject();
        report.put("analysis_type","Exploratory independent-sample comparison, not biological validation");
        report.put("generated_utc",Instant.now().toString());
        report.put("method","Welch two-sided t-test and Benjamini-Hochberg correction, only where explicit unambiguous phenotypes and logged normalized data are documented");
        report.put("files_attempted",files);report.put("eligible_studies",results.length());
        report.put("results",results);report.put("skipped_studies",rejections);
        report.put("challenge_solved",false);
        report.put("needs_independent_validation",true);
        File tmp=new File(dir,"group-comparison.json.partial");
        try(FileOutputStream out=new FileOutputStream(tmp)){
            out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        File finalFile=new File(dir,"group-comparison.json");
        if(finalFile.exists()&&!finalFile.delete())throw new Exception("Cannot update comparison");
        if(!tmp.renameTo(finalFile))throw new Exception("Cannot finish comparison");
        return new Result(results.length(),"Exploratory comparison: "+results.length()+
                " eligible study/studies; "+rejections.length()+
                " skipped for missing metadata, processing evidence or other safety checks. "+
                "Statistical results are exploratory; challenge not validated.");
    }
}
