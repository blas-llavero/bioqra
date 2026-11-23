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
    private android.widget.CheckBox applyAll;
    private TextView info,remaining;

    private Button accountButton;
    private TextView accountStatus;
    private TextView backupStatus,pdfFolderStatus;
    private boolean signingIn=false;
    private List<ChatGptConnection.Model> available=new ArrayList<>();
    private List<String> modelKeys=new ArrayList<>();

    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String content,int size){
        TextView t=new TextView(this);
        t.setText(UiLanguage.tr(this,content));t.setTextSize(size);t.setTextColor(Color.rgb(24,72,91));
        t.setPadding(dp(2),dp(12),dp(2),dp(9));
        return t;
    }
    private Button button(String title){
        Button b=new Button(this);b.setText(UiLanguage.tr(this,title));b.setAllCaps(false);return b;
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
        TextView languageLabel=label(UiLanguage.tr(this,"Language"),17);
        root.addView(languageLabel);
        Spinner languagePicker=new Spinner(this);
        String[] codes={"system","ca","es","en"};
        String[] names={
            UiLanguage.tr(this,"Device language (automatic)"),
            "Català","Español","English"
        };
        ArrayAdapter<String> languageAdapter=new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item,names);
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languagePicker.setAdapter(languageAdapter);
        String preferred=UiLanguage.preference(this);
        for(int i=0;i<codes.length;i++)if(codes[i].equals(preferred))
            languagePicker.setSelection(i);
        languagePicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            private boolean initialized=false;
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){
                String next=codes[position];
                if(!initialized){initialized=true;return;}
                if(next.equals(UiLanguage.preference(AiSettingsActivity.this)))return;
                UiLanguage.setPreference(AiSettingsActivity.this,next);
                recreate();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        root.addView(languagePicker,new LinearLayout.LayoutParams(-1,dp(52)));
        android.widget.CheckBox automaticTranslation=new android.widget.CheckBox(this);
        automaticTranslation.setText(UiLanguage.tr(this,
            "Automatically translate public challenge sections (uses ChatGPT requests)"));
        automaticTranslation.setChecked(settings.autoTranslatePublicSections());
        automaticTranslation.setOnCheckedChangeListener((buttonView,checked)->{
            if(!checked){
                settings.setAutoTranslatePublicSections(false);
                return;
            }
            new AlertDialog.Builder(this)
                .setTitle(UiLanguage.tr(this,"Authorize automatic public translations?"))
                .setMessage(UiLanguage.tr(this,
                    "BIOQRA will send approved PUBLIC source sections to your connected ChatGPT account when viewed. Each uncached section can use one of your daily AI requests. Source originals stay unchanged. No patient data is transmitted."))
                .setNegativeButton(UiLanguage.tr(this,"Cancel"),(dialog,which)->
                    automaticTranslation.setChecked(false))
                .setOnCancelListener(dialog->automaticTranslation.setChecked(false))
                .setPositiveButton(UiLanguage.tr(this,"Enable"),(dialog,which)->
                    settings.setAutoTranslatePublicSections(true))
                .show();
        });
        root.addView(automaticTranslation);

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
        applyAll=new android.widget.CheckBox(this);
        applyAll.setText(UiLanguage.tr(this,"Apply this model to ALL challenges"));
        applyAll.setChecked(settings.applyModelToAll());
        root.addView(applyAll,new LinearLayout.LayoutParams(-1,-2));
        applyAll.setOnCheckedChangeListener((button,checked)->settings.setApplyModelToAll(checked));

        TextView controlTitle=label("Automatic Mode",19);
        controlTitle.setTypeface(null,Typeface.BOLD);
        root.addView(controlTitle);
        root.addView(label("One ChatGPT login authorizes automatic analysis of the public challenges you open. Research, eligible open-data downloads, proposal generation and PDF saving proceed without extra buttons. Private patient data and contest submissions are excluded.",14));
        // The official ChatGPT sign-in flow grants model-use permission.
        // No additional BIOQRA checkbox or repeated consent dialog is required.
        if(account.canUsePlan())settings.setAutoConsent(true);

        limitPicker=new Spinner(this);
        final int[] limits={1,3,5,10,20,40};
        String[] limitLabels={"1 attempt","3 attempts","5 attempts","10 attempts","20 attempts","40 attempts (default)"};
        ArrayAdapter<String> limitsAdapter=new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,limitLabels);
        limitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        limitPicker.setAdapter(limitsAdapter);
        // Existing local request cap remains active without requiring user setup.
        for(int i=0;i<limits.length;i++)if(settings.dailyLimit()==limits[i])
            limitPicker.setSelection(i);
        limitPicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){
                settings.setDailyLimit(limits[pos]);updateRemaining();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        remaining=label("",14);root.addView(remaining);updateRemaining();
        root.addView(label("A local limit of up to 40 ChatGPT attempts per day applies by default; provider plan restrictions also apply.",13));
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
        Button selectFolder=button("Optional: choose another PDF destination");
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
        root.addView(label("Default PDF location: Downloads/Bioqra/YYMMDD-Project/Project.pdf. "
                +"You can optionally choose another parent folder. New dates create separate project folders; "
                +"exporting twice on the same day updates that file. "
                +"No folder configuration is needed for the default location.",13));
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
                ? "Default PDF folder: Downloads/Bioqra/YYMMDD-Project/ (no setup needed)."
                : "Custom PDF folder: "+Uri.parse(folder).getLastPathSegment()+"/Bioqra/YYMMDD-Project/");
    }
    private void exportAllPdfs(){
        String folder=settings.pdfTreeUri();

        List<AiPreferences.SavedResult> saved=settings.savedResults();
        if(saved.isEmpty()){backupStatus.setText("No saved AI results to export.");return;}
        backupStatus.setText("Exporting "+saved.size()+" research PDFs...");
        WORKER.execute(()->{
            int count=0;String failure="";
            for(AiPreferences.SavedResult item:saved){
                try{ArchiveFiles.makePdf(this,folder==null?null:Uri.parse(folder),item);count++;}
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
                    populateModels(available);
                    applyAll.setChecked(settings.applyModelToAll());
                    updateRemaining();
                    int[] limits={1,3,5,10,20,40};
                    for(int i=0;i<limits.length;i++)
                        if(settings.dailyLimit()==limits[i])limitPicker.setSelection(i);
                }
            });
        });
    }

    @Override protected void onResume(){
        super.onResume();
        if(account!=null){
            if(account.canUsePlan())settings.setAutoConsent(true);
            updateAccountStatus();
        }
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
            startChatGptLogin();
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
            if(success && account.canUsePlan()){
                settings.setAutoConsent(true);
                getSharedPreferences("bioqra_mode",MODE_PRIVATE).edit()
                    .putString("mode","automatic").apply();
                loadModels();
                accountStatus.setText("ChatGPT connected. Automatic mode is ready. "
                    +"Open a challenge to start automatic research and PDF saving.");
            }else{
                new AlertDialog.Builder(this)
                    .setTitle("ChatGPT connection")
                    .setMessage(message)
                    .setPositiveButton("OK",null).show();
            }
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
