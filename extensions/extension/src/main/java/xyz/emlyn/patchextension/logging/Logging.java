package xyz.emlyn.patchextension.logging;

import android.util.Log;

@SuppressWarnings("unused")
public class Logging {
    public static void log(String k, String v) {
        Log.d("xyz.emlyn.morphe", "[ " + k + " ]" + v);
    }
}
