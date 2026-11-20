package xyz.emlyn.patchextension.qbf.ig;

import java.util.Map;
import java.util.WeakHashMap;

import xyz.emlyn.patchextension.shared.logging.Logging;
import xyz.emlyn.patchextension.shared.logging.Lvl;
import xyz.emlyn.patchextension.qbf.api.Lru;

// Carousel swipe listener.@SuppressWarnings("unused")
public class IgCarousel {
    private static final Map<Object, String> postIds = new WeakHashMap<>();
    private static final Map<String, Integer> pages = new Lru<>(InstagramRepository.MAX_ENTRIES, null);

    static synchronized void register(Object mediaState, String postId) {
        if (mediaState != null && postId != null)
            postIds.put(mediaState, postId);
    }

    public static synchronized int currentPage(String postId) {
        Integer page = pages.get(postId);
        return page == null ? 0 : page;
    }

    // patched into the per-post state's page setter
    public static void onPageChanged(Object mediaState, int page) {
        try {
            String postId;
            synchronized (IgCarousel.class) {
                postId = postIds.get(mediaState);
                if (postId == null)
                    return;
                pages.put(postId, page);
            }
            InstagramRepository.redraw(postId);
        } catch (Throwable t) {
            Logging.log(Lvl.Error, "IgCarousel.onPageChanged threw: " + t);
        }
    }
}
