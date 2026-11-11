package com.example.pawsomeminigames.reactiongame;

import com.example.pawsomeminigames.R;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;

import com.example.pawsomeminigames.reactiongame.audio.SoundManager;
import com.example.pawsomeminigames.reactiongame.game.GameManager;
import com.example.pawsomeminigames.reactiongame.game.GameOverReason;
import com.example.pawsomeminigames.reactiongame.game.GameState;
import com.example.pawsomeminigames.reactiongame.game.Round;
import com.example.pawsomeminigames.reactiongame.game.Tile;
import com.example.pawsomeminigames.reactiongame.game.TileRole;
import com.example.pawsomeminigames.reactiongame.ui.AnimalAssets;
import com.example.pawsomeminigames.reactiongame.ui.OutlineTextView;
import com.example.pawsomeminigames.reactiongame.ui.TileGridLayout;
import com.example.pawsomeminigames.reactiongame.ui.TileView;
import com.example.pawsomeminigames.reactiongame.ui.TimerBarView;

import java.util.Locale;

public class GameActivity extends BaseActivity {
    private static final long INTRO_STEP_MS = 700L;
    private static final long GO_MS = 550L;
    private static final long TICK_MS = 33L;
    private static final long NEXT_ROUND_DELAY_MS = 520L;
    private static final long OVER_DELAY_WRONG_MS = 1150L;
    private static final long OVER_DELAY_TIMEOUT_MS = 950L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private GameManager game;

    private TileGridLayout tileGrid;
    private final TileView[] tileViews =
            new TileView[GameManager.TILE_COUNT];

    private TileView cueTile;
    private View cuePanel;
    private TextView cueName;
    private TextView scoreText;
    private TextView timerText;
    private View timerChip;
    private TimerBarView timerBar;
    private View introOverlay;
    private OutlineTextView introText;
    private View pauseOverlay;
    private View overOverlay;

    private long remainingMs;
    private long lastTickUptime;

    private boolean runCommitted;
    private boolean overShown;
    private boolean newBest;
    private boolean shuffledThisRound;

    private boolean warned1;
    private boolean warned2;
    private boolean warned3;

    private int introStep;

