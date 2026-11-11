package com.example.pawsomeminigames.crittertoss;

import com.example.pawsomeminigames.R;
import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.*;

public class GameView extends View {

    // Portrait logical canvas: fixed width, height follows the screen's aspect ratio
    static final float W = 640f;
    static final float TOP_MIN = 48f;      // smallest HUD top margin; grows with the camera cutout / status bar
    // The game is drawn on a 640-wide logical canvas whose height follows the screen, but is kept inside
    // this range. Wider/shorter windows (tablets, foldables, landscape) get side bars; taller ones get top/bottom bars.
    static final float MIN_H = 860f;
    static final float MAX_H = 1500f;
    static final int MAX_TARGETS = 4;
    static final Random RNG = new Random();

    private float H = 960f;
    private final float[] lanes = {317f, 467f, 618f, 768f};

    enum Type {
        //     image                      bullseye x,y   width speed hitR  weight points  seconds
        FOX(       R.drawable.fox,        .60f, .74f,    160f, 1.15f, .19f,  20,    10,     0),
        RACCOON(   R.drawable.raccoon,    .70f, .78f,    165f, 1.00f, .19f,  20,    10,     0),
        SLOTH(     R.drawable.sloth,      .54f, .77f,    155f, 0.55f, .19f,  20,     5,     0),
        CAPYBARA(  R.drawable.capybara,   .51f, .73f,    155f, 0.80f, .19f,  20,    10,     0),
        GOLDEN(    R.drawable.golden_fox, .709f, .659f,  170f, 2.30f, .085f,  6,    50,     0),   // fast + tiny target
        KING(      R.drawable.king_raccoon, .694f, .751f, 165f, 1.00f, .19f,  8,    25,     0),
        SAD_CAPY(  R.drawable.sad_capy,   .510f, .710f,  160f, 0.80f, .19f,  6,   -10,     0),   // penalty: -10 points
        SAD_RACCOON(R.drawable.sad_raccoon, .673f, .703f, 165f, 1.00f, .19f,  6,   -10,     0),   // penalty: -10 points
        SAD_SLOTH( R.drawable.sad_sloth,  .525f, .708f,  155f, 0.55f, .19f,  6,    -5,     0),   // penalty: -5 points
        SAD_FOX(   R.drawable.sad_fox,    .748f, .697f,  165f, 1.20f, .19f,  6,     0,   -10),   // penalty: -10 seconds
        CLOCK_FOX( R.drawable.clock_fox,  .776f, .646f,  165f, 1.20f, .19f,  8,     0,    15);   // bonus time

        final int res, weight, points, seconds;
        final float bx, by, w, speedMul, hitR;

        Type(int res, float bx, float by, float w, float speedMul, float hitR, int weight, int points, int seconds) {
            this.res = res; this.bx = bx; this.by = by; this.w = w; this.speedMul = speedMul;
            this.hitR = hitR; this.weight = weight; this.points = points; this.seconds = seconds;
        }
    }

    enum State { MENU, COUNTDOWN, PLAY, PAUSE, OVER }

    static class Target {
        final Type type; float x; float lane; final int dir; final float speed;
        float t = RNG.nextFloat() * 6f; boolean hit = false; float ht = 0f;
        Target(Type type, float x, float lane, int dir, float speed) {
            this.type = type; this.x = x; this.lane = lane; this.dir = dir; this.speed = speed;
        }
    }

    static class Ball {
        final float sx, sy, tx, ty; float t = 0f;
        Ball(float sx, float sy, float tx, float ty) { this.sx = sx; this.sy = sy; this.tx = tx; this.ty = ty; }
    }

