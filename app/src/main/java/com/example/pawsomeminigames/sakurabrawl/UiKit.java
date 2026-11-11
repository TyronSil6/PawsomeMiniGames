package com.example.pawsomeminigames.sakurabrawl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/** Shared drawing helpers for the black-lacquer-and-gold UI (same look as Critter Toss). */
public class UiKit {


    // ---- Critter Toss palette (matches Ui.java in that game) ----
    public static final int GOLD = 0xFFFFC957;          // edge of lacquer boards
    public static final int LACQ_TOP = 0xFF3E343C, LACQ_BOT = 0xFF211B21;
    public static final int YEL_TOP = 0xFFFFD764, YEL_BOT = 0xFFFFBA2A, YEL_EDGE = 0xFFC8861E;
    public static final int GRN_TOP = 0xFF93E276, GRN_BOT = 0xFF5BBF47, GRN_EDGE = 0xFF3E8F36;
    public static final int RED_TOP = 0xFFF25A3C, RED_BOT = 0xFFC8341E;
    public static final int CHIP_TOP = 0xFFF7E9C8, CHIP_BOT = 0xFFE6D0A0, CHIP_EDGE = 0xFFC99A5B;
    public static final int TAN = 0xFFBC8F50, CREAM_PANEL = 0xFFFFF8E3;
    public static final int INK = 0xFF5A3519;           // dark text on light buttons / panels
    public static final int BTN_TEXT = 0xFFFFE3A0;      // text on lacquer buttons
    public static final int TITLE_TEXT = 0xFFFFF3D6;    // text on title tabs
    public static final int TITLE_GOLD = 0xFFFFD66B, TITLE_RED = 0xFFFF6A45;   // logo + numbers
    public static final int KNOB_RING = 0xFFE0A21C, TRACK = 0xFFEAD9B0, FILL = 0xFFFFC531;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF tmp = new RectF();
    private final Path path = new Path();

