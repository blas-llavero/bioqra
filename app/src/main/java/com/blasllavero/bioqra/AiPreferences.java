package com.blasllavero.bioqra;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * On-device AI preferences; no credentials or medical records are persisted here.
 * The model slug is resolved against the CURRENT account's server model catalog.
 */
public final class AiPreferences {
    public static final String AUTO="__bioqra_auto__";
    private final SharedPreferences prefs;

    public AiPreferences(Context context){
        prefs=context.getApplicationContext().getSharedPreferences("bioqra_ai_preferences_v1",Context.MODE_PRIVATE);
    }

    public static String stableKey(String sourceId,String title){
        String raw=(sourceId==null||sourceId.isEmpty())?title:sourceId;
        if(raw==null)raw="unknown";
        try {
            byte[] data=MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb=new StringBuilder();
            for(int i=0;i<16;i++)sb.append(String.format(Locale.ROOT,"%02x",data[i]&0xff));
            return sb.toString();
        }catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);}
    }

    public String globalModel(){return prefs.getString("global_model",AUTO);}
    public boolean applyModelToAll(){return prefs.getBoolean("apply_model_to_all",true);}
    public void setApplyModelToAll(boolean enabled){
        prefs.edit().putBoolean("apply_model_to_all",enabled).apply();
    }
    public String override(String key){return applyModelToAll()?null:prefs.getString("model_"+key,null);}
    public String selected(String key){
        String override=override(key);
        return override==null?globalModel():override;
    }
    public void setOverride(String key,String model){
        if(applyModelToAll())return;
        if(model==null || model.equals(globalModel())){
            prefs.edit().remove("model_"+key).apply();
        }else prefs.edit().putString("model_"+key,model).apply();
    }
    public void setGlobalModel(String model){
        prefs.edit().putString("global_model",model).apply();
    }
    /** Apply selected model to all (existing and future) challenges. */
    public void applyGlobalToAll(String model){
        SharedPreferences.Editor e=prefs.edit().putString("global_model",model).putBoolean("apply_model_to_all",true);
        for(String key:prefs.getAll().keySet()){
            if(key.startsWith("model_"))e.remove(key);
        }
        e.apply();
    }

    public static final class Selection {
        public final String slug,label;
        Selection(String slug,String label){this.slug=slug;this.label=label;}
    }

    /**
     * Default selection favors the strongest available reasoning tier for the
     * account. This is a transparent heuristic, NOT a provider capability score.
     * Never fabricate or access models absent from the authenticated model list.
     */
    private static int capabilityHint(ChatGptConnection.Model m){
        String n=(m.slug+" "+m.title).toLowerCase(Locale.ROOT);
        java.util.regex.Matcher version=java.util.regex.Pattern.compile(
                "gpt[- ]?(\\d+)(?:[.](\\d+))?").matcher(n);
        int score=0;
        if(version.find()){
            int major=Integer.parseInt(version.group(1));
            int minor=version.group(2)==null?0:Integer.parseInt(version.group(2));
            score=Math.min(999,major*100+minor*10)*10;
        }
        if(n.contains("pro") || n.contains("max"))score+=400;
        if(n.contains("extra high") || n.contains("xhigh"))score+=300;
        else if(n.contains("high") || n.contains("reasoning"))score+=200;
        else if(n.contains("medium"))score+=80;
        else if(n.contains("instant") || n.contains("fast"))score-=90;
        if(n.contains("mini") || n.contains("nano") || n.contains("lite"))score-=1600;
        if(n.contains("preview") || n.contains("deprecated"))score-=40;
        return score;
    }

    public static Selection choose(List<ChatGptConnection.Model> available,String requested){
        if(available==null || available.isEmpty())
            throw new IllegalArgumentException("No ChatGPT models available");
        if(requested!=null && !requested.equals(AUTO)){
            for(ChatGptConnection.Model m:available){
                if(requested.equals(m.slug))return new Selection(m.slug,m.title);
            }
            throw new IllegalArgumentException(
                    "The saved model is no longer offered by this ChatGPT account. Select another in AI Settings.");
        }
        ChatGptConnection.Model preferred=available.get(0);
        int preferredScore=capabilityHint(preferred);
        for(ChatGptConnection.Model model:available){
            int score=capabilityHint(model);
            if(score>preferredScore){preferred=model;preferredScore=score;}
        }
        return new Selection(preferred.slug,preferred.title);
    }

    /** Automatic translation requires separate opt-in from research processing. */
    /** Translation is requested by default, but never transmitted without a separate grant. */
    public boolean autoTranslatePublicSections(){
        return prefs.getBoolean("auto_translate_public_sections",true);
    }
    public boolean automaticTranslationAuthorized(){
        return prefs.getBoolean("auto_translate_public_sections_authorized",false);
    }
    public void setAutoTranslatePublicSections(boolean enabled){
        SharedPreferences.Editor editor=prefs.edit().putBoolean("auto_translate_public_sections",enabled);
        if(!enabled)editor.putBoolean("auto_translate_public_sections_authorized",false);
        editor.apply();
    }
    public void authorizeAutomaticTranslation(){
        prefs.edit().putBoolean("auto_translate_public_sections",true)
            .putBoolean("auto_translate_public_sections_authorized",true).apply();
    }
    public void revokeAutomaticTranslationAuthorization(){
        prefs.edit().putBoolean("auto_translate_public_sections_authorized",false).apply();
    }

    public boolean autoConsent(){return prefs.getBoolean("auto_consent",false);}
    public void setAutoConsent(boolean consent){prefs.edit().putBoolean("auto_consent",consent).apply();}
    public int dailyLimit(){return prefs.getInt("daily_limit_v2",40);}
    /** One canonical, persisted stage-based percentage for list and detail.
     * Stage 0..4 = 0..80%; 100% only after verified complete export.
     */
    public int processingPercent(String key){
        return Math.min(100,Math.max(0,processingStage(key))*20);
    }
    public int processingStage(String key){
        return automaticExportCompleted(key)?5:prefs.getInt("processing_stage_"+key,0);
    }
    public void setProcessingStage(String key,int stage){
        prefs.edit().putInt("processing_stage_"+key,Math.max(0,Math.min(4,stage))).apply();
    }
    public void setDailyLimit(int limit){
        if(limit!=1&&limit!=3&&limit!=5&&limit!=10&&limit!=20&&limit!=40)
            throw new IllegalArgumentException("Unsupported limit");
        prefs.edit().putInt("daily_limit_v2",limit).apply();
    }

    private String today(){return LocalDate.now().toString();}
    private int usedToday(){
        return today().equals(prefs.getString("usage_day",""))?prefs.getInt("usage_count",0):0;
    }
    public synchronized int remaining(){return Math.max(0,dailyLimit()-usedToday());}

    /**
     * Reserve one request before HTTP begins. A failure still uses the local
     * allowance, conservatively preventing accidental additional requests.
     */
    public synchronized boolean reserve(boolean requireAutoConsent){
        if(requireAutoConsent && !autoConsent())return false;
        int used=usedToday();
        if(used>=dailyLimit())return false;
        return prefs.edit().putString("usage_day",today()).putInt("usage_count",used+1).commit();
    }

    public String processingOutcome(String key){return prefs.getString("processing_outcome_"+key,"");}
    public void setProcessingOutcome(String key,String outcome){
        prefs.edit().putString("processing_outcome_"+key,outcome).apply();
    }
    public boolean automaticExportCompleted(String key){
        return prefs.getBoolean("auto_export_complete_"+key,false)
                && lastResult(key)!=null;
    }
    public void markAutomaticExportCompleted(String key){
        prefs.edit().putBoolean("auto_export_complete_"+key,true).apply();
    }

    public void invalidateAutomaticExport(String key){
        prefs.edit().putBoolean("auto_export_complete_"+key,false)
                .putString("processing_outcome_"+key,"export_error").apply();
    }

    /** Never consume multiple source-suggestion AI requests on repeat screen visits. */
    public boolean sourceSuggestionAttemptedToday(String key){
        return today().equals(prefs.getString("official_suggest_day_"+key,""));
    }
    public void markSourceSuggestionAttempted(String key){
        prefs.edit().putString("official_suggest_day_"+key,today()).apply();
    }

    public boolean alreadyAttempted(String key){
        return today().equals(prefs.getString("attempt_"+key,""));
    }
    public void markAttempted(String key){
        prefs.edit().putString("attempt_"+key,today()).apply();
    }
    public String lastResult(String key){
        return prefs.getString("latest_"+key,null);
    }

    /** Only public, approved challenge navigation metadata; never secrets or research data. */
    public static final class VisitedChallenge {
        public final String title,url,kind;
        public final int number;
        VisitedChallenge(String title,String url,String kind,int number){
            this.title=title;this.url=url;this.kind=kind;this.number=number;
        }
    }

    public void rememberVisitedChallenge(String title,String url,String kind,int number){
        if(title==null || title.isEmpty() || title.length()>250
                || url==null || !TrustedChallengeReader.approved(url))return;
        prefs.edit().putString("last_visited_title",title)
            .putString("last_visited_url",url)
            .putString("last_visited_kind",kind==null?"directory":kind)
            .putInt("last_visited_number",Math.max(0,number)).apply();
    }

    public VisitedChallenge lastVisitedChallenge(){
        String title=prefs.getString("last_visited_title",null);
        String url=prefs.getString("last_visited_url",null);
        if(title==null || title.isEmpty() || url==null
                || !TrustedChallengeReader.approved(url))return null;
        return new VisitedChallenge(title,url,
            prefs.getString("last_visited_kind","directory"),
            prefs.getInt("last_visited_number",0));
    }

    public String pdfTreeUri(){ return prefs.getString("pdf_tree_uri",null); }
    public void setPdfTreeUri(String uri){
        prefs.edit().putString("pdf_tree_uri",uri).apply();
    }
    public void recoverResult(String key,String answer,String date){
        if(key==null || !key.matches("[a-f0-9]{32}") || answer==null || answer.length()>90000)return;
        String existing=prefs.getString("last_date_"+key,"");
        if(date==null || !date.matches("\\d{4}-\\d{2}-\\d{2}"))date=today();
        if(existing.compareTo(date)>0)return; // keep a newer local result
        prefs.edit().putString("latest_"+key,answer)
                .putString("last_date_"+key,date).apply();
    }

    /** Portable preferences do not include OAuth credentials, file grants, or prior AI consent. */
    public org.json.JSONObject exportSettings(){
        org.json.JSONObject root=new org.json.JSONObject();
        try{
            root.put("type","bioqra-settings");
            root.put("version",1);
            root.put("global_model",globalModel());
            root.put("apply_model_to_all",applyModelToAll());
            root.put("daily_limit",dailyLimit());
            org.json.JSONObject overrides=new org.json.JSONObject();
            for(java.util.Map.Entry<String,?> e:prefs.getAll().entrySet()){
                String k=e.getKey();
                if(k.matches("model_[a-f0-9]{32}") && e.getValue() instanceof String)
                    overrides.put(k.substring(6),(String)e.getValue());
            }
            root.put("overrides",overrides);
            root.put("pdf_folder","Choose folder again on this device");
            root.put("automatic_consent","Requires reauthorization on import");
        }catch(org.json.JSONException e){throw new IllegalStateException(e);}
        return root;
    }

    public void importSettings(org.json.JSONObject json) throws Exception{
        if(!"bioqra-settings".equals(json.optString("type")) || json.optInt("version")!=1)
            throw new Exception("Not a supported BIOQRA settings backup.");
        String model=json.getString("global_model");
        if(model.length()>160 || model.isEmpty())throw new Exception("Invalid model name.");
        int limit=json.getInt("daily_limit");
        if(limit!=1 && limit!=3 && limit!=5 && limit!=10 && limit!=20 && limit!=40)
            throw new Exception("Invalid daily request limit.");
        org.json.JSONObject overrides=json.getJSONObject("overrides");
        if(overrides.length()>1500)throw new Exception("Too many model overrides.");
        java.util.Map<String,String> validated=new java.util.HashMap<>();
        java.util.Iterator<String> keys=overrides.keys();
        while(keys.hasNext()){
            String k=keys.next();
            String value=overrides.getString(k);
            if(!k.matches("[a-f0-9]{32}") || value.length()<1 || value.length()>160)
                throw new Exception("Invalid challenge/model preference.");
            validated.put("model_"+k,value);
        }
        SharedPreferences.Editor edit=prefs.edit();
        for(String key:prefs.getAll().keySet())if(key.matches("model_[a-f0-9]{32}"))edit.remove(key);
        edit.putString("global_model",model).putInt("daily_limit",limit)
                .putBoolean("auto_consent",false)
                .putBoolean("apply_model_to_all",json.optBoolean("apply_model_to_all",true));
        for(java.util.Map.Entry<String,String> entry:validated.entrySet())
            edit.putString(entry.getKey(),entry.getValue());
        if(!edit.commit())throw new Exception("Could not save restored settings.");
    }
    public void storeResult(String key,String answer){
        storeResult(key,"Challenge",null,null,answer);
    }
    public void storeResult(String key,String title,String url,String model,String answer){
        if(answer!=null && answer.length()<=90000){
            SharedPreferences.Editor edit=prefs.edit().remove("auto_export_complete_"+key).remove("processing_outcome_"+key).putString("latest_"+key,answer)
                .putString("last_date_"+key,today()).putString("title_"+key,
                    title==null?"Challenge":title);
            if(url!=null)edit.putString("source_"+key,url);
            if(model!=null)edit.putString("ai_model_"+key,model);
            edit.apply();
        }
    }

    public void updateLegacyMetadata(String key,String title,String source){
        if(!prefs.contains("latest_"+key))return;
        SharedPreferences.Editor e=prefs.edit();
        if(!prefs.contains("title_"+key) && title!=null)e.putString("title_"+key,title);
        if(!prefs.contains("source_"+key) && source!=null)e.putString("source_"+key,source);
        e.apply();
    }

    public static final class SavedResult {
        public final String key,title,date,source,model,answer;
        SavedResult(String key,String title,String date,String source,String model,String answer){
            this.key=key;this.title=title;this.date=date;this.source=source;
            this.model=model;this.answer=answer;
        }
    }

    public java.util.List<SavedResult> savedResults(){
        java.util.List<SavedResult> found=new java.util.ArrayList<>();
        for(java.util.Map.Entry<String,?> e:prefs.getAll().entrySet()){
            if(!e.getKey().matches("latest_[a-f0-9]{32}") || !(e.getValue() instanceof String))continue;
            String key=e.getKey().substring(7);
            found.add(new SavedResult(key,prefs.getString("title_"+key,"Challenge"),
                prefs.getString("last_date_"+key,today()),
                prefs.getString("source_"+key,""),prefs.getString("ai_model_"+key,""),
                (String)e.getValue()));
        }
        found.sort((a,b)->b.date.compareTo(a.date));
        return found;
    }

    public org.json.JSONObject exportResults(){
        org.json.JSONObject root=new org.json.JSONObject();
        try{
            root.put("type","bioqra-results");root.put("version",1);
            org.json.JSONArray items=new org.json.JSONArray();
            for(SavedResult item:savedResults()){
                org.json.JSONObject obj=new org.json.JSONObject();
                obj.put("key",item.key);obj.put("title",item.title);
                obj.put("date",item.date);obj.put("source",item.source);
                obj.put("model",item.model);obj.put("answer",item.answer);
                items.put(obj);
            }
            root.put("results",items);
        }catch(org.json.JSONException e){throw new IllegalStateException(e);}
        return root;
    }

    public int importResults(org.json.JSONObject root) throws Exception{
        if(!"bioqra-results".equals(root.optString("type")) || root.optInt("version")!=1)
            throw new Exception("Unsupported BIOQRA results archive.");
        org.json.JSONArray items=root.getJSONArray("results");
        if(items.length()>1500)throw new Exception("Too many results.");
        java.util.List<SavedResult> validated=new java.util.ArrayList<>();
        for(int i=0;i<items.length();i++){
            org.json.JSONObject obj=items.getJSONObject(i);
            String key=obj.getString("key"),title=obj.getString("title");
            String date=obj.getString("date"),answer=obj.getString("answer");
            String source=obj.optString("source",""),model=obj.optString("model","");
            if(!key.matches("[a-f0-9]{32}") || title.length()>250 || title.isEmpty()
                || !date.matches("\\d{4}-\\d{2}-\\d{2}")
                || answer.length()>90000 || source.length()>1000 || model.length()>160)
                throw new Exception("Invalid archive entry #"+(i+1));
            validated.add(new SavedResult(key,title,date,source,model,answer));
        }
        SharedPreferences.Editor edit=prefs.edit();
        for(SavedResult item:validated){
            if(prefs.getString("last_date_"+item.key,"").compareTo(item.date)>0)continue;
            edit.putString("latest_"+item.key,item.answer)
                .putString("title_"+item.key,item.title)
                .putString("last_date_"+item.key,item.date)
                .putString("source_"+item.key,item.source)
                .putString("ai_model_"+item.key,item.model);
        }
        if(!edit.commit())throw new Exception("Unable to restore results.");
        return validated.size();
    }
}
