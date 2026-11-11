package com.example.pawsomeminigames;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * One place that hides the phone's status bar + navigation bar (the buttons / icons at the
 * screen edges) so every screen of every game is truly full screen.
 *
 * Call Immersive.apply(this) in onCreate() AND in onWindowFocusChanged(true) of each activity,
 * because Android brings the bars back after a dialog, a swipe, or switching apps.
 */
public final class Immersive {
    private Immersive() {}

    public static void apply(Activity a) {
        apply(a.getWindow());
        if (Build.VERSION.SDK_INT >= 28) {
            a.getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }

    public static void apply(Window w) {
        if (w == null) return;
        WindowCompat.setDecorFitsSystemWindows(w, false);
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(w, w.getDecorView());
        c.hide(WindowInsetsCompat.Type.systemBars());
        c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    /** Shows a dialog without the system bars popping up while it opens. */
    public static void show(Dialog d) {
        Window w = d.getWindow();
        if (w != null) {
            w.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
        d.show();
        if (w != null) {
            apply(w);
            w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }
}