
package com.example.pawsomeminigames.popmania;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

public class HoleView extends View {

    public interface Listener {
        void onCritterHit(HoleView hole, CritterType type);
        void onCritterEscaped(HoleView hole, CritterType type);
    }

    private static final long RISE_MS = 220;
    private static final long HIDE_MS = 180;
    private static final long HIT_MS = 260;

    private static final float HITTABLE_RISE = 0.30f;

    // Bomb size adjustment
    private static final float BOMB_SIZE_MULTIPLIER = 1f;

    private static final SparseArray<Bitmap> SPRITES = new SparseArray<>();

    private static Bitmap loadSprite(Context context, int resId) {
        Bitmap bitmap = SPRITES.get(resId);

        if (bitmap == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;

            bitmap = BitmapFactory.decodeResource(
                    context.getResources(), resId, options
            );

            SPRITES.put(resId, bitmap);
        }

        return bitmap;
    }

    private final Paint spritePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final LightingColorFilter flashFilter =
            new LightingColorFilter(0xFFFFFFFF, 0x00606060);

    private final Path clipPath = new Path();

    private final RectF holeRect = new RectF();
    private final RectF rimRect = new RectF();
    private final RectF rimShadowRect = new RectF();
    private final RectF spriteRect = new RectF();

    private final OvershootInterpolator overshoot =
            new OvershootInterpolator(1.7f);

    private float centerX;
    private float holeCenterY;
    private float holeRadiusX;
    private float holeRadiusY;
    private float restBottom;

    private float spriteW;
    private float spriteH;

    private final float touchSlop;

    private CritterType critter;
    private Bitmap sprite;

    private float rise;
    private float squash;

    private boolean flash;
    private boolean hittable;

    private long stayMs;

    private ValueAnimator lifeAnim;
    private ValueAnimator hitAnim;

    private Listener listener;

    public HoleView(Context context) {
        this(context, null);
    }

    public HoleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        touchSlop = 10f *
                context.getResources().getDisplayMetrics().density;

        rimPaint.setColor(0xFFB8793C);
        rimShadowPaint.setColor(0xFF5A3216);

        lipPaint.setStyle(Paint.Style.STROKE);
        lipPaint.setColor(0xFFB8793C);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public boolean isOccupied() {
        return critter != null;
    }

    public float getTargetX() {
        return centerX;
    }

    public float getTargetY() {
        return restBottom - spriteH * 0.55f;
    }

    // --------------------------------------------------
    // Layout
    // --------------------------------------------------

    @Override
    protected void onSizeChanged(
            int w, int h, int oldw, int oldh) {

        super.onSizeChanged(w, h, oldw, oldh);

        centerX = w / 2f;
        holeCenterY = h * 0.64f;

        holeRadiusX = w * 0.40f;
        holeRadiusY = h * 0.135f;

        restBottom = holeCenterY + holeRadiusY * 0.9f;

        holeRect.set(
                centerX - holeRadiusX,
                holeCenterY - holeRadiusY,
                centerX + holeRadiusX,
                holeCenterY + holeRadiusY
        );

        rimRect.set(
                holeRect.left - w * 0.035f,
                holeRect.top - w * 0.025f,
                holeRect.right + w * 0.035f,
                holeRect.bottom + w * 0.03f
        );

        rimShadowRect.set(rimRect);
        rimShadowRect.offset(0, w * 0.025f);

        holePaint.setShader(new LinearGradient(
                0,
                holeRect.top,
                0,
                holeRect.bottom,
                0xFF120702,
                0xFF3C1E0B,
                Shader.TileMode.CLAMP
        ));

        lipPaint.setStrokeWidth(w * 0.03f);

        glowPaint.setShader(new RadialGradient(
                0,
                0,
                w * 0.62f,
                new int[]{0xCCFFE066, 0x00FFE066},
                null,
                Shader.TileMode.CLAMP
        ));

        Path above = new Path();
        above.addRect(
                0, 0, w, holeCenterY,
                Path.Direction.CW
        );

        Path opening = new Path();
        opening.addOval(holeRect, Path.Direction.CW);

        clipPath.reset();
        clipPath.op(above, opening, Path.Op.UNION);

        layoutSprite();
    }

    // --------------------------------------------------
    // SPRITE SIZE ADJUSTMENT
    // --------------------------------------------------

    private void layoutSprite() {

        if (sprite == null || getWidth() == 0 ||
                getHeight() == 0) {
            return;
        }

        float scale = (getWidth() * 0.80f)
                / sprite.getWidth();

        float maxHeight = getHeight() * 0.68f;

        if (sprite.getHeight() * scale > maxHeight) {
            scale = maxHeight / sprite.getHeight();
        }

        // Make ONLY the bomb bigger
        if (critter != null && critter.isBomb()) {

            float bombWidth = (holeRadiusX * 2f)
                    * BOMB_SIZE_MULTIPLIER;

            scale = bombWidth / sprite.getWidth();
        }

        spriteW = sprite.getWidth() * scale;
        spriteH = sprite.getHeight() * scale;
    }

    private void updateSpriteRect() {

        if (sprite == null) {
            spriteRect.setEmpty();
            return;
        }

        float bottom = restBottom +
                (1f - rise) *
                        (spriteH + holeRadiusY * 0.5f);

        spriteRect.set(
                centerX - spriteW / 2f,
                bottom - spriteH,
                centerX + spriteW / 2f,
                bottom
        );
    }

    // --------------------------------------------------
    // Drawing
    // --------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        canvas.drawOval(rimShadowRect, rimShadowPaint);
        canvas.drawOval(rimRect, rimPaint);
        canvas.drawOval(holeRect, holePaint);

