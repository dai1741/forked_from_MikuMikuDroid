package jp.gauzau.MikuMikuDroid;

import java.util.ArrayList;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import android.opengl.GLU;
import android.util.Log;

public class MikuRenderer extends MikuRendererBase {
	private float[] mLightDir = new float[3];
	private int[] mTexSize = new int[1];
	private boolean mNpot;

	public MikuRenderer(CoreLogic cl) {
		super(cl);
		mCoreLogic = cl;
		clear();
	}

	public void initializeBuffers(GL10 gl) {
		mLightDir[0] = -0.5f; mLightDir[1] = -1.0f; mLightDir[2] = -0.5f;	// in left-handed region
		Vector.normalize(mLightDir);
		
		for (Miku miku : mCoreLogic.getMiku()) {
			// toon shading
			gl.glActiveTexture(GL10.GL_TEXTURE0);
			miku.mModel.calcToonTexCoord(mLightDir);
			readAndBindToonTexture(gl, miku.mModel);

			// Texture
			gl.glActiveTexture(GL10.GL_TEXTURE1);
			readAndBindTexture(gl, miku.mModel);

			// buffer bindings
			bindBuffer(miku.mModel, gl);
		}
	}
	
	public void initializeBuffers(GL10 gl, Miku miku) {
		mLightDir[0] = -0.5f; mLightDir[1] = -1.0f; mLightDir[2] = -0.5f;	// in left-handed region
		Vector.normalize(mLightDir);
		
		// toon shading
		gl.glActiveTexture(GL10.GL_TEXTURE0);
		miku.mModel.calcToonTexCoord(mLightDir);
		readAndBindToonTexture(gl, miku.mModel);

		// Texture
		gl.glActiveTexture(GL10.GL_TEXTURE1);
		readAndBindTexture(gl, miku.mModel);
	}

	public void initializeStageBuffers(GL10 gl) {
		mLightDir[0] = -0.5f; mLightDir[1] = -1.0f; mLightDir[2] = -0.5f;	// in left-handed region
		Vector.normalize(mLightDir);
		
		// toon shading
		gl.glActiveTexture(GL10.GL_TEXTURE0);
		mCoreLogic.getMikuStage().mModel.calcToonTexCoord(mLightDir);
		readAndBindToonTexture(gl, mCoreLogic.getMikuStage().mModel);

		// Texture
		gl.glActiveTexture(GL10.GL_TEXTURE1);
		readAndBindTexture(gl, mCoreLogic.getMikuStage().mModel);

		// buffer bindings
		bindBuffer(mCoreLogic.getMikuStage().mModel, gl);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		super.onDrawFrame(gl);
		GL11 gl11 = (GL11) gl;

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		int pos = mCoreLogic.applyCurrentMotion();

		if (mCoreLogic.getMiku() != null) {
			for (Miku miku : mCoreLogic.getMiku()) {
				if(miku.mModel.mIsTextureLoaded == false) {
					initializeBuffers(gl, miku);
					miku.mModel.mIsTextureLoaded = true;
				}
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
			if (mCoreLogic.getMikuStage().mModel.mIsTextureLoaded == false) {
				initializeStageBuffers(gl);
				mCoreLogic.getMikuStage().mModel.mIsTextureLoaded = true;
			}
			gl.glDisable(GL11Ext.GL_MATRIX_PALETTE_OES);
			gl11.glDisableClientState(GL11Ext.GL_MATRIX_INDEX_ARRAY_OES);
			gl11.glDisableClientState(GL11Ext.GL_WEIGHT_ARRAY_OES);
			bindBuffer(mCoreLogic.getMikuStage().mModel, gl);
			draw(gl, mCoreLogic.getMikuStage().mModel);
		}
		gl.glEnable(GL11Ext.GL_MATRIX_PALETTE_OES);

		gl.glFlush();
		mCoreLogic.onDraw(pos);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		super.onSurfaceChanged(gl, width, height);

		gl.glViewport(0, 0, width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);

		mNpot = gl.glGetString(GL10.GL_EXTENSIONS).contains("GL_ARB_texture_non_power_of_two");
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

		gl.glClearColor(1, 1, 1, 1);

		gl.glEnable(GL10.GL_NORMALIZE);
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glFrontFace(GL10.GL_CW);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);

		// gl.glEnable(GL10.GL_LIGHTING);
		// gl.glEnable(GL10.GL_LIGHT0);

		// GLUtils.texImage2D generates premultiplied-alpha texture. so we use GL_ONE instead of GL_ALPHA
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
//		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		gl.glEnable(GL11Ext.GL_MATRIX_PALETTE_OES);

		gl.glGetIntegerv(GL11.GL_MAX_TEXTURE_SIZE, mTexSize, 0);

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
	}
	
