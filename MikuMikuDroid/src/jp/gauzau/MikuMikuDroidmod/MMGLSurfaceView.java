package jp.gauzau.MikuMikuDroidmod;

import java.util.ArrayList;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;

import com.example.gdc11.MultisampleConfigChooser;

public class MMGLSurfaceView extends GLSurfaceView {

	private MikuRendererBase mMikuRendarer;
	
	private static final boolean kUseMultisampling = true;
    private int mSamples = 2;

    // If |kUseMultisampling| is set, this is what chose the multisampling config.
    private MultisampleConfigChooser mConfigChooser;

	public MMGLSurfaceView(Context context, CoreLogic cl) {
		super(context);
		setRendar(context, cl);
	}

	public void setRendar(Context ctx, CoreLogic cl) {
		if (detectOpenGLES20(ctx)) {
			setEGLContextClientVersion(2);
			boolean usesCoverageAa = false;
			if (kUseMultisampling && 1 < mSamples) {
			    setEGLConfigChooser(mConfigChooser = new MultisampleConfigChooser(mSamples));
			    usesCoverageAa = mConfigChooser.usesCoverageAa();
			}
			mMikuRendarer = new MikuRendererGLES20(cl, usesCoverageAa);
			//mMikuRendarer = new MikuRenderer(cl);
		} else {
			mMikuRendarer = new MikuRenderer(cl);
		}
//		setEGLConfigChooser(5, 6, 5, 0, 24, 0);
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
