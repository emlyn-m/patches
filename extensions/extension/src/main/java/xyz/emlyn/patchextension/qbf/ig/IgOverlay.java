package xyz.emlyn.patchextension.qbf.ig;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import xyz.emlyn.patchextension.shared.logging.Logging;
import xyz.emlyn.patchextension.shared.logging.Lvl;

// Feed media overlays. All calls on main thread.
@SuppressWarnings("unused")
public class IgOverlay {
    private static final Map<View, String> keys = new WeakHashMap<>();
    private static final Map<View, Object> urls = new WeakHashMap<>();
    private static final Map<View, MediaOverlayDrawable> overlays = new WeakHashMap<>();

    // view: IgImageView, imageUrl: ImageUrl (nullable)
    public static void onImageUrl(View view, Object imageUrl) {
        try {
            if (view == null)
                return;
            String key = imageUrl == null ? null : Post.imageKey(IgMedia.imageUrl(imageUrl));
            if (key == null) {
                keys.remove(view);
                urls.remove(view);
            } else {
                keys.put(view, key);
                urls.put(view, imageUrl);
            }

            MediaOverlayDrawable overlay = overlays.get(view);
            if (overlay != null)
                overlay.bind(key);
            else if (key != null && InstagramRepository.postForImage(key) != null)
                attach(view).bind(key);
        } catch (Throwable t) {
            Logging.log(Lvl.Error, "IgOverlay.onImageUrl threw: " + t);
        }
    }

    public static void redraw(String postId) {
        for (Map.Entry<View, String> e : new ArrayList<>(keys.entrySet())) {
            View view = e.getKey();
            Post post = InstagramRepository.postForImage(e.getValue());
            if (view == null || post == null || !postId.equals(post.id))
                continue;
            MediaOverlayDrawable overlay = overlays.get(view);
            if (overlay == null) {
                overlay = attach(view);
                overlay.bind(e.getValue());
            } else {
                overlay.invalidateSelf();
            }
        }
    }

    static Map<View, Object> imageUrls() {
        return new WeakHashMap<>(urls);
    }

    static Object imageUrlFor(String postId, int page) {
        Object found = null;
        for (Map.Entry<View, Object> e : urls.entrySet()) {
            String key = keys.get(e.getKey());
            Post post = key == null ? null : InstagramRepository.postForImage(key);
            Integer viewPage = post == null ? null : post.imagePages.get(key);
            if (post == null || !postId.equals(post.id) || viewPage == null || viewPage != page)
                continue;
            found = e.getValue();
            if (e.getKey().isShown())
                break;
        }
        return found;
    }

    static RectF imageRect(View view) {
        if (!(view instanceof ImageView))
            return null;
        ImageView iv = (ImageView) view;
        Drawable d = iv.getDrawable();
        if (d == null)
            return null;

        RectF rect;
        Object layer = IgMedia.vitoImageLayer(d);
        if (layer != null) {
            int w = IgMedia.layerSourceWidth(layer);
            int h = IgMedia.layerSourceHeight(layer);
            Rect bounds = IgMedia.layerBounds(layer);
            if (w <= 0 || h <= 0 || bounds == null)
                return null;
            Matrix m = IgMedia.layerMatrix(layer);
            if (m == null) {
                rect = new RectF(bounds);
            } else {
                rect = new RectF(0, 0, w, h);
                m.mapRect(rect);
            }
        } else {
            if (d.getIntrinsicWidth() <= 0 || d.getIntrinsicHeight() <= 0)
                return null;
            rect = new RectF(d.getBounds());
        }
        iv.getImageMatrix().mapRect(rect);
        rect.offset(iv.getPaddingLeft(), iv.getPaddingTop());
        return rect;
    }

    private static MediaOverlayDrawable attach(View view) {
        MediaOverlayDrawable overlay = new MediaOverlayDrawable(view);
        overlay.setBounds(0, 0, view.getWidth(), view.getHeight());
        view.getOverlay().add(overlay);
        view.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> overlay.setBounds(0, 0, r - l, b - t));
        overlays.put(view, overlay);
        return overlay;
    }
}
