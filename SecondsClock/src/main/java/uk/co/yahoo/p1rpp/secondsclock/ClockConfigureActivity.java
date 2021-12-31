/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class handles the settings for the full screen clock.
 * It also shows a demonstration of what it will look like
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class ClockConfigureActivity extends ConfigureActivity
    implements SeekBar.OnSeekBarChangeListener, View.OnLongClickListener,
    CompoundButton.OnCheckedChangeListener
{

    private static final int DEMOCLOCK = LASTITEM + 1;
    private static final int NOSECONDS = DEMOCLOCK + 1;
    private static final int SMALLSECONDS = NOSECONDS + 1;
    private static final int LARGESECONDS = SMALLSECONDS + 1;
    private static final int SECONDSSIZER = LARGESECONDS + 1;
    private static final int BRIGHTNESS = SECONDSSIZER + 1;
    private static final int SEVENSEGMENTS = BRIGHTNESS + 1;
    private static final int FORCEVERTICAL = SEVENSEGMENTS + 1;
    private static final int TEXTCOLOUR = FORCEVERTICAL + 1;
    private static final int RUNCLOCK = TEXTCOLOUR + 1;

    private LongClickableSeekBar m_brightness;
    private ClockView m_clockView;
    private LongClickableSeekBar m_secondsSizer;

    @SuppressLint("ApplySharedPref")
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            switch (seekBar.getId()) {
                case SECONDSSIZER:
                    m_prefs.edit().putInt("CsecondsSize", progress).commit();
                    m_clockView.updateLayout();
                    break;
                case BRIGHTNESS:
                    m_prefs.edit().putInt("Cbrightness", progress).commit();
                    m_clockView.adjustColour();
                    break;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case LONGPRESSHELP: doToast(R.string.clockconfighelp); return true;
            case DEMOCLOCK:
                doToast(getString(R.string.clockdemo,
                    (m_config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                        ? getString(R.string.landscape) : getString(R.string.portrait)));
                return true;
            case NOSECONDS: doToast(R.string.nosecondshelp); return true;
            case SMALLSECONDS: doToast(R.string.smallsecondshelp); return true;
            case LARGESECONDS: doToast(R.string.largesecondshelp); return true;
            case SECONDSSIZER: doToast(R.string.secondssizerhelp); return true;
            case BRIGHTNESS: doToast(R.string.brightnesshelp); return true;
            case SEVENSEGMENTS: doToast(R.string.sevensegmentshelp); return true;
            case FORCEVERTICAL: doToast(R.string.forceverticalhelp); return true;
            case TEXTCOLOUR: doToast(R.string.setclockcolourhelp); return true;
            case RUNCLOCK:doToast(R.string.runclockhelp); return true;
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
            case FORCEVERTICAL:
                m_prefs.edit().putBoolean("Cforcevertical", isChecked).commit();
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
        RelativeLayout.LayoutParams below1 =
            new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        below1.addRule(RelativeLayout.BELOW, LARGESECONDS);
        l1.addView(m_secondsSizer, below1);
        RelativeLayout.LayoutParams below2 =
            new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        below2.addRule(RelativeLayout.BELOW, SECONDSSIZER);
        l1.addView(m_brightness, below2);
        LinearLayout l3 = new LinearLayout(this);
        l3.setOrientation(LinearLayout.VERTICAL);
        l3.addView(m_helptext);
        l3.addView(showLongWeekDayCheckBox);
        l3.addView(showShortWeekDayCheckBox);
        l3.addView(showShortDateCheckBox);
        l3.addView(showMonthDayCheckBox);
        l3.addView(showLongMonthCheckBox);
        l3.addView(showShortMonthCheckBox);
        l3.addView(showYearCheckBox);
        CheckBox cb = new CheckBox(this);
        cb.setId(SEVENSEGMENTS);
        cb.setText(R.string.sevensegments);
        cb.setChecked(m_prefs.getBoolean("C7seg", false));
        cb.setOnLongClickListener(this);
        cb.setOnCheckedChangeListener(this);
        l3.addView(cb); cb = new CheckBox(this);
        if (m_config.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            cb.setId(FORCEVERTICAL);
            cb.setText(R.string.forcevertical);
            cb.setChecked(m_prefs.getBoolean("Cforcevertical", false));
            cb.setOnLongClickListener(this);
            cb.setOnCheckedChangeListener(this);
            l3.addView(cb);
        }
        Button button = new Button(this);
        button.setText(R.string.setclockcolour);
        button.setAllCaps(false);
        button.setId(TEXTCOLOUR);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doChooserLayout();
            }
        });
        button.setOnLongClickListener(this);
        LinearLayout.LayoutParams l3pars = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        l3pars.gravity = Gravity.CENTER_HORIZONTAL;
        l3.addView(button, l3pars);
        button = new Button(this);
        button.setText(R.string.runclock);
        button.setAllCaps(false);
        button.setId(RUNCLOCK);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(m_activity, ClockActivity.class);
                startActivity(intent);
            }
        });
        button.setOnLongClickListener(this);
        l3.addView(button, l3pars);
        ScrollView sv = new ScrollView(this);
        sv.addView(l3);
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
            gl.addView(l1, -1, layoutParams);
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(0, 2, 1f),
                GridLayout.spec(1, 1, 1f)
            );
            layoutParams.width = 0;
            layoutParams.height = 0;
            gl.addView(sv, -1, layoutParams);
            m_topLayout.addView(gl);
        } else { // assume PORTRAIT
            LinearLayout l4 = new LinearLayout(this);
            l4.setOrientation(LinearLayout.VERTICAL);
            l4.addView(m_clockView, new LinearLayout.LayoutParams(
                width, height / 2));
            l4.addView(l1);
            l4.addView(sv);
            m_topLayout.addView(l4);
        }
    }

    @Override
    protected void doChooserLayout() {
        //super.doChooserLayout(); // commented until we have code for new layout
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_CorW = "clock";
        m_clockView = new ClockView(this);
        m_clockView.setId(DEMOCLOCK);
        m_clockView.setOnLongClickListener(this);
        m_clockView.setOwner(this);
    }

    /* This is called by the ClockView when the ambient light level changes,
     * to set the maximum value of the brightness slider.
     */
    public void setOpacity(int opacity) {
        int fgColour = m_prefs.getInt("CfgColour", 0xFFFFFFFF);
        m_brightness.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {0xFF000000,  (opacity << 24) | (fgColour & 0xFFFFFF)}));
    }

    @Override
    protected void onResume() {
        m_key = "C";
        super.onResume();
        /* This is in fact the height of the actual view, but the ClockView
         * can't read its own height before it has been laid out, and in PORTRAIT
         * orientation we need it before then to assign space to its children.
         */
        m_clockView.setHeight(m_metrics.heightPixels / 2);
        m_secondsSizer = new LongClickableSeekBar(this);
        m_secondsSizer.setMax(255);
        m_secondsSizer.setId(SECONDSSIZER);
        m_secondsSizer.setOnSeekBarChangeListener(this);
        m_secondsSizer.setOnLongClickListener(this);
        m_secondsSizer.setBackgroundColor(0xFF000000);
        m_secondsSizer.setProgress(m_prefs.getInt("CsecondsSize", 255));
        m_brightness = new LongClickableSeekBar(this);
        m_brightness.setMax(255);
        m_brightness.setId(BRIGHTNESS);
        m_brightness.setOnSeekBarChangeListener(this);
        m_brightness.setOnLongClickListener(this);
        m_brightness.setProgress(m_prefs.getInt("Cbrightness", 255));
        ColorStateList cl = ColorStateList.valueOf(0xFFFFFFFF);
        m_brightness.setProgressTintList(cl);
        m_brightness.setThumbTintList(cl);
        setOpacity(255);
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
