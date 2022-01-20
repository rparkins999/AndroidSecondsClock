/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class implements a SeekBar which responds to long clicks.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.SeekBar;

@SuppressLint("AppCompatCustomView")
class LongClickableSeekBar extends SeekBar {

    // This is the maximum amount of motion in any direction that we consider
    // to be finger shake rather than a real pointer move action.
    private static final float DELTA_IN = 0.03F;
    // These will not usually be different, but they can be.
    private final float m_Xdelta; // same thing in pixels for X direction
    private final float m_Ydelta; // same thing in pixels for Y direction

    private boolean m_longClicked = false;
    private boolean m_timerRunning = false;
    private int m_savedProgress;
    private float m_startX;
    private float m_startY;

    private final Runnable longClickTimer = new Runnable() {
        @Override
        public void run() {
            // timeout expired
            //Log.d("MotionEvent","timer expired");
            m_longClicked = true;
            performLongClick();
            m_timerRunning = false;
        }
    };

    public LongClickableSeekBar(Context context)
    {
        super(context);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        m_Xdelta = DELTA_IN * dm.xdpi;
        m_Ydelta = DELTA_IN * dm.ydpi;
        setLongClickable(true);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                //Log.d("MotionEvent","ACTION_DOWN");
                // it is possible to get a down event when we think that the pointer
                // is already down: we abandon any action in  progress
                removeCallbacks(longClickTimer); // cancel the timeout
                m_longClicked = false;
                m_savedProgress = getProgress();
                m_startX = event.getX();
                m_startY = event.getY();
                // start the timeout
                //Log.d("MotionEvent","Starting long click timer");
                /* We give it twice the normal long click timeout because of the
                 * user's reaction time to see that they have
                 * touched the thumb before moving it.
                 */
                postDelayed(longClickTimer,
                    2L * ViewConfiguration.getLongPressTimeout());
                m_timerRunning = true;
                result = super.onTouchEvent(event);
                // undo the thumb move in case we decide it's a long click
                setProgress(m_savedProgress);
                return result;
            case MotionEvent.ACTION_UP:
                if (m_longClicked) {
                    m_longClicked = false;
                    setProgress(m_savedProgress);
                    event.setAction(MotionEvent.ACTION_CANCEL);
                    //Log.d("MotionEvent","ACTION_UP cancelled");
                } else {
                    removeCallbacks(longClickTimer); // cancel the timeout
                    m_timerRunning = false;
                    //Log.d("MotionEvent","ACTION_UP => super");
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (m_longClicked) {
                    setProgress(m_savedProgress);
                    event.setAction(MotionEvent.ACTION_CANCEL);
                    //Log.d("MotionEvent","ACTION_MOVE cancelled");
                } else if (   (   (event.getX() - m_startX > m_Xdelta)
                               || (m_startX - event.getX() > m_Xdelta)
                               || (event.getY() - m_startY > m_Ydelta)
                               || (m_startY - event.getY() > m_Ydelta))
                           && m_timerRunning)
                {
                    // real move action
                    removeCallbacks(longClickTimer); // cancel the timeout
                    m_timerRunning = false;
                    //Log.d("MotionEvent","ACTION_MOVE => stop timer");
                } else if (m_timerRunning) { // treat it as finger shake
                    result = super.onTouchEvent(event);
                    // undo the thumb move in case we decide it's a long click
                    setProgress(m_savedProgress);
                    //Log.d("MotionEvent","ACTION_MOVE => finger shake");
                    return result;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                m_longClicked = false;
                removeCallbacks(longClickTimer); // cancel the timeout
                m_timerRunning = false;
                //Log.d("MotionEvent","ACTION_CANCEL");
                break;
            default:
                //Log.d("MotionEvent",event.toString());
                if (m_longClicked) {
                    event.setAction(MotionEvent.ACTION_CANCEL);
                } else if (m_timerRunning) {
                    result = super.onTouchEvent(event);
                    // undo any thumb move in case we decide it's a long click
                    setProgress(m_savedProgress);
                    return result;
                }
                break;
        }
        return super.onTouchEvent(event);
    }
}