    public UiKit() {
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /**
     * A rounded button / board: soft shadow, a solid edge, and a top-to-bottom gradient inside.
     * Parameters keep their old meaning: base = bottom colour, border = edge, light = top colour.
     */
    public void woodBox(Canvas c, RectF r, float radius, int base, int border, int light) {
        float b = Math.max(2f, r.height() * 0.07f);
        paint.setStyle(Paint.Style.FILL);

        tmp.set(r.left, r.top + b * 0.8f, r.right, r.bottom + b * 0.8f);
        paint.setColor(0x44000000);
        c.drawRoundRect(tmp, radius, radius, paint);

        paint.setColor(border);
        c.drawRoundRect(r, radius, radius, paint);

        float inner = Math.max(1f, radius - b * 0.6f);
        tmp.set(r.left + b, r.top + b, r.right - b, r.bottom - b);
        paint.setShader(new LinearGradient(0f, tmp.top, 0f, tmp.bottom, light, base, Shader.TileMode.CLAMP));
        c.drawRoundRect(tmp, inner, inner, paint);
        paint.setShader(null);
    }

    public void lacquer(Canvas c, RectF r, float radius) { woodBox(c, r, radius, LACQ_BOT, GOLD, LACQ_TOP); }
    public void yellow(Canvas c, RectF r, float radius)  { woodBox(c, r, radius, YEL_BOT, YEL_EDGE, YEL_TOP); }
    public void green(Canvas c, RectF r, float radius)   { woodBox(c, r, radius, GRN_BOT, GRN_EDGE, GRN_TOP); }
    public void chip(Canvas c, RectF r, float radius)    { woodBox(c, r, radius, CHIP_BOT, CHIP_EDGE, CHIP_TOP); }
    public void red(Canvas c, RectF r, float radius)     { woodBox(c, r, radius, RED_BOT, GOLD, RED_TOP); }

    /** Pop Mania's panel card (gold-tan frame, cream inside), used for the settings / pause / how-to-play panels. */
    public void panel(Canvas c, RectF r, float radius) {
        paint.setStyle(Paint.Style.FILL);
        tmp.set(r.left, r.top + r.height() * 0.012f, r.right, r.bottom + r.height() * 0.012f);
        paint.setColor(0x44000000);
        c.drawRoundRect(tmp, radius, radius, paint);

        paint.setColor(0xFFA27234);
        c.drawRoundRect(r, radius, radius, paint);

        float e = Math.max(2f, Math.min(r.width(), r.height()) * 0.006f);
        tmp.set(r.left + e, r.top + e, r.right - e, r.bottom - e);
        paint.setShader(new LinearGradient(0f, tmp.top, 0f, tmp.bottom, 0xFFD6AE6E, 0xFFB98846, Shader.TileMode.CLAMP));
        c.drawRoundRect(tmp, radius - e * 0.5f, radius - e * 0.5f, paint);
        paint.setShader(null);

        float m = Math.min(r.width(), r.height()) * 0.035f;
        float inner = Math.max(2f, radius * 0.5f);
        tmp.set(r.left + m, r.top + m, r.right - m, r.bottom - m);
        paint.setColor(0xFFFFF6E2);
        c.drawRoundRect(tmp, inner, inner, paint);
    }

    /** ON = green with white text, OFF = black lacquer. */
    public void toggle(Canvas c, RectF r, boolean on) {
        if (on) green(c, r, r.height() * 0.5f);
        else lacquer(c, r, r.height() * 0.5f);
        fitText(c, on ? "ON" : "OFF", r.centerX(), r.centerY(),
                r.height() * 0.5f, r.width() * 0.7f, on ? 0xFFFFFFFF : TITLE_TEXT);
    }

    /** Yellow slider with a round cream knob. unit = the panel's size unit (knob radius = 0.35 * unit). */
    public void slider(Canvas c, RectF r, float value, boolean enabled, float unit) {
        float kr = unit * 0.35f;
        float cy = r.centerY();
        float th = unit * 0.30f;
        float left = r.left + kr;
        float right = r.right - kr;
        float kx = left + (right - left) * value;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(TRACK);
        tmp.set(left, cy - th / 2f, right, cy + th / 2f);
        c.drawRoundRect(tmp, th / 2f, th / 2f, paint);

        paint.setColor(enabled ? FILL : 0xFFB5A48A);
        tmp.set(left, cy - th / 2f, Math.max(left + th, kx), cy + th / 2f);
        c.drawRoundRect(tmp, th / 2f, th / 2f, paint);

        paint.setColor(0x33000000);
        c.drawCircle(kx, cy + kr * 0.08f, kr, paint);
        paint.setColor(KNOB_RING);
        c.drawCircle(kx, cy, kr, paint);
        paint.setColor(CREAM_PANEL);
        c.drawCircle(kx, cy, kr * 0.70f, paint);
    }

    /** Centered text that shrinks to fit maxWidth, with a soft dark shadow. */
    public void fitText(Canvas c, String s, float cx, float cy, float size,
                        float maxWidth, int color) {
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(size);
        float width = textPaint.measureText(s);
        if (width > maxWidth && width > 0f) {
            size *= maxWidth / width;
            textPaint.setTextSize(size);
        }
        float y = cy - (textPaint.ascent() + textPaint.descent()) / 2f;
        textPaint.setColor(0x66000000);
        c.drawText(s, cx + size * 0.04f, y + size * 0.06f, textPaint);
        textPaint.setColor(color);
        c.drawText(s, cx, y, textPaint);
    }

    /** Left-aligned plain text, vertically centered on cy. */
    public void leftText(Canvas c, String s, float x, float cy, float size, int color) {
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        float y = cy - (textPaint.ascent() + textPaint.descent()) / 2f;
        c.drawText(s, x, y, textPaint);
    }

    /** A small capybara face icon. */
    public void face(Canvas c, float cx, float cy, float r) {
        face(c, cx, cy, r, Species.CAPYBARA);
    }

    /** A small face icon for any of the animals. */
    public void face(Canvas c, float cx, float cy, float r, int species) {
        species = Species.safe(species);

        // Use the face picture if it exists (res/drawable/face_*.png)
        Bitmap pic = FaceIcons.get(species);
        if (pic != null) {
            float box = r * 2.4f;
            float scale = box / Math.max(pic.getWidth(), pic.getHeight());
            float hw = pic.getWidth() * scale / 2f;
            float hh = pic.getHeight() * scale / 2f;
            tmp.set(cx - hw, cy - hh, cx + hw, cy + hh);
            c.drawBitmap(pic, null, tmp, bitmapPaint);
            return;
        }

        int head = Species.HEAD[species];
        int dark = Species.DARK[species];
        paint.setStyle(Paint.Style.FILL);

        // Ears
        if (species == Species.FOX) {
            paint.setColor(dark);
            triangle(c, cx - r * 1.00f, cy - r * 0.35f, cx - r * 0.25f, cy - r * 0.80f,
                    cx - r * 0.80f, cy - r * 1.45f);
            triangle(c, cx + r * 1.00f, cy - r * 0.35f, cx + r * 0.25f, cy - r * 0.80f,
                    cx + r * 0.80f, cy - r * 1.45f);
        } else {
            paint.setColor(dark);
            c.drawCircle(cx - r * 0.62f, cy - r * 0.78f, r * 0.28f, paint);
            c.drawCircle(cx + r * 0.62f, cy - r * 0.78f, r * 0.28f, paint);
            // Soft inside of the ears
            paint.setColor(species == Species.RACCOON ? 0xFF2E2E36 : 0xFFE8A8A0);
            c.drawCircle(cx - r * 0.62f, cy - r * 0.76f, r * 0.15f, paint);
            c.drawCircle(cx + r * 0.62f, cy - r * 0.76f, r * 0.15f, paint);
        }

        // Outline and head
        paint.setColor(0xFF5A3A1E);
        c.drawCircle(cx, cy, r * 1.08f, paint);
        paint.setColor(head);
        c.drawCircle(cx, cy, r, paint);
        paint.setColor(0x40FFFFFF);                       // soft shine on top
        oval(c, cx - r * 0.08f, cy - r * 0.62f, r * 0.5f, r * 0.17f);

        // Face patches
        boolean raccoon = species == Species.RACCOON;
        if (raccoon) {
            paint.setColor(0xFFF0F0F0);                       // white muzzle
            oval(c, cx, cy + r * 0.38f, r * 0.42f, r * 0.30f);
            paint.setColor(0xFF2E2E36);                       // eye mask
            oval(c, cx - r * 0.38f, cy - r * 0.10f, r * 0.30f, r * 0.22f);
            oval(c, cx + r * 0.38f, cy - r * 0.10f, r * 0.30f, r * 0.22f);
        } else if (species == Species.FOX) {
            paint.setColor(0xFFFFF4E6);                       // white cheeks
            oval(c, cx - r * 0.50f, cy + r * 0.38f, r * 0.45f, r * 0.32f);
            oval(c, cx + r * 0.50f, cy + r * 0.38f, r * 0.45f, r * 0.32f);
        } else if (species == Species.SLOTH) {
            paint.setColor(0xFFE8DCC0);                       // pale face
            oval(c, cx, cy + r * 0.05f, r * 0.72f, r * 0.60f);
            paint.setColor(0xFF6A5538);                       // eye stripes
            oval(c, cx - r * 0.38f, cy - r * 0.10f, r * 0.26f, r * 0.15f);
            oval(c, cx + r * 0.38f, cy - r * 0.10f, r * 0.26f, r * 0.15f);
        }

        // Eyes
        if (raccoon) {
            paint.setColor(0xFFF2F2F2);
            c.drawCircle(cx - r * 0.38f, cy - r * 0.10f, r * 0.17f, paint);
            c.drawCircle(cx + r * 0.38f, cy - r * 0.10f, r * 0.17f, paint);
            paint.setColor(0xFF111111);
            c.drawCircle(cx - r * 0.38f, cy - r * 0.10f, r * 0.13f, paint);
            c.drawCircle(cx + r * 0.38f, cy - r * 0.10f, r * 0.13f, paint);
        } else {
            paint.setColor(0xFF2A180A);
            c.drawCircle(cx - r * 0.38f, cy - r * 0.10f, r * 0.13f, paint);
            c.drawCircle(cx + r * 0.38f, cy - r * 0.10f, r * 0.13f, paint);
        }
        // Shiny eyes
        paint.setColor(0xFFFFFFFF);
        c.drawCircle(cx - r * 0.38f + r * 0.05f, cy - r * 0.10f - r * 0.05f, r * 0.055f, paint);
        c.drawCircle(cx + r * 0.38f + r * 0.05f, cy - r * 0.10f - r * 0.05f, r * 0.055f, paint);

        // Blush
        paint.setColor(0x99FF8FA3);
        c.drawCircle(cx - r * 0.55f, cy + r * 0.22f, r * 0.16f, paint);
        c.drawCircle(cx + r * 0.55f, cy + r * 0.22f, r * 0.16f, paint);

        // Capybara buck teeth (under the nose)
        if (species == Species.CAPYBARA) {
            paint.setColor(0xFF5A3A1E);
            tmp.set(cx - r * 0.20f, cy + r * 0.24f, cx + r * 0.20f, cy + r * 0.62f);
            c.drawRoundRect(tmp, r * 0.06f, r * 0.06f, paint);
            paint.setColor(0xFFFFFFFF);
            tmp.set(cx - r * 0.17f, cy + r * 0.26f, cx - r * 0.015f, cy + r * 0.59f);
            c.drawRoundRect(tmp, r * 0.04f, r * 0.04f, paint);
            tmp.set(cx + r * 0.015f, cy + r * 0.26f, cx + r * 0.17f, cy + r * 0.59f);
            c.drawRoundRect(tmp, r * 0.04f, r * 0.04f, paint);
        }

        // Nose, with a tiny shine
        paint.setColor(Species.NOSE[species]);
        c.drawCircle(cx, cy + r * 0.18f, r * 0.14f, paint);
        paint.setColor(0xAAFFFFFF);
        c.drawCircle(cx - r * 0.05f, cy + r * 0.14f, r * 0.04f, paint);

        // Sloth smile
        if (species == Species.SLOTH) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(r * 0.07f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(0xFF4A2E18);
            tmp.set(cx - r * 0.25f, cy + r * 0.22f, cx + r * 0.25f, cy + r * 0.52f);
            c.drawArc(tmp, 20f, 140f, false, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void oval(Canvas c, float cx, float cy, float rx, float ry) {
        tmp.set(cx - rx, cy - ry, cx + rx, cy + ry);
        c.drawOval(tmp, paint);
    }

    private void triangle(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3) {
        path.reset();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.close();
        c.drawPath(path, paint);
    }
}