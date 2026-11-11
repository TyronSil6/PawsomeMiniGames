package com.example.pawsomeminigames.crittertoss;

import com.example.pawsomeminigames.R;
import com.example.pawsomeminigames.Ui;
import com.example.pawsomeminigames.Immersive;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class MenuActivity extends AppCompatActivity {

    private ValueAnimator bannerPulse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.critter_activity_menu);
        Immersive.apply(this);

        View play = findViewById(R.id.btnPlay);
        View board = findViewById(R.id.btnLeaderboard);
        View quit = findViewById(R.id.btnQuit);

        play.setBackground(Ui.red(this));
        board.setBackground(Ui.yellow(this));
        quit.setBackground(Ui.wood(this));

        // title sign: first child is the black plate, the other four are the gold studs
        ViewGroup banner = findViewById(R.id.bannerFrame);
        for (int i = 0; i < banner.getChildCount(); i++) {
            banner.getChildAt(i).setBackground(i == 0 ? Ui.banner(this) : Ui.dot(this));
        }
        bannerPulse = Ui.pulse(banner);   // sign "breathes" like the Sakura Brawl logo

        addPressEffect(play);
        addPressEffect(board);
        addPressEffect(quit);

        play.setOnClickListener(v -> {
            SoundManager.get(this).play(SoundManager.CLICK);
            showPlay();
        });
        board.setOnClickListener(v -> {
            SoundManager.get(this).play(SoundManager.CLICK);
            showLeaderboard();
        });
        quit.setOnClickListener(v -> finish());   // back to the Pawsome hub
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Immersive.apply(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SoundManager.get(this).startMusic();
        if (bannerPulse != null && !bannerPulse.isStarted()) bannerPulse.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SoundManager.get(this).pauseMusic();
        if (bannerPulse != null) bannerPulse.cancel();
    }

    // ---------------------------------------------------------------- dialogs

    private Dialog makeDialog(int layout) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(layout);
        Window win = d.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setDimAmount(.6f);
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        d.findViewById(R.id.dialogPanel).setBackground(Ui.pmPanel(this));   // same card as Pop Mania
        d.findViewById(R.id.dialogTitle).setBackground(Ui.tab(this));
        View done = d.findViewById(R.id.btnDone);
        done.setBackground(Ui.pmDark(this));
        ((TextView) done).setTextColor(Color.parseColor("#FFF1D0"));
        addPressEffect(done);
        done.setOnClickListener(v -> {
            SoundManager.get(this).play(SoundManager.CLICK);
            d.dismiss();
        });
        return d;
    }

    private void popIn(Dialog d) {
        final View panel = d.findViewById(R.id.dialogPanel);
        panel.setScaleX(.8f);
        panel.setScaleY(.8f);
        panel.setAlpha(0f);
        Immersive.show(d);
        panel.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(260)
                .setInterpolator(new OvershootInterpolator(1.4f)).start();
    }

    /** Full-screen "How to play" window with the mode picker and the PLAY button. */
    private void showPlay() {
        final SoundManager sm = SoundManager.get(this);
        final Dialog d = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        d.setContentView(R.layout.critter_dialog_play);
        Window win = d.getWindow();
        if (win != null) win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        d.findViewById(R.id.howPanel).setBackground(Ui.panel(this));
        d.findViewById(R.id.howTitle).setBackground(Ui.tab(this));
        // points come straight from the game's critter table, so this text can't go out of date
        ((TextView) d.findViewById(R.id.howGolden)).setText(
                "Catch the golden fox for " + GameView.Type.GOLDEN.points + " points! It's fast and tiny, so aim carefully.");
        ((TextView) d.findViewById(R.id.howTimer)).setText(
                "Beat the " + (int) GameView.ROUND_TIME + "-second timer! Clock foxes give you bonus time.");

        // power-up icons + text come straight from the game's Power table
        final int[] powIds = {R.id.powFreeze, R.id.powBig, R.id.powTriple, R.id.powBomb};
        final GameView.Power[] pows = {GameView.Power.FREEZE, GameView.Power.BIG,
                GameView.Power.TRIPLE, GameView.Power.BOMB};
        final String[] powText = {
                "Freeze: critters stop for " + GameView.Power.FREEZE.dur + " seconds.",
                "Big Blast: a much bigger hit area for " + (int) GameView.Power.BIG.dur + " seconds.",
                "Triple Shot: 3 balls per tap for " + (int) GameView.Power.TRIPLE.dur + " seconds.",
                "Bomb: pops every critter on screen at once!"};
        for (int i = 0; i < pows.length; i++) {
            ((PowerIconView) d.findViewById(powIds[i])).setPower(pows[i]);
            ((TextView) d.findViewById(powIds[i] == R.id.powFreeze ? R.id.powFreezeText
                    : powIds[i] == R.id.powBig ? R.id.powBigText
                    : powIds[i] == R.id.powTriple ? R.id.powTripleText : R.id.powBombText)).setText(powText[i]);
        }

        final TextView prev = d.findViewById(R.id.modePrev);
        final TextView next = d.findViewById(R.id.modeNext);
        final TextView pill = d.findViewById(R.id.modePill);
        final TextView info = d.findViewById(R.id.modeInfo);
        final TextView best = d.findViewById(R.id.modeBest);
        final View start = d.findViewById(R.id.btnStart);
        final View back = d.findViewById(R.id.btnBack);

        prev.setBackground(Ui.yellow(this));
        next.setBackground(Ui.yellow(this));
        start.setBackground(Ui.red(this));
        back.setBackground(Ui.wood(this));

        final String[] stars = {"\u2605", "\u2605\u2605", "\u2605\u2605\u2605"};
        final String[] about = {
                "Slower critters and bigger targets.",
                "The classic challenge.",
                "Fast critters, tiny targets, more of them!"};
        final int[] cur = {GameData.getDifficulty(this)};

        final Runnable refresh = () -> {
            int c = cur[0];
            pill.setText(stars[c] + "  " + GameData.NAMES[c].toUpperCase());
            pill.setBackground(c == 0 ? Ui.green(this) : c == 1 ? Ui.yellow(this) : Ui.red(this));
            pill.setTextColor(c == 1 ? Color.parseColor("#5A3519") : Color.WHITE);
            info.setText(about[c]);
            best.setText("Best: " + GameData.best(this, c));
        };
        refresh.run();

        final View.OnClickListener cycle = v -> {
            cur[0] = (cur[0] + (v == next ? 1 : 2)) % 3;   // wraps around
            GameData.setDifficulty(this, cur[0]);
            sm.play(SoundManager.CLICK);
            refresh.run();
            pill.setScaleX(.85f);
            pill.setScaleY(.85f);
            pill.animate().scaleX(1f).scaleY(1f).setDuration(220)
                    .setInterpolator(new OvershootInterpolator(2f)).start();
        };
        prev.setOnClickListener(cycle);
        next.setOnClickListener(cycle);

        addPressEffect(prev);
        addPressEffect(next);
        addPressEffect(start);
        addPressEffect(back);
        start.setOnClickListener(v -> {
            sm.play(SoundManager.CLICK);
            startActivity(new Intent(this, MainActivity.class));
            d.dismiss();
        });
        back.setOnClickListener(v -> {
            sm.play(SoundManager.CLICK);
            d.dismiss();
        });

        // pop-in
        final View content = d.findViewById(R.id.playContent);
        content.setScaleX(.9f);
        content.setScaleY(.9f);
        content.setAlpha(0f);
        Immersive.show(d);
        content.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(280)
                .setInterpolator(new OvershootInterpolator(1.2f)).start();
    }

    private void showLeaderboard() {
        final Dialog d = makeDialog(R.layout.critter_dialog_leaderboard);
        final LinearLayout list = d.findViewById(R.id.lbList);
        final TextView[] chips = {
                d.findViewById(R.id.lbEasy), d.findViewById(R.id.lbNormal), d.findViewById(R.id.lbHard)};
        final int[] shown = {GameData.getDifficulty(this)};

        final Runnable refresh = () -> {
            for (int i = 0; i < chips.length; i++) styleChip(chips[i], i == shown[0]);
            fillBoard(list, shown[0]);
        };
        refresh.run();
        for (int i = 0; i < chips.length; i++) {
            final int idx = i;
            addPressEffect(chips[i]);
            chips[i].setOnClickListener(v -> {
                shown[0] = idx;
                SoundManager.get(this).play(SoundManager.CLICK);
                refresh.run();
            });
        }
        popIn(d);
    }

    private void fillBoard(LinearLayout list, int diff) {
        list.removeAllViews();
        List<GameData.Entry> entries = GameData.scores(this, diff);
        if (entries.isEmpty()) {
            TextView t = text("No scores yet.\nPlay a round to get on the board!", 18, "#8A5A33", false);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(28), 0, dp(28));
            list.addView(t, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return;
        }
        DateFormat df = android.text.format.DateFormat.getMediumDateFormat(this);
        String[] medals = {"🥇", "🥈", "🥉"};
        for (int i = 0; i < entries.size(); i++) {
            GameData.Entry e = entries.get(i);
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

    // ---------------------------------------------------------------- helpers

    private TextView text(String s, int sp, String color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(Color.parseColor(color));
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void styleChip(TextView t, boolean on) {
        t.setBackground(on ? Ui.yellow(this) : Ui.chip(this));
        t.setTextColor(Color.parseColor("#5A3519"));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addPressEffect(View v) {
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
            return false; // let the click still go through
        });
    }

    // ---- info for the hub's "More" window (read straight from the game's own tables) ----

    /** One critter row: picture, name and what shooting it gives or costs. */
    public static final class CritterInfo {
        public final int res;
        public final String name, value;
        public final boolean good;
        CritterInfo(int res, String name, String value, boolean good) {
            this.res = res; this.name = name; this.value = value; this.good = good;
        }
    }

    private static CritterInfo info(GameView.Type t, String name) {
        String v;
        boolean good;
        if (t.seconds != 0) {
            v = (t.seconds > 0 ? "+" : "") + t.seconds + " seconds";
            good = t.seconds > 0;
        } else {
            v = (t.points > 0 ? "+" : "") + t.points + " points";
            good = t.points > 0;
        }
        return new CritterInfo(t.res, name, v, good);
    }

    public static java.util.List<CritterInfo> critterInfo() {
        java.util.List<CritterInfo> l = new java.util.ArrayList<>();
        l.add(info(GameView.Type.FOX, "Fox"));
        l.add(info(GameView.Type.RACCOON, "Raccoon"));
        l.add(info(GameView.Type.CAPYBARA, "Capybara"));
        l.add(info(GameView.Type.SLOTH, "Sloth"));
        l.add(info(GameView.Type.KING, "King Raccoon"));
        l.add(info(GameView.Type.GOLDEN, "Golden Fox"));
        l.add(info(GameView.Type.CLOCK_FOX, "Clock Fox"));
        l.add(info(GameView.Type.SAD_CAPY, "Sad Capybara"));
        l.add(info(GameView.Type.SAD_RACCOON, "Sad Raccoon"));
        l.add(info(GameView.Type.SAD_SLOTH, "Sad Sloth"));
        l.add(info(GameView.Type.SAD_FOX, "Sad Fox"));
        return l;
    }

    public static int roundSeconds() { return (int) GameView.ROUND_TIME; }

    public static String[] powerUpLines() {
        return new String[]{
                "Freeze: critters stop for " + GameView.Power.FREEZE.dur + " seconds.",
                "Big Blast: a much bigger hit area for " + (int) GameView.Power.BIG.dur + " seconds.",
                "Triple Shot: 3 balls per tap for " + (int) GameView.Power.TRIPLE.dur + " seconds.",
                "Bomb: pops every critter on screen at once!"};
    }

    /** Called by the splash screen on a background thread: gets the game's pictures + music ready. */
    public static void preload(android.content.Context c) {
        GameView.ensureAssets(c.getResources());
        SoundManager.get(c).warmMusic();
    }
}