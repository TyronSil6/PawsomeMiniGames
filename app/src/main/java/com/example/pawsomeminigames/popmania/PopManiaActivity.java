package com.example.pawsomeminigames.popmania;

import com.example.pawsomeminigames.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.pawsomeminigames.Ui;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** Home screen: Play, Leaderboard and Quit. */
public class PopManiaActivity extends Activity {

    private ScoreManager scoreManager;
    private SoundManager soundManager;
    private TextView tvBestScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pm_activity_main);

        scoreManager = new ScoreManager(this);
        soundManager = new SoundManager(this);

        tvBestScore = findViewById(R.id.tvBestScore);
        View btnPlay = findViewById(R.id.btnPlay);
        View btnLeaderboard = findViewById(R.id.btnLeaderboard);
        View btnQuit = findViewById(R.id.btnQuit);

        // Button press effects
        UiUtils.addPressEffect(btnPlay);
        UiUtils.addPressEffect(btnLeaderboard);
        UiUtils.addPressEffect(btnQuit);

        btnPlay.setOnClickListener(v -> {
            soundManager.playClick();
            startActivity(new Intent(PopManiaActivity.this, InstructionActivity.class));
        });

        btnLeaderboard.setOnClickListener(v -> {
            soundManager.playClick();
            showLeaderboard();
        });

        btnQuit.setOnClickListener(v -> {
            soundManager.playClick();
            finish();   // straight back to the Pawsome hub, no pop-up
        });
    }

    // ------------------------------------------------------------------
    // Pop-up cards
    // ------------------------------------------------------------------
    private Dialog createPopDialog(View content) {
        Dialog dialog = new Dialog(this, R.style.PopManiaDialog);
        dialog.setContentView(content);
        return dialog;
    }

    /** Shows a card sized for the screen, without letting the system bars pop back in. */
    private void showPopDialog(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int width = Math.min((int) (dm.widthPixels * 0.90f), (int) (420 * dm.density));
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
        dialog.show();
        if (window != null) {
            UiUtils.enterFullscreen(window);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    private void showLeaderboard() {
        View view = getLayoutInflater().inflate(R.layout.pm_dialog_leaderboard, null);
        final Dialog dialog = createPopDialog(view);

        // Same card, title sign and DONE button as the Critter Toss leaderboard
        view.findViewById(R.id.dialogPanel).setBackground(Ui.pmPanel(this));
        view.findViewById(R.id.dialogTitle).setBackground(Ui.tab(this));
        final TextView done = view.findViewById(R.id.btnDone);
        done.setBackground(Ui.pmDark(this));
        done.setTextColor(Color.parseColor("#FFF1D0"));
        UiUtils.addPressEffect(done);
        done.setOnClickListener(v -> {
            soundManager.playClick();
            dialog.dismiss();
        });

        final LinearLayout list = view.findViewById(R.id.lbList);
        final TextView[] chips = {
                view.findViewById(R.id.lbEasy), view.findViewById(R.id.lbNormal), view.findViewById(R.id.lbHard)};
        final int[] shown = {scoreManager.getCurrentModeIndex()};

        final Runnable refresh = () -> {
            for (int i = 0; i < chips.length; i++) {
                chips[i].setBackground(i == shown[0] ? Ui.yellow(this) : Ui.chip(this));
                chips[i].setTextColor(Color.parseColor("#5A3519"));
            }
            fillBoard(list, shown[0]);
        };
        refresh.run();
        for (int i = 0; i < chips.length; i++) {
            final int idx = i;
            UiUtils.addPressEffect(chips[i]);
            chips[i].setOnClickListener(v -> {
                shown[0] = idx;
                soundManager.playClick();
                refresh.run();
            });
        }
        showPopDialog(dialog);
    }

    private void fillBoard(LinearLayout list, int mode) {
        list.removeAllViews();
        List<ScoreManager.Entry> entries = scoreManager.getScores(mode);
        if (entries.isEmpty()) {
            TextView t = text("No scores yet.\nPlay a round to get on the board!", 18, "#8A5A33", false);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(28), 0, dp(28));
            list.addView(t, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return;
        }
        DateFormat df = android.text.format.DateFormat.getMediumDateFormat(this);
        String[] medals = {"\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49"};
        for (int i = 0; i < entries.size(); i++) {
            ScoreManager.Entry e = entries.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(5), dp(8), dp(5));
            if (i == 0) row.setBackground(Ui.pmGold(this));

            TextView rank = text(i < 3 ? medals[i] : String.valueOf(i + 1), i < 3 ? 22 : 18, "#5A3519", true);
            rank.setGravity(Gravity.CENTER);
            row.addView(rank, new LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView score = text(e.score + " pts", 20, "#5A3519", true);
            row.addView(score, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView date = text(df.format(new Date(e.time)), 14, "#8A5A33", false);
            date.setGravity(Gravity.END);
            row.addView(date, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            list.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private TextView text(String s, int sp, String color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(Color.parseColor(color));
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        UiUtils.enterFullscreen(this);
        tvBestScore.setText(getString(R.string.pm_best_score_format, scoreManager.getOverallBest()));
        soundManager.startMenuMusic();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (soundManager != null) soundManager.pauseMusic();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundManager != null) soundManager.release();
        if (isFinishing()) SoundManager.releaseMusic();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            UiUtils.enterFullscreen(this);
        }
    }
}