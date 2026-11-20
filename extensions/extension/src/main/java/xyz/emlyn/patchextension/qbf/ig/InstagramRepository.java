package xyz.emlyn.patchextension.qbf.ig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import xyz.emlyn.patchextension.shared.logging.Logging;
import xyz.emlyn.patchextension.shared.logging.Lvl;
import xyz.emlyn.patchextension.qbf.api.Lru;
import xyz.emlyn.patchextension.qbf.api.QbfJson;
import xyz.emlyn.patchextension.qbf.api.QbfNet;
import xyz.emlyn.patchextension.shared.Result;

public class InstagramRepository {
    public static final int MAX_ENTRIES = 100;
    public static final int MAX_DRAWABLE_ENTRIES = 20;

    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    private static final Lru<String, Post> ids = new Lru<>(MAX_ENTRIES, null);
    // Post.imageKey -> post id
    private static final Lru<String, String> images = new Lru<>(MAX_ENTRIES * 100, null);
    private static final Lru<String, QbfIconDrawable> drawables = new Lru<>(MAX_DRAWABLE_ENTRIES, null);

    public static synchronized QbfIconDrawable drawable(String id, Supplier<QbfIconDrawable> create) {
        QbfIconDrawable drawable = drawables.get(id);
        if (drawable == null) {
            drawable = create.get();
            drawables.put(id, drawable);
        }
        return drawable;
    }

    static void redraw(String id) {
        main.post(() -> {
            QbfIconDrawable drawable;
            synchronized (InstagramRepository.class) {
                drawable = drawables.get(id);
            }
            if (drawable != null)
                drawable.invalidateSelf();
            IgOverlay.redraw(id);
        });
    }

    public static synchronized boolean registerData(Post post) {
        Post entry = ids.get(post.id);
        // merge if not loaded yet
        if (entry == null || post.imagePages.size() > entry.imagePages.size()) {
            if (entry != null) {
                post.results = entry.results;
                post.errors = entry.errors;
                post.scanning = entry.scanning;
                post.overlayVisible = entry.overlayVisible;
            }
            ids.put(post.id, post);
            for (String key : post.imagePages.keySet())
                images.put(key, post.id);
            redraw(post.id);
        }

        int page = IgCarousel.currentPage(post.id);

        return ((entry != null) && (entry.results.containsKey(page)));
    }

    public static synchronized Post entry(String id) {
        Post entry = ids.get(id);
        return entry;
    }

    public static synchronized QbfJson.ImageScan currentResult(String id) {
        Post entry = ids.get(id);
        return entry == null ? null : entry.results.get(IgCarousel.currentPage(id));
    }

    public static synchronized String currentError(String id) {
        Post entry = ids.get(id);
        return entry == null ? null : entry.errors.get(IgCarousel.currentPage(id));
    }

    public static synchronized Post postForImage(String key) {
        String postId = images.get(key);
        return postId == null ? null : ids.get(postId);
    }

