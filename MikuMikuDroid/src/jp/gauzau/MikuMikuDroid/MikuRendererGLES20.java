package jp.gauzau.MikuMikuDroid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.util.Log;

public class MikuRendererGLES20 extends MikuRendererBase {

	private String TAG = "MikuRendarGLES20";
	
	class GLSL {
		public int mProgram;

		//// Vertex shader
		// Attributes
		public int maPositionHandle;
		public int maNormalHandle;
		public int maBlendHandle;

		// Uniforms
		public int muPMatrix;
		public int muPow;
		public int muMBone;
		public int muLightDir;

		//// Fragment shader
		// texture samplers
		public int msTextureSampler;
		public int msToonSampler;
		public int msSphereSampler;

		// Uniforms
		public int muSpaEn;
		public int muSphEn;
		public int muDif;
		public int muSpec;
		
		public GLSL(String v, String f) {
			mProgram = createProgram(v, f);
			if (mProgram == 0) {
				return;
			}

			GLES20.glUseProgram(mProgram);
			checkGlError("glUseProgram");

			// attribute & uniform handles
			maPositionHandle	= GLES20.glGetAttribLocation(mProgram, "aPosition");
			maNormalHandle		= GLES20.glGetAttribLocation(mProgram, "aNormal");
			maBlendHandle		= GLES20.glGetAttribLocation(mProgram, "aBlend");

			muPMatrix			= GLES20.glGetUniformLocation(mProgram, "uPMatrix");
			muPow				= GLES20.glGetUniformLocation(mProgram, "uPow");
			muMBone				= GLES20.glGetUniformLocation(mProgram, "uMBone");
			muLightDir			= GLES20.glGetUniformLocation(mProgram, "uLightDir");
			
			msTextureSampler	= GLES20.glGetUniformLocation(mProgram, "sTex");
			msToonSampler		= GLES20.glGetUniformLocation(mProgram, "sToon");
			msSphereSampler		= GLES20.glGetUniformLocation(mProgram, "sSphere");
			
			muSpaEn				= GLES20.glGetUniformLocation(mProgram, "bSpaEn");
			muSphEn				= GLES20.glGetUniformLocation(mProgram, "bSphEn");
			muDif				= GLES20.glGetUniformLocation(mProgram, "uDif");
			muSpec				= GLES20.glGetUniformLocation(mProgram, "uSpec");
		}
		
		private int loadShader(int shaderType, String source) {
			int shader = GLES20.glCreateShader(shaderType);
			if (shader != 0) {
				GLES20.glShaderSource(shader, source);
				GLES20.glCompileShader(shader);
				int[] compiled = new int[1];
				GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
				if (compiled[0] == 0) {
					Log.e(TAG, "Could not compile shader " + shaderType + ":");
					Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
					Log.e(TAG, "message ends.");
					GLES20.glDeleteShader(shader);
					shader = 0;
				}
			}
			return shader;
		}

		private int createProgram(String vertexSource, String fragmentSource) {
			int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
			if (vertexShader == 0) {
				return 0;
			}

			int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
			if (pixelShader == 0) {
				return 0;
			}

			int program = GLES20.glCreateProgram();
			if (program != 0) {
				GLES20.glAttachShader(program, vertexShader);
				checkGlError("glAttachShader");
				GLES20.glAttachShader(program, pixelShader);
				checkGlError("glAttachShader");
				GLES20.glLinkProgram(program);
				int[] linkStatus = new int[1];
				GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
				if (linkStatus[0] != GLES20.GL_TRUE) {
					Log.e(TAG, "Could not link program: ");
					Log.e(TAG, GLES20.glGetProgramInfoLog(program));
					GLES20.glDeleteProgram(program);
					program = 0;
				}
			}
			return program;
		}
		
		
	};
	
	private GLSL mGLSL;
//	private GLSL mGLSL1S;
	private GLSL mGLSLST;
	private GLSL mGLSLBG;
	
	// for background
	private MikuModel mBG = new MikuModel();
	private FloatBuffer mBgVertex;
	private ShortBuffer mBgIndex;
	private int mBgWidth;
	private int mBgHeight;
	