        if (critter != null && sprite != null &&
                rise > 0.001f) {

            updateSpriteRect();

            canvas.save();
            canvas.clipPath(clipPath);

            if (critter.isGolden()) {

                glowPaint.setAlpha(
                        170 + (int) (
                                85 * Math.sin(
                                        System.currentTimeMillis() / 110.0
                                )
                        )
                );

                canvas.save();
                canvas.translate(
                        centerX,
                        spriteRect.centerY()
                );

                canvas.drawCircle(
                        0, 0,
                        getWidth() * 0.62f,
                        glowPaint
                );

                canvas.restore();
            }

            canvas.save();

            canvas.scale(
                    1f + 0.18f * squash,
                    1f - 0.30f * squash,
                    centerX,
                    spriteRect.bottom
            );

            spritePaint.setColorFilter(
                    flash ? flashFilter : null
            );

            canvas.drawBitmap(
                    sprite,
                    null,
                    spriteRect,
                    spritePaint
            );

            canvas.restore();
            canvas.restore();
        }

        canvas.drawArc(
                holeRect,
                0,
                180,
                false,
                lipPaint
        );
    }

    // --------------------------------------------------
    // Game Control
    // --------------------------------------------------

    public void popUp(CritterType type, long stayMs) {

        if (critter != null) {
            return;
        }

        this.critter = type;
        this.stayMs = stayMs;

        this.sprite = loadSprite(
                getContext(),
                type.drawableRes
        );

        this.rise = 0f;
        this.squash = 0f;
        this.flash = false;
        this.hittable = false;

        layoutSprite();

        final long total =
                RISE_MS + stayMs + HIDE_MS;

        lifeAnim = ValueAnimator.ofFloat(0f, 1f);

        lifeAnim.setDuration(total);
        lifeAnim.setInterpolator(
                new LinearInterpolator()
        );

        lifeAnim.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(
                            ValueAnimator animation) {

                        long t = (long) (
                                animation.getAnimatedFraction() * total
                        );

                        if (t < RISE_MS) {

                            rise = overshoot.getInterpolation(
                                    t / (float) RISE_MS
                            );

                        } else if (t < RISE_MS + HoleView.this.stayMs) {

                            rise = 1f;

                        } else {

                            float f = (
                                    t - RISE_MS - HoleView.this.stayMs
                            ) / (float) HIDE_MS;

                            rise = 1f - f * f;
                        }

                        hittable = rise > HITTABLE_RISE;

                        invalidate();
                    }
                });

        lifeAnim.addListener(new FinishListener() {

            @Override
            void onFinished() {

                CritterType escaped = critter;

                resetState();

                if (listener != null && escaped != null) {
                    listener.onCritterEscaped(
                            HoleView.this,
                            escaped
                    );
                }
            }
        });

        lifeAnim.start();
        invalidate();
    }

    public void pauseAnim() {

        if (lifeAnim != null &&
                lifeAnim.isStarted() &&
                !lifeAnim.isPaused()) {

            lifeAnim.pause();
        }

        if (hitAnim != null &&
                hitAnim.isStarted() &&
                !hitAnim.isPaused()) {

            hitAnim.pause();
        }
    }

    public void resumeAnim() {

        if (lifeAnim != null && lifeAnim.isPaused()) {
            lifeAnim.resume();
        }

        if (hitAnim != null && hitAnim.isPaused()) {
            hitAnim.resume();
        }
    }

    public void clear() {

        if (lifeAnim != null) {
            lifeAnim.cancel();
            lifeAnim = null;
        }

        if (hitAnim != null) {
            hitAnim.cancel();
            hitAnim = null;
        }

        resetState();
    }

    private void resetState() {

        critter = null;
        sprite = null;

        rise = 0f;
        squash = 0f;

        flash = false;
        hittable = false;

        invalidate();
    }

    // --------------------------------------------------
    // Touch Handling
    // --------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_POINTER_DOWN) {

            int index = event.getActionIndex();

            if (isOnCritter(
                    event.getX(index),
                    event.getY(index))) {

                hit();
            }

            return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean isOnCritter(float x, float y) {

        if (critter == null || !hittable) {
            return false;
        }

        updateSpriteRect();

        float visibleBottom =
                holeCenterY + holeRadiusY;

        RectF area = new RectF(spriteRect);

        area.inset(
                -touchSlop,
                -touchSlop
        );

        return area.contains(x, y) &&
                y <= visibleBottom;
    }

    private void hit() {

        final CritterType hitType = critter;

        hittable = false;

        if (lifeAnim != null) {
            lifeAnim.cancel();
            lifeAnim = null;
        }

        final float startRise = Math.min(rise, 1f);

        hitAnim = ValueAnimator.ofFloat(0f, 1f);

        hitAnim.setDuration(HIT_MS);

        hitAnim.setInterpolator(
                new LinearInterpolator()
        );

        hitAnim.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(
                            ValueAnimator animation) {

                        float f = animation.getAnimatedFraction();

                        rise = startRise *
                                (1f - f) * (1f - f);

                        squash = (float) Math.sin(Math.PI * f);

                        flash = f < 0.45f;

                        invalidate();
                    }
                });

        hitAnim.addListener(new FinishListener() {

            @Override
            void onFinished() {
                resetState();
            }
        });

        hitAnim.start();

        if (listener != null) {
            listener.onCritterHit(this, hitType);
        }
    }

    // --------------------------------------------------
    // Animation Listener
    // --------------------------------------------------

    private abstract static class FinishListener
            extends AnimatorListenerAdapter {

        private boolean cancelled;

        @Override
        public void onAnimationCancel(Animator animation) {
            cancelled = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {

            if (!cancelled) {
                onFinished();
            }
        }

        abstract void onFinished();
    }
}
