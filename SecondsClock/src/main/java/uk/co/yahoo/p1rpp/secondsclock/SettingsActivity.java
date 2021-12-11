/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class handles the settings for SecondsClockWidget.
 * It also shows what the widget will look like in 1x1 size
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private AppWidgetManager appWidgetManager;
    private ComponentName secondsClockWidget;
    private int maxHeight = 121;
    private int minWidth = 93;
    private SeekBar alphaSlider;
    private SeekBar redSlider;
    private SeekBar greenSlider;
    private SeekBar blueSlider;
    private int showTime;
    private int showWeekDay;
    private int showShortDate;
    private int showMonthDay;
    private int showMonth;
    private int showYear;
    private TextClock demo;
    private DisplayMetrics metrics;
    private int currentView;
        static final int SETWIDGETBACKGROUNDCOLOUR = 0;
        static final int SETWIDGETTEXTCOLOUR = 1;
        static final int CONFIGUREWIDGET = 2;
    private int m_bgcolour;
    private SharedPreferences prefs;
    private int m_fgcolour;
    private BitmapWrapper m_activeWrapper = null;
    private int multiplier;
    private Configuration config;
    private TextView helptext;
    private FrameLayout topLayout;
    private LinearLayout demobox;
    private LinearLayout demoboxbox;
    private CheckBox showTimeCheckBox;
    private CheckBox showSecondsCheckBox;
    private CheckBox showShortWeekDayCheckBox;
    private CheckBox showLongWeekDayCheckBox;
    private CheckBox showShortDateCheckBox;
    private CheckBox showMonthDayCheckBox;
    private CheckBox showShortMonthCheckBox;
    private CheckBox showLongMonthCheckBox;
    private CheckBox showYearCheckBox;
    private Button bgButton;
    private Button fgButton;
    private Bitmap m_bitmap;
    private SettingsActivity ac;
    private boolean colourmaplongclicked = false;
    private Button okButton;
    private static final int[][] EMPTY = new int[][] { new int[0] };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
    }

    private void updateWidget() {
        int[] widgetIds = appWidgetManager.getAppWidgetIds(secondsClockWidget);
        if (widgetIds.length > 0) {
            Bundle newOptions =
                appWidgetManager.getAppWidgetOptions(widgetIds[0]);
            maxHeight = newOptions.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight);
            minWidth = newOptions.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidth);
        }
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.setClassName("uk.co.yahoo.p1rpp.secondsclock",
            "uk.co.yahoo.p1rpp.secondsclock.SecondsClockWidget");
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        sendBroadcast(intent);
    }

    private void setSliders(int colour) {
        int val = (colour >> 24) & 0xFF;
        alphaSlider.setProgress(val);
        //alphaValue.setText(Integer.toString(val));
        val = (colour >> 16) & 0xFF;
        //redValue.setText(Integer.toString(val));
        redSlider.setProgress(val);
        val = (colour >> 8) & 0xFF;
        //greenValue.setText(Integer.toString(val));
        greenSlider.setProgress(val);
        val = colour & 0xFF;
        //blueValue.setText(Integer.toString(val));
        blueSlider.setProgress(val);
    }

    void updateDemo() {
        Formatter f = new Formatter();
        f.set(this, minWidth, maxHeight,
            showTime, showWeekDay, showShortDate,
            showMonthDay, showMonth, showYear);
        int width = (int)(minWidth * metrics.density);
        int height = (int)(maxHeight * metrics.density);
        demo.setMinimumWidth(width);
        demo.setMaxWidth(width);
        demo.setMinimumHeight(height);
        demo.setMaxHeight(height);
        demo.setFormat12Hour(f.time12 + f.rest);
        demo.setFormat24Hour(f.time24 + f.rest);
        demo.setLines(f.lines);
        demo.setAutoSizeTextTypeWithDefaults(
            TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
    }

    @SuppressLint("ApplySharedPref")
    private void setColour(int colour) {
        switch (currentView) {
            case SETWIDGETBACKGROUNDCOLOUR:
                m_bgcolour = colour;
                demo.setBackgroundColor(m_bgcolour);
                prefs.edit().putInt("Wbgcolour", colour).commit();
                updateDemo();
                setSliders(colour);
                break;
            case SETWIDGETTEXTCOLOUR:
                m_fgcolour = colour;
                demo.setTextColor(m_fgcolour);
                prefs.edit().putInt("Wfgcolour", colour).commit();
                setSliders(colour);
                updateDemo();
                break;
            case CONFIGUREWIDGET:
        }
        updateWidget();
    }

    private void fillBitmapInBackground(int colour) {
        FillBitmap task;
        if (m_activeWrapper != null) {
            task = m_activeWrapper.m_active;
            if (task != null) {
                task.cancel(true);
            }
        }
        m_activeWrapper = new BitmapWrapper(colour, m_bitmap, multiplier);
        task = new FillBitmap();
        m_activeWrapper.m_active = task;
        task.execute(m_activeWrapper);
    }

    void setMultiplier() {
        int size;
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            size = metrics.heightPixels;
            int sb = 72;
            Resources res = getResources();
            int resourceId = res.getIdentifier(
                "status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                sb = res.getDimensionPixelSize(resourceId);
            }
            int hh = helptext.getBottom();
            size -= sb + hh;
        } else {
            size = metrics.widthPixels;
        }
        if (size < 512) {
            multiplier = 1;
        } else if (size < 1024) {
            multiplier = 2;
        } else {
            multiplier = 4;
        }
    }

    // recursive version
    void removeAllViews(View v) {
        if (v instanceof ViewGroup) {
            int n = ((ViewGroup)v).getChildCount();
            for ( int i = 0; i < n; ++i) {
                removeAllViews(((ViewGroup) v).getChildAt(i));
            }
            ((ViewGroup)v).removeAllViews();
        }
    }

    void doMainLayout() {
        removeAllViews(topLayout);
        LinearLayout l1 = new LinearLayout(this);
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            l1.setOrientation(LinearLayout.HORIZONTAL);
            demoboxbox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_VERTICAL);
            l1.addView(demoboxbox);
            LinearLayout l2 = new LinearLayout(this);
            l2.setOrientation(LinearLayout.VERTICAL);
            l2.addView(helptext);
            LinearLayout l3 = new LinearLayout(this);
            l3.setOrientation(LinearLayout.HORIZONTAL);
            l3.addView(showTimeCheckBox);
            l3.addView(showSecondsCheckBox);
            l2.addView(l3);
            LinearLayout l4 = new LinearLayout(this);
            l4.setOrientation(LinearLayout.HORIZONTAL);
            l4.addView(showShortWeekDayCheckBox);
            l4.addView(showLongWeekDayCheckBox);
            l2.addView(l4);
            LinearLayout l5 = new LinearLayout(this);
            l5.setOrientation(LinearLayout.HORIZONTAL);
            l5.addView(showShortDateCheckBox);
            l5.addView(showMonthDayCheckBox);
            l2.addView(l5);
            LinearLayout l6 = new LinearLayout(this);
            l6.setOrientation(LinearLayout.HORIZONTAL);
            l6.addView(showShortMonthCheckBox);
            l6.addView(showLongMonthCheckBox);
            l2.addView(l6);
            l2.addView(showYearCheckBox);
            LinearLayout l7 = new LinearLayout(this);
            l7.setOrientation(LinearLayout.HORIZONTAL);
            l7.addView(bgButton);
            l7.addView(fgButton);
            l2.addView(l7);
            l1.addView(l2);
        } else { // assume Portrait
            l1.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_HORIZONTAL);
            l1.addView(demoboxbox);
            l1.addView(helptext);
            helptext.setGravity(Gravity.START);
            l1.addView(showTimeCheckBox);
            showTimeCheckBox.setGravity(Gravity.START);
            l1.addView(showSecondsCheckBox);
            showSecondsCheckBox.setGravity(Gravity.START);
            l1.addView(showShortWeekDayCheckBox);
            showShortWeekDayCheckBox.setGravity(Gravity.START);
            l1.addView(showLongWeekDayCheckBox);
            showLongWeekDayCheckBox.setGravity(Gravity.START);
            l1.addView(showShortDateCheckBox);
            showShortDateCheckBox.setGravity(Gravity.START);
            l1.addView(showMonthDayCheckBox);
            showMonthDayCheckBox.setGravity(Gravity.START);
            l1.addView(showShortMonthCheckBox);
            showShortMonthCheckBox.setGravity(Gravity.START);
            l1.addView(showLongMonthCheckBox);
            showLongMonthCheckBox.setGravity(Gravity.START);
            l1.addView(showYearCheckBox);
            showYearCheckBox.setGravity(Gravity.START);
            LinearLayout l2 = new LinearLayout(this);
            l2.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            l2.setOrientation(LinearLayout.VERTICAL);
            l2.setGravity(Gravity.CENTER_HORIZONTAL);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            l2.addView(bgButton, lp);
            l2.addView(fgButton, lp);
            l1.addView(l2);
            /* debugging
            Button testButton = new Button(this);
            testButton.setText("Test going to system clock app");
            testButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        startActivity(new Intent(
                            "android.intent.action.SHOW_TIMERS"));
                    } catch (ActivityNotFoundException ignore) {
                        Toast.makeText(ac, "ActivityNotFoundException",
                            Toast.LENGTH_LONG).show();
                    }
                }
            });
            l1.addView(testButton);
             */ // end of debugging code
        }
        topLayout.addView(l1);
    }

    @SuppressLint("ClickableViewAccessibility")
    void doChooserLayout() {
        setMultiplier();
        /* These have to be created to order because their sizes can depend
         * on whether the device is in portrait or landscape orientation.
         */
        int size = 256 * multiplier;
        m_bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
            fillBitmapInBackground(m_bgcolour);
        } else {
            fillBitmapInBackground(m_fgcolour);
            setColour(m_fgcolour);
        }
        ImageView colourmap = new ImageView(this);
        colourmap.setAdjustViewBounds(true);
        colourmap.setMaxWidth(size);
        colourmap.setMaxHeight(size);
        colourmap.setImageBitmap(m_bitmap);
        colourmap.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (  (event.getAction() == MotionEvent.ACTION_UP)
                    && !colourmaplongclicked)
                {
                    setColour(m_bitmap.getPixel(
                        (int)event.getX(), (int)event.getY()));
                    return true;
                } else {
                    colourmaplongclicked = false;
                    return false;
                }
            }
        });
        colourmap.setLongClickable(true);
        colourmap.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                colourmaplongclicked = true;
                Toast.makeText(ac, getString((
                    currentView == SETWIDGETTEXTCOLOUR)
                        ? R.string.fghelp : R.string.bghelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        int colour = (currentView == SETWIDGETTEXTCOLOUR)
            ? m_fgcolour : m_bgcolour;
        removeAllViews(topLayout);
        alphaSlider.setProgress((colour >> 24) & 0xFF);
        redSlider.setProgress((colour >> 16) & 0xFF);
        greenSlider.setProgress((colour >> 8) & 0xFF);
        blueSlider.setProgress(colour & 0xFF);
        LinearLayout l1 = new LinearLayout(this);
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            l1.setOrientation(LinearLayout.HORIZONTAL);
            demoboxbox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_VERTICAL);
            l1.addView(demoboxbox);
            LinearLayout l2 = new LinearLayout(this);
            l2.setOrientation(LinearLayout.HORIZONTAL);
            l2.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
            l2.setGravity(Gravity.CENTER_VERTICAL);
            l2.addView(colourmap);
            l1.addView(l2);
            LinearLayout l3 = new LinearLayout(this);
            l3.setOrientation(LinearLayout.VERTICAL);
            l3.addView(helptext);
            l3.addView(redSlider);
            l3.addView(greenSlider);
            l3.addView(blueSlider);
            if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                l3.addView(alphaSlider);
            }
            l1.addView(l3);
        } else { // assume Portrait
            l1.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_HORIZONTAL);
            l1.addView(demoboxbox);
            l1.addView(helptext);
            LinearLayout l2 = new LinearLayout(this);
            l2.setOrientation(LinearLayout.VERTICAL);
            l2.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            l2.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout l3 = new LinearLayout(this);
            l3.setOrientation(LinearLayout.VERTICAL);
            l3.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            l3.addView(colourmap);
            l2.addView(l3);
            l1.addView(l2);
            l1.addView(redSlider);
            l1.addView(greenSlider);
            l1.addView(blueSlider);
            if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                l1.addView(alphaSlider);
            }
            LinearLayout l4 = new LinearLayout(this);
            l4.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            l4.setOrientation(LinearLayout.VERTICAL);
            l4.setGravity(Gravity.CENTER_HORIZONTAL);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            l4.addView(okButton, lp);
            l1.addView(l4);
        }
        topLayout.addView(l1);
    }

    @Override
    @SuppressLint({"ApplySharedPref", "RtlHardcoded", "SetTextI18n"})
    protected void onResume() {
        super.onResume();
        Resources res = getResources();
        metrics = res.getDisplayMetrics();
        config = res.getConfiguration();
        secondsClockWidget = new ComponentName(
            getApplicationContext(), SecondsClockWidget.class);
        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        ac = this;
        prefs = getSharedPreferences("SecondsClock", Context.MODE_PRIVATE);
        showTime = prefs.getInt("WshowTime", 2); // include seconds
        showWeekDay = prefs.getInt("WshowWeekDay",2); // long format
        showShortDate = prefs.getInt("WshowShortDate",0);
        showMonthDay = prefs.getInt("WshowMonthDay",1);
        showMonth = prefs.getInt("WshowMonth", 2); // long format
        showYear = prefs.getInt("WshowYear", 1);
        m_bgcolour = prefs.getInt("Wbgcolour", 0x00000000);
        m_fgcolour = prefs.getInt("Wfgcolour",0xFFFFFFFF);
        currentView = prefs.getInt("Wview", CONFIGUREWIDGET);

        demo = new TextClock(this);
        demo.setGravity(Gravity.CENTER_HORIZONTAL);
        demo.setBackgroundColor(m_bgcolour);
        demo.setTextColor(m_fgcolour);
        demobox = new LinearLayout(this);
        demobox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        demobox.addView(demo);
        demobox.setBackgroundResource(R.drawable.background);
        demoboxbox = new LinearLayout(this);
        demoboxbox.setPadding(10, 0, 10, 0);
        demoboxbox.addView(demobox);
        helptext = new TextView(this);
        showTimeCheckBox = new CheckBox(this);
        showSecondsCheckBox = new CheckBox(this);
        showShortWeekDayCheckBox = new CheckBox(this);
        showLongWeekDayCheckBox = new CheckBox(this);
        showShortDateCheckBox = new CheckBox(this);
        showMonthDayCheckBox = new CheckBox(this);
        showShortMonthCheckBox = new CheckBox(this);
        showLongMonthCheckBox = new CheckBox(this);
        showYearCheckBox = new CheckBox(this);
        bgButton = new Button(this);
        fgButton = new Button(this);
        alphaSlider = new SeekBar(this);
        redSlider = new SeekBar(this);
        greenSlider = new SeekBar(this);
        blueSlider = new SeekBar(this);
        okButton = new Button(this);

        String s = "";
        try
        {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName (), 0);
            s = getString(R.string.app_name) + " " + pi.versionName
                + " built " + getString(R.string.build_time) + "\n";
        } catch (PackageManager.NameNotFoundException ignore) {}
        helptext.setText(s + getString(R.string.longpresslabel));
        helptext.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.editwidgethelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        demo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.demohelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        showTimeCheckBox.setText(R.string.show_time);
        showTimeCheckBox.setChecked(showTime != 0);
        showTimeCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showTime = 1;
                        showSecondsCheckBox.setChecked(false);
                        showSecondsCheckBox.setVisibility(View.VISIBLE);
                    } else {
                        showTime = 0;
                        if (config.orientation !=
                            Configuration.ORIENTATION_LANDSCAPE)
                        {
                            showSecondsCheckBox.setVisibility(View.GONE);
                        } else {
                            showSecondsCheckBox.setVisibility(View.INVISIBLE);
                        }
                    }
                    prefs.edit().putInt("WshowTime", showTime).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showTimeCheckBox.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.showtimehelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        showSecondsCheckBox.setText(R.string.show_seconds);
        showSecondsCheckBox.setChecked(showTime == 2);
        showSecondsCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showTime = 2;
                    } else {
                        showTime = 1;
                    }
                    prefs.edit().putInt("WshowTime", showTime).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showSecondsCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showsecondshelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showShortWeekDayCheckBox.setText(R.string.show_short_weekday);
        showShortWeekDayCheckBox.setChecked(showWeekDay == 1);
        showShortWeekDayCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showLongWeekDayCheckBox.setChecked(false);
                        showWeekDay = 1;
                    } else {
                        showWeekDay = 0;
                    }
                    prefs.edit().putInt("WshowWeekDay", showWeekDay).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showShortWeekDayCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showshortweekdayhelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showLongWeekDayCheckBox.setText(R.string.show_long_weekday);
        showLongWeekDayCheckBox.setChecked(showWeekDay == 2);
        showLongWeekDayCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showShortWeekDayCheckBox.setChecked(false);
                        showWeekDay = 2;
                    } else {
                        showWeekDay = 0;
                    }
                    prefs.edit().putInt("WshowWeekDay", showWeekDay).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showLongWeekDayCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showlongweekdayhelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showShortDateCheckBox.setText(R.string.show_short_date);
        showShortDateCheckBox.setChecked(showShortDate != 0);
        showShortDateCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
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
                    prefs.edit().putInt(
                        "showShortDate", showShortDate).commit();
                    prefs.edit().putInt("WshowMonthDay", showMonthDay).commit();
                    prefs.edit().putInt("WshowMonth", showMonth).commit();
                    prefs.edit().putInt("WshowYear", showYear).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showShortDateCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showshortdatehelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showMonthDayCheckBox.setText(R.string.show_month_day);
        showMonthDayCheckBox.setChecked(showMonthDay != 0);
        showMonthDayCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showMonthDay = 1;
                    } else {
                        showMonthDay = 0;
                    }
                    prefs.edit().putInt("WshowMonthDay", showMonthDay).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showMonthDayCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showmonthdayhelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showShortMonthCheckBox.setText(R.string.show_short_month);
        showShortMonthCheckBox.setChecked(showMonth == 1);
        showShortMonthCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showLongMonthCheckBox.setChecked(false);
                        showMonth = 1;
                    } else {
                        showMonth = 0;
                    }
                    prefs.edit().putInt("WshowMonth", showMonth).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showShortMonthCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showshortmonthhelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showLongMonthCheckBox.setText(R.string.show_long_month);
        showLongMonthCheckBox.setChecked(showMonth == 2);
        showLongMonthCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showShortMonthCheckBox.setChecked(false);
                        showMonth = 2;
                    } else {
                        showMonth = 0;
                    }
                    prefs.edit().putInt("WshowMonth", showMonth).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showLongMonthCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showlongmonthhelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showYearCheckBox.setText(R.string.show_year);
        showYearCheckBox.setChecked(showYear != 0);
        showYearCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showYear = 1;
                    } else {
                        showYear = 0;
                    }
                    prefs.edit().putInt("WshowYear", showYear).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showYearCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showyearhelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        bgButton.setText(R.string.set_bg_colour);
        bgButton.setAllCaps(false);
        bgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentView = SETWIDGETBACKGROUNDCOLOUR;
                prefs.edit().putInt("Wview", currentView).commit();
                doChooserLayout();
            }
        });
        bgButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.setbgcolourhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        fgButton.setAllCaps(false);
        fgButton.setText(R.string.set_fg_colour);
        fgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentView = SETWIDGETTEXTCOLOUR;
                prefs.edit().putInt("Wview", currentView).commit();
                doChooserLayout();
            }
        });
        fgButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.setfgcolourhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        alphaSlider.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser)
                {
                    if (fromUser) {
                        int val = seekBar.getProgress();
                        //alphaValue.setText(Integer.toString(val));
                        m_bgcolour = (val << 24) + (m_bgcolour & 0xFFFFFF);
                        fillBitmapInBackground(m_bgcolour);
                        demo.setBackgroundColor(m_bgcolour);
                        prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                        updateWidget();
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        alphaSlider.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.setalphasliderhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        alphaSlider.setMax(255);
        ColorStateList cl = ColorStateList.valueOf(0xFF888888);
        alphaSlider.setProgressTintList(cl);
        alphaSlider.setThumbTintList(cl);
        redSlider.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser)
                {
                    if (fromUser) {
                        int val = seekBar.getProgress();
                        //redValue.setText(Integer.toString(val));
                        if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                            m_bgcolour = (val << 16) + (m_bgcolour & 0xFF00FFFF);
                            fillBitmapInBackground(m_bgcolour);
                            demo.setBackgroundColor(m_bgcolour);
                            prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                        }
                        // no else, can only set opacity for background colour
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        redSlider.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.setredsliderhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        redSlider.setMax(255);
        cl = new ColorStateList(EMPTY, new int[] { 0xFFFF0000 });
        redSlider.setProgressTintList(cl);
        redSlider.setThumbTintList(cl);
        greenSlider.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser)
                {
                    if (fromUser) {
                        int val = seekBar.getProgress();
                        //greenValue.setText(Integer.toString(val));
                        if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                            m_bgcolour = (val << 8) + (m_bgcolour & 0xFFFF00FF);
                            fillBitmapInBackground(m_bgcolour);
                            demo.setBackgroundColor(m_bgcolour);
                            prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                        } else {
                            m_fgcolour = (val << 8) + (m_fgcolour & 0xFFFF00FF);
                            fillBitmapInBackground(m_fgcolour);
                            demo.setTextColor(m_fgcolour);
                            prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
                        }
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        greenSlider.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.setgreensliderhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        greenSlider.setMax(255);
        cl = new ColorStateList(EMPTY, new int[] { 0xFF00FF00 });
        greenSlider.setProgressTintList(cl);
        greenSlider.setThumbTintList(cl);
        blueSlider.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser)
                {
                    if (fromUser) {
                        int val = seekBar.getProgress();
                        //blueValue.setText(Integer.toString(val));
                        int colour;
                        if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                            m_bgcolour = val + (m_bgcolour & 0xFFFFFF00);
                            fillBitmapInBackground(m_bgcolour);
                            demo.setBackgroundColor(m_bgcolour);
                            prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                        } else {
                            m_fgcolour = val + (m_fgcolour & 0xFFFFFF00);
                            fillBitmapInBackground(m_fgcolour);
                            demo.setTextColor(m_fgcolour);
                            fillBitmapInBackground(m_fgcolour);
                            prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
                        }
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        blueSlider.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.setbluesliderhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        blueSlider.setMax(255);
        cl = new ColorStateList(EMPTY, new int[] { 0xFF0000FF });
        blueSlider.setProgressTintList(cl);
        blueSlider.setThumbTintList(cl);
        updateWidget();
        updateDemo();

        topLayout = findViewById(R.id.settingslayout);
        switch (currentView) {
            case SETWIDGETBACKGROUNDCOLOUR:
            case SETWIDGETTEXTCOLOUR:
                doChooserLayout();
                break;
            case CONFIGUREWIDGET:
                doMainLayout();
                break;
        }
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onBackPressed() {
        switch (currentView) {
            case SETWIDGETBACKGROUNDCOLOUR:
            case SETWIDGETTEXTCOLOUR:
                currentView = CONFIGUREWIDGET;
                prefs.edit().putInt("Wview", currentView).commit();
                doMainLayout();
                break;
            case CONFIGUREWIDGET:
                super.onBackPressed();
                break;
        }
    }
}
