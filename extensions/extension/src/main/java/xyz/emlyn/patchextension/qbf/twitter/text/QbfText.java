package xyz.emlyn.patchextension.qbf.twitter.text;

import xyz.emlyn.patchextension.shared.logging.Logging;
import xyz.emlyn.patchextension.shared.logging.Lvl;
import xyz.emlyn.patchextension.qbf.api.QbfJson;
import xyz.emlyn.patchextension.qbf.twitter.Compose;
import xyz.emlyn.patchextension.qbf.twitter.Tweet;
import xyz.emlyn.patchextension.qbf.twitter.TwitterRepository;
import xyz.emlyn.patchextension.qbf.twitter.TwtColor;
import xyz.emlyn.patchextension.qbf.ui.Color;

import java.util.List;

@SuppressWarnings("unused")
public class QbfText {
    private static String currentFullText;
    private static String currentRichText;
    private static Object scanVersion;

    public static void onBuildEntry(String fullText) {
        currentFullText = fullText;
    }

    public static void onStyle(Object builder, String cutText, Object entities) {
        try {
            String full = currentFullText;
            if (full == null)
                return;

            List<?> ents = (List<?>) entities;
            int[] starts = new int[ents.size()];
            int[] ends = new int[ents.size()];
            for (int i = 0; i < ents.size(); i++) {
                starts[i] = TextStubs.entityStart(ents.get(i));
                ends[i] = TextStubs.entityEnd(ents.get(i));
            }

            DisplayMap map = DisplayMap.build(full, cutText, starts, ends, Compose.builderText(builder));
            if (map == null) {
                Logging.log(Lvl.Warning, "couldn't map text: " + cutText);
                return;
            }
            TwitterRepository.putMap(full, map);
        } catch (Throwable t) {
            Logging.log(Lvl.Error, "onStyle threw: " + t);
        }
    }

    public static Object decorate(Object annotated) {
        try {
            // attach state change listener
            Compose.stateGet(scanVersion());

            String full = currentFullText;
            if (full == null)
                return annotated;
            TextRecord rec = TwitterRepository.recordFor(full);
            if (rec == null || rec.id == null || rec.map == null)
                return annotated;
            Tweet entry = TwitterRepository.entry(rec.id);
            if (entry == null || !entry.overlayVisible || entry.scanMap == null)
                return annotated;
            QbfJson.TextScan result = entry.result;
            if (result == null || result.windows == null || result.windows.isEmpty())
                return annotated;

            return applyWindows(annotated, rec.map, entry, result);
        } catch (Throwable t) {
            Logging.log(Lvl.Error, "decorate threw: " + t);
            return annotated;
        }
    }

    // full text (entityless)
    public static void onRichTextEntry(String fullText) {
        currentRichText = fullText;
    }

    // entityless posts
    public static Object decoratePlain(Object annotated) {
        try {
            Compose.stateGet(scanVersion());

            String full = currentRichText;
            if (full == null)
                return annotated;
            TextRecord rec = TwitterRepository.recordFor(full);
            if (rec == null || rec.id == null)
                return annotated;
            Tweet entry = TwitterRepository.entry(rec.id);
            if (entry == null || !entry.overlayVisible || entry.scanMap == null)
                return annotated;
            QbfJson.TextScan result = entry.result;
            if (result == null || result.windows == null || result.windows.isEmpty())
                return annotated;

            DisplayMap map = rec.map;
            if (map == null) {
                map = DisplayMap.buildPlain(full, annotated.toString());
                if (map == null) {
                    Logging.log(Lvl.Warning, "couldn't map plain text: " + full);
                    return annotated;
                }
                TwitterRepository.putMap(full, map);
            }
            return applyWindows(annotated, map, entry, result);
        } catch (Throwable t) {
            Logging.log(Lvl.Error, "decoratePlain threw: " + t);
            return annotated;
        }
    }

    private static Object applyWindows(Object annotated, DisplayMap map, Tweet entry, QbfJson.TextScan result) {
        int[] scanToRaw = entry.scanMap.rawIndex;
        int maxScan = scanToRaw.length - 1;
        int length = ((CharSequence) annotated).length();

        Object builder = null;
        for (QbfJson.TextScan.Window w : result.windows) {
            int color = colorFor(w.label);
            if (color == TwtColor.NONE)
                continue;
            int s = Math.max(0, Math.min(w.start_index, maxScan));
            int e = Math.max(0, Math.min(w.end_index, maxScan));
            int[] d = map.mapRange(scanToRaw[s], scanToRaw[e]);
            if (d == null || d[1] > length)
                continue;

            if (builder == null)
                builder = Compose.builderFrom(annotated);
            Compose.builderAddStyle(builder, Compose.spanStyleBackground(Compose.packColor(color)), d[0], d[1]);
        }
        return builder == null ? annotated : Compose.builderBuild(builder);
    }

    public static void bumpScanVersion() {
        try {
            Object state = scanVersion();
            Compose.stateSet(state, ((Integer) Compose.stateGet(state)) + 1);
        } catch (Throwable t) {
            Logging.log(Lvl.Error, "bumpScanVersion threw: " + t);
        }
    }

    private static Object scanVersion() {
        if (scanVersion == null)
            scanVersion = Compose.mutableStateOf(0);
        return scanVersion;
    }

    private static int colorFor(String label) {
        int base;
        if (label == null || label.equals("Human Written"))
            return TwtColor.NONE;
        else if (label.equals("AI-Assisted"))
            base = TwtColor.AI_ASSISTED;
        else if (label.equals("AI-Generated"))
            base = TwtColor.AI_AI;
        else
            return TwtColor.NONE;
        return (base & 0x00ffffff) | TwtColor.OVERLAY_ALPHA;
    }
}
