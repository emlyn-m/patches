package xyz.emlyn.patchextension.shared;

public class Result<T, E> {
    public boolean ok;
    private T val;
    private E err;

    private Result(boolean ok, T t, E e) {
        this.ok = ok;
        this.val = t;
        this.err = e;
    }

    public T unwrap() {
        if (this.ok) {
            return this.val;
        }
        throw new IllegalStateException(
                "Attempt to unwrap a non-ok result. (Result had: " +
                        String.valueOf(this.err) +
                        ").");
    }

    public E err() {
        return this.ok ? null : this.err;
    }

    public T unwrapOr(T def) {
        return this.ok ? this.val : def;
    }

    public static <T, E> Result<T, E> Ok(T t) {
        return new Result<T, E>(true, t, null);
    }

    public static <T, E> Result<T, E> Err(E e) {
        return new Result<T, E>(false, null, e);
    }
}
