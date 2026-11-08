/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/** Receives the component selected in Android's app chooser. */
public class SystemClockAppSelectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ComponentName chosen = (ComponentName) intent.getParcelableExtra(
                Intent.EXTRA_CHOSEN_COMPONENT);
        SystemClockApp.saveOverride(context, chosen);
    }
}
