/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class is is the background process which updates the saturation and value
 * chooser as you move the RGB sliders.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.graphics.Bitmap;
import android.os.AsyncTask;

@SuppressWarnings("deprecation")
public class FillBitmap extends AsyncTask<BitmapWrapper, Void, BitmapWrapper> {

    protected BitmapWrapper doInBackground(BitmapWrapper... bm) {
        Bitmap bitmap = bm[0].m_Bitmap;
        int colour = bm[0].m_colour;
        if ((colour & 0xFFFFF) == 0) { colour += 0xFFFFFF; }
        int red;
        int green;
        int blue;
        red = (colour >> 16) & 255;
        int sat = red;
        green = (colour >> 8) & 255;
        if (sat < green) {
            sat = green;
        }
        blue = colour & 255;
        if (sat < blue) {
            sat = blue;
        }
        int multiplier = bm[0].m_multiplier;
        switch (multiplier) {
            case 1:
                for (int i = 0; i < 256; ++i) {
                    int redhue = red * i / sat;
                    if (redhue > 255) { redhue = 255; } // safety
                    int greenhue = green * i / sat;
                    if (greenhue > 255) { greenhue = 255; } // safety
                    int bluehue = blue * i / sat;
                    if (bluehue > 255) { bluehue = 255; } // safety
                    for (int j = 0; j < 256; ++j) {
                        int pxred = redhue + (j * (255 - redhue)) / 255;
                        int pxgreen = greenhue + (j * (255 - greenhue)) / 255;
                        int pxblue = bluehue + (j * (255 - bluehue)) / 255;
                        int pixel = 0xFF000000
                            + (pxred << 16) + (pxgreen << 8) + pxblue;
                        bitmap.setPixel(i, j, pixel);
                    }
                }
                break;
            case 2:
                for (int i = 0; i < 256; ++i) {
                    int redhue = red * i / sat;
                    if (redhue > 255) { redhue = 255; } // safety
                    int greenhue = green * i / sat;
                    if (greenhue > 255) { greenhue = 255; } // safety
                    int bluehue = blue * i / sat;
                    if (bluehue > 255) { bluehue = 255; } // safety
                    for (int j = 0; j < 256; ++j) {
                        int pxred = redhue + (j * (255 - redhue)) / 255;
                        int pxgreen = greenhue + (j * (255 - greenhue)) / 255;
                        int pxblue = bluehue + (j * (255 - bluehue)) / 255;
                        int pixel = 0xFF000000
                            + (pxred << 16) + (pxgreen << 8) + pxblue;
                        bitmap.setPixel(i * 2, j * 2, pixel);
                        bitmap.setPixel(i * 2 + 1, j * 2, pixel);
                        bitmap.setPixel(i * 2, j * 2 + 1, pixel);
                        bitmap.setPixel(i * 2 + 1, j * 2 + 1, pixel);
                    }
                }
                break;
            case 4:
                for (int i = 0; i < 256; ++i) {
                    int redhue = red * i / sat;
                    if (redhue > 255) { redhue = 255; } // safety
                    int greenhue = green * i / sat;
                    if (greenhue > 255) { greenhue = 255; } // safety
                    int bluehue = blue * i / sat;
                    if (bluehue > 255) { bluehue = 255; } // safety
                    for (int j = 0; j < 256; ++j) {
                        int pxred = redhue + (j * (255 - redhue)) / 255;
                        int pxgreen = greenhue + (j * (255 - greenhue)) / 255;
                        int pxblue = bluehue + (j * (255 - bluehue)) / 255;
                        int pixel = 0xFF000000
                            + (pxred << 16) + (pxgreen << 8) + pxblue;
                        bitmap.setPixel(i * 4, j * 4, pixel);
                        bitmap.setPixel(i * 4 + 1, j * 4, pixel);
                        bitmap.setPixel(i * 4 + 2, j * 4, pixel);
                        bitmap.setPixel(i * 4 + 3, j * 4, pixel);
                        bitmap.setPixel(i * 4, j * 4 + 1, pixel);
                        bitmap.setPixel(i * 4 + 1, j * 4 + 1, pixel);
                        bitmap.setPixel(i * 4 + 2, j * 4 + 1, pixel);
                        bitmap.setPixel(i * 4 + 3, j * 4 + 1, pixel);
                        bitmap.setPixel(i * 4, j * 4 + 2, pixel);
                        bitmap.setPixel(i * 4 + 1, j * 4 + 2, pixel);
                        bitmap.setPixel(i * 4 + 2, j * 4 + 2, pixel);
                        bitmap.setPixel(i * 4 + 3, j * 4 + 2, pixel);
                        bitmap.setPixel(i * 4, j * 4 + 3, pixel);
                        bitmap.setPixel(i * 4 + 1, j * 4 + 3, pixel);
                        bitmap.setPixel(i * 4 + 2, j * 4 + 3, pixel);
                        bitmap.setPixel(i * 4 + 3, j * 4 + 3, pixel);
                    }
                }
                break;
        }
        bm[0].m_active = null;
        return bm[0];
    }

    @Override
    protected void onPostExecute(BitmapWrapper bitmapWrapper) {
        if (bitmapWrapper.m_firstTime) {
            bitmapWrapper.m_owner.redisplayBitmap();
        }
        super.onPostExecute(bitmapWrapper);
    }
}
