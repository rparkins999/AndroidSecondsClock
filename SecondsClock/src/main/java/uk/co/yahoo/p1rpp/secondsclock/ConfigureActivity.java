/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is common logic for WidgetConfigureActivity and ClockConfigureActivity.
 * It doesn't need to be in the manifest because it is an abstract class
 * and no instance of it can ever be created.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

abstract class ConfigureActivity extends Activity_common
    implements View.OnLongClickListener, CompoundButton.OnCheckedChangeListener
{

    protected String m_CorW;
    protected int currentView;
    protected Configuration m_config;
    protected DisplayMetrics m_metrics;
    protected CheckBox showShortWeekDayCheckBox;
    protected CheckBox showLongWeekDayCheckBox;
    protected CheckBox showShortDateCheckBox;
    protected CheckBox showMonthDayCheckBox;
    protected CheckBox showShortMonthCheckBox;
    protected CheckBox showLongMonthCheckBox;
    protected CheckBox showYearCheckBox;

    protected static final int CONFIGURE = 0;
    protected static final int SETTEXTCOLOUR = 1;

    protected static final int SHORTWEEKDAY = 666;
    protected static final int LONGWEEKDAY = SHORTWEEKDAY + 1;
    protected static final int SHORTDATE = LONGWEEKDAY + 1;
    protected static final int MONTHDAY = SHORTDATE + 1;
    protected static final int SHORTMONTH = MONTHDAY + 1;
    protected static final int LONGMONTH = SHORTMONTH + 1;
    protected static final int SHOWYEAR = LONGMONTH + 1;

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case SHORTWEEKDAY:
                doToast(R.string.showshortweekdayhelp, m_CorW); return true;
            case LONGWEEKDAY:
                doToast(R.string.showlongweekdayhelp, m_CorW); return true;
            case SHORTDATE:
                doToast(R.string.showshortdatehelp, m_CorW); return true;
            case MONTHDAY:
                doToast(R.string.showmonthdayhelp, m_CorW); return true;
            case SHORTMONTH:
                doToast(R.string.showshortmonthhelp, m_CorW); return true;
            case LONGMONTH:
                doToast(R.string.showlongmonthhelp, m_CorW); return true;
            case SHOWYEAR:
                doToast(R.string.showyearhelp, m_CorW); return true;
        }
        return false;
    }

    protected abstract void updateFromCheckBox();

    @SuppressLint("ApplySharedPref")
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case SHORTWEEKDAY:
                if (isChecked) {
                    showLongWeekDayCheckBox.setChecked(false);
                    showWeekDay = 1;
                } else {
                    showWeekDay = 0;
                }
                m_prefs.edit().putInt(m_key + "showWeekDay", showWeekDay).commit();
                updateFromCheckBox();
                break;
            case LONGWEEKDAY:
                if (isChecked) {
                    showShortWeekDayCheckBox.setChecked(false);
                    showWeekDay = 2;
                } else {
                    showWeekDay = 0;
                }
                m_prefs.edit().putInt(m_key + "showWeekDay", showWeekDay).commit();
                updateFromCheckBox();
                break;
            case SHORTDATE:
                if (isChecked) {
                    showShortDate = 1;
                    showMonthDayCheckBox.setChecked(false);
                    showShortMonthCheckBox.setChecked(false);
                    showLongMonthCheckBox.setChecked(false);
                    showYearCheckBox.setChecked(false);
                    showYearCheckBox.setVisibility(View.GONE);
                    showMonthDayCheckBox.setVisibility(View.GONE);
                    showShortMonthCheckBox.setVisibility(View.GONE);
                    showLongMonthCheckBox.setVisibility(View.GONE);
                    showYearCheckBox.setVisibility(View.GONE);
                } else {
                    showShortDate = 0;
                    showMonthDayCheckBox.setVisibility(View.VISIBLE);
                    showShortMonthCheckBox.setVisibility(View.VISIBLE);
                    showLongMonthCheckBox.setVisibility(View.VISIBLE);
                    showYearCheckBox.setVisibility(View.VISIBLE);
                }
                m_prefs.edit().putInt(m_key + "showShortDate", showShortDate).commit();
                m_prefs.edit().putInt(m_key + "showMonthDay", showMonthDay).commit();
                m_prefs.edit().putInt(m_key + "showMonth", showMonth).commit();
                m_prefs.edit().putInt(m_key + "showYear", showYear).commit();
                updateFromCheckBox();
                break;
            case MONTHDAY:
                showMonthDay = isChecked ? 1: 0;
                m_prefs.edit().putInt(m_key + "showMonthDay", showMonthDay).commit();
                updateFromCheckBox();
                break;
            case SHORTMONTH:
                if (isChecked) {
                    showLongMonthCheckBox.setChecked(false);
                    showMonth = 1;
                } else {
                    showMonth = 0;
                }
                m_prefs.edit().putInt(m_key + "showMonth", showMonth).commit();
                updateFromCheckBox();
                break;
            case LONGMONTH:
                if (isChecked) {
                    showShortMonthCheckBox.setChecked(false);
                    showMonth = 2;
                } else {
                    showMonth = 0;
                }
                m_prefs.edit().putInt(m_key + "showMonth", showMonth).commit();
                updateFromCheckBox();
                break;
            case SHOWYEAR:
                showYear = isChecked ? 1: 0;
                m_prefs.edit().putInt(m_key + "showYear", showMonth).commit();
                updateFromCheckBox();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generic_layout);
    }

    protected void doMainLayout() {
        removeAllViews(m_topLayout);
    }

    protected void doChooserLayout() {
        removeAllViews(m_topLayout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Resources res = getResources();
        m_metrics = res.getDisplayMetrics();
        m_config = res.getConfiguration();
        showShortWeekDayCheckBox = new CheckBox(this);
        showShortWeekDayCheckBox.setId(SHORTWEEKDAY);
        showShortWeekDayCheckBox.setChecked(showWeekDay == 1);
        showShortWeekDayCheckBox.setText(getString(R.string.show_short_weekday, m_CorW));
        showShortWeekDayCheckBox.setOnCheckedChangeListener(this);
        showShortWeekDayCheckBox.setOnLongClickListener(this);
        showLongWeekDayCheckBox = new CheckBox(this);
        showLongWeekDayCheckBox.setId(LONGWEEKDAY);
        showLongWeekDayCheckBox.setText(getString(R.string.show_long_weekday, m_CorW));
        showLongWeekDayCheckBox.setChecked(showWeekDay == 2);
        showLongWeekDayCheckBox.setOnCheckedChangeListener(this);
        showLongWeekDayCheckBox.setOnLongClickListener(this);
        showShortDateCheckBox = new CheckBox(this);
        showShortDateCheckBox.setId(SHORTDATE);
        showShortDateCheckBox.setChecked(showShortDate != 0);
        showShortDateCheckBox.setOnCheckedChangeListener(this);
        showShortDateCheckBox.setText(getString(R.string.show_short_date, m_CorW));
        showShortDateCheckBox.setOnLongClickListener(this);
        showMonthDayCheckBox = new CheckBox(this);
        showMonthDayCheckBox.setId(MONTHDAY);
        showMonthDayCheckBox.setChecked(showMonthDay != 0);
        showMonthDayCheckBox.setOnCheckedChangeListener(this);
        showMonthDayCheckBox.setText(getString(R.string.show_month_day, m_CorW));
        showMonthDayCheckBox.setOnLongClickListener(this);
        showShortMonthCheckBox = new CheckBox(this);
        showShortMonthCheckBox.setId(SHORTMONTH);
        showShortMonthCheckBox.setChecked(showMonth == 1);
        showShortMonthCheckBox.setOnCheckedChangeListener(this);
        showShortMonthCheckBox.setText(getString(R.string.show_short_month, m_CorW));
        showShortMonthCheckBox.setOnLongClickListener(this);
        showLongMonthCheckBox = new CheckBox(this);
        showLongMonthCheckBox.setId(LONGMONTH);
        showLongMonthCheckBox.setChecked(showMonth == 2);
        showLongMonthCheckBox.setOnCheckedChangeListener(this);
        showLongMonthCheckBox.setText(getString(R.string.show_long_month, m_CorW));
        showLongMonthCheckBox.setOnLongClickListener(this);
        showYearCheckBox = new CheckBox(this);
        showYearCheckBox.setId(SHOWYEAR);
        showYearCheckBox.setChecked(showYear != 0);
        showYearCheckBox.setOnCheckedChangeListener(this);
        showYearCheckBox.setText(getString(R.string.show_year, m_CorW));
        showYearCheckBox.setOnLongClickListener(this);
        if (showShortDate != 0) {
            showMonthDayCheckBox.setVisibility(View.GONE);
            showShortMonthCheckBox.setVisibility(View.GONE);
            showLongMonthCheckBox.setVisibility(View.GONE);
            showYearCheckBox.setVisibility(View.GONE);
        }
    }

    protected abstract void setCurrentView(int viewnum);

    @Override
    public void onBackPressed() {
        if (currentView == CONFIGURE) {
            super.onBackPressed();
        } else {
            setCurrentView(CONFIGURE);
        }
    }
}
