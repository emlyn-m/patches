package xyz.emlyn.patchextension.settings;

import android.animation.LayoutTransition;
import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.EditText;
import android.graphics.Insets;
import android.graphics.Rect;
import android.graphics.Typeface;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(isNight()
                ? android.R.style.Theme_DeviceDefault
                : android.R.style.Theme_DeviceDefault_Light);
        super.onCreate(savedInstanceState);
        setTitle("Patches");

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        root.setLayoutTransition(new LayoutTransition());
        this.drawGroups(root);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        if (!isNight())
            lightStatusBar();

        // keep content clear of action bar + keyboard >= API36
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            findViewById(android.R.id.content).setOnApplyWindowInsetsListener((v, insets) -> {
                Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                v.setPadding(i.left, i.top, i.right, i.bottom);
                return insets;
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void drawGroups(LinearLayout root) {
        // patches inject their own calls here
    }

    @SuppressWarnings("deprecation")
    private void lightStatusBar() {
        Window window = getWindow();
        // pre-API35 isn't edge-to-edge, bar has its own colour
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            TypedValue bg = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.colorBackground, bg, true);
            window.setStatusBarColor(bg.data);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.getInsetsController().setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        } else {
            View decor = window.getDecorView();
            decor.setSystemUiVisibility(decor.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    public boolean isNight() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    public int dp(int v) {
        return Math
                .round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    public int color(int light, int night) {
        return isNight() ? night : light;
    }

    public Typeface font(String name) {
        int id = getResources().getIdentifier(name, "font", getPackageName());
        if (id == 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null;
        }
        return getResources().getFont(id);
    }

}
