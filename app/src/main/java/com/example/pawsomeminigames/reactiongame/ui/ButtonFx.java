package com.example.pawsomeminigames.reactiongame.ui;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

public final class ButtonFx {
    private ButtonFx() {
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void attach(View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(70).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }
}
