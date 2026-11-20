package xyz.emlyn.patchextension.qbf.twitter;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

public final class BorderThrobberDrawable extends Drawable implements Animatable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path border = new Path();
    private final Path segment = new Path();
    private final Path wrappedSegment = new Path();
    private final PathMeasure measure = new PathMeasure();
    private final RectF rect = new RectF();
    private final Matrix shaderMatrix = new Matrix();
    private final float[] point = new float[2];
    private final int[] gradientColors;
    private final float[] gradientStops = new float[5];

    private final float strokeWidthPx;
    private final float cornerRadiusPx;

    private float sweepFraction = 0.50f;
    private long period = 1200L;
    private float length;
    private boolean running;
    private long startTimeMs;

    public BorderThrobberDrawable(float strokeWidthPx, float cornerRadiusPx, int color) {
        this.strokeWidthPx = strokeWidthPx;
        this.cornerRadiusPx = cornerRadiusPx;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidthPx);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        int clear = color & 0x00FFFFFF;
        gradientColors = new int[] { clear, color, color, clear, clear };
    }

    public void setSweepFraction(float fraction) {
        sweepFraction = Math.max(0.01f, Math.min(0.9f, fraction));
        invalidateSelf();
    }

    public void setPeriod(long period) {
        this.period = Math.max(1L, period);
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        float inset = strokeWidthPx / 2f;
        rect.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset);

        border.reset();
        length = 0f;
        if (rect.width() <= 0f || rect.height() <= 0f) {
            return;
        }

        float radius = Math.min(cornerRadiusPx, Math.min(rect.width(), rect.height()) / 2f);
        border.addRoundRect(rect, radius, radius, Path.Direction.CW);
        measure.setPath(border, true);
        length = measure.getLength();
    }

    @Override
    public void draw(Canvas canvas) {
        if (!running || length <= 0f) {
            return;
        }

        long elapsed = SystemClock.uptimeMillis() - startTimeMs;
        float start = (elapsed % period) / (float) period * length;
        float end = start + sweepFraction * length;

        segment.rewind();
        measure.getSegment(start, Math.min(end, length), segment, true);
        if (end > length) {
            wrappedSegment.rewind();
            measure.getSegment(0f, end - length, wrappedSegment, true);
            segment.addPath(wrappedSegment);
        }
        updateShader(start, end);
        canvas.drawPath(segment, paint);

        invalidateSelf();
    }

    private void updateShader(float start, float end) {
        float tailAngle = angleAt(start);
        float headArc = arcFrom(tailAngle, angleAt(end));
        float capArc = Math.max(headArc, arcFrom(tailAngle, angleAt(end + strokeWidthPx)));

        gradientStops[0] = 0f;
        gradientStops[1] = headArc / 360f;
        gradientStops[2] = capArc / 360f;
        gradientStops[3] = capArc / 360f;
        gradientStops[4] = 1f;

        float centerX = rect.centerX();
        float centerY = rect.centerY();
        SweepGradient shader = new SweepGradient(centerX, centerY, gradientColors, gradientStops);
        shaderMatrix.setRotate(tailAngle, centerX, centerY);
        shader.setLocalMatrix(shaderMatrix);
        paint.setShader(shader);
    }

    private float angleAt(float distance) {
        measure.getPosTan(distance % length, point, null);
        return (float) Math.toDegrees(
                Math.atan2(point[1] - rect.centerY(), point[0] - rect.centerX()));
    }

    private static float arcFrom(float fromAngle, float toAngle) {
        return (toAngle - fromAngle + 360f) % 360f;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        startTimeMs = SystemClock.uptimeMillis();
        invalidateSelf();
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        invalidateSelf();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return paint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
