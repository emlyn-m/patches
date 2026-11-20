package xyz.emlyn.patchextension.shared.logging;

@SuppressWarnings("unused")
public enum Lvl {
    Error(5, "err"),
    Warning(4, "wrn"),
    Debug(0, "dbg");

    private final int _level;
    private final String _prefix;

    Lvl(int level, String prefix) {
        this._level = level;
        this._prefix = prefix;
    }

    String prefix() {
        return this._prefix;
    }
}
