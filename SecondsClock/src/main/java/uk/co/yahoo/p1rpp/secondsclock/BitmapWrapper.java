/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class packages a Bitmap and a colour to pass to FillBitmap
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.graphics.Bitmap;

class BitmapWrapper {
    public Bitmap m_Bitmap;
    public int m_colour;
    public FillBitmap m_active;
    public int m_multiplier;
    BitmapWrapper(int colour, Bitmap bitmap, int multiplier) {
        m_Bitmap = bitmap;
        m_colour = colour;
        m_active = null;
        m_multiplier = multiplier;
    }
}
