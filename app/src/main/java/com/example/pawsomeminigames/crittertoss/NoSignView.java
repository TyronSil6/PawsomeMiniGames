package com.example.pawsomeminigames.crittertoss;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** A red "forbidden" ring with a slash, drawn over an icon (used in the How-to-play window). */
public class NoSignView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public NoSignView(Context c) { super(c); init(); }
    public NoSignView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas c) {
        float size = Math.min(getWidth(), getHeight());
        float stroke = size * .09f;
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = size / 2f - stroke * 1.2f;
        float d = r * .7071f;
        // white outline first so the sign reads on any picture, then the red sign on top
        p.setColor(0xFFFFFFFF);
        p.setStrokeWidth(stroke * 1.9f);
        c.drawCircle(cx, cy, r, p);
        c.drawLine(cx - d, cy - d, cx + d, cy + d, p);
        p.setColor(0xFFE53935);
        p.setStrokeWidth(stroke);
        c.drawCircle(cx, cy, r, p);
        c.drawLine(cx - d, cy - d, cx + d, cy + d, p);
    }
}