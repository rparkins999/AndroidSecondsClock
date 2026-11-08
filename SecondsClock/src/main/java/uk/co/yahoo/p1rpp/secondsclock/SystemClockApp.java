/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

/** Helpers for selecting and remembering an app to use as the system clock. */
final class SystemClockApp {
    static final String OVERRIDE_KEY = "systemClockOverride";
    static final String OVERRIDE_LABEL_KEY = "systemClockOverrideLabel";
    static final String DONT_ASK_KEY = "systemClockDontAsk";
    private static final int CHOSEN_COMPONENT_REQUEST = 8104;

    private SystemClockApp() {}

    static ComponentName getOverride(SharedPreferences prefs) {
        String stored = prefs.getString(OVERRIDE_KEY, null);
        if (stored == null) return null;
        ComponentName component = ComponentName.unflattenFromString(stored);
        if (component == null) clearOverride(prefs);
        return component;
    }

    static void clearOverride(SharedPreferences prefs) {
        prefs.edit()
                .remove(OVERRIDE_KEY)
                .remove(OVERRIDE_LABEL_KEY)
                .apply();
    }

    static void reset(SharedPreferences prefs) {
        prefs.edit()
                .remove(OVERRIDE_KEY)
                .remove(OVERRIDE_LABEL_KEY)
                .remove(DONT_ASK_KEY)
                .apply();
    }

    static String getOverrideLabel(SharedPreferences prefs) {
        return prefs.getString(OVERRIDE_LABEL_KEY, "");
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    static void openSelector(Activity activity, boolean returnToHomeAfterLaunch) {
        Intent target = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);
        if (returnToHomeAfterLaunch) {
            target.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        }
        PendingIntent chosenComponent = PendingIntent.getBroadcast(
                activity,
                CHOSEN_COMPONENT_REQUEST,
                new Intent(activity, SystemClockAppSelectionReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Intent chooser = Intent.createChooser(target,
                activity.getString(R.string.system_clock_selector_title),
                chosenComponent.getIntentSender());
        chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS,
                new ComponentName[] {
                    new ComponentName(activity, MainActivity.class)
                });
        activity.startActivity(chooser);
    }

    static void saveOverride(Context context, ComponentName component) {
        if (component == null
                || context.getPackageName().equals(component.getPackageName())) {
            return;
        }
        String label = component.getPackageName();
        try {
            PackageManager packageManager = context.getPackageManager();
            ActivityInfo activityInfo = packageManager.getActivityInfo(component, 0);
            CharSequence activityLabel = activityInfo.loadLabel(packageManager);
            if (activityLabel != null && activityLabel.length() > 0) {
                label = activityLabel.toString();
            }
        } catch (PackageManager.NameNotFoundException | SecurityException ignored) {
            // Keep the package name when package visibility or an OEM prevents lookup.
        }
        context.getSharedPreferences("SecondsClock", Context.MODE_PRIVATE)
                .edit()
                .putString(OVERRIDE_KEY, component.flattenToString())
                .putString(OVERRIDE_LABEL_KEY, label)
                .apply();
    }
}
