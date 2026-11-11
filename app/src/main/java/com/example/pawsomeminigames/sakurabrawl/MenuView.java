package com.example.pawsomeminigames.sakurabrawl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/** The main menu screen and the settings panel. */
public class MenuView extends View {

    public interface Listener {
        void onPlay(boolean twoPlayers);
        void onQuit();
    }

    private static final int BTN_NONE = 0;
    private static final int BTN_PLAY = 1;
    private static final int BTN_QUIT = 3;
    private static final int BTN_TWO = 5;
    private static final int BTN_GO = 6;      // PLAY inside the How to Play card
    private static final int BTN_BACK = 7;    // BACK inside the How to Play card
    private static final int BTN_ARROW_L = 8; // difficulty: previous
    private static final int BTN_ARROW_R = 9; // difficulty: next

    private static final int CREAM = UiKit.BTN_TEXT;        // text on lacquer buttons
    private static final int DARK_BROWN = UiKit.INK;        // text on yellow buttons and cream panels
    private static final int SOFT_WHITE = 0xFFFFF8E3;       // text drawn straight on the picture

    private final Listener listener;
    private final SettingsManager settings;
    private final SoundManager sound;
    private final UiKit kit = new UiKit();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF tmp = new RectF();

    private boolean running = false;
    private long lastTimeNs;
    private float w, h, time;
    private Shader skyShader;

    // Menu picture (res/drawable/menu_bg). If it is missing, the drawn scene is used instead.
    // Fractions of the picture height: where the fence area starts, where the ground starts,
    // the plain grass band that gets repeated, and the bottom strip with the leaves and rocks.
    private static final float BG_MID_TOP = 0.4557f;
    private static final float BG_GROUND = 0.6771f;
    private static final float BG_GRASS_A = 0.6836f;
    private static final float BG_GRASS_B = 0.8464f;
    private static final float BG_EDGE = 0.8398f;
    // Pictures: menu_bg (portrait, also used cropped in landscape) and optional menu_bg_land (landscape).
    private Bitmap menuBg;                 // the picture in use right now
    private Bitmap menuBgPortrait, menuBgLand;
    private boolean menuBgPortraitTried = false, menuBgLandTried = false;
    private boolean menuSingle = false;    // true: one picture rectangle, no repeated grass
    private boolean land = false;          // wider than tall
    private float wu;                      // size unit: the width in portrait, a portrait-like width in landscape
    private final Paint bgPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect srcTop = new Rect(), srcMid = new Rect(), srcGrass = new Rect(), srcEdge = new Rect();
    private final RectF dstTop = new RectF(), dstMid = new RectF(), dstEdge = new RectF();
    private final RectF tileRect = new RectF();
    private float grassTop, grassBottom, tileH;
    private Petals petals;
    private Capybara leftCapy, rightCapy;
    private int character;                       // the animal player 1 picked
    private int character2;                      // the animal player 2 picked (2-player mode)
    private final RectF pickRect = new RectF();  // tap here to change player 1's animal
    private final RectF pickRect2 = new RectF(); // tap here to change player 2's animal
    private int pickPressed = 0;                 // 0 = none, 1 = left, 2 = right
    private float charPop = 0f;                  // little bounce after changing animal
    private float charPop2 = 0f;

    // Main menu layout
    private final RectF logoRect = new RectF();
    private final RectF playRect = new RectF();
    private final RectF twoRect = new RectF();
    private final RectF quitRect = new RectF();

    private float u;

    // How to Play card (opens when 1 PLAYER is tapped; also holds the difficulty choice)
    private boolean howOpen = false;
    private final RectF howPanel = new RectF();
    private final RectF howHeader = new RectF();
    private final RectF howArrowL = new RectF();
    private final RectF howArrowR = new RectF();
    private final RectF howDiff = new RectF();
    private final RectF howGo = new RectF();
    private final RectF howBack = new RectF();
    private final float[] howRowY = new float[4];     // vertical center of each rule row
    private float howRowH, howTextX, howTextSize, howDescY;
    private float hu;                 // size unit for the How to Play card (shrinks on short screens)
    private float diffPop = 0f;       // little bounce of the difficulty pill after changing it
    private final String[][] howLines = new String[4][];
    private static final String[] HOW_TEXT = {
            "Push the other animal out of the ring!",
            "Tap to charge at them. Drag to steer.",
            "Each round is 30 seconds. After 20 seconds, the ring starts to shrink!",
            "Win 4 of 7 rounds to win the match."
    };
    private static final String[] HOW_BADGE = {null, "TAP", "30s", "4/7"};
    private static final int[] HOW_BADGE_COLOR = {0, 0xFF6CC24A, 0xFF4FA3F7, 0xFFFFC233};
    private final Paint measure = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Button press animation
    private int pressed = BTN_NONE;
    private final float[] anim = new float[10];

    // Current setting values (music / sound live in the Pawsome hub's Settings)
    private int difficulty;

