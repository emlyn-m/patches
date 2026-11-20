package xyz.emlyn.patchextension.qbf.twitter.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DisplayMap {
    private static final class Seg {
        final int rawStart, rawEnd; // fullText coords
        int dispStart, dispEnd;
        final int[] rawIndex; // offset per display char

        Seg(int rawStart, int rawEnd, int[] rawIndex) {
            this.rawStart = rawStart;
            this.rawEnd = rawEnd;
            this.rawIndex = rawIndex;
        }

        boolean linear() {
            return rawIndex != null;
        }
    }

    private final int base;
    private final int cutLength;
    private final List<Seg> segs;

    private DisplayMap(int base, int cutLength, List<Seg> segs) {
        this.base = base;
        this.cutLength = cutLength;
        this.segs = segs;
    }

    public int base() {
        return base;
    }

    public static DisplayMap build(String fullText, String cutText, int[] entStarts, int[] entEnds, String built) {
        if (fullText == null || cutText == null || built == null || cutText.isEmpty())
            return null;
        return buildAt(fullText.indexOf(cutText), cutText, entStarts, entEnds, built);
    }

    // entity-less text
    public static DisplayMap buildPlain(String fullText, String built) {
        if (fullText == null || built == null || built.isEmpty())
            return null;
        HtmlUnescape.Result u = HtmlUnescape.unescape(fullText);
        int idx = u.text.indexOf(built.substring(0, Math.min(built.length(), 32)));
        if (idx < 0)
            return null;
        int n = 0;
        while (n < built.length() && idx + n < u.text.length() && u.text.charAt(idx + n) == built.charAt(n))
            n++;
        int rawStart = u.rawIndex[idx];
        int rawEnd = u.rawIndex[idx + n];
        return buildAt(rawStart, fullText.substring(rawStart, rawEnd), new int[0], new int[0], built);
    }

    private static DisplayMap buildAt(int base, String cutText, int[] entStarts, int[] entEnds, String built) {
        if (base < 0 || cutText.isEmpty())
            return null;

        int len = cutText.length();
        int[][] ents = new int[entStarts.length][];
        int nEnts = 0;
        for (int i = 0; i < entStarts.length; i++) {
            if (entStarts[i] >= 0 && entStarts[i] < entEnds[i] && entEnds[i] <= len)
                ents[nEnts++] = new int[] { entStarts[i], entEnds[i] };
        }
        ents = Arrays.copyOf(ents, nEnts);
        Arrays.sort(ents, (a, b) -> Integer.compare(a[0], b[0]));

        List<Seg> segs = new ArrayList<>();
        int pos = 0;
        int cursor = 0;
        for (int[] e : ents) {
            if (e[0] < pos)
                continue; // overlapping, skip
            cursor = addGap(segs, cutText, base, pos, e[0], built, cursor);
            if (cursor < 0)
                return null;
            segs.add(new Seg(base + e[0], base + e[1], null));
            pos = e[1];
        }
        if (addGap(segs, cutText, base, pos, len, built, cursor) < 0)
            return null;

        for (int i = 0; i < segs.size(); i++) {
            Seg s = segs.get(i);
            if (s.linear())
                continue;
            int start = 0;
            for (int j = i - 1; j >= 0; j--) {
                if (segs.get(j).linear()) {
                    start = segs.get(j).dispEnd;
                    break;
                }
            }
            int end = built.length();
            for (int j = i + 1; j < segs.size(); j++) {
                if (segs.get(j).linear()) {
                    end = segs.get(j).dispStart;
                    break;
                }
            }
            if (end < start)
                return null;
            s.dispStart = start;
            s.dispEnd = end;
        }

        return new DisplayMap(base, len, segs);
    }

    private static int addGap(List<Seg> segs, String cutText, int base, int from, int to, String built, int cursor) {
        if (from >= to)
            return cursor;
        HtmlUnescape.Result u = HtmlUnescape.unescape(cutText.substring(from, to));
        int anchor = built.indexOf(u.text, cursor);
        if (anchor < 0)
            return -1;
        Seg s = new Seg(base + from, base + to, u.rawIndex);
        s.dispStart = anchor;
        s.dispEnd = anchor + u.text.length();
        segs.add(s);
        return s.dispEnd;
    }

    public int[] mapRange(int rawStart, int rawEnd) {
        int rs = Math.max(rawStart, base);
        int re = Math.min(rawEnd, base + cutLength);
        if (rs >= re)
            return null;
        int ds = mapStart(rs);
        int de = mapEnd(re);
        if (ds < 0 || de < 0 || ds >= de)
            return null;
        return new int[] { ds, de };
    }

    private int mapStart(int r) {
        for (Seg s : segs) {
            if (r >= s.rawStart && r < s.rawEnd)
                return s.linear() ? s.dispStart + firstAtOrAfter(s.rawIndex, r - s.rawStart) : s.dispStart;
        }
        return -1;
    }

    private int mapEnd(int r) {
        for (Seg s : segs) {
            if (r > s.rawStart && r <= s.rawEnd)
                return s.linear() ? s.dispStart + firstAtOrAfter(s.rawIndex, r - s.rawStart) : s.dispEnd;
        }
        return -1;
    }

    private static int firstAtOrAfter(int[] rawIndex, int local) {
        for (int k = 0; k < rawIndex.length; k++) {
            if (rawIndex[k] >= local)
                return k;
        }
        return rawIndex.length - 1;
    }
}
