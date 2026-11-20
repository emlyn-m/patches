package xyz.emlyn.patchextension.qbf.ig;

import java.util.function.Function;

import android.text.InputType;
import android.view.View;
import android.widget.LinearLayout;
import xyz.emlyn.patchextension.shared.Result;
import xyz.emlyn.patchextension.qbf.api.QbfNet;
import xyz.emlyn.patchextension.settings.SettingsActivity;
import xyz.emlyn.patchextension.settings.SettingsEntry;

public class QbfSettings {

    public class Prefs {
        private final static String PREF_KEY = "xyz.emlyn.patches.instagram.qbf";
        public final static SettingsEntry<Boolean> Enable = SettingsEntry.Boolean(PREF_KEY, "enable", true);
        // public final static SettingsEntry<Boolean> Autoscan =
        // SettingsEntry.Boolean(PREF_KEY, "autoscan", true);
        public final static SettingsEntry<String> ApiKey = SettingsEntry.String(PREF_KEY, "api_key", null);
        public final static SettingsEntry<Boolean> AutoOverlay = SettingsEntry.Boolean(PREF_KEY, "auto_overlay", false);
        public final static SettingsEntry<Boolean> DefaultApis = SettingsEntry.Boolean(PREF_KEY, "default_apis", true);
        // effectively disable text scanning
        public final static SettingsEntry<String> ApiUrlText = SettingsEntry.String(PREF_KEY, "api_url_text", null);
        public final static SettingsEntry<String> ApiUrlImage = SettingsEntry.String(PREF_KEY, "api_url_image",
                "https://image.external-api.pangram.com");
        public final static SettingsEntry<Boolean> DefaultModels = SettingsEntry.Boolean(PREF_KEY, "default_models",
                true);
        public final static SettingsEntry<String> ModelText = SettingsEntry.String(PREF_KEY, "model_text", null);
        public final static SettingsEntry<String> ModelImage = SettingsEntry.String(PREF_KEY, "model_image", "default");
    }

    // private static LinearLayout autoscanEntry;
    private static LinearLayout apiKeyEntry;
    private static LinearLayout autoOverlayEntry;
    private static LinearLayout defaultApiEntry;
    private static LinearLayout imageApiEntry;
    private static LinearLayout defaultModelEntry;
    private static LinearLayout imageModelEntry;

    private static int visibility(boolean visible) {
        return visible ? View.VISIBLE : View.GONE;
    }

    private static void revalidateVisibility() {
        boolean enabled = Prefs.Enable.get();
        boolean defaultApis = Prefs.DefaultApis.get();
        boolean defaultModels = Prefs.DefaultModels.get();

        // if (autoscanEntry != null)
        // autoscanEntry.setVisibility(visibility(enabled));
        if (apiKeyEntry != null)
            apiKeyEntry.setVisibility(visibility(enabled));
        if (autoOverlayEntry != null)
            autoOverlayEntry.setVisibility(visibility(enabled));
        if (defaultApiEntry != null)
            defaultApiEntry.setVisibility(visibility(enabled));
        if (imageApiEntry != null)
            imageApiEntry.setVisibility(visibility(enabled && !defaultApis));
        if (defaultModelEntry != null)
            defaultModelEntry.setVisibility(visibility(enabled));
        if (imageModelEntry != null)
            imageModelEntry.setVisibility(visibility(enabled && !defaultModels));
    }

    public static void draw(SettingsActivity ctx, LinearLayout root) {
        InstagramSettingsUI.chrome(ctx);
        InstagramSettingsUI.header(ctx, root, "QBF AI");

        InstagramSettingsUI.entryBoolean(ctx, root, "Enable", "Enable or disable all features",
                Prefs.Enable, (_v) -> revalidateVisibility());
        // autoscanEntry = InstagramSettingsUI.entryBoolean(ctx, root, "Automatically
        // scan posts",
        // "Warning! This will likely cause very high usage UNIMPL",
        // Prefs.Autoscan, (_v) -> revalidateVisibility()).first;

        Function<String, Result<String, String>> validNormApiKey = (String key) -> {
            if (key != null && !key.isBlank()) {
                return Result.Ok(key);
            }
            return Result.Err("Key cannot be empty");
        };

        // todo: show/hide
        apiKeyEntry = InstagramSettingsUI.entryText(ctx, root, "API Key", null,
                "sk-6963616e7462656c696576656974736e6f74616e6170696b657921",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                Prefs.ApiKey, validNormApiKey).first;

        autoOverlayEntry = InstagramSettingsUI.entryBoolean(ctx, root, "Automatically show overlay",
                null, Prefs.AutoOverlay, (_v) -> revalidateVisibility()).first;

        defaultApiEntry = InstagramSettingsUI.entryBoolean(ctx, root, "Use default API",
                Prefs.ApiUrlImage.defaultValue(), Prefs.DefaultApis, (_v) -> revalidateVisibility()).first;

        imageApiEntry = InstagramSettingsUI.entryText(ctx, root, "Image API", null,
                Prefs.ApiUrlImage.defaultValue(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI,
                Prefs.ApiUrlImage,
                QbfNet::validateNormalizeURL).first;

        defaultModelEntry = InstagramSettingsUI.entryBoolean(ctx, root, "Use default models",
                "Always send `" + QbfSettings.Prefs.ModelImage.defaultValue() + "` as the model selector",
                Prefs.DefaultModels, (_v) -> revalidateVisibility()).first;

        Function<String, Result<String, String>> validNormModel = (
                String x) -> (x != null && !x.isBlank()) ? Result.Ok(x) : Result.Err("Model selector cannot be empty");
        imageModelEntry = InstagramSettingsUI.entryText(ctx, root, "Image Model", null,
                Prefs.ModelImage.defaultValue(), InputType.TYPE_CLASS_TEXT, Prefs.ModelImage,
                validNormModel).first;

        revalidateVisibility();
    }
}
