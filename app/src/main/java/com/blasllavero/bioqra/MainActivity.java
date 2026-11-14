package com.blasllavero.bioqra;

import android.app.Activity;
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
        row("Cancer Research", "Refresh", () -> showSection("Cancer Research"), this::refresh);
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
        row("Challenges", "Hackathons", () -> showSection("Challenges"),
                () -> showSection("Hackathons"));
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

    private void showSection(String sectionName) {
        section = sectionName;
        search.setVisibility("Challenges".equals(section) ? View.VISIBLE : View.GONE);
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
            String query = search.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
            int displayed = 0;
            for (Challenge c : challenges) {
                if (!(c.title.toLowerCase(java.util.Locale.ROOT).contains(query) ||
                        c.id.toLowerCase(java.util.Locale.ROOT).contains(query))) continue;
                addLink(c.title, "Rosalind · " + c.id.toUpperCase(java.util.Locale.ROOT), c.url);
                displayed++;
                if (displayed >= 100) break;
            }
            if (displayed == 0) list.addView(text("No matching challenges in the current catalog.", 15, false));
            if (displayed == 100) list.addView(text("Showing first 100 matches. Narrow your search for more.", 13, false));
        } else if (section.equals("Cancer Research")) {
            list.addView(text("Research topics are not clinical treatments or confirmed cures.", 15, false));
            addLink("Cancer Grand Challenges", "Active research themes: prevention, tumor biology, and treatments", "https://www.cancergrandchallenges.org/challenges/active-challenges");
            addLink("DREAM Challenges", "Research competitions and biomedical prediction", "https://dreamchallenges.org/");
        } else if (section.equals("Hackathons")) {
            list.addView(text("Check organizers for current dates, registration and remote eligibility.", 15, false));
            addLink("BioHackathon Europe", "Organizer's project-based bioinformatics hackathon", "https://biohackathon-europe.org/");
            addLink("Bioinformatics Hackathon Directory", "Community event listing; verify each organizer", "https://biohackathons.github.io/");
        } else {
            list.addView(text("BIOQRA is an experimental open-source catalog of public bioinformatics challenges. It is not a medical device.\n\n"
                    + "Challenge links open in your browser. The app attempts to refresh the public Rosalind catalog at launch. If offline, it uses the saved copy.\n\n"
                    + "Conceived and programmed by Blas Llavero · blasllavero.com\n\n"
                    + "Multilingual accessibility, scientific solvers, patient features and videos are planned; they are not yet included.", 16, false));
        }
    }

    private void refresh() {
        if (status == null) return;
        status.setText("Refreshing Rosalind (5 tracks)...");
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
                runOnUiThread(() -> {
                    challenges.clear();
                    challenges.addAll(all.values());
                    status.setText(challenges.size() + " verified Rosalind challenges · updated now");
                    showItems();
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText(challenges.size() +
                        " saved challenges · refresh unavailable (" + e.getMessage() + ")"));
            }
        });
    }
}
