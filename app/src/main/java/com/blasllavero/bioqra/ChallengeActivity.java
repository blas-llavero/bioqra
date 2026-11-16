package com.blasllavero.bioqra;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.os.Build;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scientific workbench for one public challenge. Reviewable AI proposals,
 * never external submission, arbitrary code execution or patient-data upload.
 */
public final class ChallengeActivity extends Activity {
    public static final String TITLE="challenge_title";
    public static final String URL="challenge_url";
    public static final String MODE="challenge_mode";
    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
    private String title, url, mode;
    private TextView status, output, statement;
    private EditText sample;
    private Button aiButton, loadButton, offlineButton;
    private String fetchedText="";
    private ChatGptConnection connection;
    private volatile boolean busy=false;

    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}

    private TextView text(String s, int size, boolean bold){
        TextView t=new TextView(this);
        t.setText(s);t.setTextColor(Color.rgb(19,57,70));t.setTextSize(size);
        if(bold)t.setTypeface(null,Typeface.BOLD);
        t.setPadding(0,dp(9),0,dp(9));
        return t;
    }
    private Button button(String name){
        Button b=new Button(this);b.setAllCaps(false);b.setText(name);
        return b;
    }
    private void showError(String message){
        status.setText(message);
        updateButtons();
    }
    private void updateButtons(){
        if(loadButton!=null)loadButton.setEnabled(!busy);
        if(aiButton!=null)aiButton.setEnabled(!busy && connection.canUsePlan()
                && fetchedText.length()>=100);
        if(offlineButton!=null)offlineButton.setEnabled(!busy);
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        connection=new ChatGptConnection(this);
        title=getIntent().getStringExtra(TITLE);
        url=getIntent().getStringExtra(URL);
        mode=getIntent().getStringExtra(MODE);
        if(title==null)title="Challenge";
        if(url==null)url="";
        if(!"automatic".equals(mode))mode="manual";
        getWindow().setStatusBarColor(Color.rgb(24,72,91));
        getWindow().setNavigationBarColor(Color.BLACK);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247,251,250));
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(10),dp(16),dp(22));
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                root.setPadding(dp(16),dp(10)+bars.top,dp(16),dp(22)+bars.bottom);
            }
            return insets;
        });
        scroll.addView(root);
        setContentView(scroll);

        Button back=button("← Back to challenges");
        back.setOnClickListener(v->finish());
        root.addView(back,new LinearLayout.LayoutParams(-1,dp(52)));
        root.addView(text(title,23,true));
        root.addView(text("Mode: "+("automatic".equals(mode)?"Automatic (ChatGPT)":"Manual (no AI)")
                +" · Read-only · No contest submissions",14,false));
        root.addView(text(url,12,false));

        status=text("Ready. Only public HTML from an approved HTTPS source can be retrieved.",14,false);
        root.addView(status);
        loadButton=button("1 · Retrieve public challenge statement");
        root.addView(loadButton,new LinearLayout.LayoutParams(-1,dp(55)));
        loadButton.setOnClickListener(v->retrieve());

        Button official=button("View original challenge in browser");
        root.addView(official,new LinearLayout.LayoutParams(-1,dp(52)));
        official.setOnClickListener(v->{
            if(TrustedChallengeReader.approved(url))
                startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
            else status.setText("This link is not in BIOQRA's approved research-source allowlist.");
        });

        root.addView(text("Public statement (read-only preview)",18,true));
        statement=text("No statement retrieved yet.",14,false);
        statement.setTextIsSelectable(true);
        root.addView(statement);

        root.addView(text("Public sample DNA/RNA data for local checks (optional)",16,true));
        sample=new EditText(this);
        sample.setHint("Paste public sample DNA/RNA sequence (no patient records)");
        sample.setTextSize(14);sample.setMinLines(3);sample.setMaxLines(8);
        sample.setGravity(Gravity.TOP);
        root.addView(sample,new LinearLayout.LayoutParams(-1,dp(112)));

        offlineButton=button("2 · Run offline sequence checks");
        root.addView(offlineButton,new LinearLayout.LayoutParams(-1,dp(55)));
        offlineButton.setOnClickListener(v->{
            String result=LocalBioinformatics.inspect(sample.getText().toString());
            output.setText(result);
            status.setText("Local checks completed. No network/AI usage.");
        });

        aiButton=button("3 · Ask ChatGPT for a solution proposal");
        root.addView(aiButton,new LinearLayout.LayoutParams(-1,dp(55)));
        aiButton.setOnClickListener(v->confirmAI());
        output=text("No solution proposal or independent verification yet.",14,false);
        output.setTextIsSelectable(true);
        root.addView(text("Results and limitations",18,true));
        root.addView(output);
        updateButtons();

        // Automatic mode retrieves the public challenge on opening.
        // Actual ChatGPT usage still requires consent to transmit each statement.
        if("automatic".equals(mode))retrieve();
    }

    private void retrieve(){
        if(busy)return;
        if(!TrustedChallengeReader.approved(url)){
            showError("Blocked: URL is not a trusted HTTPS research domain. Open only after independent review.");
            return;
        }
        busy=true;
        status.setText("Reading public HTML; no logins or website scripts are executed...");
        updateButtons();
        EXEC.execute(()->{
            try {
                TrustedChallengeReader.Result result=TrustedChallengeReader.read(url);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    fetchedText=result.text;
                    statement.setText(result.text);
                    status.setText("Public statement retrieved ("+result.text.length()
                            +" characters). Check that it contains the real task and rules.");
                    busy=false;updateButtons();
                    if("automatic".equals(mode) && connection.canUsePlan())
                        confirmAI();
                });
            } catch(Exception e){
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;
                    showError("Automatic retrieval unavailable: "+e.getMessage()
                        +" You can still open the original site in your browser.");
                });
            }
        });
    }

    private void confirmAI(){
        if(busy)return;
        if(!connection.canUsePlan()){
            new AlertDialog.Builder(this).setTitle("ChatGPT authorization required")
                .setMessage("The account has not authorized ChatGPT plan usage. Return to the main screen and reconnect. No AI request was sent.")
                .setPositiveButton("OK",null).show();
            return;
        }
        if(fetchedText.length()<100){showError("Retrieve and review a complete public statement first.");return;}
        new AlertDialog.Builder(this)
            .setTitle("Send public challenge to ChatGPT?")
            .setMessage("BIOQRA will send this publicly available statement and any PUBLIC sequence you entered to your authorized ChatGPT account. This uses plan capacity and may be subject to limits. Do not include patient records or private competition data.\n\nThe result is a proposal, NOT a verified solution. Nothing will be submitted externally.")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Authorize this attempt",(d,w)->chooseModelAndRun())
            .show();
    }

    private void chooseModelAndRun(){
        busy=true;status.setText("Checking available ChatGPT models...");
        updateButtons();
        EXEC.execute(()->{
            try {
                List<ChatGptConnection.Model> models=connection.models();
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;updateButtons();
                    String[] labels=new String[models.size()];
                    for(int i=0;i<models.size();i++)labels[i]=models.get(i).title;
                    new AlertDialog.Builder(this)
                        .setTitle("Choose ChatGPT model")
                        .setItems(labels,(d,which)->runAI(models.get(which).slug))
                        .setNegativeButton("Cancel",null).show();
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;showError("ChatGPT model list unavailable: "+e.getMessage());
                });
            }
        });
    }

    private void runAI(String model) {
        if(busy)return;
        busy=true;status.setText("ChatGPT is preparing a draft solution; do not close the app...");
        output.setText("AI analysis in progress. Results are not yet verified.");
        updateButtons();
        final String input=sample.getText().toString();
        EXEC.execute(()->{
            try{
                String answer=connection.respond(model,title,fetchedText,input);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;
                    output.setText("AI DRAFT — UNVERIFIED\n\n"+answer
                        +"\n\nBIOQRA did not run generated code, submit results or validate any external benchmark.");
                    status.setText("AI draft ready. Independently check algorithms, data and organizer requirements.");
                    updateButtons();
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;output.setText("No verified AI result.");
                    showError("AI attempt failed: "+e.getMessage());
                });
            }
        });
    }
}
