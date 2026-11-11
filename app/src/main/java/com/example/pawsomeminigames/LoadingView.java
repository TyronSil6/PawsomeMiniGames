package com.example.pawsomeminigames;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/**
 * The loading screen. The artwork fills the whole screen (only a little is trimmed at the left and
 * right on tall phones). Our own yellow bar fills up inside
 * the painted wooden bar; its position is stored in IMAGE pixels so it lines up on every phone.
 */
public class LoadingView extends View {
    // inside of the painted (empty) bar, in the 1024 x 1536 artwork
    private static final float TRACK_L = 282f, TRACK_T = 1311f, TRACK_R = 740f, TRACK_B = 1351f;

    private final Bitmap art;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);   // the yellow, always fully solid
    private final Paint overlay = new Paint(Paint.ANTI_ALIAS_FLAG);     // stripes + shine (see-through on purpose)
    private final RectF track = new RectF();
    private final RectF fill = new RectF();
    private final Path clip = new Path();
    private final Path fillClip = new Path();
    private final Path stripe = new Path();

    private Bitmap scene;                 // backdrop + sharp picture, built once per size
    private Shader fillShader;
    private float progress = 0f;          // 0..1
    private boolean running = true;

    public LoadingView(Context c) {
        super(c);
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        art = BitmapFactory.decodeResource(c.getResources(), R.drawable.bg_loading, o);
    }

    public void setProgress(float p) {
        progress = Math.max(0f, Math.min(1f, p));
        invalidate();
    }

    public void stopAnimating() { running = false; }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (art == null || w <= 0 || h <= 0) return;

        // Fill the whole screen with the picture (no blurred strips, no empty space).
        // On tall phones only the left/right edges are trimmed (the bar and animals stay in view);
        // on wide screens the picture is lined up with the bottom so the bar is never cut off.
        float bw = art.getWidth(), bh = art.getHeight();
        float s = Math.max(w / bw, h / bh);
        float ox = (w - bw * s) / 2f;
        float oy = h - bh * s;
        RectF dst = new RectF(ox, oy, ox + bw * s, oy + bh * s);

        scene = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        new Canvas(scene).drawBitmap(art, null, dst, paint);

        // the bar, mapped from image pixels to screen pixels
        track.set(ox + TRACK_L * s, oy + TRACK_T * s, ox + TRACK_R * s, oy + TRACK_B * s);
        fillShader = new LinearGradient(0, track.top, 0, track.bottom,
                new int[]{Color.parseColor("#FFE04A"), Color.parseColor("#FFC800"), Color.parseColor("#F5A300")},
                new float[]{0f, .45f, 1f}, Shader.TileMode.CLAMP);
        clip.reset();
        float r = track.height() / 2f;
        clip.addRoundRect(track, r, r, Path.Direction.CW);
    }

    @Override protected void onDraw(Canvas cv) {
        if (scene == null) return;
        cv.drawBitmap(scene, 0, 0, null);

        float r = track.height() / 2f;
        cv.save();
        cv.clipPath(clip);

        // filled part
        float fw = track.width() * progress;
        if (fw > 0f) {
            fill.set(track.left, track.top, track.left + Math.max(fw, r * 1.2f), track.bottom);
            fillPaint.setShader(fillShader);
            fillPaint.setAlpha(255);
            cv.drawRoundRect(fill, r, r, fillPaint);

            // sliding diagonal stripes, so the bar looks busy while it loads
            cv.save();
            fillClip.reset();
            fillClip.addRoundRect(fill, r, r, Path.Direction.CW);
            cv.clipPath(fillClip);
            overlay.setColor(0x55D97A00);
            float period = track.height() * 1.1f, sw = period * .45f;
            float shift = (SystemClock.uptimeMillis() % 900) / 900f * period;
            for (float x = fill.left - period * 2 + shift; x < fill.right + period; x += period) {
                stripe.reset();
                stripe.moveTo(x, fill.bottom);
                stripe.lineTo(x + sw, fill.bottom);
                stripe.lineTo(x + sw + track.height() * .6f, fill.top);
                stripe.lineTo(x + track.height() * .6f, fill.top);
                stripe.close();
                cv.drawPath(stripe, overlay);
            }
            // soft shine along the top
            overlay.setColor(0x2EFFFFFF);
            cv.drawRect(fill.left, fill.top + track.height() * .10f, fill.right, fill.top + track.height() * .22f, overlay);
            cv.restore();
        }
        cv.restore();

        if (running) postInvalidateOnAnimation();   // keeps the stripes moving
    }
}