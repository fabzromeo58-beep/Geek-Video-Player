package com.geekvplayer.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import java.util.Collections;

@UnstableApi
public final class PlayerActivity extends Activity {
    private static final int OPEN_SUBTITLE = 20;
    private ExoPlayer player;
    private PlayerView playerView;
    private Uri videoUri, subtitleUri;
    private String title, subtitleMime = "application/x-subrip";
    private HistoryStore history;
    private long position;
    private long duration;
    private boolean playWhenReady = true, zoom;
    private float speed = 1f;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable checkpoint = new Runnable() {
        @Override public void run() {
            saveProgress();
            handler.postDelayed(this, 5000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        videoUri = getIntent().getData();
        // Only user-granted local content is accepted, including Open with from file managers.
        if (videoUri == null || !"content".equals(videoUri.getScheme())) { finish(); return; }
        history = new HistoryStore(this);
        HistoryStore.Entry previous = history.find(videoUri.toString());
        title = getIntent().getStringExtra("title");
        if (title == null || title.isEmpty()) title = previous == null ? getString(R.string.video) : previous.title;
        if (previous != null) { position = previous.position; duration = previous.duration; }
        if (state != null) {
            position = state.getLong("position", position);
            duration = state.getLong("duration", duration);
            playWhenReady = state.getBoolean("playing", true);
            speed = state.getFloat("speed", 1f);
            zoom = state.getBoolean("zoom");
            String subtitle = state.getString("subtitle");
            if (subtitle != null) subtitleUri = Uri.parse(subtitle);
            subtitleMime = state.getString("subtitle_mime", subtitleMime);
        }
        setContentView(R.layout.player);
        Ui.applyInsets(this, findViewById(R.id.player_root));
        playerView = findViewById(R.id.player_view);
        playerView.setShowSubtitleButton(true);
        playerView.setResizeMode(zoom ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM : AspectRatioFrameLayout.RESIZE_MODE_FIT);
        ((TextView) findViewById(R.id.video_title)).setText(title);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        Button fit = findViewById(R.id.fit);
        fit.setText(zoom ? R.string.fill : R.string.fit);
        fit.setOnClickListener(v -> {
            zoom = !zoom;
            playerView.setResizeMode(zoom ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM : AspectRatioFrameLayout.RESIZE_MODE_FIT);
            fit.setText(zoom ? R.string.fill : R.string.fit);
        });
        findViewById(R.id.rotate).setOnClickListener(v -> setRequestedOrientation(
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE));
        findViewById(R.id.subtitle).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
            try { startActivityForResult(intent, OPEN_SUBTITLE); }
            catch (android.content.ActivityNotFoundException e) { Toast.makeText(this, R.string.subtitle_error, Toast.LENGTH_LONG).show(); }
        });
        findViewById(R.id.retry).setOnClickListener(v -> {
            if (player != null) { showError(false); player.prepare(); player.play(); }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (videoUri != null && playerView != null) initializePlayer();
    }

    private void initializePlayer() {
        if (player != null) return;
        boolean low = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("low_memory", true);
        DefaultLoadControl load = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(low ? 3000 : 10000, low ? 10000 : 30000, 500, 1000)
                .setTargetBufferBytes((low ? 8 : 24) * 1024 * 1024)
                .setPrioritizeTimeOverSizeThresholds(false).setBackBuffer(0, false).build();
        player = new ExoPlayer.Builder(this, new DefaultRenderersFactory(this).setEnableDecoderFallback(true))
                .setLoadControl(load).setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true);
        player.setHandleAudioBecomingNoisy(true);
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) showError(false);
                if (state == Player.STATE_ENDED) saveProgress();
            }
            @Override public void onPlayerError(PlaybackException error) { showError(true); }
        });
        MediaItem.Builder item = new MediaItem.Builder().setUri(videoUri);
        if (subtitleUri != null) {
            item.setSubtitleConfigurations(Collections.singletonList(new MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                    .setMimeType(subtitleMime).setLabel("External subtitles")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()));
        }
        player.setMediaItem(item.build());
        player.seekTo(PlaybackPolicy.resumePosition(position, duration));
        player.setPlaybackSpeed(speed);
        player.setPlayWhenReady(playWhenReady);
        player.prepare();
        handler.postDelayed(checkpoint, 5000);
    }

    private void showError(boolean visible) {
        findViewById(R.id.player_error).setVisibility(visible ? View.VISIBLE : View.GONE);
        findViewById(R.id.retry).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void saveProgress() {
        if (player == null || history == null) return;
        long currentDuration = player.getDuration();
        if (currentDuration > 0) duration = currentDuration;
        position = PlaybackPolicy.resumePosition(player.getCurrentPosition(), duration);
        speed = player.getPlaybackParameters().speed;
        playWhenReady = player.getPlayWhenReady();
        history.save(new HistoryStore.Entry(videoUri.toString(), title, position, duration));
    }

    private void releasePlayer() {
        handler.removeCallbacks(checkpoint);
        if (player == null) return;
        saveProgress();
        playerView.setPlayer(null);
        player.release(); player = null;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override protected void onPause() { releasePlayer(); super.onPause(); }
    @Override protected void onDestroy() { releasePlayer(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        saveProgress();
        state.putLong("position", position); state.putLong("duration", duration);
        state.putBoolean("playing", playWhenReady); state.putFloat("speed", speed); state.putBoolean("zoom", zoom);
        if (subtitleUri != null) state.putString("subtitle", subtitleUri.toString());
        state.putString("subtitle_mime", subtitleMime);
        super.onSaveInstanceState(state);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != OPEN_SUBTITLE || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri selected = data.getData();
        if (!"content".equals(selected.getScheme())) return;
        // Ask explicitly because many file providers report both formats as text/plain.
        new android.app.AlertDialog.Builder(this).setTitle(R.string.subtitle)
                .setItems(new String[]{"SubRip (.srt)", "WebVTT (.vtt)", "Remove external subtitles"}, (dialog, choice) -> {
                    releasePlayer();
                    subtitleUri = choice == 2 ? null : selected;
                    subtitleMime = choice == 1 ? "text/vtt" : "application/x-subrip";
                    initializePlayer();
                }).setNegativeButton(R.string.cancel, null).show();
    }
}
