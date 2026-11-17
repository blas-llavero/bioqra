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
    private Button chatGptLoginButton;
    private ChatGptConnection chatGptConnection;
    private boolean chatGptSigningIn = false;
    private String challengeMode = "manual";
    private Button manualModeButton, automaticModeButton;
    private EditText search;
    private Spinner sourcePicker, availabilityPicker;
    private int currentPage = 0;
    private final List<ResearchCatalog.Item> researchItems = new ArrayList<>();
    private String section = "Challenges";

    private static class Challenge {
        final String id, title, url;
        Challenge(String id, String title, String url) {
            this.id = id; this.title = title; this.url = url;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BLUE);
        getWindow().setNavigationBarColor(Color.BLACK);
        chatGptConnection = new ChatGptConnection(this);
        challengeMode = getSharedPreferences("bioqra_mode", MODE_PRIVATE)
                .getString("mode","manual");
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
        t.setText(value); t.setTextSize(size); t.setTextColor(BLUE);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
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
        // Persistent account entry in the upper-right corner of the main screen.
        // The button starts the official browser-based Sign in with ChatGPT flow.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("BIOQRA", 30, true);
        title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(58), 1));
        chatGptLoginButton = button("ChatGPT login");
        chatGptLoginButton.setContentDescription("Log in with ChatGPT");
        chatGptLoginButton.setTextSize(13);
        header.addView(chatGptLoginButton, new LinearLayout.LayoutParams(dp(144), dp(52)));
        chatGptLoginButton.setOnClickListener(v -> showChatGptAccount());
        root.addView(header);
        updateChatGptButton();
        TextView subtitle = text("Open Bioinformatics Research & Patient Assistance", 13, false);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(subtitle);
        status = text("Loading verified public challenges...", 13, false);
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);
        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search challenges...");
        search.setTextSize(16);
        root.addView(search);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { currentPage = 0; showItems(); }
            public void afterTextChanged(Editable s) {}
        });
        sourcePicker = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                ResearchCatalog.FILTERS);
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
                android.R.layout.simple_spinner_item, ResearchCatalog.AVAILABILITY_FILTERS);
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
        if (!chatGptSigningIn) updateChatGptButton();
    }

    private void updateChatGptButton() {
        if (chatGptLoginButton == null || chatGptConnection == null) return;
        if (chatGptSigningIn) {
            chatGptLoginButton.setText("Connecting...");
            chatGptLoginButton.setEnabled(false);
        } else {
            boolean connected = !"Not connected".equals(chatGptConnection.accountLabel());
            chatGptLoginButton.setText(connected ? "ChatGPT ✓" : "ChatGPT login");
            chatGptLoginButton.setContentDescription(connected
                    ? "ChatGPT connected. Open account options."
                    : "Log in with ChatGPT");
            chatGptLoginButton.setEnabled(true);
        }
    }

    private void startChatGptSignIn() {
        if (chatGptSigningIn) return;
        chatGptSigningIn = true;
        updateChatGptButton();
        chatGptConnection.signIn(this, (success, message) -> {
            chatGptSigningIn = false;
            updateChatGptButton();
            if (isFinishing() || isDestroyed()) return;
            new AlertDialog.Builder(this)
                    .setTitle("ChatGPT connection")
                    .setMessage(message + "\n\nBIOQRA will not submit solutions to external contests automatically.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void showChatGptAccount() {
        if (chatGptSigningIn) return;
        String account = chatGptConnection.accountLabel();
        boolean connected = !"Not connected".equals(account);
        if (!connected) {
            new AlertDialog.Builder(this)
                    .setTitle("Connect with ChatGPT")
                    .setMessage("Sign in using the official ChatGPT website in your browser. "
                            + "BIOQRA never asks for your password or reads your conversations.\n\n"
                            + "Eligible users can separately authorize AI requests using their ChatGPT plan. "
                            + "Usage is subject to plan limits and your approval.")
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Continue with ChatGPT",
                            (dialog, which) -> startChatGptSignIn())
                    .show();
            return;
        }
        String permission = chatGptConnection.canUsePlan()
                ? "ChatGPT plan use: authorized"
                : "ChatGPT plan use: not authorized";
        new AlertDialog.Builder(this)
                .setTitle("ChatGPT account")
                .setMessage("Connected account: " + account + "\n" + permission
                        + "\n\nYou can reconnect to update authorization, or sign out of BIOQRA.")
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Reconnect", (dialog, which) -> startChatGptSignIn())
                .setNegativeButton("Sign out", (dialog, which) ->
                        new AlertDialog.Builder(this)
                            .setTitle("Sign out from BIOQRA?")
                            .setMessage("BIOQRA will stop using this account. "
                                    + "You can also manage app permissions in ChatGPT settings.")
                            .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                            .setPositiveButton("Sign out", (d, w) -> {
                                chatGptConnection.signOut();
                                updateChatGptButton();
                            }).show())
                .show();
    }

    private void setChallengeMode(String next) {
        if ("automatic".equals(next) && !chatGptConnection.canUsePlan()) {
            new AlertDialog.Builder(this)
                .setTitle("Connect ChatGPT first")
                .setMessage("Automatic mode requires an eligible ChatGPT account with permission to use plan capacity. Sign in using ChatGPT login above. Manual mode always works without AI.")
                .setPositiveButton("OK",null).show();
            return;
        }
        challengeMode = "automatic".equals(next) ? "automatic" : "manual";
        getSharedPreferences("bioqra_mode",MODE_PRIVATE).edit()
                .putString("mode",challengeMode).apply();
        updateModeControls();
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
            status.setText("Mode: "+("automatic".equals(challengeMode)
                    ? "Automatic — AI draft after explicit consent" : "Manual — offline tools and no AI usage"));
    }

    private void openChallenge(String title, String url, String kind){
        Intent intent=new Intent(this,ChallengeActivity.class);
        intent.putExtra(ChallengeActivity.TITLE,title);
        intent.putExtra(ChallengeActivity.URL,url);
        intent.putExtra(ChallengeActivity.MODE,challengeMode);
        intent.putExtra(ChallengeActivity.KIND,kind);
        startActivity(intent);
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
        addLink(title, description, url, "directory");
    }

    private void addLink(String title, String description, String url, String kind) {
        TextView t = text(title + "\n" + description, 16, false);
        t.setBackground(bg(Color.WHITE));
        t.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(8));
        list.addView(t, lp);
        t.setOnClickListener(v -> openChallenge(title,url,kind));
    }

    private static class ResultRow {
        final String title, description, url, kind;
        ResultRow(String title, String description, String url, String kind) {
            this.title=title; this.description=description; this.url=url; this.kind=kind;
        }
    }

    private void addResearch(List<ResultRow> rows, ResearchCatalog.Item item) {
        rows.add(new ResultRow(item.title, item.source+" · "+item.label()
                +"\n"+item.summary, item.url, item.kind));
    }

    private void displayPage(List<ResultRow> rows) {
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
        TextView summary=text("Showing "+(start+1)+"–"+end+" of "+total+
                " · Page "+(currentPage+1)+" / "+pageCount,14,true);
        summary.setPadding(0,dp(8),0,dp(12));
        list.addView(summary);
        for (int i=start; i<end; i++) {
            ResultRow row=rows.get(i);
            addLink(row.title,row.description,row.url,row.kind);
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
        heading.setText(section);
        List<ResultRow> rows=new ArrayList<>();
        if (section.equals("Challenges")) {
            String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);
            String source=(String) sourcePicker.getSelectedItem();
            if (source==null) source="All sources";
            String availability=(String) availabilityPicker.getSelectedItem();
            if (availability==null) availability="Open / Upcoming";

            // Real competitions, research benchmarks and education are separated.
            if (availability.equals("Educational exercises") || availability.equals("All records")) {
                if (source.equals("All sources") || source.equals(ResearchCatalog.ROSALIND)) {
                    for (Challenge challenge:challenges) {
                        String haystack=(challenge.title+" "+challenge.id+" Rosalind").toLowerCase(Locale.ROOT);
                        if(haystack.contains(query)) rows.add(new ResultRow(challenge.title,
                                "Rosalind · Educational exercise · "+challenge.id.toUpperCase(Locale.ROOT),
                                challenge.url, "educational"));
                    }
                }
            }
            for(ResearchCatalog.Item item:researchItems) {
                if(!source.equals("All sources") && !source.equals(item.source)) continue;
                String haystack=(item.title+" "+item.source+" "+item.summary+" "+item.topic)
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
                    status.setText("Official Grand Challenge index refreshed: "
                            + updated.size()+" accepting/upcoming entries · Rosalind and Open Problems available");
                    showItems();
                });
            } catch(Exception e) {
                runOnUiThread(() -> {
                    status.setText("Grand Challenge live search unavailable; saved list kept. "
                            + "Use Refresh to retry.");
                    showItems();
                });
            }
        });
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
