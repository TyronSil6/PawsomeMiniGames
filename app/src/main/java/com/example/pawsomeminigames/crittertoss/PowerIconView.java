package com.example.pawsomeminigames.crittertoss;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Draws a power-up orb (same look as in the game) for the How-to-play window. */
public class PowerIconView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private GameView.Power power = GameView.Power.FREEZE;

    public PowerIconView(Context c) { super(c); }
    public PowerIconView(Context c, AttributeSet a) { super(c, a); }

    public void setPower(GameView.Power pw) {
        power = pw;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f, cy = getHeight() / 2f, R = s * .38f;
        p.setStyle(Paint.Style.FILL);
        p.setColor((70 << 24) | (power.color & 0xFFFFFF));           // soft glow
        c.drawCircle(cx, cy, s * .5f, p);
        p.setColor(Color.rgb(255, 248, 225));                         // cream rim
        c.drawCircle(cx, cy, R + s * .04f, p);
        p.setColor(power.color);                                      // orb
        c.drawCircle(cx, cy, R, p);
        p.setColor(Color.argb(110, 255, 255, 255));                   // shine
        c.drawCircle(cx - R * .3f, cy - R * .35f, R * .28f, p);
        drawIcon(c, cx, cy, R * .62f);
    }

    private void drawIcon(Canvas c, float cx, float cy, float r) {
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, r * .22f));
        p.setStrokeCap(Paint.Cap.ROUND);
        switch (power) {
            case FREEZE:                                              // snowflake
                for (int k = 0; k < 3; k++) {
                    double a = k * Math.PI / 3;
                    float dx = (float) Math.cos(a) * r, dy = (float) Math.sin(a) * r;
                    c.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, p);
                }
                break;
            case BIG:                                                 // big target
                c.drawCircle(cx, cy, r, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy, r * .38f, p);
                break;
            case TRIPLE:                                              // three balls
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy - r * .55f, r * .34f, p);
                c.drawCircle(cx - r * .62f, cy + r * .45f, r * .34f, p);
                c.drawCircle(cx + r * .62f, cy + r * .45f, r * .34f, p);
                break;
            case BOMB:                                                // bomb with a fuse
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx - r * .1f, cy + r * .2f, r * .72f, p);
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx + r * .35f, cy - r * .35f, cx + r * .75f, cy - r * .85f, p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(Color.rgb(255, 230, 120));
                c.drawCircle(cx + r * .85f, cy - r * .95f, r * .22f, p);
                break;
        }
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStyle(Paint.Style.FILL);
    }
}