	public float[]  mBoneMatrix = new float[16 * 256];	// ad-hock number: will be fixed to mBoneNum
	private float[] mLightDir = new float[3];
	private float[] mDifAmb = new float[4];
	private int[] mTexSize = new int[1];
	private boolean mNpot;

	public MikuRendererGLES20(CoreLogic cl) {
		super(cl);
		
		// for background
		mBG.mTexture = new HashMap<String, Integer>();
		ByteBuffer bb = ByteBuffer.allocateDirect(4 * 4 * 4);
		bb.order(ByteOrder.nativeOrder());
		mBgVertex = bb.asFloatBuffer();
		
		mBgVertex.position(0);
		
		mBgVertex.put(-1);
		mBgVertex.put(-1);
		mBgVertex.put(0);
		mBgVertex.put(1);

		mBgVertex.put(-1);
		mBgVertex.put(1);
		mBgVertex.put(0);
		mBgVertex.put(0);

		mBgVertex.put(1);
		mBgVertex.put(1);
		mBgVertex.put(1);
		mBgVertex.put(0);

		mBgVertex.put(1);
		mBgVertex.put(-1);
		mBgVertex.put(1);
		mBgVertex.put(1);

		mBgVertex.position(0);
		
		bb = ByteBuffer.allocateDirect(6 * 2);
		bb.order(ByteOrder.nativeOrder());
		mBgIndex = bb.asShortBuffer();
		
		mBgIndex.put((short) 3);
		mBgIndex.put((short) 1);
		mBgIndex.put((short) 0);

		mBgIndex.put((short) 3);
		mBgIndex.put((short) 2);
		mBgIndex.put((short) 1);
		
		mBgIndex.position(0);

		clear();
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		mLightDir[0] = -0.5f; mLightDir[1] = -1.0f; mLightDir[2] = -0.5f;	// in left-handed region
		Vector.normalize(mLightDir);
		
		GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		initializeAllTexture(false);

		int pos = mCoreLogic.applyCurrentMotion();

		////////////////////////////////////////////////////////////////////
		//// draw model
		GLES20.glUseProgram(mGLSL.mProgram);

		// Projection Matrix
		GLES20.glUniformMatrix4fv(mGLSL.muPMatrix, 1, false, mCoreLogic.getProjectionMatrix(), 0);

		// LightPosition
		GLES20.glUniform3fv(mGLSL.muLightDir, 1, mLightDir, 0);

		GLES20.glUniform1i(mGLSL.msToonSampler, 0);
		GLES20.glUniform1i(mGLSL.msTextureSampler, 1);
		GLES20.glUniform1i(mGLSL.msSphereSampler, 2);
		checkGlError("on onDrawFrame");

		if (mCoreLogic.getMiku() != null) {
			for (Miku miku : mCoreLogic.getMiku()) {
				if(miku.mModel.mIsTextureLoaded) {
					bindBufferGLES20(miku.mModel, mGLSL);
					drawGLES20(miku.mModel, mGLSL);					
				}
			}
		}

		////////////////////////////////////////////////////////////////////
		//// draw stage
		Miku stage = mCoreLogic.getMikuStage();
		if (stage != null && stage.mModel.mIsTextureLoaded) {
			GLES20.glUseProgram(mGLSLST.mProgram);

			// Projection, Model, View Matrix
			GLES20.glUniformMatrix4fv(mGLSLST.muPMatrix, 1, false, mCoreLogic.getProjectionMatrix(), 0);

			// LightPosition
			GLES20.glUniform3fv(mGLSLST.muLightDir, 1, mLightDir, 0);		

			GLES20.glUniform1i(mGLSLST.msToonSampler, 0);
			GLES20.glUniform1i(mGLSLST.msTextureSampler, 1);
//			checkGlError("on onDrawFrame");

			bindBufferGLES20(stage.mModel, mGLSLST);
			drawGLES20(stage.mModel, mGLSLST);
		}
		
		////////////////////////////////////////////////////////////////////
		//// draw BG
		String bg = mCoreLogic.getBG();
		if(bg != null) {
			GLES20.glUseProgram(mGLSLBG.mProgram);
			GLES20.glUniform1i(mGLSLBG.msTextureSampler, 1);

			bindBgBufferGLES20(mGLSLBG);
			drawBgGLES20(bg);
		}

		GLES20.glFlush();
		mCoreLogic.onDraw(pos);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		super.onSurfaceChanged(gl, width, height);
		GLES20.glViewport(0, 0, width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// GL configurations
		int bonenum = 48;
		GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, mTexSize, 0);
		mCoreLogic.setGLConfig(bonenum);
		mNpot = GLES20.glGetString(GLES20.GL_EXTENSIONS).contains("GL_OES_texture_npot");
		
		// initialize
		GLES20.glClearColor(1, 1, 1, 1);

		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glFrontFace(GLES20.GL_CW);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		// GLUtils.texImage2D generates premultiplied-alpha texture. so we use GL_ONE instead of GL_ALPHA
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);

