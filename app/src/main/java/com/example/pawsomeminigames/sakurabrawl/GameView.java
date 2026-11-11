package com.example.pawsomeminigames.sakurabrawl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import java.util.Arrays;
import java.util.Random;

/** Custom View that draws the whole game, runs the game loop, and reads touch. */
public class GameView extends View {

    /** MainActivity implements this to go back to the main menu. */
    public interface ExitListener {
        void onExitToMenu();
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ringRect = new RectF();
    private final RectF outerRect = new RectF();
    private final RectF baseRingRect = new RectF();    // the ring at full size
    private final RectF baseOuterRect = new RectF();

    private final SettingsManager settings;
    private final SoundManager sound;
    private final GameManager game;
    private final ExitListener exitListener;
    private final boolean twoPlayer;                  // false = you vs the NPC
    private final int[] pointerOwner = new int[10];   // which player each finger controls (0 = none)

    // Floating stick per player (index 1 = P1, 2 = P2; index 0 unused)
    private static final float STICK_DEAD = 0.03f;   // drag shorter than this (x screen width) = just a tap
    private static final float STICK_MAX = 0.10f;    // how far the stick can stretch (x screen width)
    private final boolean[] stickActive = new boolean[3];
    private final boolean[] dashPending = new boolean[3];  // a fresh tap that still needs its one dash
    private final boolean[] swipeUsed = new boolean[3];    // this touch already did its one swipe dash
    private final float[] stickAnchorX = new float[3];
    private final float[] stickAnchorY = new float[3];
    private final float[] stickX = new float[3];
    private final float[] stickY = new float[3];

    // Collision tuning
    private static final float CLASH_RESIST = 0.7f;  // when both push, how much of the other's power cancels yours
    private static final float RECOIL = 0.55f;       // speed the attacker loses on impact (was 0.8)

    // Which animals are fighting (the opponent is picked at random each match)
    private final Random rng = new Random();
    private int playerSpecies = Species.CAPYBARA;
    private int opponentSpecies = Species.RACCOON;

    private boolean running = false;
    private boolean paused = false;
    private long lastTimeNs;

    private float screenW, screenH;
    private Shader skyShader;

    // Arena pictures. If a picture is missing, a simple drawn arena is used instead.
    //  res/drawable/arena_bg       portrait picture (also used, cropped, in landscape if there is no landscape one)
    //  res/drawable/arena_bg_land  optional landscape picture (shown without stretching)
    // Positions below are fractions of the picture, measured on the painted ring.
    private static final float BG_RING_CX = 0.4980f;
    private static final float BG_RING_CY = 0.8021f;
    private static final float BG_RING_RX = 0.4590f;
    private static final float BG_RING_RY = 0.1159f;
    private static final float BG_RING_BOTTOM = 0.9277f;   // lowest point of the painted ring
    private static final float BG_SPLIT = 0.3906f;         // above this the picture may stretch
    private static final float BG_FOOT = 0.9505f;          // below this the picture may stretch
    // Ring position inside the landscape picture (adjust if your picture is different)
    private static final float LAND_RING_CX = 0.5000f;
    private static final float LAND_RING_CY = 0.6000f;
    private static final float LAND_RING_RX = 0.3100f;
    private static final float LAND_RING_RY = 0.2380f;

    private Bitmap background;                 // the picture in use right now
    private boolean backgroundIsLandscape = false;
    private Bitmap bgPortrait, bgLand;
    private boolean bgPortraitTried = false, bgLandTried = false;
    private boolean landscape = false;
    private float unit;                        // size unit: the short side of the screen
    private final Paint bgPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final android.graphics.Rect bgSrcTop = new android.graphics.Rect();
    private final android.graphics.Rect bgSrcMid = new android.graphics.Rect();
    private final android.graphics.Rect bgSrcFoot = new android.graphics.Rect();
    private final RectF bgDstTop = new RectF();
    private final RectF bgDstMid = new RectF();
    private final RectF bgDstFoot = new RectF();
    private final Path dangerPath = new Path();

    private Capybara player1;
    private Capybara player2;
    private NPCController npcController;
    private Petals petals;
    private Effects effects;
    private GameUI ui;
    private GameManager.State lastState = null;
    private String lastCountdownText = null;

    // Ring pressure: the ring closes in during the last seconds of a round
    private static final float MIN_RING_SCALE = 0.62f;   // smallest ring, as a fraction of full size
    private float shrink = 0f;                           // 0..1, eased
    private boolean shrinkAnnounced = false;
    private int lastTickSecond = -1;
    private float fxTime = 0f;

    // Slow motion when someone is knocked out of the ring
    private static final float SLOW_TIME = 0.8f;   // seconds of real time
    private float slowTimer = 0f;

    // Phone vibration
    private final Vibrator vibrator;
    private long lastBuzzMs = 0;
    private float p1StartX, p2StartX, startY;

