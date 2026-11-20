package xyz.emlyn.patchextension.qbf.ig;

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
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import xyz.emlyn.patchextension.settings.SettingsActivity;
import xyz.emlyn.patchextension.settings.SettingsEntry;
import xyz.emlyn.patchextension.shared.Result;
import xyz.emlyn.patchextension.shared.ui.MaterialSwitch;

public class InstagramSettingsUI {
    private static final String FONT = "IGUIBetav9_Regular";
    private static final String FONT_SEMIBOLD = "IGUIBetav9_SemiBold";
    private static final String FONT_BOLD = "IGUIBetav9_Bold";

    public static final int IG_BACKGROUND = 0xffffffff;
    public static final int IG_BACKGROUND_NIGHT = 0xff0c1014;
    public static final int IG_TEXT = 0xff0c1014;
    public static final int IG_TEXT_NIGHT = 0xfff8f9f9;
    public static final int IG_TEXT_SECONDARY = 0xff686f78;
    public static final int IG_TEXT_SECONDARY_NIGHT = 0xffa2a9b4;
    public static final int IG_HINT = 0xffa2aab4;
    public static final int IG_HINT_NIGHT = 0xff5e646d;
    public static final int IG_ERR = 0xffe4010d;
    public static final int IG_ERR_NIGHT = 0xfffe7180;

    private static TextView text(SettingsActivity ctx, String text, int sp, String font, int color) {
        TextView view = new TextView(ctx);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTypeface(ctx.font(font));
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        return view;
    }

    private static TextView caption(SettingsActivity ctx, String text, int color) {
        TextView view = text(ctx, text, 12, FONT, color);
        view.setPadding(0, ctx.dp(8), 0, ctx.dp(12));
        return view;
    }

    public static void chrome(SettingsActivity ctx) {
        int bg = ctx.color(IG_BACKGROUND, IG_BACKGROUND_NIGHT);
        int fg = ctx.color(IG_TEXT, IG_TEXT_NIGHT);
        // edge-to-edge, so this also sits behind the status bar
        ctx.getWindow().setBackgroundDrawable(new ColorDrawable(bg));

        ActionBar actionBar = ctx.getActionBar();
        if (actionBar == null)
            return;
        actionBar.setBackgroundDrawable(new ColorDrawable(bg));
        actionBar.setElevation(0);

        Drawable up = ctx.getDrawable(
                ctx.getResources().getIdentifier("arrow_left_outline_24", "drawable", ctx.getPackageName()));
        up.mutate().setTint(fg);
        actionBar.setHomeAsUpIndicator(up);

        SpannableString title = new SpannableString(ctx.getTitle());
        title.setSpan(new AbsoluteSizeSpan(20, true), 0, title.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        title.setSpan(new ForegroundColorSpan(fg), 0, title.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        Typeface bold = ctx.font(FONT_BOLD);
        if (bold != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            title.setSpan(new TypefaceSpan(bold), 0, title.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        actionBar.setTitle(title);
    }

    public static void header(SettingsActivity ctx, LinearLayout root, String title) {
        TextView header = text(ctx, title, 16, FONT_SEMIBOLD, ctx.color(IG_TEXT_SECONDARY, IG_TEXT_SECONDARY_NIGHT));
        header.setMinHeight(ctx.dp(48));
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header);
    }

    public static Pair<LinearLayout, LinearLayout> wrapper(SettingsActivity ctx, LinearLayout root, String title,
            String desc, int rowHeight) {
        LinearLayout wrapper = new LinearLayout(ctx);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, ctx.dp(16), 0, 0);
        row.setLayoutParams(rowParams);
        row.setMinimumHeight(ctx.dp(rowHeight));
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView labelView = text(ctx, title, 16, FONT, ctx.color(IG_TEXT, IG_TEXT_NIGHT));
        labelView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, .4f));
        row.addView(labelView);

        Space spacer = new Space(ctx);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, .2f);
        spacer.setLayoutParams(spacerParams);
        row.addView(spacer);

