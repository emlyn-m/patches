package xyz.emlyn.patchextension.qbf.ig;

import xyz.emlyn.patchextension.qbf.api.QbfJson;
import xyz.emlyn.patchextension.qbf.ig.QbfSettings.Prefs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Post {
    private static final int MEDIA_TYPE_VIDEO = 2;

    public final String id;
    public final String caption;
    // imageKey of every image size -> page
    public final Map<String, Integer> imagePages;

    public boolean scanning;
    // per-page
    public Map<Integer, QbfJson.ImageScan> results;
    // per-page last scan failure, cleared on rescan
    public Map<Integer, String> errors;
    public boolean overlayVisible;

    Post(String id, String caption, Map<String, Integer> imagePages) {
        this.id = id;
        this.caption = caption;
        this.imagePages = imagePages;
        this.results = new HashMap<>();
        this.errors = new HashMap<>();
        this.overlayVisible = Prefs.AutoOverlay.get();
    }

    public static Post fromUiState(Object uiState) {
        Object media = IgMedia.fromUiState(uiState);
        String caption = IgMedia.caption(media);

        List<?> pageMedia = pageMedia(media);
        Map<String, Integer> imagePages = new HashMap<>();
        for (int i = 0; i < pageMedia.size(); i++) {
            List<?> candidates = IgMedia.imageCandidates(pageMedia.get(i));
            if (candidates == null)
                continue;
            for (Object c : candidates) {
                String key = imageKey(IgMedia.imageUrl(c));
                if (key != null)
                    imagePages.put(key, i);
            }
        }

        return new Post(IgMedia.id(media), caption == null ? "" : caption, Collections.unmodifiableMap(imagePages));
    }

    private static List<?> pageMedia(Object media) {
        List<?> children = IgMedia.carousel(media);
        return children != null && !children.isEmpty() ? children : Collections.singletonList(media);
    }

    public static Set<Integer> videoPages(Object media) {
        List<?> pageMedia = pageMedia(media);
        Set<Integer> pages = new HashSet<>();
        for (int i = 0; i < pageMedia.size(); i++)
            if (IgMedia.mediaType(pageMedia.get(i)) == MEDIA_TYPE_VIDEO)
                pages.add(i);
        return pages;
    }

    // cdn url without query (size-independent) (nullable)
    public static String imageKey(String url) {
        if (url == null)
            return null;
        int query = url.indexOf('?');
        return query < 0 ? url : url.substring(0, query);
    }

    public String toString() {
        return "Post(id=" + id + ", caption=" + caption + ", pages=" + new HashSet<>(imagePages.values()).size() + ")";
    }
}
