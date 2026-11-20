package xyz.emlyn.patchextension.qbf.twitter;

import xyz.emlyn.patchextension.qbf.api.QbfNet;
import xyz.emlyn.patchextension.settings.SettingsActivity;
import xyz.emlyn.patchextension.settings.SettingsEntry;
import xyz.emlyn.patchextension.shared.Result;

import java.util.function.Function;

import android.text.InputType;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.FrameLayout;

public class Settings {
    public class Prefs {
        private final static String PREF_KEY = "xyz.emlyn.patches.twitter.qbf";
        public final static SettingsEntry<Boolean> Enable = SettingsEntry.Boolean(PREF_KEY, "enable", true);
        public final static SettingsEntry<Boolean> Autoscan = SettingsEntry.Boolean(PREF_KEY, "autoscan", false);
        public final static SettingsEntry<Integer> AutoscanLength = SettingsEntry.Int(PREF_KEY, "autoscan_length", 240);
        public final static SettingsEntry<Boolean> AutoOverlay = SettingsEntry.Boolean(PREF_KEY, "auto_overlay", false);
        // public final static SettingsEntry<Boolean> AutoscanImages =
        // SettingsEntry.Boolean(PREF_KEY, "autoscan_images", false);
        public final static SettingsEntry<String> ApiKey = SettingsEntry.String(PREF_KEY, "api_key", null);
        public final static SettingsEntry<Boolean> DefaultApis = SettingsEntry.Boolean(PREF_KEY, "default_apis", true);
        public final static SettingsEntry<String> ApiUrlText = SettingsEntry.String(PREF_KEY, "api_url_text",
                "https://text.external-api.pangram.com");
        // public final static SettingsEntry<String> ApiUrlImage =
        // SettingsEntry.String(PREF_KEY, "api_url_image",
        // "http://localhost:8000/v1/tasks");
        public final static SettingsEntry<Boolean> DefaultModels = SettingsEntry.Boolean(PREF_KEY, "default_models",
                true);
        public final static SettingsEntry<String> ModelText = SettingsEntry.String(PREF_KEY, "model_text", "default");
        // public final static SettingsEntry<String> ModelImage =
        // SettingsEntry.String(PREF_KEY, "model_image", "default");
    }

    private static FrameLayout autoscanEntry;
    private static FrameLayout autoscanLengthEntry;
    private static FrameLayout autoOverlayEntry;
    // private static FrameLayout autoscanImagesEntry;
    private static FrameLayout apiKeyEntry;
    private static FrameLayout defaultApiEntry;
    private static FrameLayout textApiEntry;
    private static FrameLayout imgApiEntry;
    private static FrameLayout defaultModelsEntry;
    private static FrameLayout textModelEntry;
    private static FrameLayout imgModelEntry;

