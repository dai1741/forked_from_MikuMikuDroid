package jp.gauzau.MikuMikuDroidmod;

import android.content.Context;
import android.preference.PreferenceManager;

public class SettingsHelper {
    
    public static final int BG_WHITE = 0;
    public static final int BG_BLACK = 1;
    public static final int BG_TRANSPARENT = 2;
    public static final int BG_CAMERA = 4;
    public static final int BG_USE_ALPHA = BG_TRANSPARENT | BG_CAMERA;
    
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
    
    public static boolean isBgUsesAlpha(int bgType) {
        return (bgType & BG_USE_ALPHA) != 0;
    }
}
