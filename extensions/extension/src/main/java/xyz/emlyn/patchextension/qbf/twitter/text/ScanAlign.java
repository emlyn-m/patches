package xyz.emlyn.patchextension.qbf.twitter.text;

public final class ScanAlign {
    // map api response text back to tweet text, null on mismatch beyond whitespace
    public static int[] align(String ours, String theirs) {
        int[] map = new int[theirs.length() + 1];
        int i = 0, j = 0;
        while (i < theirs.length()) {
            char a = theirs.charAt(i);
            if (j < ours.length() && ours.charAt(j) == a) {
                map[i++] = j++;
            } else if (j < ours.length() && Character.isWhitespace(ours.charAt(j))) {
                j++;
            } else if (Character.isWhitespace(a)) {
                map[i++] = j;
            } else {
                return null;
            }
        }
        map[i] = j;
        return map;
    }
}
