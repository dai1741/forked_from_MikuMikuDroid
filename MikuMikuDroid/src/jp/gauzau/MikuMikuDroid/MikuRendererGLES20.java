package jp.gauzau.MikuMikuDroid;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.util.Log;

public class MikuRendererGLES20 extends MikuRendererBase {

	private String TAG = "MikuRendarGLES20";

	private final String mVertexShader =
		"precision mediump float;\n" + 
		"attribute vec4 aPosition;\n" + 
		"attribute vec4 aNormal;\n" + 
		"attribute vec3 aBlend;\n" +
		"uniform vec4 vLightPos;\n" +
		"uniform mat4 uPMatrix;\n" +
		"uniform mat4 uMBone[%d];\n" +
		"uniform float uPow;\n" +
		"varying vec4 vTexCoord;\n" +
		"void main() {\n" +
		"  float v;\n" +
		"  float spec;\n" +
		"  vec4 b1;\n" +
		"  vec4 b2;\n" +
		"  vec4 b;\n" +
		"  vec3 n1;\n" +
		"  vec3 n2;\n" +
		"  vec3 n;\n" +
		"  vec4 pos;\n" +
		"  mat4 m1;\n" +
		"  mat4 m2;\n" +

		"  pos = vec4(aPosition.x, aPosition.y, aPosition.z, 1.0);\n" +
		"  m1  = uMBone[int(aBlend.x)];\n" +
		"  m2  = uMBone[int(aBlend.y)];\n" +
		"  b1  = m1 * pos;\n" +
		"  b2  = m2 * pos;\n" +
		"  b   = mix(b2, b1, aBlend.z * 0.01);\n" +
		"  gl_Position = uPMatrix * b;\n" +

		"  n = mat3(m1[0].xyz, m1[1].xyz, m1[2].xyz) * vec3(aPosition.w, aNormal.xy);\n" +
		"  v = dot(n, normalize(b.xyz - vLightPos.xyz)); \n" +
		// "  v = dot(normalize(n), normalize(b.xyz - vLightPos.xyz));\n" +
		"  spec = min(1.0, pow(max(-v, 0.0), uPow));\n" +
		// "  spec = max(-v, 0.0) / uPow;\n" +
		"  v = v * 0.5 + 0.5;\n" +
		"  vTexCoord   = vec4(aNormal.zw, v, spec);\n" +
		"}\n";

	private final String mVertexShaderNoMotion =
		"precision mediump float;\n" +
		"attribute vec4 aPosition;\n" +
		"attribute vec4 aNormal;\n" +
		"uniform vec4 vLightPos;\n" +
		"uniform mat4 uPMatrix;\n" +
		"varying vec3 vTexCoord;\n" +
		"void main() {\n" +
		"  float v;\n" +
		"  vec3 n;\n" +
		"  vec4 pos;\n" +

		"  pos = vec4(aPosition.x, aPosition.y, aPosition.z, 1.0);\n" +
		"  gl_Position = uPMatrix * pos;\n" +

		"  n = vec3(aPosition.w, aNormal.xy);\n" + "  v = dot(n, normalize(pos.xyz - vLightPos.xyz)); \n" +
		// "  v = dot(normalize(n), normalize(pos.xyz - vLightPos.xyz)); \n" +
		"  v = v * 0.5 + 0.5;\n" +
		"  vTexCoord   = vec3(aNormal.zw, v);\n" +
		"}\n";

	private final String mFragmentShader =
		"precision mediump float;\n" +
		"varying vec4 vTexCoord;\n" +
		"uniform sampler2D sToon;\n" +
		"uniform sampler2D sTex;\n"	+
		"uniform bool bTexEn;\n" +
		"uniform vec4 vColor;\n" +
		"uniform vec4 vSpec;\n" +
		"uniform vec4 vAmb;\n" +
		"void main() {\n" +
		"  vec4 toon;\n" +
		"  vec4 tex;\n" +
		"  vec4 spec;\n" +
		"  vec4 tmp;\n" +
		"  toon = texture2D(sToon, vec2(0.5, vTexCoord.z));\n" +
		"  if(bTexEn) {" +
		"    tex  = texture2D(sTex,  vTexCoord.xy);\n" +
		"  } else {\n" +
		"    tex  = vec4(1, 1, 1, 1);\n" +
		"  }\n" +
		"  spec = vSpec  * vTexCoord.w;\n" +
		"  tmp  = vColor * toon + vAmb;\n" +
		"  gl_FragColor = tex * tmp + spec;\n" +
		"}\n";

