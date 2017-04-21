package com.telenav.osv.ui.custom;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.style.ReplacementSpan;

/**
 * Created by Kalman on 04/01/2017.
 */

public class CenteredSpan extends ReplacementSpan {

    public CenteredSpan() {
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        int xPos = (int) x;
        int yPos = (int) ((bottom / 2) - ((paint.descent() + paint.ascent()) / 2)) ;
        canvas.drawText(text, start, end, xPos, yPos, paint);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end));
    }
}