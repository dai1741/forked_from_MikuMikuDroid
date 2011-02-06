package jp.gauzau.MikuMikuDroid;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;

public class MMGLSurfaceView extends GLSurfaceView {

	private Renderer mMikuRendarer;

	public MMGLSurfaceView(Context context, CoreLogic cl) {
		super(context);
		setRendar(context, cl);
	}

	public void setRendar(Context ctx, CoreLogic cl) {
		if (detectOpenGLES20(ctx)) {
			setEGLContextClientVersion(2);
			mMikuRendarer = new MikuRendererGLES20(cl);
			// mMikuRendarer = new MikuRenderer();
		} else {
			mMikuRendarer = new MikuRenderer(cl);
		}
		setRenderer(mMikuRendarer);
	}

	private boolean detectOpenGLES20(Context ctx) {
		ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
		ConfigurationInfo info = am.getDeviceConfigurationInfo();
		return (info.reqGlEsVersion >= 0x20000);
	}

}
