package dev.nevertime.client;
/** Pure calculations shared by preferences, overlay and regression tests. */
public final class HudMath {
    private HudMath() {}
    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    public static float fraction(float value) { return Float.isFinite(value) ? Math.max(0f, Math.min(1f, value)) : 0f; }
    public static int position(float fraction, int screen, int widget) {
        return Math.round(fraction(fraction) * Math.max(0, screen - widget));
    }
    public static float normalized(int px, int screen, int widget) {
        return screen <= widget ? 0f : fraction((float)px / (screen - widget));
    }
    public static String duration(long milliseconds) {
        long seconds = Math.max(0, milliseconds / 1000);
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }
}
