package com.example.pawsomeminigames.crittertoss;

import com.example.pawsomeminigames.R;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import com.example.pawsomeminigames.R;
import com.example.pawsomeminigames.Ui;
import com.example.pawsomeminigames.Immersive;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {
    private GameView game;
    private Dialog pauseDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // phones stay portrait; tablets / foldables / desktop windows may rotate (the game letterboxes itself)
        if (getResources().getConfiguration().smallestScreenWidthDp < 600) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        WindowInsetsControllerCompat c =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        c.hide(WindowInsetsCompat.Type.systemBars());
        c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        game = new GameView(this);
        game.setOnExit(this::finish);                       // "Main menu" on the game-over screen
        game.setOnPauseRequested(this::showPauseDialog);    // pause button -> pause menu
        setContentView(game);

        // Back button: open the pause menu instead of quitting mid-round
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (game.isPaused()) showPauseDialog();
                else if (game.isGameplay()) { game.pauseGame(); showPauseDialog(); }
                else finish();                              // game-over screen
            }
        });
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
        if (game.isPaused()) showPauseDialog();             // came back from another app while paused
    }

    @Override
    protected void onPause() {
        super.onPause();
        game.pauseGame();
        SoundManager.get(this).pauseMusic();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pauseDialog != null) pauseDialog.dismiss();
    }

    // ---------------------------------------------------------------- pause menu

    private void showPauseDialog() {
        if (pauseDialog != null && pauseDialog.isShowing()) return;
        final SoundManager sm = SoundManager.get(this);
        final Dialog d = Dialogui.make(this, R.layout.critter_dialog_pause);
        pauseDialog = d;

        d.setCanceledOnTouchOutside(false);
        d.setOnCancelListener(x -> game.resumeGame());      // phone's back button = resume
        d.setOnDismissListener(x -> { if (pauseDialog == d) pauseDialog = null; });

        View resume = d.findViewById(R.id.btnResume);
        View restart = d.findViewById(R.id.btnRestart);
        View menu = d.findViewById(R.id.btnMenu);

        resume.setBackground(Ui.yellow(this));
        restart.setBackground(Ui.wood(this));
        menu.setBackground(Ui.wood(this));
        Dialogui.press(resume); Dialogui.press(restart); Dialogui.press(menu);

        resume.setOnClickListener(v -> {
            sm.play(SoundManager.CLICK);
            d.dismiss();
            game.resumeGame();
        });
        restart.setOnClickListener(v -> {
            sm.play(SoundManager.CLICK);
            d.dismiss();
            game.restartGame();                             // fresh round, with the 3-2-1 countdown
        });
        menu.setOnClickListener(v -> {
            sm.play(SoundManager.CLICK);
            d.dismiss();
            finish();
        });

        Dialogui.show(d, true);
    }
}