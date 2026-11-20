package xyz.emlyn.patchextension.qbf.api;

import java.util.LinkedHashMap;
import java.util.Map;

public class Lru<K, V> extends LinkedHashMap<K, V> {
    public interface Evicted<V> {
        void accept(V value);
    }

    private final int max;
    private final Evicted<V> onEvict;

    public Lru(int max, Evicted<V> onEvict) {
        super(16, 0.75f, true);
        this.max = max;
        this.onEvict = onEvict;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        if (size() <= max) {
            return false;
        }
        if (onEvict != null) {
            onEvict.accept(eldest.getValue());
        }

        return true;
    }
}
