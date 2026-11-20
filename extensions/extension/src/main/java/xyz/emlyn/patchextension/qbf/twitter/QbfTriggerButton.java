package xyz.emlyn.patchextension.qbf.twitter;

import xyz.emlyn.patchextension.shared.logging.Logging;
import xyz.emlyn.patchextension.shared.logging.Lvl;
import xyz.emlyn.patchextension.qbf.api.QbfJson;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.util.TypedValue;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import kotlin.jvm.functions.Function1;

@SuppressWarnings("unused")
public class QbfTriggerButton {
    private static final int TAG_POSTID = 0x8ea34adc; // >= 0x20000000
    // focal post header is scaled up, matches its 8dp button spacing
    private static final float FOCAL_SCALE = 4f / 3f;
    private static final Function1<Context, View> FACTORY = ctx -> new ButtonView(ctx, 1f);
    private static final Function1<Context, View> FOCAL_FACTORY = ctx -> new ButtonView(ctx, FOCAL_SCALE);
    // main thread only
    private static final Set<ButtonView> attached = Collections.newSetFromMap(new WeakHashMap<>());

    // composable, called from timeline post headers
    public static void render(Object post, Object composer) {
        Compose.androidView(FACTORY, Compose.padding(Compose.headerAlign(), 6f, 0f, 0f, 0f), update(post), composer);
    }

    // composable, called from the focal post header
    public static void renderFocal(Object post, Object composer) {
        Compose.androidView(FOCAL_FACTORY, Compose.padding(Compose.headerAlign(), 6f * FOCAL_SCALE, 0f, 0f, 0f),
                update(post), composer);
    }

    private static Context context() throws Exception {
        return (Context) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
    }

