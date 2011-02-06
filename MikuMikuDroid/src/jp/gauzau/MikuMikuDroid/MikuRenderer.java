package jp.gauzau.MikuMikuDroid;

import java.util.ArrayList;
import java.util.Map.Entry;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import android.opengl.GLUtils;

public class MikuRenderer extends MikuRendererBase {
	private boolean mBufferInitialized;
	private boolean mStageBufferInitialized;

	public MikuRenderer(CoreLogic cl) {
		super(cl);
		mCoreLogic = cl;
		clear();
		mBufferInitialized = false;
		mStageBufferInitialized = false;
	}

	public void initializeBuffers(GL10 gl) {
		for (Miku miku : mCoreLogic.getMiku()) {
			// toon shading
			gl.glActiveTexture(GL10.GL_TEXTURE0);
			miku.mModel.calcToonTexCoord(0, -10f, -9f);
			miku.mModel.readToonTexture();
			bindToonTexture(miku.mModel, gl);

			// Texture
			gl.glActiveTexture(GL10.GL_TEXTURE1);
			miku.mModel.readAndBindTexture(gl);

			// buffer bindings
			bindBuffer(miku.mModel, gl);
		}
	}

	public void initializeStageBuffers(GL10 gl) {
		// toon shading
		gl.glActiveTexture(GL10.GL_TEXTURE0);
		mCoreLogic.getMikuStage().mModel.calcToonTexCoord(0, -10f, -9f);
		mCoreLogic.getMikuStage().mModel.readToonTexture();
		bindToonTexture(mCoreLogic.getMikuStage().mModel, gl);

		// Texture
		gl.glActiveTexture(GL10.GL_TEXTURE1);
		mCoreLogic.getMikuStage().mModel.readAndBindTexture(gl);

		// buffer bindings
		bindBuffer(mCoreLogic.getMikuStage().mModel, gl);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		super.onDrawFrame(gl);
		GL11 gl11 = (GL11) gl;

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		mCoreLogic.applyCurrentMotion();

		if (mCoreLogic.getMiku() != null) {
			if (mBufferInitialized == false) {
				initializeBuffers(gl);
				mBufferInitialized = true;
			}

			gl.glMatrixMode(GL10.GL_PROJECTION);
			gl.glLoadMatrixf(mCoreLogic.getProjectionMatrix(), 0);

			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glLoadIdentity();

			gl11.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl11.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl11.glEnableClientState(GL11Ext.GL_MATRIX_INDEX_ARRAY_OES);
			gl11.glEnableClientState(GL11Ext.GL_WEIGHT_ARRAY_OES);

			// float white_light[] = {0.6f, 0.6f, 0.6f, 1.0f};
			// float lmodel_ambient[] = {0.6f, 0.6f, 0.6f, 1.0f};
			// float light_pos[] = {0f, 5f, -3f};
			// gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
			// gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, light_pos, 0);
			// gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, white_light, 0);
			// gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, lmodel_ambient, 0);
			// gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);

			gl.glEnable(GL11Ext.GL_MATRIX_PALETTE_OES);
			for (Miku miku : mCoreLogic.getMiku()) {
				bindBuffer(miku.mModel, gl);
				draw(gl, miku.mModel);
			}
		}

		if (mCoreLogic.getMikuStage() != null) {
			if (mStageBufferInitialized == false) {
				initializeStageBuffers(gl);
				mStageBufferInitialized = true;
			}
			gl.glDisable(GL11Ext.GL_MATRIX_PALETTE_OES);
			gl11.glDisableClientState(GL11Ext.GL_MATRIX_INDEX_ARRAY_OES);
			gl11.glDisableClientState(GL11Ext.GL_WEIGHT_ARRAY_OES);
			bindBuffer(mCoreLogic.getMikuStage().mModel, gl);
			draw(gl, mCoreLogic.getMikuStage().mModel);
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

		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

		gl.glClearColor(1, 1, 1, 1);

		gl.glEnable(GL10.GL_NORMALIZE);
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glFrontFace(GL10.GL_CW);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		// gl.glEnable(GL10.GL_LIGHTING);
		// gl.glEnable(GL10.GL_LIGHT0);

		// GLUtils.texImage2D generates premultiplied-alpha texture, but OpenGL ES1.3 on android 2.1 is not support GL_CONSTANT_ALPHA...
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		gl.glEnable(GL11Ext.GL_MATRIX_PALETTE_OES);

		int matnum[] = new int[1];
		gl.glGetIntegerv(GL11Ext.GL_MAX_PALETTE_MATRICES_OES, matnum, 0);
		mCoreLogic.setGLConfig(matnum[0]);

		// gl.glEnable( GL10.GL_ALPHA_TEST );
		// gl.glAlphaFunc( GL10.GL_GEQUAL, 0.05f );

		if (mCoreLogic.getMiku() != null) {
			initializeBuffers(gl);
		}
		if (mCoreLogic.getMikuStage() != null) {
			initializeStageBuffers(gl);
		}
	}

	@Override
	public void clear() {
		super.clear();
		mBufferInitialized = false;
		mStageBufferInitialized = false;
	}
	
	public void draw(GL10 gl, MikuModel miku) {
		GL11Ext gl11Ext = (GL11Ext) gl;
	
		if (miku.mAnimation) {
			gl.glMatrixMode(GL11Ext.GL_MATRIX_PALETTE_OES);
		}
	
		ArrayList<Material> rendar = miku.mAnimation ? miku.mRendarList : miku.mMaterial;
		for (Material mat : rendar) {
			if (miku.mAnimation) {
				for (Entry<Integer, Integer> ren : mat.rename_hash.entrySet()) {
					if (ren.getValue() < miku.mRenameBone) {
						gl11Ext.glCurrentPaletteMatrixOES(ren.getValue());
						gl11Ext.glLoadPaletteFromModelViewMatrixOES();
						gl.glMultMatrixf(miku.mBone.get(ren.getKey()).matrix, 0);
					}
				}
				// gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
				gl11Ext.glMatrixIndexPointerOES(2, GL10.GL_UNSIGNED_BYTE, 3, mat.rename_index);
			}
	
			// Toon texture
			gl.glActiveTexture(GL10.GL_TEXTURE0);
			gl.glEnable(GL10.GL_TEXTURE_2D);
			gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, miku.mToon.get(mat.toon_index).tex);
	
			if (mat.texture != null) {
				gl.glActiveTexture(GL10.GL_TEXTURE1);
				gl.glEnable(GL10.GL_TEXTURE_2D);
				gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
				gl.glBindTexture(GL10.GL_TEXTURE_2D, miku.mTexture.get(mat.texture).tex);
			} else {
				gl.glActiveTexture(GL10.GL_TEXTURE1);
				gl.glDisable(GL10.GL_TEXTURE_2D);
			}
	
			// gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, mat.face_color, 0);
			// gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, mat.specular_color, 0);
			// gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, mat.emmisive_color, 0);
			// gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, mat.power);
	
			gl.glColor4f(mat.diffuse_color[0], mat.diffuse_color[1], mat.diffuse_color[2], mat.diffuse_color[3]);
			miku.mIndexBuffer.position(mat.face_vart_offset);
			gl.glDrawElements(GL10.GL_TRIANGLES, mat.face_vert_count, GL10.GL_UNSIGNED_SHORT, miku.mIndexBuffer);
		}
		miku.mIndexBuffer.position(0);
	}


