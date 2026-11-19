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
    public static final String URL="challenge_url";
    public static final String MODE="challenge_mode";
    public static final String KIND="challenge_kind";
    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
    private String title, url, mode, kind;
    private TextView status, output, statement, resolvedLink, deadlinesView;
    private TextView sectionHeading, searchCounter;
    private Button originalDocumentButton;
    private String activeDocumentUrl="";
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
        root.addView(text("Participation dates and supporting documents",18,true));
        deadlinesView=text("Not checked. Dates must be verified on the official organizer website.",13,false);
        deadlinesView.setTextIsSelectable(true);
        deadlinesView.setLineSpacing(dp(4),1.08f);
        root.addView(deadlinesView);

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
        output.setLineSpacing(dp(5),1.12f);
        output.setPadding(dp(5),dp(12),dp(5),dp(20));
        root.addView(output);
        renderProposal(output.getText().toString());
        Button savePdf=button("Save this challenge result as PDF");
        root.addView(savePdf,new LinearLayout.LayoutParams(-1,dp(52)));
        savePdf.setOnClickListener(v->saveSavedPdf());
        updateButtons();

        // Automatic mode retrieves the public challenge on opening.
        // Actual ChatGPT usage still requires consent to transmit each statement.
        if("automatic".equals(mode))retrieve();
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
        backSectionButton=button("← Enrere");
        forwardSectionButton=button("Endavant →");
        backSectionButton.setOnClickListener(v->stepHistory(-1));
        forwardSectionButton.setOnClickListener(v->stepHistory(1));
        controls.addView(backSectionButton,new LinearLayout.LayoutParams(dp(117),dp(48)));
        controls.addView(forwardSectionButton,new LinearLayout.LayoutParams(dp(130),dp(48)));
        Button fullText=button("Full text");
        fullText.setOnClickListener(v->selectSection(ChallengeSectionNavigator.FULL,true));
        controls.addView(fullText,new LinearLayout.LayoutParams(dp(112),dp(48)));
        Button searchToggle=button("🔍 Cerca");
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
            int width=(key.equals(ChallengeSectionNavigator.DOCUMENTS)
                    ||key.equals(ChallengeSectionNavigator.RELATED))?dp(164):dp(125);
            controls.addView(tab,new LinearLayout.LayoutParams(width,dp(48)));
            tab.setOnClickListener(v->selectSection(key,true));
        }
        horizontal.addView(controls);
        wrapper.addView(horizontal,new LinearLayout.LayoutParams(-1,dp(58)));

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
        originalDocumentButton.setText(TrustedChallengeReader.approvedPdf(activeDocumentUrl)
            ?"📄 Open original PDF with its layout":"🌐 Open original source in browser");
    }

    private void showSection(String key){
        if(navigation==null){
            status.setText("First retrieve the official challenge statement.");
            return;
        }
        ChallengeSectionNavigator.Section selected=navigation.get(key);
        sectionHeading.setText(ChallengeSectionNavigator.label(key));
        String link=navigation.link(key);
        if(selected!=null){
            renderStatement(selected.body);
            setDocumentLink(selected.url);
            resolvedLink.setText("Official source: "+selected.url);
        }else if(link==null){
            setDocumentLink(navigation.sourceUrl);
            renderStatement("This section was not identified in the public official "
                +"statement. BIOQRA will not invent missing challenge details. "
                +"Use the original organizer page for more information.");
            resolvedLink.setText("Source: "+navigation.sourceUrl);
        }else{
            renderStatement("Official link identified. Retrieving the public section...");
            setDocumentLink(link);
            resolvedLink.setText("Official source to check: "+link);
        }
        highlightSearch(false);
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
                                resolvedLink.setText("Official source: "+loaded.url);
                            }else renderStatement("This linked section is not accessible.");
                            highlightSearch(false);
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
                    renderStatement(result.text);
                    resolvedLink.setText("Selected public page: "+result.resolvedUrl);
                    loadedOfficialSections.clear();
                    loadingOfficialSections.clear();
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
        final String officialContext=(navigation==null?"":navigation.fullText);
        final String researchSource=publicStatement.equals(officialContext)?publicStatement
                :publicStatement+"\n\nOfficial challenge page context:\n"+officialContext;
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
                    renderProposal("AI analysis in progress. No scientific result validated.");
                });
                String answer=connection.respond(selection.slug,title,
                    "Source: "+source+"\n\n"+researchSource,input,kind);
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
                    renderProposal((isFunding()
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
