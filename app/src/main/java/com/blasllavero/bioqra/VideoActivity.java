package com.blasllavero.bioqra;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

/** Plays the user-approved local introduction; no external player or account required. */
public final class VideoActivity extends Activity {
    private VideoView video;

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

        TextView title = new TextView(this);
        title.setText("BIOQRA — App Description");
        title.setTextSize(21);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        screen.addView(title, new LinearLayout.LayoutParams(-1, dp(60)));

        video = new VideoView(this);
        video.setKeepScreenOn(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, 0, 1);
        screen.addView(video, params);

        TextView note = new TextView(this);
        note.setText("English narration · 30 seconds · Captions included in video");
        note.setTextColor(Color.WHITE);
        note.setGravity(Gravity.CENTER);
        note.setPadding(dp(4), dp(12), dp(4), 0);
        screen.addView(note);

        setContentView(screen);

        int resource = getResources().getIdentifier(
                "bioqra_app_description_en", "raw", getPackageName());
        if (resource == 0) {
            title.setText("Video not included in this build");
            note.setText("The approved video must be added to the Android project before compiling the APK.");
            return;
        }

        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + resource);
        video.setVideoURI(uri);
        MediaController controls = new MediaController(this);
        controls.setAnchorView(video);
        video.setMediaController(controls);
        video.setOnPreparedListener(player -> {
            player.setLooping(false);
            video.start();
        });
        video.setOnErrorListener((player, what, extra) -> {
            title.setText("Unable to play video");
            note.setText("Please check that the approved MP4 was packaged correctly.");
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
