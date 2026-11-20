package xyz.emlyn.patchextension.qbf.twitter;

// stubs: implemented by patch
@SuppressWarnings("unused")
public class Compose {
    public static Object padding(Object modifier, float start, float top, float end, float bottom) {
        throw new UnsupportedOperationException("Compose.padding not implemented");
    }

    // RowScope.align, using the grok button's alignment
    public static Object headerAlign() {
        throw new UnsupportedOperationException("Compose.headerAlign not implemented");
    }

    // AndroidView(factory, modifier, update), composable
    public static void androidView(Object factory, Object modifier, Object update, Object composer) {
        throw new UnsupportedOperationException("Compose.androidView not implemented");
    }

    // AnnotatedString.Builder's current text
    public static String builderText(Object builder) {
        throw new UnsupportedOperationException("Compose.builderText not implemented");
    }

    // AnnotatedString.Builder(annotatedString)
    public static Object builderFrom(Object annotatedString) {
        throw new UnsupportedOperationException("Compose.builderFrom not implemented");
    }

    // builder.addStyle(spanStyle, start, end)
    public static void builderAddStyle(Object builder, Object spanStyle, int start, int end) {
        throw new UnsupportedOperationException("Compose.builderAddStyle not implemented");
    }

    // builder.toAnnotatedString()
    public static Object builderBuild(Object builder) {
        throw new UnsupportedOperationException("Compose.builderBuild not implemented");
    }

    // SpanStyle(background = color), color is a packed compose Color (see
    // packColor)
    public static Object spanStyleBackground(long color) {
        throw new UnsupportedOperationException("Compose.spanStyleBackground not implemented");
    }

    // mutableStateOf(value)
    public static Object mutableStateOf(Object value) {
        throw new UnsupportedOperationException("Compose.mutableStateOf not implemented");
    }

    public static Object stateGet(Object state) {
        throw new UnsupportedOperationException("Compose.stateGet not implemented");
    }

    public static void stateSet(Object state, Object value) {
        throw new UnsupportedOperationException("Compose.stateSet not implemented");
    }

    // android argb -> compose Color (srgb)
    public static long packColor(int argb) {
        return ((long) argb) << 32;
    }
}
