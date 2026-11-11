package com.example.pawsomeminigames.reactiongame;

import com.example.pawsomeminigames.R;
import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;

import com.example.pawsomeminigames.reactiongame.game.AnimalType;
import com.example.pawsomeminigames.reactiongame.game.Tile;
import com.example.pawsomeminigames.reactiongame.game.TileColor;
import com.example.pawsomeminigames.reactiongame.game.TileRole;
import com.example.pawsomeminigames.reactiongame.ui.TileGridLayout;
import com.example.pawsomeminigames.reactiongame.ui.TileView;

public class HowToPlayActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.rg_activity_how_to_play);
        float d = getResources().getDisplayMetrics().density;

        FrameLayout slot1 = findViewById(R.id.how_slot_1);
        TileGridLayout grid = new TileGridLayout(this);
        grid.setGap(Math.round(3 * d));
        AnimalType[] animals = {AnimalType.BEAR, AnimalType.RACCOON, AnimalType.FOX, AnimalType.SLOTH};
        TileColor[] colors = {TileColor.RED, TileColor.BLUE, TileColor.GREEN, TileColor.YELLOW};
        for (int i = 0; i < 4; i++) {
            TileView t = new TileView(this);
            t.setClickable(false);
            t.setFocusable(false);
            t.bind(new Tile(animals[i], colors[i], TileRole.FILLER));
            t.setContentDescription(null);
            grid.addView(t);
        }
        slot1.addView(grid, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        FrameLayout slot2 = findViewById(R.id.how_slot_2);
        TileView one = new TileView(this);
        one.setClickable(false);
        one.setFocusable(false);
        one.bind(new Tile(AnimalType.BEAR, TileColor.RED, TileRole.CORRECT));
        one.showCorrect();
        int size = Math.round(66 * d);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size, Gravity.CENTER);
        slot2.addView(one, lp);

        if (getIntent().getBooleanExtra("start_game", false)) {
            View start = findViewById(R.id.btn_start);
            start.setVisibility(View.VISIBLE);
            onTap(R.id.btn_start, new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(HowToPlayActivity.this, GameActivity.class));
                    finish();
                }
            });
        }

        onTap(R.id.btn_back, new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }
}