    public GameView(Context context, SoundManager sound, boolean twoPlayer,
                    ExitListener exitListener) {
        super(context);
        this.sound = sound;
        this.twoPlayer = twoPlayer;
        this.exitListener = exitListener;
        settings = new SettingsManager(context);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        game = new GameManager(new GameManager.Listener() {
            @Override
            public void onNewRound(int round) {
                // Called at the start of every round
                if (player1 == null || npcController == null) return;
                if (round == 1) {
                    playerSpecies = settings.getCharacter();
                    // 2 players: P2 uses their own pick. 1 player: a random opponent.
                    opponentSpecies = twoPlayer ? settings.getCharacter2()
                            : Species.randomOpponent(playerSpecies, rng);
                }
                player1.setSpecies(playerSpecies);
                player2.setSpecies(opponentSpecies);
                if (ui != null) ui.setSpecies(playerSpecies, opponentSpecies);
                if (effects != null) effects.clear();
                slowTimer = 0f;
                clearSticks();
                shrinkAnnounced = false;
                lastTickSecond = -1;
                updateRing();               // the ring grows back to full size
                resetPositions();
                npcController.setLevel(settings.getDifficulty(), round);
            }
        });
    }

    // ---- Lifecycle, called from MainActivity ----
    public void resume() {
        running = true;
        lastTimeNs = System.nanoTime();
        invalidate();
    }

    /** Called when the app goes to the background: stop the loop and pause the match. */
    public void pause() {
        running = false;
        if (game.getState() != GameManager.State.GAME_OVER) {
            paused = true;
        }
    }

    /** Back button: toggles pause during a match, or returns to the menu after it. */
    public boolean handleBack() {
        if (game.getState() == GameManager.State.GAME_OVER) {
            exitListener.onExitToMenu();
        } else {
            paused = !paused;
            if (ui != null) ui.setPressed(GameUI.BTN_NONE);
        }
        return true;
    }

    // ---- Layout: runs whenever the size is known or changes (also when the phone is rotated) ----
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        screenW = w;
        screenH = h;
        landscape = w > h;
        unit = Math.min(w, h);

        // Remember where everything is (relative to the ring) so a rotation keeps the match going
        boolean firstLayout = player1 == null;
        float oldCx = 0f, oldCy = 0f, oldRx = 1f, oldRy = 1f;
        float[] keep = null;
        if (!firstLayout && !baseRingRect.isEmpty()) {
            oldCx = baseRingRect.centerX();
            oldCy = baseRingRect.centerY();
            oldRx = baseRingRect.width() / 2f;
            oldRy = baseRingRect.height() / 2f;
            keep = new float[]{player1.x, player1.y, player1.vx, player1.vy,
                    player2.x, player2.y, player2.vx, player2.vy};
        }

        skyShader = new LinearGradient(0, 0, 0, h * 0.45f,
                0xFF8FD3FF, 0xFFE3F6FF, Shader.TileMode.CLAMP);

        chooseBackground();

        float cx, cy, rx, ry;
        if (background != null) {
            layoutBackground(w, h);   // also sets ringRect to match the painted ring
        } else if (landscape) {
            ry = h * 0.33f;
            rx = Math.min(ry / 0.62f, w * 0.42f);
            setRing(w / 2f, h * 0.52f, rx, ry);
        } else {
            rx = w * 0.42f;
            setRing(w / 2f, h * 0.55f, rx, rx * 0.62f);
        }
        cx = ringRect.centerX();
        cy = ringRect.centerY();
        rx = ringRect.width() / 2f;
        ry = ringRect.height() / 2f;

        outerRect.set(ringRect);
        outerRect.inset(-unit * 0.02f, -unit * 0.02f);
        baseRingRect.set(ringRect);
        baseOuterRect.set(outerRect);

        // Wrestler size. In portrait: a bit smaller with the picture (its ring is flatter).
        // In landscape the ring is much wider than the screen is tall, so size follows the ring height.
        float size;
        if (landscape) {
            size = ry * (background != null ? 0.38f : 0.42f);
        } else {
            size = background != null ? w * 0.09f : w * 0.11f;
        }
        float fxUnit = size / (background != null ? 0.09f : 0.11f);   // particle size scale

        p1StartX = cx - rx * 0.45f;
        p2StartX = cx + rx * 0.45f;
        startY = cy;

        if (firstLayout) {
            player1 = new Capybara(p1StartX, startY, size, 1, 0xFF2F7DFF);   // blue belt
            player2 = new Capybara(p2StartX, startY, size, -1, 0xFFE8433A);  // red belt
            npcController = new NPCController(player2);
        } else if (keep != null) {
            // Same spots and speeds, measured against the new ring
            player1.radius = size;
            player2.radius = size;
            float kx = rx / oldRx, ky = ry / oldRy;
            player1.x = cx + (keep[0] - oldCx) * kx;
            player1.y = cy + (keep[1] - oldCy) * ky;
            player1.vx = keep[2] * kx;
            player1.vy = keep[3] * ky;
            player2.x = cx + (keep[4] - oldCx) * kx;
            player2.y = cy + (keep[5] - oldCy) * ky;
            player2.vx = keep[6] * kx;
            player2.vy = keep[7] * ky;
        }

