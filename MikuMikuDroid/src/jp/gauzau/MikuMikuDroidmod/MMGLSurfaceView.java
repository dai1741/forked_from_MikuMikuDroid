package jp.gauzau.MikuMikuDroidmod;

import java.util.ArrayList;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;

import com.example.gdc11.MultisampleConfigChooser;

public class MMGLSurfaceView extends GLSurfaceView {

	private MikuRendererBase mMikuRendarer;

    private MultisampleConfigChooser mConfigChooser;

	public MMGLSurfaceView(Context context, CoreLogic cl) {
		this(context, cl, SettingsHelper.BG_WHITE);
	}

    public MMGLSurfaceView(Context context, CoreLogic cl, int bgType) {
        super(context);
        setRendar(context, cl, bgType);
    }

	public void setRendar(Context ctx, CoreLogic cl, int bgType) {
        int samples = SettingsHelper.getSamples(ctx);
        boolean hasAlpha = SettingsHelper.bgUsesGlAlpha(bgType);
	    if(hasAlpha) getHolder().setFormat(PixelFormat.TRANSLUCENT);
	    
		if (detectOpenGLES20(ctx)) {
			setEGLContextClientVersion(2);
		    setEGLConfigChooser(mConfigChooser = new MultisampleConfigChooser(samples, hasAlpha));
			mMikuRendarer = new MikuRendererGLES20(cl, bgType, mConfigChooser.usesCoverageAa());
		} else {
		    if(hasAlpha) setEGLConfigChooser(8, 8, 8, 8, 24, 0);
		    else setEGLConfigChooser(5, 6, 5, 0, 24, 0);
			mMikuRendarer = new MikuRenderer(cl);
		}
		setRenderer(mMikuRendarer);			
	}

	private boolean detectOpenGLES20(Context ctx) {
		ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
		ConfigurationInfo info = am.getDeviceConfigurationInfo();
		return (info.reqGlEsVersion >= 0x20000);
	}

	public void deleteTextures(final ArrayList<MikuModel> mm) {
		queueEvent(new Runnable() {
			@Override
			public void run() {
				for(MikuModel m: mm) {
					mMikuRendarer.deleteTexture(m);
				}
			}
		});
	}

	public void deleteTexture(final String tex) {
		queueEvent(new Runnable() {
			@Override
			public void run() {
				mMikuRendarer.deleteTexture(tex);
			}
		});
	}

}
