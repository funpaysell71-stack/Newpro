package dev.nevertime.client;

import android.content.Context;

public final class ThemePalette {
    public static final String PREFS = "visual_theme";
    private static final int[] COLORS = {0xFFF55538,0xFFF545AC,0xFF16AEEA,0xFFAE36EF,0xFF13B45A,0xFFF99019,0xFF70B62E,0xFF00CDB1};
    private ThemePalette() {}
    public static int selected(Context context) {
        int index = context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt("index",4);
        return index >= 0 && index < COLORS.length ? index : 4;
    }
    public static int accent(Context context) { return COLORS[selected(context)]; }
    public static void save(Context context,int index) {
        if (index >= 0 && index < COLORS.length) context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putInt("index",index).apply();
    }
}