        petals = new Petals(w, h, 14, unit);
        effects = new Effects(fxUnit);
        ui = new GameUI(w, h);
        clearSticks();

        if (firstLayout) {
            paused = false;
            game.startGame();
        } else {
            ui.setSpecies(playerSpecies, opponentSpecies);
            updateRing();
        }
    }

    private void setRing(float cx, float cy, float rx, float ry) {
        ringRect.set(cx - rx, cy - ry, cx + rx, cy + ry);
    }

    /** Picks the picture for the current orientation (loading it the first time it is needed). */
    private void chooseBackground() {
        background = null;
        backgroundIsLandscape = false;
        if (landscape) {
            if (!bgLandTried) {
                bgLandTried = true;
                bgLand = loadDrawable("arena_bg_land");
            }
            if (bgLand != null) {
                background = bgLand;
                backgroundIsLandscape = true;
                return;
            }
        }
        if (!bgPortraitTried) {
            bgPortraitTried = true;
            bgPortrait = loadDrawable("arena_bg");
        }
        background = bgPortrait;
    }

    /** Loads a picture from res/drawable by name, or returns null if it is not there. */
    private Bitmap loadDrawable(String name) {
        return Assets.bitmap(getContext(), name);   // cached: the splash screen already loaded it
    }

    /**
     * Fits the picture to the screen without stretching it, and puts ringRect exactly on the painted ring.
     *  - Landscape picture: scaled to cover the screen (only the edges can be cropped).
     *  - Portrait picture on a portrait screen: fitted by width. On tall screens only the sky at the
     *    top and the grass at the bottom stretch; the fence and ring keep their shape.
     *  - Portrait picture on a landscape screen: fitted by width and cropped so the ring is in view.
     */
    private void layoutBackground(int w, int h) {
        float bw = background.getWidth();
        float bh = background.getHeight();
        bgDstTop.setEmpty();
        bgDstFoot.setEmpty();

        if (backgroundIsLandscape) {
            float s = Math.max(w / bw, h / bh);
            float ox = (w - bw * s) / 2f;
            float oy = (h - bh * s) / 2f;
            bgSrcMid.set(0, 0, (int) bw, (int) bh);
            bgDstMid.set(ox, oy, ox + bw * s, oy + bh * s);
            setRing(ox + bw * LAND_RING_CX * s, oy + bh * LAND_RING_CY * s,
                    bw * LAND_RING_RX * s, bh * LAND_RING_RY * s);
            return;
        }

        if (w > h) {
            float s = w / bw;
            float ringY = bh * BG_RING_CY * s;
            float oy = h * 0.52f - ringY;                        // ring a little below the middle
            oy = Math.min(0f, Math.max(h - bh * s, oy));         // never show past the picture
            bgSrcMid.set(0, 0, (int) bw, (int) bh);
            bgDstMid.set(0, oy, w, oy + bh * s);
            setRing(bw * BG_RING_CX * s, oy + ringY, bw * BG_RING_RX * s, bh * BG_RING_RY * s);
            return;
        }

        float s = w / bw;                     // screen pixels per picture pixel
        float split = bh * BG_SPLIT;
        float foot = bh * BG_FOOT;

        float y2 = h * 0.89f + (foot - bh * BG_RING_BOTTOM) * s;   // where the foot starts
        float y1 = y2 - (foot - split) * s;                         // where the middle starts
        if (y1 < h * 0.25f) {
            y1 = h * 0.25f;
            y2 = y1 + (foot - split) * s;
        }

        bgSrcTop.set(0, 0, (int) bw, (int) split);
        bgDstTop.set(0, 0, w, y1 + 1f);
        bgSrcMid.set(0, (int) split, (int) bw, (int) foot);
        bgDstMid.set(0, y1, w, y2 + 1f);
        bgSrcFoot.set(0, (int) foot, (int) bw, (int) bh);
        bgDstFoot.set(0, y2, w, Math.max(y2, (float) h));

        setRing(bw * BG_RING_CX * s, y1 + (bh * BG_RING_CY - split) * s,
                bw * BG_RING_RX * s, bh * BG_RING_RY * s);
    }

    // ---- Touch ----
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (player1 == null || player2 == null || ui == null) return true;

        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();

        // --- Buttons: the pause button and the pause menu ---
        if (action == MotionEvent.ACTION_DOWN) {
            int hit = GameUI.BTN_NONE;
            if (paused) {
                hit = ui.menuButtonAt(x, y);
            } else if (canPause() && ui.isPauseButton(x, y)) {
                hit = GameUI.BTN_PAUSE;
            } else if (endMenuVisible() && ui.isEndMenuButton(x, y)) {
                hit = GameUI.BTN_END_MENU;
            }
            if (hit != GameUI.BTN_NONE) {
                ui.setPressed(hit);
                return true;
            }
        }

