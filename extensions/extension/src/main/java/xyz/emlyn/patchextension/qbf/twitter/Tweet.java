package xyz.emlyn.patchextension.qbf.twitter;

import xyz.emlyn.patchextension.shared.logging.Logging;
import xyz.emlyn.patchextension.shared.logging.Lvl;
import xyz.emlyn.patchextension.qbf.api.QbfJson;
import xyz.emlyn.patchextension.qbf.twitter.text.HtmlUnescape;
import xyz.emlyn.patchextension.qbf.twitter.text.TextStubs;

public class Tweet {
    public String id;
    public String shortText;
    public String longText;

    public boolean scanning;
    public boolean overlayVisible;
    public QbfJson.TextScan result;
    // last scan failure, cleared on rescan
    public String error;

    public String scanText;
    public HtmlUnescape.Result scanMap;

    private Tweet(String id, String shortText, String longText) {
        this.id = id;
        this.shortText = shortText;
        this.longText = longText;

        HtmlUnescape.Result ours = HtmlUnescape.unescape(longText);
        String theirs = TextStubs.htmlUnescape(longText);
        if (ours.text.equals(theirs)) {
            this.scanText = ours.text;
            this.scanMap = ours;
        } else {
            Logging.log(Lvl.Warning, "unescape mismatch for <" + id + ">, highlights disabled");
            this.scanText = theirs;
            this.scanMap = null;
        }

        this.scanning = false;
        this.overlayVisible = false;
        this.result = null;
        this.error = null;
    }

    public static Tweet fromObfuscated(Object _twt) {
        throw new UnsupportedOperationException("Tweet.fromObfuscated not implemented");
    }

    public String toString() {
        return "Tweet(id=" + id + ", shortText=" + shortText + ", longText=" + longText + ", scanning=" + scanning
                + ", overlayVisible=" + overlayVisible + ", scanResult=" + result + ", error=" + error;
    }
}
