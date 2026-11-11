package com.example.pawsomeminigames.sakurabrawl;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.Random;

/** Visual "juice": dust puffs, impact stars and screen shake. No game rules in here. */
public class Effects {

    private static final int MAX = 100;
    private static final int DUST = 0;
    private static final int STAR = 1;

    private final float w;
    private final Random random = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    // Particle data (fixed arrays: no memory is created while the game runs)
    private final float[] x = new float[MAX];
    private final float[] y = new float[MAX];
    private final float[] vx = new float[MAX];
    private final float[] vy = new float[MAX];
    private final float[] life = new float[MAX];
    private final float[] maxLife = new float[MAX];
    private final float[] size = new float[MAX];
    private final float[] rot = new float[MAX];
    private final float[] spin = new float[MAX];
    private final int[] kind = new int[MAX];

    private float shake = 0f;   // 0..1, fades out by itself

    public Effects(float screenW) {
        this.w = screenW;
    }

    /** Removes all particles and stops any shake (used when a new round starts). */
    public void clear() {
        for (int i = 0; i < MAX; i++) life[i] = 0f;
        shake = 0f;
    }

    // ------------------------------------------------------------ spawning

    /** Puffs of dust. amount 0..1 sets how many and how big. */
    public void dust(float cx, float cy, float amount) {
        int n = 3 + (int) (7 * clamp01(amount));
        for (int k = 0; k < n; k++) {
            int i = free();
            if (i < 0) return;
            float a = random.nextFloat() * 6.2832f;
            float sp = w * (0.05f + 0.16f * random.nextFloat()) * (0.5f + amount);
            kind[i] = DUST;
            x[i] = cx + (random.nextFloat() - 0.5f) * w * 0.03f;
            y[i] = cy + (random.nextFloat() - 0.5f) * w * 0.015f;
            vx[i] = (float) Math.cos(a) * sp;
            vy[i] = (float) Math.sin(a) * sp * 0.5f - w * 0.03f;   // flatter, drifts up a bit
            size[i] = w * (0.012f + 0.016f * random.nextFloat()) * (0.7f + 0.6f * amount);
            life[i] = maxLife[i] = 0.35f + 0.3f * random.nextFloat();
        }
    }

    /** A burst of little sparkle stars. */
    public void stars(float cx, float cy, float amount) {
        int n = 2 + (int) (5 * clamp01(amount));
        for (int k = 0; k < n; k++) {
            int i = free();
            if (i < 0) return;
            float a = random.nextFloat() * 6.2832f;
            float sp = w * (0.25f + 0.35f * random.nextFloat());
            kind[i] = STAR;
            x[i] = cx;
            y[i] = cy;
            vx[i] = (float) Math.cos(a) * sp;
            vy[i] = (float) Math.sin(a) * sp - w * 0.15f;
            size[i] = w * (0.018f + 0.02f * random.nextFloat());
            rot[i] = random.nextFloat() * 90f;
            spin[i] = (random.nextFloat() - 0.5f) * 500f;
            life[i] = maxLife[i] = 0.45f + 0.35f * random.nextFloat();
        }
    }

    /** Shakes the screen. amount 0..1. A new shake never weakens a stronger one still running. */
    public void shake(float amount) {
        shake = Math.max(shake, clamp01(amount));
    }

    private int free() {
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0f) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------- update

    public void update(float dt) {
        float drag = (float) Math.exp(-3f * dt);
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0f) continue;
            life[i] -= dt;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            if (kind[i] == DUST) {
                vx[i] *= drag;
                vy[i] *= drag;
            } else {
                vy[i] += w * 1.4f * dt;      // stars fall back down
                vx[i] *= drag;
                rot[i] += spin[i] * dt;
            }
        }
    }

    /** The shake fades in real time, so call this with the real (unslowed) dt. */
    public void updateShake(float dt) {
        shake = Math.max(0f, shake - dt * 3f);
    }

    // ------------------------------------------------------------- drawing

    /** Call before drawing the scene. Always pair with endShake. */
    public void beginShake(Canvas c, float screenW, float screenH) {
        c.save();
        if (shake <= 0.001f) return;
        float mag = shake * shake * w * 0.014f;
        float ox = (random.nextFloat() * 2f - 1f) * mag;
        float oy = (random.nextFloat() * 2f - 1f) * mag;
        // A tiny zoom keeps the screen edges covered while the picture moves
        float s = 1f + 0.03f * shake;
        c.scale(s, s, screenW / 2f, screenH / 2f);
        c.translate(ox, oy);
    }

    public void endShake(Canvas c) {
        c.restore();
    }

    public void draw(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0f) continue;
            float p = 1f - life[i] / maxLife[i];       // 0 = just born, 1 = gone

            if (kind[i] == DUST) {
                float alpha = (1f - p) * 0.65f;
                paint.setColor(((int) (alpha * 255) << 24) | 0xFFF6E3);
                c.drawCircle(x[i], y[i], size[i] * (0.6f + 1.6f * p), paint);
            } else {
                float alpha = p < 0.7f ? 1f : (1f - p) / 0.3f;
                float s = size[i] * (1f - 0.4f * p);
                c.save();
                c.translate(x[i], y[i]);
                c.rotate(rot[i]);
                path.reset();
                path.moveTo(0, -s);
                path.lineTo(s * 0.28f, -s * 0.28f);
                path.lineTo(s, 0);
                path.lineTo(s * 0.28f, s * 0.28f);
                path.lineTo(0, s);
                path.lineTo(-s * 0.28f, s * 0.28f);
                path.lineTo(-s, 0);
                path.lineTo(-s * 0.28f, -s * 0.28f);
                path.close();
                paint.setColor(((int) (alpha * 255) << 24) | 0xFFE066);
                c.drawPath(path, paint);
                c.restore();
            }
        }
    }

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}