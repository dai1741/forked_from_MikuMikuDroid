package jp.gauzau.MikuMikuDroidmod;

import android.opengl.GLES20;
import android.opengl.GLU;
import android.util.Log;

public class RenderScenario {
	private static final String TAG = "RenderScenario";
	
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
	}
	
	public class RenderTarget {
		private int FBO;
		private int RBOD;
		private int RBOC;
		private int mWidth;
		private int mHeight;
		
		public RenderTarget() {
			FBO  = 0;
			RBOD = 0;
			RBOC = 0;
		}

		public RenderTarget(int width, int height) {
			mWidth = width;
			mHeight = height;
			create(width, height);
		}
		
		public void switchTargetFrameBuffer(int width, int height) {
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, FBO);
			if(FBO == 0) {
//				GLES20.glViewport(0, 0, mCoreLogic.getScreenWidth(), mCoreLogic.getScreenHeight());
				GLES20.glViewport(0, 0, width, height);
			} else {
				GLES20.glViewport(0, 0, mWidth, mHeight);
			}
		}
		
		public void bindTexture() {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, RBOC);
		}
		
		public void resize(int width, int height) {
			if(FBO != 0) {
				int[] args = {RBOC, RBOD, FBO};
				GLES20.glDeleteTextures(1, args, 0);
				GLES20.glDeleteRenderbuffers(1, args, 1);
				GLES20.glDeleteFramebuffers(1, args, 2);
				create(width, height);
			}
		}
		
		private void create(int width, int height) {
			// FBO
			int[] ret = new int[1];
			
			// frame buffer
			GLES20.glGenFramebuffers(1, ret, 0);
			FBO = ret[0];
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, FBO);
			
			// depth buffer
			GLES20.glGenRenderbuffers(1, ret, 0);
			RBOD = ret[0];
			GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, RBOD);
			GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
			GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, RBOD);
			
			// color buffer (is texture)
			GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
			GLES20.glGenTextures(1, ret, 0);
			RBOC = ret[0];
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, RBOC);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
			GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, RBOC, 0);
			
			if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
				Log.d(TAG, "Fail to create FBO.");
				FBO = 0;
				RBOD = 0;
				RBOC = 0;
			}
		}
	}

	private void checkGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, op + ": glError " + error + " " + GLU.gluErrorString(error));
//			throw new RuntimeException(op + ": glError " + error);
		}
	}
}
