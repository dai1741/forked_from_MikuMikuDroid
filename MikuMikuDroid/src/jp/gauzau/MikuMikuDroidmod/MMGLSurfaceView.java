package jp.gauzau.MikuMikuDroidmod;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;

import com.example.gdc11.MultisampleConfigChooser;

public class MMGLSurfaceView extends GLSurfaceView {

	private MikuRendererBase mMikuRendarer;

    private MultisampleConfigChooser mConfigChooser;
    private boolean mGLES20IsAvailable;

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
	    
	    mGLES20IsAvailable = detectOpenGLES20(ctx);
		if (mGLES20IsAvailable) {
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
	
	public boolean isGLES20Available() {
	    return mGLES20IsAvailable;
	}

    /**
     * Creates a bitmap from the current frame of the view.
     * You must not call this from the ui thread because this may block it for a few seconds.
     *  
     * @return rgba bitmap of the view
     */
    public Bitmap getCurrentFrameBitmap() {
        CoreLogic cl = mMikuRendarer.mCoreLogic;
        final int w = cl.getScreenWidth();
        final int h = cl.getScreenHeight();
        final int[] data = new int[w * h];
        
        final CountDownLatch latch = new CountDownLatch(1);

        queueEvent(new Runnable() {
            public void run() {
                mMikuRendarer.getCurrentFramePixels(data, w, h);
                latch.countDown();
            }
        });
        try {
            latch.await();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return convertIntArrayToBitmap(data, w, h);
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
