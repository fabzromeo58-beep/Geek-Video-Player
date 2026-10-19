package com.geekvplayer.app;

/** Pure policy functions, shared by playback and history presentation. */
public final class PlaybackPolicy {
    private PlaybackPolicy() {}

    public static long resumePosition(long position, long duration) {
        if (position < 0) return 0;
        if (duration > 0 && position >= Math.max(0, duration - 2000)) return 0;
        return position;
    }

    public static String time(long milliseconds) {
        long seconds = Math.max(0, milliseconds) / 1000;
        long hours = seconds / 3600;
        return hours > 0
                ? String.format(java.util.Locale.ROOT, "%d:%02d:%02d", hours, seconds / 60 % 60, seconds % 60)
                : String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
