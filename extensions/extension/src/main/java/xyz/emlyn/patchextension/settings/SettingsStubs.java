package xyz.emlyn.patchextension.settings;

import android.content.Intent;

import java.util.List;

// stubs: implemented by patch
@SuppressWarnings("unused")
public class SettingsStubs {
    // TextSpec.Literal(text)
    public static Object literal(String text) {
        throw new UnsupportedOperationException("SettingsStubs.literal not implemented");
    }

    // NavigateToIntent(intent)
    public static Object navigateToIntent(Intent intent) {
        throw new UnsupportedOperationException("SettingsStubs.navigateToIntent not implemented");
    }

    // SettingsListItem(title, description, navigationTarget = target), description may be null
    public static Object item(Object title, Object description, Object target) {
        throw new UnsupportedOperationException("SettingsStubs.item not implemented");
    }

    // SettingsListSection(title = null, description = null, items)
    public static Object section(List<Object> items) {
        throw new UnsupportedOperationException("SettingsStubs.section not implemented");
    }
}
