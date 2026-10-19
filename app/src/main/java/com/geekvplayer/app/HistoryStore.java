package com.geekvplayer.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

/** A bounded, local-only history. All operations are serialized across activities. */
final class HistoryStore {
    static final int LIMIT = 50;
    private final SharedPreferences prefs;
    private final Context context;

    static final class Entry {
        final String uri, title;
        final long position, duration;
        Entry(String uri, String title, long position, long duration) {
            this.uri = uri; this.title = title; this.position = position; this.duration = duration;
        }
    }

    HistoryStore(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE);
    }

    List<Entry> read() {
        synchronized (HistoryStore.class) {
            List<Entry> result = new ArrayList<>();
            try {
                JSONArray array = new JSONArray(prefs.getString("entries", "[]"));
                for (int i = 0; i < Math.min(array.length(), LIMIT); i++) {
                    JSONObject item = array.getJSONObject(i);
                    result.add(new Entry(item.getString("uri"), item.getString("title"),
                            item.optLong("position"), item.optLong("duration")));
                }
            } catch (JSONException ignored) { /* Recover gracefully from damaged history. */ }
            return result;
        }
    }

    Entry find(String uri) {
        for (Entry entry : read()) if (entry.uri.equals(uri)) return entry;
        return null;
    }

    void save(Entry entry) {
        synchronized (HistoryStore.class) {
            List<Entry> entries = read();
            for (int i = entries.size() - 1; i >= 0; i--) if (entries.get(i).uri.equals(entry.uri)) entries.remove(i);
            entries.add(0, entry);
            while (entries.size() > LIMIT) release(entries.remove(entries.size() - 1).uri);
            write(entries);
        }
    }

    void remove(String uri) {
        synchronized (HistoryStore.class) {
            List<Entry> entries = read();
            for (int i = entries.size() - 1; i >= 0; i--) if (entries.get(i).uri.equals(uri)) entries.remove(i);
            write(entries);
            release(uri);
        }
    }

    void clear() {
        synchronized (HistoryStore.class) {
            for (Entry entry : read()) release(entry.uri);
            prefs.edit().remove("entries").apply();
        }
    }

    private void release(String uri) {
        try {
            context.getContentResolver().releasePersistableUriPermission(Uri.parse(uri),
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) { /* Temporary grants need no release. */ }
    }

    private void write(List<Entry> entries) {
        JSONArray array = new JSONArray();
        try {
            for (Entry entry : entries) {
                JSONObject item = new JSONObject();
                item.put("uri", entry.uri); item.put("title", entry.title);
                item.put("position", entry.position); item.put("duration", entry.duration);
                array.put(item);
            }
            prefs.edit().putString("entries", array.toString()).apply();
        } catch (JSONException ignored) { /* Values above are strings and finite longs. */ }
    }
}
