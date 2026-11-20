package xyz.emlyn.patchextension.qbf.ig;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import java.util.Collections;
import java.util.Set;

import xyz.emlyn.patchextension.qbf.api.QbfJson;
import xyz.emlyn.patchextension.qbf.ui.Color;

// Reads state from + is invalidated by InstagramRepository
public final class QbfIconDrawable extends Drawable {
    private static final long SPIN_PERIOD = 1000L;
    private static final float TEXT_SIZE = 0.28f;
    private static final float TEXT_MAX_WIDTH = 0.62f;
    private static final float BADGE_RADIUS = 0.14f;

    private final Drawable icon;
    private final Drawable ring;
    private final Drawable ringTinted;
    private final Drawable shield;
    private final String postId;

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path gauge = new Path();
    private QbfJson.ImageScan shownResult;
    private String scoreText;

    private int alpha = 0xff;
    private volatile Set<Integer> videoPages = Collections.emptySet();

    // drawables nullable
    public QbfIconDrawable(Drawable icon, Drawable ring, Drawable shield, String postId) {
        this.icon = icon == null ? null : icon.mutate();
        this.ring = ring == null ? null : ring.mutate();
        this.ringTinted = ring == null ? null : ring.getConstantState().newDrawable().mutate();
        this.shield = shield == null ? null : shield.mutate();
        this.postId = postId;

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        badgePaint.setStyle(Paint.Style.FILL);
        badgePaint.setColor(Color.SCAN_ERROR);
    }

    public void setVideoPages(Set<Integer> videoPages) {
        this.videoPages = videoPages;
    }

    // theme UFI colour (dark mode)
    public void setColor(int color) {
        arcPaint.setColor(color);
    }

    static int verdictColor(String prediction) {
        if (prediction == null)
            return Color.AI_UNSCANNED;
        switch (prediction) {
            case "AI Detected":
            case "Likely AI":
                return Color.AI_AI;
            case "Partially AI":
            case "Unsure":
                return Color.AI_ASSISTED;
            default:
                return Color.AI_HUMAN;
        }
    }

    static boolean isC2pa(QbfJson.ImageScan result) {
        return "c2pa".equals(result.detection_source);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        for (Drawable d : new Drawable[] { icon, ring, ringTinted, shield })
            if (d != null)
                d.setBounds(bounds);
        float size = Math.min(bounds.width(), bounds.height());
        arcPaint.setStrokeWidth(size * 0.08f);
        shownResult = null; // refit text
    }

    @Override
    public void draw(Canvas canvas) {
        if (!QbfSettings.Prefs.Enable.get())
            return;

        Rect b = getBounds();
        float size = Math.min(b.width(), b.height());
        if (size <= 0)
            return;
        float cx = b.exactCenterX(), cy = b.exactCenterY();

        int page = IgCarousel.currentPage(postId);
        if (videoPages.contains(page))
            return;

        boolean scanning = false;
        boolean failed = false;
        QbfJson.ImageScan result = null;
        Post entry = InstagramRepository.entry(postId);
        synchronized (InstagramRepository.class) {
            if (entry != null) {
                scanning = entry.scanning;
                result = entry.results.get(page);
                failed = entry.errors.containsKey(page);
            }
        }

        if (result != shownResult) {
            shownResult = result;
            if (result != null)
                showResult(result, b, size);
        }

        if (result != null) {
            Drawable mark = isC2pa(result) ? shield : ring;
            if (mark != null) {
                mark.setAlpha(alpha);
                mark.draw(canvas);
            }
            if (!isC2pa(result) && ringTinted != null) {
                ringTinted.setAlpha(alpha);
                canvas.save();
                canvas.clipPath(gauge);
                ringTinted.draw(canvas);
                canvas.restore();
            }
            textPaint.setAlpha(alpha);
            float baseline = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
            canvas.drawText(scoreText, cx, baseline, textPaint);
            return;
        }

        if (icon != null) {
            icon.setAlpha(alpha);

            if (scanning) {
                float angle = (SystemClock.uptimeMillis() % SPIN_PERIOD) * 360f / SPIN_PERIOD;

                canvas.save();
                canvas.rotate(angle, cx, cy);
                icon.draw(canvas);
                canvas.restore();

                invalidateSelf(); // animates self
            } else {
                icon.draw(canvas);
            }
        }

        // badge rather than tint so it can't read as a verdict
        if (failed && !scanning) {
            float r = size * BADGE_RADIUS;
            badgePaint.setAlpha(alpha);
            canvas.drawCircle(cx + size / 2f - r, cy - size / 2f + r, r, badgePaint);
        }
    }

    private void showResult(QbfJson.ImageScan result, Rect b, float size) {
        int color = verdictColor(result.prediction);
        PorterDuffColorFilter tint = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
        if (ringTinted != null)
            ringTinted.setColorFilter(tint);
        if (shield != null)
            shield.setColorFilter(tint);
        textPaint.setColor(color);

        float likelihood = Math.max(0f, Math.min(1f, result.ai_likelihood));
        scoreText = isC2pa(result) ? "AI" : Math.round(100 * likelihood) + "%";

        textPaint.setTextSize(size * TEXT_SIZE);
        float width = textPaint.measureText(scoreText);
        if (width > size * TEXT_MAX_WIDTH)
            textPaint.setTextSize(size * TEXT_SIZE * size * TEXT_MAX_WIDTH / width);

        // gauge angle
        RectF oval = new RectF(b);
        oval.inset(-size, -size);
        gauge.reset();
        if (likelihood >= 1f) {
            gauge.addRect(oval, Path.Direction.CW);
        } else {
            gauge.moveTo(b.exactCenterX(), b.exactCenterY());
            gauge.arcTo(oval, -90f, 360f * likelihood);
            gauge.close();
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return icon == null ? -1 : icon.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return icon == null ? -1 : icon.getIntrinsicHeight();
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

    // theme tint
    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (icon != null)
            icon.setColorFilter(colorFilter);
        if (ring != null)
            ring.setColorFilter(colorFilter);
        arcPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
