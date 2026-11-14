/*
 * Copyright © 2026. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

/** Generates the custom dial bitmap sent to the standard RemoteViews ImageView. */
final class AnalogClockFace {
    private static final float DESIGN_SIZE = 512.0f;
    private static final int BITMAP_SIZE = 256;
    private static final float CENTER = DESIGN_SIZE / 2.0f;

    private AnalogClockFace() {}

    static Bitmap createBitmap(int color) {
        Bitmap bitmap = Bitmap.createBitmap(
                BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float scale = BITMAP_SIZE / DESIGN_SIZE;
        canvas.scale(scale, scale);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4.0f);
        canvas.drawCircle(CENTER, CENTER, 240.0f, paint);

        for (int minute = 0; minute < 60; minute++) {
            double angle = Math.toRadians(minute * 6.0 - 90.0);
            float inner = minute % 5 == 0 ? 207.0f : 217.0f;
            paint.setStrokeWidth(minute % 5 == 0 ? 5.0f : 2.5f);
            canvas.drawLine(
                    CENTER + (float) Math.cos(angle) * inner,
                    CENTER + (float) Math.sin(angle) * inner,
                    CENTER + (float) Math.cos(angle) * 226.0f,
                    CENTER + (float) Math.sin(angle) * 226.0f,
                    paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(42.0f);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baselineOffset = -(metrics.ascent + metrics.descent) / 2.0f;
        for (int hour = 1; hour <= 12; hour++) {
            double angle = Math.toRadians(hour * 30.0 - 90.0);
            float x = CENTER + (float) Math.cos(angle) * 176.0f;
            float y = CENTER + (float) Math.sin(angle) * 176.0f;
            canvas.drawText(Integer.toString(hour), x, y + baselineOffset, paint);
        }
        return bitmap;
    }
}
