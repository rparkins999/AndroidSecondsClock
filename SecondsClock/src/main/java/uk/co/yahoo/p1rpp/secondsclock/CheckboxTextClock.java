/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.text.Spanned;
import android.util.AttributeSet;
import android.widget.TextClock;

/** Restores formatted line colors in the local configuration preview on older Android. */
final class CheckboxTextClock extends TextClock {
    private boolean applyingLineColors;

    CheckboxTextClock(Context context) {
        super(context);
    }

    CheckboxTextClock(Context context, AttributeSet attrs) {
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
        CharSequence colored = LineColors.colorFormattedText(text, format);
        if (colored == text) return;

        applyingLineColors = true;
        try {
            setText(colored, BufferType.SPANNABLE);
        } finally {
            applyingLineColors = false;
        }
    }
}
