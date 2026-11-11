package com.example.pawsomeminigames.reactiongame.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class TileGridLayout extends ViewGroup {
    private int gap;
    private int animationSpace;

    public TileGridLayout(Context context) {
        this(context, null);
    }

    public TileGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        setClipChildren(false);
        setClipToPadding(false);

        float density = getResources().getDisplayMetrics().density;

        gap = Math.round(12 * density);
        animationSpace = Math.round(12 * density);
    }

    public void setGap(int px) {
        gap = px;
        requestLayout();
    }

    private int tileSize(int w, int h) {

        int byW = (w - getPaddingLeft()
                - getPaddingRight()
                - gap - (animationSpace * 2)) / 2;

        int byH = (h - getPaddingTop()
                - getPaddingBottom()
                - gap - (animationSpace * 2)) / 2;

        return Math.max(0, Math.min(byW, byH));
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED ? w : MeasureSpec.getSize(heightSpec);
        int s = tileSize(w, h);
        int childSpec = MeasureSpec.makeMeasureSpec(s, MeasureSpec.EXACTLY);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(childSpec, childSpec);
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l, h = b - t;
        int s = tileSize(w, h);
        int totalW = s * 2 + gap, totalH = s * 2 + gap;
        int left = (w - totalW) / 2, top = (h - totalH) / 2;
        for (int i = 0; i < getChildCount() && i < 4; i++) {
            View c = getChildAt(i);
            int col = i % 2, row = i / 2;
            int x = left + col * (s + gap), y = top + row * (s + gap);
            c.layout(x, y, x + s, y + s);
        }
    }
}
