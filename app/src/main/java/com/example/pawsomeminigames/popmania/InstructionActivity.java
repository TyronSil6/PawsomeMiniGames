package com.example.pawsomeminigames.popmania;

import com.example.pawsomeminigames.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.pawsomeminigames.Ui;

public class InstructionActivity extends Activity {
    private SoundManager soundManager;
    private TextView tvDifficulty;
    private TextView tvDescription;
    private int selectedMode = 0;

    private final String[] modes = {"EASY", "MEDIUM", "HARD"};
    private final String[] descriptions = {
            "40 seconds • Slower critters • More time to aim",
            "30 seconds • Balanced speed • A fun challenge",
            "30 seconds • Fast critters • Quick reactions needed"
    };
    private final String[] stars = {"★ EASY", "★★ NORMAL", "★★★ HARD"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pm_activity_how_to_play);
        soundManager = new SoundManager(this);

        tvDifficulty = findViewById(R.id.tvDifficulty);
        tvDescription = findViewById(R.id.tvDifficultyDescription);
        View previous = findViewById(R.id.btnPreviousMode);
        View next = findViewById(R.id.btnNextMode);
        View play = findViewById(R.id.btnStartGame);
        View back = findViewById(R.id.btnHowBack);

        // Same PLAY button color as Critter Toss and Sakura Brawl
        play.setBackground(Ui.red(this));
        play.setPadding(play.getPaddingLeft(), 0, play.getPaddingRight(), 0);
        tvDifficulty.setPadding(0, 0, 0, 0);

        UiUtils.addPressEffect(previous);
        UiUtils.addPressEffect(next);
        UiUtils.addPressEffect(play);
        UiUtils.addPressEffect(back);

        String saved = getSharedPreferences("popmania_settings", MODE_PRIVATE)
                .getString("difficulty", "MEDIUM");
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(saved)) selectedMode = i;
        }
        updateModeDisplay();

        previous.setOnClickListener(v -> {
            soundManager.playClick();
            selectedMode = (selectedMode + modes.length - 1) % modes.length;
            updateModeDisplay();
        });

        next.setOnClickListener(v -> {
            soundManager.playClick();
            selectedMode = (selectedMode + 1) % modes.length;
            updateModeDisplay();
        });

        play.setOnClickListener(v -> {
            soundManager.playClick();
            getSharedPreferences("popmania_settings", MODE_PRIVATE)
                    .edit().putString("difficulty", modes[selectedMode]).apply();
            startActivity(new Intent(InstructionActivity.this, GameActivity.class));
        });

        back.setOnClickListener(v -> {
            soundManager.playClick();
            finish();
        });
    }

    private void updateModeDisplay() {
        tvDifficulty.setText(stars[selectedMode]);
        // Same level colors as Critter Toss / Sakura Brawl: green = easy, yellow = normal, red = hard
        tvDifficulty.setBackground(selectedMode == 0 ? Ui.green(this)
                : selectedMode == 1 ? Ui.yellow(this) : Ui.red(this));
        tvDifficulty.setTextColor(selectedMode == 1 ? Color.parseColor("#5A3519") : Color.WHITE);
        tvDescription.setText(descriptions[selectedMode]);
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiUtils.enterFullscreen(this);
        if (soundManager != null) soundManager.startMenuMusic();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (soundManager != null) soundManager.pauseMusic();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) UiUtils.enterFullscreen(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundManager != null) soundManager.release();
    }
}