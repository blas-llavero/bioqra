package com.blasllavero.bioqra;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import java.util.Locale;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int BLUE = Color.rgb(24, 72, 91);
    private static final int GREEN = Color.rgb(18, 121, 94);
    private static final String INDEX = "https://rosalind.info/problems/list-view/";
    private static final String[] TRACKS = {"", "python-village", "bioinformatics-armory",
            "bioinformatics-textbook-track", "algorithmic-heights"};
    private static final Pattern LINK = Pattern.compile(
            "<a\\b[^>]*href=[\\\"'](/problems/([a-z0-9]+)/?)[\\\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();

    private final List<Challenge> challenges = new ArrayList<>();
    private LinearLayout root, list;
    private ScrollView mainScroll;
    private TextView status, heading;
    private ChatGptConnection chatGptConnection;
    private AiPreferences aiPreferences;
    private final List<ChatGptConnection.Model> aiModels=new ArrayList<>();
    private boolean loadingModels=false;
    private String challengeMode = "automatic";
    private Button manualModeButton, automaticModeButton;
    private EditText search;
    private Spinner sourcePicker, availabilityPicker;
    private int currentPage = 0;
    private final List<ResearchCatalog.Item> researchItems = new ArrayList<>();
    private String section = "Challenges";
    private String lastLanguage="en";

    private static class Challenge {
        final String id, title, url;
        Challenge(String id, String title, String url) {
            this.id = id; this.title = title; this.url = url;
        }
    }

    public static final String OPEN_FIRST_CHALLENGE="open_first_challenge_from_settings";

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        if(intent.getBooleanExtra(OPEN_FIRST_CHALLENGE,false)){
            intent.removeExtra(OPEN_FIRST_CHALLENGE);
            openFirstDisplayedChallenge();
        }
    }

    private void openFirstDisplayedChallenge(){
        if(displayedRows.isEmpty())showItems();
        if(displayedRows.isEmpty()){
            status.setText(UiLanguage.tr(this,"No matching challenges. Change the filters or refresh."));
            return;
        }
        ResultRow first=displayedRows.get(0);
        // This is an explicit navigation request, not a request to restart the
        // entire automatic queue or consume another ChatGPT attempt.
        Intent detail=new Intent(this,ChallengeActivity.class);
        detail.putExtra(ChallengeActivity.TITLE,first.title);
        detail.putExtra(ChallengeActivity.NUMBER,1);
        detail.putExtra(ChallengeActivity.URL,first.url);
        detail.putExtra(ChallengeActivity.KIND,first.kind);
        detail.putExtra(ChallengeActivity.MODE,"manual");
        startActivity(detail);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        lastLanguage=UiLanguage.language(this);
        getWindow().setStatusBarColor(BLUE);
        getWindow().setNavigationBarColor(Color.BLACK);
        chatGptConnection = new ChatGptConnection(this);
        aiPreferences = new AiPreferences(this);
        challengeMode = getSharedPreferences("bioqra_mode", MODE_PRIVATE)
                .getString("mode","automatic");
        if(chatGptConnection.canUsePlan() && aiPreferences.autoConsent()
                && !"manual".equals(challengeMode))
            challengeMode="automatic";
        seed();
        researchItems.addAll(ResearchCatalog.initial());
        mergeGrand(GrandChallengeDiscovery.load(this));
        loadCached();
        buildUi();
        refresh();
        refreshGrand();
    }

    private void seed() {
        String[][] data = {
            {"dna", "Counting DNA Nucleotides"},
            {"rna", "Transcribing DNA into RNA"},
            {"revc", "Complementing a Strand of DNA"},
            {"gc", "Computing GC Content"},
            {"hamm", "Counting Point Mutations"},
            {"subs", "Finding a Motif in DNA"}
        };
        for (String[] item : data) challenges.add(new Challenge(item[0], item[1],
                "https://rosalind.info/problems/" + item[0] + "/"));
    }

    private void loadCached() {
        try {
            byte[] bytes = new byte[1024 * 1024];
            int size;
            try (InputStream input = openFileInput("rosalind.json")) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                while ((size = input.read(buffer)) != -1) output.write(buffer, 0, size);
                size = output.size();
                if (size > bytes.length) return;
                bytes = output.toByteArray();
            }
            JSONArray arr = new JSONArray(new String(bytes, StandardCharsets.UTF_8));
            if (arr.length() < 50) return;
            List<Challenge> cached = new ArrayList<>();
            for (int i=0; i<arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                cached.add(new Challenge(o.getString("id"), o.getString("title"), o.getString("url")));
            }
            challenges.clear();
            challenges.addAll(cached);
        } catch (Exception ignored) { /* use bundled starter catalog */ }
    }

    private GradientDrawable bg(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(12));
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(UiLanguage.tr(this,value)); t.setTextSize(size); t.setTextColor(BLUE);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(UiLanguage.tr(this,value));
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackground(bg(GREEN));
        b.setPadding(dp(8), dp(10), dp(8), dp(10));
        return b;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        mainScroll = scroll;
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(248, 251, 250));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(20));
        // Android system bars never overlap app controls.
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars;
            if (Build.VERSION.SDK_INT >= 30) {
                bars = insets.getInsets(WindowInsets.Type.systemBars());
                root.setPadding(dp(16), dp(16) + bars.top, dp(16), dp(20) + bars.bottom);
            }
            return insets;
        });
        scroll.addView(root);
        setContentView(scroll);
        TextView title = text("BIOQRA", 30, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(58)));
        TextView subtitle = text("Resolving bioinformatics challenges with AI to minimize the impact of treatments on patients", 13, false);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(subtitle);
        Button aiSettingsButton=button("⚙ AI Settings · models & automation");
        LinearLayout.LayoutParams aiSettingsLayout=new LinearLayout.LayoutParams(-1,dp(49));
        aiSettingsLayout.topMargin=dp(12);
        root.addView(aiSettingsButton,aiSettingsLayout);
        aiSettingsButton.setOnClickListener(v->startActivity(new Intent(this,AiSettingsActivity.class)));
        status = text("Loading verified public challenges...", 13, false);
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);
        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint(UiLanguage.tr(this,"Search challenges..."));
        search.setTextSize(16);
        root.addView(search);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { currentPage = 0; showItems(); }
            public void afterTextChanged(Editable s) {}
        });
        sourcePicker = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                localizedOptions(ResearchCatalog.FILTERS));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourcePicker.setAdapter(adapter);
        sourcePicker.setContentDescription("Filter bioinformatics challenges by source");
        root.addView(sourcePicker, new LinearLayout.LayoutParams(-1, dp(52)));
        sourcePicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                    int position, long id) { currentPage = 0; showItems(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        availabilityPicker = new Spinner(this);
        ArrayAdapter<String> availabilityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,localizedOptions(ResearchCatalog.AVAILABILITY_FILTERS));
        availabilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        availabilityPicker.setAdapter(availabilityAdapter);
        availabilityPicker.setContentDescription("Filter by competition availability");
        root.addView(availabilityPicker, new LinearLayout.LayoutParams(-1, dp(52)));
        availabilityPicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                    int position, long id) { currentPage = 0; showItems(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        row("Manual Mode", "Automatic Mode",
                () -> setChallengeMode("manual"),
                () -> setChallengeMode("automatic"));
        updateModeControls();
        LinearLayout controls=new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        for(String action:new String[]{"Pause processing","Stop processing","Resume processing"}){
            Button control=button(UiLanguage.tr(this,action));
            control.setAllCaps(false);
            LinearLayout.LayoutParams spacing=new LinearLayout.LayoutParams(0,dp(58),1);
            spacing.setMargins(dp(5),dp(5),dp(5),dp(5));
            controls.addView(control,spacing);
            control.setTag(action);
            ProcessingControl.styleButton(control,action);
            control.setOnClickListener(v->{
                if("Pause processing".equals(action))ProcessingControl.pause();
                else if("Stop processing".equals(action)){
                    ProcessingControl.stop();
                }else{
                    ProcessingControl.resume();
                    if("automatic".equals(challengeMode)){
                        if(automaticRows.isEmpty()){
                            automaticIndex=-1;
                            showItems();
                            startAutomaticQueue();
                        }else if(automaticIndex>=0 && automaticIndex<automaticRows.size()){
                            ResultRow pending=automaticRows.get(automaticIndex);
                            launchChallenge(pending.title,pending.url,pending.kind,true);
                        }
                    }
                }
                for(int i=0;i<controls.getChildCount();i++){
                    Button item=(Button)controls.getChildAt(i);
                    ProcessingControl.styleButton(item,(String)item.getTag());
                }
                status.setText(UiLanguage.tr(this,ProcessingControl.label()));
            });
        }
        root.addView(controls);
        row("Cancer Research", "Refresh", () -> showSection("Cancer Research"), () -> {
            refresh();
            refreshGrand();
        });
        row("Challenges", "Hackathons", () -> showSection("Challenges"),
                () -> showSection("Hackathons"));
        row("App Description", "How to Use",
                this::playApprovedIntroduction,
                () -> showVideoNotice("How to Use"));
        heading = text("Challenges", 22, true);
        heading.setPadding(0, dp(14), 0, dp(10));
        root.addView(heading);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        row("Home", "Help", () -> showSection("Challenges"), () -> showSection("Help"));
        showItems();
        refreshAiModels();
    }

    private void row(String left, String right, Runnable first, Runnable second) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, dp(12), 0, dp(4));
        Button a = button(left); Button b = button(right);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(56), 1);
        p.setMargins(0, 0, dp(6), 0);
        r.addView(a, p);
        LinearLayout.LayoutParams q = new LinearLayout.LayoutParams(0, dp(56), 1);
        q.setMargins(dp(6), 0, 0, 0);
        r.addView(b, q);
        a.setOnClickListener(v -> first.run()); b.setOnClickListener(v -> second.run());
        if("Manual Mode".equals(left) && "Automatic Mode".equals(right)) {
            manualModeButton=a; automaticModeButton=b;
            a.setAlpha("manual".equals(challengeMode)?1f:0.56f);
            b.setAlpha("automatic".equals(challengeMode)?1f:0.56f);
        }
        root.addView(r);
    }

    @Override protected void onResume() {
        super.onResume();
        if(list!=null){
            if(!lastLanguage.equals(UiLanguage.language(this))){
                recreate();return;
            }
            if(chatGptConnection!=null && chatGptConnection.canUsePlan()
                    && aiPreferences.autoConsent()
                    && "automatic".equals(getSharedPreferences("bioqra_mode",MODE_PRIVATE)
                        .getString("mode","automatic"))){
                challengeMode="automatic";
                updateModeControls();
            }
            if(chatGptConnection!=null && !chatGptConnection.canUsePlan()){
                aiModels.clear();
            }
            showItems();
            refreshAiModels();
            if(initialCatalogDiscoveryFinished)startDefaultAutomaticQueueOnce();
        }
    }

    private void setChallengeMode(String next) {
        challengeMode = "automatic".equals(next) ? "automatic" : "manual";
        getSharedPreferences("bioqra_mode",MODE_PRIVATE).edit()
                .putString("mode",challengeMode).apply();
        updateModeControls();
        if("automatic".equals(challengeMode)){
            showItems();
            if(chatGptConnection.canUsePlan() && aiPreferences.autoConsent()){
                initialAutomaticQueueStarted=true;
                startAutomaticQueue();
            }else if(!chatGptConnection.canUsePlan()){
                status.setText(UiLanguage.tr(this,
                    "Automatic mode selected. Connect ChatGPT in Settings to start research."));
                new AlertDialog.Builder(this)
                    .setTitle(UiLanguage.tr(this,"ChatGPT setup required"))
                    .setMessage(UiLanguage.tr(this,
                        "Automatic mode needs a connected and authorized ChatGPT account. Configure ChatGPT in AI Settings to start processing challenges."))
                    .setNegativeButton(UiLanguage.tr(this,"Not now"),null)
                    .setPositiveButton(UiLanguage.tr(this,"Open AI Settings"),(dialog,which)->
                        startActivity(new Intent(this,AiSettingsActivity.class)))
                    .show();
            }else status.setText(UiLanguage.tr(this,
                "Automatic mode selected. Authorize automatic AI processing in Settings to begin."));
        }else{
            automaticRows.clear();
            automaticIndex=-1;
        }
    }

    /** Start from the first eligible challenge in the current filtered list.
     * No user click on an individual challenge is required. */
    private void startAutomaticQueue(){
        if(!"automatic".equals(challengeMode)
                ||!chatGptConnection.canUsePlan()||!aiPreferences.autoConsent())return;
        if(automaticIndex>=0 && automaticIndex<automaticRows.size())return;
        ProcessingControl.resume();
        automaticRows.clear();
        if(!"Challenges".equals(section))showSection("Challenges");
        automaticRows.addAll(displayedRows);
        automaticIndex=-1;
        continueAutomaticQueue();
    }

    private void continueAutomaticQueue(){
        if(ProcessingControl.paused()||ProcessingControl.stopped()
                ||!"automatic".equals(challengeMode))return;
        while(++automaticIndex<automaticRows.size()){
            ResultRow next=automaticRows.get(automaticIndex);
            if(!TrustedChallengeReader.approved(next.url))continue;
            String key=AiPreferences.stableKey(next.title,next.title);
            if(aiPreferences.automaticExportCompleted(key))continue;
            if(aiPreferences.alreadyAttempted(key) && aiPreferences.lastResult(key)==null){
                status.setText(UiLanguage.format(this,"Automatic processing paused: previous attempt has no saved result for %s",UiLanguage.challengeTitle(this,next.title)));
                return;
            }
            if(aiPreferences.remaining()<=0 && aiPreferences.lastResult(key)==null){
                status.setText(UiLanguage.tr(this,"Automatic queue paused: daily request limit reached."));
                return;
            }
            status.setText(UiLanguage.format(this,"Automatic processing: %s",UiLanguage.challengeTitle(this,next.title)));
            launchChallenge(next.title,next.url,next.kind,true);
            return;
        }
        status.setText(UiLanguage.tr(this,"Automatic queue finished."));
        automaticRows.clear();
        automaticIndex=-1;
    }

    private void updateModeControls() {
        if(manualModeButton!=null && automaticModeButton!=null){
            manualModeButton.setAlpha("manual".equals(challengeMode)?1f:0.55f);
            automaticModeButton.setAlpha("automatic".equals(challengeMode)?1f:0.55f);
            manualModeButton.setContentDescription("Manual mode, no AI usage"+
                    ("manual".equals(challengeMode)?", selected":""));
            automaticModeButton.setContentDescription("Automatic mode, ChatGPT analysis"+
                    ("automatic".equals(challengeMode)?", selected":""));
        }
        // Use a short and readable confirmation in the existing status line.
        if(status!=null && status.getText().toString().startsWith("Mode:"))
            status.setText(UiLanguage.format(this,"Mode: %s",UiLanguage.tr(this,"automatic".equals(challengeMode)
                    ? "Automatic — public research starts when a challenge opens" : "Manual — offline tools and no AI usage")));
    }

    private void refreshAiModels() {
        if(loadingModels || !chatGptConnection.canUsePlan())return;
        loadingModels=true;
        WORKER.execute(()->{
            try{
                List<ChatGptConnection.Model> models=chatGptConnection.models();
                runOnUiThread(()->{
                    loadingModels=false;
                    aiModels.clear();aiModels.addAll(models);
                    showItems();
                });
            }catch(Exception e){
                runOnUiThread(()->{loadingModels=false;});
            }
        });
    }

    private final List<ResultRow> automaticRows=new ArrayList<>();
    private final List<ResultRow> displayedRows=new ArrayList<>();
    private int automaticIndex=-1;
    private boolean initialAutomaticQueueStarted=false;
    private boolean initialCatalogDiscoveryFinished=false;
    private static final int AUTOMATIC_CHALLENGE=8124;

    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=AUTOMATIC_CHALLENGE)return;
        if(ProcessingControl.paused()||ProcessingControl.stopped())return;
        if(!"automatic".equals(challengeMode)
                || !chatGptConnection.canUsePlan() || !aiPreferences.autoConsent()){
            automaticRows.clear();automaticIndex=-1;return;
        }
        // Android may cancel an Activity result on a lifecycle transition even
        // when the export checkpoint has already been persisted successfully.
        boolean savedComplete=automaticIndex>=0 && automaticIndex<automaticRows.size()
                && aiPreferences.automaticExportCompleted(AiPreferences.stableKey(
                    automaticRows.get(automaticIndex).title,automaticRows.get(automaticIndex).title));
        if(result==ChallengeActivity.RESULT_REVIEW_REQUIRED){
            // A documented source retrieval failure is not a completed result.
            // It can be skipped for this queue run but remains eligible for
            // explicit manual review and later, user-approved retry.
            status.setText(UiLanguage.tr(this,
                "Source unavailable: marked for manual review. Continuing with the next challenge."));
            showItems();
            continueAutomaticQueue();
            return;
        }
        if(result!=RESULT_OK && !savedComplete){
            // Never skip incomplete exports or attempt a second AI request silently.
            status.setText(UiLanguage.tr(this,
                "Automatic queue interrupted: the current challenge did not complete. Reopen it only after checking its saved state."));
            showItems();
            return;
        }
        showItems();
        continueAutomaticQueue();
    }

    private void openChallenge(String title, String url, String kind){
        String key=AiPreferences.stableKey(title,title);
        if(aiPreferences.automaticExportCompleted(key) || ProcessingControl.paused() || ProcessingControl.stopped()){
            Intent review=new Intent(this,ChallengeActivity.class);
            review.putExtra(ChallengeActivity.TITLE,title);
            review.putExtra(ChallengeActivity.NUMBER,numberFor(title,url));
            review.putExtra(ChallengeActivity.URL,url);
            review.putExtra(ChallengeActivity.KIND,kind);
            review.putExtra(ChallengeActivity.MODE,"manual");
            startActivity(review);
            return;
        }
        automaticRows.clear();automaticIndex=-1;
        if("automatic".equals(challengeMode) && "Challenges".equals(section)){
            automaticRows.addAll(displayedRows);
            for(int i=0;i<automaticRows.size();i++){
                ResultRow row=automaticRows.get(i);
                if(row.title.equals(title)&&row.url.equals(url)){automaticIndex=i;break;}
            }
        }
        launchChallenge(title,url,kind,automaticIndex>=0);
    }

    private void launchChallenge(String title,String url,String kind,boolean queue){
        Intent intent=new Intent(this,ChallengeActivity.class);
        intent.putExtra(ChallengeActivity.TITLE,title);
        intent.putExtra(ChallengeActivity.NUMBER,numberFor(title,url));
        intent.putExtra(ChallengeActivity.URL,url);
        intent.putExtra(ChallengeActivity.MODE,challengeMode);
        intent.putExtra(ChallengeActivity.KIND,kind);
        intent.putExtra(ChallengeActivity.AUTO_QUEUE,queue);
        if(queue)startActivityForResult(intent,AUTOMATIC_CHALLENGE);
        else startActivity(intent);
    }

    private void playApprovedIntroduction() {
        startActivity(new Intent(this, VideoActivity.class));
    }

    private void showVideoNotice(String videoType) {
        String description = videoType.equals("App Description")
                ? "This video will explain BIOQRA's mission: helping researchers explore and verify bioinformatics challenges, including cancer research, while making scientific knowledge more accessible."
                : "This video will demonstrate how to search challenges, refresh Rosalind data, open cancer research resources and navigate hackathon listings, using real screenshots from the application.";
        new AlertDialog.Builder(this)
                .setTitle(videoType + " — Video coming soon")
                .setMessage(description + "\n\nA narrated, captioned video of up to 30 seconds will be available after the project owner has reviewed and approved the media. No video is included in this build.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showSection(String sectionName) {
        section = sectionName;
        currentPage = 0;
        // Both the source filter and search apply throughout the research catalog.
        search.setVisibility("Challenges".equals(section) ? View.VISIBLE : View.GONE);
        sourcePicker.setVisibility("Challenges".equals(section) ? View.VISIBLE : View.GONE);
        availabilityPicker.setVisibility("Challenges".equals(section) ? View.VISIBLE : View.GONE);
        showItems();
    }

    private void addLink(String title, String description, String url) {
        addLink(title, description, url, "directory",0);
    }

    private void addLink(String title, String description, String url, String kind,int number) {
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(bg(Color.WHITE));
        card.setPadding(dp(14),dp(10),dp(14),dp(10));
        LinearLayout.LayoutParams outer=new LinearLayout.LayoutParams(-1,-2);
        outer.setMargins(0,dp(4),0,dp(8));
        list.addView(card,outer);
        TextView t=text((number>0?number+". ":"")+UiLanguage.challengeTitle(this,title)+"\n"+UiLanguage.translateNote(this,description),16,false);
        card.addView(t);
        t.setOnClickListener(v->openChallenge(title,url,kind));
        LinearLayout modelRow=new LinearLayout(this);
        modelRow.setOrientation(LinearLayout.HORIZONTAL);
        modelRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=text("AI model:",13,true);
        modelRow.addView(label,new LinearLayout.LayoutParams(dp(78),dp(48)));
        Spinner picker=new Spinner(this);
        List<String> names=new ArrayList<>();
        List<String> keys=new ArrayList<>();
        names.add(UiLanguage.tr(this,"Use global model"));keys.add(AiPreferences.AUTO);
        for(ChatGptConnection.Model model:aiModels){
            names.add(model.title);keys.add(model.slug);
        }
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        picker.setAdapter(adapter);
        String key=AiPreferences.stableKey(title,title);
        int stage=aiPreferences.processingStage(key);
        String[] stageLabels={"Processing pending","Challenge statement retrieved / in preparation",
                "Dataset search and analysis","AI proposal in preparation",
                "PDF and ZIP export pending","Processing completed"};
        TextView processing=text(UiLanguage.tr(this,stageLabels[Math.max(0,Math.min(5,stage))]),14,true);
        String outcome=aiPreferences.processingOutcome(key);
        if("review_required".equals(outcome)){
            processing.setText(UiLanguage.tr(this,"Manual review required — official statement unavailable"));
            processing.setTextColor(Color.rgb(170,91,32));
        }else if("export_error".equals(outcome)){
            processing.setText(UiLanguage.tr(this,"PDF or ZIP export failed"));
            processing.setTextColor(Color.rgb(160,48,48));
        }else processing.setTextColor(stage==5?GREEN:BLUE);
        card.addView(processing,new LinearLayout.LayoutParams(-1,-2));
        if(stage==5 && !"export_error".equals(outcome)){
            String evidenceNote="";
            if("insufficient".equals(outcome)){
                // Older releases incorrectly labelled every unvalidated dataset
                // insufficient. Recheck local files without repeating an AI call.
                evidenceNote=ResearchEvidenceArchive.downloadedCount(this,key)>0
                        ?"Research data downloaded; scientific validation pending"
                        :"No real research data downloaded";
            }else if("no_research_data".equals(outcome)){
                evidenceNote="No real research data downloaded";
            }else if("data_unvalidated".equals(outcome)){
                evidenceNote="Research data downloaded; scientific validation pending";
            }
            if(!evidenceNote.isEmpty()){
                TextView note=text(UiLanguage.tr(this,evidenceNote),13,false);
                note.setTextColor(Color.rgb(157,102,18));
                card.addView(note,new LinearLayout.LayoutParams(-1,-2));
            }
        }
        android.widget.ProgressBar progress=new android.widget.ProgressBar(
                this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(false);
        progress.setMax(100);
        progress.setProgress(aiPreferences.processingPercent(key));
        card.addView(progress,new LinearLayout.LayoutParams(-1,dp(12)));
        TextView percent=text(aiPreferences.processingPercent(key)+"%",12,true);
        percent.setGravity(Gravity.END);
        card.addView(percent);
        String override=aiPreferences.override(key);
        if(override!=null && !keys.contains(override)){
            names.add("Unavailable saved model: "+override);
            keys.add(override);
            adapter.notifyDataSetChanged();
        }
        int selected=override==null?0:keys.indexOf(override);
        picker.setSelection(Math.max(0,selected));
        picker.setEnabled(!aiModels.isEmpty() && !aiPreferences.applyModelToAll());
        picker.setContentDescription("Select ChatGPT model for "+title);
        picker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                    View view,int position,long id){
                if(position<0 || position>=keys.size())return;
                aiPreferences.setOverride(key,position==0?null:keys.get(position));
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
        });
        modelRow.addView(picker,new LinearLayout.LayoutParams(0,dp(48),1));
        card.addView(modelRow);
        Button open=button("Open challenge");
        card.addView(open,new LinearLayout.LayoutParams(-1,dp(42)));
        open.setOnClickListener(v->openChallenge(title,url,kind));
        if(aiPreferences.automaticExportCompleted(key)){
            Button viewPdf=button("View PDF");
            LinearLayout.LayoutParams pdfButtonLayout=new LinearLayout.LayoutParams(-1,dp(42));
            pdfButtonLayout.topMargin=dp(9);
            card.addView(viewPdf,pdfButtonLayout);
            viewPdf.setOnClickListener(v->showCompletedPdf(key));
        }
    }

    /** Open the completed research PDF directly from the catalog; no need
     * to visit the challenge screen to finish a previously exported item. */
    private void showCompletedPdf(String key){
        AiPreferences.SavedResult found=null;
        for(AiPreferences.SavedResult item:aiPreferences.savedResults()){
            if(key.equals(item.key)){found=item;break;}
        }
        if(found==null){
            status.setText(UiLanguage.tr(this,"Saved PDF unavailable"));
            return;
        }
        final AiPreferences.SavedResult saved=found;
        final String destination=aiPreferences.pdfTreeUri();
        status.setText(UiLanguage.tr(this,"Checking saved PDF..."));
        WORKER.execute(()->{
            Uri tree=destination==null?null:Uri.parse(destination);
            Uri pdf=null;
            String error=null;
            try{
                pdf=ArchiveFiles.findSavedPdf(this,tree,saved);
                if(pdf==null)throw new Exception("PDF file is missing");
                try(android.os.ParcelFileDescriptor descriptor=
                    SecurePdfAccess.open(this,pdf,tree,saved)){
                    if(descriptor==null)throw new Exception("PDF is not readable");
                }
            }catch(Exception e){error=e.getMessage();}
            final Uri verified=pdf, selectedTree=tree;
            final String failure=error;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                if(failure!=null){
                    aiPreferences.invalidateAutomaticExport(key);
                    status.setText(UiLanguage.tr(this,"Saved PDF unavailable")+": "+failure);
                    showItems();
                }else ChallengePdfViewer.show(this,verified,selectedTree,saved);
            });
        });
    }

    private static class ResultRow {
        final String title, description, url, kind;
        ResultRow(String title, String description, String url, String kind) {
            this.title=title; this.description=description; this.url=url; this.kind=kind;
        }
    }

    private void addResearch(List<ResultRow> rows, ResearchCatalog.Item item) {
        rows.add(new ResultRow(item.title, item.source+" · "+UiLanguage.researchStatus(this,item)
                +"\n"+UiLanguage.translateNote(this,item.summary), item.url, item.kind));
    }

    private String[] localizedOptions(String[] options){
        String[] result=new String[options.length];
        for(int i=0;i<options.length;i++)result[i]=UiLanguage.tr(this,options[i]);
        return result;
    }

    /** The visible 1-based number follows the filtered catalog and its pages. */
    private int numberFor(String title,String url){
        for(int i=0;i<displayedRows.size();i++){
            ResultRow item=displayedRows.get(i);
            if(item.title.equals(title)&&item.url.equals(url))return i+1;
        }
        for(int i=0;i<automaticRows.size();i++){
            ResultRow item=automaticRows.get(i);
            if(item.title.equals(title)&&item.url.equals(url))return i+1;
        }
        return 0;
    }

    private void displayPage(List<ResultRow> rows) {
        displayedRows.clear();displayedRows.addAll(rows);
        int total=rows.size();
        int pageCount=Math.max(1, (total + ResearchCatalog.PAGE_SIZE - 1)
                / ResearchCatalog.PAGE_SIZE);
        currentPage=Math.max(0,Math.min(currentPage,pageCount-1));
        if(total==0) {
            list.addView(text("No matching challenges. Change the filters or refresh.",15,false));
            return;
        }
        int start=currentPage * ResearchCatalog.PAGE_SIZE;
        int end=Math.min(total,start + ResearchCatalog.PAGE_SIZE);
        TextView summary=text(UiLanguage.tr(this,"Showing")+" "+(start+1)+"–"+end
                +" "+UiLanguage.tr(this,"of")+" "+total+" · "+UiLanguage.tr(this,"Page")
                +" "+(currentPage+1)+" / "+pageCount,14,true);
        summary.setPadding(0,dp(8),0,dp(12));
        list.addView(summary);
        for (int i=start; i<end; i++) {
            ResultRow row=rows.get(i);
            addLink(row.title,row.description,row.url,row.kind,i+1);
        }
        LinearLayout nav=new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(0,dp(12),0,dp(12));
        Button prev=button("Previous");
        Button next=button("Next");
        prev.setEnabled(currentPage>0);
        next.setEnabled(currentPage+1<pageCount);
        prev.setAlpha(prev.isEnabled() ? 1f : 0.45f);
        next.setAlpha(next.isEnabled() ? 1f : 0.45f);
        LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(0,dp(52),1);
        l.setMargins(0,0,dp(6),0);
        nav.addView(prev,l);
        LinearLayout.LayoutParams r=new LinearLayout.LayoutParams(0,dp(52),1);
        r.setMargins(dp(6),0,0,0);
        nav.addView(next,r);
        prev.setOnClickListener(v -> { currentPage--; showItems(); scrollToResults(); });
        next.setOnClickListener(v -> { currentPage++; showItems(); scrollToResults(); });
        list.addView(nav);
    }

    private void scrollToResults() {
        if (mainScroll != null) {
            mainScroll.post(() -> mainScroll.smoothScrollTo(0, heading.getTop()));
        }
    }

    private void showItems() {
        if (list == null) return;
        list.removeAllViews();
        heading.setText(UiLanguage.tr(this,section));
        List<ResultRow> rows=new ArrayList<>();
        if (section.equals("Challenges")) {
            String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);
            String source=ResearchCatalog.FILTERS[sourcePicker.getSelectedItemPosition()<0?0:sourcePicker.getSelectedItemPosition()];
            if (source==null) source="All sources";
            String availability=ResearchCatalog.AVAILABILITY_FILTERS[availabilityPicker.getSelectedItemPosition()<0?0:availabilityPicker.getSelectedItemPosition()];
            if (availability==null) availability="Open / Upcoming";

            // Real competitions, research benchmarks and education are separated.
            if (availability.equals("Educational exercises") || availability.equals("All records")) {
                if (source.equals("All sources") || source.equals(ResearchCatalog.ROSALIND)) {
                    for (Challenge challenge:challenges) {
                        String haystack=(challenge.title+" "+UiLanguage.challengeTitle(this,challenge.title)
                                +" "+challenge.id+" Rosalind").toLowerCase(Locale.ROOT);
                        if(haystack.contains(query)) rows.add(new ResultRow(challenge.title,
                                "Rosalind · Educational exercise · "+challenge.id.toUpperCase(Locale.ROOT),
                                challenge.url, "educational"));
                    }
                }
            }
            for(ResearchCatalog.Item item:researchItems) {
                if(!source.equals("All sources") && !source.equals(item.source)) continue;
                String haystack=(item.title+" "+UiLanguage.challengeTitle(this,item.title)
                        +" "+item.source+" "+item.summary+" "+UiLanguage.translateNote(this,item.summary)
                        +" "+item.topic)
                        .toLowerCase(Locale.ROOT);
                if(!haystack.contains(query)) continue;
                String state=item.availability();
                boolean include=false;
                if(availability.equals("Open / Upcoming"))
                    include=state.equals("OPEN") || state.equals("UPCOMING");
                else if(availability.equals("Research benchmarks"))
                    include=state.equals("BENCHMARK");
                else if(availability.equals("Needs verification"))
                    include=state.equals("CHECK");
                else if(availability.equals("All records"))
                    include=true;
                if(include) addResearch(rows,item);
            }
            TextView context=text("Current competitions are prioritized. Open / Upcoming is a dated snapshot, not a guarantee of registration. Use All records to include educational and historical entries.",13,false);
            context.setPadding(0,dp(8),0,dp(8));
            list.addView(context);
        } else if (section.equals("Cancer Research")) {
            list.addView(text("Priority: real cancer research opportunities. Projects already funded are hidden; use All records in Challenges to see historical projects. Confirm applications with each organizer.",15,false));
            for(ResearchCatalog.Item item:researchItems) {
                if(!item.topic.contains("cancer") && !ResearchCatalog.CANCER.equals(item.source)) continue;
                if(item.availability().equals("OPEN") || item.availability().equals("UPCOMING"))
                    addResearch(rows,item);
            }
        } else if (section.equals("Hackathons")) {
            list.addView(text("Showing confirmed open/upcoming opportunities only. Historical hackathons and unverified directories are available under Challenges → All records.",15,false));
            for(ResearchCatalog.Item item:researchItems) {
                if(!ResearchCatalog.HACK.equals(item.source) && !ResearchCatalog.ML.equals(item.source)) continue;
                if(item.availability().equals("OPEN") || item.availability().equals("UPCOMING"))
                    addResearch(rows,item);
            }
        } else {
            list.addView(text("BIOQRA is an experimental open-source research catalog, not a medical device. Challenge links open in your browser.\n\n"
                    +"The app checks Rosalind and Open Problems automatically. Competition availability is reviewed periodically and may change; always check registration with the official organizer.\n\n"
                    +"Conceived and programmed by Blas Llavero · blasllavero.com",16,false));
            return;
        }
        displayPage(rows);
    }

    private void mergeGrand(List<ResearchCatalog.Item> live) {
        if(live == null || live.isEmpty()) return;
        // The official live index supersedes curated Grand Challenge snapshots.
        // Keep all non-Grand sources unchanged and avoid duplicate challenge URLs.
        researchItems.removeIf(item -> ResearchCatalog.GRAND.equals(item.source));
        researchItems.addAll(live);
    }

    private void refreshGrand() {
        WORKER.execute(() -> {
            try {
                List<ResearchCatalog.Item> updated=GrandChallengeDiscovery.discover();
                GrandChallengeDiscovery.save(this,updated);
                runOnUiThread(() -> {
                    mergeGrand(updated);
                    status.setText(UiLanguage.format(this,"Official Grand Challenge index refreshed: %d accepting/upcoming entries · Rosalind and Open Problems available",updated.size()));
                    showItems();
                    startDefaultAutomaticQueueOnce();
                });
            } catch(Exception e) {
                runOnUiThread(() -> {
                    status.setText(UiLanguage.tr(this,
                        "Grand Challenge live search unavailable; saved list kept. Use Refresh to retry."));
                    showItems();
                    startDefaultAutomaticQueueOnce();
                });
            }
        });
    }

    /** Starts only after catalog refresh, with prior user authorization, once per screen launch. */
    private void startDefaultAutomaticQueueOnce(){
        initialCatalogDiscoveryFinished=true;
        if(initialAutomaticQueueStarted || !"automatic".equals(challengeMode)
                || !chatGptConnection.canUsePlan() || !aiPreferences.autoConsent()
                || ProcessingControl.paused() || ProcessingControl.stopped())return;
        initialAutomaticQueueStarted=true;
        startAutomaticQueue();
    }

    private void refresh() {
        if (status == null) return;
        status.setText("Refreshing Rosalind and Open Problems...");
        WORKER.execute(() -> {
            try {
                Map<String, Challenge> all = new LinkedHashMap<>();
                for (String track : TRACKS) {
                    String url = INDEX + (track.isEmpty() ? "" : "?location=" + track);
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(12000);
                    conn.setReadTimeout(15000);
                    conn.setInstanceFollowRedirects(false);
                    conn.setRequestProperty("User-Agent", "BIOQRA-Android/0.1");
                    if (conn.getResponseCode() != 200) throw new Exception("HTTP " + conn.getResponseCode());
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    try (InputStream input = conn.getInputStream()) {
                        byte[] chunk = new byte[8192]; int n;
                        while ((n = input.read(chunk)) > -1) {
                            data.write(chunk, 0, n);
                            if (data.size() > 2_000_000) throw new Exception("Response too large");
                        }
                    } finally { conn.disconnect(); }
                    Matcher m = LINK.matcher(data.toString("UTF-8"));
                    int count = 0;
                    while (m.find()) {
                        String id = m.group(2).toLowerCase(java.util.Locale.ROOT);
                        String title = android.text.Html.fromHtml(m.group(3).replaceAll("<[^>]+>", " "),
                                android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim().replaceAll("\\s+", " ");
                        if (title.length() < 4 || title.length() > 160) continue;
                        if (!all.containsKey(id)) all.put(id, new Challenge(id, title,
                                "https://rosalind.info/problems/" + id + "/"));
                        count++;
                    }
                    if (count == 0) throw new Exception("Unexpected official page format");
                }
                if (all.size() < 200) throw new Exception("Incomplete index: " + all.size());
                JSONArray array = new JSONArray();
                for (Challenge c : all.values()) {
                    JSONObject item = new JSONObject();
                    item.put("id", c.id); item.put("title", c.title); item.put("url", c.url);
                    array.put(item);
                }
                try (java.io.OutputStream output = openFileOutput("rosalind.json", MODE_PRIVATE)) {
                    output.write(array.toString().getBytes(StandardCharsets.UTF_8));
                }
                // Open Problems is checked independently. An unavailable official page
                // must not invalidate a successful Rosalind update.
                List<ResearchCatalog.Item> openUpdated = null;
                String openStatus = "Open Problems: reviewed catalog";
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(
                            "https://openproblems.bio/benchmarks/").openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setInstanceFollowRedirects(false);
                    if (conn.getResponseCode()!=200) throw new Exception("HTTP "+conn.getResponseCode());
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    try (InputStream input=conn.getInputStream()) {
                        byte[] buf=new byte[8192]; int n;
                        while ((n=input.read(buf))!=-1) {
                            data.write(buf,0,n);
                            if (data.size()>2_000_000) throw new Exception("Response too large");
                        }
                    } finally { conn.disconnect(); }
                    openUpdated=ResearchCatalog.parseOpenProblems(data.toString("UTF-8"));
                    openStatus="Open Problems: "+openUpdated.size()+" verified index links";
                } catch (Exception ignored) {
                    // Keep pre-reviewed entries; no unsupported real-time claims.
                }
                final List<ResearchCatalog.Item> updatedOpen = openUpdated;
                final String finalOpenStatus = openStatus;
                runOnUiThread(() -> {
                    challenges.clear();
                    challenges.addAll(all.values());
                    if (updatedOpen != null) {
                        researchItems.removeIf(item -> ResearchCatalog.OPEN.equals(item.source));
                        researchItems.addAll(updatedOpen);
                    }
                    status.setText(challenges.size() + " Rosalind challenges · " + finalOpenStatus);
                    showItems();
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText(challenges.size() +
                        " saved challenges · refresh unavailable (" + e.getMessage() + ")"));
            }
        });
    }
}
