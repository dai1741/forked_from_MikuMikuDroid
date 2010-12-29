package jp.gauzau.MikuMikuDroid;

import java.io.IOException;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class MMGLSurfaceView extends GLSurfaceView {

	private MikuRendererInterface mMikuRendarer;

	public MMGLSurfaceView(Context context) {
		super(context);
		setRendar(context);
	}

	public MMGLSurfaceView(Context context, AttributeSet attr) {
		super(context, attr);
		setRendar(context);
	}

	public void setRendar(Context ctx) {
		if (detectOpenGLES20(ctx)) {
			setEGLContextClientVersion(2);
			mMikuRendarer = new MikuRendererGLES20();
			// mMikuRendarer = new MikuRenderer();
		} else {
			mMikuRendarer = new MikuRenderer();
		}
		setRenderer(mMikuRendarer);
	}

	public void loadModel(final String model) {
		queueEvent(new Runnable() {
			public void run() {
				try {
					mMikuRendarer.loadModel(model);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	public void loadMotion(final String motion) {
		queueEvent(new Runnable() {
			public void run() {
				try {
					mMikuRendarer.loadMotion(motion);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	public double getFPS() {
		return mMikuRendarer.getFPS();
	}

	public void setMedia(MediaPlayer media) {
		mMikuRendarer.setMedia(media);
	}

	private boolean detectOpenGLES20(Context ctx) {
		ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
		ConfigurationInfo info = am.getDeviceConfigurationInfo();
		return (info.reqGlEsVersion >= 0x20000);
	}

	public void loadStage(final String stage) {
		queueEvent(new Runnable() {
			public void run() {
				try {
					mMikuRendarer.loadStage(stage);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	public void loadCamera(final String camera) {
		queueEvent(new Runnable() {
			public void run() {
				try {
					mMikuRendarer.loadCamera(camera);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	public void clear() {
		queueEvent(new Runnable() {
			public void run() {
				mMikuRendarer.clear();
			}
		});
	}

	public void setScreenAngle(final int angle) {
		queueEvent(new Runnable() {
			public void run() {
				mMikuRendarer.setScreenAngle(angle);
			}
		});
	}
}
