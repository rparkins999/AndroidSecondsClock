/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is the activity which runs the fullscreen clock.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.Calendar;

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
        m_topLayout.addView(m_clockView);
    }

    @Override
    protected void onResume() {
        m_key = "C";
        super.onResume();
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
}
