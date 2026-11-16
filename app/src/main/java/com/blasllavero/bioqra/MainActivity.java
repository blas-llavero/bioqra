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
    private TextView status, heading;
    private EditText search;
    private Spinner sourcePicker;
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
        seed();
        researchItems.addAll(ResearchCatalog.initial());
        loadCached();
        buildUi();
        refresh();
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
        root.addView(title);
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
            public void onTextChanged(CharSequence s, int start, int before, int count) { showItems(); }
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
                    int position, long id) { showItems(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        row("Cancer Research", "Refresh", () -> showSection("Cancer Research"), this::refresh);
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
        root.addView(r);
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
                .setMessage(description + "\\n\\nA narrated, captioned video of up to 30 seconds will be available after the project owner has reviewed and approved the media. No video is included in this build.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showSection(String sectionName) {
        section = sectionName;
        // Both the source filter and search apply throughout the research catalog.
        search.setVisibility("Challenges".equals(section) ? View.VISIBLE : View.GONE);
        sourcePicker.setVisibility("Challenges".equals(section) ? View.VISIBLE : View.GONE);
        showItems();
    }

    private void addLink(String title, String description, String url) {
        TextView t = text(title + "\n" + description, 16, false);
        t.setBackground(bg(Color.WHITE));
        t.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(8));
        list.addView(t, lp);
        t.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
    }

    private void showItems() {
        if (list == null) return;
        list.removeAllViews();
        heading.setText(section);
        if (section.equals("Challenges")) {
            String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            String source = (String) sourcePicker.getSelectedItem();
            if (source == null) source = "All sources";
            int displayed = 0;
            int matched = 0;
            if ("All sources".equals(source) || ResearchCatalog.ROSALIND.equals(source)) {
                for (Challenge challenge : challenges) {
                    if (!(challenge.title.toLowerCase(Locale.ROOT).contains(query)
                            || challenge.id.toLowerCase(Locale.ROOT).contains(query)
                            || "rosalind".contains(query))) continue;
                    if (++matched <= 90) {
                        addLink(challenge.title, "Rosalind · Educational problem · "
                                + challenge.id.toUpperCase(Locale.ROOT), challenge.url);
                        displayed++;
                    }
                }
            }
            for (ResearchCatalog.Item item : researchItems) {
                if (!"All sources".equals(source) && !item.source.equals(source)) continue;
                String searchable = (item.title+" "+item.source+" "+item.summary+" "+item.topic)
                                    .toLowerCase(Locale.ROOT);
                if (!searchable.contains(query)) continue;
                matched++;
                if (displayed >= 100) continue;
                addLink(item.title, item.source+" · "+item.label()+"\n"+item.summary, item.url);
                displayed++;
            }
            if (matched == 0) list.addView(text("No matching challenges in the current catalog.",15,false));
            if (matched > displayed) list.addView(text("Showing "+displayed+" of "+matched+
                " results. Narrow your search or choose a source.",13,false));
        } else if (section.equals("Cancer Research")) {
            list.addView(text("Cancer research findings are not validated medical treatments. Check organizer dates and eligibility.", 15, false));
            for (ResearchCatalog.Item item : researchItems) {
                if (ResearchCatalog.CANCER.equals(item.source) || item.topic.contains("cancer")) {
                    addLink(item.title, item.source+" · "+item.label()+"\n"+item.summary, item.url);
                }
            }
        } else if (section.equals("Hackathons")) {
            list.addView(text("Check organizer dates, registration and online eligibility. Directory entries are not confirmed open events.", 15, false));
            for (ResearchCatalog.Item item : researchItems) {
                if (ResearchCatalog.HACK.equals(item.source) || ResearchCatalog.ML.equals(item.source)) {
                    addLink(item.title,item.source+" · "+item.label()+"\n"+item.summary,item.url);
                }
            }
        } else {
            list.addView(text("BIOQRA is an experimental open-source catalog of public bioinformatics challenges. It is not a medical device.\n\n"
                    + "Challenge links open in your browser. The app attempts to refresh the public Rosalind catalog at launch. If offline, it uses the saved copy.\n\n"
                    + "Conceived and programmed by Blas Llavero · blasllavero.com\n\n"
                    + "Multilingual accessibility, scientific solvers, patient features and videos are planned; they are not yet included.", 16, false));
        }
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