	private void draw(GL10 gl, MikuModel miku) {
		GL11Ext gl11Ext = (GL11Ext) gl;
		ArrayList<Bone> bs = miku.mBone;
	
		if (miku.mAnimation) {
			gl.glMatrixMode(GL11Ext.GL_MATRIX_PALETTE_OES);
		}
	
		ArrayList<Material> rendar = miku.mAnimation ? miku.mRendarList : miku.mMaterial;
		for (Material mat : rendar) {
			if (miku.mAnimation) {
				for (int j = 0; j < miku.mRenameBone; j++) {
					int inv = mat.rename_inv_map[j];
					if (inv >= 0) {
						Bone b = bs.get(inv);
						gl11Ext.glCurrentPaletteMatrixOES(j);
						gl11Ext.glLoadPaletteFromModelViewMatrixOES();
						gl.glMultMatrixf(b.matrix, 0);
					}
				}

				// gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
				gl11Ext.glMatrixIndexPointerOES(2, GL10.GL_UNSIGNED_BYTE, 3, mat.rename_index);
			}
			
			// don't cull face that has alpha value 0.99
			if(mat.diffuse_color[3] < 1) {
				gl.glDisable(GL10.GL_CULL_FACE);
			} else {
				gl.glEnable(GL10.GL_CULL_FACE);
			}

	
			// Toon texture
			gl.glActiveTexture(GL10.GL_TEXTURE0);
			gl.glEnable(GL10.GL_TEXTURE_2D);
			gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, miku.mToon.get(mat.toon_index));
			float wi = 0.6f;
	
			if (mat.texture != null) {
				gl.glActiveTexture(GL10.GL_TEXTURE1);
				gl.glEnable(GL10.GL_TEXTURE_2D);
				gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
				gl.glBindTexture(GL10.GL_TEXTURE_2D, miku.mTexture.get(mat.texture));
				gl.glColor4f(mat.diffuse_color[0] * wi + mat.emmisive_color[0], mat.diffuse_color[1] * wi + mat.emmisive_color[1], mat.diffuse_color[2] * wi + mat.emmisive_color[2], mat.diffuse_color[3]);
			} else {
				gl.glActiveTexture(GL10.GL_TEXTURE1);
				gl.glDisable(GL10.GL_TEXTURE_2D);
				gl.glColor4f((mat.diffuse_color[0] * wi + mat.emmisive_color[0]) * mat.diffuse_color[3],
						     (mat.diffuse_color[1] * wi + mat.emmisive_color[1]) * mat.diffuse_color[3],
						     (mat.diffuse_color[2] * wi + mat.emmisive_color[2]) * mat.diffuse_color[3], mat.diffuse_color[3]);
			}
	
			// gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, mat.face_color, 0);
			// gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, mat.specular_color, 0);
			// gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, mat.emmisive_color, 0);
			// gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, mat.power);
	
			miku.mIndexBuffer.position(mat.face_vart_offset);
			gl.glDrawElements(GL10.GL_TRIANGLES, mat.face_vert_count, GL10.GL_UNSIGNED_SHORT, miku.mIndexBuffer);
		}
		miku.mIndexBuffer.position(0);
	}


	private void bindBuffer(MikuModel miku, GL10 gl) {
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
	
	public void readAndBindTexture(GL10 gl, MikuModel model) {
		gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 1);
	
		model.mTexture = new HashMap<String, Integer>();
		for (int i = 0; i < model.mMaterial.size(); i++) {
			Material mat = model.mMaterial.get(i);
			if (mat.texture != null) {
				if (model.mTexture.get(mat.texture) == null) {
					int tex[] = new int[1];
					gl.glGenTextures(1, tex, 0);
					gl.glBindTexture(GL10.GL_TEXTURE_2D, tex[0]);
					TextureFile.loadTexture(model.mBase, mat.texture, 2, mTexSize[0], mNpot);
	
					int err = gl.glGetError();
					if (err != 0) {
						Log.d("MikuModel", GLU.gluErrorString(err));
					}
					model.mTexture.put(mat.texture, tex[0]);
				}
			}
		}
	}

	private void readAndBindToonTexture(GL10 gl, MikuModel model) {
		int tex[] = new int[11];
		gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 1);
		gl.glGenTextures(11, tex, 0);

		model.mToon = new ArrayList<Integer>();
		for (int i = 0; i < 11; i++) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, tex[i]);
			TextureFile.loadTexture(model.mBase, model.mToonFileName.get(i), 1, mTexSize[0], mNpot);
			model.mToon.add(tex[i]);
		}
	}

}
