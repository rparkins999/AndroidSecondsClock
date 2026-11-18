package uk.co.yahoo.p1rpp.secondsclock;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class LineColorsBackupRoundTripTest {
    private SharedPreferences preferences;
    private Map<String, ?> originalPreferences;
    private AppWidgetManager widgetManager;
    private ComponentName widgetComponent;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        preferences = context.getSharedPreferences(
                "SecondsClock", Context.MODE_PRIVATE);
        originalPreferences = new HashMap<>(preferences.getAll());
        widgetManager = AppWidgetManager.getInstance(context);
        widgetComponent = new ComponentName(context, SecondsClockWidget.class);
    }

    @After
    public void tearDown() {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (Map.Entry<String, ?> entry : originalPreferences.entrySet()) {
            putPreference(editor, entry.getKey(), entry.getValue());
        }
        editor.commit();
    }

    @Test
    public void automaticColorSettingsSurviveBackupImportRoundTrip() throws Exception {
        preferences.edit()
                .putBoolean(LineColors.ENABLED_KEY, true)
                .putInt(LineColors.PROFILE_KEY, LineColors.DEUTERANOPIA)
                .putString(LineColors.SEED_KEY, "backup-round-trip-seed")
                .commit();

        JSONObject backup = SettingsStore.createBackup(
                preferences, widgetManager, widgetComponent);
        JSONObject globals = backup.getJSONObject("active")
                .getJSONObject("globals");
        assertEquals(true, globals.getJSONObject("lineColorsEnabled")
                .getBoolean("value"));
        assertEquals(LineColors.DEUTERANOPIA,
                globals.getJSONObject("lineColorProfile").getInt("value"));
        assertEquals("backup-round-trip-seed", globals
                .getJSONObject("lineColorSeed").getString("value"));

        preferences.edit()
                .putBoolean(LineColors.ENABLED_KEY, false)
                .putInt(LineColors.PROFILE_KEY, LineColors.NORMAL)
                .putString(LineColors.SEED_KEY, "changed-after-export")
                .commit();

        SettingsStore.importBackup(
                preferences, widgetManager, widgetComponent, backup);

        assertEquals(true, preferences.getBoolean(LineColors.ENABLED_KEY, false));
        assertEquals(LineColors.DEUTERANOPIA,
                preferences.getInt(LineColors.PROFILE_KEY, LineColors.NORMAL));
        assertEquals("backup-round-trip-seed",
                preferences.getString(LineColors.SEED_KEY, null));
    }

    private static void putPreference(
            SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof String) editor.putString(key, (String) value);
        else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
        else if (value instanceof Long) editor.putLong(key, (Long) value);
        else if (value instanceof Float) editor.putFloat(key, (Float) value);
        else if (value instanceof Set<?>) {
            @SuppressWarnings("unchecked")
            Set<String> strings = (Set<String>) value;
            editor.putStringSet(key, strings);
        }
    }
}
