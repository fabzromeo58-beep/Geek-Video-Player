package com.geekvplayer.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int OPEN_VIDEO = 10;
    private static final int WHITE = Color.rgb(242, 244, 255), MUTED = Color.rgb(164, 174, 194);
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private HistoryStore history;
    private List<HistoryStore.Entry> entries = new ArrayList<>();
    private BaseAdapter adapter;
    private TextView empty;
    private Button open;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        history = new HistoryStore(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(12, 14, 23));
        setContentView(root);
        Ui.applyInsets(this, root);

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setClipToPadding(false);
        list.setPadding(dp(20), 0, dp(20), dp(12));
        root.addView(list, new LinearLayout.LayoutParams(-1, -1));
        LinearLayout header = column();
        header.setPadding(0, dp(28), 0, dp(16));
        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        logo.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.geek_logo, options));
        logo.setContentDescription(getString(R.string.logo_description));
        brand.addView(logo, new LinearLayout.LayoutParams(dp(64), dp(64)));
        LinearLayout words = column();
        words.setPadding(dp(14), 0, 0, 0);
        words.addView(label(getString(R.string.app_name), 27, WHITE, true));
        words.addView(label(getString(R.string.tagline), 13, MUTED, false));
        brand.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(brand);

        TextView intro = label("Press play.\nKeep it simple.", 34, WHITE, true);
        intro.setPadding(0, dp(30), 0, dp(20));
        header.addView(intro);
        open = new Button(this);
        open.setText(R.string.open_video); open.setAllCaps(false);
        open.setTextSize(16); open.setTextColor(Color.rgb(6, 26, 27));
        open.setBackground(rounded(Color.rgb(101, 229, 210)));
        open.setOnClickListener(v -> pickVideos());
        header.addView(open, new LinearLayout.LayoutParams(-1, dp(56)));
        TextView privacy = label(getString(R.string.privacy), 11, MUTED, false);
        privacy.setLetterSpacing(.10f); privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(0, dp(16), 0, dp(24)); header.addView(privacy);

        Switch low = new Switch(this);
        low.setText(R.string.low_memory); low.setTextColor(WHITE); low.setTextSize(16);
        low.setMinHeight(dp(48));
        low.setChecked(getSharedPreferences("settings", MODE_PRIVATE).getBoolean("low_memory", true));
        low.setOnCheckedChangeListener((button, checked) -> getSharedPreferences("settings", MODE_PRIVATE)
                .edit().putBoolean("low_memory", checked).apply());
        header.addView(low);
        TextView memory = label(getString(R.string.low_memory_description), 12, MUTED, false);
        memory.setPadding(0, 0, 0, dp(24)); header.addView(memory);
        LinearLayout recent = new LinearLayout(this);
        recent.setGravity(Gravity.CENTER_VERTICAL);
        recent.addView(label(getString(R.string.library), 20, WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button clear = new Button(this); clear.setText(R.string.clear); clear.setAllCaps(false);
        clear.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage(R.string.clear_message)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.clear, (d, w) -> {
                    history.clear(); refresh();
                }).show());
        recent.addView(clear); header.addView(recent);
        empty = label(getString(R.string.empty_title) + "\n\n" + getString(R.string.empty_body), 16, MUTED, false);
        empty.setPadding(dp(20), dp(24), dp(20), dp(24));
        empty.setBackground(rounded(Color.rgb(23, 28, 43))); header.addView(empty);
        list.addHeaderView(header, null, false);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return entries.size(); }
            @Override public Object getItem(int position) { return entries.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                LinearLayout row;
                if (recycled == null) {
                    row = column(); row.setPadding(dp(16), dp(18), dp(16), dp(18));
                    TextView title = label("", 16, WHITE, true);
                    title.setMaxLines(2); title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    row.addView(title); row.addView(label("", 12, MUTED, false));
                } else row = (LinearLayout) recycled;
                HistoryStore.Entry entry = entries.get(position);
                ((TextView) row.getChildAt(0)).setText(entry.title);
                ((TextView) row.getChildAt(1)).setText(entry.position > 0
                        ? getString(R.string.resume_at, PlaybackPolicy.time(entry.position)) : getString(R.string.ready_to_play));
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            int index = position - list.getHeaderViewsCount();
            if (index >= 0 && index < entries.size()) play(entries.get(index));
        });
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            int index = position - list.getHeaderViewsCount();
            if (index < 0 || index >= entries.size()) return false;
            HistoryStore.Entry entry = entries.get(index);
            new AlertDialog.Builder(this).setTitle(entry.title).setItems(new String[]{getString(R.string.remove)},
                    (d, w) -> { history.remove(entry.uri); refresh(); }).show();
            return true;
        });
    }

    @Override protected void onResume() { super.onResume(); refresh(); }
    private void refresh() {
        entries = history.read(); adapter.notifyDataSetChanged();
        empty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
    }
    private void pickVideos() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("video/*")
                .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try { startActivityForResult(intent, OPEN_VIDEO); }
        catch (android.content.ActivityNotFoundException e) { Toast.makeText(this, R.string.open_error, Toast.LENGTH_LONG).show(); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != OPEN_VIDEO || result != RESULT_OK || data == null) return;
        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < Math.min(data.getClipData().getItemCount(), HistoryStore.LIMIT); i++)
                uris.add(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) uris.add(data.getData());
        if (uris.isEmpty()) return;
        open.setEnabled(false); open.setText(R.string.loading);
        io.execute(() -> {
            for (int i = uris.size() - 1; i >= 0; i--) {
                Uri uri = uris.get(i);
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (SecurityException ignored) { /* Some providers offer temporary access only. */ }
                HistoryStore.Entry old = history.find(uri.toString());
                history.save(new HistoryStore.Entry(uri.toString(), title(uri), old == null ? 0 : old.position, old == null ? 0 : old.duration));
            }
            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) return;
                open.setEnabled(true); open.setText(R.string.open_video); refresh();
                HistoryStore.Entry first = history.find(uris.get(0).toString());
                if (first != null) play(first);
            });
        });
    }
    private String title(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getString(0);
        } catch (RuntimeException ignored) { /* Unavailable provider: playback displays a useful error. */ }
        return getString(R.string.video);
    }
    private void play(HistoryStore.Entry entry) {
        startActivity(new Intent(this, PlayerActivity.class).setData(Uri.parse(entry.uri)).putExtra("title", entry.title));
    }
    @Override protected void onDestroy() { io.shutdown(); super.onDestroy(); }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(null, Typeface.BOLD); return view;
    }
    private GradientDrawable rounded(int color) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(16)); return d; }
}