		GLES20.glDepthFunc(GLES20.GL_LEQUAL);
//		GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
//		GLES20.glPolygonOffset(-1.0f, -2.0f);
		
		// sharder programs
		mGLSL   = new GLSL(String.format(mCoreLogic.getRawResourceString(R.raw.vs), bonenum), mCoreLogic.getRawResourceString(R.raw.fs));
//		mGLSL1S = new GLSL(String.format(mCoreLogic.getRawResourceString(R.raw.vs_1m), bonenum), mCoreLogic.getRawResourceString(R.raw.fs_nm));
		mGLSLST = new GLSL(mCoreLogic.getRawResourceString(R.raw.vs_nm), mCoreLogic.getRawResourceString(R.raw.fs_nm));
		mGLSLBG = new GLSL(mCoreLogic.getRawResourceString(R.raw.vs_bg), mCoreLogic.getRawResourceString(R.raw.fs_bg));


		// bind textures
		initializeAllTexture(true);
	}
	
	
	// for GLES20
	private void drawGLES20(MikuModel miku, GLSL glsl) {
		ArrayList<Material> rendar = miku.mAnimation ? miku.mRendarList : miku.mMaterial;
		ArrayList<Bone> bs = miku.mBone;
	
		int max = rendar.size();
		for (int r = 0; r < max; r++) {
			Material mat = rendar.get(r);
			if (miku.mAnimation) {
				if(mat.rename_inv_map == null) {
					for (int j = 0; j < bs.size(); j++) {
						Bone b = bs.get(j);
						if(b != null) {
							System.arraycopy(b.matrix, 0, mBoneMatrix, j * 16, 16);
						}
					}
				} else {
					for (int j = 0; j < miku.mRenameBone; j++) {
						int inv = mat.rename_inv_map[j];
						if (inv >= 0) {
							Bone b = bs.get(inv);
							System.arraycopy(b.matrix, 0, mBoneMatrix, j * 16, 16);
						}
					}
				}
				GLES20.glUniformMatrix4fv(glsl.muMBone, mat.rename_hash_size, false, mBoneMatrix, 0);
	
				GLES20.glEnableVertexAttribArray(glsl.maBlendHandle);
				GLES20.glVertexAttribPointer(glsl.maBlendHandle, 3, GLES20.GL_UNSIGNED_BYTE, false, 0, mat.rename_index);
//				checkGlError("drawGLES20 VertexAttribPointer blend");
			}
	
			// don't cull face that has alpha value 0.99
			if(mat.diffuse_color[3] < 1) {
				GLES20.glDisable(GLES20.GL_CULL_FACE);
			} else {
				GLES20.glEnable(GLES20.GL_CULL_FACE);
			}
	
			// initialize color
			for(int i = 0; i < mDifAmb.length; i++) {
				mDifAmb[i] = 1.0f;
			}
			
			// Toon texture
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, miku.mToon.get(mat.toon_index));

			// texture
			if (mat.texture != null) {
				Integer tb = miku.mTexture.get(mat.texture);
				if(tb != null) {
					GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tb);
				} else {	// avoid crash
					GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, miku.mToon.get(0));	// white texture using toon0.bmp
					for(int i = 0; i < 3; i++) {	// for emulate premultiplied alpha
						mDifAmb[i] *= mat.diffuse_color[3];
					}
				}
			} else {
				GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, miku.mToon.get(0));	// white texture using toon0.bmp
				for(int i = 0; i < 3; i++) {	// for emulate premultiplied alpha
					mDifAmb[i] *= mat.diffuse_color[3];
				}
			}
			
			// sphere map
			if(glsl.muSphEn >= 0) {
				if (mat.sphere != null) {
					GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, miku.mTexture.get(mat.sphere));
					if(mat.sphere.endsWith(".spa")) {
						GLES20.glUniform1i(glsl.muSpaEn, 1);					
						GLES20.glUniform1i(glsl.muSphEn, 0);
					} else {
						GLES20.glUniform1i(glsl.muSpaEn, 0);					
						GLES20.glUniform1i(glsl.muSphEn, 1);					
					}
				} else {
					GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
					GLES20.glUniform1i(glsl.muSpaEn, 0);
					GLES20.glUniform1i(glsl.muSphEn, 0);
				}
			}

