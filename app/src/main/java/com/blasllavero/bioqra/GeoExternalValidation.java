package com.blasllavero.bioqra;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Frozen-feature, external-study exploratory replication.
 * Selection takes place only in discovery GEO study. The same probe IDs
 * are directly tested against real numeric values of a disjoint study.
 * This is not clinical validation or proof of independent donors.
 */
public final class GeoExternalValidation {
    private GeoExternalValidation(){}

    public static final class Result {
        public final int attempted,eligible,replicated;
        public final String summary;
        Result(int attempted,int eligible,int replicated,String summary){
            this.attempted=attempted;this.eligible=eligible;
            this.replicated=replicated;this.summary=summary;
        }
    }
    private static JSONObject load(File file)throws Exception{
        if(!file.isFile()||file.length()>750000)
            throw new Exception("Exploratory comparison report unavailable");
        byte[] bytes=new byte[(int)file.length()];
        try(FileInputStream in=new FileInputStream(file)){
            int pos=0;
            while(pos<bytes.length){
                int n=in.read(bytes,pos,bytes.length-pos);
                if(n<0)throw new Exception("Truncated comparison report");
                pos+=n;
            }
        }
        return new JSONObject(new String(bytes,StandardCharsets.UTF_8));
    }
    private static Set<String> ids(JSONObject study){
        Set<String> result=new HashSet<>();
        JSONArray array=study.optJSONArray("sample_identifiers");
        if(array!=null)for(int i=0;i<array.length();i++){
            String id=array.optString(i,"");
            if(id.matches("GSM[0-9]+"))result.add(id);
        }
        return result;
    }
    private static Set<String> frozen(JSONObject discovery){
        Set<String> result=new HashSet<>();
        JSONArray top=discovery.optJSONArray("top_100_features");
        if(top!=null)for(int i=0;i<top.length();i++){
            JSONObject feature=top.optJSONObject(i);
            if(feature==null)continue;
            String id=feature.optString("feature_id","");
            if(id.matches("[A-Za-z0-9_.-]{1,100}") &&
                    feature.optDouble("bh_fdr",1)<0.05)result.add(id);
        }
        return result;
    }
    private static JSONObject record(JSONObject discovery,JSONObject external,String status,
                                      String reason)throws Exception{
        JSONObject result=new JSONObject();
        result.put("discovery_accession",discovery.optString("accession"));
        result.put("external_accession",external.optString("accession"));
        result.put("shared_platform",discovery.optString("platform_accession"));
        result.put("status",status);
        result.put("reason",reason);
        return result;
    }
    public static Result evaluate(Context context,String key)throws Exception{
        if(key==null || !key.matches("[a-f0-9]{32}"))
            throw new Exception("Invalid challenge key");
        File dir=new File(new File(context.getFilesDir(),"research-evidence"),key);
        JSONObject discoveryReport=load(new File(dir,"group-comparison.json"));
        JSONArray studies=discoveryReport.optJSONArray("results");
        if(studies==null)studies=new JSONArray();
        JSONArray outcomes=new JSONArray();
        int attempted=0,eligible=0,replicated=0;
        // Discovery study is chosen deterministically as the lowest GEO
        // accession with adequate discovery hits; never choose based on the
        // external cohort's observed results.
        JSONObject discovery=null;
        for(int i=0;i<studies.length();i++){
            JSONObject candidate=studies.optJSONObject(i);
            if(candidate==null||frozen(candidate).size()<5)continue;
            if(discovery==null || candidate.optString("accession")
                    .compareTo(discovery.optString("accession"))<0)
                discovery=candidate;
        }
        if(discovery!=null){
            Set<String> selected=frozen(discovery);
            Set<String> firstIds=ids(discovery);
            for(int i=0;i<studies.length()&&attempted<5;i++){
                JSONObject validation=studies.optJSONObject(i);
                if(validation==null || validation==discovery)continue;
                attempted++;
                String acc=validation.optString("accession");
                if(!discovery.optString("platform_accession").equals(
                        validation.optString("platform_accession"))){
                    outcomes.put(record(discovery,validation,"SKIPPED","Different measurement platform"));
                    continue;
                }
                Set<String> secondIds=ids(validation);
                if(firstIds.isEmpty()||secondIds.isEmpty()){
                    outcomes.put(record(discovery,validation,"SKIPPED","Missing independent GSM accession evidence"));
                    continue;
                }
                Set<String> overlap=new HashSet<>(firstIds);
                overlap.retainAll(secondIds);
                if(!overlap.isEmpty()){
                    outcomes.put(record(discovery,validation,"SKIPPED","GSM samples overlap between studies"));
                    continue;
                }
                try{
                    JSONObject retested=GeoGroupStatistics.retest(context,key,acc,selected);
                    JSONArray measured=retested.optJSONArray("top_100_features");
                    if(measured==null||measured.length()==0)
                        throw new Exception("No frozen probes evaluated");
                    int supported=0,matching=0;
                    // Sign and adjusted p-value are determined in the held-out
                    // external study; its other probes were not screened.
                    JSONArray discoveries=discovery.optJSONArray("top_100_features");
                    java.util.Map<String,Double> directions=new java.util.HashMap<>();
                    if(discoveries!=null)for(int d=0;d<discoveries.length();d++){
                        JSONObject v=discoveries.optJSONObject(d);
                        if(v!=null&&selected.contains(v.optString("feature_id")))
                            directions.put(v.optString("feature_id"),
                                v.optDouble("mean_difference_log2"));
                    }
                    JSONArray matches=new JSONArray();
                    for(int j=0;j<measured.length();j++){
                        JSONObject m=measured.optJSONObject(j);
                        if(m==null)continue;
                        Double expected=directions.get(m.optString("feature_id"));
                        if(expected==null)continue;
                        double external=m.optDouble("mean_difference_log2",Double.NaN);
                        if(!Double.isFinite(external))continue;
                        if(Math.signum(expected)==Math.signum(external)){
                            matching++;
                            if(m.optDouble("bh_fdr",1)<0.05){
                                supported++;
                                JSONObject observation=new JSONObject();
                                observation.put("feature_id",m.optString("feature_id"));
                                observation.put("external_p",m.optDouble("welch_p",1));
                                observation.put("external_bh_fdr",m.optDouble("bh_fdr",1));
                                observation.put("external_effect",external);
                                matches.put(observation);
                            }
                        }
                    }
                    JSONObject outcome=record(discovery,validation,
                        "EXPLORATORY_EXTERNAL_RETEST",
                        "Frozen probe set tested on nonoverlapping GSM sample IDs with same GPL; donor independence and confounding still unverified");
                    outcome.put("prespecified_features",selected.size());
                    outcome.put("features_evaluable",retested.optInt("features_tested"));
                    outcome.put("matching_effect_directions",matching);
                    outcome.put("same_direction_external_fdr_below_0_05",supported);
                    outcome.put("candidate_results",matches);
                    outcome.put("statistically_replicated_in_exploratory_test",supported>0);
                    outcome.put("clinically_validated",false);
                    outcomes.put(outcome);
                    eligible++;
                    if(supported>0)replicated++;
                }catch(Exception ex){
                    outcomes.put(record(discovery,validation,"SKIPPED",
                        "External matrix or preprocessing not suitable: "+ex.getMessage()));
                }
            }
        }
        JSONObject report=new JSONObject();
        report.put("generated_utc",Instant.now().toString());
        report.put("workflow","Discovery cohort freezes FDR-qualified top probe IDs; independent GEO Series retested using original matrices");
        report.put("discovery_accession",discovery==null?"NONE":discovery.optString("accession"));
        report.put("cohort_pairs_attempted",attempted);
        report.put("external_studies_tested",eligible);
        report.put("external_studies_with_exploratory_support",replicated);
        report.put("pairs",outcomes);
        report.put("clinical_validation",false);
        report.put("challenge_solved",false);
        report.put("limitations","Exploratory, data-dependent discovery selection, unadjusted Welch tests, incomplete annotations, unverified donor independence, no matched genes or preregistered external criteria. Biological significance and generalizability are not established.");
        if(discovery==null)report.put("status","No discovery study has five unambiguous FDR-positive probes");
        File tmp=new File(dir,"external-validation.json.partial");
        try(FileOutputStream out=new FileOutputStream(tmp)){
            out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        File finished=new File(dir,"external-validation.json");
        if(finished.exists()&&!finished.delete())throw new Exception("Could not replace external validation report");
        if(!tmp.renameTo(finished))throw new Exception("Could not finalize external validation report");
        return new Result(attempted,eligible,replicated,
            "External exploratory retest: "+eligible+" independently listed study/studies evaluated; "
            +replicated+" showed one or more same-direction probes with external BH FDR below 0.05. "
            +"This does not establish scientific or clinical validation.");
    }
}
