package com.example.pawsomeminigames.sakurabrawl;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Draws the HUD and pause menu, and knows where the buttons are. */
public class GameUI {

    public static final int BTN_NONE = 0;
    public static final int BTN_PAUSE = 1;
    public static final int BTN_RESUME = 2;
    public static final int BTN_RESTART = 3;
    public static final int BTN_MENU = 4;
    public static final int BTN_END_MENU = 7; // "MAIN MENU" button on the end-of-match screen

    private static final int CREAM = UiKit.BTN_TEXT;   // text on lacquer buttons
    private static final int DARK_BROWN = UiKit.INK;   // text on yellow buttons

    private final float w, h;
    private final float u;   // size unit: the screen width in portrait, a portrait-like width in landscape
    private final UiKit kit = new UiKit();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tmp = new RectF();

    private final RectF pauseRect = new RectF();
    private final RectF roundRect = new RectF();
    private final RectF timerRect = new RectF();
    private final RectF p1Rect = new RectF();
    private final RectF p2Rect = new RectF();
    private final RectF panelRect = new RectF();
    private final RectF headerRect = new RectF();
    private final RectF resumeRect = new RectF();
    private final RectF restartRect = new RectF();
    private final RectF menuRect = new RectF();
    private final RectF endMenuRect = new RectF();
    private int pressed = BTN_NONE;
    private final float[] anim = new float[8];   // press animation per button, 0..1

    private int p1Species = Species.CAPYBARA, p2Species = Species.RACCOON;

    private float p1Bar = 1f, p2Bar = 1f;
    private float p1Target = 1f, p2Target = 1f;

    public GameUI(float w, float h) {
        this.w = w;
        this.h = h;
        this.u = Math.min(w, h);

        // Top row
        float m = u * 0.03f;
        float top = h * 0.02f;
        float bar = u * 0.115f;
        pauseRect.set(m, top, m + bar, top + bar);
        roundRect.set(w / 2f - u * 0.19f, top, w / 2f + u * 0.19f, top + bar);
        timerRect.set(w - m - u * 0.24f, top, w - m, top + bar);

        // Bottom row
        float pillH = u * 0.13f;
        float pillTop = h * 0.97f - pillH;
        p1Rect.set(m, pillTop, m + u * 0.46f, pillTop + pillH);
        p2Rect.set(w - m - u * 0.46f, pillTop, w - m, pillTop + pillH);

        // End-of-match "MAIN MENU" button
        float ew = u * 0.56f;
        float eh = u * 0.13f;
        endMenuRect.set(w / 2f - ew / 2f, h * 0.72f, w / 2f + ew / 2f, h * 0.72f + eh);

        // Pause menu
        float pw = u * 0.74f;
        float ph = pw * 0.92f;   // three buttons (RESUME / RESTART / MAIN MENU), same margins as before
        panelRect.set((w - pw) / 2f, h * 0.5f - ph / 2f, (w + pw) / 2f, h * 0.5f + ph / 2f);
        float hh = pw * 0.2f;
        float hw = pw * 0.7f;
        headerRect.set(w / 2f - hw / 2f, panelRect.top - hh * 0.45f,
                w / 2f + hw / 2f, panelRect.top + hh * 0.55f);
        float bw = pw * 0.74f;
        float bh = pw * 0.16f;
        float gap = pw * 0.04f;
        float bTop = panelRect.top + pw * 0.224f;
        resumeRect.set(w / 2f - bw / 2f, bTop, w / 2f + bw / 2f, bTop + bh);
        restartRect.set(resumeRect.left, resumeRect.bottom + gap,
                resumeRect.right, resumeRect.bottom + gap + bh);
        menuRect.set(resumeRect.left, restartRect.bottom + gap,
                resumeRect.right, restartRect.bottom + gap + bh);
    }

    // ---------------------------------------------------------------- state

    /** Values 0..1: how safe each capybara is in the ring. */
    public void setBalance(float p1, float p2) {
        p1Target = clamp01(p1);
        p2Target = clamp01(p2);
    }

    /** Which animal face to show on each player pill. */
    public void setSpecies(int p1, int p2) {
        p1Species = p1;
        p2Species = p2;
    }

    public void update(float dt) {
        float k = Math.min(1f, dt * 18f);
        for (int i = 0; i < anim.length; i++) {
            float target = (i == pressed) ? 1f : 0f;
            anim[i] += (target - anim[i]) * k;
        }
        float b = Math.min(1f, dt * 8f);
        p1Bar += (p1Target - p1Bar) * b;
        p2Bar += (p2Target - p2Bar) * b;
    }

    public void setPressed(int id) { pressed = id; }
    public int getPressed() { return pressed; }

