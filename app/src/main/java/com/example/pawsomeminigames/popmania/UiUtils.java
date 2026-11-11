package com.example.pawsomeminigames.popmania;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import com.example.pawsomeminigames.Immersive;

/** Small helpers shared by all screens. */
public final class UiUtils {

    private UiUtils() { }

    /** Hides the status and navigation bars so the game fills the whole screen (same as the rest of the app). */
    public static void enterFullscreen(Activity activity) {
        Immersive.apply(activity);
    }

    /** Same as above, for any window (activities and pop-up dialogs). */
    public static void enterFullscreen(Window window) {
        Immersive.apply(window);
    }

    /** Makes a button shrink slightly while it is pressed. Clicks still work normally. */
    public static void addPressEffect(View button) {
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                        break;
                    default:
                        break;
                }
                return false; // false = still let the click listener run
            }
        });
    }
}
