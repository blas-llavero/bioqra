package com.blasllavero.bioqra;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Audit of cross-study concordance for independent GEO Series using the exact
 * same GPL probe identifiers. NOT independent scientific validation: top-100
 * selection, cohort heterogeneity, lack of biological replicate checks,
 * unknown covariates and label ambiguity preclude generalizable claims.
 */
public final class GeoCohortConcordance {
    private GeoCohortConcordance(){}
    public static final class Result {
        public final String summary;
        public final int eligiblePairs;
        Result(String summary,int eligiblePairs){this.summary=summary;this.eligiblePairs=eligiblePairs;}
    }
    private static JSONObject read(File f) throws Exception{
        if(!f.isFile()||f.length()>600000)
            throw new Exception("Bounded group comparison results not available");
        byte[] raw=new byte[(int)f.length()];
        try(FileInputStream in=new FileInputStream(f)){
            int offset=0;
            while(offset<raw.length){
                int n=in.read(raw,offset,raw.length-offset);
                if(n<0)throw new Exception("Comparison report truncated");
                offset+=n;
            }
        }
        return new JSONObject(new String(raw,StandardCharsets.UTF_8));
    }
    private static Map<String,Double> top(JSONObject study){
        Map<String,Double> selected=new HashMap<>();
        JSONArray features=study.optJSONArray("top_100_features");
        if(features!=null)for(int k=0;k<features.length();k++){
            JSONObject item=features.optJSONObject(k);
            if(item==null||item.optDouble("bh_fdr",1)>=0.05)continue;
            String id=item.optString("feature_id","");
            double effect=item.optDouble("mean_difference_log2",Double.NaN);
            if(id.matches("[A-Za-z0-9_.-]{1,100}") && Double.isFinite(effect)
                    && Math.abs(effect)>1e-12)selected.put(id,effect);
        }
        return selected;
    }
    public static Result inspect(Context context,String key) throws Exception{
        if(key==null||!key.matches("[a-f0-9]{32}"))throw new Exception("Invalid challenge key");
        File directory=new File(new File(context.getFilesDir(),"research-evidence"),key);
        JSONObject prior=read(new File(directory,"group-comparison.json"));
        JSONArray studies=prior.optJSONArray("results");
        if(studies==null)studies=new JSONArray();
        JSONArray pairs=new JSONArray();
        int samePlatformPairs=0,concordantPairs=0;
        for(int i=0;i<studies.length();i++){
            JSONObject a=studies.optJSONObject(i);
            if(a==null)continue;
            String aAcc=a.optString("accession"),platform=a.optString("platform_accession");
            if(!aAcc.matches("GSE[0-9]+")||!platform.matches("GPL[0-9]+"))continue;
            for(int j=i+1;j<studies.length();j++){
                JSONObject b=studies.optJSONObject(j);
                if(b==null||aAcc.equals(b.optString("accession"))||
                        !platform.equals(b.optString("platform_accession")))continue;
                Set<String> first=new HashSet<>(),second=new HashSet<>();
                JSONArray idsA=a.optJSONArray("sample_identifiers"),idsB=b.optJSONArray("sample_identifiers");
                if(idsA==null||idsB==null)continue;
                for(int k=0;k<idsA.length();k++)first.add(idsA.optString(k));
                for(int k=0;k<idsB.length();k++)second.add(idsB.optString(k));
                Set<String> overlap=new HashSet<>(first);overlap.retainAll(second);
                if(!overlap.isEmpty())continue; // Not independent samples
                Map<String,Double> hitsA=top(a),hitsB=top(b);
                int intersection=0,sameDirection=0;
                for(Map.Entry<String,Double> hit:hitsA.entrySet()){
                    Double other=hitsB.get(hit.getKey());
                    if(other==null)continue;
                    intersection++;
                    if(Math.signum(hit.getValue())==Math.signum(other))sameDirection++;
                }
                JSONObject pair=new JSONObject();
                pair.put("study_a",aAcc);pair.put("study_b",b.optString("accession"));
                pair.put("shared_platform",platform);
                pair.put("overlapping_gsm_samples",false);
                pair.put("significant_top100_common_probe_ids",intersection);
                pair.put("matching_effect_directions",sameDirection);
                pair.put("discordant_effect_directions",intersection-sameDirection);
                pair.put("interpretation","Descriptive top-hit direction agreement only; no independent validation, matched gene identifiers, meta-analysis or clinical confirmation.");
                pairs.put(pair);
                samePlatformPairs++;
                if(intersection>0 && sameDirection>0)concordantPairs++;
            }
        }
        JSONObject audit=new JSONObject();
        audit.put("generated_utc",Instant.now().toString());
        audit.put("analysis_type","Cross-study exact-GPL probe-level top-hit concordance audit");
        audit.put("eligible_same_platform_independent_pairs",samePlatformPairs);
        audit.put("pairs_with_any_direction_agreement",concordantPairs);
        audit.put("pairs",pairs);
        audit.put("not_validation",true);
        audit.put("challenge_solved",false);
        audit.put("limitations","Same GPL and no shared GSM are minimum conditions, not proof of independent cohorts or study comparability. Only top-100 significant features are available; this introduces selection bias. Studies may differ in processing, annotation, tumor definition, covariates or platform batch. A preregistered external-cohort analysis is still required.");
        File pending=new File(directory,"cohort-concordance.json.partial");
        try(FileOutputStream out=new FileOutputStream(pending)){
            out.write(audit.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        File destination=new File(directory,"cohort-concordance.json");
        if(destination.exists()&&!destination.delete())throw new Exception("Cannot update concordance report");
        if(!pending.renameTo(destination))throw new Exception("Cannot finalize concordance report");
        return new Result("Independent GEO study pairs sharing exact GPL platform: "
            +samePlatformPairs+"; pairs with any agreeing top-hit probe directions: "
            +concordantPairs+". Descriptive cross-study audit only; NOT validated replication.",
            samePlatformPairs);
    }
}