    // ------------------------------------------------------------- hit tests

    public boolean isPauseButton(float x, float y) {
        tmp.set(pauseRect);
        tmp.inset(-u * 0.02f, -u * 0.02f);
        return tmp.contains(x, y);
    }

    /** True if the touch is on the end-of-match MAIN MENU button. */
    public boolean isEndMenuButton(float x, float y) {
        return endMenuRect.contains(x, y);
    }

    /** Returns the pause-menu button at a touch point, or BTN_NONE. */
    public int menuButtonAt(float x, float y) {
        if (resumeRect.contains(x, y)) return BTN_RESUME;
        if (restartRect.contains(x, y)) return BTN_RESTART;
        if (menuRect.contains(x, y)) return BTN_MENU;
        return BTN_NONE;
    }

    // --------------------------------------------------------------- drawing

    public void drawHud(Canvas c, String roundLabel, int timeSec,
                        int p1Wins, int p2Wins, int totalRounds, boolean showPause) {

        // Pause button
        if (showPause) {
            beginPress(c, BTN_PAUSE, pauseRect);
            kit.red(c, pauseRect, pauseRect.width() * 0.28f);
            float cx = pauseRect.centerX();
            float cy = pauseRect.centerY();
            float bw = pauseRect.width() * 0.13f;
            float bh = pauseRect.height() * 0.42f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFFFFFF);
            tmp.set(cx - bw * 1.6f, cy - bh / 2f, cx - bw * 0.6f, cy + bh / 2f);
            c.drawRoundRect(tmp, bw * 0.4f, bw * 0.4f, paint);
            tmp.set(cx + bw * 0.6f, cy - bh / 2f, cx + bw * 1.6f, cy + bh / 2f);
            c.drawRoundRect(tmp, bw * 0.4f, bw * 0.4f, paint);
            endPress(c);
        }

        // Round pill
        kit.woodBox(c, roundRect, roundRect.height() * 0.5f, UiKit.LACQ_BOT, UiKit.GOLD, UiKit.LACQ_TOP);
        kit.fitText(c, roundLabel, roundRect.centerX(), roundRect.centerY(),
                roundRect.height() * 0.42f, roundRect.width() * 0.86f, UiKit.TITLE_TEXT);