	public void bindBuffer(MikuModel miku, GL10 gl) {
		GL11Ext gl11Ext = (GL11Ext) gl;
	
		miku.mAllBuffer.position(0);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 8 * 4, miku.mAllBuffer);
	
		miku.mAllBuffer.position(3);
		gl.glNormalPointer(GL10.GL_FLOAT, 8 * 4, miku.mAllBuffer);
	
		gl11Ext.glWeightPointerOES(2, GL10.GL_FLOAT, 0, miku.mWeightBuffer);
	
		gl.glClientActiveTexture(GL10.GL_TEXTURE0);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, miku.mToonCoordBuffer);
	
		miku.mAllBuffer.position(6);
		gl.glClientActiveTexture(GL10.GL_TEXTURE1);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 8 * 4, miku.mAllBuffer);
	
		miku.mAllBuffer.position(0);
	
	}
	

	public void bindToonTexture(MikuModel miku, GL10 gl) {
		int tex[] = new int[11];
		gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 1);
		gl.glGenTextures(11, tex, 0);
	
		for (int i = 0; i < 11; i++) {
			TexBitmap tb = miku.mToon.get(i);
			tb.tex = tex[i];
	
			gl.glBindTexture(GL10.GL_TEXTURE_2D, tb.tex);
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, tb.bmp, 0);
		}
	}

}
