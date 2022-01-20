/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This contains a few bits of shared code for all the app's Activities
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

public abstract class Activity_common extends Activity implements View.OnLongClickListener {

    protected TextView m_helptext;
    protected String m_key;
    protected FrameLayout m_topLayout;
    protected Activity m_activity = this;
    protected SharedPreferences m_prefs;
    protected int showWeekDay;
    protected int showShortDate;
    protected int showMonthDay;
    protected int showMonth;
    protected int showYear;

    protected TextView textLabel(CharSequence cs, int id) {
        TextView tv = new TextView(this);
        tv.setText(cs);
        tv.setId(id);
        tv.setOnLongClickListener(this);
        return tv;
    }

    protected TextView textLabel(int resid, int id) {
        return textLabel(getString(resid), id);
    }

    protected void doToast(String s) {
        Toast.makeText(m_activity, s, Toast.LENGTH_LONG).show();
    }

    protected void doToast(int resId) {
        Toast.makeText(m_activity, getString(resId), Toast.LENGTH_LONG).show();
    }

    protected void doToast(int resId, String arg) {
        Toast.makeText(m_activity, getString(resId, arg), Toast.LENGTH_LONG).show();
    }

    // recursive version
    protected void removeAllViews(View v) {
        if (v instanceof ViewGroup) {
            int n = ((ViewGroup)v).getChildCount();
            for ( int i = 0; i < n; ++i) {
                removeAllViews(((ViewGroup)v).getChildAt(i));
            }
            ((ViewGroup)v).removeAllViews();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_prefs = getSharedPreferences("SecondsClock", Context.MODE_PRIVATE);
    }

    protected void getDatePrefs() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        // m_key has been set before derived class calls super.onResume()
        showMonth = m_prefs.getInt(m_key + "showMonth", 2); // long format
        showMonthDay = m_prefs.getInt(m_key + "showMonthDay",1);
        showShortDate = m_prefs.getInt(m_key + "showShortDate",0);
        showWeekDay = m_prefs.getInt(
            m_key + "showWeekDay",2); // long format
        showYear = m_prefs.getInt(m_key + "showYear", 1);
        m_topLayout = findViewById(R.id.genericlayout);
    }
}
