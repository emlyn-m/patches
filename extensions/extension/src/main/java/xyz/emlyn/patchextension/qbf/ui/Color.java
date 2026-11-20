package xyz.emlyn.patchextension.qbf.ui;

class Tuple {
    public double x;
    public double y;
    public double z;

    Tuple(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    static Tuple rgbFromInt(int color) {
        return new Tuple(((double) ((color >> 16) & 0xff)) / 255., ((double) ((color >> 8) & 0xff)) / 255.,
                ((double) (color & 0xff)) / 255.);
    }

    int toRgbInt() {
        return ((((int) Math.round(255 * this.x)) & 0xff) << 16)
                | ((((int) Math.round(255 * this.y)) & 0xff) << 8)
                | (((int) Math.round(255 * this.z)) & 0xff);

    }
}

public class Color {
    public static final int NONE = 0x00000000;
    public static final int OVERLAY_ALPHA = 0x50000000;

    public static final int AI_UNSCANNED = 0xffeeeeee;
    public static final int AI_AI = 0xffdb5c5c;
    public static final int AI_ASSISTED = 0xfff2ae5a;
    public static final int AI_HUMAN = 0xff60bd6a;

    // scan failed. vivid + only ever drawn as outline/badge so it can't read as AI_ASSISTED
    public static final int SCAN_ERROR = 0xffff7a00;

    public static final int OVERLAY_HUMAN = 0xff2e395e;
    public static final int OVERLAY_AI = 0xfff0374a;

    // utility methods
    private static double linearize(double x) {
        return (x >= 0.04045) ? Math.pow((x + 0.055) / (1 + 0.055), 2.4) : x / 12.92;
    }

    private static double inv_linearize(double x) {
        return (x >= 0.0031308) ? (1.055 * Math.pow(x, 1.0 / 2.4)) - 0.055 : 12.92 * x;
    }

    private static Tuple rgbToOklch(Tuple c) {
        double rL = linearize(c.x);
        double gL = linearize(c.y);
        double bL = linearize(c.z);

        double l = Math.cbrt(0.4122214708 * rL + 0.5363325363 * gL + 0.0514459929 * bL);
        double m = Math.cbrt(0.2119034982 * rL + 0.6806995451 * gL + 0.1073969566 * bL);
        double s = Math.cbrt(0.0883024619 * rL + 0.2817188376 * gL + 0.6299787005 * bL);

        double L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s;
        double a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s;
        double b = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s;

        return new Tuple(L, Math.pow(a * a + b * b, 0.5), Math.atan2(b, a));
    }

    private static Tuple oklchToRgb(Tuple c) {
        double a = c.y * Math.cos(c.z);
        double b = c.y * Math.sin(c.z);

        double l = Math.pow(c.x + 0.3963377774 * a + 0.2158037573 * b, 3);
        double m = Math.pow(c.x - 0.1055613458 * a - 0.0638541728 * b, 3);
        double s = Math.pow(c.x - 0.0894841775 * a - 1.2914855480 * b, 3);

        return new Tuple(
                inv_linearize(+4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
                inv_linearize(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
                inv_linearize(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s));
    }

    public static int hueInterpolate(int colorA, int colorB, double t) {
        Tuple a = rgbToOklch(Tuple.rgbFromInt(colorA));
        Tuple b = rgbToOklch(Tuple.rgbFromInt(colorB));

        double L = (b.x * t) + (a.x * (1. - t));
        double c = (b.y * t) + (a.y * (1. - t));
        double ht = (2 * Math.PI) - (b.z - a.z);

        double h = (a.z - ht * t);
        Tuple out = oklchToRgb(new Tuple(L, c, h));
        // emlyn submits worst code of all time - asked to leave the colorspace
        out.x = Math.max(0, Math.min(out.x, 1));
        out.y = Math.max(0, Math.min(out.y, 1));
        out.z = Math.max(0, Math.min(out.z, 1));
        return out.toRgbInt();
    }

}
