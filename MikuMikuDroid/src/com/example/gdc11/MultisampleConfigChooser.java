/*
 * This class is originally created by the gdc2011-android-opengl project,
 * licensed under Apache License 2.0.
 * See http://code.google.com/p/gdc2011-android-opengl/ for more information.
 */

package com.example.gdc11;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

import android.opengl.GLSurfaceView;
import android.util.Log;

// This class shows how to use multisampling. To use this, call
//   myGLSurfaceView.setEGLConfigChooser(new MultisampleConfigChooser());
// before calling setRenderer(). Multisampling will probably slow down
// your app -- measure performance carefully and decide if the vastly
// improved visual quality is worth the cost.
public class MultisampleConfigChooser implements GLSurfaceView.EGLConfigChooser {
    static private final String kTag = "GDC11";
    
    private final boolean mHasAlpha;
    
    public MultisampleConfigChooser(int samples, boolean hasAlpha) {
        mSamples = samples;
        mHasAlpha = hasAlpha;
    }

    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        return chooseConfig0(egl, display, mHasAlpha);
    }
    
    private EGLConfig chooseConfig0(EGL10 egl, EGLDisplay display, boolean hasAlpha) {
        mValue = new int[1];

        int red, green, blue, alpha;
        if (hasAlpha) red = green = blue = alpha = 8;
        else {
            red = blue = 5;
            green = 6;
            alpha = 0;
        }
        
        // Try to find a normal multisample configuration first.
        int[] configSpec = {
                EGL10.EGL_RED_SIZE, red,
                EGL10.EGL_GREEN_SIZE, green,
                EGL10.EGL_BLUE_SIZE, blue,
                EGL10.EGL_ALPHA_SIZE, alpha,
                EGL10.EGL_DEPTH_SIZE, 16,
                // Requires that setEGLContextClientVersion(2) is called on the view.
                EGL10.EGL_RENDERABLE_TYPE, 4 /* EGL_OPENGL_ES2_BIT */,
                EGL10.EGL_SAMPLE_BUFFERS, 1 /* true */,
                EGL10.EGL_SAMPLES, mSamples,
                EGL10.EGL_NONE
        };

        if (mSamples > 1 && !egl.eglChooseConfig(display, configSpec, null, 0,
                mValue)) {
            throw new IllegalArgumentException("eglChooseConfig failed");
        }
        int numConfigs = mValue[0];

        if (numConfigs <= 0) {
            // No normal multisampling config was found. Try to create a
            // converage multisampling configuration, for the nVidia Tegra2.
            // See the EGL_NV_coverage_sample documentation.

            if(mSamples > 1) {
                final int EGL_COVERAGE_BUFFERS_NV = 0x30E0;
                final int EGL_COVERAGE_SAMPLES_NV = 0x30E1;
    
                configSpec = new int[]{
                        EGL10.EGL_RED_SIZE, red,
                        EGL10.EGL_GREEN_SIZE, green,
                        EGL10.EGL_BLUE_SIZE, blue,
                        EGL10.EGL_ALPHA_SIZE, alpha,
                        EGL10.EGL_DEPTH_SIZE, 16,
                        EGL10.EGL_RENDERABLE_TYPE, 4 /* EGL_OPENGL_ES2_BIT */,
                        EGL_COVERAGE_BUFFERS_NV, 1 /* true */,
                        EGL_COVERAGE_SAMPLES_NV, mSamples,  // always 5 in practice on tegra 2
                        EGL10.EGL_NONE
                };
    
                if (!egl.eglChooseConfig(display, configSpec, null, 0,
                        mValue)) {
                    throw new IllegalArgumentException("2nd eglChooseConfig failed");
                }
                numConfigs = mValue[0];
            }

            if (numConfigs <= 0) {
                // Give up, try without multisampling.
                configSpec = new int[]{
                        EGL10.EGL_RED_SIZE, red,
                        EGL10.EGL_GREEN_SIZE, green,
                        EGL10.EGL_BLUE_SIZE, blue,
                        EGL10.EGL_ALPHA_SIZE, alpha,
                        EGL10.EGL_DEPTH_SIZE, 16,
                        EGL10.EGL_RENDERABLE_TYPE, 4 /* EGL_OPENGL_ES2_BIT */,
                        EGL10.EGL_NONE
                };

                if (!egl.eglChooseConfig(display, configSpec, null, 0,
                        mValue)) {
                    throw new IllegalArgumentException("3rd eglChooseConfig failed");
                }
                numConfigs = mValue[0];
            } else {
                mUsesCoverageAa = true;
            }
        }
        
        if (numConfigs <= 0) {
            if(hasAlpha) return chooseConfig0(egl, display, false);// Try without alpha
            else throw new IllegalArgumentException("No configs match configSpec");
        }

        // Get all matching configurations.
        EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!egl.eglChooseConfig(display, configSpec, configs, numConfigs,
                mValue)) {
            throw new IllegalArgumentException("data eglChooseConfig failed");
        }

        // CAUTION! eglChooseConfigs returns configs with higher bit depth
        // first: Even though we asked for rgb565 configurations, rgb888
        // configurations are considered to be "better" and returned first.
        // You need to explicitly filter the data returned by eglChooseConfig!
        int index = -1;
        for (int i = 0; i < configs.length; ++i) {
            if (findConfigAttrib(egl, display, configs[i], EGL10.EGL_RED_SIZE, 0) == red) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            Log.w(kTag, "Did not find sane config, using first");
            index = 0;
        }
        EGLConfig config = configs.length > 0 ? configs[index] : null;
        return config;
    }

    private int findConfigAttrib(EGL10 egl, EGLDisplay display,
            EGLConfig config, int attribute, int defaultValue) {
        if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
            return mValue[0];
        }
        return defaultValue;
    }

    public boolean usesCoverageAa() {
        return mUsesCoverageAa;
    }

    private int[] mValue;
    private boolean mUsesCoverageAa;
    private final int mSamples;
}