        if (ui.getPressed() != GameUI.BTN_NONE) {
            if (action == MotionEvent.ACTION_UP) {
                int pressedId = ui.getPressed();
                int hit = GameUI.BTN_NONE;
                if (paused) {
                    hit = ui.menuButtonAt(x, y);
                } else if (ui.isPauseButton(x, y)) {
                    hit = GameUI.BTN_PAUSE;
                } else if (endMenuVisible() && ui.isEndMenuButton(x, y)) {
                    hit = GameUI.BTN_END_MENU;
                }
                ui.setPressed(GameUI.BTN_NONE);
                if (hit == pressedId) {
                    performButton(pressedId);
                }
            } else if (action == MotionEvent.ACTION_CANCEL) {
                ui.setPressed(GameUI.BTN_NONE);
            }
            return true;
        }

        // While paused, only the menu buttons react
        if (paused) {
            clearSticks();
            return true;
        }

        // --- Game touches ---

        // After the match: tap to play again
        if (game.getState() == GameManager.State.GAME_OVER) {
            if (action == MotionEvent.ACTION_DOWN && game.canRestart()) {
                game.startGame();
            }
            return true;
        }

        // Only play while the round is running
        if (!game.isPlaying()) {
            clearSticks();
            return true;
        }

        // One finger per player. Tap = charge the opponent, drag = steer (floating stick).
        handleStickTouch(event);
        return true;
    }

    /**
     * Floating stick: wherever a finger lands becomes the stick's center.
     * A tap charges the opponent once; a swipe dashes once in the swipe direction (holding does nothing).
     * 1 player: the whole screen is yours. 2 players: left half = P1, right half = P2.
     */
    private void handleStickTouch(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int id = event.getPointerId(index);
                if (id >= pointerOwner.length) break;
                int owner = twoPlayer ? (event.getX(index) < screenW / 2f ? 1 : 2) : 1;
                if (stickActive[owner]) break;          // this player already has a finger down
                pointerOwner[id] = owner;
                stickActive[owner] = true;
                stickAnchorX[owner] = stickX[owner] = event.getX(index);
                stickAnchorY[owner] = stickY[owner] = event.getY(index);
                dashPending[owner] = true;               // one tap = one dash
                swipeUsed[owner] = false;
                break;
            }
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int id = event.getPointerId(i);
                    if (id >= pointerOwner.length || pointerOwner[id] == 0) continue;
                    int owner = pointerOwner[id];
                    stickX[owner] = event.getX(i);
                    stickY[owner] = event.getY(i);

                    // If the finger goes far, the stick center follows it (quick direction changes)
                    float dx = stickX[owner] - stickAnchorX[owner];
                    float dy = stickY[owner] - stickAnchorY[owner];
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    float maxR = unit * STICK_MAX;
                    if (dist > maxR) {
                        stickAnchorX[owner] += dx / dist * (dist - maxR);
                        stickAnchorY[owner] += dy / dist * (dist - maxR);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int id = event.getPointerId(index);
                if (id < pointerOwner.length && pointerOwner[id] != 0) {
                    stickActive[pointerOwner[id]] = false;
                    pointerOwner[id] = 0;
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                clearSticks();
                break;
            default:
                break;
        }
    }

    /**
     * Turns touches into dashes. Runs every frame while the round is playing.
     * TAP = one dash at the opponent. SWIPE = one dash in the swipe direction.
     * Holding a finger down does nothing more, so you have to tap again to push again.
     */
    private void applySticks() {
        for (int owner = 1; owner <= 2; owner++) {
            Capybara me = owner == 1 ? player1 : player2;
            Capybara other = owner == 1 ? player2 : player1;
            float dead = unit * STICK_DEAD;

            float dx = 0f, dy = 0f;
            boolean dragging = false;
            if (stickActive[owner]) {
                dx = stickX[owner] - stickAnchorX[owner];
                dy = stickY[owner] - stickAnchorY[owner];
                dragging = dx * dx + dy * dy >= dead * dead;
            }

            if (dashPending[owner]) {
                // the tap's dash: toward the opponent (or the swipe direction if already dragging)
                float tx = dragging ? dx : other.x - me.x;
                float ty = dragging ? dy : other.y - me.y;
                if (me.dash(tx, ty)) {
                    dashPending[owner] = false;
                    if (dragging) swipeUsed[owner] = true;
                } else if (!stickActive[owner]) {
                    dashPending[owner] = false;      // finger already lifted during the cooldown: drop it
                }
            } else if (stickActive[owner] && dragging && !swipeUsed[owner]) {
                if (me.dash(dx, dy)) swipeUsed[owner] = true;   // one swipe = one dash
            }
        }
    }

    private void clearSticks() {
        Arrays.fill(pointerOwner, 0);
        Arrays.fill(stickActive, false);
        Arrays.fill(dashPending, false);
        Arrays.fill(swipeUsed, false);
    }

    /** True while the MAIN MENU button is shown on the end-of-match screen. */
    private boolean endMenuVisible() {
        return !paused && game.canRestart();
    }

    private boolean canPause() {
        return game.getState() != GameManager.State.GAME_OVER;
    }

    private void performButton(int id) {
        sound.play(SoundManager.CLICK);
        switch (id) {
            case GameUI.BTN_PAUSE:
                paused = true;
                break;
            case GameUI.BTN_RESUME:
                paused = false;
                break;
            case GameUI.BTN_RESTART:
                paused = false;
                lastState = null;
                lastCountdownText = null;
                game.startGame();
                break;
            case GameUI.BTN_MENU:
            case GameUI.BTN_END_MENU:
                exitListener.onExitToMenu();
                break;
            default:
                break;
        }
    }

    // ---- Game loop: update, then draw, then ask for the next frame ----
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long now = System.nanoTime();
        float dt = (now - lastTimeNs) / 1_000_000_000f;
        lastTimeNs = now;
        if (dt > 0.05f) dt = 0.05f; // avoid huge jumps after a lag

        update(dt);
        if (effects != null) effects.beginShake(canvas, screenW, screenH);
        drawScene(canvas);
        if (effects != null) effects.endShake(canvas);
        drawOverlay(canvas);

        if (running) {
            postInvalidateOnAnimation();
        }
    }

    private void update(float dt) {
        if (player1 == null || player2 == null
                || ui == null || effects == null) return;

        // The UI animates even while paused (button presses)
        ui.update(dt);

        // Frozen while paused
        if (paused) return;

        // Slow motion: time runs slowly, then eases back to normal speed
        effects.updateShake(dt);
        float timeScale = 1f;
        if (slowTimer > 0f) {
            slowTimer = Math.max(0f, slowTimer - dt);
            float k = 1f - slowTimer / SLOW_TIME;
            timeScale = 0.25f + 0.75f * k * k;
        }
        dt *= timeScale;

        game.update(dt);
        fxTime += dt;
        updateRing();
        playPressureCues();
        boolean playing = game.isPlaying();
        if (playing) applySticks();

        // The NPC only acts while the round is running
        if (playing && !twoPlayer) {
            npcController.update(dt, player1, ringRect);
        }

        // Physics keeps running after a round ends, so the loser slides out of the ring
        player1.update(dt);
        player2.update(dt);

        // Push sounds (the NPC is a bit quieter so your own dashes stand out)
        if (player1.consumeDash()) {
            sound.play(SoundManager.PUSH, 1f, 1f);
            effects.dust(player1.x, player1.y + player1.radius * 0.7f, 0.2f);
        }
        if (player2.consumeDash()) {
            sound.play(SoundManager.PUSH, twoPlayer ? 1f : 0.6f, twoPlayer ? 1f : 0.9f);
            effects.dust(player2.x, player2.y + player2.radius * 0.7f, 0.2f);
        }
        playCountdownSound();

        // Always face the opponent
        player1.facing = player2.x >= player1.x ? 1 : -1;
        player2.facing = player1.x >= player2.x ? 1 : -1;

        if (playing) {
            resolveCollision();
            game.checkRound(edgeAmount(player1), edgeAmount(player2));
        }

        // HUD bars: full when well inside the ring, empty on the edge
        ui.setBalance(balance(player1), balance(player2));

        petals.update(dt);
        effects.update(dt);
        reactToStateChange();
    }

    /** Sets the ring size from how little time is left. Everything (rules, NPC, HUD) uses ringRect. */
    private void updateRing() {
        if (baseRingRect.isEmpty()) return;
        float p = game.getShrinkProgress();
        shrink = p * p * (3f - 2f * p);                       // smooth start and end
        float s = 1f - (1f - MIN_RING_SCALE) * shrink;
        scaleAroundCenter(baseRingRect, ringRect, s);
        scaleAroundCenter(baseOuterRect, outerRect, s);
    }

    private void scaleAroundCenter(RectF src, RectF dst, float s) {
        float hw = src.width() / 2f * s;
        float hh = src.height() / 2f * s;
        dst.set(src.centerX() - hw, src.centerY() - hh, src.centerX() + hw, src.centerY() + hh);
    }

    /** A warning when the ring starts closing, and a tick for each of the last 5 seconds. */
    private void playPressureCues() {
        if (!game.isPlaying()) return;
        if (!shrinkAnnounced && game.getTimeLeft() <= GameManager.SHRINK_START) {
            shrinkAnnounced = true;
            sound.play(SoundManager.COUNTDOWN, 1f, 0.7f);
            effects.shake(0.35f);
            buzz(40);
        }
        int sec = game.getTimeSeconds();
        if (sec <= 5 && sec > 0 && sec != lastTickSecond) {
            lastTickSecond = sec;
            sound.play(SoundManager.COUNTDOWN, 0.6f, 1.25f);
        }
    }

    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** Short phone vibration. Safe to call anytime: it does nothing if vibration is unavailable. */
    private void buzz(int ms) {
        if (vibrator == null) return;
        long now = System.currentTimeMillis();
        if (now - lastBuzzMs < 100) return;   // no buzzing machine-gun
        lastBuzzMs = now;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(ms);
            }
        } catch (Exception ignored) {
            // No permission or no vibrator: just skip it
        }
    }

    /** Beeps on 3, 2, 1 and a higher beep on FIGHT! (same file, faster playback). */
    private void playCountdownSound() {
        if (game.getState() != GameManager.State.COUNTDOWN) {
            lastCountdownText = null;
            return;
        }
        String text = game.getCountdownText();
        if (text.equals(lastCountdownText)) return;
        lastCountdownText = text;
        boolean fight = "FIGHT!".equals(text);
        sound.play(SoundManager.COUNTDOWN, 1f, fight ? 1.5f : 1f);
    }

    private float balance(Capybara c) {
        return Math.max(0f, Math.min(1f, (1f - edgeAmount(c)) / 0.6f));
    }

    /** The crowd celebrates when a round or the match ends. */
    private void reactToStateChange() {
        GameManager.State st = game.getState();
        if (st == lastState) return;

        // Someone was knocked out of the ring: big burst, shake, buzz and slow motion
        if (st == GameManager.State.ROUND_END) {
            Capybara loser = edgeAmount(player1) > edgeAmount(player2) ? player1 : player2;
            if (edgeAmount(loser) > 1f) {
                effects.stars(loser.x, loser.y, 1f);
                effects.dust(loser.x, loser.y + loser.radius * 0.6f, 1f);
                effects.shake(0.9f);
                buzz(70);
                slowTimer = SLOW_TIME;
            }
        }

        if (st == GameManager.State.ROUND_END
                && game.getRoundWinner() != GameManager.NONE) {
            sound.play(SoundManager.CHEER);
        } else if (st == GameManager.State.GAME_OVER) {
            int winner = game.getMatchWinner();
            if (winner == GameManager.PLAYER || (twoPlayer && winner == GameManager.NPC)) {
                sound.play(SoundManager.WIN);
            } else {
                if (winner == GameManager.NPC) sound.play(SoundManager.LOSE);
            }
        }
        lastState = st;
    }

    /** Pushes the capybaras apart and gives knockback based on how hard each one hit. */
    private void resolveCollision() {
        float dx = player2.x - player1.x;
        float dy = player2.y - player1.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float minDist = (player1.radius + player2.radius) * 0.85f;
        if (dist >= minDist || dist < 0.0001f) return;

        float nx = dx / dist;   // unit vector from player 1 to player 2
        float ny = dy / dist;

        // Separate so they don't overlap
        float overlap = minDist - dist;
        player1.x -= nx * overlap / 2f;
        player1.y -= ny * overlap / 2f;
        player2.x += nx * overlap / 2f;
        player2.y += ny * overlap / 2f;

        // How fast each one was moving into the other
        float p1Power = Math.max(0f, player1.vx * nx + player1.vy * ny);
        float p2Power = Math.max(0f, -(player2.vx * nx + player2.vy * ny));

        float threshold = player1.radius * 0.4f;
        float baseKnock = player1.radius * 1.5f;

        // If both push at the same time, the stronger one wins the clash instead of both bouncing off
        boolean clash = p1Power > threshold && p2Power > threshold;
        if (p1Power > threshold) {
            float k = clash ? Math.max(0f, p1Power - CLASH_RESIST * p2Power) : p1Power;
            player2.knock(nx, ny, k + baseKnock);
        }
        if (p2Power > threshold) {
            float k = clash ? Math.max(0f, p2Power - CLASH_RESIST * p1Power) : p2Power;
            player1.knock(-nx, -ny, k + baseKnock);
        }

        // The crowd cheers in proportion to how hard the hit was
        float hit = Math.max(p1Power, p2Power);
        if (hit > threshold) {
            float strength = Math.min(1f, hit / (player1.radius * 6f));
            sound.play(SoundManager.HIT, 0.5f + 0.5f * strength, 1f);

            // Visual and touch feedback at the point where they collided
            float hx = (player1.x + player2.x) / 2f;
            float hy = (player1.y + player2.y) / 2f;
            effects.dust(hx, hy + player1.radius * 0.5f, strength);
            if (strength > 0.3f) effects.stars(hx, hy, strength);
            effects.shake(strength * 0.55f);
            if (strength > 0.3f) buzz((int) (12 + 28 * strength));
        }

        // Attackers lose most of their forward speed (recoil)
        player1.vx -= nx * p1Power * RECOIL;
        player1.vy -= ny * p1Power * RECOIL;
        player2.vx += nx * p2Power * RECOIL;
        player2.vy += ny * p2Power * RECOIL;
    }

    /** 0 = center of the ring, 1 = exactly on the edge, above 1 = outside. */
    private float edgeAmount(Capybara c) {
        float nx = (c.x - ringRect.centerX()) / (ringRect.width() / 2f);
        float ny = (c.y - ringRect.centerY()) / (ringRect.height() / 2f);
        return (float) Math.sqrt(nx * nx + ny * ny);
    }

    private void resetPositions() {
        player1.resetTo(p1StartX, startY, 1);
        player2.resetTo(p2StartX, startY, -1);
        npcController.reset();
    }

    /** Simple drawn arena, used only if res/drawable/arena_bg is missing. */
    private void drawPlainArena(Canvas canvas) {
        // Sky
        paint.setShader(skyShader);
        canvas.drawRect(0, 0, screenW, screenH * (landscape ? 0.22f : 0.45f), paint);
        paint.setShader(null);

        // Grass
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF7BC96F);
        canvas.drawRect(0, screenH * (landscape ? 0.16f : 0.30f), screenW, screenH, paint);

        // Ring: the full-size ring first. Once it starts closing, the part that is
        // "out of bounds" turns a warning pink so the player can see the danger zone.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFC9A66B);
        canvas.drawOval(baseOuterRect, paint);
        paint.setColor(lerpColor(0xFFEBD7A8, 0xFFE9B49C, shrink));
        canvas.drawOval(baseRingRect, paint);

        // The live ring: its rope blinks red while it is closing
        if (shrink > 0.001f) {
            float blink = 0.5f + 0.5f * (float) Math.sin(fxTime * 9f);
            paint.setColor(lerpColor(0xFFC9A66B, 0xFFE8433A, Math.min(1f, shrink * 3f) * (0.4f + 0.6f * blink)));
            canvas.drawOval(outerRect, paint);
        }
        paint.setColor(0xFFEBD7A8);
        canvas.drawOval(ringRect, paint);

        // Center line
        paint.setColor(0xFFC9A66B);
        paint.setStrokeWidth(unit * 0.008f);
        canvas.drawLine(ringRect.centerX(), ringRect.centerY() - unit * 0.03f,
                ringRect.centerX(), ringRect.centerY() + unit * 0.03f, paint);
    }

    /** The arena picture, plus a red warning zone while the ring is closing in. */
    private void drawPictureArena(Canvas canvas) {
        if (!bgDstTop.isEmpty()) canvas.drawBitmap(background, bgSrcTop, bgDstTop, bgPaint);
        canvas.drawBitmap(background, bgSrcMid, bgDstMid, bgPaint);
        if (bgDstFoot.height() > 1f) {
            canvas.drawBitmap(background, bgSrcFoot, bgDstFoot, bgPaint);
        }

        if (shrink > 0.001f) {
            // The part of the ring that is now out of bounds turns red
            dangerPath.reset();
            dangerPath.setFillType(Path.FillType.EVEN_ODD);
            dangerPath.addOval(baseRingRect, Path.Direction.CW);
            dangerPath.addOval(ringRect, Path.Direction.CW);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(((int) (120 * Math.min(1f, shrink * 2f)) << 24) | 0xE8433A);
            canvas.drawPath(dangerPath, paint);

            // The new edge blinks
            float blink = 0.5f + 0.5f * (float) Math.sin(fxTime * 9f);
            float a = Math.min(1f, shrink * 3f) * (0.4f + 0.6f * blink);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(unit * 0.01f);
            paint.setColor(((int) (255 * a) << 24) | 0xE8433A);
            canvas.drawOval(ringRect, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawScene(Canvas canvas) {
        if (background != null) {
            drawPictureArena(canvas);
        } else {
            drawPlainArena(canvas);
        }

        // Wrestlers (the one lower on screen is drawn last so it overlaps correctly)
        if (player1 != null && player2 != null) {
            if (player1.y <= player2.y) {
                player1.draw(canvas);
                player2.draw(canvas);
            } else {
                player2.draw(canvas);
                player1.draw(canvas);
            }
        }

        // Dust puffs and impact stars
        if (effects != null) {
            effects.draw(canvas);
        }

        // Falling sakura petals
        if (petals != null) {
            petals.draw(canvas);
        }
    }

    // ---- Big center messages, the HUD, and the pause menu ----

    private void drawOverlay(Canvas canvas) {
        if (ui == null) return;

        // Two players: a dotted line shows where the screen is split
        if (twoPlayer && (game.getState() == GameManager.State.COUNTDOWN || game.isPlaying())) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x55FFFFFF);
            for (float dy = screenH * 0.40f; dy < screenH * 0.88f; dy += unit * 0.045f) {
                canvas.drawCircle(screenW / 2f, dy, unit * 0.004f, paint);
            }
        }

        // The floating sticks, so players can see where their finger "center" is
        if (game.isPlaying()) {
            float r = unit * STICK_MAX;
            for (int owner = 1; owner <= 2; owner++) {
                if (!stickActive[owner]) continue;
                int rgb = owner == 1 ? 0x5B9DFF : 0xFF6F66;
                float dx = stickX[owner] - stickAnchorX[owner];
                float dy = stickY[owner] - stickAnchorY[owner];
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d > r) { dx = dx / d * r; dy = dy / d * r; }

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0x33000000 | rgb);
                canvas.drawCircle(stickAnchorX[owner], stickAnchorY[owner], r, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(unit * 0.006f);
                paint.setColor(0x99000000 | rgb);
                canvas.drawCircle(stickAnchorX[owner], stickAnchorY[owner], r, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xAA000000 | rgb);
                canvas.drawCircle(stickAnchorX[owner] + dx, stickAnchorY[owner] + dy, r * 0.4f, paint);
            }
        }

        switch (game.getState()) {
            case COUNTDOWN:
                drawText(canvas, game.getCountdownText(), screenW / 2f, screenH * 0.38f,
                        unit * 0.20f, 0xFFFFFFFF);
                if (game.isMatchPoint()) {
                    float pulse = 1f + 0.06f * (float) Math.sin(fxTime * 10f);
                    drawText(canvas, "MATCH POINT!", screenW / 2f, screenH * 0.47f,
                            unit * 0.08f * pulse, 0xFFFFD84D);
                }
                if (twoPlayer) {
                    drawText(canvas, "P1 TOUCH HERE", screenW * 0.25f, screenH * 0.78f,
                            unit * 0.05f, 0xFF9CC8FF);
                    drawText(canvas, "P2 TOUCH HERE", screenW * 0.75f, screenH * 0.78f,
                            unit * 0.05f, 0xFFFF9C9C);
                    drawText(canvas, "Tap = charge  -  Drag = steer", screenW / 2f, screenH * 0.84f,
                            unit * 0.04f, 0xFFFFE9C0);
                } else {
                    drawText(canvas, "Tap = charge  -  Drag = steer", screenW / 2f, screenH * 0.80f,
                            unit * 0.045f, 0xFFFFE9C0);
                }
                break;

            case ROUND_END:
                drawText(canvas, roundResultText(), screenW / 2f, screenH * 0.38f,
                        unit * 0.13f, roundResultColor());
                break;

            case GAME_OVER:
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0x88000000);
                canvas.drawRect(0, 0, screenW, screenH, paint);

                int mw = game.getMatchWinner();
                String title = mw == GameManager.PLAYER ? (twoPlayer ? "P1 WINS!" : "VICTORY!")
                        : mw == GameManager.NPC ? (twoPlayer ? "P2 WINS!" : "DEFEAT") : "DRAW";
                int color = mw == GameManager.PLAYER ? (twoPlayer ? 0xFF9CC8FF : 0xFFFFE07A)
                        : mw == GameManager.NPC ? 0xFFFF9C9C : 0xFFFFFFFF;

                drawText(canvas, title, screenW / 2f, screenH * 0.38f, unit * 0.16f, color);
                drawText(canvas, game.getPlayerWins() + " - " + game.getNpcWins(),
                        screenW / 2f, screenH * 0.50f, unit * 0.10f, 0xFFFFFFFF);
                if (game.isComeback()) {
                    drawText(canvas, "WHAT A COMEBACK!", screenW / 2f, screenH * 0.57f,
                            unit * 0.07f, 0xFFFFD84D);
                }
                if (game.canRestart()) {
                    drawText(canvas, "Tap to play again", screenW / 2f, screenH * 0.62f,
                            unit * 0.06f, 0xFFFFFFFF);
                    drawText(canvas, "Back button: main menu", screenW / 2f, screenH * 0.68f,
                            unit * 0.04f, 0xFFFFE9C0);
                    ui.drawEndMenuButton(canvas);
                }
                break;

            default:
                break;
        }

        // "Ring shrinking" announcement for the first moments of the squeeze
        if (game.isPlaying()) {
            float left = game.getTimeLeft();
            if (left <= GameManager.SHRINK_START && left > GameManager.SHRINK_START - 2.5f) {
                float pulse = 1f + 0.07f * (float) Math.sin(fxTime * 12f);
                drawText(canvas, "RING SHRINKING!", screenW / 2f, screenH * 0.21f,
                        unit * 0.075f * pulse, 0xFFFF8A80);
            }
        }

        // HUD on top of the scene
        ui.drawHud(canvas, game.getRoundLabel(), game.getTimeSeconds(),
                game.getPlayerWins(), game.getNpcWins(), GameManager.WINS_NEEDED,
                canPause());

        // Pause menu on top of everything
        if (paused) {
            ui.drawPauseMenu(canvas);
        }
    }

    private String roundResultText() {
        switch (game.getRoundWinner()) {
            case GameManager.PLAYER: return twoPlayer ? "P1 WINS!" : "YOU WIN!";
            case GameManager.NPC: return "P2 WINS!";
            default: return "DRAW";
        }
    }

    private int roundResultColor() {
        switch (game.getRoundWinner()) {
            case GameManager.PLAYER: return 0xFF9CC8FF;
            case GameManager.NPC: return 0xFFFF9C9C;
            default: return 0xFFFFFFFF;
        }
    }

    /** Draws centered text with a dark outline so it is readable on any background. */
    private void drawText(Canvas canvas, String text, float x, float y, float size, int color) {
        textPaint.setTextSize(size);

        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStrokeWidth(size * 0.14f);
        textPaint.setColor(0xFF5A3A1E);
        canvas.drawText(text, x, y, textPaint);

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(color);
        canvas.drawText(text, x, y, textPaint);
    }
}