    public static synchronized void checkAI(String id) {
        Post entry = ids.get(id);
        if (entry == null) {
            Logging.log(Lvl.Error, "Failed to get post data!");
            IgQbfButton.showToast("Unable to get post data.");
            return;
        }
        int scanPage = IgCarousel.currentPage(entry.id);
        if (entry.scanning) {
            Logging.log(Lvl.Debug, "Already scanning post " + entry.id);
            return;
        }

        String apiKey = QbfSettings.Prefs.ApiKey.get();
        if (apiKey == null || apiKey.length() == 0) {
            Logging.log(Lvl.Error, "Failed to get api key");
            main.post(() -> IgQbfButton.showToast("No API key configured."));
            return;
        }

        String apiUrl = QbfSettings.Prefs.DefaultApis.get() ? QbfSettings.Prefs.ApiUrlImage.defaultValue()
                : QbfSettings.Prefs.ApiUrlImage.get();
        if (apiUrl == null || apiUrl.length() == 0) {
            Logging.log(Lvl.Error, "Failed to get image API url.");
            main.post(() -> IgQbfButton.showToast("No image API configured."));
            return;
        }

        String model = QbfSettings.Prefs.DefaultModels.get() ? QbfSettings.Prefs.ModelImage.defaultValue()
                : QbfSettings.Prefs.ModelImage.get();
        if (model == null || model.length() == 0) {
            Logging.log(Lvl.Error, "Failed to get image model.");
            main.post(() -> IgQbfButton.showToast("No Image model configured."));
            return;
        }

        if (entry.results.containsKey(scanPage)) {
            Logging.log(Lvl.Warning,
                    "Already have result for post " + entry.id + " (page " + scanPage + "), refusing to re-scan.");
            return;
        }

        String scanId = entry.id;
        Object imageUrl = IgOverlay.imageUrlFor(scanId, scanPage);
        if (imageUrl == null) {
            Logging.log(Lvl.Error, "No view showing page " + scanPage + " of post " + scanId);
            IgQbfButton.showToast("Unable to find post image.");
            return;
        }
        int urlWidth = IgMedia.imageWidth(imageUrl);
        int urlHeight = IgMedia.imageHeight(imageUrl);
        entry.scanning = true;
        entry.errors.remove(scanPage);
        redraw(scanId);

        executor.execute(() -> {
            QbfJson.ImageScan scan = null;
            String error = null;
            try {
                CachedImage image = CachedImage.awaitFull(imageUrl);
                if (image == null) {
                    Logging.log(Lvl.Warning, "Timed out waiting for page " + scanPage + " of post " + scanId);
                    error = "Image hasn't finished loading.";
                    main.post(() -> IgQbfButton.showToast("Image hasn't finished loading."));
                } else if (!sameAspect(image.width, image.height, urlWidth, urlHeight)) {
                    Logging.log(Lvl.Error, "Cached image " + image.width + "x" + image.height + " doesn't match url "
                            + urlWidth + "x" + urlHeight + ", not scanning");
                    error = "Unable to read post image.";
                    main.post(() -> IgQbfButton.showToast("Unable to read post image."));
                } else {
                    Logging.log(Lvl.Debug, "Scanning post " + scanId + " page " + scanPage + ": " + image.width + "x"
                            + image.height + " " + image.mimeType + ", " + image.bytes.length + "B");
                    String payload = Base64.encodeToString(image.bytes, Base64.NO_WRAP);
                    Result<QbfJson.ImageScan, String> scanResult = QbfNet.checkImage(apiKey, apiUrl, model, payload);
                    scan = scanResult.unwrapOr(null);
                    if (scan == null) {
                        Logging.log(Lvl.Error, "Error in image scan: " + scanResult.err());
                        error = scanResult.err();
                        String errPost = error;
                        main.post(() -> IgQbfButton.showToast(errPost));
                    }
                }
            } catch (Throwable t) {
                Logging.log(Lvl.Error, "Image scan threw: " + t);
                error = String.valueOf(t);
                main.post(() -> IgQbfButton.showToast("An unknown error occured while scanning."));
            }

            synchronized (InstagramRepository.class) {
                Post updateEntry = ids.get(scanId);
                if (updateEntry != null) {
                    if (scan != null)
                        updateEntry.results.put(scanPage, scan);
                    else
                        updateEntry.errors.put(scanPage, error);
                    updateEntry.scanning = false;
                } else {
                    Logging.log(Lvl.Warning, "Discarding result for post " + scanId + ", evicted");
                }
            }

            redraw(scanId);
        });
    }

    private static boolean sameAspect(int w, int h, int otherW, int otherH) {
        if (w <= 0 || h <= 0 || otherW <= 0 || otherH <= 0)
            return false;
        return Math.abs((float) w / h - (float) otherW / otherH) < 0.01f;
    }

    public static synchronized void toggleOverlay(String id) {
        Post entry = ids.get(id);
        int page = IgCarousel.currentPage(id);
        if (entry == null || !entry.results.containsKey(page)) {
            Logging.log(Lvl.Error, "No result for post " + id + "(page " + page + "), not toggling overlay.");
            return;
        }
        QbfJson.ImageScan result = entry.results.get(page);
        if (result.heatmap == null || result.heatmap.grid == null) {
            IgQbfButton.showToast("No overlay available for this page");
            return;
        }
        entry.overlayVisible = !entry.overlayVisible;
        redraw(id);
    }
}
