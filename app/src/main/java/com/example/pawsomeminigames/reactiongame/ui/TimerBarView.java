package com.example.pawsomeminigames.reactiongame.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.pawsomeminigames.R;

public class TimerBarView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float progress = 1f;
    private final int ok, warn, danger, track;

    public TimerBarView(Context context) {
        this(context, null);
    }

    public TimerBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ok = ContextCompat.getColor(context, R.color.rg_timer_ok);
        warn = ContextCompat.getColor(context, R.color.rg_timer_warn);
        danger = ContextCompat.getColor(context, R.color.rg_timer_danger);
        track = ContextCompat.getColor(context, R.color.rg_timer_track);
    }

    public void setProgress(float p) {
        progress = Math.max(0f, Math.min(1f, p));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float d = getResources().getDisplayMetrics().density;
        float w = getWidth(), h = getHeight(), r = h / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF4A2A10);
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, r, r, paint);
        paint.setColor(track);
        rect.set(2 * d, 2 * d, w - 2 * d, h - 2 * d);
        canvas.drawRoundRect(rect, r, r, paint);
        if (progress > 0f) {
            int c = progress > 0.5f ? ok : (progress > 0.25f ? warn : danger);
            paint.setColor(c);
            float right = 2 * d + (w - 4 * d) * progress;
            rect.set(2 * d, 2 * d, Math.max(right, 2 * d + (h - 4 * d)), h - 2 * d);
            canvas.drawRoundRect(rect, r, r, paint);
            paint.setColor(0x44FFFFFF);
            rect.set(rect.left + 3 * d, rect.top + 1.5f * d, rect.right - 3 * d, rect.top + (h - 4 * d) * 0.42f);
            canvas.drawRoundRect(rect, r, r, paint);
        }
    }
}