        LinearLayout contentWrapper = new LinearLayout(ctx);
        contentWrapper.setOrientation(LinearLayout.HORIZONTAL);
        contentWrapper.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams contentWrapperParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, .4f);
        contentWrapperParams.setMarginStart(ctx.dp(16));
        contentWrapper.setLayoutParams(contentWrapperParams);
        row.addView(contentWrapper);

        wrapper.addView(row);
        if (desc != null)
            wrapper.addView(caption(ctx, desc, ctx.color(IG_TEXT_SECONDARY, IG_TEXT_SECONDARY_NIGHT)));

        root.addView(wrapper);
        return new Pair<>(wrapper, contentWrapper);
    }

    public static Pair<LinearLayout, LinearLayout> entryBoolean(SettingsActivity ctx, LinearLayout root, String label,
            String desc, SettingsEntry<Boolean> entry, Consumer<Boolean> onUpdate) {
        Pair<LinearLayout, LinearLayout> wrappers = wrapper(ctx, root, label, desc, 56);
        LinearLayout wrapper = wrappers.second;

        MaterialSwitch entrySwitch = new MaterialSwitch(ctx,
                "material_switch_selected_track", "material_switch_unselected_track",
                "material_switch_selected_handle", "material_switch_unselected_handle",
                null);
        entrySwitch.setChecked(entry.get());
        entrySwitch.setOnToggle((checked) -> {
            entry.put(checked);
            onUpdate.accept(checked);
        });
        wrapper.addView(entrySwitch);
        ((View) wrapper.getParent()).setOnClickListener((_v) -> entrySwitch.toggle());

        return wrappers;
    }

    private static <T> void setErrorState(TextView errorView, Result<T, String> err) {
        if (err.ok) {
            errorView.setVisibility(View.GONE);
        } else {
            errorView.setVisibility(View.VISIBLE);
            errorView.setText(err.err());
        }
    }

    public static <T> Pair<LinearLayout, LinearLayout> entryText(SettingsActivity ctx, LinearLayout root, String label,
            String desc, String hint, int inputType, SettingsEntry<T> entry,
            Function<String, Result<T, String>> validNorm) {
        Pair<LinearLayout, LinearLayout> wrappers = wrapper(ctx, root, label, desc, 48);
        LinearLayout wrapper = wrappers.second;

        TextView errorView = caption(ctx, null, ctx.color(IG_ERR, IG_ERR_NIGHT));
        errorView.setVisibility(View.GONE);
        wrappers.first.addView(errorView);

        EditText inp = new EditText(ctx);
        inp.setBackground(null);
        inp.setPadding(0, ctx.dp(12), 0, ctx.dp(12));
        inp.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        inp.setHint(hint);
        inp.setMaxLines(1);
        inp.setInputType(inputType);
        inp.setImeOptions(EditorInfo.IME_ACTION_DONE);
        inp.setText(String.valueOf(entry.get()));
        inp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        inp.setTypeface(ctx.font(FONT));
        inp.setTextColor(ctx.color(IG_TEXT_SECONDARY, IG_TEXT_SECONDARY_NIGHT));
        inp.setHintTextColor(ctx.color(IG_HINT, IG_HINT_NIGHT));
        inp.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        inp.setOnEditorActionListener((v, actionId, event) -> {
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
        inp.setOnFocusChangeListener((v, focused) -> {
            if (!focused) {
                String newRaw = ((TextView) v).getText().toString();
                Result<T, String> normed = validNorm.apply(newRaw);
                setErrorState(errorView, normed);
                if (normed.ok) {
                    entry.put(normed.unwrap());
                    ((TextView) v).setText(String.valueOf(normed.unwrap()));
                }

                if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    ((EditText) v).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            } else {
                if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    ((EditText) v)
                            .setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                }
            }
        });

        wrapper.addView(inp);

        return wrappers;
    }
}
