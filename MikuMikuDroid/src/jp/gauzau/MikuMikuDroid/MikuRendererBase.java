package jp.gauzau.MikuMikuDroid;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView.Renderer;

public class MikuRendererBase implements Renderer {
	protected CoreLogic	mCoreLogic;

	public MikuRendererBase(CoreLogic cl) {
		mCoreLogic = cl;
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
