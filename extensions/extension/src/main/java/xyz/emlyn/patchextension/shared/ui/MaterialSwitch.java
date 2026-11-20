package xyz.emlyn.patchextension.shared.ui;

import java.util.function.Consumer;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Checkable;
import android.widget.Switch;

public class MaterialSwitch extends View implements Checkable {
    private static final float WIDTH = 52, HEIGHT = 32, THUMB_OFF = 16, THUMB_ON = 24;

    private final float density;
    private final Drawable trackOn, trackOff, thumbOn, thumbOff;
    private boolean checked;
    private float progress;
    private ValueAnimator anim;
    private Consumer<Boolean> onToggle;

    public MaterialSwitch(Activity ctx, String trackOnId, String trackOffId, String thumbOnId,
            String thumbOffId, String themeOverlay) {
        super(ctx);
        density = ctx.getResources().getDisplayMetrics().density;
        Resources.Theme theme = ctx.getTheme();
        if (themeOverlay != null) {
            theme = ctx.getResources().newTheme();
            theme.setTo(ctx.getTheme());
            theme.applyStyle(id(ctx, themeOverlay, "style"), true);
        }
        trackOn = drawable(ctx, theme, trackOnId);
        trackOff = drawable(ctx, theme, trackOffId);
        thumbOn = drawable(ctx, theme, thumbOnId);
        thumbOff = drawable(ctx, theme, thumbOffId);
        setOnClickListener((_v) -> toggle());
    }

    private static int id(Activity ctx, String name, String type) {
        return ctx.getResources().getIdentifier(name, type, ctx.getPackageName());
    }

    private static Drawable drawable(Activity ctx, Resources.Theme theme, String name) {
        return ctx.getResources().getDrawable(id(ctx, name, "drawable"), theme).mutate();
    }

    public void setTints(Integer trackOn, Integer trackOff, Integer thumbOn, Integer thumbOff) {
        tint(this.trackOn, trackOn);
        tint(this.trackOff, trackOff);
        tint(this.thumbOn, thumbOn);
        tint(this.thumbOff, thumbOff);
        invalidate();
    }

    private static void tint(Drawable d, Integer color) {
        if (color != null)
            d.setTintList(ColorStateList.valueOf(color));
    }

    public void setOnToggle(Consumer<Boolean> onToggle) {
        this.onToggle = onToggle;
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (anim != null)
            anim.cancel();
        this.checked = checked;
        progress = checked ? 1 : 0;
        invalidate();
    }

    @Override
    public void toggle() {
        checked = !checked;
        if (anim != null)
            anim.cancel();
        anim = ValueAnimator.ofFloat(progress, checked ? 1 : 0).setDuration(150);
        anim.addUpdateListener((a) -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
        if (onToggle != null)
            onToggle.accept(checked);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(Math.round(WIDTH * density), Math.round(HEIGHT * density));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        drawFaded(canvas, trackOff, 1 - progress, 0, 0, w, h);
        drawFaded(canvas, trackOn, progress, 0, 0, w, h);

        float r = (THUMB_OFF + (THUMB_ON - THUMB_OFF) * progress) * density / 2;
        float cx = h / 2f + (w - h) * progress, cy = h / 2f;
        int l = Math.round(cx - r), t = Math.round(cy - r), rr = Math.round(cx + r), b = Math.round(cy + r);
        drawFaded(canvas, thumbOff, 1 - progress, l, t, rr, b);
        drawFaded(canvas, thumbOn, progress, l, t, rr, b);
    }

    private static void drawFaded(Canvas canvas, Drawable d, float alpha, int l, int t, int r, int b) {
        if (alpha <= 0)
            return;
        d.setBounds(l, t, r, b);
        d.setAlpha(Math.round(alpha * 255));
        d.draw(canvas);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Switch.class.getName();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setCheckable(true);
        info.setChecked(checked);
    }
}
