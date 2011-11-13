package jp.gauzau.MikuMikuDroidmod;

import android.content.Context;
import android.preference.PreferenceManager;

public class SettingsHelper {
    
    public static final int BG_WHITE = 0;
    public static final int BG_BLACK = 1;
    public static final int BG_TRANSPARENT = 2;
    public static final int BG_CAMERA = 4;
    public static final int BG_USE_GL_ALPHA = BG_TRANSPARENT | BG_CAMERA;
    public static final int BG_USE_WINDOW_ALPHA = BG_TRANSPARENT;
    
    public static int getSamples(Context context) {

        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(
                context).getString(
                context.getResources().getString(R.string.pref_key_antialias), "0"));
    }
    
    public static int getBgType(Context context) {

        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(
                context).getString(
                        context.getResources().getString(R.string.pref_key_background_type), "0"));
    }
    
    public static boolean bgUsesGlAlpha(int bgType) {
        return (bgType & BG_USE_GL_ALPHA) != 0;
    }
    
    public static boolean bgUsesWindowAlpha(int bgType) {
        return (bgType & BG_USE_WINDOW_ALPHA) != 0;
    }
}
