package com.example.pawsomeminigames.crittertoss;

import com.example.pawsomeminigames.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/** A LinearLayout that never gets wider than app:maxContentWidth, so menus stay phone-sized on tablets. */
public class MaxWidthLinearLayout extends LinearLayout {
    private int maxWidth = 0;

    public MaxWidthLinearLayout(Context c) { this(c, null); }

    public MaxWidthLinearLayout(Context c, AttributeSet a) {
        super(c, a);
        if (a != null) {
            TypedArray t = c.obtainStyledAttributes(a, R.styleable.MaxWidthLinearLayout);
            maxWidth = t.getDimensionPixelSize(R.styleable.MaxWidthLinearLayout_maxContentWidth, 0);
            t.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if (maxWidth > 0) {
            int mode = MeasureSpec.getMode(widthSpec);
            if (mode != MeasureSpec.UNSPECIFIED && MeasureSpec.getSize(widthSpec) > maxWidth) {
                widthSpec = MeasureSpec.makeMeasureSpec(maxWidth, mode);
            }
        }
        super.onMeasure(widthSpec, heightSpec);
    }
}
