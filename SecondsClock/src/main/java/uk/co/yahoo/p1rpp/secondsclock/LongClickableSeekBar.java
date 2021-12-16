/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class implements a SeekBar which responds to long clicks.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcel;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.SeekBar;

@SuppressLint("AppCompatCustomView")
class LongClickableSeekBar extends SeekBar {

    private boolean m_longClicked = false;
    private int m_savedProgress;
    private final Runnable longClickTimer = new Runnable() {
        @Override
        public void run() {
            // timeout expired
            m_longClicked = true;
            performLongClick();
        }
    };

    public LongClickableSeekBar(Context context)
    {
        super(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // it is possible to get a down event when we think that the pointer
                // is already down: we abandon any action in  progress
                removeCallbacks(longClickTimer); // cancel the timeout
                m_longClicked = false;
                m_savedProgress = getProgress();
                // start the timeout
                postDelayed(longClickTimer, ViewConfiguration.getLongPressTimeout());
                boolean result = super.onTouchEvent(event);
                setProgress(m_savedProgress);
                return result;
            case MotionEvent.ACTION_UP:
                if (m_longClicked) {
                    m_longClicked = false;
                    setProgress(m_savedProgress);
                    event.setAction(MotionEvent.ACTION_CANCEL);
                } else {
                    removeCallbacks(longClickTimer); // cancel the timeout
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (m_longClicked) {
                    setProgress(m_savedProgress);
                    event.setAction(MotionEvent.ACTION_CANCEL);
                } else {
                    removeCallbacks(longClickTimer); // cancel the timeout
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                m_longClicked = false;
                break;
            default:
                if (m_longClicked) {
                    event.setAction(MotionEvent.ACTION_CANCEL);
                }
                break;
        }
        return super.onTouchEvent(event);
    }
}
