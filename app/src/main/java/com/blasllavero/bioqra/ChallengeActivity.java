package com.blasllavero.bioqra;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.text.method.LinkMovementMethod;
import android.text.Layout;
import java.util.regex.Pattern;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.inputmethod.EditorInfo;
import android.widget.HorizontalScrollView;
import java.util.ArrayList;
import java.util.Locale;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    public static final String NUMBER="challenge_number";
    public static final String URL="challenge_url";
    public static final String MODE="challenge_mode";
    public static final String KIND="challenge_kind";
    public static final String AUTO_QUEUE="automatic_queue";
    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
    // PDF viewing must not wait behind a long-running research or ChatGPT operation.
    private static final ExecutorService PDF_READS=Executors.newFixedThreadPool(2);
    private String title, url, mode, kind;
    private int challengeNumber=0;
    private TextView status, output, statement, resolvedLink, deadlinesView, pdfFeedback;
    private Button viewSavedPdf, savePdf;
    private android.widget.ProgressBar pdfProgress;
    private TextView pdfProgressLabel;
    private int pdfOperations=0;
    private Uri lastSavedPdf;
    private Uri lastSavedPdfTree;
    private AiPreferences.SavedResult lastSavedPdfResult;
    private TextView sectionHeading, searchCounter, translationView;
    private Button translateButton;
    private boolean translating=false;
    private final Map<String,String> translatedSections=new HashMap<>();
    private final Set<String> attemptedAutomaticTranslations=new HashSet<>();
    private Button originalDocumentButton;
    private String activeDocumentUrl="";
    private PublicDatasetDiscovery.Report discoveredDatasets;
    private String datasetDiscoveryNote="";
    private TextView datasetStatus;
    private LinearLayout datasetResults;
    private boolean datasetSearchInProgress=false;
    private ResearchEvidenceArchive.Outcome evidenceOutcome;
    private static final int EXPORT_EVIDENCE_ZIP=7123;
    private TextView evidenceStatus;
    private EditText searchText;
    private LinearLayout searchPanel;
    private ScrollView pageScroll;
    private ChallengeSectionNavigator.Snapshot navigation;
    private String activeSection=ChallengeSectionNavigator.FULL;
    private final ArrayList<String> sectionHistory=new ArrayList<>();
    private int historyIndex=-1,searchPosition=-1;
    private final Set<String> loadedOfficialSections=new HashSet<>();
    private final Set<String> loadingOfficialSections=new HashSet<>();
    private Button backSectionButton,forwardSectionButton;
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
        t.setText(UiLanguage.tr(this,s));t.setTextColor(Color.rgb(19,57,70));t.setTextSize(size);
        if(bold)t.setTypeface(null,Typeface.BOLD);
        t.setPadding(0,dp(9),0,dp(9));
        return t;
    }
    private Button button(String name){
        Button b=new Button(this);b.setAllCaps(false);b.setText(UiLanguage.tr(this,name));
        return b;
    }
    /**
     * Preserve the active queued research Activity when the user views the
     * catalog. A normal finish() here destroyed the export owner and returned
     * RESULT_CANCELED, leaving the queue stalled even though it wasn't paused.
     */
    private void returnToChallengeList(){
        if(getIntent().getBooleanExtra(AUTO_QUEUE,false)
                && "automatic".equals(mode)
                && !aiPreferences.automaticExportCompleted(challengeKey)
                && !ProcessingControl.stopped()){
            android.content.Intent catalog=new android.content.Intent(this,MainActivity.class);
            catalog.addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    |android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(catalog);
            // Do not finish: the Activity continues export work while stopped.
            // This remains lifecycle-dependent; a future background worker is needed.
            return;
        }
        finish();
    }

    @Override public void onBackPressed(){
        returnToChallengeList();
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
        refreshPdfProgress();
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        connection=new ChatGptConnection(this);
        aiPreferences=new AiPreferences(this);
        title=getIntent().getStringExtra(TITLE);
        challengeNumber=Math.max(0,getIntent().getIntExtra(NUMBER,0));
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
        pageScroll=scroll;
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
        back.setOnClickListener(v->returnToChallengeList());
        root.addView(back,new LinearLayout.LayoutParams(-1,dp(52)));
        root.addView(text((challengeNumber>0?challengeNumber+". ":"")
                +UiLanguage.challengeTitle(this,title),23,true));
        LinearLayout processingControls=new LinearLayout(this);
        processingControls.setOrientation(LinearLayout.HORIZONTAL);
        for(String action:new String[]{"Pause processing","Stop processing","Resume processing"}){
            Button control=button(UiLanguage.tr(this,action));
            control.setAllCaps(false);
            LinearLayout.LayoutParams spacing=new LinearLayout.LayoutParams(0,dp(58),1);
            spacing.setMargins(dp(5),dp(5),dp(5),dp(5));
            processingControls.addView(control,spacing);
            control.setTag(action);
            ProcessingControl.styleButton(control,action);
            control.setOnClickListener(v->{
                if("Pause processing".equals(action))ProcessingControl.pause();
                else if("Stop processing".equals(action))ProcessingControl.stop();
                else{
                    ProcessingControl.resume();
                    if(!busy&&!datasetSearchInProgress&&pdfOperations==0
                            &&"automatic".equals(mode)&&!aiPreferences.automaticExportCompleted(challengeKey)){
                        if(aiPreferences.lastResult(challengeKey)!=null)recoverQueueResult();
                        else retrieve();
                    }
                }
                for(int i=0;i<processingControls.getChildCount();i++){
                    Button item=(Button)processingControls.getChildAt(i);
                    ProcessingControl.styleButton(item,(String)item.getTag());
                }
                status.setText(UiLanguage.tr(this,ProcessingControl.label()));
                refreshPdfProgress();
            });
        }
        root.addView(processingControls);
        root.addView(text(UiLanguage.tr(this,"Mode: ")
                +UiLanguage.tr(this,"automatic".equals(mode)?"Automatic (ChatGPT)":"Manual (no AI)")
                +UiLanguage.tr(this," · Read-only · No contest submissions"),14,false));
        root.addView(text(url,12,false));
        root.addView(text(UiLanguage.format(this,
                "AI model: configured for this challenge on the main list; otherwise inherits the global AI Settings model. Automatic use is limited to %d requests per day on this device.",
                aiPreferences.dailyLimit()),13,false));
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

        // Browser-like, read-only section navigation for the selected challenge.
        root.addView(createSectionNavigation());
        sectionHeading=text("Public statement (read-only preview)",18,true);
        root.addView(sectionHeading);
        resolvedLink=text("Detailed statement URL: not identified yet",12,false);
        resolvedLink.setTextIsSelectable(true);
        root.addView(resolvedLink);
        originalDocumentButton=button("📄 Open original document (PDF/web)");
        originalDocumentButton.setVisibility(View.GONE);
        originalDocumentButton.setOnClickListener(v->{
            if(TrustedChallengeReader.approved(activeDocumentUrl))
                startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(activeDocumentUrl)));
        });
        root.addView(originalDocumentButton,new LinearLayout.LayoutParams(-1,dp(52)));
        statement=text("No statement retrieved yet.",14,false);
        statement.setTextIsSelectable(true);
        statement.setLineSpacing(dp(5),1.1f);
        statement.setPadding(dp(5),dp(16),dp(5),dp(24));
        statement.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY);
        statement.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL);
        statement.setLinksClickable(true);
        statement.setMovementMethod(LinkMovementMethod.getInstance());
        root.addView(statement);
        translateButton=button("Translate official section with ChatGPT");
        translateButton.setOnClickListener(v->confirmSectionTranslation());
        root.addView(translateButton,new LinearLayout.LayoutParams(-1,dp(52)));
        translationView=text("Official source (original language). Translation is optional and requires permission.",13,false);
        translationView.setTextIsSelectable(true);
        translationView.setLineSpacing(dp(4),1.08f);
        root.addView(translationView);
        root.addView(text("Participation dates and supporting documents",18,true));
        deadlinesView=text("Not checked. Dates must be verified on the official organizer website.",13,false);
        deadlinesView.setTextIsSelectable(true);
        deadlinesView.setLineSpacing(dp(4),1.08f);
        root.addView(deadlinesView);

        root.addView(text("Open biomedical datasets",18,true));
        root.addView(text("Search official public repositories for this research challenge. Search results are not verified data matches; inspect study provenance, sample size, licensing, metadata and whether individual-level access needs approval.",13,false));
        Button searchDatasets=button("🔎 Search open data for this challenge");
        root.addView(searchDatasets,new LinearLayout.LayoutParams(-1,dp(54)));
        if("automatic".equals(mode))searchDatasets.setVisibility(View.GONE);
        datasetStatus=text("No live dataset search has been performed.",13,false);
        root.addView(datasetStatus);
        datasetResults=new LinearLayout(this);
        datasetResults.setOrientation(LinearLayout.VERTICAL);
        datasetResults.setVisibility(View.GONE);
        root.addView(datasetResults);
        searchDatasets.setOnClickListener(v->beginDatasetDiscovery(false));
        evidenceStatus=text("No research dataset files downloaded yet.",13,false);
        root.addView(evidenceStatus);
        Button exportEvidence=button("Export research evidence and proposal (ZIP)");
        root.addView(exportEvidence,new LinearLayout.LayoutParams(-1,dp(52)));
        if("automatic".equals(mode))exportEvidence.setVisibility(View.GONE);
        exportEvidence.setOnClickListener(v->{
            android.content.Intent intent=new android.content.Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_TITLE,
                "BIOQRA-"+ArchiveFiles.cleanName(title)+ "-evidence.zip");
            startActivityForResult(intent,EXPORT_EVIDENCE_ZIP);
        });

        root.addView(text("Public sample DNA/RNA data for local checks (optional)",16,true));
        sample=new EditText(this);
        sample.setHint("Paste public sample DNA/RNA sequence (no patient records)");
        sample.setTextSize(14);sample.setMinLines(3);sample.setMaxLines(8);
        sample.setGravity(Gravity.TOP);
        root.addView(sample,new LinearLayout.LayoutParams(-1,dp(112)));
        if("automatic".equals(mode))sample.setVisibility(View.GONE);

        offlineButton=button("2 · Run offline sequence checks");
        root.addView(offlineButton,new LinearLayout.LayoutParams(-1,dp(55)));
        if("automatic".equals(mode))offlineButton.setVisibility(View.GONE);
        offlineButton.setOnClickListener(v->{
            String result=LocalBioinformatics.inspect(sample.getText().toString());
            output.setText(result);
            status.setText("Local checks completed. No network/AI usage.");
        });

        aiButton=button(isFunding()
                ? "3 · Ask ChatGPT for a research proposal"
                : "3 · Ask ChatGPT for a solution proposal");
        root.addView(aiButton,new LinearLayout.LayoutParams(-1,dp(55)));
        if("automatic".equals(mode))aiButton.setVisibility(View.GONE);
        aiButton.setOnClickListener(v->confirmAI());
        String oldResult=aiPreferences.lastResult(challengeKey);
        output=text(oldResult!=null ? UiLanguage.tr(this,"Original saved result (language may differ from your selected language).")+"\n\n"+oldResult
                : isFunding() ? "No research proposal or independent validation yet."
                : "No solution proposal or independent verification yet.",14,false);
        output.setTextIsSelectable(true);
        root.addView(text("Results and limitations",18,true));
        LinearLayout pdfActions=new LinearLayout(this);
        pdfActions.setOrientation(LinearLayout.VERTICAL);
        output.setLineSpacing(dp(5),1.12f);
        output.setPadding(dp(5),dp(12),dp(5),dp(20));
        root.addView(output);
        renderProposal(output.getText().toString());
        savePdf=button("Save this challenge result as PDF");
        root.addView(savePdf,new LinearLayout.LayoutParams(-1,dp(52)));
        if("automatic".equals(mode))savePdf.setVisibility(View.GONE);
        savePdf.setOnClickListener(v->saveSavedPdf());
        viewSavedPdf=button("View PDF");
        viewSavedPdf.setVisibility(View.VISIBLE);
        viewSavedPdf.setOnClickListener(v->{
            if(lastSavedPdf!=null&&lastSavedPdfResult!=null
                    && aiPreferences.automaticExportCompleted(challengeKey)){
                ChallengePdfViewer.show(this,lastSavedPdf,lastSavedPdfTree,lastSavedPdfResult);
            }else recoverSavedPdf(true);
        });
        pdfProgressLabel=text("Preparing PDF and ZIP...",13,false);
        pdfProgressLabel.setVisibility(View.GONE);
        pdfProgress=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        pdfProgress.setIndeterminate(false);
        pdfProgress.setMax(100);
        pdfProgress.setProgress(aiPreferences.processingPercent(challengeKey));
        pdfProgress.setVisibility(View.GONE);
        pdfActions.addView(viewSavedPdf,new LinearLayout.LayoutParams(-1,dp(52)));
        // The processing indicator belongs at the very bottom of the result
        // section, after the PDF viewer action.
        pdfActions.addView(pdfProgressLabel,new LinearLayout.LayoutParams(-1,-2));
        pdfActions.addView(pdfProgress,new LinearLayout.LayoutParams(-1,dp(12)));
        pdfFeedback=text("PDFs save by default to Downloads/Bioqra.",13,false);
        root.addView(pdfFeedback);
        root.addView(pdfActions,new LinearLayout.LayoutParams(-1,-2));
        updatePdfFeedback();
        updateButtons();

        // Automatic mode retrieves the public challenge on opening.
        // Actual ChatGPT usage still requires consent to transmit each statement.
        if(aiPreferences.automaticExportCompleted(challengeKey)
                && !getIntent().getBooleanExtra(AUTO_QUEUE,false)){
            // Reviewing a completed challenge never regenerates files or reissues AI calls.
            inspectCompletedPdf();
        }else if("automatic".equals(mode) && getIntent().getBooleanExtra(AUTO_QUEUE,false)
                && aiPreferences.lastResult(challengeKey)!=null){
            recoverQueueResult();
        }else{
            if("automatic".equals(mode)){
                // Do not queue a second PDF recovery while AI research and
                // exports are already running; it would block queue completion.
                retrieve();
            }else recoverSavedPdf();
        }
    }

    private LinearLayout createSectionNavigation(){
        LinearLayout wrapper=new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        TextView hint=text("Navigate official challenge sections",15,true);
        wrapper.addView(hint);
        HorizontalScrollView horizontal=new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(true);
        LinearLayout controls=new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        backSectionButton=button("← Back");
        forwardSectionButton=button("Forward →");
        backSectionButton.setOnClickListener(v->stepHistory(-1));
        forwardSectionButton.setOnClickListener(v->stepHistory(1));
        controls.addView(backSectionButton,new LinearLayout.LayoutParams(dp(117),dp(48)));
        controls.addView(forwardSectionButton,new LinearLayout.LayoutParams(dp(130),dp(48)));
        Button fullText=button("Full text");
        fullText.setOnClickListener(v->selectSection(ChallengeSectionNavigator.FULL,true));
        controls.addView(fullText,new LinearLayout.LayoutParams(dp(112),dp(48)));
        Button searchToggle=button("🔍 Search");
        searchToggle.setContentDescription("Search within the current challenge document");
        controls.addView(searchToggle,new LinearLayout.LayoutParams(dp(119),dp(48)));
        searchToggle.setOnClickListener(v->{
            if(searchPanel==null)return;
            boolean open=searchPanel.getVisibility()!=View.VISIBLE;
            searchPanel.setVisibility(open?View.VISIBLE:View.GONE);
            if(open)searchText.requestFocus();
        });
        for(String key:ChallengeSectionNavigator.KEYS){
            Button tab=button(ChallengeSectionNavigator.label(key));
            tab.setSingleLine(false);
            tab.setMaxLines(2);
            tab.setEllipsize(null);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(14),dp(3),dp(14),dp(3));
            // The Related Content label needs more width in Catalan and
            // Spanish. Calculate a width from the actual localized text.
            int textWidth=(int)Math.ceil(tab.getPaint().measureText(tab.getText().toString()));
            int desired=Math.max(dp(125),textWidth+dp(42));
            int width=Math.min(dp(270),desired);
            controls.addView(tab,new LinearLayout.LayoutParams(width,dp(64)));
            tab.setOnClickListener(v->selectSection(key,true));
        }
        horizontal.addView(controls);
        wrapper.addView(horizontal,new LinearLayout.LayoutParams(-1,dp(76)));

        searchPanel=new LinearLayout(this);
        searchPanel.setOrientation(LinearLayout.HORIZONTAL);
        searchPanel.setGravity(Gravity.CENTER_VERTICAL);
        searchText=new EditText(this);
        searchText.setSingleLine(true);
        searchText.setTextSize(15);
        searchText.setHint("Search text in the current section");
        searchText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchPanel.addView(searchText,new LinearLayout.LayoutParams(0,dp(54),1));
        Button previous=button("◀");
        previous.setContentDescription("Previous match");
        searchPanel.addView(previous,new LinearLayout.LayoutParams(dp(52),dp(54)));
        Button next=button("▶");
        next.setContentDescription("Next match");
        searchPanel.addView(next,new LinearLayout.LayoutParams(dp(52),dp(54)));
        searchCounter=text("0/0",12,false);
        searchPanel.addView(searchCounter,new LinearLayout.LayoutParams(dp(57),dp(54)));
        previous.setOnClickListener(v->findMatch(-1));
        next.setOnClickListener(v->findMatch(1));
        searchText.setOnEditorActionListener((field,action,event)->{
            if(action==EditorInfo.IME_ACTION_SEARCH){findMatch(1);return true;}
            return false;
        });
        searchText.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                searchPosition=-1;
                highlightSearch(false);
            }
            public void afterTextChanged(android.text.Editable editable){}
        });
        searchPanel.setVisibility(View.GONE);
        wrapper.addView(searchPanel);
        updateNavigationButtons();
        return wrapper;
    }

    private void updateNavigationButtons(){
        if(backSectionButton!=null)backSectionButton.setEnabled(historyIndex>0);
        if(forwardSectionButton!=null)
            forwardSectionButton.setEnabled(historyIndex>=0
                    &&historyIndex<sectionHistory.size()-1);
    }
    private void selectSection(String key,boolean record){
        if(record && (historyIndex<0 || !key.equals(sectionHistory.get(historyIndex)))){
            while(sectionHistory.size()>historyIndex+1)
                sectionHistory.remove(sectionHistory.size()-1);
            sectionHistory.add(key);
            historyIndex=sectionHistory.size()-1;
        }
        activeSection=key;
        updateNavigationButtons();
        showSection(key);
    }
    private void stepHistory(int delta){
        int next=historyIndex+delta;
        if(next<0 || next>=sectionHistory.size())return;
        historyIndex=next;
        selectSection(sectionHistory.get(next),false);
    }
    /** Format a plain-text organizer excerpt without changing its wording or trusting HTML. */
    private void renderStatement(String content){
        if(content==null)content="";
        // The source text is unchanged; visual spans only improve readability.
        SpannableStringBuilder rich=new SpannableStringBuilder(content);
        String[] lines=content.split("\\n",-1);
        int offset=0;
        for(String line:lines){
            String trimmed=line.trim();
            int begin=offset+line.indexOf(trimmed.isEmpty()?"":trimmed);
            int end=begin+trimmed.length();
            boolean page=trimmed.matches("(?i)^(page\\s+\\d+\\s*/\\s*\\d+|cancer grand challenges)$");
            boolean heading=trimmed.matches("(?i)^((\\d+(?:\\.\\d+)*\\.?\\s+)?(summary of|introduction|application process|award|about |assessment criteria|challenge teams|contents|official links|application guidelines|research proposal|key dates|documents|additional official source).*)$")
                    || trimmed.matches("(?i)^\\d+(?:\\.\\d+)*\\.?\\s+[A-Z][^.!?]{2,85}$");
            boolean label=trimmed.endsWith(":") && trimmed.length()<95 && !trimmed.startsWith("http");
            if(end>begin && (page||heading||label)){
                rich.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),begin,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                rich.setSpan(new ForegroundColorSpan(Color.rgb(14,76,95)),begin,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if(heading && !page)rich.setSpan(new RelativeSizeSpan(1.12f),begin,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            offset+=line.length()+1;
        }
        java.util.regex.Matcher urls=Pattern.compile("https://[^\\s<>]+").matcher(content);
        while(urls.find()){
            int end=urls.end();
            while(end>urls.start() && ".,;)".indexOf(content.charAt(end-1))>=0)end--;
            String link=content.substring(urls.start(),end);
            if(TrustedChallengeReader.approved(link))
                rich.setSpan(new URLSpan(link),urls.start(),end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        statement.setText(rich,TextView.BufferType.SPANNABLE);
    }

    private void setDocumentLink(String link){
        activeDocumentUrl=TrustedChallengeReader.approved(link)?link:"";
        originalDocumentButton.setVisibility(activeDocumentUrl.isEmpty()?View.GONE:View.VISIBLE);
        originalDocumentButton.setText(UiLanguage.tr(this,TrustedChallengeReader.approvedPdf(activeDocumentUrl)
            ?"📄 Open original PDF with its layout":"🌐 Open original source in browser"));
    }


    private String translationKey(ChallengeSectionNavigator.Section section){
        return UiLanguage.language(this)+"|"+section.url+"|"
            +section.body.hashCode()+"|"+section.body.length();
    }

    private void updateTranslationView(){
        if(translationView==null || navigation==null)return;
        ChallengeSectionNavigator.Section current=navigation.get(activeSection);
        if(current==null){
            translationView.setText(UiLanguage.tr(this,
                "Official source (original language). Translation is optional and requires permission."));
            return;
        }
        String localized=translatedSections.get(translationKey(current));
        translationView.setText(localized!=null?localized:UiLanguage.tr(this,
            "Official source (original language). Translation is optional and requires permission."));
    }

    private void maybeAutoTranslateSection(){
        if(!aiPreferences.autoTranslatePublicSections()
                || !aiPreferences.automaticTranslationAuthorized() || translating || busy
                || navigation==null || !connection.canUsePlan() || aiPreferences.remaining()<=0)return;
        String language=UiLanguage.language(this);
        if(!"ca".equals(language) && !"es".equals(language))return;
        ChallengeSectionNavigator.Section selected=navigation.get(activeSection);
        if(selected==null || selected.body.length()<20 || selected.body.length()>10000
                || !TrustedChallengeReader.approved(selected.url))return;
        if(navigation.link(activeSection)!=null
                && !loadedOfficialSections.contains(activeSection))return;
        String key=translationKey(selected);
        if(translatedSections.containsKey(key) || !attemptedAutomaticTranslations.add(key))return;
        runSectionTranslation(navigation,activeSection,selected,
            "ca".equals(language)?"Catalan":"Spanish",language);
    }

    private void confirmSectionTranslation(){
        if(translating || busy || navigation==null)return;
        final ChallengeSectionNavigator.Snapshot originalSnapshot=navigation;
        final String sectionKey=activeSection;
        final ChallengeSectionNavigator.Section source=navigation.get(sectionKey);
        if(source==null || source.body.trim().length()<20){
            status.setText(UiLanguage.tr(this,"No official text is available for translation."));
            return;
        }
        if(!TrustedChallengeReader.approved(source.url)){
            status.setText(UiLanguage.tr(this,"This source is not approved for translation."));
            return;
        }
        if(source.body.length()>10000){
            status.setText(UiLanguage.tr(this,
                "This section is too long for the current translation preview."));
            return;
        }
        if(!connection.canUsePlan() || aiPreferences.remaining()<=0){
            status.setText(UiLanguage.tr(this,
                "Connect ChatGPT and check your daily request allowance."));
            return;
        }
        final String code=UiLanguage.language(this);
        final String language="ca".equals(code)?"Catalan":"es".equals(code)?"Spanish":"English";
        new AlertDialog.Builder(this)
            .setTitle(UiLanguage.tr(this,"Translate official section with ChatGPT"))
            .setMessage(UiLanguage.tr(this,"Send the PUBLIC official section to ChatGPT?")
                +"\n"+source.url+"\n"+source.body.length()+" characters"
                +"\n"+language+" · "+aiPreferences.remaining()+" requests remaining"
                +"\n"+UiLanguage.tr(this,"The original source stays unchanged."))
            .setNegativeButton(UiLanguage.tr(this,"Cancel"),null)
            .setPositiveButton(UiLanguage.tr(this,"Translate once"),
                (dialog,which)->runSectionTranslation(originalSnapshot,sectionKey,source,language,code))
            .show();
    }

    private void runSectionTranslation(ChallengeSectionNavigator.Snapshot snapshot,
                                        String key,ChallengeSectionNavigator.Section source,
                                        String language,String code){
        if(translating || busy)return;
        translating=true;
        if(translateButton!=null)translateButton.setEnabled(false);
        status.setText(UiLanguage.tr(this,"Translating official public text..."));
        EXEC.execute(()->{
            try{
                List<ChatGptConnection.Model> available=connection.models();
                AiPreferences.Selection selection=AiPreferences.choose(
                    available,aiPreferences.selected(challengeKey));
                if(!aiPreferences.reserve(false))
                    throw new Exception("Daily AI request limit reached");
                String translated=connection.translatePublicDocument(
                    selection.slug,source.url,source.body,language);
                final String originalUrl=source.url;
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    translating=false;translateButton.setEnabled(true);
                    if(navigation!=snapshot || !UiLanguage.language(this).equals(code))return;
                    translatedSections.put(translationKey(source),
                        UiLanguage.tr(this,"AI-generated translation — unverified")
                        +"\n"+originalUrl+"\n\n"+translated);
                    if(key.equals(activeSection))updateTranslationView();
                    status.setText(UiLanguage.tr(this,"Translation ready; verify against official source."));
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    translating=false;translateButton.setEnabled(true);
                    status.setText(UiLanguage.tr(this,"Translation unavailable: ")+e.getMessage());
                });
            }
        });
    }

    private void showSection(String key){
        if(navigation==null){
            status.setText(UiLanguage.tr(this,"First retrieve the official challenge statement."));
            return;
        }
        ChallengeSectionNavigator.Section selected=navigation.get(key);
        updateTranslationView();
        sectionHeading.setText(UiLanguage.tr(this,ChallengeSectionNavigator.label(key)));
        String link=navigation.link(key);
        if(selected!=null){
            renderStatement(selected.body);
            setDocumentLink(selected.url);
            resolvedLink.setText(UiLanguage.tr(this,"Official source: ")+selected.url);
        }else if(link==null){
            setDocumentLink(navigation.sourceUrl);
            renderStatement("This section was not identified in the public official "
                +"statement. BIOQRA will not invent missing challenge details. "
                +"Use the original organizer page for more information.");
            resolvedLink.setText(UiLanguage.tr(this,"Source: ")+navigation.sourceUrl);
        }else{
            renderStatement("Official link identified. Retrieving the public section...");
            setDocumentLink(link);
            resolvedLink.setText(UiLanguage.tr(this,"Official source to check: ")+link);
        }
        highlightSearch(false);
        maybeAutoTranslateSection();
        pageScroll.post(()->pageScroll.smoothScrollTo(0,sectionHeading.getTop()));
        if(link!=null && !loadedOfficialSections.contains(key)
                && !loadingOfficialSections.contains(key)){
            loadingOfficialSections.add(key);
            final ChallengeSectionNavigator.Snapshot snapshot=navigation;
            EXEC.execute(()->{
                try{
                    ChallengeSectionNavigator.Section loaded=
                            ChallengeSectionNavigator.follow(this,snapshot,key);
                    runOnUiThread(()->{
                        loadingOfficialSections.remove(key);
                        if(isFinishing()||isDestroyed()||navigation!=snapshot)return;
                        loadedOfficialSections.add(key);
                        if(key.equals(activeSection)){
                            if(loaded!=null){
                                renderStatement(loaded.body);
                                setDocumentLink(loaded.url);
                                resolvedLink.setText(UiLanguage.tr(this,"Official source: ")+loaded.url);
                            }else renderStatement(UiLanguage.tr(this,"This linked section is not accessible."));
                            highlightSearch(false);
                            maybeAutoTranslateSection();
                            pageScroll.post(()->pageScroll.smoothScrollTo(0,sectionHeading.getTop()));
                        }
                    });
                }catch(Exception e){
                    runOnUiThread(()->{
                        loadingOfficialSections.remove(key);
                        if(isFinishing()||isDestroyed()||navigation!=snapshot)return;
                        loadedOfficialSections.add(key);
                        if(key.equals(activeSection)){
                            String original=selected==null?"":selected.body+"\n\n";
                            renderStatement(original+"The official linked document could not be "
                                    +"read automatically: "+e.getMessage()+"\nSource: "+link);
                        }
                    });
                }
            });
        }
    }

    /** Highlight case-insensitive text matches without altering source data. */
    private void highlightSearch(boolean scrollToMatch){
        if(searchText==null || statement==null)return;
        String text=statement.getText().toString();
        String query=searchText.getText().toString().trim();
        if(query.isEmpty()){
            searchCounter.setText("0/0");
            return;
        }
        String haystack=text.toLowerCase(Locale.ROOT);
        String needle=query.toLowerCase(Locale.ROOT);
        ArrayList<Integer> found=new ArrayList<>();
        for(int from=0;from<haystack.length() && found.size()<400;){
            int i=haystack.indexOf(needle,from);
            if(i<0)break;
            found.add(i);from=i+Math.max(1,needle.length());
        }
        if(found.isEmpty()){
            searchCounter.setText("0/0");
            return;
        }
        if(searchPosition>=found.size())searchPosition=-1;
        SpannableStringBuilder marked=new SpannableStringBuilder(statement.getText());
        for(BackgroundColorSpan old:marked.getSpans(0,marked.length(),BackgroundColorSpan.class))
            marked.removeSpan(old);
        for(int i=0;i<found.size();i++){
            int start=found.get(i);
            marked.setSpan(new BackgroundColorSpan(i==searchPosition
                        ?Color.rgb(255,192,65):Color.rgb(255,238,165)),
                    start,start+needle.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        statement.setText(marked,TextView.BufferType.SPANNABLE);
        searchCounter.setText((searchPosition<0?0:searchPosition+1)+"/"+found.size());
        if(scrollToMatch && searchPosition>=0){
            final int position=found.get(searchPosition);
            statement.post(()->{
                if(statement.getLayout()==null)return;
                int line=statement.getLayout().getLineForOffset(
                    Math.min(position,statement.length()));
                int y=statement.getTop()+statement.getLayout().getLineTop(line);
                pageScroll.smoothScrollTo(0,Math.max(0,y-dp(105)));
            });
        }
    }
    private void findMatch(int direction){
        // Search the entire retrieved statement regardless of the section
        // currently displayed; navigate back to the original section with ←.
        if(navigation!=null && !ChallengeSectionNavigator.FULL.equals(activeSection))
            selectSection(ChallengeSectionNavigator.FULL,true);
        String query=searchText.getText().toString().trim();
        if(query.isEmpty()){searchCounter.setText("0/0");return;}
        String haystack=statement.getText().toString().toLowerCase(Locale.ROOT);
        String needle=query.toLowerCase(Locale.ROOT);
        int count=0;
        for(int i=0;(i=haystack.indexOf(needle,i))>=0 && count<400;i+=needle.length())
            count++;
        if(count==0){searchCounter.setText("0/0");return;}
        searchPosition=(searchPosition+direction+count)%count;
        highlightSearch(true);
    }

    /** Keep the stored draft unchanged while rendering its Markdown safely on screen. */
    private void renderProposal(String raw){
        output.setText(ResearchMarkdownFormatter.format(raw));
    }

    private void pdfNotice(String message){
        if(pdfFeedback!=null)pdfFeedback.setText(message);
        if(status!=null)status.setText(message);
        android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_LONG).show();
    }

    private void updatePdfFeedback(){
        if(pdfFeedback==null)return;
        String folder=aiPreferences.pdfTreeUri();
        pdfFeedback.setText(UiLanguage.tr(this,folder==null
            ?"PDFs save automatically to Downloads/Bioqra/YYMMDD-Project/."
            :"PDFs save to Bioqra/YYMMDD-Project/ inside your selected folder."));
    }

    @Override protected void onResume(){
        super.onResume();
        if(aiPreferences!=null && lastSavedPdf==null
                && !aiPreferences.automaticExportCompleted(challengeKey))updatePdfFeedback();
    }

    private void refreshPdfProgress(){
        if(pdfProgress==null)return;
        boolean active=pdfOperations>0 || ("automatic".equals(mode)
                && (busy || datasetSearchInProgress));
        // Same persisted stage and percentage as the main challenge list.
        int percent=aiPreferences.processingPercent(challengeKey);
        pdfProgress.setIndeterminate(false);
        pdfProgress.setMax(100);
        pdfProgress.setProgress(percent);
        if(active){
            boolean exporting=pdfOperations>0 || aiPreferences.processingStage(challengeKey)>=4;
            String message=UiLanguage.tr(this,exporting
                    ?"Preparing PDF and ZIP...":"Processing challenge...");
            pdfProgressLabel.setText(message+" · "+percent+"%");
        }
        pdfProgressLabel.setVisibility(active?View.VISIBLE:View.GONE);
        pdfProgress.setVisibility(active?View.VISIBLE:View.GONE);
        viewSavedPdf.setEnabled(!active);
        savePdf.setEnabled(!active);
    }
    private void beginPdfOperation(){
        if(isFinishing()||isDestroyed())return;
        pdfOperations++;
        refreshPdfProgress();
    }
    private void endPdfOperation(){
        if(isFinishing()||isDestroyed())return;
        pdfOperations=Math.max(0,pdfOperations-1);
        refreshPdfProgress();
    }

    private void verifyFinishedPdf(Uri pdf,Uri tree,AiPreferences.SavedResult record)throws Exception{
        android.os.ParcelFileDescriptor descriptor=SecurePdfAccess.open(this,pdf,tree,record);
        android.graphics.pdf.PdfRenderer renderer=null;
        try{
            renderer=new android.graphics.pdf.PdfRenderer(descriptor);
            if(renderer.getPageCount()<1||renderer.getPageCount()>150)
                throw new Exception("Invalid PDF page count");
            try(android.graphics.pdf.PdfRenderer.Page page=renderer.openPage(0)){
                android.graphics.Bitmap test=android.graphics.Bitmap.createBitmap(120,170,
                        android.graphics.Bitmap.Config.ARGB_8888);
                try{page.render(test,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);}
                finally{test.recycle();}
            }
        }finally{
            if(renderer!=null)renderer.close();else descriptor.close();
        }
    }
    private String evidenceOutcome(){
        if(!isFunding() && !"benchmark".equals(kind))return "";
        // The existence of authentic downloaded files is distinct from scientific
        // validation. Both states may accompany a successfully generated report.
        return ResearchEvidenceArchive.downloadedCount(this,challengeKey)>0
                ?"data_unvalidated":"no_research_data";
    }
    private void finishVerifiedQueue(String researchOutcome){
        aiPreferences.setProcessingOutcome(challengeKey,researchOutcome);
        aiPreferences.markAutomaticExportCompleted(challengeKey);
        refreshPdfProgress();
        if(getIntent().getBooleanExtra(AUTO_QUEUE,false)
                && !ProcessingControl.paused() && !ProcessingControl.stopped()){
            // PDF rendering and ZIP writing have both completed and been
            // verified before this callback. UI state must never be a gate
            // to the next item in the queue.
            setResult(RESULT_OK);
            finish();
        }
    }

    private void recoverQueueResult(){
        AiPreferences.SavedResult found=null;
        for(AiPreferences.SavedResult record:aiPreferences.savedResults())
            if(challengeKey.equals(record.key)){found=record;break;}
        if(found==null){showError("Saved result unavailable");return;}
        final AiPreferences.SavedResult record=found;
        aiPreferences.setProcessingStage(challengeKey,4);
        beginPdfOperation();
        EXEC.execute(()->{
            String error=null;
            Uri verifiedPdf=null;
            Uri verifiedTree=null;
            String researchOutcome=evidenceOutcome();
            try{
                String selected=aiPreferences.pdfTreeUri();
                Uri tree=selected==null?null:Uri.parse(selected);
                Uri pdf=ArchiveFiles.findSavedPdf(this,tree,record);
                if(pdf==null)pdf=ArchiveFiles.makePdf(this,tree,record);
                verifyFinishedPdf(pdf,tree,record);
                DefaultEvidenceStorage.export(this,challengeKey,record);
                verifiedPdf=pdf;verifiedTree=tree;
            }catch(Exception ex){error=ex.getMessage()==null?"Export failed":ex.getMessage();}
            final String failure=error;
            final Uri readyPdf=verifiedPdf, readyTree=verifiedTree;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                endPdfOperation();
                if(failure!=null){
                    aiPreferences.setProcessingOutcome(challengeKey,"export_error");
                    pdfNotice("Automatic queue paused: "+failure);return;
                }
                lastSavedPdf=readyPdf;lastSavedPdfTree=readyTree;lastSavedPdfResult=record;
                pdfNotice(UiLanguage.tr(this,"Processing completed")
                        +("no_research_data".equals(researchOutcome)?" · "+UiLanguage.tr(this,"No real research data downloaded"):""));
                finishVerifiedQueue(researchOutcome);
            });
        });
    }

    private void inspectCompletedPdf(){
        status.setText(UiLanguage.tr(this,"Processing completed"));
        pdfFeedback.setText(UiLanguage.tr(this,"Checking saved PDF..."));
        viewSavedPdf.setEnabled(false);
        final AiPreferences.SavedResult record=findSavedRecord();
        if(record==null){
            aiPreferences.invalidateAutomaticExport(challengeKey);
            pdfFeedback.setText(UiLanguage.tr(this,"Saved PDF unavailable"));
            viewSavedPdf.setEnabled(true);
            return;
        }
        final String location=aiPreferences.pdfTreeUri();
        PDF_READS.execute(()->{
            Uri pdf=null, tree=location==null?null:Uri.parse(location);
            String error=null;
            try{
                pdf=ArchiveFiles.findSavedPdf(this,tree,record);
                if(pdf==null)error="Saved PDF missing";
                else verifyFinishedPdf(pdf,tree,record);
            }catch(Exception e){error=e.getMessage();}
            final Uri found=pdf, selectedTree=tree;
            final String reason=error;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                if(reason!=null){
                    lastSavedPdf=null;
                    lastSavedPdfResult=null;
                    aiPreferences.invalidateAutomaticExport(challengeKey);
                    pdfFeedback.setText(UiLanguage.tr(this,"Saved PDF unavailable")+": "+reason);
                    status.setText(UiLanguage.tr(this,"PDF or ZIP export failed"));
                    viewSavedPdf.setEnabled(true);
                    return;
                }
                lastSavedPdf=found;lastSavedPdfTree=selectedTree;lastSavedPdfResult=record;
                pdfFeedback.setText(UiLanguage.tr(this,"Processing completed")
                    +" · "+DefaultPdfStorage.filename(record));
                refreshPdfProgress();
                viewSavedPdf.setEnabled(true);
            });
        });
    }

    private AiPreferences.SavedResult findSavedRecord(){
        for(AiPreferences.SavedResult record:aiPreferences.savedResults())
            if(challengeKey.equals(record.key))return record;
        return null;
    }

    private void recoverSavedPdf(){recoverSavedPdf(false);}

    private void recoverSavedPdf(boolean openWhenReady){
        AiPreferences.SavedResult found=null;
        for(AiPreferences.SavedResult stored:aiPreferences.savedResults())
            if(challengeKey.equals(stored.key)){found=stored;break;}
        if(found==null){
            if(openWhenReady)pdfNotice("No saved AI result for this challenge; PDF not available yet.");
            return;
        }
        final AiPreferences.SavedResult record=found;
        final String selected=aiPreferences.pdfTreeUri();
        beginPdfOperation();
        PDF_READS.execute(()->{
            Uri tree=selected==null?null:Uri.parse(selected);
            Uri recovered=null;
            String failure=null;
            try{
                recovered=ArchiveFiles.findSavedPdf(this,tree,record);
                if(recovered==null && (openWhenReady || "automatic".equals(mode))){
                    try{DefaultEvidenceStorage.export(this,challengeKey,record);}
                    catch(Exception zipError){android.util.Log.w("BIOQRA","Evidence ZIP export unavailable",zipError);}
                    recovered=ArchiveFiles.makePdf(this,tree,record);
                    try(android.os.ParcelFileDescriptor fd=
                            SecurePdfAccess.open(this,recovered,tree,record)){
                        if(fd==null)throw new SecurityException("PDF read permission unavailable");
                    }
                }
            }catch(Exception ex){recovered=null;failure=ex.getMessage();}
            final Uri pdf=recovered;
            final Uri destination=tree;
            final String error=failure;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                endPdfOperation();
                if(!openWhenReady && lastSavedPdf!=null)return;
                if(openWhenReady){lastSavedPdf=null;lastSavedPdfResult=null;}
                if(pdf!=null){
                    lastSavedPdf=pdf;lastSavedPdfTree=destination;
                    lastSavedPdfResult=record;
                    viewSavedPdf.setVisibility(View.VISIBLE);
                    pdfFeedback.setText(UiLanguage.tr(this,"View PDF")+": "+DefaultPdfStorage.filename(record));
                    if(openWhenReady)ChallengePdfViewer.show(this,pdf,destination,record);
                }else if(error!=null){
                    pdfFeedback.setText("PDF preview unavailable: "+error);
                }
            });
        });
    }

    private void saveSavedPdf(){
        AiPreferences.SavedResult record=null;
        for(AiPreferences.SavedResult r:aiPreferences.savedResults())
            if(challengeKey.equals(r.key)){record=r;break;}
        if(record==null){pdfNotice("No saved AI result for this challenge; PDF not created.");return;}
        final AiPreferences.SavedResult result=record;
        String folder=aiPreferences.pdfTreeUri();
        lastSavedPdf=null;
        aiPreferences.setProcessingStage(challengeKey,4);
        beginPdfOperation();
        pdfNotice("Saving PDF in Bioqra/"+DefaultPdfStorage.projectFolder(result)+"...");
        EXEC.execute(()->{
            String message;
            boolean zipSaved=false;
            Uri pdf=null;
            Uri tree=folder==null?null:Uri.parse(folder);
            try{
                try{DefaultEvidenceStorage.export(this,challengeKey,result);zipSaved=true;}
                catch(Exception zipError){android.util.Log.w("BIOQRA","Evidence ZIP export unavailable",zipError);}
                Uri saved=ArchiveFiles.makePdf(this,tree,result);
                try(android.os.ParcelFileDescriptor verified=SecurePdfAccess.open(this,saved,tree,result)){
                    if(verified==null)throw new SecurityException("Saved PDF cannot be read");
                }catch(Exception previewError){
                    throw new Exception("PDF saved but preview verification failed: "
                            +previewError.getMessage(),previewError);
                }
                pdf=saved;
                message="PDF SAVED: "+(folder==null
                    ?DefaultPdfStorage.relativeDirectory(result)
                    :"Selected folder/Bioqra/"+DefaultPdfStorage.projectFolder(result)+"/")
                    +DefaultPdfStorage.filename(result);
            }catch(Exception e){message=e.getMessage()!=null && e.getMessage().startsWith("PDF saved but preview")
                    ?e.getMessage():"PDF export failed: "+e.getMessage();}
            final String feedback=message;
            final Uri confirmed=pdf;
            final boolean confirmedZip=zipSaved;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                endPdfOperation();
                if(confirmed!=null){
                    if(confirmedZip)aiPreferences.markAutomaticExportCompleted(challengeKey);
                    lastSavedPdf=confirmed;lastSavedPdfTree=tree;
                    lastSavedPdfResult=result;
                    viewSavedPdf.setVisibility(View.VISIBLE);
                }
                pdfNotice(feedback);
            });
        });
    }

    private boolean isFunding() {
        return "funding".equals(kind) || "funded_research".equals(kind);
    }

    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=EXPORT_EVIDENCE_ZIP || result!=RESULT_OK || data==null
                || data.getData()==null)return;
        Uri target=data.getData();
        evidenceStatus.setText("Exporting evidence ZIP...");
        EXEC.execute(()->{
            String resultMessage;
            try{
                AiPreferences.SavedResult record=null;
                for(AiPreferences.SavedResult saved:aiPreferences.savedResults()){
                    if(challengeKey.equals(saved.key)){record=saved;break;}
                }
                ResearchEvidenceArchive.export(this,challengeKey,target,record);
                resultMessage="Evidence ZIP saved successfully to selected destination.";
            }catch(Exception e){resultMessage="Evidence export failed: "+e.getMessage();}
            final String feedback=resultMessage;
            runOnUiThread(()->{
                if(!isFinishing()&&!isDestroyed()){
                    evidenceStatus.setText(feedback);
                    android.widget.Toast.makeText(this,feedback,
                        android.widget.Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /** Live public metadata search, performed before AI in automatic mode. */
    private void beginDatasetDiscovery(boolean thenAi){
        if(datasetSearchInProgress||ProcessingControl.stopped()||ProcessingControl.paused())return;
        datasetSearchInProgress=true;
        aiPreferences.setProcessingStage(challengeKey,2);
        refreshPdfProgress();
        discoveredDatasets=null;
        datasetStatus.setText("Searching NCBI GEO for real study metadata...");
        datasetResults.removeAllViews();
        datasetResults.setVisibility(View.VISIBLE);
        EXEC.execute(()->{
            PublicDatasetDiscovery.Report found=null;
            String failure=null;
            try{found=PublicDatasetDiscovery.discover(title);}
            catch(Exception e){failure=e.getMessage();}
            final PublicDatasetDiscovery.Report report=found;
            final String error=failure;
            ResearchEvidenceArchive.Outcome acquired=null;
            String downloadError="";
            if("automatic".equals(mode) && report!=null){
                try{acquired=ResearchEvidenceArchive.acquire(this,challengeKey,
                    title,report,verifiedPageUrl.isEmpty()?url:verifiedPageUrl);}
                catch(Exception e){downloadError=e.getMessage();}
            }
            GeoMatrixQualityAnalyzer.Result initialQc=null;
            String qcError="";
            if("automatic".equals(mode) && acquired!=null && acquired.files>0){
                try{initialQc=GeoMatrixQualityAnalyzer.analyze(this,challengeKey);}
                catch(Exception e){qcError=e.getMessage();}
            }
            GeoGroupStatistics.Result initialComparison=null;
            String compareError="";
            if("automatic".equals(mode) && acquired!=null && acquired.files>0){
                try{initialComparison=GeoGroupStatistics.analyze(this,challengeKey);}
                catch(Exception e){compareError=e.getMessage();}
            }
            GeoCohortConcordance.Result initialConcordance=null;
            String concordanceError="";
            if("automatic".equals(mode) && initialComparison!=null){
                try{initialConcordance=GeoCohortConcordance.inspect(this,challengeKey);}
                catch(Exception e){concordanceError=e.getMessage();}
            }
            GeoExternalValidation.Result initialExternal=null;
            String externalError="";
            if("automatic".equals(mode) && initialComparison!=null){
                try{initialExternal=GeoExternalValidation.evaluate(this,challengeKey);}
                catch(Exception e){externalError=e.getMessage();}
            }
            final GeoExternalValidation.Result external=initialExternal;
            final String externalFailure=externalError;
            final GeoCohortConcordance.Result concordance=initialConcordance;
            final String concordanceFailure=concordanceError;
            final GeoGroupStatistics.Result comparison=initialComparison;
            final String comparisonError=compareError;
            final GeoMatrixQualityAnalyzer.Result quality=initialQc;
            final String qualityError=qcError;
            final ResearchEvidenceArchive.Outcome evidence=acquired;
            final String evidenceError=downloadError;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                datasetSearchInProgress=false;
                discoveredDatasets=report;
                evidenceOutcome=evidence;
                if(evidence!=null)evidenceStatus.setText(evidence.summary
                    +(quality==null?"":"\n"+quality.summary)
                    +(qualityError.isEmpty()?"":"\nQC failed: "+qualityError)
                    +(comparison==null?"":"\n"+comparison.summary)
                    +(comparisonError.isEmpty()?"":"\nComparison unavailable: "+comparisonError)
                    +(concordance==null?"":"\n"+concordance.summary)
                    +(concordanceFailure.isEmpty()?"":"\nCohort audit unavailable: "+concordanceFailure)
                    +(external==null?"":"\n"+external.summary)
                    +(externalFailure.isEmpty()?"":"\nExternal retest unavailable: "+externalFailure));
                else if(!evidenceError.isEmpty())evidenceStatus.setText("Evidence acquisition failed: "+evidenceError);
                else evidenceStatus.setText("No data files downloaded.");
                datasetDiscoveryNote=error==null?"":"GEO metadata search unavailable: "+error;
                datasetResults.removeAllViews();
                if(report!=null){
                    datasetStatus.setText("NCBI GEO: "+report.studies.size()
                        +" public study records found. Metadata only; data files not loaded.");
                    for(PublicDatasetDiscovery.Study study:report.studies){
                        datasetResults.addView(text(study.accession+" · "+study.title,15,true));
                        datasetResults.addView(text(study.url+"\n"+study.summary,12,false));
                        Button source=button("Open verified GEO study "+study.accession);
                        datasetResults.addView(source,new LinearLayout.LayoutParams(-1,dp(46)));
                        source.setOnClickListener(v->startActivity(
                            new Intent(Intent.ACTION_VIEW,Uri.parse(study.url))));
                    }
                }else{
                    datasetStatus.setText("No dataset loaded. "+datasetDiscoveryNote);
                }
                datasetResults.addView(text("Only bounded exploratory QC is performed on downloaded real GEO matrices. No differential expression, clinical prediction, or challenge solution is established.",13,false));
                if(thenAi && !ProcessingControl.stopped() && !ProcessingControl.paused() && connection.canUsePlan() && aiPreferences.autoConsent()
                        && !aiPreferences.alreadyAttempted(challengeKey) && specificPage)
                    chooseModelAndRun(true);
                refreshPdfProgress();
            });
        });
    }

    private void retrieve(){
        if(busy||ProcessingControl.stopped()||ProcessingControl.paused())return;
        if(!TrustedChallengeReader.approved(url)){
            showError("Blocked: URL is not a trusted HTTPS research domain. Open only after independent review.");
            return;
        }
        busy=true;
        aiPreferences.setProcessingStage(challengeKey,1);
        refreshPdfProgress();
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
                    renderStatement(result.text);
                    resolvedLink.setText("Selected public page: "+result.resolvedUrl);
                    loadedOfficialSections.clear();
                    loadingOfficialSections.clear();
                    translatedSections.clear();
                    attemptedAutomaticTranslations.clear();
                    navigation=ChallengeSectionNavigator.fromText(result.resolvedUrl,
                            result.text,result.deadlineSummary,result.documentLinks);
                    selectSection(ChallengeSectionNavigator.FULL,true);
                    // Enrich the section index with original organizer links.
                    // Failure is nonfatal: the local formatted text remains usable.
                    EXEC.execute(()->{
                        try{
                            TrustedChallengeReader.Page page=
                                    TrustedChallengeReader.fetchPage(result.resolvedUrl);
                            ChallengeSectionNavigator.Snapshot extra=
                                    ChallengeSectionNavigator.fromPage(page,
                                        result.deadlineSummary,result.documentLinks);
                            runOnUiThread(()->{
                                if(!isFinishing()&&!isDestroyed()){
                                    navigation=extra;
                                    loadedOfficialSections.clear();
                                    loadingOfficialSections.clear();
                                    if(!ChallengeSectionNavigator.FULL.equals(activeSection))
                                        showSection(activeSection);
                                    else maybeAutoTranslateSection();
                                }
                            });
                        }catch(Exception ignored){}
                    });
                    StringBuilder sources=new StringBuilder(result.deadlineSummary);
                    if(!result.documentLinks.isEmpty()){
                        sources.append("\n\nPUBLIC SUPPORTING PDF DOCUMENTS (read-only):\n");
                        for(String doc:result.documentLinks)sources.append(doc).append("\n");
                    }
                    deadlinesView.setText(sources.toString());
                    status.setText(result.message+" Retrieved "+result.text.length()+" characters.");
                    busy=false;updateButtons();
                    maybeAutoTranslateSection();
                    if("automatic".equals(mode) && specificPage && !ProcessingControl.stopped() && !ProcessingControl.paused()){
                        // Search public metadata independently of AI consent.
                        boolean authorized=connection.canUsePlan() && aiPreferences.autoConsent()
                            && !aiPreferences.alreadyAttempted(challengeKey);
                        beginDatasetDiscovery(authorized);
                        if(!authorized)status.setText("Searching real public study metadata. "
                            +"AI proposal awaits consent or the next permitted attempt.");
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
        if(busy||ProcessingControl.stopped()||ProcessingControl.paused())return;
        if(unattended && (!aiPreferences.autoConsent()
                || aiPreferences.alreadyAttempted(challengeKey)))return;
        if(aiPreferences.remaining()<=0){
            showError("Local daily ChatGPT request limit reached. Change it in AI Settings or try tomorrow.");
            return;
        }
        busy=true;
        aiPreferences.setProcessingStage(challengeKey,3);
        refreshPdfProgress();
        status.setText("Checking your saved model selection...");
        updateButtons();
        final String publicStatement=fetchedText;
        final String officialContext=(navigation==null?"":navigation.fullText);
        final String researchSource=ChallengeTextFormatter.truncate(
                publicStatement.equals(officialContext)?publicStatement
                :publicStatement+"\n\nOfficial challenge page context:\n"+officialContext,22000);
        final String source=verifiedPageUrl;
        final String datasetEvidence=(discoveredDatasets==null
                ?"LIVE DATASET DISCOVERY: no verified study metadata available. "+datasetDiscoveryNote
                :discoveredDatasets.forModel())+"\n\nLOCAL FILE ACQUISITION: "
                +ResearchEvidenceArchive.summary(this,challengeKey);
        // Unattended requests never send typed user samples, even if present.
        final String input=unattended?"":sample.getText().toString();
        final long processingToken=ProcessingControl.token();
        EXEC.execute(()->{
            try{
                ProcessingControl.checkpoint(processingToken);
                List<ChatGptConnection.Model> offered=connection.models();
                AiPreferences.Selection selection=AiPreferences.choose(
                        offered,aiPreferences.selected(challengeKey));
                StringBuilder evidenceLog=new StringBuilder(datasetEvidence);
                java.util.Set<String> triedQueries=new java.util.HashSet<>();
                // At most two planning rounds, never consuming the last AI request
                // reserved for the final proposal. Further rounds require a future
                // explicit user-configured quota, not unbounded agent activity.
                for(int round=1;unattended && round<=2 && aiPreferences.remaining()>=2
                        && aiPreferences.autoConsent() && connection.canUsePlan();round++){
                    ProcessingControl.checkpoint(processingToken);
                    if(!aiPreferences.reserve(true))break;
                    final int currentRound=round;
                    try{
                        runOnUiThread(()->{
                            if(!isFinishing()&&!isDestroyed())
                                datasetStatus.setText("Research data planning pass "+currentRound+"/2...");
                        });
                        ResearchDataPlan plan=connection.planDatasetSearch(
                            selection.slug,title,publicStatement+"\n\n"+
                            ResearchReadiness.assess(this,challengeKey,discoveredDatasets).describe());
                        if(!triedQueries.add(plan.geoQuery.toLowerCase(Locale.ROOT))){
                            evidenceLog.append("\nRepeated search query; no further search.");
                            break;
                        }
                        PublicDatasetDiscovery.Report extra=
                            PublicDatasetDiscovery.discoverQuery(plan.geoQuery,title);
                        int before=ResearchEvidenceArchive.downloadedCount(this,challengeKey);
                        ResearchEvidenceArchive.Outcome acquired=
                            ResearchEvidenceArchive.acquire(this,challengeKey,title,extra,source);
                        int after=ResearchEvidenceArchive.downloadedCount(this,challengeKey);
                        ResearchEvidenceArchive.recordPlanning(this,challengeKey,round,
                            plan.geoQuery,before,after,
                            acquired.summary);
                        evidenceLog.append("\n\nPLANNING PASS ").append(round)
                            .append(" query: ").append(plan.geoQuery).append("\n")
                            .append(extra.forModel()).append("\n").append(acquired.summary);
                        final ResearchReadiness.Assessment assessment=
                            ResearchReadiness.assess(this,challengeKey,extra);
                        evidenceLog.append("\n").append(assessment.describe());
                        runOnUiThread(()->{
                            if(isFinishing()||isDestroyed())return;
                            discoveredDatasets=extra;
                            evidenceOutcome=acquired;
                            datasetStatus.setText("Planning pass "+currentRound+
                                " finished · evidence status: "+assessment.state.name());
                            evidenceStatus.setText(acquired.summary+"\n"+assessment.missing);
                        });
                        // No automatic scientific-readiness assertion is possible
                        // without assessing source variables and experimental design.
                        if(after<=before)break;
                    }catch(Exception planningError){
                        evidenceLog.append("\nPlanning pass ").append(round)
                            .append(" unavailable: ").append(planningError.getMessage());
                        break;
                    }
                }
                try{
                    GeoMatrixQualityAnalyzer.Result qc=GeoMatrixQualityAnalyzer.analyze(this,challengeKey);
                    evidenceLog.append("\n\nEXECUTED LOCAL GEO QUALITY INSPECTION:\n")
                        .append(qc.summary)
                        .append("\nThis is not a clinical result or an assessment of challenge success.");
                    runOnUiThread(()->{
                        if(!isFinishing()&&!isDestroyed())evidenceStatus.setText(qc.summary);
                    });
                }catch(Exception e){
                    evidenceLog.append("\n\nLocal GEO quality inspection not completed: ")
                        .append(e.getMessage());
                }
                try{
                    GeoGroupStatistics.Result compared=GeoGroupStatistics.analyze(this,challengeKey);
                    evidenceLog.append("\n\nEXECUTED OPTIONAL EXPLORATORY GROUP COMPARISON:\n")
                        .append(compared.summary)
                        .append("\nDetails and rejected studies are preserved in research/group-comparison.json.");
                    runOnUiThread(()->{
                        if(!isFinishing()&&!isDestroyed())evidenceStatus.setText(compared.summary);
                    });
                }catch(Exception e){
                    evidenceLog.append("\n\nExploratory group comparison unavailable: ")
                        .append(e.getMessage());
                }
                try{
                    GeoCohortConcordance.Result concordance=GeoCohortConcordance.inspect(this,challengeKey);
                    evidenceLog.append("\n\nCROSS-STUDY PROBE CONCORDANCE AUDIT:\n")
                        .append(concordance.summary)
                        .append("\nStudy pair checks saved in research/cohort-concordance.json.");
                }catch(Exception e){
                    evidenceLog.append("\n\nCross-study concordance audit unavailable: ")
                        .append(e.getMessage());
                }
                try{
                    GeoExternalValidation.Result external=GeoExternalValidation.evaluate(this,challengeKey);
                    evidenceLog.append("\n\nFROZEN-CANDIDATE EXTERNAL COHORT RETEST:\n")
                        .append(external.summary)
                        .append("\nThe independent validation status is not established by this preliminary test.")
                        .append("\nEvidence stored in research/external-validation.json.");
                }catch(Exception e){
                    evidenceLog.append("\n\nExternal cohort retest unavailable: ")
                        .append(e.getMessage());
                }
                evidenceLog.append("\n\nFINAL DATA READINESS CHECK:\n")
                    .append(ResearchReadiness.assess(this,challengeKey,discoveredDatasets).describe());
                String liveEvidence=ChallengeTextFormatter.truncate(evidenceLog.toString(),6500);
                ProcessingControl.checkpoint(processingToken);
                if(!aiPreferences.reserve(unattended))
                    throw new Exception("Local limit reached or automatic permission disabled.");
                aiPreferences.markAttempted(challengeKey);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    status.setText("Using "+selection.label+"; preparing an unverified draft...");
                    renderProposal("AI analysis in progress. No scientific result validated.");
                });
                String answer=connection.respond(selection.slug,title,
                    "Source: "+source+"\n\n"+researchSource+"\n\n"+liveEvidence,input,kind);
                aiPreferences.storeResult(challengeKey,title,source,selection.label,answer);
                aiPreferences.setProcessingStage(challengeKey,4);
                ProcessingControl.checkpoint(processingToken);
                String pdfStatus="";
                Uri automaticallySavedPdf=null;
                boolean evidenceSaved=false;
                AiPreferences.SavedResult automaticallySavedRecord=null;
                String destination=aiPreferences.pdfTreeUri();
                if("automatic".equals(mode)){
                    AiPreferences.SavedResult record=null;
                    for(AiPreferences.SavedResult r:aiPreferences.savedResults()){
                        if(challengeKey.equals(r.key)){record=r;break;}
                    }
                    if(record==null){
                        pdfStatus=" · Result remains saved locally; PDF and ZIP could not be created.";
                    }else{
                        automaticallySavedRecord=record;
                        Uri selectedTree=destination==null?null:Uri.parse(destination);
                        try{
                            Uri created=ArchiveFiles.makePdf(this,selectedTree,record);
                            verifyFinishedPdf(created,selectedTree,record);
                            automaticallySavedPdf=created;
                            pdfStatus=" · PDF saved: "+(destination==null
                                ?DefaultPdfStorage.relativeDirectory(record)
                                :"Selected folder/Bioqra/"+DefaultPdfStorage.projectFolder(record)+"/")
                                +DefaultPdfStorage.filename(record);
                        }catch(Exception pdfError){
                            pdfStatus=" · PDF unavailable: "+pdfError.getMessage();
                        }
                        ProcessingControl.checkpoint(processingToken);
                        // An evidence archive should not depend on the PDF
                        // preview check succeeding. The saved proposal is preserved.
                        try{
                            DefaultEvidenceStorage.export(this,challengeKey,record);
                            evidenceSaved=true;
                            pdfStatus+=" · Research ZIP saved: "
                                +DefaultEvidenceStorage.relativeDirectory(record)
                                +DefaultEvidenceStorage.filename(record);
                        }catch(Exception evidenceError){
                            pdfStatus+=" · Research ZIP unavailable: "+evidenceError.getMessage();
                        }
                    }
                }else{
                    pdfStatus=" · Manual mode: use Save as PDF to export this draft.";
                }
                ProcessingControl.checkpoint(processingToken);
                final String researchOutcome=evidenceOutcome();
                final String pdfResult=pdfStatus;
                final Uri automaticPdf=automaticallySavedPdf;
                final boolean automaticEvidenceSaved=evidenceSaved;
                final AiPreferences.SavedResult automaticRecord=automaticallySavedRecord;
                final Uri automaticTree=destination==null?null:Uri.parse(destination);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    busy=false;
                    if(automaticPdf!=null && automaticRecord!=null){
                        lastSavedPdf=automaticPdf;lastSavedPdfTree=automaticTree;
                        lastSavedPdfResult=automaticRecord;
                        viewSavedPdf.setVisibility(View.VISIBLE);
                    }
                    renderProposal((isFunding()
                            ? "AI RESEARCH PROPOSAL — UNVERIFIED"
                            : "AI DRAFT — UNVERIFIED")
                        +"\nModel: "+selection.label+"\n\n"+answer
                        +"\n\nBIOQRA has not run any generated program or validated a benchmark.");
                    pdfNotice("AI draft saved locally for this challenge. "
                        +"Request limit remaining: "+aiPreferences.remaining()+pdfResult);
                    updateButtons();
                    if("automatic".equals(mode)){
                        if(automaticPdf!=null && automaticEvidenceSaved)finishVerifiedQueue(researchOutcome);
                        else aiPreferences.setProcessingOutcome(challengeKey,"export_error");
                    }
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