    public static void draw(SettingsActivity ctx, LinearLayout root) {
        TwitterSettingsUI.chrome(ctx);
        TwitterSettingsUI.groupHeader(ctx, root, "QBF AI");

        Runnable revalidateVisibility = () -> {
            Function<Boolean, Integer> visibility = x -> x ? View.VISIBLE : View.GONE;
            boolean enabled = Settings.Prefs.Enable.get();
            boolean autoscan = Settings.Prefs.Autoscan.get();
            boolean defaultApis = Settings.Prefs.DefaultApis.get();
            boolean defaultModels = Settings.Prefs.DefaultModels.get();

            if (autoscanEntry != null)
                autoscanEntry.setVisibility(visibility.apply(enabled));
            if (autoscanLengthEntry != null)
                autoscanLengthEntry.setVisibility(visibility.apply(enabled && autoscan));
            // if (autoscanImagesEntry != null)
            // autoscanImagesEntry.setVisibility(visibility.apply(enabled && autoscan));
            if (autoOverlayEntry != null)
                autoOverlayEntry.setVisibility(visibility.apply(enabled));
            if (apiKeyEntry != null)
                apiKeyEntry.setVisibility(visibility.apply(enabled));
            if (defaultApiEntry != null)
                defaultApiEntry.setVisibility(visibility.apply(enabled));
            if (textApiEntry != null)
                textApiEntry.setVisibility(visibility.apply(enabled && !defaultApis));
            if (imgApiEntry != null)
                imgApiEntry.setVisibility(visibility.apply(enabled && !defaultApis));
            if (defaultModelsEntry != null)
                defaultModelsEntry.setVisibility(visibility.apply(enabled));
            if (textModelEntry != null)
                textModelEntry.setVisibility(visibility.apply(enabled && !defaultModels));
            if (imgModelEntry != null)
                imgModelEntry.setVisibility(visibility.apply(enabled && !defaultModels));

        };

        TwitterSettingsUI.entryBoolean(ctx, root, "Enable AI detection", null, Settings.Prefs.Enable,
                (enabled) -> revalidateVisibility.run());

        autoscanEntry = TwitterSettingsUI.entryBoolean(ctx, root, "Automatically scan posts",
                null, Settings.Prefs.Autoscan, autoscan -> revalidateVisibility.run()).first;
        Function<String, Result<Integer, String>> validNormAutoscanLength = (String lengthS) -> {
            try {
                int length = Integer.parseInt(lengthS);
                return Result.Ok(length);
            } catch (NumberFormatException _e) {
                return Result.Err("Invalid length \"" + lengthS + "\"");
            }
        };
        autoscanLengthEntry = TwitterSettingsUI.entryText(ctx, root, "Minimum length",
                "Scan posts ≥ this many characters", "240", InputType.TYPE_CLASS_NUMBER,
                Settings.Prefs.AutoscanLength, validNormAutoscanLength).first;
        Function<String, Result<String, String>> validNormApiKey = (String key) -> {
            if (key == null || key.isBlank()) {
                return Result.Err("API key cannot be blank");
            }
            return Result.Ok(key);
        };
        // autoscanImagesEntry = TwitterSettingsUI.entryBoolean(ctx, root,
        // "Automatically scan images", "UNIMPL",
        // Settings.Prefs.AutoscanImages, (_enabled) ->
        // revalidateVisibility.run()).first;

        autoOverlayEntry = TwitterSettingsUI.entryBoolean(ctx, root, "Automatically show overlay",
                null, Prefs.AutoOverlay, (_enabled) -> revalidateVisibility.run()).first;

        apiKeyEntry = TwitterSettingsUI.entryText(ctx, root, "API Key", "Sent as 'x-api-key'",
                "sk-6963616e7462656c696576656974736e6f74616e6170696b657921",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
                Settings.Prefs.ApiKey, validNormApiKey).first;
        defaultApiEntry = TwitterSettingsUI.entryBoolean(ctx, root, "Use default APIs",
                Prefs.ApiUrlText.defaultValue(), // + "\n" + Prefs.ApiUrlImage.defaultValue(),
                Settings.Prefs.DefaultApis, (useDefault) -> {
                    revalidateVisibility.run();
                }).first;
        textApiEntry = TwitterSettingsUI.entryText(ctx, root, "Text API url", null,
                Settings.Prefs.ApiUrlText.defaultValue(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI,
                Settings.Prefs.ApiUrlText, QbfNet::validateNormalizeURL).first;
        // imgApiEntry = TwitterSettingsUI.entryText(ctx, root, "Image API url",
        // null, Settings.Prefs.ApiUrlImage.defaultValue(),
        // InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI,
        // Settings.Prefs.ApiUrlImage, QbfNet::validateNormalizeURL).first;

        defaultModelsEntry = TwitterSettingsUI.entryBoolean(ctx, root,
                "Use default models", "Always send 'default' as the model slug",
                Settings.Prefs.DefaultModels, (useDefault) -> revalidateVisibility.run()).first;
        Function<String, Result<String, String>> validNormModel = (
                String model) -> model.isBlank() ? Result.Err("Model slug cannot be blank") : Result.Ok(model);
        textModelEntry = TwitterSettingsUI.entryText(ctx, root, "Text Model", null,
                Settings.Prefs.ModelText.defaultValue(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT,
                Settings.Prefs.ModelText,
                validNormModel).first;
        // imgModelEntry = TwitterSettingsUI.entryText(ctx, root, "Image Model", null,
        // Settings.Prefs.ModelImage.defaultValue(),
        // InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT,
        // Settings.Prefs.ModelImage,
        // (String model) -> model.isBlank() ? Result.Err("Model slug cannot be blank")
        // : Result.Ok(model)).first;

        revalidateVisibility.run();
    }
}
