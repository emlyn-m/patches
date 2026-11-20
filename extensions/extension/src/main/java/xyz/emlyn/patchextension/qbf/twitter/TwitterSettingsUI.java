package xyz.emlyn.patchextension.qbf.twitter;

import java.util.function.Consumer;
import java.util.function.Function;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import xyz.emlyn.patchextension.settings.SettingsActivity;
import xyz.emlyn.patchextension.settings.SettingsEntry;
import xyz.emlyn.patchextension.shared.Result;
import xyz.emlyn.patchextension.shared.ui.MaterialSwitch;

public class TwitterSettingsUI {
    private static final String FONT = "chirp_regular_400";
    private static final String FONT_SEMIBOLD = "chirp_medium_500";
    private static final String FONT_BOLD = "chirp_bold_700";

    private static void setEntryErrorState(SettingsActivity ctx, TextView v, String error) {
        FrameLayout majWrapper = (FrameLayout) v.getParent().getParent().getParent();

        TextView e = ((TextView) majWrapper.getChildAt(1));
        e.setText(error == null ? "" : error);
    }

    private static TextView text(SettingsActivity ctx, String text, int sp, String font, int color) {
        TextView view = new TextView(ctx);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTypeface(ctx.font(font));
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        return view;
    }

    public static void chrome(SettingsActivity ctx) {
        int bg = ctx.color(TwtColor.BACKGROUND, TwtColor.BACKGROUND_NIGHT);
        int fg = ctx.color(TwtColor.TEXT, TwtColor.TEXT_NIGHT);

        ctx.getWindow().setBackgroundDrawable(new ColorDrawable(bg));

        ActionBar actionBar = ctx.getActionBar();
        if (actionBar == null)
            return;
        actionBar.setBackgroundDrawable(new ColorDrawable(bg));
        actionBar.setElevation(0);

        Drawable up = ctx.getDrawable(
                ctx.getResources().getIdentifier("ic_vector_arrow_left", "drawable", ctx.getPackageName()));
        up.mutate().setTint(fg);
        actionBar.setHomeAsUpIndicator(up);

        SpannableString title = new SpannableString(ctx.getTitle());
        title.setSpan(new AbsoluteSizeSpan(20, true), 0, title.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        title.setSpan(new ForegroundColorSpan(fg), 0, title.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        Typeface bold = ctx.font(FONT_BOLD);
        if (bold != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            title.setSpan(new TypefaceSpan(bold), 0, title.length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        actionBar.setTitle(title);
    }

    public static void groupHeader(SettingsActivity ctx, LinearLayout root, String title) {
        TextView header = text(ctx, title, 12, FONT_BOLD,
                ctx.color(TwtColor.TEXT_SECONDARY, TwtColor.TEXT_SECONDARY_NIGHT));
        header.setPadding(ctx.dp(2), 0, 0, 0);
        root.addView(header);
    }

    private static Pair<FrameLayout, LinearLayout> wrapper(SettingsActivity ctx, LinearLayout parent) {
        FrameLayout frame = new FrameLayout(ctx);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        frameParams.setMargins(0, ctx.dp(16), 0, ctx.dp(8));
        frame.setLayoutParams(frameParams);

        LinearLayout wrapper = new LinearLayout(ctx);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        wrapperParams.setMargins(0, ctx.dp(16), 0, 0);
        wrapper.setLayoutParams(wrapperParams);
        wrapper.setGravity(Gravity.CENTER_VERTICAL);
        frame.addView(wrapper);

        TextView errorState = text(ctx, "", 10, FONT_BOLD, ctx.color(TwtColor.ERR, TwtColor.ERR_NIGHT));
        FrameLayout.LayoutParams errorParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        errorParams.gravity = Gravity.TOP | Gravity.START;
        errorParams.setMargins(ctx.dp(2), ctx.dp(6), 0, 0);
        errorState.setLayoutParams(errorParams);
        frame.addView(errorState);

        parent.addView(frame);
        return new Pair<>(frame, wrapper);
    }

    private static LinearLayout entryLabel(SettingsActivity ctx, String label, String desc) {
        LinearLayout wrapper = new LinearLayout(ctx);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        wrapper.addView(text(ctx, label, 14, FONT_SEMIBOLD, ctx.color(TwtColor.TEXT, TwtColor.TEXT_NIGHT)));
        if (desc != null) {
            TextView descView = text(ctx, desc, 12, FONT, ctx.color(TwtColor.HINT, TwtColor.HINT_NIGHT));
            descView.setPadding(0, ctx.dp(4), 0, 0);
            wrapper.addView(descView);
        }
        return wrapper;
    }

    public static Pair<FrameLayout, LinearLayout> entryBoolean(SettingsActivity ctx, LinearLayout root, String label,
            String desc, SettingsEntry<Boolean> entry, Consumer<Boolean> onUpdate) {
        Pair<FrameLayout, LinearLayout> wrappers = wrapper(ctx, root);
        LinearLayout wrapper = wrappers.second;

        LinearLayout labelWrapper = entryLabel(ctx, label, desc);
        LinearLayout.LayoutParams labelWrapperParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, .4f);
        labelWrapper.setLayoutParams(labelWrapperParams);
        wrapper.addView(labelWrapper);

        Space spacer = new Space(ctx);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, .2f);
        spacer.setLayoutParams(spacerParams);
        wrapper.addView(spacer);

        MaterialSwitch entrySwitch = new MaterialSwitch(ctx,
                "mtrl_switch_track", "mtrl_switch_track_decoration",
                "mtrl_switch_thumb_checked", "mtrl_switch_thumb_unchecked",
                ctx.isNight() ? "ThemeOverlay.Material3.Dark" : "ThemeOverlay.Material3.Light");
        if (ctx.isNight()) {
            entrySwitch.setTints(TwtColor.ACCENT_NIGHT, TwtColor.TEXT_SECONDARY_NIGHT,
                    TwtColor.BACKGROUND_NIGHT,
                    TwtColor.TEXT_SECONDARY_NIGHT);
        } else {
            entrySwitch.setTints(TwtColor.ACCENT, TwtColor.TEXT_SECONDARY, TwtColor.BACKGROUND,
                    TwtColor.TEXT_SECONDARY);
        }

        entrySwitch.setChecked(entry.get());
        entrySwitch.setOnToggle((checked) -> {
            entry.put(checked);
            onUpdate.accept(checked);
        });
        entrySwitch.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        ((View) wrapper.getParent()).setOnClickListener((_v) -> entrySwitch.toggle());
        wrapper.addView(entrySwitch);

        return wrappers;
    }

    public static <T> Pair<FrameLayout, LinearLayout> entryText(SettingsActivity ctx, LinearLayout root, String label,
            String desc, String hint, int inputType, SettingsEntry<T> entry,
            Function<String, Result<T, String>> validateNormalize) {
        Pair<FrameLayout, LinearLayout> wrappers = wrapper(ctx, root);
        LinearLayout wrapper = wrappers.second;

        LinearLayout labelWrapper = entryLabel(ctx, label, desc);
        LinearLayout.LayoutParams labelWrapperParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, .4f);
        labelWrapper.setLayoutParams(labelWrapperParams);
        wrapper.addView(labelWrapper);

        Space spacer = new Space(ctx);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, .2f);
        spacer.setLayoutParams(spacerParams);
        wrapper.addView(spacer);

        EditText entryInput = new EditText(ctx);
        entryInput.setPadding(0, ctx.dp(12), 0, ctx.dp(12));
        entryInput.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        entryInput.setHint(hint);
        entryInput.setMaxLines(1);
        entryInput.setInputType(inputType);
        entryInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        entryInput.setText(String.valueOf(entry.get()));
        entryInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        entryInput.setTypeface(ctx.font(FONT));
        entryInput.setPadding(ctx.dp(4), ctx.dp(8), ctx.dp(4), ctx.dp(8));
        entryInput.setTextColor(ctx.color(TwtColor.TEXT_SECONDARY, TwtColor.TEXT_SECONDARY_NIGHT));
        entryInput.setHintTextColor(ctx.color(TwtColor.HINT, TwtColor.HINT_NIGHT));
        entryInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        entryInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
                InputMethodManager imm = (InputMethodManager) v.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                v.clearFocus();
                return true;
            } else {
                return false;
            }
        });
        entryInput.setOnFocusChangeListener((v, focused) -> {
            if (focused) {
                if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    ((EditText) v)
                            .setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                }
                v.post(() -> ((EditText) v).setSelection(((TextView) v).getText().length()));
            } else {
                String newVal = ((TextView) v).getText().toString();
                Result<T, String> entryVal = validateNormalize.apply(newVal);
                if (entryVal.ok) {
                    T normed = entryVal.unwrap();
                    ((TextView) v).setText(String.valueOf(normed));
                    setEntryErrorState(ctx, (TextView) v, null);
                    entry.put(normed);
                } else {
                    setEntryErrorState(ctx, (TextView) v, entryVal.err());
                }
                if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    ((EditText) v).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });
        LinearLayout inputWrapper = new LinearLayout(ctx);
        inputWrapper.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, .4f));
        inputWrapper.addView(entryInput);
        inputWrapper.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        wrapper.addView(inputWrapper);

        return wrappers;
    }
}