//			checkGlError("on DrawGLES20");
			
			// diffusion and ambient
			float wi = 0.6f;	// light color = (0.6, 0.6, 0.6)
			for(int i = 0; i < 3; i++) {
				mDifAmb[i] *= mat.diffuse_color[i] * wi + mat.emmisive_color[i];
			}
			mDifAmb[3] *= mat.diffuse_color[3];
			Vector.min(mDifAmb, 1.0f);
			GLES20.glUniform4fv(glsl.muDif, 1, mDifAmb, 0);
			
			// speculation
			if (glsl.muPow >= 0) {
				GLES20.glUniform4f(glsl.muSpec, mat.specular_color[0], mat.specular_color[1], mat.specular_color[2], 0);
				GLES20.glUniform1f(glsl.muPow, mat.power);
			}
			
			// draw
			if(miku.mIsOneSkinning || !miku.mAnimation) {
				SphereArea.SphereBone[] sba = mat.area.getSphereBone();
				for(int i = 0; i < sba.length; i++) {
					int n = sba[i].makeRenderIndex(mCoreLogic.getProjectionMatrix());
					int[] ri = sba[i].getRenderIndex();
					for(int j = 0; j < n; j++) {
						miku.mIndexBuffer.position(ri[j * 2]);
						GLES20.glDrawElements(GLES20.GL_TRIANGLES, ri[j * 2 + 1], GLES20.GL_UNSIGNED_SHORT, miku.mIndexBuffer);
					}
				}
			} else {
				miku.mIndexBuffer.position(mat.face_vart_offset);
				GLES20.glDrawElements(GLES20.GL_TRIANGLES, mat.face_vert_count, GLES20.GL_UNSIGNED_SHORT, miku.mIndexBuffer);
			}
