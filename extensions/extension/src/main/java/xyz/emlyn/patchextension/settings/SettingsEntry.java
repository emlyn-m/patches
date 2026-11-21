package xyz.emlyn.patchextension.settings;

public class SettingsEntry<T> {
    private final String id;
    private final String key;
    public final Class<T> type;
    private T defaultValue;

    private SettingsEntry(String id, String key, T defaultValue, Class<T> type) {
        this.id = id;
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String id() {
        return this.id;
    }

    public String key() {
        return this.key;
    }

    public T defaultValue() {
        return this.defaultValue;
    }

    public void setDefault(T def) {
        this.defaultValue = def;
    }

    public T get() {
        return Settings.getEntry(this);
    }

    public void put(T t) {
        Settings.putEntry(this, t);
    }

    public static SettingsEntry<Integer> Int(String id, String key, int def) {
        return new SettingsEntry<>(id, key, def, Integer.class);
    }

    public static SettingsEntry<String> String(String id, String key, String def) {
        return new SettingsEntry<>(id, key, def, String.class);
    }

    public static SettingsEntry<Boolean> Boolean(String id, String key, Boolean def) {
        return new SettingsEntry<>(id, key, def, Boolean.class);
    }
}