    static class Particle {
        float x, y, vx, vy, life; final int color; final float size;
        Particle(float x, float y, float vx, float vy, float life, int color, float size) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = life; this.color = color; this.size = size;
        }
    }

    static class FloatText {
        final float x, y; float t = 0f; final String label; final int fill, stroke;
        FloatText(float x, float y, String label, int fill, int stroke) {
            this.x = x; this.y = y; this.label = label; this.fill = fill; this.stroke = stroke;
        }
    }

    /** Soft round effect: smoke, ball trail, ambient sparkles. */
    static class Puff {
        float x, y, vx, vy, life; final float max, size, grow, a0; final int color; final boolean twinkle;
        Puff(float x, float y, float vx, float vy, float life, float size, float grow, int color, float a0, boolean twinkle) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = life; this.max = life;
            this.size = size; this.grow = grow; this.color = color; this.a0 = a0; this.twinkle = twinkle;
        }
    }

    /** Expanding shockwave ring. */
    static class Ring {
        final float x, y, max; final int color; float t = 0f;
        Ring(float x, float y, float max, int color) { this.x = x; this.y = y; this.max = max; this.color = color; }
    }

    /** Power-ups that float across the screen in glowing orbs. Shoot an orb to collect it. */
    enum Power {
        //     color RGB          label           seconds weight
        FREEZE(110, 200, 255, "FREEZE!",       4.5f, 3),   // animals stop moving
        BIG(   255, 170,  40, "BIG BLAST!",    8f,   3),   // much bigger hit area
        TRIPLE(170, 120, 255, "TRIPLE SHOT!",  8f,   3),   // 3 balls per tap, misses are free
        BOMB(  235,  70,  60, "BOMB!",         0f,   2);   // instantly pops everything (safe from sad critters)

        final int r, g, b, color, weight;
        final String label;
        final float dur;

        Power(int r, int g, int b, String label, float dur, int weight) {
            this.r = r; this.g = g; this.b = b; this.color = Color.rgb(r, g, b);
            this.label = label; this.dur = dur; this.weight = weight;
        }
        int light() { return Color.rgb(Math.min(255, r + 90), Math.min(255, g + 90), Math.min(255, b + 90)); }
        int dark()  { return Color.rgb(r / 2, g / 2, b / 2); }
    }

    static class Orb {
        final Power kind; float x; float lane; final int dir; final float speed; float t = 0f;
        Orb(Power kind, float x, float lane, int dir, float speed) {
            this.kind = kind; this.x = x; this.lane = lane; this.dir = dir; this.speed = speed;
        }
    }

    private final Map<Type, Bitmap> sprites = new EnumMap<>(Type.class);
    private final Bitmap bgBmp;
    private final Rect bgSrc = new Rect();

    private final List<Target> targets = new ArrayList<>();
    private final List<Ball> balls = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatText> texts = new ArrayList<>();
    private final List<Float> spawnTimers = new ArrayList<>();

    private State state = State.MENU;
    private int score = 0, best = 0;
    static final float ROUND_TIME = 35f;
    private float time = ROUND_TIME;
    private Runnable onExit, onPauseRequest;
    private State pausedFrom = State.PLAY;
    private long lastNs = 0L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF pauseRect = new RectF(W - 74f, TOP_MIN, W - 14f, TOP_MIN + 64f);
    private final RectF dst = new RectF();
    private final RectF btn1 = new RectF(), btn2 = new RectF();
    private final RectF tmp = new RectF();

    // cannon + effects state
    static final float MUZZLE = 84f;
    static final float FIRE_COOLDOWN = .32f;           // seconds between shots (raise it to slow the cannon down more)
    private float fireCd = 0f;
    private float timeGood = 0f, timeBad = 0f, scoreBad = 0f;
    private int diff = GameData.NORMAL, maxTargets = 4;
    private float diffSpeed = 1f, diffHit = 1f;
    private boolean newBest = false;
    private float aim = 0f, recoil = 0f, flash = 0f, shake = 0f, pop = 0f, ambient = 0f;
    private final List<Puff> puffs = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();

    // combo + power-up state
    static final float ORB_R = 34f, ORB_HIT = 54f;
    static final float FREEZE_FADE = 1f;
    private static final int[] COMBO_COL = {
            Color.rgb(255, 244, 170), Color.rgb(255, 214, 64), Color.rgb(255, 150, 60), Color.rgb(255, 96, 150)};
    // "3, 2, 1, GO!" before each round
    static final float COUNT_SECONDS = 3f;                 // seconds of countdown (change to 5f for a longer one)
    private float countT = 0f, goT = 0f;
    private int lastTick = 0;

    private int combo = 0, bestCombo = 0;
    private float comboPop = 0f, comboLostT = 0f;
    private final List<Orb> orbs = new ArrayList<>();
    private float orbTimer = 8f;
    private float freezeT = 0f, bigT = 0f, tripleT = 0f, bombFlash = 0f, frostSpark = 0f;
    private final ColorFilter frostFilter = new PorterDuffColorFilter(Color.argb(120, 120, 200, 255), PorterDuff.Mode.SRC_ATOP);
    private final RectF hr = new RectF();

    private float scale = 1f, offX = 0f, offY = 0f, top = TOP_MIN;
    private int insetTopPx = 0;
    private final Rect bgFullSrc = new Rect();
    private final RectF fullDst = new RectF();

    // sprites + background are decoded once and shared, so starting a round does not freeze the screen
    private static final java.util.Map<Type, Bitmap> SPRITE_CACHE = new java.util.EnumMap<>(Type.class);
    private static Bitmap BG_CACHE;

    static synchronized void ensureAssets(android.content.res.Resources res) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        for (Type t : Type.values()) {
            if (SPRITE_CACHE.containsKey(t)) continue;
            Bitmap src = BitmapFactory.decodeResource(res, t.res, opts);
            // sprites are ~1200px; shrink them so they draw faster and use less memory
            int tw = 420;
            int th = Math.round(tw * (float) src.getHeight() / src.getWidth());
            Bitmap small = Bitmap.createScaledBitmap(src, tw, th, true);
            if (small != src) src.recycle();
            SPRITE_CACHE.put(t, small);
        }
        if (BG_CACHE == null) BG_CACHE = BitmapFactory.decodeResource(res, R.drawable.bg_shrine, opts);
    }

    public GameView(Context context) {
        super(context);
        ensureAssets(getResources());   // instant when the splash screen already did this in the background
        sprites.putAll(SPRITE_CACHE);
        bgBmp = BG_CACHE;
        txt.setTypeface(Typeface.DEFAULT_BOLD);
        txt.setTextAlign(Paint.Align.CENTER);

        // keep the HUD clear of camera cutouts / status bar on every device
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            insetTopPx = bars.top;
            updateTop();
            return insets;
        });
    }

    private void updateTop() {
        top = Math.max(TOP_MIN, (insetTopPx - offY) / scale + 12f);
        pauseRect.set(W - 74f, top, W - 14f, top + 64f);
    }

    private float remapLane(float lane, float[] old) {
        int best = 0;
        for (int i = 1; i < old.length; i++) if (Math.abs(old[i] - lane) < Math.abs(old[best] - lane)) best = i;
        return lanes[best];
    }

    /** Center-crops the background bitmap to the given width/height ratio. */
    private void crop(Rect out, float aspect) {
        float bw = bgBmp.getWidth(), bh = bgBmp.getHeight();
        if (bw / bh > aspect) {
            float sw = bh * aspect;
            out.set((int) ((bw - sw) / 2f), 0, (int) ((bw + sw) / 2f), (int) bh);
        } else {
            float sh = bw / aspect;
            out.set(0, (int) ((bh - sh) / 2f), (int) bw, (int) ((bh + sh) / 2f));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w <= 0 || h <= 0) return;
        float[] oldLanes = lanes.clone();
        scale = w / W;
        H = h / scale;
        offX = 0f; offY = 0f;
        if (H < MIN_H) {                 // wide / short window: fit the height, centre the 640-wide play field
            scale = h / MIN_H;
            H = MIN_H;
            offX = (w - W * scale) / 2f;
        } else if (H > MAX_H) {          // extra-tall window: centre vertically
            H = MAX_H;
            offY = (h - H * scale) / 2f;
        }

        // lanes spread over the middle/lower part of the play field
        for (int i = 0; i < lanes.length; i++) {
            lanes[i] = H * (.33f + .47f * i / (lanes.length - 1));
        }
        // window was resized mid-round (rotate, fold/unfold, split-screen): move critters to the new lanes
        for (Target t : targets) t.lane = remapLane(t.lane, oldLanes);
        for (Orb o : orbs) o.lane = remapLane(o.lane, oldLanes);

        crop(bgSrc, W / H);                         // background inside the play field
        crop(bgFullSrc, w / (float) h);             // background behind the side / top bars
        fullDst.set(0f, 0f, w, h);
        updateTop();
        if (state == State.MENU) startGame();
    }

    /** Called when the player taps "Main menu". */
    public void setOnExit(Runnable r) { onExit = r; }

    /** Called when the player taps the pause button; the activity shows the pause menu dialog. */
    public void setOnPauseRequested(Runnable r) { onPauseRequest = r; }

    private void sfx(int id) { SoundManager.get(getContext()).play(id); }
    private void sfxPitch(int id, float rate) { SoundManager.get(getContext()).playPitched(id, rate); }

    // ---------- game flow ----------
    private void startGame() {
        diff = GameData.getDifficulty(getContext());
        switch (diff) {
            case GameData.EASY:   diffSpeed = .75f; diffHit = 1.3f;  maxTargets = 4; break;
            case GameData.HARD:   diffSpeed = 1.35f; diffHit = .85f; maxTargets = 5; break;
            default:              diffSpeed = 1f;   diffHit = 1f;    maxTargets = 4; break;
        }
        best = GameData.best(getContext(), diff);
        score = 0;
        time = ROUND_TIME;
        targets.clear(); balls.clear(); particles.clear(); texts.clear(); spawnTimers.clear();
        rings.clear(); aim = 0f; timeGood = 0f; timeBad = 0f; scoreBad = 0f; newBest = false;
        fireCd = 0f;
        orbs.clear(); combo = 0; bestCombo = 0; comboPop = 0f; comboLostT = 0f;
        freezeT = 0f; bigT = 0f; tripleT = 0f; bombFlash = 0f;
        orbTimer = 6f + RNG.nextFloat() * 3f;
        for (int i = 0; i < maxTargets; i++) {
            spawn();
            targets.get(targets.size() - 1).x = RNG.nextFloat() * (W - 160f) + 80f;
        }
        state = State.COUNTDOWN;
        countT = COUNT_SECONDS; lastTick = 0; goT = 0f;
        lastNs = System.nanoTime();
    }

    /** Runs the pre-round countdown. The round clock and targets stay frozen until it reaches 0. */
    private void updateCountdown(float dt) {
        for (Target o : targets) o.t += dt;              // critters bob in place while we wait
        countT -= dt;
        if (countT <= 0f) {
            state = State.PLAY;
            goT = .7f;
            sfx(SoundManager.COMBO);
            rings.add(new Ring(W / 2f, H / 2f, 280f, Color.rgb(110, 235, 160)));
            rings.add(new Ring(W / 2f, H / 2f, 160f, Color.WHITE));
            burstMulti(W / 2f, H / 2f, 34);
            return;
        }
        int n = (int) Math.ceil(countT);                 // 3, 2, 1
        if (n != lastTick) {
            lastTick = n;
            sfxPitch(SoundManager.CLICK, .7f + (COUNT_SECONDS - n) * .18f);   // each tick a bit higher
        }
    }

    public void pauseGame() {
        if (state == State.PLAY || state == State.COUNTDOWN) { pausedFrom = state; state = State.PAUSE; }
    }

    public void resumeGame() {
        if (state == State.PAUSE) { state = pausedFrom; lastNs = System.nanoTime(); }
    }

    public void restartGame() { startGame(); }

    public boolean isPaused() { return state == State.PAUSE; }

    /** True while a round is running (including the 3-2-1 countdown). */
    public boolean isGameplay() { return state == State.PLAY || state == State.COUNTDOWN; }

    private boolean inPauseButton(float lx, float ly) {
        // slightly generous hit-area so it's easy to tap
        return lx > pauseRect.left - 12f && lx < pauseRect.right + 12f
                && ly > pauseRect.top - 12f && ly < pauseRect.bottom + 12f;
    }

    private void requestPause() {
        sfx(SoundManager.CLICK);
        pauseGame();
        if (onPauseRequest != null) onPauseRequest.run();
    }

    private void spawn() {
        Type type = pickType();
        int dir = RNG.nextBoolean() ? 1 : -1;
        float speed = (50f + RNG.nextFloat() * 85f) * type.speedMul * diffSpeed;
        float lane = lanes[RNG.nextInt(lanes.length)];
        targets.add(new Target(type, dir > 0 ? -90f : W + 90f, lane, dir, speed));
    }

    /** Spawn weight of a critter in the current difficulty. Clock foxes get rarer and sad foxes
     *  more common as the mode gets harder, so the time bonus can't outweigh the time penalty. */
    private float weightOf(Type t) {
        if (t == Type.CLOCK_FOX) return t.weight * (diff == GameData.HARD ? .35f : diff == GameData.NORMAL ? .5f : 1f);
        if (t == Type.SAD_FOX)   return t.weight * (diff == GameData.HARD ? 1.3f : 1f);
        return t.weight;
    }

    private Type pickType() {
        boolean goldenOut = false;
        for (Target o : targets) if (o.type == Type.GOLDEN && !o.hit) goldenOut = true;
        float total = 0f;
        for (Type t : Type.values()) if (!(t == Type.GOLDEN && goldenOut)) total += weightOf(t);
        float r = RNG.nextFloat() * total;
        for (Type t : Type.values()) {
            if (t == Type.GOLDEN && goldenOut) continue;
            r -= weightOf(t);
            if (r < 0f) return t;
        }
        return Type.FOX;
    }

    private float spriteH(Type t) {
        Bitmap b = sprites.get(t);
        return t.w * b.getHeight() / b.getWidth();
    }

    /** returns {centerX, centerY, radius} */
    private float[] bullseye(Target o) {
        Type t = o.type;
        return new float[]{
                o.x + (t.bx - .5f) * t.w,
                o.lane + 8f - spriteH(t) * (1f - t.by),
                t.w * t.hitR * diffHit * (bigT > 0f ? 1.6f : 1f)};
    }

    private float bx(Ball b) { return b.sx + (b.tx - b.sx) * b.t; }
    private float by(Ball b) { return b.sy + (b.ty - b.sy) * b.t - (float) Math.sin(b.t * 3.14f) * 60f; }

    private void fire(float lx, float ly) {
        fireCd = FIRE_COOLDOWN;
        float px = W / 2f, py = H - 60f;
        aim = Math.max(-1.25f, Math.min(1.25f, (float) Math.atan2(lx - px, py - ly)));
        float dx = (float) Math.sin(aim), dy = -(float) Math.cos(aim);
        float mx = px + dx * MUZZLE, my = py + dy * MUZZLE;
        balls.add(new Ball(mx, my, lx, ly));
        if (tripleT > 0f) {
            balls.add(new Ball(mx, my, Math.max(20f, lx - 72f), ly));
            balls.add(new Ball(mx, my, Math.min(W - 20f, lx + 72f), ly));
        }
        recoil = 1f; flash = .12f;
        for (int i = 0; i < 7; i++) {
            float sp = 50f + RNG.nextFloat() * 130f;
            puffs.add(new Puff(mx, my, dx * sp + (RNG.nextFloat() - .5f) * 90f, dy * sp + (RNG.nextFloat() - .5f) * 90f,
                    .45f + RNG.nextFloat() * .3f, 9f + RNG.nextFloat() * 7f, 45f, Color.rgb(246, 244, 255), .85f, false));
        }
        sfx(SoundManager.THROW);
    }

    private static final int[] CONFETTI = {
            Color.rgb(255, 214, 64), Color.rgb(255, 96, 140), Color.rgb(110, 220, 255),
            Color.rgb(160, 255, 140), Color.WHITE, Color.rgb(255, 150, 60)};

    private void burstMulti(float x, float y, int n) {
        for (int i = 0; i < n; i++) {
            double a = RNG.nextFloat() * 6.28;
            float sp = 90f + RNG.nextFloat() * 230f;
            particles.add(new Particle(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp - 80f,
                    .6f + RNG.nextFloat() * .4f, CONFETTI[RNG.nextInt(CONFETTI.length)], 4f + RNG.nextFloat() * 6f));
        }
    }

    private void burst(float x, float y, int n, int color) {
        for (int i = 0; i < n; i++) {
            double a = RNG.nextFloat() * 6.28;
            float s = 80f + RNG.nextFloat() * 180f;
            particles.add(new Particle(x, y, (float) Math.cos(a) * s, (float) Math.sin(a) * s - 60f,
                    .7f, color, 4f + RNG.nextFloat() * 5f));
        }
    }

    // ---------- combo ----------
    private int mult() { return Math.min(4, 1 + combo / 4); }

    /** Counts a successful hit; returns the multiplier to use for its points. */
    private int registerHit() {
        int before = mult();
        combo++;
        bestCombo = Math.max(bestCombo, combo);
        comboPop = .3f;
        int now = mult();
        if (now > before) {
            texts.add(new FloatText(W / 2f, top + 190f, "COMBO x" + now + "!",
                    COMBO_COL[now - 1], Color.rgb(214, 51, 108)));
            rings.add(new Ring(W / 2f, H - 60f, 160f, COMBO_COL[now - 1]));
            sfx(SoundManager.COMBO);
            if (now == 3 && orbs.isEmpty()) spawnOrb();      // reward: a bonus orb at x3
        }
        return now;
    }

    private void breakCombo() {
        if (combo >= 3) comboLostT = .8f;
        combo = 0;
    }

    // ---------- power-ups ----------
    private float orbY(Orb o) { return o.lane - 70f + (float) Math.sin(o.t * 4f) * 8f; }

    private void spawnOrb() {
        int total = 0;
        for (Power p : Power.values()) total += p.weight;
        int r = RNG.nextInt(total);
        Power kind = Power.FREEZE;
        for (Power p : Power.values()) { r -= p.weight; if (r < 0) { kind = p; break; } }
        int dir = RNG.nextBoolean() ? 1 : -1;
        orbs.add(new Orb(kind, dir > 0 ? -50f : W + 50f, lanes[RNG.nextInt(lanes.length)], dir,
                85f + RNG.nextFloat() * 40f));
        orbTimer = 10f + RNG.nextFloat() * 6f;
    }

    private void collect(Orb o) {
        float x = o.x, y = orbY(o);
        Power p = o.kind;
        rings.add(new Ring(x, y, 115f, p.color));
        rings.add(new Ring(x, y, 60f, Color.WHITE));
        burst(x, y, 24, p.light());
        texts.add(new FloatText(x, y - 30f, p.label, p.light(), p.dark()));
        sfx(SoundManager.POWER);
        shake = Math.max(shake, .15f);
        switch (p) {
            case FREEZE: freezeT = p.dur; break;
            case BIG:    bigT = p.dur; break;
            case TRIPLE: tripleT = p.dur; break;
            case BOMB:   detonate(); break;
        }
        orbTimer = 9f + RNG.nextFloat() * 6f;
    }

    private void detonate() {
        float cx = W / 2f, cy = H - 60f;
        bombFlash = .4f;
        shake = .6f;
        rings.add(new Ring(cx, cy, 260f, Color.rgb(255, 120, 60)));
        rings.add(new Ring(cx, cy, 440f, Color.rgb(255, 210, 90)));
        rings.add(new Ring(cx, cy, 720f, Color.WHITE));
        burstMulti(cx, cy - 40f, 60);
        sfx(SoundManager.BOOM);
        for (Target o : new ArrayList<>(targets)) if (!o.hit) applyHit(o, true);
    }

    // ---------- landing ----------
    private void land(Ball b) {
        // power-up orbs first
        for (Orb o : orbs) {
            if (Math.hypot(o.x - b.tx, orbY(o) - b.ty) < ORB_HIT) {
                orbs.remove(o);
                collect(o);
                return;
            }
        }

        Target hitT = null;
        float bestD = Float.MAX_VALUE;
        for (Target o : targets) {
            if (o.hit) continue;
            float[] c = bullseye(o);
            float d = (float) Math.hypot(c[0] - b.tx, c[1] - b.ty);
            if (d < c[2] * 1.15f && d < bestD) { bestD = d; hitT = o; }
        }
        if (hitT != null) {
            applyHit(hitT, false);
        } else {
            burst(b.tx, b.ty, 8, Color.rgb(74, 163, 255));
            rings.add(new Ring(b.tx, b.ty, 40f, Color.rgb(120, 190, 255)));
            for (int k = 0; k < 4; k++)
                puffs.add(new Puff(b.tx, b.ty, (RNG.nextFloat() - .5f) * 90f, -20f - RNG.nextFloat() * 40f, .5f, 8f, 30f, Color.rgb(226, 214, 190), .7f, false));
            sfx(SoundManager.MISS);
            if (tripleT <= 0f) breakCombo();        // during Triple Shot, misses are free
        }
    }

    /** safe = true when a bomb pops the critter (sad critters just vanish, no penalty). */
    private void applyHit(Target hitT, boolean safe) {
        hitT.hit = true; hitT.ht = 0f;
        Type ty = hitT.type;
        float[] c = bullseye(hitT);
        boolean bad = ty.points < 0 || ty.seconds < 0;

        if (bad && safe) {
            for (int k = 0; k < 6; k++)
                puffs.add(new Puff(c[0], c[1], (RNG.nextFloat() - .5f) * 120f, -30f - RNG.nextFloat() * 60f, .6f, 10f, 30f, Color.rgb(226, 214, 190), .8f, false));
            spawnTimers.add(.45f);
            return;
        }
        if (bad) breakCombo();

        if (ty.points < 0) {
            // penalty animal: lose points (never below 0)
            score = Math.max(0, score + ty.points);
            scoreBad = .9f;
            burst(c[0], c[1], 26, Color.rgb(90, 160, 255));
            rings.add(new Ring(c[0], c[1], 90f, Color.rgb(255, 90, 90)));
            rings.add(new Ring(c[0], c[1], 55f, Color.rgb(120, 190, 255)));
            shake = ty.points <= -10 ? .4f : .25f;
            texts.add(new FloatText(c[0], c[1] - 20f, String.valueOf(ty.points),
                    Color.rgb(255, 205, 205), Color.rgb(170, 30, 40)));
            sfx(SoundManager.MISS);
        } else if (ty.seconds < 0) {
            // sad fox with the clock: lose time
            time = Math.max(0f, time + ty.seconds);
            timeBad = .9f;
            burst(c[0], c[1], 26, Color.rgb(90, 160, 255));
            rings.add(new Ring(c[0], c[1], 90f, Color.rgb(255, 90, 90)));
            rings.add(new Ring(c[0], c[1], 55f, Color.rgb(120, 190, 255)));
            shake = .4f;
            texts.add(new FloatText(c[0], c[1] - 20f, ty.seconds + "s",
                    Color.rgb(255, 205, 205), Color.rgb(170, 30, 40)));
            sfx(SoundManager.MISS);
        } else if (ty.seconds > 0) {
            // clock fox: gain time (still counts toward the combo)
            registerHit();
            time = Math.min(99f, time + ty.seconds);
            timeGood = .9f;
            burstMulti(c[0], c[1], 30);
            rings.add(new Ring(c[0], c[1], 95f, Color.rgb(110, 235, 160)));
            rings.add(new Ring(c[0], c[1], 55f, Color.WHITE));
            shake = .2f;
            texts.add(new FloatText(c[0], c[1] - 20f, "+" + ty.seconds + "s",
                    Color.rgb(190, 255, 215), Color.rgb(20, 130, 90)));
            sfxPitch(SoundManager.HIT, 1f + Math.min(combo, 12) * .03f);
        } else {
            int mult = registerHit();
            int gain = ty.points * mult;
            score += gain;
            pop = .3f;
            boolean big = ty.points >= 25;
            burstMulti(c[0], c[1], big ? 48 : 28);
            rings.add(new Ring(c[0], c[1], big ? 130f : 90f, COMBO_COL[mult - 1]));
            rings.add(new Ring(c[0], c[1], 55f, Color.WHITE));
            if (ty == Type.GOLDEN) rings.add(new Ring(c[0], c[1], 190f, Color.rgb(255, 170, 40)));
            shake = big ? .35f : ty.points < 10 ? .12f : .22f;
            texts.add(new FloatText(c[0], c[1] - 20f, "+" + gain + (mult > 1 ? " x" + mult : ""),
                    COMBO_COL[mult - 1], Color.rgb(214, 51, 108)));
            sfxPitch(SoundManager.HIT, 1f + Math.min(combo, 12) * .03f);
        }
        spawnTimers.add(.45f);
    }

    private void update(float dt) {
        time -= dt;
        if (time <= 0f) {
            time = 0f;
            int rank = GameData.addScore(getContext(), diff, score);
            newBest = rank == 0;
            best = GameData.best(getContext(), diff);
            state = State.OVER;
            sfx(SoundManager.OVER);
            if (newBest) {
                burstMulti(W / 2f, H / 2f - 60f, 60);
                rings.add(new Ring(W / 2f, H / 2f - 60f, 220f, Color.rgb(255, 214, 64)));
            }
            return;
        }

        // power-up timers
        freezeT = Math.max(0f, freezeT - dt);
        bigT = Math.max(0f, bigT - dt);
        tripleT = Math.max(0f, tripleT - dt);

        // power-up orbs drift across the screen
        orbTimer -= dt;
        if (orbTimer <= 0f) { if (orbs.isEmpty()) spawnOrb(); else orbTimer = 3f; }
        Iterator<Orb> oi = orbs.iterator();
        while (oi.hasNext()) {
            Orb o = oi.next();
            o.t += dt;
            o.x += o.dir * o.speed * dt;
            if (RNG.nextFloat() < .5f) {
                puffs.add(new Puff(o.x + (RNG.nextFloat() - .5f) * 40f, orbY(o) + (RNG.nextFloat() - .5f) * 40f,
                        0f, -12f, .6f, 3f + RNG.nextFloat() * 2f, 0f, o.kind.light(), .9f, true));
            }
            if (o.x < -90f || o.x > W + 90f) { oi.remove(); orbTimer = Math.min(orbTimer, 6f + RNG.nextFloat() * 4f); }
        }

        for (Target o : targets) {
            if (freezeT <= 0f || o.hit) o.t += dt;          // frozen critters stop bobbing too
            if (o.hit) o.ht += dt; else if (freezeT <= 0f) o.x += o.dir * o.speed * dt;
            if (o.type == Type.GOLDEN && !o.hit && RNG.nextFloat() < .6f) {
                puffs.add(new Puff(o.x - o.dir * (20f + RNG.nextFloat() * 50f), o.lane - 20f - RNG.nextFloat() * 110f,
                        0f, -10f, .6f, 3f + RNG.nextFloat() * 3f, 0f, Color.rgb(255, 222, 90), .95f, true));
            }
        }

        Iterator<Target> it = targets.iterator();
        while (it.hasNext()) {
            Target o = it.next();
            if (o.hit && o.ht >= 1f) it.remove();
            else if (!o.hit && (o.x < -130f || o.x > W + 130f)) { it.remove(); spawnTimers.add(.3f); }
        }

        int ready = 0;
        for (int i = spawnTimers.size() - 1; i >= 0; i--) {
            float v = spawnTimers.get(i) - dt;
            if (v <= 0f) { spawnTimers.remove(i); ready++; } else spawnTimers.set(i, v);
        }
        for (int i = 0; i < ready; i++) spawn();
        while (targets.size() + spawnTimers.size() < maxTargets) spawn();

        Iterator<Ball> bi = balls.iterator();
        while (bi.hasNext()) {
            Ball b = bi.next();
            b.t += dt / .2f;
            if (b.t >= 1f) { land(b); bi.remove(); }
            else {
                float sc = 1f - b.t * .55f;
                puffs.add(new Puff(bx(b), by(b), 0f, 0f, .35f, 12f * sc, -22f, Color.rgb(255, 228, 150), .8f, false));
            }
        }
    }

    /** Visual-only updates; keeps running on the game-over screen too. */
    private void updateFx(float dt) {
        recoil = Math.max(0f, recoil - dt * 6f);
        fireCd = Math.max(0f, fireCd - dt);
        flash = Math.max(0f, flash - dt);
        shake = Math.max(0f, shake - dt);
        pop = Math.max(0f, pop - dt);
        timeGood = Math.max(0f, timeGood - dt);
        scoreBad = Math.max(0f, scoreBad - dt);
        timeBad = Math.max(0f, timeBad - dt);
        comboPop = Math.max(0f, comboPop - dt);
        comboLostT = Math.max(0f, comboLostT - dt);
        goT = Math.max(0f, goT - dt);
        bombFlash = Math.max(0f, bombFlash - dt);
        if (freezeT > 0f && state == State.PLAY) {
            frostSpark += dt;
            while (frostSpark > .06f) {
                frostSpark -= .06f;
                puffs.add(new Puff(RNG.nextFloat() * W, RNG.nextFloat() * H, (RNG.nextFloat() - .5f) * 10f,
                        8f + RNG.nextFloat() * 14f, 1.6f, 2.5f + RNG.nextFloat() * 3f, 0f,
                        RNG.nextBoolean() ? Color.rgb(200, 235, 255) : Color.WHITE, .9f, true));
            }
        }

        Iterator<Particle> pi = particles.iterator();
        while (pi.hasNext()) {
            Particle p = pi.next();
            p.life -= dt; p.x += p.vx * dt; p.y += p.vy * dt; p.vy += 500f * dt;
            if (p.life <= 0f) pi.remove();
        }
        Iterator<FloatText> ti = texts.iterator();
        while (ti.hasNext()) {
            FloatText f = ti.next();
            f.t += dt;
            if (f.t >= .9f) ti.remove();
        }
        Iterator<Ring> ri = rings.iterator();
        while (ri.hasNext()) {
            Ring r = ri.next();
            r.t += dt / .45f;
            if (r.t >= 1f) ri.remove();
        }
        Iterator<Puff> qi = puffs.iterator();
        while (qi.hasNext()) {
            Puff q = qi.next();
            q.life -= dt; q.x += q.vx * dt; q.y += q.vy * dt;
            q.vx -= q.vx * 2.5f * dt; q.vy -= q.vy * 2.5f * dt;
            if (q.life <= 0f) qi.remove();
        }
        // ambient floating sparkles
        ambient += dt;
        while (ambient > .22f) {
            ambient -= .22f;
            puffs.add(new Puff(RNG.nextFloat() * W, H * (.35f + RNG.nextFloat() * .6f),
                    (RNG.nextFloat() - .5f) * 12f, -(14f + RNG.nextFloat() * 26f),
                    3f + RNG.nextFloat() * 2f, 2.5f + RNG.nextFloat() * 3f, 0f,
                    CONFETTI[RNG.nextInt(CONFETTI.length)], .85f, true));
        }
    }

    // ---------- touch ----------
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        // One finger only: ACTION_DOWN is the first finger touching the screen. Extra fingers
        // (ACTION_POINTER_DOWN) are ignored, so two fingers can never fire two balls at once.
        if (action != MotionEvent.ACTION_DOWN) return true;
        int i = e.getActionIndex();
        float lx = (e.getX(i) - offX) / scale;
        float ly = (e.getY(i) - offY) / scale;
        if (lx < 0f || lx > W || ly < 0f || ly > H) return true;   // tap on the side / top bars
        switch (state) {
            case PAUSE:
                break;                                   // the pause dialog handles everything
            case COUNTDOWN:
                if (inPauseButton(lx, ly)) requestPause();
                break;
            case OVER:
                if (btn1.contains(lx, ly)) {
                    sfx(SoundManager.CLICK);
                    startGame();
                } else if (btn2.contains(lx, ly)) {
                    sfx(SoundManager.CLICK);
                    if (onExit != null) onExit.run();
                }
                break;
            case PLAY:
                if (inPauseButton(lx, ly)) requestPause();
                else if (fireCd <= 0f) fire(lx, ly);      // still reloading -> tap is ignored
                break;
            default:
                break;
        }
        return true;
    }

    // ---------- drawing ----------
    @Override
    protected void onDraw(Canvas canvas) {
        long now = System.nanoTime();
        float dt = (lastNs == 0L) ? 0f : Math.min(.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        if (state == State.COUNTDOWN) updateCountdown(dt);
        if (state == State.PLAY) update(dt);
        if (state != State.PAUSE) updateFx(dt);

        canvas.drawColor(Color.rgb(29, 24, 18));
        if (offX > 0f || offY > 0f) {                          // bars around the play field: dimmed background
            canvas.drawBitmap(bgBmp, bgFullSrc, fullDst, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(175, 20, 14, 10));
            canvas.drawRect(fullDst, paint);
        }
        canvas.save();
        if (shake > 0f) {
            float amp = shake * 40f * scale;
            canvas.translate((RNG.nextFloat() - .5f) * amp, (RNG.nextFloat() - .5f) * amp);
        }
        canvas.translate(offX, offY);
        canvas.scale(scale, scale);
        canvas.clipRect(0f, 0f, W, H);

        dst.set(0f, 0f, W, H);
        canvas.drawBitmap(bgBmp, bgSrc, dst, paint);
        drawRails(canvas);

        List<Target> sorted = new ArrayList<>(targets);
        Collections.sort(sorted, (a, b) -> Float.compare(a.lane, b.lane));
        for (Target o : sorted) drawTarget(canvas, o);
        drawOrbs(canvas);

        if (freezeT > 0f) {                                    // icy tint while frozen
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(withAlpha(Color.rgb(150, 210, 255), (int) (42 * Math.min(1f, freezeT / FREEZE_FADE))));
            canvas.drawRect(0f, 0f, W, H, paint);
        }

        drawEffects(canvas);

        if (bombFlash > 0f) {                                  // bomb flash
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(withAlpha(Color.WHITE, (int) (215 * bombFlash / .4f)));
            canvas.drawRect(0f, 0f, W, H, paint);
        }
        drawHud(canvas);
        if (state == State.COUNTDOWN) drawCountdown(canvas);
        else if (state != State.PLAY) drawOverlay(canvas);
        if (goT > 0f) drawGo(canvas);

        canvas.restore();
        postInvalidateOnAnimation();
    }

    private void drawRails(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        for (float y : lanes) {
            paint.setColor(Color.argb(55, 0, 0, 0));      c.drawRect(0f, y + 34f, W, y + 42f, paint);   // soft shadow
            paint.setColor(Color.rgb(34, 28, 36));        c.drawRect(0f, y + 7f, W, y + 15f, paint);    // black cap (like a torii top beam)
            paint.setColor(Color.rgb(226, 72, 40));       c.drawRect(0f, y + 15f, W, y + 31f, paint);   // vermilion lacquer
            paint.setColor(Color.rgb(255, 132, 92));      c.drawRect(0f, y + 17f, W, y + 21f, paint);   // shine
            paint.setColor(Color.rgb(160, 40, 24));       c.drawRect(0f, y + 27f, W, y + 31f, paint);   // shade
            paint.setColor(Color.rgb(40, 24, 26));        c.drawRect(0f, y + 31f, W, y + 34f, paint);   // dark trim
            paint.setColor(Color.rgb(255, 206, 92));
            for (float x = 45f; x < W; x += 90f) c.drawCircle(x, y + 23f, 3.5f, paint);               // gold studs
        }
    }

    private void drawTarget(Canvas c, Target o) {
        Type ty = o.type;
        float w = ty.w, h = spriteH(ty), t = o.t;
        float x = o.x, y = o.lane + 8f;
        float rot = 0f, sx = 1f, sy = 1f, alpha = 1f;

        if (!o.hit) {
            switch (ty) {
                case FOX:
                    y -= Math.abs((float) Math.sin(t * 6f)) * 24f;
                    rot = (float) Math.sin(t * 6f) * 3.5f; break;
                case RACCOON: case SAD_RACCOON:
                    rot = (float) Math.sin(t * 8f) * 7f;
                    y -= Math.abs((float) Math.sin(t * 8f)) * 5f; break;
                case SLOTH:
                    rot = (float) Math.sin(t * 2f) * 4f;
                    sy = 1f + (float) Math.sin(t * 3f) * .03f; break;
                case GOLDEN:
                    rot = (float) Math.sin(t * 10f) * 5f;
                    sy = 1f + (float) Math.sin(t * 12f) * .04f; break;
                case KING:
                    rot = (float) Math.sin(t * 7f) * 6f;
                    y -= Math.abs((float) Math.sin(t * 7f)) * 5f; break;
                case SAD_SLOTH:
                    rot = (float) Math.sin(t * 2f) * 4f;
                    sy = 1f + (float) Math.sin(t * 3f) * .03f; break;
                case CLOCK_FOX: case SAD_FOX:
                    rot = (float) Math.sin(t * 6f) * 3.5f;
                    y -= Math.abs((float) Math.sin(t * 6f)) * 10f; break;
                case CAPYBARA: case SAD_CAPY:
                    sy = 1f + (float) Math.sin(t * 5f) * .05f;
                    sx = 1f - (float) Math.sin(t * 5f) * .03f;
                    y -= Math.abs((float) Math.sin(t * 2.5f)) * 8f; break;
            }
            paint.setColor(Color.argb(46, 0, 0, 0));
            c.drawOval(x - w * .38f, o.lane + 5f, x + w * .38f, o.lane + 19f, paint);
        } else {
            float k = o.ht, p = k;
            alpha = 1f - Math.max(0f, p - .6f) / .4f;
            switch (ty) {
                case FOX: case GOLDEN: case CLOCK_FOX: case SAD_FOX: {
                    rot = (float) Math.toDegrees(k * 16f);
                    y -= (float) Math.sin(p * 3.14f) * 110f;
                    float s = 1f - p * .4f; sx = s; sy = s; break;
                }
                case RACCOON: case KING: case SAD_RACCOON:
                    sx = (float) Math.cos(k * 12f);
                    y += k * k * 260f - (float) Math.sin(p * 3.14f) * 60f;
                    rot = (float) Math.sin(k * 10f) * 11f; break;
                case SLOTH: case SAD_SLOTH: {
                    rot = (float) Math.sin(k * 5f) * 17f;
                    float d = Math.max(0f, k - .25f);
                    y += d * d * 520f;
                    sy = 1f + (float) Math.sin(k * 8f) * .08f; break;
                }
                case CAPYBARA: case SAD_CAPY: {
                    float s = p < .5f ? 1f + p * .9f : Math.max(0f, 1.45f - (p - .5f) * 2.9f);
                    sx = s; sy = s;
                    y -= (float) Math.sin(p * 3.14f) * 30f; break;
                }
            }
        }
        c.save();
        c.translate(x, y); c.rotate(rot); c.scale(sx, sy);
        paint.setAlpha((int) (Math.max(0f, alpha) * 255));
        if (freezeT > 0f && !o.hit) paint.setColorFilter(frostFilter);
        dst.set(-w / 2f, -h, w / 2f, 0f);
        c.drawBitmap(sprites.get(ty), null, dst, paint);
        paint.setColorFilter(null);
        paint.setAlpha(255);
        c.restore();
    }

    private void drawEffects(Canvas c) {
        // shockwave rings
        paint.setStyle(Paint.Style.STROKE);
        for (Ring r : rings) {
            float k = r.t, rad = r.max * (1f - (1f - k) * (1f - k));
            paint.setStrokeWidth(10f * (1f - k) + 1f);
            paint.setColor(r.color);
            paint.setAlpha((int) (255 * (1f - k)));
            c.drawCircle(r.x, r.y, rad, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);

        // soft puffs: smoke, trails, sparkles
        for (Puff q : puffs) {
            float k = q.life / q.max;
            float rad = Math.max(.5f, q.size + (q.max - q.life) * q.grow);
            float al = q.twinkle ? (float) Math.sin(3.14159f * (1f - k)) : k;
            int a = (int) (Math.max(0f, Math.min(1f, al)) * q.a0 * 255f);
            if (q.twinkle) {
                paint.setColor(q.color); paint.setAlpha(a / 4);
                c.drawCircle(q.x, q.y, rad * 2.6f, paint);
            }
            paint.setColor(q.color); paint.setAlpha(a);
            c.drawCircle(q.x, q.y, rad, paint);
        }
        paint.setAlpha(255);

        for (Particle p : particles) {
            paint.setColor(p.color);
            paint.setAlpha((int) (Math.min(1f, p.life * 2f) * 255));
            c.drawCircle(p.x, p.y, p.size, paint);
        }
        paint.setAlpha(255);

        for (FloatText f : texts) {
            int a = (int) ((1f - f.t / .9f) * 255);
            txt.setTextSize(36f + Math.max(0f, .25f - f.t) * 110f);
            float half = txt.measureText(f.label) / 2f + 10f;
            float tx = Math.max(half, Math.min(W - half, f.x));
            txt.setStyle(Paint.Style.STROKE); txt.setStrokeWidth(7f); txt.setColor((a << 24) | (f.stroke & 0xFFFFFF));
            c.drawText(f.label, tx, f.y - f.t * 60f, txt);
            txt.setStyle(Paint.Style.FILL); txt.setColor((a << 24) | (f.fill & 0xFFFFFF));
            c.drawText(f.label, tx, f.y - f.t * 60f, txt);
        }

        // cute cannonballs
        for (Ball b : balls) {
            float s = 1f - b.t * .55f, x = bx(b), y = by(b), R = 26f * s * (bigT > 0f ? 1.3f : 1f);
            paint.setStyle(Paint.Style.FILL);
            if (bigT > 0f) { paint.setColor(Color.argb(110, 255, 190, 60)); c.drawCircle(x, y, R * 1.45f, paint); }
            paint.setColor(Color.argb(60, 0, 0, 0)); c.drawCircle(x + 3f * s, y + 5f * s, R, paint);
            paint.setColor(Color.rgb(30, 32, 60)); c.drawCircle(x, y, R, paint);
            paint.setColor(Color.rgb(70, 78, 130)); c.drawCircle(x, y, R * .88f, paint);
            paint.setColor(Color.argb(150, 255, 255, 255)); c.drawCircle(x - R * .38f, y - R * .42f, R * .2f, paint);
            // eyes
            paint.setColor(Color.WHITE);
            c.drawCircle(x - R * .32f, y - R * .02f, R * .21f, paint);
            c.drawCircle(x + R * .32f, y - R * .02f, R * .21f, paint);
            paint.setColor(Color.rgb(30, 32, 60));
            c.drawCircle(x - R * .30f, y, R * .11f, paint);
            c.drawCircle(x + R * .34f, y, R * .11f, paint);
            // cheeks
            paint.setColor(Color.argb(175, 255, 120, 160));
            c.drawCircle(x - R * .58f, y + R * .30f, R * .14f, paint);
            c.drawCircle(x + R * .58f, y + R * .30f, R * .14f, paint);
            // smile
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.5f, R * .08f));
            paint.setColor(Color.WHITE);
            tmp.set(x - R * .17f, y + R * .10f, x + R * .17f, y + R * .46f);
            c.drawArc(tmp, 20f, 140f, false, paint);
            // fuse + spark
            paint.setColor(Color.rgb(122, 74, 38));
            paint.setStrokeWidth(Math.max(2f, R * .12f));
            c.drawLine(x, y - R * .9f, x + R * .28f, y - R * 1.28f, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(255, 214, 64)); c.drawCircle(x + R * .30f, y - R * 1.32f, R * .2f, paint);
            paint.setColor(Color.WHITE); c.drawCircle(x + R * .30f, y - R * 1.32f, R * .09f, paint);
        }

        drawCannon(c);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(150, 146, 136)); c.drawRect(0f, H - 14f, W, H, paint);
        paint.setColor(Color.rgb(200, 196, 184)); c.drawRect(0f, H - 14f, W, H - 9f, paint);
        paint.setColor(Color.rgb(112, 108, 100));
        for (float x = 30f; x < W; x += 80f) c.drawRect(x, H - 9f, x + 2f, H, paint);
    }

    private void drawCannon(Canvas c) {
        float px = W / 2f, py = H - 60f;
        paint.setStyle(Paint.Style.FILL);

        // barrel: red lacquer with gold + black trim (rotates toward the last tap, kicks back when firing)
        c.save();
        c.translate(px, py);
        c.rotate((float) Math.toDegrees(aim));
        c.translate(0f, recoil * 14f);
        paint.setColor(Color.rgb(120, 28, 18));   c.drawRoundRect(-33f, -MUZZLE - 4f, 33f, 24f, 22f, 22f, paint);
        paint.setColor(Color.rgb(226, 72, 40));   c.drawRoundRect(-29f, -MUZZLE, 29f, 20f, 20f, 20f, paint);
        paint.setColor(Color.argb(110, 255, 190, 150)); c.drawRoundRect(-21f, -MUZZLE + 22f, -11f, 4f, 6f, 6f, paint);
        // black muzzle band with a gold line and dark opening
        paint.setColor(Color.rgb(52, 46, 54));    c.drawRoundRect(-38f, -MUZZLE - 8f, 38f, -MUZZLE + 20f, 10f, 10f, paint);
        paint.setColor(Color.rgb(255, 206, 92));  c.drawRect(-36f, -MUZZLE + 12f, 36f, -MUZZLE + 16f, paint);
        paint.setColor(Color.rgb(15, 10, 12));    c.drawOval(-24f, -MUZZLE - 9f, 24f, -MUZZLE + 3f, paint);
        // gold band near the base
        paint.setColor(Color.rgb(255, 206, 92));  c.drawRect(-31f, -8f, 31f, -2f, paint);
        // sakura flower
        paint.setColor(Color.rgb(255, 214, 228));
        for (int k = 0; k < 5; k++) {
            double a = k * 1.2566 - 1.5708;
            c.drawCircle((float) Math.cos(a) * 5f, -53f + (float) Math.sin(a) * 5f, 3.6f, paint);
        }
        paint.setColor(Color.rgb(255, 206, 92));  c.drawCircle(0f, -53f, 2.6f, paint);
        // little face
        paint.setColor(Color.WHITE);
        c.drawCircle(-13f, -30f, 7.5f, paint); c.drawCircle(13f, -30f, 7.5f, paint);
        paint.setColor(Color.rgb(30, 20, 24));
        c.drawCircle(-12f, -29f, 4.2f, paint); c.drawCircle(14f, -29f, 4.2f, paint);
        paint.setColor(Color.argb(175, 255, 150, 180));
        c.drawCircle(-23f, -17f, 6f, paint); c.drawCircle(23f, -17f, 6f, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3f); paint.setColor(Color.rgb(30, 20, 24));
        tmp.set(-8f, -26f, 8f, -10f);
        c.drawArc(tmp, 20f, 140f, false, paint);
        paint.setStyle(Paint.Style.FILL);
        c.restore();

        // stone pedestal (like the one the fox statue sits on), with a little moss
        paint.setColor(Color.rgb(104, 100, 92));  c.drawRoundRect(px - 72f, py + 26f, px + 72f, H - 8f, 8f, 8f, paint);
        paint.setColor(Color.rgb(176, 172, 160)); c.drawRoundRect(px - 69f, py + 29f, px + 69f, H - 11f, 6f, 6f, paint);
        paint.setColor(Color.rgb(104, 100, 92));  c.drawRoundRect(px - 54f, py - 4f, px + 54f, py + 30f, 8f, 8f, paint);
        paint.setColor(Color.rgb(196, 192, 180)); c.drawRoundRect(px - 51f, py - 1f, px + 51f, py + 27f, 6f, 6f, paint);
        paint.setColor(Color.rgb(222, 218, 206)); c.drawRect(px - 46f, py + 1f, px + 46f, py + 6f, paint);
        paint.setColor(Color.rgb(88, 150, 70));
        c.drawOval(px - 52f, py - 5f, px - 24f, py + 5f, paint);
        c.drawOval(px + 38f, py + 25f, px + 68f, py + 36f, paint);
        paint.setColor(Color.rgb(120, 180, 90));  c.drawOval(px - 46f, py - 4f, px - 32f, py + 1f, paint);

        // reload ring: fills up while the cannon is reloading
        if (fireCd > 0f) {
            float fill = 1f - fireCd / FIRE_COOLDOWN;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(7f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Color.argb(235, 255, 214, 64));
            tmp.set(px - 54f, py - 54f, px + 54f, py + 54f);
            c.drawArc(tmp, -90f, 360f * fill, false, paint);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.FILL);
        }

        // muzzle flash
        if (flash > 0f) {
            float k = flash / .12f;
            float mx = px + (float) Math.sin(aim) * MUZZLE, my = py - (float) Math.cos(aim) * MUZZLE;
            paint.setColor(Color.argb((int) (200 * k), 255, 160, 40)); c.drawCircle(mx, my, 44f * k + 10f, paint);
            paint.setColor(Color.argb((int) (255 * k), 255, 244, 170)); c.drawCircle(mx, my, 26f * k + 6f, paint);
            paint.setColor(Color.argb((int) (255 * k), 255, 255, 255)); c.drawCircle(mx, my, 12f * k + 3f, paint);
        }
    }

    private void plaque(Canvas c, RectF r, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(24, 12, 12));
        c.drawRoundRect(r.left - 5, r.top - 5, r.right + 5, r.bottom + 5, 19f, 19f, paint);
        paint.setColor(Color.rgb(255, 201, 87));
        c.drawRoundRect(r.left - 3, r.top - 3, r.right + 3, r.bottom + 3, 17f, 17f, paint);
        paint.setColor(color);
        c.drawRoundRect(r, 14f, 14f, paint);
        paint.setColor(Color.argb(55, 255, 255, 255));
        c.drawRoundRect(r.left + 6f, r.top + 5f, r.right - 6f, r.top + r.height() * .4f, 10f, 10f, paint);
    }

    private void drawHud(Canvas c) {
        txt.setStyle(Paint.Style.FILL);
        int dark = Color.rgb(38, 30, 34);
        float T = top;

        plaque(c, new RectF(14f, T, 154f, T + 64f), scoreBad > 0f ? Color.rgb(200, 50, 50) : dark);
        txt.setColor(Color.rgb(255, 243, 214)); txt.setTextSize(18f); c.drawText("SCORE", 84f, T + 22f, txt);
        txt.setColor(Color.rgb(255, 214, 74)); txt.setTextSize(36f + (pop / .3f) * 12f);
        c.drawText(String.valueOf(score), 84f, T + 56f, txt);

        boolean low = state == State.PLAY && time <= 5f;
        float pulse = low ? .5f + .5f * (float) Math.sin(time * 8f) : 0f;
        int timeBg = low ? Color.rgb((int) (110 + 90 * pulse), 48, 40) : dark;
        if (timeGood > 0f) timeBg = Color.rgb(40, 150, 90);
        if (timeBad > 0f) timeBg = Color.rgb(200, 50, 50);
        plaque(c, new RectF(W - 234f, T, W - 84f, T + 64f), timeBg);
        txt.setColor(Color.rgb(255, 243, 214)); txt.setTextSize(18f); c.drawText("TIME", W - 159f, T + 22f, txt);
        txt.setColor(low ? Color.rgb(255, (int) (225 - 110 * pulse), 90) : Color.rgb(255, 214, 74));
        txt.setTextSize(36f + pulse * 8f + (timeGood + timeBad) / .9f * 12f);
        c.drawText(String.valueOf((int) Math.ceil(time)), W - 159f, T + 56f, txt);

        plaque(c, pauseRect, Color.rgb(226, 72, 40));
        paint.setColor(Color.WHITE);
        float cx = pauseRect.centerX(), cy = pauseRect.centerY();
        c.drawRect(cx - 13f, cy - 14f, cx - 3f, cy + 14f, paint);
        c.drawRect(cx + 3f, cy - 14f, cx + 13f, cy + 14f, paint);

        drawCombo(c);
        drawPowerChips(c);
    }

    private static int withAlpha(int color, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (color & 0xFFFFFF);
    }

    private void drawCombo(Canvas c) {
        float y = top + 78f;
        txt.setStyle(Paint.Style.FILL);
        if (combo >= 2) {
            int m = mult();
            int col = COMBO_COL[m - 1];
            float k = comboPop / .3f;
            hr.set(14f, y, 154f, y + 52f);
            plaque(c, hr, Color.rgb(38, 30, 34));
            txt.setColor(Color.rgb(255, 243, 214)); txt.setTextSize(16f);
            c.drawText("COMBO " + combo, 84f, y + 20f, txt);
            txt.setColor(col); txt.setTextSize(24f + k * 8f);
            c.drawText("x" + m, 40f, y + 44f, txt);
            float frac = m >= 4 ? 1f : (combo % 4) / 4f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(90, 255, 255, 255)); c.drawRoundRect(68f, y + 31f, 144f, y + 41f, 5f, 5f, paint);
            paint.setColor(col); c.drawRoundRect(68f, y + 31f, 68f + 76f * Math.max(.06f, frac), y + 41f, 5f, 5f, paint);
        } else if (comboLostT > 0f) {
            hr.set(14f, y, 154f, y + 40f);
            plaque(c, hr, Color.rgb(200, 50, 50));
            txt.setColor(Color.WHITE); txt.setTextSize(18f);
            c.drawText("COMBO LOST", 84f, y + 27f, txt);
        }
    }

    private void drawPowerChips(Canvas c) {
        float[] left = {freezeT, bigT, tripleT};
        Power[] kinds = {Power.FREEZE, Power.BIG, Power.TRIPLE};
        float x = W - 14f - 62f, y = top + 78f;
        for (int i = 0; i < kinds.length; i++) {
            if (left[i] <= 0f) continue;
            boolean blink = left[i] < 1.5f && ((int) (left[i] * 6f) % 2 == 0);   // flashes when about to expire
            hr.set(x, y, x + 62f, y + 52f);
            plaque(c, hr, blink ? Color.rgb(38, 30, 34) : kinds[i].color);
            drawIcon(c, kinds[i], x + 31f, y + 21f, 12f);
            float frac = Math.min(1f, left[i] / kinds[i].dur);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(110, 0, 0, 0)); c.drawRoundRect(x + 8f, y + 39f, x + 54f, y + 46f, 3.5f, 3.5f, paint);
            paint.setColor(Color.WHITE); c.drawRoundRect(x + 8f, y + 39f, x + 8f + 46f * frac, y + 46f, 3.5f, 3.5f, paint);
            x -= 76f;
        }
    }

    private void drawOrbs(Canvas c) {
        for (Orb o : orbs) {
            float x = o.x, y = orbY(o), pulse = .5f + .5f * (float) Math.sin(o.t * 6f);
            int col = o.kind.color;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(40, 0, 0, 0)); c.drawOval(x - 22f, o.lane + 9f, x + 22f, o.lane + 21f, paint);
            paint.setColor(withAlpha(col, 45 + (int) (40 * pulse))); c.drawCircle(x, y, ORB_R * 1.9f + pulse * 6f, paint);
            paint.setColor(withAlpha(col, 95)); c.drawCircle(x, y, ORB_R * 1.4f, paint);
            paint.setColor(Color.WHITE);
            for (int k = 0; k < 3; k++) {                       // orbiting sparkles
                double a = o.t * 2.4 + k * 2.094;
                c.drawCircle(x + (float) Math.cos(a) * (ORB_R + 15f), y + (float) Math.sin(a) * (ORB_R + 15f), 4f, paint);
            }
            paint.setColor(Color.rgb(255, 248, 225)); c.drawCircle(x, y, ORB_R + 4f, paint);
            paint.setColor(col);                       c.drawCircle(x, y, ORB_R, paint);
            paint.setColor(Color.argb(110, 255, 255, 255)); c.drawCircle(x - ORB_R * .3f, y - ORB_R * .35f, ORB_R * .28f, paint);
            drawIcon(c, o.kind, x, y, ORB_R * .62f);
        }
        paint.setAlpha(255);
    }

    /** Simple white icons drawn with shapes (no image files needed). */
    private void drawIcon(Canvas c, Power p, float cx, float cy, float r) {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, r * .22f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        switch (p) {
            case FREEZE:                                         // snowflake
                for (int k = 0; k < 3; k++) {
                    double a = k * Math.PI / 3;
                    float dx = (float) Math.cos(a) * r, dy = (float) Math.sin(a) * r;
                    c.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, paint);
                }
                break;
            case BIG:                                            // big target
                c.drawCircle(cx, cy, r, paint);
                paint.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy, r * .38f, paint);
                break;
            case TRIPLE:                                         // three balls
                paint.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy - r * .55f, r * .34f, paint);
                c.drawCircle(cx - r * .62f, cy + r * .45f, r * .34f, paint);
                c.drawCircle(cx + r * .62f, cy + r * .45f, r * .34f, paint);
                break;
            case BOMB:                                           // bomb with a fuse
                paint.setStyle(Paint.Style.FILL);
                c.drawCircle(cx - r * .1f, cy + r * .2f, r * .72f, paint);
                paint.setStyle(Paint.Style.STROKE);
                c.drawLine(cx + r * .35f, cy - r * .35f, cx + r * .75f, cy - r * .85f, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(255, 230, 120));
                c.drawCircle(cx + r * .85f, cy - r * .95f, r * .22f, paint);
                break;
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCountdown(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(95, 38, 16, 52));
        c.drawRect(0f, 0f, W, H, paint);

        int n = Math.max(1, (int) Math.ceil(countT));
        float p = 1f - (countT - (n - 1));               // 0 at the start of a number, 1 at its end
        float s = p < .35f ? 1.7f - .7f * (p / .35f) : 1f;          // pops in big, settles
        float a = p > .8f ? 1f - .6f * ((p - .8f) / .2f) : 1f;      // fades slightly before the next one
        int fill = n >= 3 ? Color.rgb(255, 130, 105) : n == 2 ? Color.rgb(255, 176, 64) : Color.rgb(255, 228, 96);
        int alpha = (int) (255 * a);

        // pulse ring
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10f * (1f - p) + 2f);
        paint.setColor(withAlpha(fill, (int) (200 * (1f - p))));
        c.drawCircle(W / 2f, H / 2f, 120f + 90f * p, paint);
        paint.setStyle(Paint.Style.FILL);

        txt.setStyle(Paint.Style.FILL);
        txt.setColor(withAlpha(Color.WHITE, 255));
        txt.setTextSize(40f);
        c.drawText("Get ready!", W / 2f, H / 2f - 190f, txt);

        txt.setTextSize(230f * s);
        txt.setStyle(Paint.Style.STROKE); txt.setStrokeWidth(18f);
        txt.setColor(withAlpha(Color.rgb(70, 24, 46), alpha));
        c.drawText(String.valueOf(n), W / 2f, H / 2f + 80f * s, txt);
        txt.setStyle(Paint.Style.FILL);
        txt.setColor(withAlpha(fill, alpha));
        c.drawText(String.valueOf(n), W / 2f, H / 2f + 80f * s, txt);

        txt.setColor(Color.rgb(255, 243, 214));
        txt.setTextSize(28f);
        c.drawText(GameData.NAMES[diff] + " mode", W / 2f, H / 2f + 170f, txt);
        txt.setTextSize(22f);
        c.drawText("Chain hits for combos - shoot the glowing orbs!", W / 2f, H / 2f + 208f, txt);
    }

    private void drawGo(Canvas c) {
        float k = goT / .7f;                             // 1 -> 0
        float s = 1f + (1f - k) * .6f;
        int alpha = (int) (255 * Math.min(1f, k * 2f));
        txt.setTextSize(150f * s);
        txt.setStyle(Paint.Style.STROKE); txt.setStrokeWidth(14f);
        txt.setColor(withAlpha(Color.rgb(20, 110, 70), alpha));
        c.drawText("GO!", W / 2f, H / 2f + 50f * s, txt);
        txt.setStyle(Paint.Style.FILL);
        txt.setColor(withAlpha(Color.rgb(140, 245, 160), alpha));
        c.drawText("GO!", W / 2f, H / 2f + 50f * s, txt);
    }

    private float drawLines(Canvas c, String[] lines, float y, float size, float lineH) {
        txt.setTextSize(size);
        for (String s : lines) { c.drawText(s, W / 2f, y, txt); y += lineH; }
        return y;
    }

    private void drawOverlay(Canvas c) {
        if (state == State.MENU || state == State.PAUSE) return;   // pause menu is a dialog now
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(155, 38, 16, 52));
        c.drawRect(0f, 0f, W, H, paint);
        txt.setStyle(Paint.Style.FILL); txt.setColor(Color.WHITE);

        String[] title, sub;
        String b1;
        if (state == State.PAUSE) {
            title = new String[]{"Paused"}; sub = new String[]{GameData.NAMES[diff] + " mode"}; b1 = "Resume";
        } else {
            title = new String[]{"Time's up!"};
            sub = new String[]{"Score: " + score, newBest ? "New high score!" : "Best: " + best,
                    "Best combo: " + bestCombo, GameData.NAMES[diff] + " mode"};
            b1 = "Play again";
        }

        float y = H / 2f - 130f;
        y = drawLines(c, title, y, 64f, 72f);
        y += 6f;
        y = drawLines(c, sub, y, 28f, 38f);
        y += 16f;

        btn1.set(W / 2f - 150f, y, W / 2f + 150f, y + 70f);
        btn2.set(W / 2f - 150f, y + 90f, W / 2f + 150f, y + 160f);

        txt.setTextSize(32f);
        plaque(c, btn1, Color.rgb(226, 72, 40));
        c.drawText(b1, W / 2f, y + 47f, txt);
        plaque(c, btn2, Color.rgb(52, 46, 54));
        c.drawText("Main menu", W / 2f, y + 137f, txt);
    }
}