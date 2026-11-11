package com.example.pawsomeminigames.reactiongame;

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

import androidx.activity.OnBackPressedCallback;

import com.example.pawsomeminigames.Immersive;
import com.example.pawsomeminigames.R;
import com.example.pawsomeminigames.Ui;
import com.example.pawsomeminigames.reactiongame.audio.SoundManager;
import com.example.pawsomeminigames.reactiongame.data.ScoreEntry;
import com.example.pawsomeminigames.reactiongame.ui.ButtonFx;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/** Home screen: Play, Leaderboard and Back (to the Pawsome hub). Same layout and look as Pop Mania's home screen. */
public class MainMenuActivity extends BaseActivity {
    private static final int LEADERBOARD_ROWS = 10;

    private TextView bestChip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.rg_activity_main_menu);
        bestChip = findViewById(R.id.tv_best);

        for (int id : new int[]{R.id.btn_play, R.id.btn_leaderboard, R.id.btn_quit}) {
            ButtonFx.attach(findViewById(id));
        }

        onTap(R.id.btn_play, new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(MainMenuActivity.this, HowToPlayActivity.class);
                i.putExtra("start_game", true);
                startActivity(i);
            }
        });
        onTap(R.id.btn_leaderboard, new Runnable() {
            @Override
            public void run() {
                showLeaderboard();
            }
        });
        onTap(R.id.btn_quit, new Runnable() {
            @Override
            public void run() {
                finish();   // straight back to the Pawsome hub, no pop-up
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
        if (savedInstanceState == null) {
            staggerIn(findViewById(R.id.btn_play), findViewById(R.id.btn_leaderboard),
                    findViewById(R.id.btn_quit));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bestChip.setText(getString(R.string.rg_best_score_fmt, prefs.getBestScore()));
    }

    // ------------------------------------------------------------------
    // Leaderboard pop-up (same card, title sign, rows and DONE button as Pop Mania)
    // ------------------------------------------------------------------
    private void showLeaderboard() {
        View view = getLayoutInflater().inflate(R.layout.rg_dialog_leaderboard, null);
        final Dialog dialog = new Dialog(this, R.style.PopManiaDialog);
        dialog.setContentView(view);

        view.findViewById(R.id.dialogPanel).setBackground(Ui.pmPanel(this));
        view.findViewById(R.id.dialogTitle).setBackground(Ui.tab(this));
        final TextView done = view.findViewById(R.id.btnDone);
        done.setBackground(Ui.pmDark(this));
        done.setTextColor(Color.parseColor("#FFF1D0"));
        ButtonFx.attach(done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sound.playSfx(SoundManager.Sfx.BUTTON);
                dialog.dismiss();
            }
        });

        fillBoard((LinearLayout) view.findViewById(R.id.lbList));

        Window window = dialog.getWindow();
        if (window != null) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int width = Math.min((int) (dm.widthPixels * 0.90f), (int) (420 * dm.density));
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        Immersive.show(dialog);
    }

    private void fillBoard(LinearLayout list) {
        list.removeAllViews();
        List<ScoreEntry> all = prefs.getHistory();
        List<ScoreEntry> entries = new ArrayList<>();
        for (ScoreEntry e : all) {
            if (e.score > 0) entries.add(e);
        }
        Collections.sort(entries, new Comparator<ScoreEntry>() {
            @Override
            public int compare(ScoreEntry a, ScoreEntry b) {
                return Integer.compare(b.score, a.score);
            }
        });
        if (entries.size() > LEADERBOARD_ROWS) entries = new ArrayList<>(entries.subList(0, LEADERBOARD_ROWS));

        if (entries.isEmpty()) {
            TextView t = text(getString(R.string.rg_no_scores), 18, "#8A5A33", false);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(28), 0, dp(28));
            list.addView(t, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return;
        }
        DateFormat df = android.text.format.DateFormat.getMediumDateFormat(this);
        String[] medals = {"\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49"};
        for (int i = 0; i < entries.size(); i++) {
            ScoreEntry e = entries.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(5), dp(8), dp(5));
            if (i == 0) row.setBackground(Ui.pmGold(this));

            TextView rank = text(i < 3 ? medals[i] : String.valueOf(i + 1), i < 3 ? 22 : 18, "#5A3519", true);
            rank.setGravity(Gravity.CENTER);
            row.addView(rank, new LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView score = text(getString(R.string.rg_points_fmt, e.score), 20, "#5A3519", true);
            row.addView(score, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView date = text(df.format(new Date(e.timeMillis)), 14, "#8A5A33", false);
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

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) sound.release();   // leaving Reaction Game: free its music + sounds
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
