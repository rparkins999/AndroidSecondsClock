/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.Calendar;

/** A full-screen analog dial that can independently show each hand. */
final class AnalogClockView extends View {
    private static final float DESIGN_SIZE = 512.0f;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color = 0xFFFFFFFF;
    private int handColor = 0xFFFFFFFF;
    private Bitmap face;
    private Calendar time = Calendar.getInstance();
    private boolean showHour = true;
    private boolean showMinute = true;
    private boolean showSecond = true;

    AnalogClockView(Context context) {
        super(context);
        setContentDescription("Analog clock");
        face = AnalogClockFace.createBitmap(color);
    }

    void setColor(int value) {
        if (color == value) return;
        color = value;
        handColor = value;
        face = AnalogClockFace.createBitmap(color);
        invalidate();
    }

    void setHandColor(int value) {
        if (handColor == value) return;
        handColor = value;
        invalidate();
    }

    void setHands(boolean hour, boolean minute, boolean second) {
        showHour = hour;
        showMinute = minute;
        showSecond = second;
        invalidate();
    }

    void setTime(Calendar value) {
        time = (Calendar) value.clone();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
        int availableHeight = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(availableWidth, availableHeight);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            size = availableHeight;
        }
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            size = availableWidth;
        }
        setMeasuredDimension(Math.max(0, size), Math.max(0, size));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;
        int save = canvas.save();
        canvas.translate((getWidth() - size) / 2.0f, (getHeight() - size) / 2.0f);
        canvas.scale(size / DESIGN_SIZE, size / DESIGN_SIZE);
        canvas.drawBitmap(face, null,
                new RectF(0.0f, 0.0f, DESIGN_SIZE, DESIGN_SIZE), null);
        drawHands(canvas);
        canvas.restoreToCount(save);
    }

    private void drawHands(Canvas canvas) {
        float center = DESIGN_SIZE / 2.0f;
        float hour = time.get(Calendar.HOUR) * 30.0f
                + time.get(Calendar.MINUTE) * 0.5f;
        float minute = time.get(Calendar.MINUTE) * 6.0f
                + time.get(Calendar.SECOND) * 0.1f;
        if (showHour) drawHand(canvas, center, hour, 130.0f, 12.0f);
        if (showMinute) drawHand(canvas, center, minute, 190.0f, 8.0f);
        if (showSecond) {
            paint.setColor(handColor);
            paint.setStrokeWidth(3.5f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            float angle = (float) Math.toRadians(
                    time.get(Calendar.SECOND) * 6.0f - 90.0f);
            canvas.drawLine(center, center,
                    center + (float) Math.cos(angle) * 210.0f,
                    center + (float) Math.sin(angle) * 210.0f, paint);
        }
        paint.setColor(handColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(center, center, 10.0f, paint);
    }

    private void drawHand(Canvas canvas, float center, float degrees,
                          float length, float width) {
        paint.setColor(handColor);
        paint.setStrokeWidth(width);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        float angle = (float) Math.toRadians(degrees - 90.0f);
        canvas.drawLine(center, center,
                center + (float) Math.cos(angle) * length,
                center + (float) Math.sin(angle) * length, paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
