package xyz.emlyn.patchextension.qbf.ig;

import xyz.emlyn.patchextension.shared.logging.Logging;
import xyz.emlyn.patchextension.shared.logging.Lvl;
import xyz.emlyn.patchextension.qbf.api.QbfJson;
import xyz.emlyn.patchextension.qbf.ui.Color;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@SuppressWarnings("unused")
public class IgQbfButton {
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final Map<String, Integer> drawableRes = new HashMap<>();

    private static Context context() throws Exception {
        return (Context) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
    }

    public static void showToast(String msg) {
        showToast(msg, Toast.LENGTH_SHORT);
    }

    public static void showToast(String msg, int duration) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(() -> showToast(msg, duration));
            return;
        }
        Context context;
        try {
            context = context();
            Toast.makeText(context, msg, duration).show();
        } catch (Exception e) {
            Logging.log(Lvl.Error, "Unable to fetch context! Not showing toast: " + msg);
            return;
        }
    }

    public static int iconRes() {
        return drawableRes("ic_lens_instagram");
    }

    private static synchronized int drawableRes(String name) {
        Integer cached = drawableRes.get(name);
        if (cached != null)
            return cached;
        int res = 0;
        try {
            Context context = context();
            res = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
            if (res == 0)
                Logging.log(Lvl.Error, name + " drawable not found");
        } catch (Throwable t) {
            Logging.log(Lvl.Error, "qbf.ig drawableRes threw: " + t);
        }
        drawableRes.put(name, res);
        return res;
    }

    private static Drawable loadDrawable(int res) {
        try {
            return res == 0 ? null : context().getDrawable(res);
        } catch (Throwable t) {
            Logging.log(Lvl.Error, "qbf.ig drawable load threw: " + t);
            return null;
        }
    }

    static String c2paMessage(QbfJson.ImageScan result) {
        return "Partially AI".equals(result.prediction)
                ? "Content Credentials show this image was edited with an AI tool."
                : "Content Credentials show this image was generated with an AI tool.";
    }

    // replaces the button drawable ctor in qbfButton,
    // same args with uiState in place of `this`.
    // runs during litho render (maybe off main): must not throw
    public static Drawable drawable(Object uiState, ImageView.ScaleType _scaleType, Object _modifier,
            Integer _pressedTint, int iconRes, int color, boolean _z) {
        try {
            Object media = IgMedia.fromUiState(uiState);
            String id = IgMedia.id(media);
            IgCarousel.register(IgMedia.stateFromUiState(uiState), id);
            QbfIconDrawable drawable = InstagramRepository.drawable(id, () -> new QbfIconDrawable(
                    loadDrawable(iconRes), loadDrawable(drawableRes("ic_lens_ring_instagram")),
                    loadDrawable(drawableRes("ic_lens_shield_instagram")), id));
            drawable.setColor(color);
            drawable.setVideoPages(Post.videoPages(media));
            return drawable;
        } catch (Throwable t) {
            Logging.log(Lvl.Error, "Error while building drawable: " + t + ", skipping post.");
            return new ColorDrawable(Color.NONE);
        }
    }

    public static Function1<Object, Object> onClick(Object uiState) {
        if (QbfSettings.Prefs.Enable.get()) {
            return event -> {
                try {
                    Post post = Post.fromUiState(uiState);
                    // should never be possible
                    if (Post.videoPages(IgMedia.fromUiState(uiState)).contains(IgCarousel.currentPage(post.id))) {
                        showToast("Video scanning unsupported");
                        return Unit.INSTANCE;
                    }
                    boolean hasResult = InstagramRepository.registerData(post);
                    if (hasResult) {
                        QbfJson.ImageScan result = InstagramRepository.currentResult(post.id);
                        if (result != null && QbfIconDrawable.isC2pa(result))
                            showToast(c2paMessage(result), Toast.LENGTH_LONG);
                        else
                            InstagramRepository.toggleOverlay(post.id);
                        return Unit.INSTANCE;
                    }

                    String error = InstagramRepository.currentError(post.id);
                    if (error != null)
                        showToast("Retrying, last scan failed: " + error);
                    InstagramRepository.checkAI(post.id);
                } catch (Throwable t) {
                    Logging.log(Lvl.Error, "Error inside IgQbfButton onClick: " + t);
                }
                return Unit.INSTANCE;
            };
        } else {
            return event -> Unit.INSTANCE;
        }
    }
}
