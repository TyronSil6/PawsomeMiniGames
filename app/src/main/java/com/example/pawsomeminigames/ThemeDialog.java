package com.example.pawsomeminigames;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Message popup in the same Critter Toss look (and pop-in) as the Settings window.
 * Used for "About Us" (one button) and "Quit?" (two buttons).
 */
final class ThemedDialog {
    private ThemedDialog() {}

    /** One-button popup (e.g. About Us). */
    static void showInfo(Activity a, String title, String message, String okText) {
        show(a, title, message, okText, null, null);
    }

    /** Two-button popup. negativeText just closes it; onPositive runs when the player agrees. */
    static void showConfirm(Activity a, String title, String message,
                            String positiveText, String negativeText, Runnable onPositive) {
        show(a, title, message, positiveText, negativeText, onPositive);
    }

    private static void show(Activity a, String title, String message,
                             String positiveText, String negativeText, Runnable onPositive) {
        final HubSound sound = HubSound.get(a);

        final Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_message);
        Window win = d.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setDimAmount(.6f);
            win.setLayout(Ui.dialogWidth(a), ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        final View panel = d.findViewById(R.id.dialogPanel);
        panel.setBackground(Ui.panel(a));

        TextView titleView = d.findViewById(R.id.dialogTitle);
        titleView.setBackground(Ui.tab(a));
        titleView.setText(title);

        ((TextView) d.findViewById(R.id.dialogMessage)).setText(message);

        TextView positive = d.findViewById(R.id.btnPositive);
        positive.setText(positiveText);
        positive.setBackground(negativeText == null ? Ui.yellow(a) : Ui.red(a));   // YES in the Quit popup is red
        positive.setTextColor(negativeText == null ? Color.parseColor("#5A3519") : Color.WHITE);
        press(positive);
        positive.setOnClickListener(v -> {
            sound.click();
            d.dismiss();
            if (onPositive != null) onPositive.run();
        });

        TextView negative = d.findViewById(R.id.btnNegative);
        View gap = d.findViewById(R.id.buttonGap);
        if (negativeText == null) {
            negative.setVisibility(View.GONE);
            gap.setVisibility(View.GONE);
        } else {
            negative.setText(negativeText);
            negative.setBackground(Ui.wood(a));
            press(negative);
            negative.setOnClickListener(v -> {
                sound.click();
                d.dismiss();
            });
        }

        // pop-in, and keep the hub's hidden system bars hidden while the window is open
        panel.setScaleX(.8f);
        panel.setScaleY(.8f);
        panel.setAlpha(0f);
        if (win != null) win.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        d.show();
        if (win != null) {
            new WindowInsetsControllerCompat(win, win.getDecorView()).hide(WindowInsetsCompat.Type.systemBars());
            win.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
        panel.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(260)
                .setInterpolator(new OvershootInterpolator(1.4f)).start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private static void press(View v) {
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(.94f).scaleY(.94f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    break;
            }
            return false;
        });
    }
}