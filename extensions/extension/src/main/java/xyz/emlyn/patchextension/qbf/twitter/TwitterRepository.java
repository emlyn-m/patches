package xyz.emlyn.patchextension.qbf.twitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import xyz.emlyn.patchextension.shared.logging.Logging;
import xyz.emlyn.patchextension.shared.logging.Lvl;
import xyz.emlyn.patchextension.qbf.api.Lru;
import xyz.emlyn.patchextension.qbf.api.QbfJson;
import xyz.emlyn.patchextension.qbf.api.QbfNet;
import xyz.emlyn.patchextension.qbf.twitter.Settings.Prefs;
import xyz.emlyn.patchextension.qbf.twitter.text.DisplayMap;
import xyz.emlyn.patchextension.qbf.twitter.text.QbfText;
import xyz.emlyn.patchextension.qbf.twitter.text.ScanAlign;
import xyz.emlyn.patchextension.qbf.twitter.text.TextRecord;
import xyz.emlyn.patchextension.shared.Result;

public class TwitterRepository {
    private static final int MAX_ENTRIES = 500;

    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    private static final Lru<String, Tweet> ids = new Lru<>(MAX_ENTRIES, TwitterRepository::onIdEvicted);
    private static final Lru<String, TextRecord> shortTexts = new Lru<>(MAX_ENTRIES, null);
    private static final Lru<String, TextRecord> longTexts = new Lru<>(MAX_ENTRIES, null);

    private static TextRecord findText(String text) {
        TextRecord rec = longTexts.get(text);
        return rec != null ? rec : shortTexts.get(text);
    }

    private static TextRecord upsertText(Lru<String, TextRecord> preferred, String text) {
        TextRecord rec = findText(text);
        if (rec == null) {
            rec = new TextRecord();
            preferred.put(text, rec);
        }
        return rec;
    }

    private static void onIdEvicted(Tweet entry) {
        for (String text : new String[] { entry.shortText, entry.longText }) {
            TextRecord rec = longTexts.containsKey(text) ? longTexts.get(text) : shortTexts.get(text);
            if (rec != null && entry.id.equals(rec.id)) {
                rec.id = null;
            }
        }
    }

    private static void touchTexts(Tweet tweet) {
        findText(tweet.shortText);
        findText(tweet.longText);
    }

    public static synchronized void putMap(String text, DisplayMap map) {
        upsertText(shortTexts, text).map = map;
    }

    public static synchronized TextRecord recordFor(String text) {
        TextRecord rec = findText(text);
        if (rec != null && rec.id != null) {
            Tweet entry = ids.get(rec.id);
            if (entry != null) {
                touchTexts(entry);
            }
        }
        return rec;
    }

    public static synchronized boolean registerData(Tweet tweet) {
        Tweet entry = ids.get(tweet.id);
        if (entry == null) {
            ids.put(tweet.id, tweet);
        }
        upsertText(shortTexts, tweet.shortText).id = tweet.id;
        upsertText(longTexts, tweet.longText).id = tweet.id;

        return ((entry != null) && (entry.result != null));

    }

    public static synchronized Tweet entry(String id) {
        Tweet entry = ids.get(id);
        if (entry != null) {
            touchTexts(entry);
        }
        return entry;
    }

    public static synchronized void checkAI(String id, View view, Consumer<View> redraw) {
        String apiKey = Settings.Prefs.ApiKey.get();
        if (apiKey == null || apiKey.length() == 0) {
            Logging.log(Lvl.Error, "Failed to get api key");
            main.post(() -> QbfTriggerButton.showToast("No API key configured."));
            return;
        }

        String apiUrl = Settings.Prefs.DefaultApis.get()
                ? Settings.Prefs.ApiUrlText.defaultValue()
                : Settings.Prefs.ApiUrlText.get();
        if (apiUrl == null || apiUrl.length() == 0) {
            Logging.log(Lvl.Error, "Failed to get text API url.");
            main.post(() -> QbfTriggerButton.showToast("No text API configured."));
            return;
        }

        String model = Settings.Prefs.DefaultModels.get()
                ? Settings.Prefs.ModelText.defaultValue()
                : Settings.Prefs.ModelText.get();
        if (model == null || model.length() == 0) {
            Logging.log(Lvl.Error, "Failed to get text model.");
            main.post(() -> QbfTriggerButton.showToast("No text model configured."));
            return;
        }

        Tweet entry = ids.get(id);
        if (entry == null) {
            Logging.log(Lvl.Error, "Failed to get post data");
            QbfTriggerButton.showToast("Unable to get post data.");
            return;
        }
        if (entry.scanning) {
            Logging.log(Lvl.Debug, "Already scanning post " + entry.id);
            return;
        }
        if (entry.result != null) {
            Logging.log(Lvl.Warning, "Already have result for post " + entry.id + ", refusing to re-scan.");
            return;
        }

        entry.scanning = true;
        entry.error = null;
        redraw.accept(view);

        String scanId = entry.id;
        String scanText = entry.scanText;
        executor.execute(() -> {
            Result<QbfJson.TextScan, String> scanResult = QbfNet.checkText(apiKey, apiUrl, model, scanText);
            QbfJson.TextScan scan = scanResult.unwrapOr(null);
            if (scan == null)
                Logging.log(Lvl.Error, "Error in text scan: " + scanResult.err());
            else
                remapWindows(scanId, scanText, scan);

            synchronized (TwitterRepository.class) {
                Tweet updateEntry = ids.get(scanId);
                if (updateEntry != null) {
                    updateEntry.result = scan;
                    updateEntry.error = scan == null ? scanResult.err() : null;
                    updateEntry.scanning = false;
                    updateEntry.overlayVisible = Prefs.AutoOverlay.get();
                }
            }

            main.post(() -> QbfTriggerButton.redrawPost(scanId));
            main.post(QbfText::bumpScanVersion);
        });
    }

    private static void remapWindows(String id, String sent, QbfJson.TextScan scan) {
        if (scan.windows == null || scan.text == null || scan.text.equals(sent))
            return;
        int[] map = ScanAlign.align(sent, scan.text);
        if (map == null) {
            Logging.log(Lvl.Warning, "scan text mismatch for <" + id + ">, highlights disabled");
            scan.windows = null;
            return;
        }
        int max = map.length - 1;
        for (QbfJson.TextScan.Window w : scan.windows) {
            w.start_index = map[Math.max(0, Math.min(w.start_index, max))];
            w.end_index = map[Math.max(0, Math.min(w.end_index, max))];
        }
    }

    public static synchronized void toggleOverlay(String id, View view, Consumer<View> redraw) {
        Tweet entry = entry(id);
        if (entry == null || entry.result == null) {
            Logging.log(Lvl.Error, "No data for requested tweet");
            main.post(() -> QbfTriggerButton.showToast("No data was found for the requested tweet"));
            return;
        }
        entry.overlayVisible = !entry.overlayVisible;

        redraw.accept(view);
        main.post(QbfText::bumpScanVersion);
    }
}
