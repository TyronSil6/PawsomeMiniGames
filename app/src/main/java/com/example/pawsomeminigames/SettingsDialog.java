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
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * The one Settings window for the whole app (Music + Sound switches and volume sliders).
 * Critter Toss and Sakura Brawl read what is saved here (see AudioSettings).
 */
final class SettingsDialog {
    private SettingsDialog() {}

    static void show(Activity a) {
        final HubSound sound = HubSound.get(a);

        final Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_settings);
        Window win = d.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setDimAmount(.6f);
            win.setLayout(Ui.dialogWidth(a), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        d.findViewById(R.id.dialogPanel).setBackground(Ui.panel(a));
        d.findViewById(R.id.dialogTitle).setBackground(Ui.tab(a));

        final TextView music = d.findViewById(R.id.btnMusic);
        final TextView sfx = d.findViewById(R.id.btnSfx);
        final SeekBar seekMusic = d.findViewById(R.id.seekMusic);
        final SeekBar seekSfx = d.findViewById(R.id.seekSfx);

        Ui.styleSeek(a, seekMusic);
        Ui.styleSeek(a, seekSfx);
        seekMusic.setProgress(Math.round(AudioSettings.getMusicVolume(a) * 100f));
        seekSfx.setProgress(Math.round(AudioSettings.getSfxVolume(a) * 100f));

        final Runnable refresh = () -> {
            styleToggle(a, music, AudioSettings.isMusicOn(a));
            styleToggle(a, sfx, AudioSettings.isSfxOn(a));
        };
        refresh.run();

        press(music);
        press(sfx);
        music.setOnClickListener(v -> {
            AudioSettings.setMusicOn(a, !AudioSettings.isMusicOn(a));
            HubMusic.get(a).refresh();   // hub music follows the switch right away
            sound.click();
            refresh.run();
        });
        sfx.setOnClickListener(v -> {
            AudioSettings.setSfxOn(a, !AudioSettings.isSfxOn(a));
            sound.click();
            refresh.run();
        });

        seekMusic.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (user) {
                    AudioSettings.setMusicVolume(a, p / 100f);
                    HubMusic.get(a).refresh();   // hub music follows the slider while dragging
                }
            }
        });
        seekSfx.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (user) AudioSettings.setSfxVolume(a, p / 100f);
            }
            @Override public void onStopTrackingTouch(SeekBar s) {
                sound.click();   // lets the player hear the new volume
            }
        });

        View done = d.findViewById(R.id.btnDone);
        done.setBackground(Ui.yellow(a));
        press(done);
        done.setOnClickListener(v -> {
            sound.click();
            d.dismiss();
        });

        // pop-in, and keep the hub's hidden system bars hidden while the window is open
        final View panel = d.findViewById(R.id.dialogPanel);
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

    private static void styleToggle(Activity a, TextView t, boolean on) {
        t.setText(on ? "ON" : "OFF");
        t.setBackground(on ? Ui.green(a) : Ui.wood(a));
        t.setTextColor(on ? Color.WHITE : Color.parseColor("#FFF3D6"));
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
            return false;   // let the click still go through
        });
    }

    private abstract static class SimpleSeek implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar s) { }
        @Override public void onStopTrackingTouch(SeekBar s) { }
    }
}