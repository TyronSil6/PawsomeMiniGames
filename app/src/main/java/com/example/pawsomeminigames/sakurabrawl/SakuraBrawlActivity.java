package com.example.pawsomeminigames.sakurabrawl;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pawsomeminigames.Immersive;

public class SakuraBrawlActivity extends AppCompatActivity {

    private MenuView menuView;
    private GameView gameView;
    private SoundManager sound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Let the phone rotate (follows the system auto-rotate setting)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);

        Immersive.apply(this);
        FaceIcons.load(this);   // the animal face pictures
        sound = new SoundManager(this);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!handleBack()) {
                    // Nothing special to do: let Android close the app
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        showMenu();
    }

    private boolean handleBack() {
        if (gameView != null) return gameView.handleBack();
        if (menuView != null) return menuView.handleBack();
        return false;
    }

    private void showMenu() {
        if (gameView != null) {
            gameView.pause();
            gameView = null;
        }
        menuView = new MenuView(this, sound, new MenuView.Listener() {
            @Override
            public void onPlay(boolean twoPlayers) {
                showGame(twoPlayers);
            }

            @Override
            public void onQuit() {
                finish();
            }
        });
        setContentView(menuView);
        Immersive.apply(this);
        menuView.resume();
    }

    private void showGame(boolean twoPlayers) {
        if (menuView != null) {
            menuView.pause();
            menuView = null;
        }
        gameView = new GameView(this, sound, twoPlayers, new GameView.ExitListener() {
            @Override
            public void onExitToMenu() {
                showMenu();
            }
        });
        setContentView(gameView);
        Immersive.apply(this);
        gameView.resume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Immersive.apply(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sound.onAppResume();
        if (gameView != null) gameView.resume();
        else if (menuView != null) menuView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sound.onAppPause();
        if (gameView != null) gameView.pause();
        else if (menuView != null) menuView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sound != null) sound.release();
    }
}