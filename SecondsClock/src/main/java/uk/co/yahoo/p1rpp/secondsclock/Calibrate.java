/*
 * Copyright © 2022. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class calibrates the map from global screen brightness
 * and application screen brightness to opacity.
 * See the comment on setOpacity() in ClockView.java for a description
 * of why this is needed.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArraySet;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

class Calibrate extends Activity_common
    implements View.OnLongClickListener, View.OnClickListener,
    Slider.OnValueChangeListener
{
    protected static final int CONFIGURE = 0; // m_currentView default value
    protected static final int HELPSCREEN = CONFIGURE + 1;

    protected static final int HELPBUTTON = VIEWIDBASE;
    protected static final int SAVEBUTTON = HELPBUTTON + 1;
    protected static final int RESETBUTTON = SAVEBUTTON + 1;
    protected static final int IMAGEVIEW = RESETBUTTON + 1;
    protected static final int OPACITY = IMAGEVIEW + 1;
    protected static final int APPBRIGHT = OPACITY + 1;
    protected static final int GLOBRIGHT = APPBRIGHT + 1;

    protected int m_currentView;
    private final Handler m_handler = new Handler();
    private boolean m_canWrite;
    private int m_bitmapScale;
    private Bitmap m_bitmap;
    private ImageView m_imageView;
    private boolean m_flickerState = false;
    private Slider m_opacity;
    private Slider m_appbright;
    private Slider m_globright;
    public TextView m_CurrentLux;

    /* This does the flicker photometry. We switch at 16.̇6Hz between displaying
     * the bitmap at the global system brightness and the set opacity, and
     * displaying it at the set window brightness and 100% opaque (opacity 255).
     * The user can then adjust the set opacity to minimise the flicker.
     */
    private final Runnable m_ticker = new Runnable() {
        public void run() {
            m_handler.removeCallbacks(this);
            m_handler.postDelayed(this, 30); // 16.̇6Hz
            Window w = getWindow();
            WindowManager.LayoutParams lp = w.getAttributes();
            m_flickerState = ! m_flickerState;
            if (m_flickerState) {
                lp.screenBrightness = -1.0F; // use global system brightness
                m_imageView.setImageAlpha(m_opacity.getValue());
            } else {
                int brightness = m_appbright.getValue();
                lp.screenBrightness = brightness;
                int alpha;
                if (brightness >= 16) {
                    alpha = 255;
                } else {
                    alpha = (int)(255 * brightness / 16);
                }
                m_imageView.setImageAlpha(alpha);
            }
            if (!m_canWrite) {
                m_globright.setValue(Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 255));
            }
        }
    };

    private void doConfigure() {
        m_canWrite = Settings.System.canWrite(this);
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.VERTICAL);
        buttons.setGravity(Gravity.CENTER_HORIZONTAL);
        m_CurrentLux = new TextView(this);
        buttons.addView(m_CurrentLux);
        Button b = new Button(this);
        b.setId(HELPBUTTON);
        b.setOnClickListener(this);
        b.setOnLongClickListener(this);
        b.setText(R.string.help);
        buttons.addView(b);
        b = new Button(this);
        b.setId(SAVEBUTTON);
        b.setOnClickListener(this);
        b.setOnLongClickListener(this);
        b.setText(R.string.save);
        buttons.addView(b);
        b = new Button(this);
        b.setId(RESETBUTTON);
        b.setOnClickListener(this);
        b.setOnLongClickListener(this);
        b.setText(R.string.reset);
        buttons.addView(b);
        m_globright = new Slider(this);
        m_globright.setId(GLOBRIGHT);
        if (m_canWrite) {
            m_globright.setOnChangeListener(this);
        } else {
            m_globright.setEnabled(false);
        }
        m_bitmapScale = m_width;
        if (m_bitmapScale > m_height) { m_bitmapScale = m_height; }
        m_bitmapScale = m_bitmapScale / ( 3 * 256); // division rounds down
        int bmSize = m_bitmapScale * 256;
        m_bitmap = Bitmap.createBitmap(
            bmSize, bmSize, Bitmap.Config.ARGB_8888);
        m_bitmap.eraseColor(0xFFFFFFFF);
        m_imageView = new ImageView(this);
        m_imageView.setId(IMAGEVIEW);
        m_imageView.setImageBitmap(m_bitmap);
        m_opacity = new Slider(this);
        m_opacity.setId(OPACITY);
        m_appbright = new Slider(this);
        m_appbright.setId(APPBRIGHT);
        m_appbright.setDirection(Slider.SLIDER_DOWNWARDS);
        RelativeLayout rl = new RelativeLayout(this);
        RelativeLayout.LayoutParams rlp1 =
            new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        rlp1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rlp1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        rl.addView(m_imageView, rlp1);
        RelativeLayout.LayoutParams rlp2 =
            new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        rlp2.addRule(RelativeLayout.LEFT_OF, IMAGEVIEW);
        rlp2.addRule(RelativeLayout.ALIGN_TOP, IMAGEVIEW);
        rlp2.addRule(RelativeLayout.ALIGN_BOTTOM, IMAGEVIEW);
        rl.addView(m_appbright, rlp2);
        RelativeLayout.LayoutParams rlp3 =
            new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        rlp3.addRule(RelativeLayout.BELOW, IMAGEVIEW);
        rlp2.addRule(RelativeLayout.ALIGN_LEFT, IMAGEVIEW);
        rlp2.addRule(RelativeLayout.ALIGN_RIGHT, IMAGEVIEW);
        rl.addView(m_globright, rlp3);
        GridLayout gl = new GridLayout(this);
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
            GridLayout.spec(0, 1),
            GridLayout.spec(0, 1)
        );
        layoutParams.setGravity(Gravity.CENTER);
        if (m_orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutParams.width = m_width / 2;
            layoutParams.height = m_height;
            gl.addView(rl, -1, layoutParams);
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(0, 1),
                GridLayout.spec(1, 1)
            );
            layoutParams.width = m_width / 2;
            layoutParams.height = m_height;
        } else { // otherwise PORTRAIT
            layoutParams.width = m_width;
            layoutParams.height = m_height / 2;
            gl.addView(rl, -1, layoutParams);
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(1, 1),
                GridLayout.spec(0, 1)
            );
            layoutParams.width = m_width;
            layoutParams.height = m_height / 2;
        }
        gl.addView(buttons, -1, layoutParams);
        m_ticker.run();
    }

    private void doHelp() {
        ScrollView sv = new ScrollView(this);
        sv.addView(textLabel(R.string.calibrationhelp, HELPSCREEN));
        m_topLayout.addView(sv);
        m_handler.removeCallbacks(m_ticker);
    }

    protected void setCurrentView(int viewnum) {
        removeAllViews(m_topLayout);
        m_currentView = viewnum;
        if (m_currentView != CONFIGURE) {
            doConfigure();
        } else {
            doHelp();
        }
    }

    @SuppressLint("ApplySharedPref")
    private void doSave() {
        int appbright = m_appbright.getValue();
        int opacity = m_opacity.getValue();
        int y = appbright * m_bitmapScale;
        int x = opacity * m_bitmapScale;
        for (int i = 0; i < m_bitmapScale; ++i) {
            for (int j = 0; j < m_bitmapScale; ++j) {
                m_bitmap.setPixel(x + i, y + j, 0xFF000000);
            }
        }
        StringBuilder sb = new StringBuilder();
        ContentResolver cr = getContentResolver();
        int globright = Settings.System.getInt(
            cr, Settings.System.SCREEN_BRIGHTNESS, 255);
        sb.append(globright).append(",") .append(appbright).append(",")
          .append(opacity);
        ArraySet<String> as = new ArraySet<>();
        as.addAll(m_prefs.getStringSet("brightmap", null));
        as.add(sb.toString());
        m_prefs.edit().putStringSet("brightmap", as).commit();
    }

    @SuppressLint("ApplySharedPref")
    private void doReset() {
        m_bitmap.eraseColor(0xFFFFFFFF);
        m_prefs.edit().remove("brightmap").commit();
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case HELPBUTTON: setCurrentView(HELPSCREEN); break;
            case SAVEBUTTON: doToast(R.string.savehelp); break;
            case RESETBUTTON: doToast(R.string.resethelp); break;
            case GLOBRIGHT: doToast(R.string.globrighthelp); break;
            case OPACITY: doToast(R.string.opacityhelp); break;
            case APPBRIGHT: doToast(R.string.appbrighthelp); break;
            default:
                if (m_currentView == CONFIGURE) { setCurrentView(HELPSCREEN); }
                else { setCurrentView(CONFIGURE); }
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case HELPBUTTON:
                setCurrentView(HELPSCREEN);
                break;
            case SAVEBUTTON:
                doSave();
                break;
            case RESETBUTTON:
                doReset();
                break;
            default:
                if (m_currentView == CONFIGURE) {
                    setCurrentView(HELPSCREEN);
                } else {
                    setCurrentView(CONFIGURE);
                }
        }
    }

    @Override
    public void onValueChanged(Slider slider, int value) {
        if (m_canWrite) {
            Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, m_globright.getValue());
        }
    }

    protected void resume() {
        super.resume();
        setCurrentView(m_currentView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_handler.removeCallbacks(m_ticker);
    }

    @Override
    public void onBackPressed() {
        if (m_currentView == CONFIGURE) {
            m_handler.removeCallbacks(m_ticker);
            super.onBackPressed();
        } else {
            setCurrentView(CONFIGURE);
        }
    }
}
