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
    public String override(String key){return prefs.getString("model_"+key,null);}
    public String selected(String key){
        String override=override(key);
        return override==null?globalModel():override;
    }
    public void setOverride(String key,String model){
        if(model==null || model.equals(globalModel())){
            prefs.edit().remove("model_"+key).apply();
        }else prefs.edit().putString("model_"+key,model).apply();
    }
    public void setGlobalModel(String model){
        prefs.edit().putString("global_model",model).apply();
    }
    /** Apply selected model to all (existing and future) challenges. */
    public void applyGlobalToAll(String model){
        SharedPreferences.Editor e=prefs.edit().putString("global_model",model);
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
     * This is a naming-based preference, not an independently confirmed
     * scientific ranking. Only actual models advertised by the signed-in
     * account are considered. Unknown models keep the server's ordering.
     */
    private static int capabilityHint(ChatGptConnection.Model m){
        String n=(m.slug+" "+m.title).toLowerCase(Locale.ROOT);
        int score=0;
        if(n.contains("gpt-6"))score=600;
        else if(n.contains("gpt-5.6"))score=560;
        else if(n.contains("gpt-5.5"))score=550;
        else if(n.contains("gpt-5"))score=500;
        else if(n.contains("gpt-4"))score=400;
        if(n.contains("mini") || n.contains("nano") || n.contains("lite"))score-=160;
        if(n.contains("pro"))score+=60;
        if(n.contains("extra high") || n.contains("xhigh"))score+=45;
        else if(n.contains("high"))score+=30;
        else if(n.contains("medium"))score+=10;
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

    public boolean autoConsent(){return prefs.getBoolean("auto_consent",false);}
    public void setAutoConsent(boolean consent){prefs.edit().putBoolean("auto_consent",consent).apply();}
    public int dailyLimit(){return prefs.getInt("daily_limit",3);}
    public void setDailyLimit(int limit){
        if(limit!=1&&limit!=3&&limit!=5&&limit!=10)
            throw new IllegalArgumentException("Unsupported limit");
        prefs.edit().putInt("daily_limit",limit).apply();
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

    public boolean alreadyAttempted(String key){
        return today().equals(prefs.getString("attempt_"+key,""));
    }
    public void markAttempted(String key){
        prefs.edit().putString("attempt_"+key,today()).apply();
    }
    public String lastResult(String key){
        return prefs.getString("latest_"+key,null);
    }
    public void storeResult(String key,String answer){
        if(answer!=null && answer.length()<=90000)
            prefs.edit().putString("latest_"+key,answer).putString("last_date_"+key,today()).apply();
    }
}
