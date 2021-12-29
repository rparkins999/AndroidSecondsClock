/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class handles the settings for the full screen clock.
 * It also shows a demonstration of what it will look like
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ClockConfigureActivity extends ConfigureActivity
    implements SeekBar.OnSeekBarChangeListener, View.OnLongClickListener,
    CompoundButton.OnCheckedChangeListener
{

    private static final int SECONDSSIZER = 7070;
    private static final int NOSECONDS = SECONDSSIZER + 1;
    private static final int SMALLSECONDS = NOSECONDS + 1;
    private static final int LARGESECONDS = SMALLSECONDS + 1;
    private static final int SEVENSEGMENTS = LARGESECONDS + 1;

    private ClockView m_clockView;

    @SuppressLint("ApplySharedPref")
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case SECONDSSIZER:
                m_prefs.edit().putInt("CsecondsSize", progress).commit();
                m_clockView.updateLayout();
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case SECONDSSIZER: doToast(R.string.secondssizerhelp); return true;
            case NOSECONDS: doToast(R.string.nosecondshelp); return true;
            case SMALLSECONDS: doToast(R.string.smallsecondshelp); return true;
            case LARGESECONDS: doToast(R.string.largesecondshelp); return true;
            case SEVENSEGMENTS: doToast(R.string.sevensegmentshelp); return true;
        }
        return super.onLongClick(v);
    }

    protected void updateFromCheckBox() {
        m_clockView.updateLayout();
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case SEVENSEGMENTS:
                m_prefs.edit().putBoolean("C7seg", isChecked).commit();
                m_clockView.updateLayout();
                break;
            default: super.onCheckedChanged(buttonView, isChecked);
        }
    }

    @SuppressLint({"RtlHardcoded", "SetTextI18n"})
    protected void doMainLayout() {
        super.doMainLayout();
        int width = m_metrics.widthPixels;
        int height = m_metrics.heightPixels;
        if (m_config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            GridLayout gl = new GridLayout(this);
            gl.setBackgroundColor(0xFF000000);
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(0, 1, 1f),
                GridLayout.spec(0, 1, 1f)
            );
            layoutParams.width = 0;
            layoutParams.height = 0;
            gl.addView(m_clockView, -1, layoutParams);
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(1, 1, 1f),
                GridLayout.spec(0, 1, 1f)
            );
            layoutParams.width = 0;
            layoutParams.height = 0;
            RelativeLayout l1 = new RelativeLayout(this);
            RelativeLayout.LayoutParams leftAlign =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            leftAlign.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            l1.addView(textLabel("00:00", NOSECONDS), leftAlign);
            LinearLayout l2 = new LinearLayout(this);
            l2.setOnLongClickListener(this);
            l2.setId(SMALLSECONDS);
            l2.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams l2pars = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            l2pars.gravity = Gravity.CENTER_VERTICAL;
            TextView tv1 = new TextView(this);
            tv1.setText("00:00");
            l2.addView(tv1, l2pars);
            TextView tv2 = new TextView(this);
            tv2.setText("00");
            tv2.setScaleX(0.6F);
            tv2.setScaleY(0.6F);
            l2.addView(tv2, l2pars);
            RelativeLayout.LayoutParams centre =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            centre.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            l1.addView(l2, centre);
            RelativeLayout.LayoutParams rightAlign =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            rightAlign.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            l1.addView(textLabel("00:00:00", LARGESECONDS), rightAlign);
            LongClickableSeekBar secondsSizer = new LongClickableSeekBar(this);
            secondsSizer.setMax(255);
            secondsSizer.setId(SECONDSSIZER);
            secondsSizer.setOnSeekBarChangeListener(this);
            secondsSizer.setOnLongClickListener(this);
            secondsSizer.setBackgroundColor(0xFF000000);
            secondsSizer.setProgress(m_prefs.getInt("CsecondsSize", 255));
            RelativeLayout.LayoutParams below1 =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            below1.addRule(RelativeLayout.BELOW, LARGESECONDS);
            l1.addView(secondsSizer, below1);
            CheckBox cb = new CheckBox(this);
            cb.setId(SEVENSEGMENTS);
            cb.setText(R.string.sevensegments);
            cb.setChecked(m_prefs.getBoolean("C7seg", false));
            cb.setOnLongClickListener(this);
            cb.setOnCheckedChangeListener(this);
            RelativeLayout.LayoutParams below2 =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            below2.addRule(RelativeLayout.BELOW, SECONDSSIZER);
            l1.addView(cb, below2);
            gl.addView(l1, -1, layoutParams);
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(0, 1, 1f),
                GridLayout.spec(1, 2, 1f)
            );
            layoutParams.width = 0;
            layoutParams.height = 0;
            LinearLayout l3 = new LinearLayout(this);
            l3.setOrientation(LinearLayout.VERTICAL);
            l3.addView(showLongWeekDayCheckBox);
            l3.addView(showShortWeekDayCheckBox);
            l3.addView(showShortDateCheckBox);
            l3.addView(showMonthDayCheckBox);
            l3.addView(showShortMonthCheckBox);
            l3.addView(showLongMonthCheckBox);
            l3.addView(showYearCheckBox);
            gl.addView(l3, -1, layoutParams);
            m_topLayout.addView(gl);
        } else { // assume PORTRAIT
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_CorW = "clock";
        m_clockView = new ClockView(this);
    }

    @Override
    protected void onResume() {
        m_key = "C";
        super.onResume();
        getDatePrefs();
        doMainLayout();
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void setCurrentView(int viewnum) {
        m_prefs.edit().putInt("Cview", currentView).commit();
        switch (currentView) {
            case CONFIGURE: doMainLayout(); break;
            case SETTEXTCOLOUR: doChooserLayout(); break;
        }
    }
}