    public MenuView(Context context, SoundManager sound, Listener listener) {
        super(context);
        this.sound = sound;
        this.listener = listener;
        settings = new SettingsManager(context);
        measure.setTypeface(Typeface.DEFAULT_BOLD);
        difficulty = settings.getDifficulty();
        character = settings.getCharacter();
        character2 = settings.getCharacter2();
    }

    // ---- Lifecycle ----
    public void resume() {
        running = true;
        lastTimeNs = System.nanoTime();
        invalidate();
    }

    public void pause() {
        running = false;
    }

    /** Back button: closes the How to Play card. Returns true if it handled the press. */
    public boolean handleBack() {
        if (howOpen) {
            howOpen = false;
            pressed = BTN_NONE;
            return true;
        }
        return false;
    }

    // ---- Layout ----
    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);
        w = width;
        h = height;
        land = w > h;
        wu = land ? h * 0.62f : w;
        u = wu * 0.1f;

        skyShader = new LinearGradient(0, 0, 0, h * 0.55f,
                0xFF8FD3FF, 0xFFE3F6FF, Shader.TileMode.CLAMP);
        petals = new Petals(w, h, 22, wu);
        chooseMenuBackground();
        if (menuBg != null) layoutMenuBackground();

        float cx = w / 2f;
        float s, capyY;
        if (land) {
            // Wide screen: the characters stand on both sides of the button column
            s = wu * 0.15f;
            capyY = h * 0.60f;
            float off = Math.min(w * 0.30f, wu * 1.15f);
            leftCapy = new Capybara(cx - off, capyY, s, 1, 0xFF2F7DFF, character);
            rightCapy = new Capybara(cx + off, capyY, s, -1, 0xFFE8433A, character2);
            logoRect.set(cx - wu * 0.85f, h * 0.04f, cx + wu * 0.85f, h * 0.04f + h * 0.19f);
        } else {
            s = wu * 0.13f;
            capyY = h * 0.48f;
            leftCapy = new Capybara(w * 0.29f, capyY, s, 1, 0xFF2F7DFF, character);
            rightCapy = new Capybara(w * 0.71f, capyY, s, -1, 0xFFE8433A, character2);
            logoRect.set(w * 0.07f, h * 0.055f, w * 0.93f, h * 0.205f);
        }
        pickRect.set(leftCapy.x - s * 1.7f, leftCapy.y - s * 1.3f,
                leftCapy.x + s * 1.9f, leftCapy.y + s * 1.2f);
        pickRect2.set(rightCapy.x - s * 1.9f, rightCapy.y - s * 1.3f,
                rightCapy.x + s * 1.7f, rightCapy.y + s * 1.2f);

        // Same button size as Pop Mania and Critter Toss: 300dp x 68dp with 16dp gaps.
        // (On short landscape screens they shrink together so all three still fit.)
        float dens = getResources().getDisplayMetrics().density;
        float gap = 16f * dens;
        float top = land ? h * 0.27f : h * 0.565f;
        float bh = Math.min(68f * dens, (h * 0.96f - top - 2f * gap) / 3f);
        float bw = Math.min(300f * dens * (bh / (68f * dens)), w * 0.9f);
        playRect.set(cx - bw / 2f, top, cx + bw / 2f, top + bh);
        twoRect.set(cx - bw / 2f, playRect.bottom + gap, cx + bw / 2f, playRect.bottom + gap + bh);
        quitRect.set(cx - bw / 2f, twoRect.bottom + gap, cx + bw / 2f, twoRect.bottom + gap + bh);

        layoutHow();
    }

    /**
     * Lays out the How to Play screen like Critter Toss: the card holds only the rules,
     * and the difficulty picker, PLAY and BACK sit below it.
     */
    private void layoutHow() {
        // Height of the whole stack in units: header overhang + card + picker + text + PLAY + BACK
        final float totalUnits = 16.5f;
        hu = u;
        if (u * totalUnits > h * 0.94f) hu = h * 0.94f / totalUnits;   // short (landscape) screens

        float pw = wu * 0.92f;
        float ph = hu * 8.1f;
        float hh = hu * 1.5f;
        float overhang = hh * 0.45f;
        float top = (h - hu * totalUnits) / 2f + overhang;
        howPanel.set((w - pw) / 2f, top, (w + pw) / 2f, top + ph);
        float hw = pw * 0.66f;
        howHeader.set(w / 2f - hw / 2f, howPanel.top - overhang,
                w / 2f + hw / 2f, howPanel.top - overhang + hh);

        float x0 = howPanel.left + hu * 0.7f;
        float x1 = howPanel.right - hu * 0.7f;
        float y = howPanel.top + hu * 1.4f;

        // Rule rows (inside the card)
        howRowH = hu * 1.55f;
        howTextX = x0 + hu * 1.7f;
        float textW = x1 - howTextX;
        howTextSize = hu * 0.42f;
        measure.setTextSize(howTextSize);
        for (int i = 0; i < 4; i++) {
            howRowY[i] = y + howRowH * (i + 0.5f);
            howLines[i] = wrap(HOW_TEXT[i], textW, howTextSize);
        }

        // Difficulty picker below the card: (<)  [stars NAME]  (>)
        y = howPanel.bottom + hu * 1.0f;
        float pillH = hu * 1.9f;
        float ds = hu * 1.6f;
        float ay = y + (pillH - ds) / 2f;
        howArrowL.set(howPanel.left, ay, howPanel.left + ds, ay + ds);
        howArrowR.set(howPanel.right - ds, ay, howPanel.right, ay + ds);
        howDiff.set(howArrowL.right + hu * 0.3f, y, howArrowR.left - hu * 0.3f, y + pillH);
        howDescY = y + pillH + hu * 0.65f;
        y += pillH + hu * 1.2f;

        // Big PLAY button, then BACK
        float gw = pw * 0.88f;
        howGo.set(w / 2f - gw / 2f, y, w / 2f + gw / 2f, y + hu * 2.0f);
        y += hu * 2.0f + hu * 0.45f;
        float bw = pw * 0.46f;
        howBack.set(w / 2f - bw / 2f, y, w / 2f + bw / 2f, y + hu * 1.1f);
    }

    /** Splits text into lines that fit maxWidth (measured with the 'measure' paint at textSize). */
    private String[] wrap(String text, float maxWidth, float textSize) {
        float size = textSize;
        String[] lines;
        while (true) {
            measure.setTextSize(size);
            java.util.ArrayList<String> list = new java.util.ArrayList<>();
            String line = "";
            for (String word : text.split(" ")) {
                String test = line.isEmpty() ? word : line + " " + word;
                if (measure.measureText(test) <= maxWidth || line.isEmpty()) {
                    line = test;
                } else {
                    list.add(line);
                    line = word;
                }
            }
            if (!line.isEmpty()) list.add(line);
            lines = list.toArray(new String[0]);
            if (lines.length <= 2 || size <= textSize * 0.7f) break;
            size *= 0.94f;   // too long: shrink a little and try again
        }
        howTextSize = Math.min(howTextSize, size);
        measure.setTextSize(howTextSize);
        return lines;
    }

    private int howButtonAt(float x, float y) {
        if (howGo.contains(x, y)) return BTN_GO;
        if (howBack.contains(x, y)) return BTN_BACK;
        if (howArrowL.contains(x, y)) return BTN_ARROW_L;
        if (howArrowR.contains(x, y)) return BTN_ARROW_R;
        return BTN_NONE;
    }

    private void changeDifficulty(int step) {
        difficulty = (difficulty + step + 3) % 3;
        settings.setDifficulty(difficulty);
        diffPop = 1f;
    }

    // ---- Touch ----
    private int mainButtonAt(float x, float y) {
        if (playRect.contains(x, y)) return BTN_PLAY;
        if (twoRect.contains(x, y)) return BTN_TWO;
        if (quitRect.contains(x, y)) return BTN_QUIT;
        return BTN_NONE;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();

        if (howOpen) {
            if (action == MotionEvent.ACTION_DOWN) {
                pressed = howButtonAt(x, y);
                if (pressed == BTN_NONE && howDiff.contains(x, y)) {
                    changeDifficulty(1);   // tapping the difficulty box also cycles it
                    sound.play(SoundManager.CLICK);
                }
            } else if (action == MotionEvent.ACTION_UP) {
                int p = pressed;
                pressed = BTN_NONE;
                if (p != BTN_NONE && howButtonAt(x, y) == p) onHowButton(p);
            } else if (action == MotionEvent.ACTION_CANCEL) {
                pressed = BTN_NONE;
            }
            return true;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            pressed = mainButtonAt(x, y);
            pickPressed = 0;
            if (pressed == BTN_NONE) {
                if (pickRect.contains(x, y)) pickPressed = 1;
                else if (pickRect2.contains(x, y)) pickPressed = 2;
            }
        } else if (action == MotionEvent.ACTION_UP) {
            int p = pressed;
            pressed = BTN_NONE;
            if (p != BTN_NONE && mainButtonAt(x, y) == p) {
                onMainButton(p);
            } else if (pickPressed == 1 && pickRect.contains(x, y)) {
                cycleCharacter();
            } else if (pickPressed == 2 && pickRect2.contains(x, y)) {
                cycleCharacter2();
            }
            pickPressed = 0;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            pressed = BTN_NONE;
            pickPressed = 0;
        }
        return true;
    }

    /** Switches to the next animal and remembers the choice. */
    private void cycleCharacter() {
        character = Species.next(character);
        settings.setCharacter(character);
        leftCapy.setSpecies(character);
        charPop = 1f;
        sound.play(SoundManager.CLICK);
    }

    /** Same for player 2 (used in 2-player mode). */
    private void cycleCharacter2() {
        character2 = Species.next(character2);
        settings.setCharacter2(character2);
        rightCapy.setSpecies(character2);
        charPop2 = 1f;
        sound.play(SoundManager.CLICK);
    }

    private void onHowButton(int id) {
        sound.play(SoundManager.CLICK);
        switch (id) {
            case BTN_GO:
                howOpen = false;
                listener.onPlay(false);
                break;
            case BTN_BACK:
                howOpen = false;
                break;
            case BTN_ARROW_L:
                changeDifficulty(-1);
                break;
            case BTN_ARROW_R:
                changeDifficulty(1);
                break;
            default:
                break;
        }
    }

    private void onMainButton(int id) {
        sound.play(SoundManager.CLICK);
        switch (id) {
            case BTN_PLAY:
                howOpen = true;   // show How to Play first; its PLAY button starts the game
                break;
            case BTN_TWO:
                listener.onPlay(true);
                break;
            case BTN_QUIT:
                listener.onQuit();
                break;
            default:
                break;
        }
    }

    // ---- Loop ----
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long now = System.nanoTime();
        float dt = (now - lastTimeNs) / 1_000_000_000f;
        lastTimeNs = now;
        if (dt > 0.05f) dt = 0.05f;

        update(dt);
        drawMenu(canvas);

        if (running) {
            postInvalidateOnAnimation();
        }
    }

    private void update(float dt) {
        if (petals == null) return;
        time += dt;
        petals.update(dt);
        leftCapy.update(dt);
        rightCapy.update(dt);
        charPop = Math.max(0f, charPop - dt * 4f);
        charPop2 = Math.max(0f, charPop2 - dt * 4f);
        diffPop = Math.max(0f, diffPop - dt * 4.5f);

        float k = Math.min(1f, dt * 18f);
        for (int i = 0; i < anim.length; i++) {
            float target = (i == pressed) ? 1f : 0f;
            anim[i] += (target - anim[i]) * k;
        }
    }

    // ---- Drawing ----
    private void drawMenu(Canvas c) {
        if (petals == null) return;

        if (menuBg != null) {
            drawPictureBackground(c);
        } else if (land) {
            drawSimpleWideBackground(c);
        } else {
            drawBackground(c);
            drawTree(c, w * 0.05f, 1);
            drawTree(c, w * 0.95f, -1);
        }

        c.save();
        float pop = 1f + 0.12f * charPop;
        c.scale(pop, pop, leftCapy.x, leftCapy.y + leftCapy.radius * 0.5f);
        leftCapy.draw(c);
        c.restore();
        c.save();
        float pop2 = 1f + 0.12f * charPop2;
        c.scale(pop2, pop2, rightCapy.x, rightCapy.y + rightCapy.radius * 0.5f);
        rightCapy.draw(c);
        c.restore();

        // Names of the chosen animals. Tap a character to change it.
        float nameY = leftCapy.y + leftCapy.radius * 0.95f;
        if (!land) nameY = Math.min(nameY, playRect.top - wu * 0.035f);
        kit.fitText(c, "< " + Species.name(character) + " >", leftCapy.x, nameY,
                wu * 0.045f, wu * 0.36f, SOFT_WHITE);
        kit.fitText(c, "< " + Species.name(character2) + " >", rightCapy.x, nameY,
                wu * 0.045f, wu * 0.36f, SOFT_WHITE);

        drawLogo(c);
        petals.draw(c);

        // Buttons
        beginPress(c, BTN_PLAY, playRect);
        kit.woodBox(c, playRect, playRect.height() * 0.35f, UiKit.YEL_BOT, UiKit.YEL_EDGE, UiKit.YEL_TOP);
        float ts = playRect.height() * 0.34f;
        kit.fitText(c, "1 PLAYER", playRect.centerX() + playRect.height() * 0.25f,
                playRect.centerY(), ts, playRect.width() * 0.6f, DARK_BROWN);
        drawPlayIcon(c, playRect.left + playRect.height() * 0.75f, playRect.centerY(),
                playRect.height() * 0.28f);
        endPress(c);

        beginPress(c, BTN_TWO, twoRect);
        kit.chip(c, twoRect, twoRect.height() * 0.35f);
        kit.fitText(c, "2 PLAYERS", twoRect.centerX() + twoRect.height() * 0.3f,
                twoRect.centerY(), twoRect.height() * 0.34f, twoRect.width() * 0.6f, DARK_BROWN);
        float fr = twoRect.height() * 0.2f;
        kit.face(c, twoRect.left + twoRect.height() * 0.55f, twoRect.centerY(), fr, leftCapy.species);
        kit.face(c, twoRect.left + twoRect.height() * 0.55f + fr * 2.5f, twoRect.centerY(), fr,
                rightCapy.species);
        endPress(c);

        beginPress(c, BTN_QUIT, quitRect);
        kit.woodBox(c, quitRect, quitRect.height() * 0.35f, UiKit.LACQ_BOT, UiKit.GOLD, UiKit.LACQ_TOP);
        kit.fitText(c, "BACK", quitRect.centerX(), quitRect.centerY(),
                quitRect.height() * 0.34f, quitRect.width() * 0.8f, CREAM);
        endPress(c);

        // Tagline (only if there is room)
        if (quitRect.bottom + wu * 0.1f < h * 0.96f) {
            kit.fitText(c, "Bloom. Brawl. Win!", w / 2f, h * 0.95f,
                    wu * 0.05f, wu * 0.8f, SOFT_WHITE);
        }

        if (howOpen) {
            drawHow(c);
        }
    }

    /** Picks the picture for the current orientation (loading it the first time it is needed). */
    private void chooseMenuBackground() {
        menuBg = null;
        menuSingle = false;
        if (land) {
            if (!menuBgLandTried) {
                menuBgLandTried = true;
                menuBgLand = loadDrawable("menu_bg_land");
            }
            if (menuBgLand != null) {
                menuBg = menuBgLand;
                menuSingle = true;
                return;
            }
        }
        if (!menuBgPortraitTried) {
            menuBgPortraitTried = true;
            menuBgPortrait = loadDrawable("menu_bg");
        }
        menuBg = menuBgPortrait;
        menuSingle = land;   // portrait picture on a wide screen: one cropped rectangle
    }

    /** Loads a picture from res/drawable by name, or returns null if it is not there. */
    private Bitmap loadDrawable(String name) {
        return Assets.bitmap(getContext(), name);   // cached: the splash screen already loaded it
    }

    /**
     * Fits the picture to the screen without stretching it.
     *  - Landscape picture: scaled to cover the screen (only the edges can be cropped).
     *  - Portrait picture on a wide screen: fitted by width, cropped around the fence and banners.
     *  - Portrait picture on a portrait screen: the fence area keeps its shape; the sky above is
     *    squeezed or stretched to fit, and the empty grass below is filled by repeating
     *    (mirroring) a plain strip of grass.
     */
    private void layoutMenuBackground() {
        float bw = menuBg.getWidth();
        float bh = menuBg.getHeight();

        if (menuSingle) {
            float s;
            float ox = 0f, oy;
            if (menuBg == menuBgLand) {
                s = Math.max(w / bw, h / bh);
                ox = (w - bw * s) / 2f;
                oy = (h - bh * s) / 2f;
            } else {
                s = w / bw;
                oy = h * 0.70f - bh * BG_GROUND * s;          // ground line a bit below the middle
                oy = Math.min(0f, Math.max(h - bh * s, oy));  // never show past the picture
            }
            srcMid.set(0, 0, (int) bw, (int) bh);
            dstMid.set(ox, oy, ox + bw * s, oy + bh * s);
            return;
        }

        float s = w / bw;
        float midTop = bh * BG_MID_TOP;
        float ground = bh * BG_GROUND;
        float ga = bh * BG_GRASS_A;
        float gb = bh * BG_GRASS_B;
        float edge = bh * BG_EDGE;

        float y2 = h * 0.46f;                         // screen y of the ground line
        float y1 = y2 - (ground - midTop) * s;        // screen y where the fence area starts
        if (y1 < h * 0.2f) {
            y1 = h * 0.2f;
            y2 = y1 + (ground - midTop) * s;
        }
        float edgeTop = Math.max(y2, h - (bh - edge) * s);

        srcTop.set(0, 0, (int) bw, (int) midTop);
        dstTop.set(0, 0, w, y1 + 1f);
        srcMid.set(0, (int) midTop, (int) bw, (int) ground);
        dstMid.set(0, y1, w, y2 + 1f);
        srcGrass.set(0, (int) ga, (int) bw, (int) gb);
        tileH = Math.max(2f, (gb - ga) * s);
        grassTop = y2;
        grassBottom = edgeTop + 1f;
        srcEdge.set(0, (int) edge, (int) bw, (int) bh);
        dstEdge.set(0, edgeTop, w, h);
    }

    private void drawPictureBackground(Canvas c) {
        if (menuSingle) {
            c.drawBitmap(menuBg, srcMid, dstMid, bgPaint);
            return;
        }

        c.drawBitmap(menuBg, srcTop, dstTop, bgPaint);
        c.drawBitmap(menuBg, srcMid, dstMid, bgPaint);

        if (grassBottom > grassTop) {
            c.save();
            c.clipRect(0, grassTop, w, grassBottom);
            int k = 0;
            for (float top = grassTop; top < grassBottom; top += tileH, k++) {
                tileRect.set(0, top, w, top + tileH);
                if ((k & 1) == 1) {
                    c.save();
                    c.scale(1f, -1f, 0f, top + tileH / 2f);   // every second strip is flipped
                    c.drawBitmap(menuBg, srcGrass, tileRect, bgPaint);
                    c.restore();
                } else {
                    c.drawBitmap(menuBg, srcGrass, tileRect, bgPaint);
                }
            }
            c.restore();
        }

        if (dstEdge.height() > 1f) {
            c.drawBitmap(menuBg, srcEdge, dstEdge, bgPaint);
        }
    }

    /** Plain sky and grass for a wide screen when no picture is available. */
    private void drawSimpleWideBackground(Canvas c) {
        paint.setShader(skyShader);
        c.drawRect(0, 0, w, h * 0.62f, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF7BC96F);
        c.drawRect(0, h * 0.58f, w, h, paint);
    }

    private void drawBackground(Canvas c) {
        paint.setStyle(Paint.Style.FILL);

        // Sky
        paint.setShader(skyShader);
        c.drawRect(0, 0, w, h * 0.55f, paint);
        paint.setShader(null);

        // Sun with glow
        paint.setColor(0x55FFE9A8);
        c.drawCircle(w / 2f, h * 0.285f, w * 0.30f, paint);
        paint.setColor(0xFFFFD27A);
        c.drawCircle(w / 2f, h * 0.285f, w * 0.20f, paint);

        // Mountain
        path.reset();
        path.moveTo(-w * 0.1f, h * 0.47f);
        path.lineTo(w * 0.28f, h * 0.31f);
        path.lineTo(w * 0.72f, h * 0.47f);
        path.close();
        paint.setColor(0xFF8EA6D8);
        c.drawPath(path, paint);

        // Snow cap
        path.reset();
        path.moveTo(w * 0.28f, h * 0.31f);
        path.lineTo(w * 0.21f, h * 0.336f);
        path.lineTo(w * 0.245f, h * 0.329f);
        path.lineTo(w * 0.28f, h * 0.342f);
        path.lineTo(w * 0.315f, h * 0.329f);
        path.lineTo(w * 0.35f, h * 0.336f);
        path.close();
        paint.setColor(0xFFFFFFFF);
        c.drawPath(path, paint);

        // Hill and grass
        paint.setColor(0xFF8FD67F);
        tmp.set(-w * 0.2f, h * 0.43f, w * 1.2f, h * 0.56f);
        c.drawOval(tmp, paint);
        paint.setColor(0xFF7BC96F);
        c.drawRect(0, h * 0.495f, w, h, paint);

        // Torii gate
        float cx = w / 2f;
        float pillarW = w * 0.036f;
        float ground = h * 0.47f;
        paint.setColor(0xFFD9392B);
        tmp.set(cx - w * 0.2f - pillarW / 2f, h * 0.24f, cx - w * 0.2f + pillarW / 2f, ground);
        c.drawRect(tmp, paint);
        tmp.set(cx + w * 0.2f - pillarW / 2f, h * 0.24f, cx + w * 0.2f + pillarW / 2f, ground);
        c.drawRect(tmp, paint);
        // Lower beam
        tmp.set(cx - w * 0.23f, h * 0.28f, cx + w * 0.23f, h * 0.295f);
        c.drawRect(tmp, paint);
        // Upper beam
        tmp.set(cx - w * 0.29f, h * 0.228f, cx + w * 0.29f, h * 0.242f);
        c.drawRect(tmp, paint);
        // Black top cap
        paint.setColor(0xFF3A2A2A);
        tmp.set(cx - w * 0.33f, h * 0.212f, cx + w * 0.33f, h * 0.228f);
        c.drawRoundRect(tmp, w * 0.01f, w * 0.01f, paint);
        // Pillar bases
        tmp.set(cx - w * 0.2f - pillarW * 0.8f, ground - h * 0.02f,
                cx - w * 0.2f + pillarW * 0.8f, ground);
        c.drawRect(tmp, paint);
        tmp.set(cx + w * 0.2f - pillarW * 0.8f, ground - h * 0.02f,
                cx + w * 0.2f + pillarW * 0.8f, ground);
        c.drawRect(tmp, paint);
    }

    /** A cherry blossom tree. dir = +1 leans in from the left edge, -1 from the right edge. */
    private void drawTree(Canvas c, float cx, int dir) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF7A4F28);
        tmp.set(cx - w * 0.02f, h * 0.28f, cx + w * 0.02f, h * 0.50f);
        c.drawRoundRect(tmp, w * 0.01f, w * 0.01f, paint);

        paint.setColor(0xFFFFB7C9);
        c.drawCircle(cx, h * 0.25f, w * 0.13f, paint);
        c.drawCircle(cx + dir * w * 0.12f, h * 0.285f, w * 0.10f, paint);
        c.drawCircle(cx + dir * w * 0.02f, h * 0.33f, w * 0.09f, paint);
        c.drawCircle(cx + dir * w * 0.14f, h * 0.33f, w * 0.08f, paint);

        paint.setColor(0xFFFFD3DE);
        c.drawCircle(cx - dir * w * 0.02f, h * 0.235f, w * 0.06f, paint);
        c.drawCircle(cx + dir * w * 0.11f, h * 0.27f, w * 0.045f, paint);
        c.drawCircle(cx + dir * w * 0.03f, h * 0.32f, w * 0.04f, paint);
    }

    private void drawLogo(Canvas c) {
        c.save();
        float pulse = 1f + 0.015f * (float) Math.sin(time * 2f);
        c.scale(pulse, pulse, logoRect.centerX(), logoRect.centerY());

        kit.woodBox(c, logoRect, logoRect.height() * 0.2f, UiKit.LACQ_BOT, UiKit.GOLD, UiKit.LACQ_TOP);

        // Gold studs in the corners, like the sign board in Critter Toss
        float sr = wu * 0.016f, sx = wu * 0.035f, sy = wu * 0.035f;
        paint.setStyle(Paint.Style.FILL);
        float[] px = {logoRect.left + sx, logoRect.right - sx, logoRect.left + sx, logoRect.right - sx};
        float[] py = {logoRect.top + sy, logoRect.top + sy, logoRect.bottom - sy, logoRect.bottom - sy};
        for (int i = 0; i < 4; i++) {
            paint.setColor(0xFFB8801E);
            c.drawCircle(px[i], py[i], sr * 1.25f, paint);
            paint.setColor(UiKit.GOLD);
            c.drawCircle(px[i], py[i], sr, paint);
        }

        kit.fitText(c, "SAKURA", logoRect.centerX(), logoRect.top + logoRect.height() * 0.33f,
                logoRect.height() * 0.38f, logoRect.width() * 0.82f, UiKit.TITLE_GOLD);
        kit.fitText(c, "BRAWL", logoRect.centerX(), logoRect.top + logoRect.height() * 0.73f,
                logoRect.height() * 0.42f, logoRect.width() * 0.5f, UiKit.TITLE_RED);
        c.restore();
    }

    private void drawPlayIcon(Canvas c, float cx, float cy, float s) {
        drawPlayIcon(c, cx, cy, s, DARK_BROWN);
    }

    private void drawPlayIcon(Canvas c, float cx, float cy, float s, int color) {
        path.reset();
        path.moveTo(cx - s * 0.5f, cy - s * 0.65f);
        path.lineTo(cx + s * 0.75f, cy);
        path.lineTo(cx - s * 0.5f, cy + s * 0.65f);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawPath(path, paint);
    }

    private void beginPress(Canvas c, int id, RectF r) {
        c.save();
        float s = 1f - 0.10f * anim[id];
        c.scale(s, s, r.centerX(), r.centerY());
    }

    private void endPress(Canvas c) {
        c.restore();
    }

    // ---- How to Play card ----
    private void drawHow(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x99000000);
        c.drawRect(0, 0, w, h, paint);

        kit.panel(c, howPanel, howPanel.width() * 0.06f);
        kit.woodBox(c, howHeader, howHeader.height() * 0.4f, UiKit.LACQ_BOT, UiKit.GOLD, UiKit.LACQ_TOP);
        kit.fitText(c, "HOW TO PLAY", howHeader.centerX(), howHeader.centerY(),
                howHeader.height() * 0.5f, howHeader.width() * 0.85f, UiKit.TITLE_TEXT);

        // Rule rows: icon on the left, text on the right
        float x0 = howPanel.left + hu * 0.7f;
        float x1 = howPanel.right - hu * 0.7f;
        measure.setTextSize(howTextSize);
        float lineH = howTextSize * 1.25f;
        for (int i = 0; i < 4; i++) {
            float cy = howRowY[i];
            float ix = x0 + hu * 0.75f;
            if (i == 0) {
                kit.face(c, ix, cy, hu * 0.42f, leftCapy.species);
            } else {
                drawBadge(c, ix, cy, hu * 0.6f, HOW_BADGE_COLOR[i], HOW_BADGE[i]);
            }
            String[] lines = howLines[i];
            float ty = cy - (lines.length - 1) * lineH / 2f;
            for (String line : lines) {
                kit.leftText(c, line, howTextX, ty, howTextSize, DARK_BROWN);
                ty += lineH;
            }
            if (i < 3) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0x22000000);
                float ly = cy + howRowH / 2f;
                c.drawRect(x0, ly - 1f, x1, ly + 1f, paint);
            }
        }

        // Difficulty picker, below the card
        beginPress(c, BTN_ARROW_L, howArrowL);
        drawArrowButton(c, howArrowL, -1);
        endPress(c);
        beginPress(c, BTN_ARROW_R, howArrowR);
        drawArrowButton(c, howArrowR, 1);
        endPress(c);
        drawDifficultyBox(c);

        String desc;
        if (difficulty == SettingsManager.EASY) desc = "Slower, gentler opponent.";
        else if (difficulty == SettingsManager.HARD) desc = "Fast, smart, tough opponent!";
        else desc = "A fair fight.";
        kit.fitText(c, desc, w / 2f, howDescY, hu * 0.5f, howPanel.width(), 0xFFFFFFFF);

        // Big vermilion PLAY with white text, then BACK
        beginPress(c, BTN_GO, howGo);
        kit.red(c, howGo, howGo.height() * 0.35f);
        kit.fitText(c, "PLAY", howGo.centerX() + howGo.height() * 0.28f, howGo.centerY(),
                howGo.height() * 0.45f, howGo.width() * 0.5f, 0xFFFFFFFF);
        drawPlayIcon(c, howGo.centerX() - howGo.height() * 0.68f, howGo.centerY(),
                howGo.height() * 0.26f, 0xFFFFFFFF);
        endPress(c);

        beginPress(c, BTN_BACK, howBack);
        kit.woodBox(c, howBack, howBack.height() * 0.35f, UiKit.LACQ_BOT, UiKit.GOLD, UiKit.LACQ_TOP);
        kit.fitText(c, "BACK", howBack.centerX(), howBack.centerY(),
                howBack.height() * 0.45f, howBack.width() * 0.7f, CREAM);
        endPress(c);
    }

    /** A round colored badge with a short label (TAP, 30s, 4/7). */
    private void drawBadge(Canvas c, float cx, float cy, float r, int color, String label) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x33000000);
        c.drawCircle(cx, cy + r * 0.08f, r, paint);
        paint.setColor(0xFFFFFFFF);
        c.drawCircle(cx, cy, r, paint);
        paint.setColor(color);
        c.drawCircle(cx, cy, r * 0.88f, paint);
        kit.fitText(c, label, cx, cy, r * 0.8f, r * 1.4f, 0xFFFFFFFF);
    }

    /** Round yellow arrow button. dir = -1 points left, +1 points right. */
    private void drawArrowButton(Canvas c, RectF r, int dir) {
        kit.woodBox(c, r, r.height() * 0.5f, UiKit.YEL_BOT, UiKit.YEL_EDGE, UiKit.YEL_TOP);
        float s = r.height() * 0.24f;
        float cx = r.centerX();
        float cy = r.centerY();
        path.reset();
        path.moveTo(cx - dir * s * 0.8f, cy - s * 1.1f);   // flat side
        path.lineTo(cx + dir * s * 0.9f, cy);              // tip, in the direction of the arrow
        path.lineTo(cx - dir * s * 0.8f, cy + s * 1.1f);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(DARK_BROWN);
        c.drawPath(path, paint);
    }

    /** The middle pill of the picker: stars + EASY / NORMAL / HARD, colored by level. */
    private void drawDifficultyBox(Canvas c) {
        String name;
        int base, border, light, textColor;
        if (difficulty == SettingsManager.EASY) {
            name = "EASY";
            base = UiKit.GRN_BOT; border = UiKit.GRN_EDGE; light = UiKit.GRN_TOP; textColor = 0xFFFFFFFF;
        } else if (difficulty == SettingsManager.HARD) {
            name = "HARD";
            base = UiKit.RED_BOT; border = UiKit.GOLD; light = UiKit.RED_TOP; textColor = 0xFFFFFFFF;
        } else {
            name = "NORMAL";
            base = UiKit.YEL_BOT; border = UiKit.YEL_EDGE; light = UiKit.YEL_TOP; textColor = DARK_BROWN;
        }

        c.save();
        float sc = 1f - 0.15f * diffPop;   // small pop when the level changes
        c.scale(sc, sc, howDiff.centerX(), howDiff.centerY());
        kit.woodBox(c, howDiff, howDiff.height() * 0.35f, base, border, light);

        // stars + name, centered together in the pill
        int n = difficulty + 1;
        float nameSize = howDiff.height() * 0.42f;
        measure.setTextSize(nameSize);
        float nameW = measure.measureText(name);
        float pitch = hu * 0.62f;
        float starsW = n * pitch;
        float gap = hu * 0.45f;
        float startX = howDiff.centerX() - (starsW + gap + nameW) / 2f;
        for (int i = 0; i < n; i++) {
            drawStar(c, startX + (i + 0.5f) * pitch, howDiff.centerY(), hu * 0.3f, textColor);
        }
        kit.fitText(c, name, startX + starsW + gap + nameW / 2f, howDiff.centerY(),
                nameSize, nameW * 1.1f, textColor);
        c.restore();
    }

    private void drawStar(Canvas c, float cx, float cy, float r, int color) {
        path.reset();
        for (int i = 0; i < 10; i++) {
            float rad = (i % 2 == 0) ? r : r * 0.45f;
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float px = cx + (float) Math.cos(a) * rad;
            float py = cy + (float) Math.sin(a) * rad;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawPath(path, paint);
    }
}