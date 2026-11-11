package com.example.pawsomeminigames;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.Gravity;
import android.widget.SeekBar;

/**
 * Builds all button / panel looks in code, so no drawable XML files are needed.
 * Shared by the hub and both games so every screen uses the same Critter Toss look.
 */
public final class Ui {
    private Ui() {}

    private static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Width for popup dialogs: the full window on phones, capped at 460dp on tablets / wide windows. */
    public static int dialogWidth(Context c) {
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        return Math.min(dm.widthPixels, dp(c, 460));
    }

    private static GradientDrawable box(Context c, String top, String bottom, String stroke, float strokeDp, float radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor(top), Color.parseColor(bottom)});
        g.setStroke(dp(c, strokeDp), Color.parseColor(stroke));
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static Drawable yellow(Context c) { return box(c, "#FFD764", "#FFBA2A", "#C8861E", 4, 26); }
    public static Drawable green(Context c)  { return box(c, "#93E276", "#5BBF47", "#3E8F36", 4, 26); }
    public static Drawable chip(Context c)   { return box(c, "#F7E9C8", "#E6D0A0", "#C99A5B", 4, 20); }
    /** Cream game card with the same gold edge as the title sign. */
    public static Drawable card(Context c)  { return box(c, "#F7E9C8", "#E6D0A0", "#FFC957", 4, 20); }
    public static Drawable red(Context c)    { return box(c, "#F25A3C", "#C8341E", "#FFC957", 4, 26); }
    /** Black lacquer with a gold edge, like the shrine's sign board. */
    public static Drawable wood(Context c)   { return box(c, "#3E343C", "#211B21", "#FFC957", 4, 22); }
    /**
     * Exact copy of Sakura Brawl's logo board (UiKit.woodBox): the gold edge is 7% of the board's
     * height, the corner radius is 20% of the height, plus the soft shadow underneath.
     */
    public static Drawable banner(Context c) { return new BoardDrawable(); }

    private static final class BoardDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF outer = new RectF(), inner = new RectF(), shadow = new RectF();
        private float radius, innerRadius, b;
        private Shader fill;

        @Override protected void onBoundsChange(Rect r) {
            float h = r.height();
            b = Math.max(2f, h * 0.07f);
            radius = h * 0.2f;
            innerRadius = Math.max(1f, radius - b * 0.6f);
            outer.set(r);
            shadow.set(r.left, r.top + b * 0.8f, r.right, r.bottom + b * 0.8f);
            inner.set(r.left + b, r.top + b, r.right - b, r.bottom - b);
            fill = new LinearGradient(0f, inner.top, 0f, inner.bottom,
                    Color.parseColor("#3E343C"), Color.parseColor("#211B21"), Shader.TileMode.CLAMP);
        }

        @Override public void draw(Canvas cv) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x44000000);
            cv.drawRoundRect(shadow, radius, radius, paint);
            paint.setColor(Color.parseColor("#FFC957"));
            cv.drawRoundRect(outer, radius, radius, paint);
            paint.setShader(fill);
            cv.drawRoundRect(inner, innerRadius, innerRadius, paint);
            paint.setShader(null);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter cf) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** Pop Mania's leaderboard card: gold-tan frame with a cream inside. */
    public static Drawable pmPanel(Context c) {
        GradientDrawable outer = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#D6AE6E"), Color.parseColor("#B98846")});
        outer.setStroke(dp(c, 2), Color.parseColor("#A27234"));
        outer.setCornerRadius(dp(c, 30));
        GradientDrawable inner = new GradientDrawable();
        inner.setColor(Color.parseColor("#FFF6E2"));
        inner.setCornerRadius(dp(c, 22));
        LayerDrawable ld = new LayerDrawable(new Drawable[]{outer, inner});
        int m = dp(c, 9);
        ld.setLayerInset(1, m, m, m, m);
        return ld;
    }

    /** Pop Mania's gold button (leaderboard top row). */
    public static Drawable pmGold(Context c) { return box(c, "#FFE070", "#F5B52A", "#E39B14", 3, 20); }

    /** Pop Mania's dark button with the gold edge (Close / Quit). */
    public static Drawable pmDark(Context c) { return box(c, "#40373F", "#1E181F", "#FFC93D", 3, 32); }

    public static Drawable tab(Context c)    { return box(c, "#3E343C", "#211B21", "#FFC957", 4, 28); }

    public static Drawable dot(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(Color.parseColor("#FFC957"));
        g.setStroke(dp(c, 2), Color.parseColor("#B8801E"));
        return g;
    }

    /** The panel card used everywhere: Pop Mania's gold-tan frame with a cream inside. */
    public static Drawable panel(Context c) { return pmPanel(c); }

    /** Yellow slider with a round cream knob. */
    public static void styleSeek(Context c, SeekBar bar) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#EAD9B0"));
        bg.setCornerRadius(dp(c, 8));
        bg.setSize(0, dp(c, 14));
        GradientDrawable fill = new GradientDrawable();
        fill.setColor(Color.parseColor("#FFC531"));
        fill.setCornerRadius(dp(c, 8));
        fill.setSize(0, dp(c, 14));
        ClipDrawable clip = new ClipDrawable(fill, Gravity.START, ClipDrawable.HORIZONTAL);
        LayerDrawable track = new LayerDrawable(new Drawable[]{bg, clip});
        track.setId(0, android.R.id.background);
        track.setId(1, android.R.id.progress);
        bar.setProgressDrawable(track);

        GradientDrawable knob = new GradientDrawable();
        knob.setShape(GradientDrawable.OVAL);
        knob.setColor(Color.parseColor("#FFF8E3"));
        knob.setStroke(dp(c, 4), Color.parseColor("#E0A21C"));
        knob.setSize(dp(c, 32), dp(c, 32));
        bar.setThumb(knob);
    }

    /**
     * Sakura Brawl's logo "breathing": the sign slowly swells and shrinks by 1.5%
     * (scale = 1 + 0.015 * sin(2t)). Call start() in onResume and cancel() in onPause.
     */
    public static ValueAnimator pulse(final View v) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, (float) (2 * Math.PI));
        a.setDuration(3142);                       // one full wave = pi seconds, same as Sakura Brawl
        a.setInterpolator(new LinearInterpolator());
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.addUpdateListener(an -> {
            float s = 1f + 0.015f * (float) Math.sin((float) an.getAnimatedValue());
            v.setScaleX(s);
            v.setScaleY(s);
        });
        return a;
    }
}