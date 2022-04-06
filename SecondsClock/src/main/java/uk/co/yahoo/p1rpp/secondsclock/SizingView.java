/*
 * Copyright © 2022. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This implements a dummy view which simply gets its size,
 * makes itself invisible to avoid wasting time drawing it,,
 * and posts a Runnable which calls setWidthAndHeight in its creator.
 * This has to be called every time an Activity gets resumed,
 * because the screen orientation may have changed.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.view.View;

@SuppressLint("ViewConstructor")
class SizingView extends View {
    Activity_common m_owner;
    private final Handler m_handler = new Handler();
    private int m_width;
    private int m_height;

    private final Runnable m_runner = new Runnable() {
        public void run() {
            m_owner.setWidthAndHeight(m_width, m_height);
        }
    };

    public SizingView(Activity_common context) {
        super(context);
        m_owner = context;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        m_width = right - left;
        m_height = bottom - top;
        setVisibility(INVISIBLE);
        m_handler.post(m_runner);
    }
}
