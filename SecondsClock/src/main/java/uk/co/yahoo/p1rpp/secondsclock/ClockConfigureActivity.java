/*
 * Copyright © 2022. Richard P. Parkins, M. A.
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
import android.provider.Settings;
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
import android.widget.TextView;

public class ClockConfigureActivity extends ConfigureActivity
    implements MySeekBar.OnMySeekBarChangeListener,
    View.OnClickListener,
    View.OnLongClickListener,
    CompoundButton.OnCheckedChangeListener
{

    private static final int DEMOCLOCK = LASTITEM + 1;
    private static final int SSLABEL = DEMOCLOCK + 1;
    private static final int NOSECONDS = SSLABEL + 1;
    private static final int SMALLSECONDS = NOSECONDS + 1;
    private static final int LARGESECONDS = SMALLSECONDS + 1;
    private static final int SECONDSSIZER = LARGESECONDS + 1;
    private static final int BRLABEL = SECONDSSIZER + 1;
    private static final int BRIGHTNESS = BRLABEL + 1;
    private static final int SEVENSEGMENTS = BRIGHTNESS + 1;
    private static final int FORCEVERTICAL = SEVENSEGMENTS + 1;
    private static final int TEXTCOLOUR = FORCEVERTICAL + 1;
    private static final int RUNCLOCK = TEXTCOLOUR + 1;

    private MySeekBar m_brightness;
    private ClockView m_clockView;
    private MySeekBar m_secondsSizer;

    @SuppressLint("ApplySharedPref")
    @Override
    public void onProgressChanged(MySeekBar seekBar, int progress) {
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case TEXTCOLOUR: doChooserLayout(); break;
            case RUNCLOCK:
                Intent intent = new Intent(m_activity, ClockActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case LONGPRESSHELP: doToast(R.string.clockconfighelp); return true;
            case DEMOCLOCK:
                doToast(getString(R.string.clockdemo,
                    (m_config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                        ? getString(R.string.landscape) : getString(R.string.portrait)));
                return true;
            case SSLABEL: doToast(
                (m_config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                    ? getString(R.string.sshelpland) : getString(R.string.sshelpport));
                return true;
            case NOSECONDS: doToast(R.string.nosecondshelp); return true;
            case SMALLSECONDS: doToast(R.string.smallsecondshelp); return true;
            case LARGESECONDS: doToast(R.string.largesecondshelp); return true;
            case SECONDSSIZER: doToast(R.string.secondssizerhelp); return true;
            case BRLABEL: doToast(R.string.minbrighthelp); return true;
            case BRIGHTNESS: doToast(R.string.brightnesshelp); return true;
            case SEVENSEGMENTS: doToast(R.string.sevensegmentshelp); return true;
            case FORCEVERTICAL: doToast(R.string.forceverticalhelp); return true;
            case TEXTCOLOUR: doToast(R.string.setclockcolourhelp); return true;
            case RUNCLOCK: doToast(R.string.runclockhelp); return true;
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
        GridLayout gl = new GridLayout(this);
        gl.setBackgroundColor(0xFF000000);
        // clockview is in top left for both layouts
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
            GridLayout.spec(0, 1),
            GridLayout.spec(0, 1)
        );
        layoutParams.width = width / 2;
        layoutParams.height = height / 2;
        gl.addView(m_clockView, -1, layoutParams);
        LinearLayout l1 = new LinearLayout(this);
        l1.setOrientation(LinearLayout.VERTICAL);
        l1.addView(showLongWeekDayCheckBox);
        l1.addView(showShortWeekDayCheckBox);
        l1.addView(showShortDateCheckBox);
        l1.addView(showMonthDayCheckBox);
        l1.addView(showLongMonthCheckBox);
        l1.addView(showShortMonthCheckBox);
        l1.addView(showYearCheckBox);
        CheckBox cb = new CheckBox(this);
        cb.setId(SEVENSEGMENTS);
        cb.setText(R.string.sevensegments);
        cb.setChecked(m_prefs.getBoolean("C7seg", false));
        cb.setOnLongClickListener(this);
        cb.setOnCheckedChangeListener(this);
        l1.addView(cb); cb = new CheckBox(this);
        if (m_config.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            cb.setId(FORCEVERTICAL);
            cb.setText(R.string.forcevertical);
            cb.setChecked(m_prefs.getBoolean("Cforcevertical", false));
            cb.setOnLongClickListener(this);
            cb.setOnCheckedChangeListener(this);
            l1.addView(cb);
        }
        Button button = new Button(this);
        button.setText(R.string.setclockcolour);
        button.setAllCaps(false);
        button.setId(TEXTCOLOUR);
        button.setOnClickListener(this);
        button.setOnLongClickListener(this);
        LinearLayout.LayoutParams l1pars = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        l1pars.gravity = Gravity.CENTER_HORIZONTAL;
        l1.addView(button, l1pars);
        button = new Button(this);
        button.setText(R.string.runclock);
        button.setAllCaps(false);
        button.setId(RUNCLOCK);
        button.setOnClickListener(this);
        button.setOnLongClickListener(this);
        l1.addView(button, l1pars);
        // common code for small seconds label
        LinearLayout l2 = new LinearLayout(this);
        // default is HORIZONTAL
        l2.setOnLongClickListener(this);
        l2.setId(SMALLSECONDS);
        LinearLayout.LayoutParams l2pars = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        l2pars.gravity = Gravity.CENTER_VERTICAL;
        TextView tv = new TextView(this);
        tv.setText("00:00");
        l2.addView(tv, l2pars);
        tv = new TextView(this);
        tv.setText("00");
        tv.setScaleX(0.6F);
        tv.setScaleY(0.6F);
        l2.addView(tv, l2pars);
        if (m_config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Sliders in bottom left
            RelativeLayout l3 = new RelativeLayout(this);
            RelativeLayout.LayoutParams rlp1 =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            rlp1.addRule(RelativeLayout.CENTER_HORIZONTAL);
            l3.addView(textLabel(R.string.secondssizelabel, SSLABEL), rlp1);
            RelativeLayout.LayoutParams rlp2 =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp2.addRule(RelativeLayout.BELOW, SSLABEL);
            rlp2.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            l3.addView(textLabel("00:00", NOSECONDS), rlp2);
            RelativeLayout.LayoutParams rlp3 =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp3.addRule(RelativeLayout.BELOW, SSLABEL);
            rlp3.addRule(RelativeLayout.CENTER_HORIZONTAL);
            l3.addView(l2, rlp3);
            RelativeLayout.LayoutParams rlp4 =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp4.addRule(RelativeLayout.BELOW, SSLABEL);
            rlp4.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            l3.addView(textLabel("00:00:00", LARGESECONDS), rlp4);
            RelativeLayout.LayoutParams rlp5 =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp5.addRule(RelativeLayout.BELOW, LARGESECONDS);
            rlp5.addRule(RelativeLayout.BELOW, SMALLSECONDS);
            rlp5.addRule(RelativeLayout.BELOW, NOSECONDS);
            m_secondsSizer.setRotation(0F);
            l3.addView(m_secondsSizer, rlp5);
            RelativeLayout.LayoutParams rlp6 =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp6.addRule(RelativeLayout.BELOW, SECONDSSIZER);
            rlp6.addRule(RelativeLayout.CENTER_HORIZONTAL);
            l3.addView(textLabel(R.string.minbright, BRLABEL), rlp6);
            RelativeLayout.LayoutParams rlp7 =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp7.addRule(RelativeLayout.BELOW, BRLABEL);
            l3.addView(m_brightness, rlp7);
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(1, 1),
                GridLayout.spec(0, 1)
            );
            layoutParams.width = width / 2;
            gl.addView(l3, -1, layoutParams);
            // remaining controls in right half
            ScrollView l5 = new ScrollView(this);
            l5.setScrollbarFadingEnabled(false);
            LinearLayout l6 = new LinearLayout(this);
            l6.setOrientation(LinearLayout.VERTICAL);
            l6.addView(m_helptext);
            l6.addView(l1);
            l5.addView(l6);
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(0, 2, 1f),
                GridLayout.spec(1, 1, 1f)
            );
            layoutParams.width = 0;
            layoutParams.height = 0;
            gl.addView(l5, -1, layoutParams);
        } else { // assume PORTRAIT
            // Vertical seconds size slider at top right
            LinearLayout l3 = new LinearLayout(this);
            l3.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams l3pars1 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            l3pars1.gravity = Gravity.CENTER_HORIZONTAL;
            l3.addView(textLabel(R.string.secondssizelabel, SSLABEL), l3pars1);
            LinearLayout l4 = new LinearLayout(this);
            // default orientation is HORIZONTAL
            LinearLayout.LayoutParams l4pars = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
            m_secondsSizer.setDirection(MySeekBar.SEEKBAR_UPWARDS);
            l4.addView(m_secondsSizer, l4pars);
            RelativeLayout l5 = new RelativeLayout(this);
            RelativeLayout.LayoutParams rlp1 =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            rlp1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            l5.addView(textLabel("00:00:00", LARGESECONDS), rlp1);
            RelativeLayout.LayoutParams rlp2 =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp2.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            rlp2.addRule(RelativeLayout.CENTER_VERTICAL);
            l5.addView(l2, rlp2);
            RelativeLayout.LayoutParams rlp3 =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp3.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            rlp3.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            l5.addView(textLabel("00:00", NOSECONDS), rlp3);
            l4.addView(l5, l4pars);
            LinearLayout.LayoutParams l3pars2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            l3pars2.weight = 1F;
            l3.addView(l4, l3pars2);
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(0, 1),
                GridLayout.spec(1, 1, 1F)
            );
            layoutParams.height = height / 2;
            gl.addView(l3, -1, layoutParams);
            // remaining controls in bottom half
            ScrollView l6 = new ScrollView(this);
            l6.setScrollbarFadingEnabled(false);
            l6.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
            LinearLayout l7 = new LinearLayout(this);
            l7.setOrientation(LinearLayout.VERTICAL);
            l7.addView(m_helptext);
            LinearLayout.LayoutParams l7pars1 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            l7pars1.gravity = Gravity.CENTER_HORIZONTAL;
            l7.addView(textLabel(R.string.minbright, BRLABEL), l7pars1);
            LinearLayout.LayoutParams l7pars2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            l7.addView(m_brightness, l7pars2);
            l7.addView(l1);
            l6.addView(l7);
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(1, 1, 1f),
                GridLayout.spec(0, 2, 1f)
            );
            layoutParams.width = 0;
            layoutParams.height = 0;
            gl.addView(l6, -1, layoutParams);
        }
        m_topLayout.addView(gl);
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
        m_secondsSizer = new MySeekBar(this);
        m_secondsSizer.setMax(255);
        m_secondsSizer.setId(SECONDSSIZER);
        m_secondsSizer.setOnChangeListener(this);
        m_secondsSizer.setOnLongClickListener(this);
        m_secondsSizer.setBackgroundColor(0xFF000000);
        m_secondsSizer.setProgress(m_prefs.getInt("CsecondsSize", 255));
        m_brightness = new MySeekBar(this);
        m_brightness.setMax(255);
        m_brightness.setId(BRIGHTNESS);
        m_brightness.setOnChangeListener(this);
        m_brightness.setOnLongClickListener(this);
        m_brightness.setProgress(m_prefs.getInt("Cbrightness", 255));
        ColorStateList cl = ColorStateList.valueOf(0xFFFFFFFF);
        m_brightness.setTrackTintList(cl);
        m_brightness.setThumbTintList(cl);
        int fgColour = m_prefs.getInt("CfgColour", 0xFFFFFFFF);
        int userbright = Settings.System.getInt(
            getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        m_brightness.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {0xFF000000,  (userbright << 24) | (fgColour & 0xFFFFFF)}));
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
