package xyz.emlyn.patchextension.settings;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import xyz.emlyn.patchextension.shared.logging.Lvl;
import xyz.emlyn.patchextension.shared.logging.Logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class Settings {
    public static List<Object> injectRootSections(List<Object> sections) {
        try {
            Context ctx = context();
            Intent intent = new Intent(ctx, SettingsActivity.class);

            Object item = SettingsStubs.item(
                    SettingsStubs.literal("em-patches"),
                    SettingsStubs.literal("patch configuration"),
                    SettingsStubs.navigateToIntent(intent));

            List<Object> out = new ArrayList<>(sections);
            out.add(SettingsStubs.section(Collections.singletonList(item)));
            return out;
        } catch (Throwable t) {
            Logging.log(Lvl.Error, "failed to inject root sections: " + t);
            return sections;
        }
    }

    static Context context() throws Exception {
        return (Application) Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null);
    }

    @SuppressWarnings("unchecked")
    static <T> T getEntry(SettingsEntry<T> entry) {
        try {
            Context ctx = context();
            SharedPreferences prefs = ctx.getSharedPreferences(entry.id(), Context.MODE_PRIVATE);

            if (entry.type == Integer.class) {
                return (T) Integer.valueOf(prefs.getInt(entry.key(), ((Integer) entry.defaultValue())));
            } else if (entry.type == String.class) {
                return (T) prefs.getString(entry.key(), (String) entry.defaultValue());
            } else if (entry.type == Boolean.class) {
                return (T) Boolean.valueOf(prefs.getBoolean(entry.key(), (Boolean) entry.defaultValue()));
            } else {
                throw new IllegalStateException("Unknown type " + entry.type + " on entry " + entry.key());
            }
        } catch (Exception e) {
            Logging.log(Lvl.Warning, "failed to get preference: " + entry.key() + "(" + e.toString() + ")");
        }

        return (T) null;
    }

    static <T> void putEntry(SettingsEntry<T> entry, T value) {
        try {
            Context ctx = context();
            SharedPreferences.Editor editor = ctx.getSharedPreferences(entry.id(), Context.MODE_PRIVATE).edit();

            if (entry.type == Integer.class) {
                editor.putInt(entry.key(), (Integer) value);
            } else if (entry.type == String.class) {
                editor.putString(entry.key(), (String) value);
            } else if (entry.type == Boolean.class) {
                editor.putBoolean(entry.key(), (Boolean) value);
            }
            editor.apply();
        } catch (Exception e) {
            Logging.log(Lvl.Warning, "failed to upsert preference: " + entry.key() + "(" + e.toString() + ")");
        }
    }
}
