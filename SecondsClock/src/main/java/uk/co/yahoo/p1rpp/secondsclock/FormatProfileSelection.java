/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.widget.Button;

final class FormatProfileSelection {
    private FormatProfileSelection() {}

    static void apply(Button button, boolean selected) {
        button.setSelected(selected);
        button.setActivated(selected);
        button.setTypeface(null);
        button.setAllCaps(selected);
    }
}
