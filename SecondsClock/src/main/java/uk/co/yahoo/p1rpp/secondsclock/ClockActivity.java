/*
 * Copyright © 2022. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is the activity which runs the fullscreen clock.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class ClockActivity extends Activity_common
    implements View.OnLongClickListener
{

    private ClockView m_clockView;

    // doesn't do anything for now
    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generic_layout);
        m_topLayout = findViewById(R.id.genericlayout);
        m_topLayout.setBackgroundColor(0xFF000000);
        m_clockView = new ClockView(this);
    }

    @Override
    protected void onResume() {
        m_key = "C";
        super.onResume();
        m_topLayout.removeAllViews();
        m_topLayout.addView(m_clockView);
        m_clockView.setHeight(getResources().getDisplayMetrics().heightPixels);
    }

    /* Touching the screen makes the clock go away.
     * This is because the back button may be too dimmed down to be visible.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            finish();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_topLayout.removeAllViews();
    }
}
