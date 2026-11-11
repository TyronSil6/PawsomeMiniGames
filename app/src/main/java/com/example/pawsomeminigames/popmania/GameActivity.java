package com.example.pawsomeminigames.popmania;

import com.example.pawsomeminigames.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.pawsomeminigames.Ui;

import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The game screen: 3x3 board, 30-second timer, score, hammer effect,
 * pause menu and game-over menu.
 *
 * How time works: a "tick" runs every 40 ms. Each tick measures how much real
 * time passed and subtracts it from the game timer and the spawn timer.
 * Pausing simply stops the ticks, so nothing moves while the game is paused.
 */
public class GameActivity extends Activity implements HoleView.Listener {

    // ----- game rules (easy to tweak) -----
    private static final long TICK_MS = 40;
    private long gameDurationMs = 40000;
    private long firstSpawnDelayMs = 500;
    private long spawnIntervalStartMs = 850;
    private long spawnIntervalEndMs = 480;
    private long stayStartMs = 1250;
    private long stayEndMs = 800;
    private long goldenStayMs = 750;

    // The hammer image is 420 x 330 pixels.
    private static final float HAMMER_ASPECT = 330f / 420f;
    private static final float HAMMER_END_ANGLE = -10f;
    private static final float HAMMER_START_ANGLE = 40f;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private final HoleView[] holes = new HoleView[9];
    private FrameLayout fxLayer;
    private ImageView imgHammer;
    private TextView tvScore;
    private TextView tvTime;
    private View pauseOverlay;
    private View gameOverOverlay;
    private TextView tvFinalScore;
    private TextView tvNewBest;
    private TextView tvBestScoreOver;
    private TextView tvGameOverTitle;

    private ScoreManager scoreManager;
    private SoundManager soundManager;