        // Timer pill with clock
        kit.woodBox(c, timerRect, timerRect.height() * 0.5f, UiKit.LACQ_BOT, UiKit.GOLD, UiKit.LACQ_TOP);
        float ph = timerRect.height();
        float clockX = timerRect.left + ph * 0.5f;
        float clockY = timerRect.centerY();
        float clockR = ph * 0.34f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFD9534F);
        c.drawCircle(clockX, clockY, clockR, paint);
        paint.setColor(CREAM);
        c.drawCircle(clockX, clockY, clockR * 0.78f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(clockR * 0.14f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(DARK_BROWN);
        c.drawLine(clockX, clockY, clockX, clockY - clockR * 0.5f, paint);
        c.drawLine(clockX, clockY, clockX + clockR * 0.35f, clockY, paint);
        paint.setStyle(Paint.Style.FILL);
        float textCx = (timerRect.left + ph * 0.95f + timerRect.right - ph * 0.15f) / 2f;
        int timeColor = timeSec <= 5 ? 0xFFFF8A80 : UiKit.TITLE_GOLD;
        kit.fitText(c, String.valueOf(timeSec), textCx, timerRect.centerY(),
                ph * 0.5f, ph * 1.2f, timeColor);

        // Player pills
        drawPlayer(c, p1Rect, true, "P1", p1Species, p1Bar, p1Wins, totalRounds,
                0xFF2F7DFF, UiKit.GOLD, 0xFF5B9DFF, 0xFF9AD0FF);
        drawPlayer(c, p2Rect, false, "P2", p2Species, p2Bar, p2Wins, totalRounds,
                0xFFE8433A, UiKit.GOLD, 0xFFFF6F66, 0xFFFFA39B);
    }

    private void drawPlayer(Canvas c, RectF r, boolean left, String label, int species, float bar,
                            int wins, int totalRounds,
                            int base, int border, int light, int fill) {
        float ph = r.height();
        kit.woodBox(c, r, ph * 0.5f, base, border, light);

        // Capybara face on the outer end
        float faceX = left ? r.left + ph * 0.5f : r.right - ph * 0.5f;
        kit.face(c, faceX, r.centerY(), ph * 0.40f, species);

        // Label and bar
        float x0 = left ? r.left + ph * 1.1f : r.left + ph * 0.35f;
        float x1 = left ? r.right - ph * 0.35f : r.right - ph * 1.1f;
        kit.fitText(c, label, (x0 + x1) / 2f, r.top + ph * 0.32f, ph * 0.32f,
                (x1 - x0), 0xFFFFFFFF);

        float barTop = r.top + ph * 0.52f;
        float barH = ph * 0.26f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x66000000);
        tmp.set(x0, barTop, x1, barTop + barH);
        c.drawRoundRect(tmp, barH / 2f, barH / 2f, paint);

        float fw = (x1 - x0) * bar;
        if (fw > barH) {
            paint.setColor(fill);
            if (left) tmp.set(x0, barTop, x0 + fw, barTop + barH);
            else tmp.set(x1 - fw, barTop, x1, barTop + barH);
            c.drawRoundRect(tmp, barH / 2f, barH / 2f, paint);
        } else if (fw > 0f) {
            paint.setColor(fill);
            float cx = left ? x0 + barH / 2f : x1 - barH / 2f;
            c.drawCircle(cx, barTop + barH / 2f, barH / 2f * (fw / barH), paint);
        }

        // Round win dots above the pill
        float dotR = ph * 0.09f;
        for (int i = 0; i < totalRounds; i++) {
            float dx = left ? r.left + dotR * 1.5f + i * dotR * 2.8f
                    : r.right - dotR * 1.5f - i * dotR * 2.8f;
            float dy = r.top - dotR * 1.8f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF5A3A1E);
            c.drawCircle(dx, dy, dotR * 1.2f, paint);
            paint.setColor(i < wins ? 0xFFFFD84D : 0xFFE8D8B0);
            c.drawCircle(dx, dy, dotR, paint);
        }
    }

    /** The wooden MAIN MENU button shown on the end-of-match screen. */
    public void drawEndMenuButton(Canvas c) {
        beginPress(c, BTN_END_MENU, endMenuRect);
        kit.woodBox(c, endMenuRect, endMenuRect.height() * 0.35f, UiKit.LACQ_BOT, UiKit.GOLD, UiKit.LACQ_TOP);
        kit.fitText(c, "MAIN MENU", endMenuRect.centerX(), endMenuRect.centerY(),
                endMenuRect.height() * 0.45f, endMenuRect.width() * 0.8f, CREAM);
        endPress(c);
    }

    public void drawPauseMenu(Canvas c) {
        // Dim the game
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x99000000);
        c.drawRect(0, 0, w, h, paint);

        // Cream panel
        kit.panel(c, panelRect, panelRect.width() * 0.07f);

        // Wooden header
        kit.woodBox(c, headerRect, headerRect.height() * 0.4f, UiKit.LACQ_BOT, UiKit.GOLD, UiKit.LACQ_TOP);
        kit.fitText(c, "PAUSED", headerRect.centerX(), headerRect.centerY(),
                headerRect.height() * 0.5f, headerRect.width() * 0.8f, UiKit.TITLE_TEXT);

        // Resume button (yellow)
        beginPress(c, BTN_RESUME, resumeRect);
        kit.woodBox(c, resumeRect, resumeRect.height() * 0.35f, UiKit.YEL_BOT, UiKit.YEL_EDGE, UiKit.YEL_TOP);
        kit.fitText(c, "RESUME", resumeRect.centerX(), resumeRect.centerY(),
                resumeRect.height() * 0.45f, resumeRect.width() * 0.8f, DARK_BROWN);
        endPress(c);

        // Restart button (wood)
        beginPress(c, BTN_RESTART, restartRect);
        kit.woodBox(c, restartRect, restartRect.height() * 0.35f, UiKit.LACQ_BOT, UiKit.GOLD, UiKit.LACQ_TOP);
        kit.fitText(c, "RESTART", restartRect.centerX(), restartRect.centerY(),
                restartRect.height() * 0.45f, restartRect.width() * 0.8f, CREAM);
        endPress(c);

        // Main menu button (wood)
        beginPress(c, BTN_MENU, menuRect);
        kit.woodBox(c, menuRect, menuRect.height() * 0.35f, UiKit.LACQ_BOT, UiKit.GOLD, UiKit.LACQ_TOP);
        kit.fitText(c, "MAIN MENU", menuRect.centerX(), menuRect.centerY(),
                menuRect.height() * 0.45f, menuRect.width() * 0.8f, CREAM);
        endPress(c);
    }

    // --------------------------------------------------------------- helpers

    /** Shrinks a button a little while it is pressed. */
    private void beginPress(Canvas c, int id, RectF r) {
        c.save();
        float s = 1f - 0.10f * anim[id];
        c.scale(s, s, r.centerX(), r.centerY());
    }

    private void endPress(Canvas c) {
        c.restore();
    }

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}