package com.example.pawsomeminigames;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.pawsomeminigames.crittertoss.MenuActivity;
import com.example.pawsomeminigames.popmania.CritterType;

/**
 * The hub's "More" window: a short description of the app, then full instructions for each game.
 * Critter Toss points / times are read from the game itself, so this text can't go out of date.
 */
final class HelpDialog {
    private HelpDialog() {}

    private static final int BROWN = Color.parseColor("#5A3519");
    private static final int GOLD_TEXT = Color.parseColor("#B07A12");
    private static final int GOOD = Color.parseColor("#3E8F36");
    private static final int BAD = Color.parseColor("#C8341E");

    static void show(Activity a) {
        final HubSound sound = HubSound.get(a);

        final Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_help);
        Window win = d.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setDimAmount(.6f);
            win.setLayout(Ui.dialogWidth(a), ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        final View panel = d.findViewById(R.id.dialogPanel);
        panel.setBackground(Ui.panel(a));
        d.findViewById(R.id.dialogTitle).setBackground(Ui.tab(a));

        // scroll area takes about 55% of the screen height so CLOSE is always on screen
        DisplayMetrics dm = a.getResources().getDisplayMetrics();
        View scroll = d.findViewById(R.id.helpScroll);
        scroll.getLayoutParams().height = Math.round(dm.heightPixels * 0.55f);

        fill(a, (LinearLayout) d.findViewById(R.id.helpContent));

        View close = d.findViewById(R.id.btnClose);
        close.setBackground(Ui.yellow(a));
        press(close);
        close.setOnClickListener(v -> {
            sound.click();
            d.dismiss();
        });

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

    // ------------------------------------------------------------------ the text

    private static void fill(Activity a, LinearLayout box) {
        // ---- the app
        heading(a, box, "PAWSOME MINI GAMES", true);
        body(a, box, "A collection of cute little games. Pick a game on the main screen to play it. "
                + "Use Settings to change the music and sound volume. More games are coming soon!");

        // ---- Sakura Brawl
        heading(a, box, "SAKURA BRAWL", false);
        body(a, box, "Push your opponent out of the ring to win the round!");
        bullet(a, box, "TAP the screen to dash at your opponent. One tap is one dash, so tap again to push again. Holding your finger down does not keep pushing.");
        bullet(a, box, "SWIPE to dash in the direction you swiped.");
        bullet(a, box, "Each dash needs a short rest before you can dash again, so time your hits.");
        bullet(a, box, "A round lasts 30 seconds. After 10 seconds the ring starts to shrink.");
        bullet(a, box, "If time runs out, the wrestler closer to the center wins the round.");
        bullet(a, box, "A match is up to 7 rounds. The first to win 4 rounds wins the match.");
        bullet(a, box, "2 PLAYERS: the left half of the screen is Player 1 and the right half is Player 2.");

        // ---- Critter Toss
        heading(a, box, "CRITTER TOSS", false);
        body(a, box, "Tap to shoot the target each critter is holding! You have "
                + MenuActivity.roundSeconds() + " seconds to get the highest score.");
        sub(a, box, "Points for every critter");
        for (MenuActivity.CritterInfo c : MenuActivity.critterInfo()) critterRow(a, box, c);
        body(a, box, "Do not shoot the sad ones! They cost you points or time. Clock foxes give you bonus time.");
        sub(a, box, "Power-ups (shoot the glowing orbs)");
        for (String line : MenuActivity.powerUpLines()) bullet(a, box, line);
        sub(a, box, "Difficulty");
        bullet(a, box, "EASY: slower critters and bigger targets.");
        bullet(a, box, "NORMAL: the classic challenge.");
        bullet(a, box, "HARD: fast critters, tiny targets, more of them!");

        // ---- Pop Mania
        heading(a, box, "POP MANIA", false);
        body(a, box, "Tap the critters popping out of the holes and collect as many points as you can before time runs out!");
        sub(a, box, "Know your targets");
        critterRow(a, box, CritterType.SLOTH.drawableRes, "Regular critters", "+" + CritterType.SLOTH.points, true);
        critterRow(a, box, CritterType.GOLDEN_RACCOON.drawableRes, "Golden critter", "+" + CritterType.GOLDEN_RACCOON.points, true);
        critterRow(a, box, CritterType.TIMER.drawableRes, "Timer", "+" + CritterType.TIMER.getTimeBonus() + " sec", true);
        critterRow(a, box, CritterType.SNAKE.drawableRes, "Snake", String.valueOf(CritterType.SNAKE.points), false);
        critterRow(a, box, CritterType.STAR.drawableRes, "Bomb", "Round over!", false);
        body(a, box, "Do not tap the bomb! It ends the round at once.");
        sub(a, box, "Quick tips");
        bullet(a, box, "Keep your eyes on all the holes.");
        bullet(a, box, "Tap quickly, but check the target first.");
        bullet(a, box, "Golden critters are rare, so grab them fast!");
        sub(a, box, "Difficulty");
        bullet(a, box, "EASY: 40 seconds, slower critters, more time to aim.");
        bullet(a, box, "NORMAL: 30 seconds, balanced speed.");
        bullet(a, box, "HARD: 30 seconds, fast critters, quick reactions needed.");

        // ---- Reaction Game
        heading(a, box, "REACTION GAME", false);
        body(a, box, "Quick eyes, fast fingers! Find the matching tile before the timer runs out.");
        bullet(a, box, "Look at the animal and the color shown in the box at the top.");
        bullet(a, box, "Tap the tile with the SAME animal in the SAME color before time runs out.");
        bullet(a, box, "Be fast! Every round has a countdown, and it gets shorter as your score grows.");
        bullet(a, box, "Beware of decoys: tiles with the right animal in the wrong color, or the right color on the wrong animal.");
        bullet(a, box, "One wrong tap or running out of time ends the run. Beat your best score!");
    }

    // ------------------------------------------------------------------ small builders

    private static int dp(Activity a, float v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }

    private static TextView text(Activity a, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private static LinearLayout.LayoutParams lp(Activity a, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(a, topDp);
        return p;
    }

    private static void heading(Activity a, LinearLayout box, String s, boolean first) {
        TextView t = text(a, s, 19, GOLD_TEXT, true);
        t.setGravity(Gravity.CENTER);
        box.addView(t, lp(a, first ? 0 : 22));
        // thin gold line under each game title
        View line = new View(a);
        line.setBackgroundColor(Color.parseColor("#E0A21C"));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 2));
        p.topMargin = dp(a, 4);
        box.addView(line, p);
    }

    private static void sub(Activity a, LinearLayout box, String s) {
        box.addView(text(a, s, 15, GOLD_TEXT, true), lp(a, 14));
    }

    private static void body(Activity a, LinearLayout box, String s) {
        box.addView(text(a, s, 15, BROWN, false), lp(a, 8));
    }

    private static void bullet(Activity a, LinearLayout box, String s) {
        box.addView(text(a, "\u2022  " + s, 15, BROWN, false), lp(a, 6));
    }

    private static void critterRow(Activity a, LinearLayout box, MenuActivity.CritterInfo c) {
        critterRow(a, box, c.res, c.name, c.value, c.good);
    }

    private static void critterRow(Activity a, LinearLayout box, int res, String label, String value, boolean good) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView img = new ImageView(a);
        img.setImageResource(res);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(img, new LinearLayout.LayoutParams(dp(a, 46), dp(a, 46)));

        TextView name = text(a, label, 15, BROWN, true);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        np.leftMargin = dp(a, 10);
        row.addView(name, np);

        row.addView(text(a, value, 15, good ? GOOD : BAD, true));
        box.addView(row, lp(a, 4));
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