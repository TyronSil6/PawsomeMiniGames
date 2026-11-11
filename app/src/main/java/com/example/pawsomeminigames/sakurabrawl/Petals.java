package com.example.pawsomeminigames.sakurabrawl;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.Random;

/** Falling cherry blossom petals. Used on the menu and in the arena. */
public class Petals {

    private final int count;
    private final float w, h;
    private final float unit;   // petal size scale (the portrait width), so they stay small in landscape
    private final float[] x, y, size, speed, drift, phase, rot, rotSpeed;
    private final Random random = new Random(3);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float time = 0f;

    public Petals(float w, float h, int count) {
        this(w, h, count, w);
    }

    public Petals(float w, float h, int count, float unit) {
        this.w = w;
        this.h = h;
        this.unit = unit;
        this.count = count;
        x = new float[count];
        y = new float[count];
        size = new float[count];
        speed = new float[count];
        drift = new float[count];
        phase = new float[count];
        rot = new float[count];
        rotSpeed = new float[count];
        for (int i = 0; i < count; i++) {
            spawn(i, true);
        }
    }

    private void spawn(int i, boolean anywhere) {
        x[i] = random.nextFloat() * w;
        y[i] = anywhere ? random.nextFloat() * h : -unit * 0.05f;
        size[i] = unit * (0.012f + random.nextFloat() * 0.012f);
        speed[i] = h * (0.05f + random.nextFloat() * 0.06f);
        drift[i] = unit * (0.02f + random.nextFloat() * 0.04f);
        phase[i] = random.nextFloat() * 6.28f;
        rot[i] = random.nextFloat() * 360f;
        rotSpeed[i] = (random.nextFloat() - 0.5f) * 180f;
    }

    public void update(float dt) {
        time += dt;
        for (int i = 0; i < count; i++) {
            y[i] += speed[i] * dt;
            x[i] += (drift[i] + (float) Math.sin(time * 1.5f + phase[i]) * unit * 0.05f) * dt;
            rot[i] += rotSpeed[i] * dt;
            if (x[i] > w + size[i] * 2f) x[i] = -size[i] * 2f;
            if (y[i] > h + size[i] * 2f) spawn(i, false);
        }
    }

    public void draw(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < count; i++) {
            canvas.save();
            canvas.translate(x[i], y[i]);
            canvas.rotate(rot[i]);
            rect.set(-size[i], -size[i] * 0.6f, size[i], size[i] * 0.6f);
            paint.setColor(0xCCFFB7C9);
            canvas.drawOval(rect, paint);
            rect.set(-size[i] * 0.5f, -size[i] * 0.3f, size[i] * 0.4f, size[i] * 0.2f);
            paint.setColor(0x66FFFFFF);
            canvas.drawOval(rect, paint);
            canvas.restore();
        }
    }
}