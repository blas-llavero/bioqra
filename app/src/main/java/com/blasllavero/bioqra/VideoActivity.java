package com.blasllavero.bioqra;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

/**
 * The owner-approved introduction can play either from an APK resource or
 * from a user-selected local MP4. Nothing is uploaded or sent to a server.
 */
public final class VideoActivity extends Activity {
    private static final int PICK_INTRO = 431;
    private static final String PREFS = "bioqra_video";
    private static final String SAVED_URI = "approved_en_uri";
    private VideoView video;
    private TextView title, note;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(24, 72, 91));
        getWindow().setNavigationBarColor(Color.BLACK);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(Color.rgb(12, 33, 43));
        screen.setPadding(dp(12), dp(16), dp(12), dp(24));
        screen.setFitsSystemWindows(true);

        Button back = new Button(this);
        back.setText("Back to BIOQRA");
        back.setAllCaps(false);
        back.setOnClickListener(v -> finish());
        screen.addView(back, new LinearLayout.LayoutParams(-1, dp(54)));

        title = new TextView(this);
        title.setText("BIOQRA — App Description");
        title.setTextSize(21);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        screen.addView(title, new LinearLayout.LayoutParams(-1, dp(60)));

        video = new VideoView(this);
        video.setKeepScreenOn(true);
        screen.addView(video, new LinearLayout.LayoutParams(-1, 0, 1));

        note = new TextView(this);
        note.setTextColor(Color.WHITE);
        note.setGravity(Gravity.CENTER);
        note.setPadding(dp(4), dp(8), dp(4), dp(8));
        screen.addView(note);

        Button choose = new Button(this);
        choose.setText("Select approved English video (MP4)");
        choose.setAllCaps(false);
        choose.setOnClickListener(v -> chooseVideo());
        screen.addView(choose, new LinearLayout.LayoutParams(-1, dp(56)));

        setContentView(screen);
        loadAvailableVideo();
    }

    private void loadAvailableVideo() {
        // A reviewed video bundled in the APK takes priority. The optional
        // document picker enables testing without committing multimedia.
        int resource = getResources().getIdentifier(
                "bioqra_app_description_en", "raw", getPackageName());
        if (resource != 0) {
            play(Uri.parse("android.resource://" + getPackageName() + "/" + resource));
            return;
        }
        String saved = getPreferencesUri();
        if (saved != null) {
            play(Uri.parse(saved));
        } else {
            note.setText("No video bundled in this build. Select your approved BIOQRA MP4 from Downloads to watch it offline.");
        }
    }

    private String getPreferencesUri() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getString(SAVED_URI, null);
    }

    private void chooseVideo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/mp4");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_INTRO);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_INTRO || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null || !"content".equals(uri.getScheme())) {
            note.setText("Please select a local MP4 using Android's file picker.");
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(SAVED_URI, uri.toString()).apply();
            play(uri);
        } catch (SecurityException e) {
            note.setText("Access to the selected video was denied. Please try another local file.");
        }
    }

    private void play(Uri uri) {
        note.setText("English narration · 30 seconds · Captions included in video");
        video.stopPlayback();
        video.setVideoURI(uri);
        MediaController controls = new MediaController(this);
        controls.setAnchorView(video);
        video.setMediaController(controls);
        video.setOnPreparedListener(player -> {
            player.setLooping(false);
            video.start();
        });
        video.setOnErrorListener((player, what, extra) -> {
            note.setText("Unable to play this MP4. Use Select approved English video to choose a valid file.");
            return true;
        });
    }

    private int dp(int size) {
        return Math.round(size * getResources().getDisplayMetrics().density);
    }

    @Override protected void onPause() {
        if (video != null) video.pause();
        super.onPause();
    }
}
