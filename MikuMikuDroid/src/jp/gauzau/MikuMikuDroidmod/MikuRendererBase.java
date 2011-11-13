package jp.gauzau.MikuMikuDroidmod;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView.Renderer;

public class MikuRendererBase implements Renderer {
	protected final CoreLogic	mCoreLogic;
	protected final int mBgType;

    public MikuRendererBase(CoreLogic cl) {
        this(cl, SettingsHelper.BG_WHITE);
    }

	public MikuRendererBase(CoreLogic cl, int bgType) {
		mCoreLogic = cl;
		mBgType = bgType;
		clear();
	}


	public void clear() {

		mCoreLogic.setDefaultCamera();
	}
	
	public void deleteTexture(MikuModel m) {
		
	}
	
	public void deleteTexture(String texf) {
		
	}


	// ///////////////////////////////////////////////////////////
	// Rendar Interfaces
	@Override
	public void onDrawFrame(GL10 gl) {
		
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		mCoreLogic.setScreenSize(width, height);
		mCoreLogic.setDefaultCamera();
	}


	// ///////////////////////////////////////////////////////////
	// Some common methods

}
