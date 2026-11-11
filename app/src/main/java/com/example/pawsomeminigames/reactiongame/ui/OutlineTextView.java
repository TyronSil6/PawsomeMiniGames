package com.example.pawsomeminigames.reactiongame.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.example.pawsomeminigames.R;

public class OutlineTextView extends AppCompatTextView {
    private int strokeColor = 0xFF4A2A10;
    private float strokeWidth;

    public OutlineTextView(Context context) {
        this(context, null);
    }

    public OutlineTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public OutlineTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        strokeWidth = 3f * getResources()
                .getDisplayMetrics().density;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.rg_OutlineTextView
            );

            strokeColor = a.getColor(
                    R.styleable.rg_OutlineTextView_rg_strokeColor,
                    strokeColor
            );

            strokeWidth = a.getDimension(
                    R.styleable.rg_OutlineTextView_rg_strokeWidth,
                    strokeWidth
            );

            a.recycle();
        }
    }

    public void setStrokeColor(int color) {
        strokeColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        ColorStateList originalColors = getTextColors();
        Paint paint = getPaint();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(strokeWidth);

        setTextColor(strokeColor);
        super.onDraw(canvas);

        paint.setStyle(Paint.Style.FILL);

        setTextColor(originalColors);
        super.onDraw(canvas);

        paint.setStyle(Paint.Style.FILL);
    }
}
