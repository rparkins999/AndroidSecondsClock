/*
 * Copyright © 2026. SecondsClock contributors
 * Released under GPL V3 or later
 */

package uk.co.yahoo.p1rpp.secondsclock.test;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Test-only host that asks Android's AppWidget service for the provider's real RemoteViews. */
public final class WidgetHostActivity extends Activity {
    static final String EXTRA_PROVIDER = "provider";
    private static final int HOST_ID = 0x5343;
    private static final int BIND_REQUEST = 1;

    static volatile WidgetHostActivity active;

    private final CountDownLatch widgetReady = new CountDownLatch(1);
    private final CountDownLatch widgetFailed = new CountDownLatch(1);
    private AppWidgetHost appWidgetHost;
    private AppWidgetHostView widgetView;
    private FrameLayout content;
    private ComponentName provider;
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private volatile String failure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        active = this;
        content = new FrameLayout(this);
        setContentView(content);

        try {
            provider = ComponentName.unflattenFromString(
                    getIntent().getStringExtra(EXTRA_PROVIDER));
            if (provider == null) {
                throw new IllegalArgumentException("Missing widget provider component");
            }

            appWidgetHost = new AppWidgetHost(this, HOST_ID);
            widgetId = appWidgetHost.allocateAppWidgetId();
            appWidgetHost.startListening();

            Bundle options = new Bundle();
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 121);
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 93);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            if (manager.bindAppWidgetIdIfAllowed(widgetId, provider, options)) {
                attachWidget(manager);
            } else {
                Intent bind = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
                startActivityForResult(bind, BIND_REQUEST);
            }
        } catch (Exception exception) {
            fail("Could not bind SecondsClock widget: " + exception);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != BIND_REQUEST) return;
        if (resultCode != RESULT_OK) {
            fail("Android's widget-bind confirmation was not accepted");
            return;
        }
        try {
            attachWidget(AppWidgetManager.getInstance(this));
        } catch (Exception exception) {
            fail("Could not attach bound SecondsClock widget: " + exception);
        }
    }

    private void attachWidget(AppWidgetManager manager) {
        AppWidgetProviderInfo info = manager.getAppWidgetInfo(widgetId);
        if (info == null || !provider.equals(info.provider)) {
            throw new IllegalStateException("Bound widget provider does not match SecondsClock");
        }
        widgetView = appWidgetHost.createView(this, widgetId, info);
        content.addView(widgetView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        widgetReady.countDown();
    }

    private void fail(String message) {
        failure = message;
        widgetFailed.countDown();
    }

    boolean awaitWidget(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            if (widgetReady.await(100, TimeUnit.MILLISECONDS)) return true;
            if (widgetFailed.getCount() == 0) return false;
        }
        return widgetReady.getCount() == 0;
    }

    AppWidgetHostView getWidgetView() {
        return widgetView;
    }

    String getFailure() {
        return failure;
    }

    void cleanup() {
        if (appWidgetHost == null) return;
        try {
            appWidgetHost.stopListening();
        } catch (RuntimeException ignored) {
            // The host might not have started listening if setup failed.
        }
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(widgetId);
            widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        }
    }

    @Override
    protected void onDestroy() {
        cleanup();
        if (active == this) active = null;
        super.onDestroy();
    }
}
