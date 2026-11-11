package com.example.pawsomeminigames;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pawsomeminigames.crittertoss.MenuActivity;
import com.example.pawsomeminigames.popmania.PopManiaActivity;
import com.example.pawsomeminigames.reactiongame.MainMenuActivity;
import com.example.pawsomeminigames.sakurabrawl.SakuraBrawlActivity;

public class MainActivity extends AppCompatActivity {

    private ValueAnimator bannerPulse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Immersive.apply(this);

        View cardSakura  = findViewById(R.id.cardSakura);
        View cardCritter = findViewById(R.id.cardCritter);
        View cardPop     = findViewById(R.id.cardPop);
        View cardReaction   = findViewById(R.id.cardReaction);

        setupCard(cardSakura,  R.drawable.icon_sakura_brawl, "Sakura Brawl", null, true);
        setupCard(cardCritter, R.drawable.icon_critter_toss, "Critter Toss", null, true);
        setupCard(cardPop,     R.drawable.icon_pop_mania,    "Pop Mania", null, true);
        setupCard(cardReaction, R.drawable.icon_reaction_game, "Reaction Game", null, true);

        // Critter Toss look for the sign + buttons (all drawn in code by Ui)
        ViewGroup banner = findViewById(R.id.bannerFrame);
        for (int i = 0; i < banner.getChildCount(); i++) {
            banner.getChildAt(i).setBackground(i == 0 ? Ui.banner(this) : Ui.dot(this));
        }
        bannerPulse = Ui.pulse(banner);   // sign "breathes" like the Sakura Brawl logo
        View btnSettings = findViewById(R.id.btnSettings);
        View btnMore     = findViewById(R.id.btnMore);
        View btnQuit     = findViewById(R.id.btnQuit);
        View btnAbout    = findViewById(R.id.btnAbout);
        btnSettings.setBackground(Ui.yellow(this));
        btnMore.setBackground(Ui.yellow(this));
        btnAbout.setBackground(Ui.chip(this));
        btnQuit.setBackground(Ui.chip(this));

        cardSakura.setOnClickListener(v -> {
            HubSound.get(this).click();
            startActivity(new Intent(this, SakuraBrawlActivity.class));
        });
        cardCritter.setOnClickListener(v -> {
            HubSound.get(this).click();
            startActivity(new Intent(this, MenuActivity.class));   // Critter Toss title screen
        });
        cardPop.setOnClickListener(v -> {
            HubSound.get(this).click();
            startActivity(new Intent(this, PopManiaActivity.class));   // Pop Mania home screen
        });
        cardReaction.setOnClickListener(v -> {
            HubSound.get(this).click();
            startActivity(new Intent(this, MainMenuActivity.class));   // Reaction Game menu
        });

        // The one Settings window for every game (music + sound)
        btnSettings.setOnClickListener(v -> {
            HubSound.get(this).click();
            SettingsDialog.show(this);
        });
        btnMore.setOnClickListener(v -> {
            HubSound.get(this).click();
            HelpDialog.show(this);   // short app description + full instructions for each game
        });
        btnAbout.setOnClickListener(v -> {
            HubSound.get(this).click();
            ThemedDialog.showInfo(this, "ABOUT US",
                    "Pawsome Mini Games\nA collection of cute little games.\n\nProgrammed by:\nR-lene Tolentino\nJames Tolentino\n Lance Tyron Silvestre\nHazel Joy Sanque\nNorie Jane Paule\nAiza Kuyan", "OK");
        });
        btnQuit.setOnClickListener(v -> {
            HubSound.get(this).click();
            ThemedDialog.showConfirm(this, "QUIT",
                    "Do you really want to quit the game?", "YES", "NO",
                    this::finishAffinity);
        });

        for (View v : new View[]{cardSakura, cardCritter, cardPop, cardReaction,
                btnSettings, btnMore, btnQuit, btnAbout}) {
            addPressAnimation(v);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Immersive.apply(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerPulse != null && !bannerPulse.isStarted()) bannerPulse.start();
        HubMusic.get(this).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bannerPulse != null) bannerPulse.cancel();
        HubMusic.get(this).pause();
    }

    private void setupCard(View card, int iconRes, String title, String subtitle, boolean playable) {
        card.setBackground(Ui.card(this));
        card.findViewById(R.id.cardLabel).setBackground(Ui.wood(this));
        ImageView iconView = card.findViewById(R.id.cardIcon);
        Bitmap ready = Preloader.icon(iconRes);   // already loaded at a good size by the splash screen
        if (ready == null) ready = Preloader.loadTrimmed(this, iconRes);   // same trimmed size if it was not ready
        if (ready != null) iconView.setImageBitmap(ready); else iconView.setImageResource(iconRes);
        ((TextView) card.findViewById(R.id.cardTitle)).setText(title);
        TextView sub = card.findViewById(R.id.cardSubtitle);
        if (subtitle != null) {
            sub.setText(subtitle);
            sub.setVisibility(View.VISIBLE);
        }
        if (!playable) card.setAlpha(0.85f);
    }

    /** Squish-on-press effect; returns false so the click still fires. */
    @SuppressLint("ClickableViewAccessibility")
    private void addPressAnimation(View v) {
        v.setOnTouchListener((view, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    break;
            }
            return false;
        });
    }
}