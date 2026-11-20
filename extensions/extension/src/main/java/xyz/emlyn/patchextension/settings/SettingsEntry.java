package xyz.emlyn.patchextension.settings;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SettingsEntry<T> {
    private final String key;
    private final T defaultValue;
    public final Class<T> type;

    private final Function<SettingsEntry<T>, T> get;
    private final BiConsumer<SettingsEntry<T>, T> put;

    private SettingsEntry(String key, T defaultValue, Class<T> type, Function<SettingsEntry<T>, T> get,
            BiConsumer<SettingsEntry<T>, T> put) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.type = type;
        this.get = get;
        this.put = put;
    }

    public String key() {
        return this.key;
    }

    public T defaultValue() {
        return this.defaultValue;
    }

    public T get() {
        return get.apply(this);
    }

    public void put(T t) {
        put.accept(this, t);
    }

    public static SettingsEntry<Integer> Int(String key, int def, Function<SettingsEntry<Integer>, Integer> get,
            BiConsumer<SettingsEntry<Integer>, Integer> put) {
        return new SettingsEntry<>(key, def, Integer.class, get, put);
    }

    public static SettingsEntry<String> String(String key, String def, Function<SettingsEntry<String>, String> get,
            BiConsumer<SettingsEntry<String>, String> put) {
        return new SettingsEntry<>(key, def, String.class, get, put);
    }

    public static SettingsEntry<Boolean> Boolean(String key, Boolean def, Function<SettingsEntry<Boolean>, Boolean> get,
            BiConsumer<SettingsEntry<Boolean>, Boolean> put) {
        return new SettingsEntry<>(key, def, Boolean.class, get, put);
    }
}
