package xyz.emlyn.patchextension.qbf.twitter.text;

public final class HtmlUnescape {
    public static final class Result {
        public final String text;
        public final int[] rawIndex;

        Result(String text, int[] rawIndex) {
            this.text = text;
            this.rawIndex = rawIndex;
        }
    }

    private static final String[][] NAMED = {
            { "amp", "&" },
            { "lt", "<" },
            { "gt", ">" },
            { "quot", "\"" },
            { "apos", "'" },
    };

    private static final int MAX_ENTITY_LEN = 10;

    public static Result unescape(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        int[] idx = new int[raw.length() + 1];
        int n = 0;

        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '&') {
                int semi = raw.indexOf(';', i + 1);
                if (semi > i + 1 && semi - i <= MAX_ENTITY_LEN) {
                    String decoded = decode(raw.substring(i + 1, semi));
                    if (decoded != null) {
                        for (int k = 0; k < decoded.length(); k++) {
                            out.append(decoded.charAt(k));
                            idx[n++] = i;
                        }
                        i = semi + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            idx[n++] = i;
            i++;
        }
        idx[n] = raw.length();

        int[] trimmed = new int[n + 1];
        System.arraycopy(idx, 0, trimmed, 0, n + 1);
        return new Result(out.toString(), trimmed);
    }

    private static String decode(String name) {
        if (name.charAt(0) == '#') {
            try {
                int cp = (name.length() > 1 && (name.charAt(1) == 'x' || name.charAt(1) == 'X'))
                        ? Integer.parseInt(name.substring(2), 16)
                        : Integer.parseInt(name.substring(1), 10);
                if (cp <= 0 || cp > Character.MAX_CODE_POINT || (cp >= 0xD800 && cp <= 0xDFFF))
                    return null;
                return new String(Character.toChars(cp));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        for (String[] e : NAMED) {
            if (e[0].equals(name))
                return e[1];
        }
        return null;
    }
}
