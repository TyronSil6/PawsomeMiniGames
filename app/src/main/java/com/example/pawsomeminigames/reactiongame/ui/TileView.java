package com.example.pawsomeminigames.reactiongame.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.pawsomeminigames.R;
import com.example.pawsomeminigames.reactiongame.game.Tile;

public class TileView extends FrameLayout {
    private final ImageView sprite;
    private final View glow;
    private final ImageView badge;

    private Tile tile;

    public TileView(Context context) {
        super(context);

        setClipChildren(false);
        setClipToPadding(false);
        setClickable(true);
        setFocusable(true);

        glow = new View(context);
        glow.setVisibility(GONE);
        glow.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        addView(glow, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        sprite = new ImageView(context);
        sprite.setScaleType(ImageView.ScaleType.FIT_CENTER);
        sprite.setVisibility(VISIBLE);
        sprite.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        addView(sprite, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        badge = new ImageView(context);
        badge.setVisibility(GONE);
        badge.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        LayoutParams bp = new LayoutParams(
                10, 10, Gravity.TOP | Gravity.END
        );

        addView(badge, bp);

        ButtonFx.attach(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int side = Math.round(w * 0.12f);

        sprite.setPadding(
                side,
                Math.round(w * 0.09f),
                side,
                Math.round(w * 0.17f)
        );

        LayoutParams bp = (LayoutParams) badge.getLayoutParams();

        int bs = Math.round(w * 0.34f);

        bp.width = bs;
        bp.height = bs;
        bp.topMargin = -Math.round(w * 0.06f);
        bp.setMarginEnd(-Math.round(w * 0.05f));

        badge.setLayoutParams(bp);

        setCameraDistance(
                8000f * getResources().getDisplayMetrics().density
        );
    }

    public Tile getTile() {
        return tile;
    }

    public void bind(Tile t) {
        tile = t;

        if (t == null) {
            sprite.setImageDrawable(null);
            return;
        }

        setBackgroundResource(AnimalAssets.tileFrame(t.color));

        sprite.setVisibility(VISIBLE);
        sprite.setAlpha(1f);
        sprite.setScaleType(ImageView.ScaleType.FIT_CENTER);

        int image = AnimalAssets.idle(t.animal);

        sprite.setImageResource(image);

        resetFeedback();

        sprite.bringToFront();
        badge.bringToFront();

        invalidate();
    }

    public void resetFeedback() {
        animate().cancel();

        setAlpha(1f);
        setScaleX(1f);
        setScaleY(1f);
        setRotationY(0f);
        setTranslationX(0f);

        glow.setVisibility(GONE);
        badge.setVisibility(GONE);

        if (tile != null) {
            sprite.setVisibility(VISIBLE);
            sprite.setImageResource(AnimalAssets.idle(tile.animal));
            sprite.bringToFront();
            badge.bringToFront();
        }
    }

    public void showCorrect() {
        if (tile == null) return;

        sprite.setImageResource(AnimalAssets.happy(tile.animal));

        glow.setBackgroundResource(R.drawable.rg_tile_glow_correct);
        glow.setVisibility(VISIBLE);

        badge.setImageResource(R.drawable.rg_badge_correct);
        badge.setVisibility(VISIBLE);

        sprite.bringToFront();
        badge.bringToFront();

        setScaleX(0.9f);
        setScaleY(0.9f);

        animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(190)
                .setInterpolator(new OvershootInterpolator(3f))
                .start();
    }

    public void showWrong() {
        if (tile == null) return;

        sprite.setImageResource(AnimalAssets.sad(tile.animal));

        glow.setBackgroundResource(R.drawable.rg_tile_glow_wrong);
        glow.setVisibility(VISIBLE);

        badge.setImageResource(R.drawable.rg_badge_wrong);
        badge.setVisibility(VISIBLE);

        sprite.bringToFront();
        badge.bringToFront();

        float d = getResources().getDisplayMetrics().density;

        ObjectAnimator shake = ObjectAnimator.ofFloat(
                this,
                View.TRANSLATION_X,
                0, -12 * d, 12 * d, -9 * d,
                9 * d, -4 * d, 4 * d, 0
        );

        shake.setDuration(380);
        shake.start();
    }

    public void showReveal() {
        glow.setBackgroundResource(R.drawable.rg_tile_glow_reveal);
        glow.setVisibility(VISIBLE);

        badge.setImageResource(R.drawable.rg_badge_correct);
        badge.setVisibility(VISIBLE);

        sprite.bringToFront();
        badge.bringToFront();

        animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(220)
                .setInterpolator(new OvershootInterpolator(2f))
                .start();
    }

    public void dim() {
        animate()
                .alpha(0.55f)
                .setDuration(180)
                .start();
    }

    public void popIn(long delayMs) {
        setAlpha(0f);
        setScaleX(0.85f);
        setScaleY(0.85f);

        animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(delayMs)
                .setDuration(220)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    public void flip() {
        setRotationY(90f);

        animate()
                .rotationY(0f)
                .setDuration(260)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();
    }
}
