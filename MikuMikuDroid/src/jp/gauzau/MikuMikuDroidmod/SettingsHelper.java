package jp.gauzau.MikuMikuDroidmod;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Helper class for obtaining user setting data.
 */
public class SettingsHelper {
    
    public static final int BG_WHITE = 0;
    public static final int BG_BLACK = 1;
    public static final int BG_TRANSPARENT = 2;
    public static final int BG_CAMERA = 4;
    public static final int BG_USE_GL_ALPHA = BG_TRANSPARENT | BG_CAMERA;
    public static final int BG_USE_WINDOW_ALPHA = BG_TRANSPARENT;
    
    /**
     * Returns the preffered number of samples used for multi sampling.
     * @return number of samples
     */
    public static int getSamples(Context context) {

        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(
                context).getString(
                context.getResources().getString(R.string.pref_key_antialias), "0"));
    }
    
    /**
     * Returns the preffered background type.
     * @return type of background color/image
     */
    public static int getBgType(Context context) {

        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(
                context).getString(
                        context.getResources().getString(R.string.pref_key_background_type), "0"));
    }
    
    /**
     * @return true if the background type requires to use alpha in GL, else false
     */
    public static boolean bgUsesGlAlpha(int bgType) {
        return (bgType & BG_USE_GL_ALPHA) != 0;
    }

    /**
     * @return true if the background type requires a translucent window, else false
     */
    public static boolean bgUsesWindowAlpha(int bgType) {
        return (bgType & BG_USE_WINDOW_ALPHA) != 0;
    }

    public static boolean isStereo3dEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_stereo3d), true);
    }

    public static float getParallaxFactor(Context context) {
        return Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(
                context.getResources().getString(R.string.pref_key_parallax_factor), "1"));
    }
    
}
