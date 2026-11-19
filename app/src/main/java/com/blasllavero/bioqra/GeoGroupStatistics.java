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
        String acc=entry.getString("accession");
        JSONObject result=new JSONObject();
        result.put("accession",acc);
        result.put("source_sha256",entry.getString("sha256"));
        String processing="";
        int[] groups=null;int controls=0,cases=0,featureRows=0;
        boolean complete=false,header=false,skipped=false;
        List<Feature> features=new ArrayList<>();
        long cells=0;
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(file)),StandardCharsets.UTF_8))){
            String line;
            while((line=reader.readLine())!=null){
                if(line.startsWith("!Series_data_processing")||line.startsWith("!Sample_data_processing")){
                    if(processing.length()<8000)processing+=line.toLowerCase(Locale.ROOT)+" ";
                }
                if(line.startsWith("!Sample_characteristics_ch1") && !header){
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
            while((line=reader.readLine())!=null){
                if(line.equals("!series_matrix_table_end")){complete=true;break;}
                if(featureRows>=MAX_FEATURES || cells+groups.length>MAX_CELLS){skipped=true;break;}
                String[] vals=line.split("\t",-1);
                if(vals.length!=groups.length+1)continue;
                featureRows++;cells+=groups.length;
                double[] sum=new double[2], sq=new double[2];int[] n=new int[2];
                boolean valid=true;
                for(int i=0;i<groups.length;i++){
                    try{
                        double value=Double.parseDouble(clean(vals[i+1]));
                        if(!Double.isFinite(value)){valid=false;break;}
                        int k=groups[i]==-1?0:1;
                        sum[k]+=value;sq[k]+=value*value;n[k]++;
                    }catch(NumberFormatException e){valid=false;break;}
                }
                if(!valid||n[0]<3||n[1]<3)continue;
                double mean0=sum[0]/n[0],mean1=sum[1]/n[1];
                double var0=Math.max(0,(sq[0]-sum[0]*sum[0]/n[0])/(n[0]-1));
                double var1=Math.max(0,(sq[1]-sum[1]*sum[1]/n[1])/(n[1]-1));
                double a=var0/n[0],b=var1/n[1],den=a+b;
                if(den<=1e-15)continue;
                double df=den*den/(a*a/(n[0]-1)+b*b/(n[1]-1));
                if(!(df>=1)||!Double.isFinite(df))continue;
                double statistic=(mean1-mean0)/Math.sqrt(den);
                double pv=2*(new TDistribution(df)).cumulativeProbability(-Math.abs(statistic));
                if(Double.isFinite(pv))features.add(new Feature(clean(vals[0]),mean1-mean0,Math.min(1,Math.max(0,pv))));
            }
        }
        if(!complete||skipped)throw new Exception("Matrix incomplete or exceeds bounded computational limits");
        if(features.size()<10)throw new Exception("Too few analyzable features");
        features.sort(Comparator.comparingDouble(f->f.p));
        final int m=features.size();
        double previous=1;
        for(int i=m-1;i>=0;i--){
            Feature f=features.get(i);
            previous=Math.min(previous,Math.min(1,f.p*m/(i+1.0)));
            f.adjusted=previous;
        }
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
        result.put("case_samples",cases);result.put("control_samples",controls);
        result.put("features_tested",m);
        result.put("features_fdr_below_0_05",passed);
        result.put("top_100_features",top);
        result.put("limitations","Automatic labels from explicit GEO characteristics only. No batch adjustment, pairing model, biological replicate verification, clinical evaluation or study-specific preprocessing validation. Statistical associations must not be interpreted as biomarkers or treatment effects.");
        result.put("scientifically_validated",false);
        return result;
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