    private static int dp(Context ctx, float px) {
        return Math.round(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, ctx.getResources().getDisplayMetrics()));
    }

    public static void showToast(String msg) {
        try {
            Toast.makeText(context(), msg, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Logging.log(Lvl.Error, "Unable to fetch context! Not showing toast: " + msg);
        }
    }

    static class ButtonView extends FrameLayout {
        private static final int CHILD_ICON = 0;
        private static final int CHILD_TEXT = 1;

        final ImageView icon;
        final TextView text;
        final GradientDrawable bg;
        final GradientDrawable showOverlay1;
        final GradientDrawable showOverlay2;
        final BorderThrobberDrawable throbber;
        final float scale;

        ButtonView(Context context, float scale) {
            super(context);
            this.scale = scale;
            int padH = dp(context, 6f * scale);
            int padV = dp(context, 2.25f * scale);
            setPadding(padH, padV, padH, padV);
            float radius = dp(context, 11.5f * scale);

            icon = new ImageView(context);
            int iconRes = context.getResources().getIdentifier("qbf_icon", "drawable", context.getPackageName());
            if (iconRes != 0)
                icon.setImageResource(iconRes);
            else
                Logging.log(Lvl.Error, "qbf_icon drawable not found");

            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int iconSize = Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 12 * scale, context.getResources().getDisplayMetrics()));
            addView(icon, CHILD_ICON, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));

            text = new TextView(context);
            text.setTextSize(10 * scale);
            text.setGravity(Gravity.CENTER);
            text.setTypeface(text.getTypeface(), Typeface.BOLD);
            // INVISIBLE (not GONE) so both children always get measured and positioned
            text.setVisibility(View.INVISIBLE);
            addView(text, CHILD_TEXT, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

            bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(radius);
            bg.setColor(TwtColor.AI_UNSCANNED);

            showOverlay1 = new GradientDrawable();
            showOverlay1.setShape(GradientDrawable.RECTANGLE);
            showOverlay1.setCornerRadius(radius);
            showOverlay1.setColor(TwtColor.NONE);

            showOverlay2 = new GradientDrawable();
            showOverlay2.setShape(GradientDrawable.RECTANGLE);
            showOverlay2.setCornerRadius(radius);
            showOverlay2.setColor(TwtColor.NONE);

            throbber = new BorderThrobberDrawable(dp(context, 0.75f * scale), radius, TwtColor.AI_AI);

            setBackground(new LayerDrawable(new Drawable[] { bg, showOverlay1, showOverlay2, throbber }));
        }

        // update will have ran if scan completed offscreen, ensure redraw
        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            attached.add(this);
            redraw(this);
        }

        @Override
        protected void onDetachedFromWindow() {
            attached.remove(this);
            super.onDetachedFromWindow();
        }

        private int contentGen = 0;
        private int measuredGen = -1;
        private int fixAttempts = 0;
        private boolean fixPending = false;

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // a layout without a measure clears the children's force-layout flag, so they'd
            // return cached sizes
            icon.forceLayout();
            text.forceLayout();
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            measuredGen = contentGen;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            if (right > left)
                scheduleFix(0);
        }

        private void contentChanged() {
            contentGen++;
            fixAttempts = 0;
            if (getWidth() > 0)
                scheduleFix(0);
        }

        private void scheduleFix(long delayMs) {
            if (fixPending)
                return;
            fixPending = true;
            postDelayed(() -> {
                fixPending = false;
                fixStale();
            }, delayMs);
        }

        // compose drops remeasures for prefetched/offscreen items, so re-request once
        // we're onscreen
        private void fixStale() {
            boolean stale = measuredGen != contentGen
                    || getWidth() != getMeasuredWidth() || getHeight() != getMeasuredHeight();
            if (!stale || getWidth() == 0 || fixAttempts >= 5)
                return;
            fixAttempts++;
            requestLayout();
            ViewParent holder = getParent();
            if (holder instanceof View && holder.getParent() != null) {
                ((View) holder).forceLayout();
                holder.getParent().requestLayout();
            }
            // compose sometimes drops the request too (clumsypose), check again in a few
            // frames
            scheduleFix(50);
        }

        void showIcon() {
            if (icon.getVisibility() == View.VISIBLE)
                return;
            icon.setVisibility(View.VISIBLE);
            text.setVisibility(View.INVISIBLE);
            contentChanged();
        }

        void showText(CharSequence s) {
            if (text.getVisibility() == View.VISIBLE && s.toString().equals(text.getText().toString()))
                return;
            text.setText(s);
            text.setVisibility(View.VISIBLE);
            icon.setVisibility(View.INVISIBLE);
            contentChanged();
        }

        void setTextColor(int color) {
            text.setTextColor(color);
        }
    }

    public static void register(Object post) {
        if (!Settings.Prefs.Enable.get()) {
            return;
        }
        TwitterRepository.registerData(Tweet.fromObfuscated(post));
    }

    public static Function1<View, Object> update(Object post) {
        if (Settings.Prefs.Enable.get()) {
            return view -> {
                Tweet twt = Tweet.fromObfuscated(post);
                view.setTag(QbfTriggerButton.TAG_POSTID, twt.id);
                TwitterRepository.registerData(twt);

                view.setOnClickListener(v -> {
                    try {
                        Object tag = v.getTag(QbfTriggerButton.TAG_POSTID);
                        Tweet entry = (tag != null) ? TwitterRepository.entry(tag.toString()) : null;
                        if (entry != null && entry.error != null)
                            QbfTriggerButton.showToast("Retrying, last scan failed: " + entry.error);
                        if (entry == null || entry.result == null)
                            TwitterRepository.checkAI(twt.id, v, v2 -> QbfTriggerButton.redraw(v2));
                        else
                            TwitterRepository.toggleOverlay(twt.id, v, v2 -> QbfTriggerButton.redraw(v2));
                    } catch (Exception e) {
                        Logging.log(Lvl.Error, "qbf click listener threw: " + e);
                    }
                });

                // don't autoscan failed posts, update() reruns on every recompose
                Tweet registered = TwitterRepository.entry(twt.id);
                boolean failed = registered != null && registered.error != null;
                if (Settings.Prefs.Autoscan.get() && !failed) {
                    if (Settings.Prefs.AutoscanLength.get() <= twt.longText.length()) {
                        TwitterRepository.checkAI(twt.id, view, v2 -> QbfTriggerButton.redraw(v2));
                    }
                }

                QbfTriggerButton.redraw(view);
                return null;
            };
        } else {
            return view -> {
                QbfTriggerButton.redraw(view);
                return null;
            };
        }

    }

    static void redrawPost(String id) {
        // views may be recycled between posts
        for (ButtonView b : new ArrayList<>(attached))
            if (id.equals(b.getTag(TAG_POSTID)))
                redraw(b);
    }

    public static void redraw(View v) {
        ButtonView b = (ButtonView) v;
        Object tag = v.getTag(QbfTriggerButton.TAG_POSTID);
        Tweet entry = (tag != null) ? TwitterRepository.entry(tag.toString()) : null;

        GradientDrawable bg = b.bg;
        GradientDrawable showOverlay1 = b.showOverlay1;
        GradientDrawable showOverlay2 = b.showOverlay2;
        BorderThrobberDrawable throbber = b.throbber;

        Context ctx;
        try {
            ctx = context();
        } catch (Exception e) {
            Logging.log(Lvl.Error, "Unable to get context in QbfTriggerButton! Refusing to draw");
            return;
        }
        int mode = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNight = mode == Configuration.UI_MODE_NIGHT_YES;

        if (!Settings.Prefs.Enable.get()) {
            v.setVisibility(View.GONE);
            return;
        }
        v.setVisibility(View.VISIBLE);
        if (entry == null || entry.result == null) {
            bg.setColor(isNight ? TwtColor.AI_UNSCANNED_NIGHT : TwtColor.AI_UNSCANNED);
            showOverlay1.setStroke(0, TwtColor.NONE);
            showOverlay2.setStroke(0, TwtColor.NONE);

            if (entry != null && entry.error != null && !entry.scanning) {
                bg.setStroke(dp(ctx, 1.5f), TwtColor.SCAN_ERROR);
                b.setTextColor(TwtColor.SCAN_ERROR);
                b.showText("!");
            } else {
                bg.setStroke(0, TwtColor.NONE);
                b.showIcon();
            }
            if (entry != null && entry.scanning) {
                throbber.start();
            } else {
                throbber.stop();
            }
        } else {
            QbfJson.TextScan res = entry.result;

            int color = res.prediction_short.equals("AI") ? TwtColor.AI_AI
                    : res.prediction_short.equals("Mixed") ? TwtColor.AI_ASSISTED : TwtColor.AI_HUMAN;
            String prefix = res.prediction_short.equals("Human") ? "H " : "AI";
            float score = res.prediction_short.equals("AI") ? res.fraction_ai
                    : res.prediction_short.equals("Mixed") ? res.fraction_ai_assisted : res.fraction_human;

            bg.setColor(color);
            bg.setStroke(0, TwtColor.NONE);
            throbber.stop();

            b.setTextColor(isNight ? TwtColor.TEXT : TwtColor.TEXT_NIGHT);
            b.showText(prefix + " " + String.valueOf(Math.round(100 * score)) + "%");

            if (entry.overlayVisible) {
                showOverlay1.setStroke(Math.round(dp(ctx, 2.25f * b.scale)),
                        isNight ? TwtColor.BACKGROUND_NIGHT : TwtColor.BACKGROUND);
                showOverlay2.setStroke(Math.round(dp(ctx, 1.125f * b.scale)), color);
            } else {
                showOverlay1.setStroke(0, TwtColor.NONE);
                showOverlay2.setStroke(0, TwtColor.NONE);
            }
        }
    }
}
