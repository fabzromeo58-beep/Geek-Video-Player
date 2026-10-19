package com.geekvplayer.app;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

final class Ui {
    private Ui() {}
    static void applyInsets(Activity activity, View root) {
        // API 35 enforces edge-to-edge; preserve usable controls around system bars/cutouts.
        if (Build.VERSION.SDK_INT >= 30) {
            activity.getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
            root.requestApplyInsets();
        }
    }
}
