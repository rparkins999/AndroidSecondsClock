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
    public boolean m_firstTime;
    public WidgetConfigureActivity m_owner;
    BitmapWrapper(int colour, Bitmap bitmap, int multiplier,
                  WidgetConfigureActivity owner, boolean firstTime) {
        m_Bitmap = bitmap;
        m_colour = colour;
        m_active = null;
        m_multiplier = multiplier;
        m_owner = owner;
        m_firstTime = firstTime;
    }
}
