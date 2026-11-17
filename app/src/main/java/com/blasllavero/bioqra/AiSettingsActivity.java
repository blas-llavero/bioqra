package com.blasllavero.bioqra;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** On-device settings for account-specific model choices and automatic AI usage. */
public final class AiSettingsActivity extends Activity {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private AiPreferences settings;
    private ChatGptConnection account;
    private Spinner models,limitPicker;
    private TextView info,remaining;
    private CheckBox consent;
    private List<ChatGptConnection.Model> available=new ArrayList<>();
    private List<String> modelKeys=new ArrayList<>();

    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String content,int size){
        TextView t=new TextView(this);
        t.setText(content);t.setTextSize(size);t.setTextColor(Color.rgb(24,72,91));
        t.setPadding(dp(2),dp(12),dp(2),dp(9));
        return t;
    }
    private Button button(String title){
        Button b=new Button(this);b.setText(title);b.setAllCaps(false);return b;
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        settings=new AiPreferences(this);
        account=new ChatGptConnection(this);
        getWindow().setStatusBarColor(Color.rgb(24,72,91));
        getWindow().setNavigationBarColor(Color.BLACK);
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(14),dp(16),dp(30));
        root.setBackgroundColor(Color.rgb(248,251,250));
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                root.setPadding(dp(16),dp(14)+bars.top,dp(16),dp(30)+bars.bottom);
            }
            return insets;
        });
        scroll.addView(root);setContentView(scroll);
        Button back=button("← Back to BIOQRA");
        back.setOnClickListener(v->finish());
        root.addView(back,new LinearLayout.LayoutParams(-1,dp(55)));
        TextView heading=label("AI Settings",25);heading.setTypeface(null,Typeface.BOLD);
        root.addView(heading);
        root.addView(label("Connected: "+account.accountLabel()
                +"\nUsing ChatGPT plan · Models and usage depend on your account.",14));
        root.addView(label("Default model (new challenges / inherited selections)",17));
        models=new Spinner(this);
        root.addView(models,new LinearLayout.LayoutParams(-1,dp(55)));
        info=label("Loading the model list for your ChatGPT account...",13);
        root.addView(info);
        populateModels(new ArrayList<>());
        Button apply=button("Apply this model to ALL challenges");
        root.addView(apply,new LinearLayout.LayoutParams(-1,dp(56)));
        apply.setOnClickListener(v->{
            if(modelKeys.isEmpty())return;
            String key=modelKeys.get(models.getSelectedItemPosition());
            new AlertDialog.Builder(this)
                .setTitle("Apply globally?")
                .setMessage("Set this model for all current and future challenges, replacing any individual challenge selections saved on this device.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Apply to all",(d,w)->{
                    settings.applyGlobalToAll(key);
                    info.setText("Applied globally. Each challenge now inherits this model unless changed individually.");
                }).show();
        });

        TextView controlTitle=label("Automatic Mode",19);
        controlTitle.setTypeface(null,Typeface.BOLD);
        root.addView(controlTitle);
        root.addView(label("In Automatic Mode, opening a selected challenge can send its PUBLIC statement directly to ChatGPT without another dialog. No private medical data, no external submissions, no background bulk runs.",14));
        consent=new CheckBox(this);
        consent.setText("Automatically analyze selected public challenges");
        consent.setTextColor(Color.rgb(24,72,91));
        consent.setChecked(settings.autoConsent());
        root.addView(consent);
        consent.setOnCheckedChangeListener((b,checked)->{
            if(!checked){settings.setAutoConsent(false);return;}
            if(!account.canUsePlan()){
                consent.setChecked(false);
                new AlertDialog.Builder(this)
                    .setTitle("ChatGPT account required")
                    .setMessage("Connect and authorize use of your ChatGPT plan before enabling Automatic Mode.")
                    .setPositiveButton("OK",null).show();
                return;
            }
            consent.setChecked(false);
            new AlertDialog.Builder(this)
                .setTitle("Authorize automatic ChatGPT usage?")
                .setMessage("For EACH challenge you open in Automatic Mode, BIOQRA may send a retrieved public problem statement to your connected ChatGPT account WITHOUT asking again. This uses your plan/credit allowance and obeys the local daily request limit below.\n\nOnly one selected challenge at a time; no patient data or private datasets; no submissions to contest websites. Turn this off whenever you want.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Enable automatic analysis",(d,w)->{
                    settings.setAutoConsent(true);
                    consent.setChecked(true);
                }).show();
        });
        root.addView(label("Maximum ChatGPT attempts per day on this device",16));
        limitPicker=new Spinner(this);
        final int[] limits={1,3,5,10};
        String[] limitLabels={"1 attempt","3 attempts (default)","5 attempts","10 attempts"};
        ArrayAdapter<String> limitsAdapter=new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,limitLabels);
        limitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        limitPicker.setAdapter(limitsAdapter);
        root.addView(limitPicker,new LinearLayout.LayoutParams(-1,dp(50)));
        for(int i=0;i<limits.length;i++)if(settings.dailyLimit()==limits[i])
            limitPicker.setSelection(i);
        limitPicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){
                settings.setDailyLimit(limits[pos]);updateRemaining();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        remaining=label("",14);root.addView(remaining);updateRemaining();
        root.addView(label("The daily limit is a local safety limit, not a promise of remaining ChatGPT credits. Failed attempts can count against it. Models not offered by your account cannot be used.",13));
        root.addView(label("AI-generated plans are proposals, not validated experiments, clinical findings or cancer treatments.",13));
        Button reload=button("Refresh models from ChatGPT");
        root.addView(reload,new LinearLayout.LayoutParams(-1,dp(52)));
        reload.setOnClickListener(v->loadModels());
        loadModels();
    }

    private void updateRemaining(){
        if(remaining!=null)
            remaining.setText("Local AI attempts remaining today: "+settings.remaining());
    }
    private void populateModels(List<ChatGptConnection.Model> source){
        available=new ArrayList<>(source);
        modelKeys.clear();
        List<String> titles=new ArrayList<>();
        titles.add("Auto · first model offered by ChatGPT");
        modelKeys.add(AiPreferences.AUTO);
        for(ChatGptConnection.Model m:available){
            titles.add(m.title);modelKeys.add(m.slug);
        }
        String chosen=settings.globalModel();
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,titles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        models.setAdapter(adapter);
        int index=modelKeys.indexOf(chosen);
        models.setSelection(Math.max(0,index));
        models.setEnabled(!source.isEmpty());
        models.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){
                if(pos>=0 && pos<modelKeys.size())
                    settings.setGlobalModel(modelKeys.get(pos));
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
    }
    private void loadModels(){
        info.setText("Refreshing available ChatGPT models...");
        WORKER.execute(()->{
            try{
                List<ChatGptConnection.Model> got=account.models();
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    populateModels(got);
                    info.setText(got.size()+" models offered by the connected account. The automatic default follows the server's list order; it is not a verified ranking of scientific capability.");
                });
            }catch(Exception ex){
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    populateModels(new ArrayList<>());
                    info.setText("Model list unavailable: "+ex.getMessage());
                });
            }
        });
    }
}
