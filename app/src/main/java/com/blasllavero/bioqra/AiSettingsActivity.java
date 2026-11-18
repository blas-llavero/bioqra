package com.blasllavero.bioqra;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.content.ContentResolver;
import org.json.JSONObject;
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
    private static final int SAVE_SETTINGS=2101,RESTORE_SETTINGS=2102;
    private static final int SAVE_RESULTS=2103,RESTORE_RESULTS=2104;
    private static final int PICK_PDF_FOLDER=2105;
    private static final int EXPORT_ALL_ZIP=2106,IMPORT_ALL_ZIP=2107;
    private AiPreferences settings;
    private ChatGptConnection account;
    private Spinner models,limitPicker;
    private TextView info,remaining;
    private CheckBox consent;
    private Button accountButton;
    private TextView accountStatus;
    private TextView backupStatus,pdfFolderStatus;
    private boolean signingIn=false;
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
        TextView accountHeading=label("ChatGPT account",19);
        accountHeading.setTypeface(null,Typeface.BOLD);
        root.addView(accountHeading);
        accountStatus=label("",14);
        root.addView(accountStatus);
        accountButton=button("ChatGPT login");
        root.addView(accountButton,new LinearLayout.LayoutParams(-1,dp(56)));
        accountButton.setOnClickListener(v->showChatGptAccount());
        updateAccountStatus();
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
            if(settings.autoConsent())return; // already approved; avoid a second dialog
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
        TextView backupHeading=label("Backup, Restore & PDF",20);
        backupHeading.setTypeface(null,Typeface.BOLD);
        root.addView(backupHeading);
        root.addView(label("Create a single ZIP containing settings, saved AI results "
                +"and PDFs, or use the individual JSON buttons below. "
                +"ChatGPT tokens/passwords and unattended consent are never exported.",14));
        Button exportAll=button("Export all · settings + results + PDFs (.zip)");
        root.addView(exportAll,new LinearLayout.LayoutParams(-1,dp(56)));
        exportAll.setOnClickListener(v->createZipFile());
        Button importAll=button("Import all · restore ZIP backup");
        root.addView(importAll,new LinearLayout.LayoutParams(-1,dp(56)));
        importAll.setOnClickListener(v->new AlertDialog.Builder(this)
            .setTitle("Restore complete BIOQRA backup?")
            .setMessage("Settings will be replaced and saved AI results merged. "
                +"Existing PDF files will not be overwritten. "
                +"Configure the PDF folder first. "
                +"ChatGPT login and permission for unattended AI use are NOT imported.")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Choose ZIP",(d,w)->openZipFile()).show());
        Button exportSettings=button("Export settings (.json)");
        root.addView(exportSettings,new LinearLayout.LayoutParams(-1,dp(50)));
        exportSettings.setOnClickListener(v->createJsonFile(SAVE_SETTINGS,"bioqra-settings.json"));
        Button importSettings=button("Import settings (.json)");
        root.addView(importSettings,new LinearLayout.LayoutParams(-1,dp(50)));
        importSettings.setOnClickListener(v->openJsonFile(RESTORE_SETTINGS));
        Button exportResults=button("Export saved AI results (.json)");
        root.addView(exportResults,new LinearLayout.LayoutParams(-1,dp(50)));
        exportResults.setOnClickListener(v->createJsonFile(SAVE_RESULTS,"bioqra-results.json"));
        Button importResults=button("Import saved AI results (.json)");
        root.addView(importResults,new LinearLayout.LayoutParams(-1,dp(50)));
        importResults.setOnClickListener(v->openJsonFile(RESTORE_RESULTS));

        pdfFolderStatus=label("",13);
        root.addView(pdfFolderStatus);
        updateFolderLabel();
        Button selectFolder=button("Choose folder for automatic PDFs");
        root.addView(selectFolder,new LinearLayout.LayoutParams(-1,dp(53)));
        selectFolder.setOnClickListener(v->{
            Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    |Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    |Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent,PICK_PDF_FOLDER);
        });
        Button exportPdf=button("Export all saved results as PDFs");
        root.addView(exportPdf,new LinearLayout.LayoutParams(-1,dp(53)));
        exportPdf.setOnClickListener(v->exportAllPdfs());
        root.addView(label("PDF location: selected folder / Challenge / YYMMDD-Challenge.pdf. "
                +"Each challenge has one folder. A new date creates a new PDF; "
                +"exporting twice on the same day updates that file. "
                +"For a new device, select the folder again after restoring.",13));
        backupStatus=label("Backups and PDFs stay on your device until you share them.",13);
        root.addView(backupStatus);
        loadModels();
    }


    private void createZipFile(){
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE,"bioqra-complete-backup.zip");
        startActivityForResult(intent,EXPORT_ALL_ZIP);
    }
    private void openZipFile(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent,IMPORT_ALL_ZIP);
    }
    private void createJsonFile(int request,String suggested){
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE,suggested);
        startActivityForResult(intent,request);
    }
    private void openJsonFile(int request){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent,request);
    }
    private void updateFolderLabel(){
        if(pdfFolderStatus==null)return;
        String folder=settings.pdfTreeUri();
        pdfFolderStatus.setText(folder==null
                ? "PDF folder: not configured (automatic PDF creation is disabled)."
                : "PDF folder configured: "+Uri.parse(folder).getLastPathSegment());
    }
    private void exportAllPdfs(){
        String folder=settings.pdfTreeUri();
        if(folder==null){backupStatus.setText("Select a PDF folder first.");return;}
        List<AiPreferences.SavedResult> saved=settings.savedResults();
        if(saved.isEmpty()){backupStatus.setText("No saved AI results to export.");return;}
        backupStatus.setText("Exporting "+saved.size()+" research PDFs...");
        WORKER.execute(()->{
            int count=0;String failure="";
            for(AiPreferences.SavedResult item:saved){
                try{ArchiveFiles.makePdf(this,Uri.parse(folder),item);count++;}
                catch(Exception e){failure=e.getMessage();break;}
            }
            final int done=count;
            final String error=failure;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                backupStatus.setText("Exported "+done+"/"+saved.size()+" PDFs."
                        +(error.isEmpty()?"":" Error: "+error));
            });
        });
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(request==PICK_PDF_FOLDER){
            try{
                int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        |Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri,flags);
                settings.setPdfTreeUri(uri.toString());
                updateFolderLabel();
                backupStatus.setText("PDF destination configured. New AI drafts will be saved automatically.");
            }catch(Exception e){backupStatus.setText("Folder permission failed: "+e.getMessage());}
            return;
        }
        backupStatus.setText("Processing BIOQRA backup...");
        WORKER.execute(()->{
            String outcome;
            try{
                if(request==EXPORT_ALL_ZIP){
                    FullBackup.Report report=FullBackup.exportAll(this,uri,settings);
                    outcome="Complete ZIP saved: settings + "+report.results
                            +" results + "+report.pdfs+" PDFs.";
                }else if(request==IMPORT_ALL_ZIP){
                    FullBackup.Report report=FullBackup.importAll(this,uri,settings);
                    outcome="Complete backup restored: settings + "+report.results
                            +" results + "+report.pdfs+" PDFs. "
                            +"Re-enable automatic analysis separately if desired.";
                }else if(request==SAVE_SETTINGS || request==SAVE_RESULTS){
                    JSONObject payload=request==SAVE_SETTINGS?settings.exportSettings()
                            :settings.exportResults();
                    ArchiveFiles.writeJson(this,uri,payload);
                    outcome="BIOQRA "+(request==SAVE_SETTINGS?"settings":"results")+" exported.";
                }else if(request==RESTORE_SETTINGS){
                    JSONObject json=ArchiveFiles.readJson(this,uri);
                    settings.importSettings(json);
                    outcome="Settings restored. ChatGPT consent is disabled; reconnect if needed.";
                }else if(request==RESTORE_RESULTS){
                    JSONObject json=ArchiveFiles.readJson(this,uri);
                    int n=settings.importResults(json);
                    outcome=n+" saved results restored. Open each challenge to view them.";
                }else return;
            }catch(Exception e){outcome="Backup operation failed: "+e.getMessage();}
            final String message=outcome;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                backupStatus.setText(message);
                if(request==RESTORE_SETTINGS || request==IMPORT_ALL_ZIP){
                    consent.setChecked(settings.autoConsent());
                    populateModels(available);
                    updateRemaining();
                    int[] limits={1,3,5,10};
                    for(int i=0;i<limits.length;i++)
                        if(settings.dailyLimit()==limits[i])limitPicker.setSelection(i);
                }
            });
        });
    }

    @Override protected void onResume(){
        super.onResume();
        if(account!=null)updateAccountStatus();
    }

    private void updateAccountStatus(){
        if(accountStatus==null || accountButton==null)return;
        boolean connected=!"Not connected".equals(account.accountLabel());
        String scope=account.canUsePlan() ? "AI plan usage authorized"
                : "AI plan usage not authorized";
        accountStatus.setText("ChatGPT: "+account.accountLabel()
                +"\n"+(connected?scope+" · Session saved securely on this device. Automatic token renewal is used where supported.":"Log in once to connect this device."));
        accountButton.setText(signingIn?"Connecting..."
                : connected?"ChatGPT ✓ · Account & sign out":"ChatGPT login");
        accountButton.setEnabled(!signingIn);
    }

    private void showChatGptAccount(){
        if(signingIn)return;
        boolean connected=!"Not connected".equals(account.accountLabel());
        if(!connected){
            new AlertDialog.Builder(this)
                    .setTitle("Log in with ChatGPT")
                    .setMessage("BIOQRA will open the official ChatGPT authorization website "
                        +"in your browser. Do not enter your password inside BIOQRA.")
                    .setNegativeButton("Cancel",null)
                    .setPositiveButton("Continue with ChatGPT",(d,w)->startChatGptLogin())
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("ChatGPT account")
                .setMessage("Connected: "+account.accountLabel()
                    +"\nPlan use: "+(account.canUsePlan()?"authorized":"not authorized")
                    +"\n\nYou can reconnect or sign out locally.")
                .setPositiveButton("Close",null)
                .setNeutralButton("Reconnect",(d,w)->startChatGptLogin())
                .setNegativeButton("Sign out",(d,w)->
                    new AlertDialog.Builder(this)
                        .setTitle("Sign out of BIOQRA?")
                        .setMessage("This removes the stored local connection and disables "
                            +"automatic ChatGPT analysis. To revoke app access completely, "
                            +"use ChatGPT account settings.")
                        .setNegativeButton("Cancel",null)
                        .setPositiveButton("Sign out",(confirm,which)->{
                            account.signOut();
                            settings.setAutoConsent(false);
                            if(consent!=null)consent.setChecked(false);
                            populateModels(new ArrayList<>());
                            updateAccountStatus();
                            info.setText("Connect a ChatGPT account to load available models.");
                        }).show())
                .show();
    }

    private void startChatGptLogin(){
        if(signingIn)return;
        signingIn=true;
        updateAccountStatus();
        account.signIn(this,(success,message)->{
            signingIn=false;
            if(isFinishing()||isDestroyed())return;
            updateAccountStatus();
            if(success)loadModels();
            new AlertDialog.Builder(this)
                .setTitle("ChatGPT connection")
                .setMessage(message)
                .setPositiveButton("OK",null).show();
        });
    }

    private void updateRemaining(){
        if(remaining!=null)
            remaining.setText("Local AI attempts remaining today: "+settings.remaining());
    }
    private void populateModels(List<ChatGptConnection.Model> source){
        available=new ArrayList<>(source);
        modelKeys.clear();
        List<String> titles=new ArrayList<>();
        titles.add("Auto · highest-tier model available");
        modelKeys.add(AiPreferences.AUTO);
        for(ChatGptConnection.Model m:available){
            titles.add(m.title);modelKeys.add(m.slug);
        }
        String chosen=settings.globalModel();
        if(!modelKeys.contains(chosen)){
            titles.add("Unavailable saved global model: "+chosen);
            modelKeys.add(chosen);
        }
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
                    info.setText(got.size()+" models offered by the connected account. Auto prefers the highest-tier available model by name and reasoning mode; not an official capability ranking.");
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