    private int score;
    private long timeLeftMs;
    private long spawnCountdownMs;
    private long lastTickTime;
    private int hammerToken;     // lets an old hammer animation know a newer swing replaced it
    private boolean running;     // a round is in progress (may be paused)
    private boolean paused;
    private boolean gameOver;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            onTick();
        }
    };

    // ------------------------------------------------------------------
    // Activity lifecycle
    // ------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pm_activity_game);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        scoreManager = new ScoreManager(this);
        soundManager = new SoundManager(this);
        applyDifficulty();

        tvScore = findViewById(R.id.tvScore);
        tvTime = findViewById(R.id.tvTime);
        fxLayer = findViewById(R.id.fxLayer);
        imgHammer = findViewById(R.id.imgHammer);
        pauseOverlay = findViewById(R.id.pauseOverlay);
        gameOverOverlay = findViewById(R.id.gameOverOverlay);
        tvFinalScore = findViewById(R.id.tvFinalScore);
        tvNewBest = findViewById(R.id.tvNewBest);
        tvBestScoreOver = findViewById(R.id.tvBestScoreOver);
        tvGameOverTitle = findViewById(R.id.tvGameOverTitle);

        int[] holeIds = {
                R.id.hole0, R.id.hole1, R.id.hole2,
                R.id.hole3, R.id.hole4, R.id.hole5,
                R.id.hole6, R.id.hole7, R.id.hole8
        };
        for (int i = 0; i < holes.length; i++) {
            holes[i] = findViewById(holeIds[i]);
            holes[i].setListener(this);
        }

        setupBoardAndHammerSize();
        setupButtons();

        // Start the first round once the screen is ready.
        findViewById(R.id.gameRoot).post(new Runnable() {
            @Override
            public void run() {
                startGame();
            }
        });
    }

    /** Makes the board a square that fits the screen, and sizes the hammer to match. */
    private void setupBoardAndHammerSize() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int boardSize = (int) Math.min(dm.widthPixels * 0.94f, dm.heightPixels * 0.60f);

        View board = findViewById(R.id.boardFrame);
        FrameLayout.LayoutParams boardParams = (FrameLayout.LayoutParams) board.getLayoutParams();
        boardParams.width = boardSize;
        boardParams.height = boardSize;
        board.setLayoutParams(boardParams);

        int hammerWidth = (int) (boardSize / 3f * 1.05f);
        FrameLayout.LayoutParams hammerParams =
                (FrameLayout.LayoutParams) imgHammer.getLayoutParams();
        hammerParams.width = hammerWidth;
        hammerParams.height = (int) (hammerWidth * HAMMER_ASPECT);
        imgHammer.setLayoutParams(hammerParams);
    }

    private void styleButton(View button, android.graphics.drawable.Drawable background, String textColor) {
        button.setBackground(background);
        button.setPadding(button.getPaddingLeft(), 0, button.getPaddingRight(), 0);
        ((TextView) button).setTextColor(android.graphics.Color.parseColor(textColor));
    }

    private void setupButtons() {
        View btnPause = findViewById(R.id.btnPause);
        View btnResume = findViewById(R.id.btnResume);
        View btnRestart = findViewById(R.id.btnRestart);
        View btnMenuPause = findViewById(R.id.btnMenuPause);
        View btnPlayAgain = findViewById(R.id.btnPlayAgain);
        View btnMenuOver = findViewById(R.id.btnMenuOver);

        // Same button colors as Critter Toss and Sakura Brawl:
        // RESUME yellow, RESTART / MAIN MENU dark, PLAY AGAIN red
        styleButton(btnResume, Ui.yellow(this), "#5A3519");
        styleButton(btnRestart, Ui.wood(this), "#FFE3A0");
        styleButton(btnMenuPause, Ui.wood(this), "#FFE3A0");
        styleButton(btnPlayAgain, Ui.red(this), "#FFFFFF");
        styleButton(btnMenuOver, Ui.wood(this), "#FFE3A0");

        UiUtils.addPressEffect(btnPause);
        UiUtils.addPressEffect(btnResume);
        UiUtils.addPressEffect(btnRestart);
        UiUtils.addPressEffect(btnMenuPause);
        UiUtils.addPressEffect(btnPlayAgain);
        UiUtils.addPressEffect(btnMenuOver);

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (soundManager != null) soundManager.playClick();
                pauseGame();
            }
        });
        btnResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (soundManager != null) soundManager.playClick();
                resumeGame();
            }
        });
        View.OnClickListener playAgain = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (soundManager != null) soundManager.playClick();
                startGame();
            }
        };
        btnRestart.setOnClickListener(playAgain);
        btnPlayAgain.setOnClickListener(playAgain);

        View.OnClickListener backToMenu = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (soundManager != null) soundManager.playClick();
                finish();
            }
        };
        btnMenuPause.setOnClickListener(backToMenu);
        btnMenuOver.setOnClickListener(backToMenu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiUtils.enterFullscreen(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            UiUtils.enterFullscreen(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (soundManager != null) soundManager.pauseMusic();
        // Leaving the app (or locking the screen) pauses the game automatically.
        pauseGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        for (HoleView hole : holes) {
            hole.clear();
        }
        if (soundManager != null) soundManager.release();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (gameOver) {
            finish();
        } else if (paused) {
            resumeGame();
        } else {
            pauseGame();
        }
    }

    // ------------------------------------------------------------------
    // Round control: start / pause / resume / end
    // ------------------------------------------------------------------
    private void startGame() {
        handler.removeCallbacks(tickRunnable);
        if (soundManager != null) soundManager.startGameMusic(true);
        for (HoleView hole : holes) {
            hole.clear();
        }
        fxLayer.removeAllViews();
        fxLayer.addView(imgHammer);
        hammerToken++;
        imgHammer.animate().cancel();
        imgHammer.setVisibility(View.INVISIBLE);

        score = 0;
        timeLeftMs = gameDurationMs;
        spawnCountdownMs = firstSpawnDelayMs;
        running = true;
        paused = false;
        gameOver = false;

        tvScore.setText(String.valueOf(score));
        updateTimeText();
        pauseOverlay.setVisibility(View.GONE);
        gameOverOverlay.setVisibility(View.GONE);

        lastTickTime = SystemClock.uptimeMillis();
        handler.postDelayed(tickRunnable, TICK_MS);
    }

    private void pauseGame() {
        if (!running || paused || gameOver) {
            return;
        }
        paused = true;
        if (soundManager != null) soundManager.pauseMusic();
        handler.removeCallbacks(tickRunnable);
        for (HoleView hole : holes) {
            hole.pauseAnim();
        }
        pauseOverlay.setVisibility(View.VISIBLE);
    }

    private void resumeGame() {
        if (!running || !paused || gameOver) {
            return;
        }
        paused = false;
        if (soundManager != null) soundManager.startGameMusic(false);
        pauseOverlay.setVisibility(View.GONE);
        for (HoleView hole : holes) {
            hole.resumeAnim();
        }
        lastTickTime = SystemClock.uptimeMillis();
        handler.postDelayed(tickRunnable, TICK_MS);
    }

    private void endGame(boolean byBomb) {
        running = false;
        gameOver = true;
        handler.removeCallbacks(tickRunnable);
        for (HoleView hole : holes) {
            hole.clear();
        }

        boolean newBest = scoreManager.saveIfBest(score);
        if (soundManager != null) {
            soundManager.stopMusic();
            // A new best gets the happy fanfare; otherwise the game-over jingle.
            // After a bomb we wait a moment so the explosion can be heard first.
            final boolean celebrate = newBest;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (soundManager == null) return;
                    if (celebrate) {
                        soundManager.playBest();
                    } else {
                        soundManager.playGameOver();
                    }
                }
            }, byBomb ? 450 : 0);
        }

        tvFinalScore.setText(String.valueOf(score));
        tvGameOverTitle.setText(byBomb ? R.string.pm_gameover_bomb_title : R.string.pm_gameover_title);
        tvNewBest.setVisibility(newBest ? View.VISIBLE : View.GONE);
        tvBestScoreOver.setText(getString(R.string.pm_best_score_format, scoreManager.getBestScore()));
        tvBestScoreOver.setVisibility(View.VISIBLE);
        gameOverOverlay.setVisibility(View.VISIBLE);
    }

    // ------------------------------------------------------------------
    // The game loop
    // ------------------------------------------------------------------
    private void onTick() {
        if (!running || paused) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        long delta = Math.min(now - lastTickTime, 100);   // cap so a lag spike can't skip time
        lastTickTime = now;

        timeLeftMs -= delta;
        if (timeLeftMs <= 0) {
            timeLeftMs = 0;
            updateTimeText();
            endGame(false);
            return;
        }
        updateTimeText();

        spawnCountdownMs -= delta;
        if (spawnCountdownMs <= 0) {
            spawnCritter();
            spawnCountdownMs = nextSpawnInterval();
        }

        handler.postDelayed(tickRunnable, TICK_MS);
    }


    private void applyDifficulty() {
        String difficulty = getSharedPreferences("popmania_settings", MODE_PRIVATE)
                .getString("difficulty", "MEDIUM");
        if ("EASY".equals(difficulty)) {
            gameDurationMs = 40000;
            firstSpawnDelayMs = 650;
            spawnIntervalStartMs = 1150; spawnIntervalEndMs = 800;
            stayStartMs = 1650; stayEndMs = 1200; goldenStayMs = 1000;
        } else if ("HARD".equals(difficulty)) {
            gameDurationMs = 30000;
            firstSpawnDelayMs = 350;
            spawnIntervalStartMs = 620; spawnIntervalEndMs = 320;
            stayStartMs = 900; stayEndMs = 520; goldenStayMs = 480;
        } else {
            gameDurationMs = 30000;
            firstSpawnDelayMs = 500;
            spawnIntervalStartMs = 850; spawnIntervalEndMs = 480;
            stayStartMs = 1250; stayEndMs = 800; goldenStayMs = 750;
        }
    }

    /** 0.0 at the start of the round, 1.0 at the end. */
    private float progress() {
        return 1f - (timeLeftMs / (float) gameDurationMs);
    }

    private long lerp(long from, long to, float t) {
        return (long) (from + (to - from) * t);
    }

    private long nextSpawnInterval() {
        long base = lerp(spawnIntervalStartMs, spawnIntervalEndMs, progress());
        float jitter = 0.8f + random.nextFloat() * 0.4f;   // 80% - 120%
        return (long) (base * jitter);
    }

    private void spawnCritter() {
        int maxAtOnce = progress() < 0.4f ? 2 : 3;

        List<HoleView> freeHoles = new ArrayList<>();
        int busy = 0;
        for (HoleView hole : holes) {
            if (hole.isOccupied()) {
                busy++;
            } else {
                freeHoles.add(hole);
            }
        }
        if (busy >= maxAtOnce || freeHoles.isEmpty()) {
            return;
        }

        HoleView hole = freeHoles.get(random.nextInt(freeHoles.size()));
        CritterType type = CritterType.pickRandom(random);
        long stay = type.isGolden() ? goldenStayMs : lerp(stayStartMs, stayEndMs, progress());
        hole.popUp(type, stay);
    }

    private void updateTimeText() {
        int seconds = (int) Math.ceil(timeLeftMs / 1000.0);
        tvTime.setText(String.valueOf(seconds));
        tvTime.setTextColor(getColor(
                seconds <= 5 ? R.color.pm_time_red : R.color.pm_score_yellow));
    }

    // ------------------------------------------------------------------
    // HoleView.Listener
    // ------------------------------------------------------------------


    @Override
    public void onCritterHit(HoleView hole, CritterType type) {

        if (!running || paused || gameOver) {
            return;
        }

        // BOMB: GAME OVER
        if (type.isBomb()) {
            if (soundManager != null) soundManager.playBomb();
            playStrikeEffect(hole, type);
            endGame(true);
            return;
        }

        // TIMER: add 10 seconds without changing the player's score.
        if (type.isTimer()) {
            timeLeftMs += type.getTimeBonus() * 1000L;
            updateTimeText();
        }

        // Update score for normal characters and snake.
        // TIMER has 0 points, so it does not increase the score.
        score += type.points;

        // Prevent score from going below zero
        score = Math.max(0, score);

        tvScore.setText(String.valueOf(score));

        // Play the sound that matches what was hit
        if (soundManager != null) {
            if (type.isGolden()) {
                soundManager.playGolden();
            } else if (type.isSnake()) {
                soundManager.playSnake();
            } else {
                soundManager.playPop();
            }
        }
        bumpScore();
        playStrikeEffect(hole, type);
    }



    @Override
    public void onCritterEscaped(HoleView hole, CritterType type) {
        // Nothing happens when a critter gets away (no penalty in Phase 1).
    }

    // ------------------------------------------------------------------
    // Visual effects
    // ------------------------------------------------------------------
    private void bumpScore() {
        tvScore.animate().cancel();
        tvScore.setScaleX(1f);
        tvScore.setScaleY(1f);
        tvScore.animate().scaleX(1.3f).scaleY(1.3f).setDuration(70).withEndAction(new Runnable() {
            @Override
            public void run() {
                tvScore.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
            }
        }).start();
    }

    /** Hammer swing + impact burst + floating "+points" text at the tapped hole. */
    private void playStrikeEffect(HoleView hole, CritterType type) {
        int[] holeOnScreen = new int[2];
        int[] layerOnScreen = new int[2];
        hole.getLocationOnScreen(holeOnScreen);
        fxLayer.getLocationOnScreen(layerOnScreen);
        final float targetX = holeOnScreen[0] - layerOnScreen[0] + hole.getTargetX();
        final float targetY = holeOnScreen[1] - layerOnScreen[1] + hole.getTargetY();

        swingHammer(targetX, targetY);
        showImpactBurst(targetX, targetY, hole.getWidth());
        if (!type.isBomb()) {
            showFloatingPoints(targetX, targetY - hole.getHeight() * 0.15f, type);
        }
    }

    private void swingHammer(float targetX, float targetY) {
        float w = imgHammer.getLayoutParams().width;
        float h = imgHammer.getLayoutParams().height;

        // The hammer turns around the end of its handle.
        float pivotX = w * 0.88f;
        float pivotY = h * 0.85f;
        imgHammer.setPivotX(pivotX);
        imgHammer.setPivotY(pivotY);

        // Where is the middle of the hammer head when the swing ends? Rotate that point
        // around the pivot, then move the image so the head lands on the target.
        double angle = Math.toRadians(HAMMER_END_ANGLE);
        float vx = w * 0.34f - pivotX;
        float vy = h * 0.37f - pivotY;
        float headX = (float) (pivotX + vx * Math.cos(angle) - vy * Math.sin(angle));
        float headY = (float) (pivotY + vx * Math.sin(angle) + vy * Math.cos(angle));

        final int token = ++hammerToken;
        imgHammer.animate().cancel();
        imgHammer.setTranslationX(targetX - headX);
        imgHammer.setTranslationY(targetY - headY);
        imgHammer.setRotation(HAMMER_START_ANGLE);
        imgHammer.setAlpha(1f);
        imgHammer.setVisibility(View.VISIBLE);

        // 1) swing down  2) short hold  3) fade out
        imgHammer.animate()
                .setStartDelay(0)
                .rotation(HAMMER_END_ANGLE)
                .setDuration(110)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (token != hammerToken) {
                            return;     // a newer swing has started
                        }
                        imgHammer.animate()
                                .setStartDelay(120)
                                .alpha(0f)
                                .setDuration(160)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (token == hammerToken) {
                                            imgHammer.setVisibility(View.INVISIBLE);
                                        }
                                    }
                                })
                                .start();
                    }
                })
                .start();
    }

    private void showImpactBurst(float x, float y, int holeWidth) {
        final ImageView burst = new ImageView(this);
        burst.setImageResource(R.drawable.pm_ic_impact_burst);
        burst.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int size = (int) (holeWidth * 0.9f);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        burst.setLayoutParams(params);
        burst.setTranslationX(x - size / 2f);
        burst.setTranslationY(y - size / 2f);
        burst.setScaleX(0.3f);
        burst.setScaleY(0.3f);
        fxLayer.addView(burst);

        burst.animate()
                .scaleX(1.3f).scaleY(1.3f).alpha(0f)
                .setDuration(300)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        fxLayer.removeView(burst);
                    }
                })
                .start();
    }

    private void showFloatingPoints(float x, float y, CritterType type) {
        final TextView label = new TextView(this);
        if (type.isTimer()) {
            label.setText("+" + type.getTimeBonus() + " SEC");
            label.setTextColor(getColor(R.color.pm_score_yellow));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        } else if (type.isGolden()) {
            label.setText(getString(R.string.pm_points_golden_format, type.points));
            label.setTextColor(getColor(R.color.pm_golden));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        } else {
            if (type.points > 0) {
                label.setText(getString(R.string.pm_points_format, type.points));
            } else {
                label.setText(String.valueOf(type.points));
            }
            label.setTextColor(getColor(
                    type == CritterType.STAR ? R.color.pm_score_yellow : R.color.pm_text_white));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        }
        label.setTypeface(ResourcesCompat.getFont(this, R.font.pm_outfit_bold));
        label.setShadowLayer(6f, 0f, 3f, 0xFF000000);
        label.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        label.setLayoutParams(params);
        label.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        label.setTranslationX(x - label.getMeasuredWidth() / 2f);
        label.setTranslationY(y - label.getMeasuredHeight() / 2f);
        fxLayer.addView(label);

        float rise = 90f * getResources().getDisplayMetrics().density;
        label.animate()
                .translationYBy(-rise)
                .alpha(0f)
                .setDuration(750)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        fxLayer.removeView(label);
                    }
                })
                .start();
    }
}