	private final String mFragmentShaderNoMotion =
		"precision mediump float;\n" +
		"varying vec3 vTexCoord;\n" +
		"uniform sampler2D sToon;\n" +
		"uniform sampler2D sTex;\n" +
		"uniform bool bTexEn;\n" +
		"uniform vec4 vColor;\n" +
		"uniform vec4 vAmb;\n" +
		"void main() {\n" +
		"  vec4 toon;\n" +
		"  vec4 tex;\n" +
		"  vec4 tmp;\n" +
		"  toon = texture2D(sToon, vec2(0.5, vTexCoord.z));\n" +
		"  if(bTexEn) {" +
		"    tex  = texture2D(sTex,  vTexCoord.xy);\n" +
		"  } else {\n" +
		"    tex  = vec4(1, 1, 1, 1);\n" +
		"  }\n" +
		"  tmp  = vColor * toon + vAmb;\n" +
		"  gl_FragColor = tex * tmp;\n" +
		"}\n";

	private int mProgram;
	private int maPositionHandle;
	private int maBlendHandle;
	private int muTextureSampler;
	private int msToonSampler;
	private int muTexEn;
	private int muColor;
	private int muSpec;
	private int muAmb;
	private int muPow;
	private int maNormalHandle;
	private int muMBone;

	private int muPMatrix;
	private int muLightPos;

	private int mProgramStage;
	private int maPositionHandleStage;
	private int maNormalHandleStage;
	private int muPMatrixStage;
	private int muTextureSamplerStage;
	private int msToonSamplerStage;
	private int muTexEnStage;
	private int muColorStage;
	private int muAmbStage;
	private int muLightPosStage;

	public MikuRendererGLES20() {
		clear();
	}

	@Override
	public void loadModel(String file) throws IOException {
		super.loadModel(file);
		initializeTextures(mMiku.get(mMiku.size() - 1));
	}

	@Override
	public void loadStage(String file) throws IOException {
		super.loadStage(file);
		initializeStageTextures();
	}

	public void initializeTextures(Miku miku) {
		// toon shading
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		miku.readToonTexture();
		miku.bindToonTextureGLES20();

		// Texture
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		miku.readAndBindTextureGLES20();
	}

