package com.blasllavero.bioqra;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

/** Plays the approved video bundled in the APK, centred at its original aspect ratio. */
public final class VideoActivity extends Activity {
    private VideoView video;
    private FrameLayout videoArea;
    private TextView errorMessage;
    private int videoWidth = 16;
    private int videoHeight = 9;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(24, 72, 91));
        getWindow().setNavigationBarColor(Color.BLACK);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(Color.rgb(12, 33, 43));
        screen.setPadding(dp(12), dp(12), dp(12), dp(12));
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

        videoArea = new FrameLayout(this);
        videoArea.setBackgroundColor(Color.rgb(12, 33, 43));
        screen.addView(videoArea, new LinearLayout.LayoutParams(-1, 0, 1));

        video = new VideoView(this);
        video.setKeepScreenOn(true);
        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                1, 1, Gravity.CENTER);
        videoArea.addView(video, videoParams);

        // Error explanation is hidden during normal playback.
        errorMessage = new TextView(this);
        errorMessage.setTextColor(Color.WHITE);
        errorMessage.setTextSize(16);
        errorMessage.setGravity(Gravity.CENTER);
        errorMessage.setVisibility(android.view.View.GONE);
        videoArea.addView(errorMessage, new FrameLayout.LayoutParams(
                -1, -2, Gravity.CENTER));

        setContentView(screen);

        // Match the available screen area without cropping, stretching or
        // leaving the video against the left edge (portrait and landscape).
        videoArea.addOnLayoutChangeListener((v, left, top, right, bottom,
                                              oldLeft, oldTop, oldRight, oldBottom) ->
                resizeVideo());

        Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + getPackageName() + "/" + R.raw.bioqra_app_description_en);
        video.setVideoURI(uri);
        MediaController controls = new MediaController(this);
        controls.setAnchorView(video);
        video.setMediaController(controls);
        video.setOnPreparedListener(player -> {
            if (player.getVideoWidth() > 0 && player.getVideoHeight() > 0) {
                videoWidth = player.getVideoWidth();
                videoHeight = player.getVideoHeight();
                resizeVideo();
            }
            player.setLooping(false);
            video.start();
        });
        video.setOnErrorListener((player, what, extra) -> {
            errorMessage.setText("Unable to play the included introduction video.");
            errorMessage.setVisibility(android.view.View.VISIBLE);
            return true;
        });
    }

    private void resizeVideo() {
        int maxWidth = videoArea.getWidth();
        int maxHeight = videoArea.getHeight();
        if (maxWidth <= 0 || maxHeight <= 0 || videoWidth <= 0 || videoHeight <= 0)
            return;
        double aspect = (double) videoWidth / videoHeight;
        int width = maxWidth;
        int height = (int) Math.round(width / aspect);
        if (height > maxHeight) {
            height = maxHeight;
            width = (int) Math.round(height * aspect);
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                Math.max(1, width), Math.max(1, height), Gravity.CENTER);
        video.setLayoutParams(params);
    }

    private int dp(int size) {
        return Math.round(size * getResources().getDisplayMetrics().density);
    }

    @Override protected void onPause() {
        if (video != null) video.pause();
        super.onPause();
    }
}
