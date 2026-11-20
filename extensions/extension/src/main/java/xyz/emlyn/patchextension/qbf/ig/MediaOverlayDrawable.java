package xyz.emlyn.patchextension.qbf.ig;

import java.lang.ref.WeakReference;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import xyz.emlyn.patchextension.qbf.api.QbfJson;
import xyz.emlyn.patchextension.qbf.ui.Color;

// Drawn over a feed image (see IgOverlay)
// Reads state from InstagramRepository on each draw
public final class MediaOverlayDrawable extends Drawable {
    private static final int HEATMAP_MAX_ALPHA = 0x90;

    private final Paint cellPaint = new Paint();
    private final WeakReference<View> view;

    private String imageKey;
    private int alpha = 0xff;

    public MediaOverlayDrawable(View view) {
        this.view = new WeakReference<>(view);
    }

    void bind(String imageKey) {
        this.imageKey = imageKey;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        if (!QbfSettings.Prefs.Enable.get())
            return;
        Rect b = getBounds();
        if (imageKey == null || b.isEmpty())
            return;

        QbfJson.ImageScan result;
        synchronized (InstagramRepository.class) {
            Post entry = InstagramRepository.postForImage(imageKey);
            if (entry == null || !entry.overlayVisible)
                return;
            result = entry.results.get(entry.imagePages.get(imageKey));
        }
        if (result == null || result.heatmap == null || result.heatmap.grid == null)
            return;

        View v = view.get();
        RectF image = v == null ? null : IgOverlay.imageRect(v);
        if (image == null)
            image = new RectF(b);
        canvas.save();
        canvas.clipRect(b);
        drawHeatmap(canvas, image, result.heatmap.grid);
        canvas.restore();
    }

    private void drawHeatmap(Canvas canvas, RectF b, List<List<Float>> grid) {
        int rows = grid.size();
        for (int y = 0; y < rows; y++) {
            List<Float> row = grid.get(y);
            int cols = row.size();
            float top = b.top + b.height() * y / (float) rows;
            float bottom = b.top + b.height() * (y + 1) / (float) rows;
            for (int x = 0; x < cols; x++) {
                float v = Math.max(0f, Math.min(1f, row.get(x)));
                int color = Color.hueInterpolate(Color.OVERLAY_HUMAN, Color.OVERLAY_AI, v);
                cellPaint.setColor(color);
                cellPaint.setAlpha((int) Math.round(HEATMAP_MAX_ALPHA * alpha / 0xff));
                canvas.drawRect(b.left + b.width() * x / (float) cols, top,
                        b.left + b.width() * (x + 1) / (float) cols, bottom, cellPaint);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        cellPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