	public void initializeStageTextures() {
		// toon shading
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		mMikuStage.readToonTexture();
		mMikuStage.bindToonTextureGLES20();

		// Texture
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		mMikuStage.readAndBindTextureGLES20();
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		double frame;
		frame = nowFrames(32767);

		if (mMiku != null) {
			for (Miku miku : mMiku) {
				miku.setBonePosByVMDFrame((float) frame);
				miku.setFaceByVMDFrame((float) frame);
			}
		}
		setCameraByVMDFrame(frame);

		GLES20.glUseProgram(mProgram);

		// Projection Matrix
		GLES20.glUniformMatrix4fv(muPMatrix, 1, false, mPMatrix, 0);

		// LightPosition
		GLES20.glUniform4f(muLightPos, 0, 0, -35f, 1f);
		// GLES20.glUniform4f(muLightPos, -10f, 10f, 0f, 1f);

		GLES20.glUniform1i(msToonSampler, 0);
		GLES20.glUniform1i(muTextureSampler, 1);
		checkGlError("on onDrawFrame");

		if (mMiku != null) {
			for (Miku miku : mMiku) {
				miku.bindBufferGLES20(maPositionHandle, maNormalHandle);
				miku.drawGLES20(muMBone, maBlendHandle, muTexEn, muColor, muSpec, muPow, muAmb);
			}
		}

		if (mMikuStage != null) {
			GLES20.glUseProgram(mProgramStage);

			// Projection, Model, View Matrix
			GLES20.glUniformMatrix4fv(muPMatrixStage, 1, false, mPMatrix, 0);
			// GLES20.glUniformMatrix4fv(muMVMatrixStage, 1, false, mMVMatrix, 0);

			// LightPosition
			GLES20.glUniform4f(muLightPosStage, 0, 0, -35f, 1f);

			GLES20.glUniform1i(msToonSamplerStage, 0);
			GLES20.glUniform1i(muTextureSamplerStage, 1);
			checkGlError("on onDrawFrame");

			mMikuStage.bindBufferGLES20(maPositionHandleStage, maNormalHandleStage);
			mMikuStage.drawGLES20(0, 0, muTexEnStage, muColorStage, -1, -1, muAmbStage);
		}

		GLES20.glFlush();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		super.onSurfaceChanged(gl, width, height);
		GLES20.glViewport(0, 0, width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GLES20.glClearColor(1, 1, 1, 1);

		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glFrontFace(GLES20.GL_CW);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		// GLUtils.texImage2D generates premultiplied-alpha texture. so we use GL_CONSTANT_ALPHA instead of GL_ALPHA
		GLES20.glEnable(GLES20.GL_BLEND);
		// GLES20.glBlendFunc( GLES20.GL_CONSTANT_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA );
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		// sharder program
		mBoneNum = 48;
		mProgram = createProgram(String.format(mVertexShader, mBoneNum), mFragmentShader);
		if (mProgram == 0) {
			return;
		}

		GLES20.glUseProgram(mProgram);
		checkGlError("glUseProgram");

		// attribute & uniform handles
		maPositionHandle = getAttribLocation(mProgram, "aPosition");
		maNormalHandle = getAttribLocation(mProgram, "aNormal");
		maBlendHandle = getAttribLocation(mProgram, "aBlend");

		muPMatrix = getUniformLocation(mProgram, "uPMatrix");
		muTextureSampler = getUniformLocation(mProgram, "sTex");
		msToonSampler = getUniformLocation(mProgram, "sToon");
		muTexEn = getUniformLocation(mProgram, "bTexEn");
		muColor = getUniformLocation(mProgram, "vColor");
		muSpec = getUniformLocation(mProgram, "vSpec");
		muAmb = getUniformLocation(mProgram, "vAmb");
		muPow = getUniformLocation(mProgram, "uPow");
		muMBone = getUniformLocation(mProgram, "uMBone");
		muLightPos = getUniformLocation(mProgram, "vLightPos");

		if (mMiku != null) {
			for (Miku miku : mMiku) {
				initializeTextures(miku);
			}
		}

		// sharder program in no animation
		mProgramStage = createProgram(mVertexShaderNoMotion, mFragmentShaderNoMotion);
		if (mProgramStage == 0) {
			return;
		}

		GLES20.glUseProgram(mProgramStage);
		checkGlError("glUseProgram mProgramStage");

		// attribute & uniform handles
		maPositionHandleStage = getAttribLocation(mProgramStage, "aPosition");
		maNormalHandleStage = getAttribLocation(mProgramStage, "aNormal");

		muPMatrixStage = getUniformLocation(mProgramStage, "uPMatrix");
		muTextureSamplerStage = getUniformLocation(mProgramStage, "sTex");
		msToonSamplerStage = getUniformLocation(mProgramStage, "sToon");
		muTexEnStage = getUniformLocation(mProgramStage, "bTexEn");
		muColorStage = getUniformLocation(mProgramStage, "vColor");
		// muSpecStage = getUniformLocation(mProgramStage, "vSpec");
		muAmbStage = getUniformLocation(mProgramStage, "vAmb");
		muLightPosStage = getUniformLocation(mProgramStage, "vLightPos");

		if (mMikuStage != null) {
			initializeStageTextures();
		}
	}

	private int getAttribLocation(int program, String string) {
		int ret = GLES20.glGetAttribLocation(program, string);
		checkGlError("glGetAttribLocation " + string);
		if (ret == -1) {
			throw new RuntimeException("Could not get attrib location for " + string);
		}
		return ret;
	}

	private int getUniformLocation(int program, String string) {
		int ret = GLES20.glGetUniformLocation(program, string);
		checkGlError("glGetUniformLocation " + string);
		if (ret == -1) {
			throw new RuntimeException("Could not get attrib location for " + string);
		}
		return ret;
	}

	// for GLES20
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

	private void checkGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, op + ": glError " + error);
			throw new RuntimeException(op + ": glError " + error);
		}
	}
}
