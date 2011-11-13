package jp.gauzau.MikuMikuDroidmod;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
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

	public /* abstract */ Bitmap getCurrentFrameBitmap() {
	    //TODO
        return Bitmap.createBitmap(50, 50, Bitmap.Config.RGB_565);
    }
    
    // from: http://www.anddev.org/how_to_get_opengl_screenshot__useful_programing_hint-t829.html
    public static Bitmap convertIntArrayToBitmap(int[] b, int w, int h) {
        
        int bt[]=new int[b.length];
        
        for(int i=0; i<h; i++)
        {//remember, that OpenGL bitmap is incompatible with Android bitmap
         //and so, some correction need.        
             for(int j=0; j<w; j++)
             {
                  int pix=b[i*w+j];
                  int pb=(pix>>16)&0xff;
                  int pr=(pix<<16)&0x00ff0000;
                  int pix1=(pix&0xff00ff00) | pr | pb;
                  bt[(h-i-1)*w+j]=pix1;
             }
        }                  
        Bitmap sb=Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
        return sb;
    }
}
