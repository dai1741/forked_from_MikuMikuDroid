package jp.gauzau.MikuMikuDroid;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

public class MikuRenderer extends MikuRendererBase {
	private boolean mBufferInitialized;
	private boolean mStageBufferInitialized;

	public MikuRenderer() {
		clear();
	}
	
	public void loadModel(String file) throws IOException {
		super.loadModel(file);
		mBufferInitialized = false;
	}
	
	public void loadStage(String file) throws IOException {
		super.loadStage(file);
		mStageBufferInitialized = false;
	}
	
	public void initializeBuffers(GL10 gl) {
		for(Miku miku: mMiku) {
	        // toon shading
	        gl.glActiveTexture(GL10.GL_TEXTURE0);
	        miku.calcToonTexCoord(0, -10f, -9f);
	        miku.readToonTexture();
	        miku.bindToonTexture(gl);
	        
	        // Texture
	        gl.glActiveTexture(GL10.GL_TEXTURE1);
	        miku.readAndBindTexture(gl);
	        
	        // buffer bindings
	        miku.bindBuffer(gl);			
		}
	}

	public void initializeStageBuffers(GL10 gl) {
        // toon shading
        gl.glActiveTexture(GL10.GL_TEXTURE0);
        mMikuStage.calcToonTexCoord(0, -10f, -9f);
        mMikuStage.readToonTexture();
        mMikuStage.bindToonTexture(gl);
        
        // Texture
        gl.glActiveTexture(GL10.GL_TEXTURE1);
        mMikuStage.readAndBindTexture(gl);
        
        // buffer bindings
        mMikuStage.bindBuffer(gl);			
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		super.onDrawFrame(gl);
        GL11 gl11 = (GL11) gl;
		
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		double frame = nowFrames(32767);
		setCameraByVMDFrame(frame);
		
		if(mMiku != null) {
			if(mBufferInitialized == false) {
				initializeBuffers(gl);
				mBufferInitialized = true;
			}
			for(Miku miku: mMiku) {
				miku.setBonePosByVMDFrame((float) frame);
				miku.setFaceByVMDFrame((float) frame);
//				miku.updateVertexBuffer();
			}

	        gl.glMatrixMode(GL10.GL_PROJECTION);
	        gl.glLoadMatrixf(mPMatrix, 0);

	        gl.glMatrixMode(GL10.GL_MODELVIEW);
	        gl.glLoadIdentity();
	        
	        gl11.glEnableClientState(GL10.GL_VERTEX_ARRAY);
	        gl11.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl11.glEnableClientState(GL11Ext.GL_MATRIX_INDEX_ARRAY_OES);
            gl11.glEnableClientState(GL11Ext.GL_WEIGHT_ARRAY_OES);

//			float white_light[] = {0.6f, 0.6f, 0.6f, 1.0f};
//	        float lmodel_ambient[] = {0.6f, 0.6f, 0.6f, 1.0f};
//	        float light_pos[] = {0f, 5f, -3f};
//	        gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
//	        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, light_pos, 0);
//	        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, white_light, 0);
//	        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, lmodel_ambient, 0);
//			gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);

            gl.glEnable(GL11Ext.GL_MATRIX_PALETTE_OES);
	    	for(Miku miku: mMiku) {
				miku.bindBuffer(gl);
		        miku.draw(gl);
	    	}
		}

		if(mMikuStage != null) {
			if(mStageBufferInitialized == false) {
				initializeStageBuffers(gl);
				mStageBufferInitialized = true;
			}
            gl.glDisable(GL11Ext.GL_MATRIX_PALETTE_OES);
            gl11.glDisableClientState(GL11Ext.GL_MATRIX_INDEX_ARRAY_OES);
            gl11.glDisableClientState(GL11Ext.GL_WEIGHT_ARRAY_OES);
			mMikuStage.bindBuffer(gl);
	        mMikuStage.draw(gl);
		}
        gl.glEnable(GL11Ext.GL_MATRIX_PALETTE_OES);
		
        gl.glFlush();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		super.onSurfaceChanged(gl, width, height);
		
        gl.glViewport(0, 0, width, height);
     }

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);
		
         gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                   GL10.GL_FASTEST);

         gl.glClearColor(1,1,1,1);
         
         gl.glEnable(GL10.GL_NORMALIZE);
         gl.glEnable(GL10.GL_CULL_FACE);
         gl.glFrontFace(GL10.GL_CW);
         gl.glShadeModel(GL10.GL_SMOOTH);
         gl.glEnable(GL10.GL_DEPTH_TEST);
//		gl.glEnable(GL10.GL_LIGHTING);
//		gl.glEnable(GL10.GL_LIGHT0);
         
 		// GLUtils.texImage2D generates premultiplied-alpha texture, but OpenGL ES1.3 on android 2.1 is not support GL_CONSTANT_ALPHA...
         gl.glEnable( GL10.GL_BLEND );
         gl.glBlendFunc( GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA );
         
         gl.glEnable(GL11Ext.GL_MATRIX_PALETTE_OES);
         
         int matnum[] = new int[1];
         gl.glGetIntegerv(GL11Ext.GL_MAX_PALETTE_MATRICES_OES, matnum, 0);
         mBoneNum = matnum[0];
         
//		gl.glEnable( GL10.GL_ALPHA_TEST );
//		gl.glAlphaFunc( GL10.GL_GEQUAL, 0.05f );

         if(mMiku != null) {
        	 initializeBuffers(gl);
         }
         if(mMikuStage != null) {
        	 initializeStageBuffers(gl);
         }
	}

	@Override
	public void clear() {
		super.clear();
		mBufferInitialized = false;
		mStageBufferInitialized = false;
	}

}
