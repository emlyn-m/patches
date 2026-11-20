package xyz.emlyn.patchextension.shared.logging;

import android.util.Log;
import xyz.emlyn.patchextension.shared.logging.Lvl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class Logging {
    private static final String TAG = "xyz.emlyn.patches.logs";
    // logd truncates long entries (~4068 bytes).
    // Sent as utf-16, so truncate to 1000 chars.
    private static final int CHUNK = 1000;
    private static final AtomicInteger SEQ = new AtomicInteger();

    // Format: "#<id> <i>/<n> <chunk>"
    public static void log(Lvl lvl, String message) {
        String msg = "[" + lvl.prefix() + "] " + message;
        int id = SEQ.getAndIncrement();
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < msg.length();) {
            int end = Math.min(start + CHUNK, msg.length());
            // avoid splitting on surrogate pairs (emoji etc.)
            if (end < msg.length() && Character.isHighSurrogate(msg.charAt(end - 1)))
                end--;
            chunks.add(msg.substring(start, end));
            start = end;
        }
        for (int i = 0; i < chunks.size(); i++) {
            Log.d(TAG, "#" + id + " " + (i + 1) + "/" + chunks.size() + " " + chunks.get(i));
        }
    }
}
