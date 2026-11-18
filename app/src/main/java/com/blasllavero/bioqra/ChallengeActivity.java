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
    public static final String KIND="challenge_kind";
    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
    private String title, url, mode, kind;
    private TextView status, output, statement, resolvedLink, deadlinesView;
    private EditText sample;
    private Button aiButton, loadButton, offlineButton;
    private String fetchedText="";
    private String verifiedPageUrl="";
    private boolean specificPage=false;
    private ChatGptConnection connection;
    private AiPreferences aiPreferences;
    private String challengeKey;
    private String selectedModelLabel="Global / Auto";
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
                && specificPage && fetchedText.length()>=100);
        if(offlineButton!=null)offlineButton.setEnabled(!busy);
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        connection=new ChatGptConnection(this);
        aiPreferences=new AiPreferences(this);
        title=getIntent().getStringExtra(TITLE);
        url=getIntent().getStringExtra(URL);
        mode=getIntent().getStringExtra(MODE);
        kind=getIntent().getStringExtra(KIND);
        if(kind==null)kind="directory";
        if(title==null)title="Challenge";
        if(url==null)url="";
        if(!"automatic".equals(mode))mode="manual";
        challengeKey=AiPreferences.stableKey(title,title);
        aiPreferences.updateLegacyMetadata(challengeKey,title,url);
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
        root.addView(text("AI model: configured for this challenge on the main list; "
                +"otherwise inherits the global AI Settings model. "
                +"Automatic use is limited to "+aiPreferences.dailyLimit()
                +" requests per day on this device.",13,false));
        if(isFunding()) {
            root.addView(text("Research funding opportunity · Not a programming contest",17,true));
            root.addView(text("ChatGPT can prepare a research proposal and an unexecuted methodology, but it cannot establish that this cancer research challenge is solved. The organizer may require an eligible research team, experimental data and scientific validation.",14,false));
        } else if("benchmark".equals(kind)) {
            root.addView(text("Scientific benchmark · Results require real evaluation data and metrics",15,true));
        } else if("directory".equals(kind)) {
            root.addView(text("Source listing · Check that the retrieved webpage contains the actual challenge rules and dataset.",14,false));
        }

        status=text("Ready. Only public HTML from an approved HTTPS source can be retrieved.",14,false);
        root.addView(status);
        loadButton=button("1 · Find detailed challenge statement");
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
        resolvedLink=text("Detailed statement URL: not identified yet",12,false);
        resolvedLink.setTextIsSelectable(true);
        root.addView(resolvedLink);
        root.addView(text("Participation dates and supporting documents",18,true));
        deadlinesView=text("Not checked. Dates must be verified on the official organizer website.",13,false);
        deadlinesView.setTextIsSelectable(true);
        deadlinesView.setLineSpacing(dp(4),1.08f);
        root.addView(deadlinesView);
        statement=text("No statement retrieved yet.",14,false);
        statement.setTextIsSelectable(true);
        statement.setLineSpacing(dp(5),1.1f);
        statement.setPadding(dp(3),dp(12),dp(3),dp(18));
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

        aiButton=button(isFunding()
                ? "3 · Ask ChatGPT for a research proposal"
                : "3 · Ask ChatGPT for a solution proposal");
        root.addView(aiButton,new LinearLayout.LayoutParams(-1,dp(55)));
        aiButton.setOnClickListener(v->confirmAI());
        String oldResult=aiPreferences.lastResult(challengeKey);
        output=text(oldResult!=null ? "SAVED AI DRAFT (not reverified)\n\n"+oldResult
                : isFunding() ? "No research proposal or independent validation yet."
                : "No solution proposal or independent verification yet.",14,false);
        output.setTextIsSelectable(true);
        root.addView(text("Results and limitations",18,true));
        root.addView(output);
        Button savePdf=button("Save this challenge result as PDF");
        root.addView(savePdf,new LinearLayout.LayoutParams(-1,dp(52)));
        savePdf.setOnClickListener(v->saveSavedPdf());
        updateButtons();

        // Automatic mode retrieves the public challenge on opening.
        // Actual ChatGPT usage still requires consent to transmit each statement.
        if("automatic".equals(mode))retrieve();
    }

    private void saveSavedPdf(){
        String folder=aiPreferences.pdfTreeUri();
        if(folder==null){
            new AlertDialog.Builder(this).setTitle("Select a PDF folder")
                .setMessage("Open AI Settings and choose the destination folder first.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("AI Settings",(d,w)->
                    startActivity(new Intent(this,AiSettingsActivity.class))).show();
            return;
        }
        AiPreferences.SavedResult record=null;
        for(AiPreferences.SavedResult r:aiPreferences.savedResults())
            if(challengeKey.equals(r.key)){record=r;break;}
        if(record==null){status.setText("No saved AI result for this challenge.");return;}
        final AiPreferences.SavedResult result=record;
        status.setText("Saving PDF to your configured folder...");
        EXEC.execute(()->{
            String message;
            try{
                ArchiveFiles.makePdf(this,Uri.parse(folder),result);
                message="PDF saved in the challenge folder as YYMMDD-"+ArchiveFiles.cleanName(result.title)+".pdf";
            }catch(Exception e){message="PDF export failed: "+e.getMessage();}
            final String feedback=message;
            runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())status.setText(feedback);});
        });
    }

    private boolean isFunding() {
        return "funding".equals(kind) || "funded_research".equals(kind);
    }

    private void retrieve(){
        if(busy)return;
        if(!TrustedChallengeReader.approved(url)){
            showError("Blocked: URL is not a trusted HTTPS research domain. Open only after independent review.");
            return;
        }
        busy=true;
        status.setText("Checking official organizer links for the detailed challenge statement...");
        updateButtons();
        EXEC.execute(()->{
            try {
                ChallengePageResolver.Outcome result=ChallengePageResolver.resolve(this,url,title);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    fetchedText=result.text;
                    specificPage=result.specific;
                    verifiedPageUrl=result.resolvedUrl;
                    statement.setText(result.text);
                    resolvedLink.setText("Selected public page: "+result.resolvedUrl);
                    StringBuilder sources=new StringBuilder(result.deadlineSummary);
                    if(!result.documentLinks.isEmpty()){
                        sources.append("\n\nPUBLIC SUPPORTING PDF DOCUMENTS (read-only):\n");
                        for(String doc:result.documentLinks)sources.append(doc).append("\n");
                    }
                    deadlinesView.setText(sources.toString());
                    status.setText(result.message+" Retrieved "+result.text.length()+" characters.");
                    busy=false;updateButtons();
                    if("automatic".equals(mode) && connection.canUsePlan() && specificPage){
                        if(aiPreferences.alreadyAttempted(challengeKey)){
                            status.setText("Already attempted today; showing saved result if available. "
                                    +"Use the AI button to explicitly request another attempt.");
                        }else if(aiPreferences.autoConsent()){
                            chooseModelAndRun(true);
                        }else{
                            status.setText("Detailed statement found. Enable one-time automatic permission "
                                    +"in AI Settings or tap the AI button for this attempt.");
                        }
                    }
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
                .setMessage("Return to the main screen and reconnect to authorize plan usage. No request was sent.")
                .setPositiveButton("OK",null).show();
            return;
        }
        if(!specificPage || fetchedText.length()<100){
            showError("A verified public challenge statement is required. No AI request was sent.");
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Authorize ChatGPT attempt?")
            .setMessage("Send this challenge's PUBLIC text and any sample you pasted "
                +"to your connected ChatGPT account? Limit remaining today: "
                +aiPreferences.remaining()+". Do not enter patient records or confidential data."
                +"\n\nThe answer is an unverified research/solution draft; no contest submissions.")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Run once",(d,w)->chooseModelAndRun(false))
            .show();
    }

    /** Automatic mode uses saved preferences and does not prompt for a model. */
    private void chooseModelAndRun(boolean unattended){
        if(busy)return;
        if(unattended && (!aiPreferences.autoConsent()
                || aiPreferences.alreadyAttempted(challengeKey)))return;
        if(aiPreferences.remaining()<=0){
            showError("Local daily ChatGPT request limit reached. Change it in AI Settings or try tomorrow.");
            return;
        }
        busy=true;
        status.setText("Checking your saved model selection...");
        updateButtons();
        final String publicStatement=fetchedText;
        final String source=verifiedPageUrl;
        // Unattended requests never send typed user samples, even if present.
        final String input=unattended?"":sample.getText().toString();
        EXEC.execute(()->{
            try{
                List<ChatGptConnection.Model> offered=connection.models();
                AiPreferences.Selection selection=AiPreferences.choose(
                        offered,aiPreferences.selected(challengeKey));
                if(!aiPreferences.reserve(unattended))
                    throw new Exception("Local limit reached or automatic permission disabled.");
                aiPreferences.markAttempted(challengeKey);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    status.setText("Using "+selection.label+"; preparing an unverified draft...");
                    output.setText("AI analysis in progress. No scientific result validated.");
                });
                String answer=connection.respond(selection.slug,title,
                    "Source: "+source+"\n\n"+publicStatement,input,kind);
                aiPreferences.storeResult(challengeKey,title,source,selection.label,answer);
                String pdfStatus="";
                String destination=aiPreferences.pdfTreeUri();
                if(destination!=null){
                    try{
                        AiPreferences.SavedResult record=null;
                        for(AiPreferences.SavedResult r:aiPreferences.savedResults()){
                            if(challengeKey.equals(r.key)){record=r;break;}
                        }
                        if(record!=null){
                            ArchiveFiles.makePdf(this,Uri.parse(destination),record);
                            pdfStatus=" · PDF saved to configured folder";
                        }
                    }catch(Exception e){pdfStatus=" · PDF export failed: "+e.getMessage();}
                }
                final String pdfResult=pdfStatus;
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;
                    output.setText((isFunding()
                            ? "AI RESEARCH PROPOSAL — UNVERIFIED"
                            : "AI DRAFT — UNVERIFIED")
                        +"\nModel: "+selection.label+"\n\n"+answer
                        +"\n\nBIOQRA has not run any generated program or validated a benchmark.");
                    status.setText("AI draft saved locally for this challenge. "
                        +"Request limit remaining: "+aiPreferences.remaining()+pdfResult);
                    updateButtons();
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;
                    showError("AI attempt unavailable: "+e.getMessage());
                });
            }
        });
    }
}
