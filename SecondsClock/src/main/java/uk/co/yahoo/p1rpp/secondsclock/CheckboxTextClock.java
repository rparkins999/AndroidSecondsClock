/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.text.Spanned;
import android.util.AttributeSet;
import android.widget.RemoteViews;
import android.widget.TextClock;

/** A TextClock that restores line colors on Android releases that drop format spans. */
@RemoteViews.RemoteView
public class CheckboxTextClock extends TextClock {
    private boolean applyingLineColors;

    public CheckboxTextClock(Context context) {
        super(context);
    }

    public CheckboxTextClock(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onTextChanged(
            CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (applyingLineColors || text == null || text.length() == 0) return;

        CharSequence format = android.text.format.DateFormat.is24HourFormat(getContext())
                ? getFormat24Hour() : getFormat12Hour();
        if (!(format instanceof Spanned)) return;
        CharSequence coloredText = LineColors.colorFormattedText(text, format);
        if (coloredText == text) return;

        applyingLineColors = true;
        try {
            setText(coloredText, BufferType.SPANNABLE);
        } finally {
            applyingLineColors = false;
        }
    }
}