    private ObjectAnimator pulse;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            onTick();
        }
    };

    private final Runnable nextRoundRunnable = new Runnable() {
        @Override
        public void run() {
            if (game.getState() == GameState.RESOLVING) {
                beginNextRound();
            }
        }
    };

    private final Runnable introRunnable = new Runnable() {
        @Override
        public void run() {
            advanceIntro();
        }
    };

    private final Runnable showOverRunnable = new Runnable() {
        @Override
        public void run() {
            showGameOver();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setScreen(R.layout.rg_activity_game);

        tileGrid = findViewById(R.id.tile_grid);
        cuePanel = findViewById(R.id.cue_panel);
        cueName = findViewById(R.id.cue_name);
        scoreText = findViewById(R.id.score_text);
        timerText = findViewById(R.id.timer_text);
        timerChip = findViewById(R.id.timer_chip);
        timerBar = findViewById(R.id.timer_bar);
        introOverlay = findViewById(R.id.intro_overlay);
        introText = findViewById(R.id.intro_text);
        pauseOverlay = findViewById(R.id.pause_overlay);
        overOverlay = findViewById(R.id.over_overlay);

        // Pause menu buttons use the same colors as Critter Toss, Pop Mania and Sakura Brawl:
        // RESUME yellow, RESTART / MAIN MENU dark with the gold edge
        stylePauseButton(R.id.btn_resume, com.example.pawsomeminigames.Ui.yellow(this), "#5A3519");
        stylePauseButton(R.id.btn_restart, com.example.pawsomeminigames.Ui.wood(this), "#FFE3A0");
        stylePauseButton(R.id.btn_pause_menu, com.example.pawsomeminigames.Ui.wood(this), "#FFE3A0");

        for (int i = 0; i < tileViews.length; i++) {
            final int index = i;

            TileView tv = new TileView(this);

            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onTileClicked(index);
                }
            });

            tileGrid.addView(tv);
            tileViews[i] = tv;
        }

        cueTile = new TileView(this);
        cueTile.setClickable(false);
        cueTile.setFocusable(false);

        ((FrameLayout) findViewById(R.id.cue_slot)).addView(
                cueTile,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        onTap(R.id.btn_pause, new Runnable() {
            @Override
            public void run() {
                pauseGame(true);
            }
        });

        onTap(R.id.btn_resume, new Runnable() {
            @Override
            public void run() {
                resumeGame();
            }
        });

        onTap(R.id.btn_restart, new Runnable() {
            @Override
            public void run() {
                startRun();
            }
        });

        onTap(R.id.btn_pause_menu, new Runnable() {
            @Override
            public void run() {
                goToMainMenu();
            }
        });

        onTap(R.id.btn_retry, new Runnable() {
            @Override
            public void run() {
                startRun();
            }
        });

        onTap(R.id.btn_over_menu, new Runnable() {
            @Override
            public void run() {
                goToMainMenu();
            }
        });

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        onBack();
                    }
                }
        );

        startRun();
    }

    @Override
    protected SoundManager.Track musicTrack() {
        return SoundManager.Track.GAME;
    }

    @Override
    protected boolean musicStartsSuspended() {
        return game != null &&
                (game.getState() == GameState.PAUSED ||
                        game.getState() == GameState.GAME_OVER);
    }

    @Override
    protected void onPause() {
        pauseGame(true);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopPulse();
        super.onDestroy();
    }

    private void onBack() {
        if (overOverlay.getVisibility() == View.VISIBLE) {
            goToMainMenu();

        } else if (pauseOverlay.getVisibility() == View.VISIBLE) {
            resumeGame();

        } else if (game.isGameOver()) {
            handler.removeCallbacks(showOverRunnable);
            showGameOver();

        } else {
            pauseGame(true);
        }
    }

    private void startRun() {
        cancelAllCallbacks();
        stopPulse();

        game = new GameManager(System.nanoTime());
        game.startNewRun();

        runCommitted = false;
        overShown = false;
        newBest = false;

        pauseOverlay.setVisibility(View.GONE);
        overOverlay.setVisibility(View.GONE);

        updateScore(false);

        timerBar.setProgress(1f);
        timerText.setText("");

        tileGrid.setVisibility(View.INVISIBLE);
        cuePanel.setVisibility(View.INVISIBLE);

        sound.playMusic(SoundManager.Track.GAME, false);

        startIntro();
    }

    private void cancelAllCallbacks() {
        handler.removeCallbacks(tickRunnable);
        handler.removeCallbacks(nextRoundRunnable);
        handler.removeCallbacks(introRunnable);
        handler.removeCallbacks(showOverRunnable);
    }

    private void startIntro() {
        introStep = 4;
        introOverlay.setVisibility(View.VISIBLE);
        advanceIntro();
    }

    private void advanceIntro() {
        introStep--;

        if (introStep >= 1) {
            showIntroText(String.valueOf(introStep));
            sound.playSfx(SoundManager.Sfx.COUNTDOWN);

            handler.postDelayed(introRunnable, INTRO_STEP_MS);

        } else if (introStep == 0) {
            showIntroText(getString(R.string.rg_go));
            sound.playSfx(SoundManager.Sfx.START);

            handler.postDelayed(introRunnable, GO_MS);

        } else {
            introOverlay.setVisibility(View.GONE);
            beginNextRound();
        }
    }

    private void showIntroText(String text) {
        introText.setText(text);
        introText.setAlpha(0f);
        introText.setScaleX(1.7f);
        introText.setScaleY(1.7f);

        introText.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(280)
                .setInterpolator(new OvershootInterpolator(1.8f))
                .start();
    }

    private void beginNextRound() {
        Round round = game.beginRound();

        remainingMs = round.timeLimitMs;

        shuffledThisRound = false;
        warned1 = false;
        warned2 = false;
        warned3 = false;

        stopPulse();

        cuePanel.setVisibility(View.VISIBLE);
        tileGrid.setVisibility(View.VISIBLE);

        Tile cue = new Tile(
                round.cueAnimal,
                round.cueColor,
                TileRole.CORRECT
        );

        cueTile.bind(cue);
        cueTile.popIn(0);

        String animal = getString(
                AnimalAssets.nameRes(round.cueAnimal)
        );

        String color = getString(
                AnimalAssets.colorNameRes(round.cueColor)
        );

        cueName.setText(
                getString(R.string.rg_cd_tile_fmt, animal, color)
        );

        cuePanel.setContentDescription(
                getString(R.string.rg_cd_cue_fmt, animal, color)
        );

        bindTiles(true);

        updateTimerUi(round);

        lastTickUptime = SystemClock.uptimeMillis();

        handler.removeCallbacks(tickRunnable);
        handler.postDelayed(tickRunnable, TICK_MS);
    }

    private void bindTiles(boolean animateIn) {
        Round round = game.getRound();

        for (int i = 0; i < tileViews.length; i++) {
            tileViews[i].bind(round.getTile(i));

            if (animateIn) {
                tileViews[i].popIn(i * 45L);
            }
        }
    }

    private void onTick() {
        if (game.getState() != GameState.PLAYING) return;

        long now = SystemClock.uptimeMillis();

        remainingMs -= (now - lastTickUptime);
        lastTickUptime = now;

        Round round = game.getRound();

        if (remainingMs <= 0) {
            remainingMs = 0;
            updateTimerUi(round);
            onTimeUp();

            return;
        }

        if (round.willShuffle()
                && !shuffledThisRound
                && remainingMs <= (long) (
                round.timeLimitMs * round.shuffleAtFraction
        )) {
            shuffledThisRound = true;

            if (game.shuffleCurrentRound()) {
                bindTiles(false);

                for (TileView tv : tileViews) {
                    tv.flip();
                }

                sound.playSfx(SoundManager.Sfx.CLICK);
            }
        }

        long w1 = Math.min(1500L, round.timeLimitMs / 2);

        if (!warned1 && remainingMs <= w1) {
            warned1 = true;
            sound.playSfx(SoundManager.Sfx.COUNTDOWN);
            startPulse();
        }

        if (!warned2 && remainingMs <= w1 * 2 / 3) {
            warned2 = true;
            sound.playSfx(SoundManager.Sfx.COUNTDOWN);
        }

        if (!warned3 && remainingMs <= w1 / 3) {
            warned3 = true;
            sound.playSfx(SoundManager.Sfx.COUNTDOWN);
        }

        updateTimerUi(round);

        handler.postDelayed(tickRunnable, TICK_MS);
    }

    private void onTileClicked(int index) {
        GameManager.AnswerResult result =
                game.submitAnswer(index);

        if (result == GameManager.AnswerResult.IGNORED) {
            return;
        }

        handler.removeCallbacks(tickRunnable);
        stopPulse();

        if (result == GameManager.AnswerResult.CORRECT) {
            sound.playSfx(SoundManager.Sfx.CORRECT);
            sound.vibrate(25);

            tileViews[index].showCorrect();

            updateScore(true);

            handler.postDelayed(
                    nextRoundRunnable,
                    NEXT_ROUND_DELAY_MS
            );

        } else {
            endRun(index);
        }
    }

    private void onTimeUp() {
        if (game.timeUp()) {
            stopPulse();
            endRun(-1);
        }
    }

    private void endRun(int tappedIndex) {
        handler.removeCallbacks(tickRunnable);
        handler.removeCallbacks(nextRoundRunnable);

        sound.playSfx(SoundManager.Sfx.WRONG);
        sound.vibrate(tappedIndex >= 0 ? 160 : 100);

        int correct = game.getRound().getCorrectIndex();

        for (int i = 0; i < tileViews.length; i++) {
            if (i == tappedIndex) {
                tileViews[i].showWrong();

            } else if (i == correct) {
                tileViews[i].showReveal();

            } else {
                tileViews[i].dim();
            }
        }

        commitRun();

        handler.removeCallbacks(showOverRunnable);

        handler.postDelayed(
                showOverRunnable,
                tappedIndex >= 0
                        ? OVER_DELAY_WRONG_MS
                        : OVER_DELAY_TIMEOUT_MS
        );
    }

    private void commitRun() {
        if (runCommitted) return;

        runCommitted = true;

        newBest = game.getScore() > 0 &&
                prefs.addScore(game.getScore());
    }

    private void showGameOver() {
        if (overShown) return;

        overShown = true;

        stopPulse();

        sound.setMusicSuspended(true);

        sound.playSfx(
                newBest
                        ? SoundManager.Sfx.HIGH_SCORE
                        : SoundManager.Sfx.GAME_OVER
        );

        Round round = game.getRound();

        ((TextView) findViewById(R.id.over_score))
                .setText(
                        getString(
                                R.string.rg_your_score_fmt,
                                game.getScore()
                        )
                );

        ((TextView) findViewById(R.id.over_best))
                .setText(
                        getString(
                                R.string.rg_best_score_fmt,
                                prefs.getBestScore()
                        )
                );

        findViewById(R.id.over_new_best)
                .setVisibility(newBest ? View.VISIBLE : View.GONE);

        ((ImageView) findViewById(R.id.over_animal))
                .setImageResource(
                        AnimalAssets.sad(round.cueAnimal)
                );

        String reason;

        Tile tapped = game.getLastTappedTile();

        if (game.getReason() == GameOverReason.TIMEOUT
                || tapped == null) {
            reason = getString(R.string.rg_over_timeout);

        } else {
            String a = getString(
                    AnimalAssets.nameRes(tapped.animal)
            );

            String c = getString(
                    AnimalAssets.colorNameRes(tapped.color)
            );

            if (tapped.role == TileRole.ANIMAL_TWIN) {
                reason = getString(
                        R.string.rg_over_decoy_animal,
                        a,
                        c
                );

            } else if (tapped.role == TileRole.COLOR_TWIN) {
                reason = getString(
                        R.string.rg_over_decoy_color,
                        a,
                        c
                );

            } else {
                reason = getString(R.string.rg_over_wrong);
            }
        }

        ((TextView) findViewById(R.id.over_reason))
                .setText(reason);

        ((TextView) findViewById(R.id.over_answer))
                .setText(
                        getString(
                                R.string.rg_over_answer_fmt,
                                getString(AnimalAssets.nameRes(round.cueAnimal)),
                                getString(AnimalAssets.colorNameRes(round.cueColor))
                        )
                );

        View card = findViewById(R.id.over_card);

        overOverlay.setVisibility(View.VISIBLE);
        overOverlay.setAlpha(0f);

        overOverlay.animate()
                .alpha(1f)
                .setDuration(220)
                .start();

        card.setScaleX(0.8f);
        card.setScaleY(0.8f);

        card.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(380)
                .setInterpolator(
                        new OvershootInterpolator(1.6f)
                )
                .start();
    }

    private void stylePauseButton(int id, android.graphics.drawable.Drawable background, String textColor) {
        TextView b = findViewById(id);
        if (b == null) return;
        b.setBackground(background);
        b.setPadding(b.getPaddingLeft(), 0, b.getPaddingRight(), 0);
        b.setTextColor(android.graphics.Color.parseColor(textColor));
        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
    }

    private void pauseGame(boolean showOverlay) {
        if (game == null || !game.pause()) return;

        handler.removeCallbacks(tickRunnable);
        handler.removeCallbacks(nextRoundRunnable);
        handler.removeCallbacks(introRunnable);

        stopPulse();

        sound.setMusicSuspended(true);

        if (showOverlay) {
            pauseOverlay.setVisibility(View.VISIBLE);
            pauseOverlay.setAlpha(0f);

            pauseOverlay.animate()
                    .alpha(1f)
                    .setDuration(160)
                    .start();
        }
    }

    private void resumeGame() {
        GameState previous = game.resume();

        if (previous == null) return;

        pauseOverlay.setVisibility(View.GONE);
        sound.setMusicSuspended(false);

        switch (previous) {
            case PLAYING:

                lastTickUptime = SystemClock.uptimeMillis();

                handler.removeCallbacks(tickRunnable);
                handler.postDelayed(tickRunnable, TICK_MS);

                break;

            case RESOLVING:

                handler.postDelayed(nextRoundRunnable, 200L);

                break;

            default:

                startIntro();
                break;
        }
    }

    private void goToMainMenu() {
        cancelAllCallbacks();
        commitRun();

        Intent i = new Intent(
                this,
                MainMenuActivity.class
        );

        i.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(i);
        finish();
    }

    private void updateScore(boolean animate) {
        scoreText.setText(
                getString(R.string.rg_score_fmt, game.getScore())
        );

        if (animate) {
            scoreText.setScaleX(1.18f);
            scoreText.setScaleY(1.18f);

            scoreText.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(260)
                    .setInterpolator(
                            new OvershootInterpolator(3f)
                    )
                    .start();
        }
    }

    private void updateTimerUi(Round round) {
        timerBar.setProgress(
                remainingMs / (float) round.timeLimitMs
        );

        timerText.setText(
                String.format(
                        Locale.US,
                        "%.1f",
                        remainingMs / 1000f
                )
        );

        boolean danger = remainingMs <=
                Math.min(1500L, round.timeLimitMs / 2);

        timerText.setTextColor(
                ContextCompat.getColor(
                        this,
                        danger
                                ? R.color.rg_timer_danger
                                : R.color.rg_cream
                )
        );
    }

    private void startPulse() {
        stopPulse();

        pulse = ObjectAnimator.ofFloat(
                timerChip,
                View.SCALE_X,
                1f,
                1.14f
        );

        pulse.setDuration(230);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);

        pulse.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        timerChip.setScaleY(
                                (float) animation.getAnimatedValue()
                        );
                    }
                }
        );

        pulse.start();
    }

    private void stopPulse() {
        if (pulse != null) {
            pulse.cancel();
            pulse = null;
        }

        if (timerChip != null) {
            timerChip.setScaleX(1f);
            timerChip.setScaleY(1f);
        }
    }
}