//			checkGlError("glDrawElements");
		}
		miku.mIndexBuffer.position(0);
	}
	
	private void drawBgGLES20(String bg) {
		GLES20.glDisable(GLES20.GL_CULL_FACE);

		// texture
		Integer tb = mBG.mTexture.get(bg);
		if(tb != null) {
			if(mBG.mIsTextureLoaded) {
				GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tb);
				GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, mBgIndex);
			}
		}

	}

	private void initializeAllTexture(boolean all) {
		for(int i = 1; i < 16; i *= 2) {
			if(tryInitializeAllTexture(all, i)) {
				return ;
			} else {
				deleteAllTexture();
			}
		}
	}

	private boolean deleteAllTexture() {
		// try binding
		if (mCoreLogic.getMiku() != null) {
			for (Miku miku : mCoreLogic.getMiku()) {
				deleteTexture(miku.mModel);
			}
		}

		if (mCoreLogic.getMikuStage() != null) {
			deleteTexture(mCoreLogic.getMikuStage().mModel);
		}
		
		if (mCoreLogic.getBG() != null) {
			deleteTexture(mBG);
		}
		
		return true;
	}
	
	private boolean tryInitializeAllTexture(boolean all, int scale) {
		// try binding
		if (mCoreLogic.getMiku() != null) {
			for (Miku miku : mCoreLogic.getMiku()) {
				if(all || miku.mModel.mIsTextureLoaded == false) {
					if(initializeTextures(miku, scale)) {
						miku.mModel.mIsTextureLoaded = true;
					} else {
						return false;
					}
				}
			}
		}

		if (mCoreLogic.getMikuStage() != null) {
			if(all || mCoreLogic.getMikuStage().mModel.mIsTextureLoaded == false) {
				if(initializeTextures(mCoreLogic.getMikuStage(), scale)) {
					mCoreLogic.getMikuStage().mModel.mIsTextureLoaded = true;
				} else {
					return false;
				}
			}
		}
		
		if (mCoreLogic.getBG() != null) {
			if(all || mBG.mIsTextureLoaded == false) {
				if(readAndBindBgTextureGLES20(scale)) {
					mBG.mIsTextureLoaded = true;
				} else {
					return false;
				}
			}
		}
		
		return true;
	}

	private boolean initializeTextures(Miku miku, int scale) {
		boolean ret = true;
		ret &= readAndBindToonTextureGLES20(miku.mModel);
		ret &= readAndBindTextureGLES20(miku.mModel, scale);
		ret &= readAndBindSphereTextureGLES20(miku.mModel, scale);
		
		return ret;
	}

	private void bindBufferGLES20(MikuModel miku, GLSL glsl) {
		GLES20.glEnableVertexAttribArray(glsl.maPositionHandle);
		miku.mAllBuffer.position(0);
		GLES20.glVertexAttribPointer(glsl.maPositionHandle, 4, GLES20.GL_FLOAT, false, 8 * 4, miku.mAllBuffer);
//		checkGlError("drawGLES20 VertexAttribPointer vertex");

		GLES20.glEnableVertexAttribArray(glsl.maNormalHandle);
		miku.mAllBuffer.position(4);
		GLES20.glVertexAttribPointer(glsl.maNormalHandle, 4, GLES20.GL_FLOAT, false, 8 * 4, miku.mAllBuffer);
//		checkGlError("drawGLES20 VertexAttribPointer normal");
		miku.mAllBuffer.position(0);
	}
	
	private void bindBgBufferGLES20(GLSL glsl) {
		float s, t;
		if(mBgWidth > mCoreLogic.getScreenWidth()) {
			s = (mBgWidth - mCoreLogic.getScreenWidth()) / (float)(2 * mBgWidth);
		} else {
			s = 0;
		}

		if(mBgHeight > mCoreLogic.getScreenHeight()) {
			t = (mBgHeight - mCoreLogic.getScreenHeight()) / (float)(2 * mBgHeight);
		} else {
			t = 0;
		}
		
		// left
		mBgVertex.put(2, s);
		mBgVertex.put(6, s);

		// right
		mBgVertex.put(10, 1 - s);
		mBgVertex.put(14, 1 - s);
		
		// top
		mBgVertex.put(3, 1 - t);
		mBgVertex.put(15, 1 - t);
		
		// bottom
		mBgVertex.put(7, t);
		mBgVertex.put(11, t);
		
		mBgVertex.position(0);

		GLES20.glEnableVertexAttribArray(glsl.maPositionHandle);
		mBgVertex.position(0);
		GLES20.glVertexAttribPointer(glsl.maPositionHandle, 4, GLES20.GL_FLOAT, false, 0, mBgVertex);
//		checkGlError("drawGLES20 VertexAttribPointer bg vertex");
		
		mBgVertex.position(0);
	}
	
	public boolean readAndBindTextureGLES20(MikuModel model, int scale) {
		GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
		
		Log.d("MikuModel", "Loading textures...");
		model.mTexture = new HashMap<String, Integer>();
		for (int i = 0; i < model.mMaterial.size(); i++) {
			Material mat = model.mMaterial.get(i);
			if (mat.texture != null) {
				try {
					readAndBindTexture1(model, mat.texture, scale);
				} catch (OutOfMemoryError e) {
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean readAndBindSphereTextureGLES20(MikuModel model, int scale) {
		GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
		for (int i = 0; i < model.mMaterial.size(); i++) {
			Material mat = model.mMaterial.get(i);
			if(mat.sphere != null) {
				try {
					readAndBindTexture1(model, mat.sphere, scale);					
				} catch (OutOfMemoryError e) {
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean readAndBindBgTextureGLES20(int scale) {
		GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
		
		Log.d("MikuModel", "Loading BG textures...");
		mBG.mTexture = new HashMap<String, Integer>();
		String bg = mCoreLogic.getBG();
		if(bg != null) {
			try {
				// check size
				BitmapFactory.Options op = new BitmapFactory.Options();
				op.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(mCoreLogic.getBG(), op);
				mBgWidth = op.outWidth;
				mBgHeight = op.outHeight;
				
				// load texture
				readAndBindTexture1(mBG, bg, scale * 2);	// lower resolution
			} catch (OutOfMemoryError e) {
				return false;
			}
		}
		return true;
	}
	
	private void readAndBindTexture1(MikuModel model, String texture, int scale) {
		if (model.mTexture.get(texture) == null) {
			// bind
			int tex[] = new int[1];
			GLES20.glGenTextures(1, tex, 0);

			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);				
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			if(mNpot) {
				GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST_MIPMAP_NEAREST);				
			} else {
				GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);				
			}
			TextureFile.loadTexture(model.mBase, texture, scale, mTexSize[0], mNpot);
			if(mNpot) {
				GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
			}

			model.mTexture.put(texture, tex[0]);

			int err = GLES20.glGetError();
			if (err != 0) {
				Log.d("MikuRendererGLES20", GLU.gluErrorString(err));
				Log.d("MikuRendererGLES20", texture);
			}
		}
	}

	
	private boolean readAndBindToonTextureGLES20(MikuModel model) {
		boolean res = true;
		int tex[] = new int[11];
		GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
		GLES20.glGenTextures(11, tex, 0);
	
		model.mToon = new ArrayList<Integer>();
		for (int i = 0; i < 11; i++) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[i]);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			try {
				TextureFile.loadTexture(model.mBase, model.mToonFileName.get(i), 1, mTexSize[0], mNpot);
			} catch (OutOfMemoryError e) {
				res = false;
			}
			model.mToon.add(tex[i]);
		}
		return res;
	}
	
	@Override
	public void deleteTexture(MikuModel model) {
		Log.d("MikuRenderGLES20", "Deleting textures...");
		int[] tex = new int[1];
		if(model != null) {
			if(model.mTexture != null) {
				for(Entry<String, Integer> t: model.mTexture.entrySet()) {
					tex[0] = t.getValue();
					GLES20.glDeleteTextures(1, tex, 0);
					checkGlError("MikuRenderGLES20");
				}			
			}
			if(model.mToon != null) {
				for(Integer t: model.mToon) {
					tex[0] = t;
					GLES20.glDeleteTextures(1, tex, 0);
					checkGlError("MikuRenderGLES20");
				}
			}
			model.mIsTextureLoaded = false;
		}
		Log.d("MikuRenderGLES20", "Done.");
	}
	
	@Override
	public void deleteTexture(String texf) {
		Integer i = mBG.mTexture.get(texf);
		if(i != null) {
			int[] tex = new int[1];
			tex[0] = i;
			GLES20.glDeleteTextures(1, tex, 0);
			mBG.mTexture.remove(texf);
			mBG.mIsTextureLoaded = false;
			checkGlError("MikuRenderGLES20");			
		}
	}
	
	private void checkGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, op + ": glError " + error);
//			throw new RuntimeException(op + ": glError " + error);
		}
	}
}
