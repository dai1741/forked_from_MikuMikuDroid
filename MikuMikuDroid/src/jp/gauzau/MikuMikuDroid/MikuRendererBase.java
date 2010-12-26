package jp.gauzau.MikuMikuDroid;

import java.io.IOException;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.media.MediaPlayer;
import android.opengl.Matrix;

public class MikuRendererBase implements MikuRendererInterface {
	protected ArrayList <Miku> mMiku;
	protected Miku mMikuStage;
	protected int mBoneNum;
	protected float[] mPMatrix = new float[16];
	
	protected VMDParser mVP;
	
	private float[] mMVMatrix = new float[16];
	private VMDParser mCamera;
	private PMDParser mPP;
	private long mPrevTime;
	private long mStartTime;
	private PMDParser mPPStage;

	private int mWidth;
	private int mHeight;
	private int mAngle;
	private float [] mCameraPos = new float[3];
	private float [] mCameraRot = new float[3];
	private CameraIndex mCameraIndex = new CameraIndex();
	private CameraPair mCameraPair = new CameraPair();
	private double mFPS;
	private MediaPlayer mMedia;
	private ArrayList<MotionIndex> mCenter = null;
	

	public MikuRendererBase() {
		clear();
		mCameraIndex.location = mCameraPos;
		mCameraIndex.rotation = mCameraRot;
	}
	
	/////////////////////////////////////////////////////////////
	// Model configurations
	@Override
	public void loadModel(String file) throws IOException {
		mPP = new PMDParser(file);
		if(mMiku == null) {
			mMiku = new ArrayList <Miku>();
		}
		Miku miku = new Miku(mPP, 256, mBoneNum, true);
		mMiku.add(miku);
	}
	
	@Override
	public void loadMotion(String file) throws IOException {
		if(mPP == null) {
			throw new IOException("PMD file does not open");
		} else {
			VMDParser vp = new VMDParser(file, mPP);
			mMiku.get(mMiku.size() - 1).initBoneManager(vp);
			mPrevTime = 0;
			mVP = vp;
		}
		mMiku.get(mMiku.size()-1).setBonePosByVMDFrame(0);
		mMiku.get(mMiku.size()-1).setFaceByVMDFrame(0);
	}
	
	@Override
	public void loadStage(String file) throws IOException {
		mPPStage = null;
		mMikuStage = null;
		mPPStage = new PMDParser(file);
		mMikuStage = new Miku(mPPStage, 256, mBoneNum, false);
	}
	
	@Override
	public double getFPS() {
    	return mFPS;
    }

	@Override
	public void setMedia(MediaPlayer media) {
		mMedia = media;
	}

	@Override
	public void loadCamera(String camera) throws IOException {
		mCamera = new VMDParser(camera);
	}

	@Override
	public void clear() {
		mMiku = null;
		mMikuStage = null;
		mPP   = null;
		mPPStage = null;
		mVP   = null;
		mPrevTime = 0;
		mStartTime = 0;
		mCamera = null;
		mMedia = null;
		mAngle = 0;
		
		setDefaultCamera();
	}
	
	@Override
	public void setScreenAngle(int angle) {
		mAngle = angle;
		if(mCamera == null) {
			setDefaultCamera();
		}
	}
	
	
	
	/////////////////////////////////////////////////////////////
	// Rendar Interfaces
	@Override
	public void onDrawFrame(GL10 gl) {
		
	}
	
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		
	}
	
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
        mWidth = width;
        mHeight = height;
        
		setDefaultCamera();
	}
	
	
	/////////////////////////////////////////////////////////////
	// Some common methods
	protected double nowFrames(int max_frame) {
		double frame;
		if(mMedia != null) {
			long timeMedia = mMedia.getCurrentPosition();
			long timeLocal = System.currentTimeMillis();
			if(Math.abs(timeLocal - mStartTime - timeMedia) > 500 || mMedia.isPlaying() == false) {
				mStartTime = timeLocal - timeMedia;
			} else {
				timeMedia = timeLocal - mStartTime;
			}
			frame = ((float)timeMedia * 30.0 / 1000.0);
			if(frame > max_frame) {
				frame = max_frame;
			}
			mFPS = 1000.0 / (timeMedia - mPrevTime);
			mPrevTime = timeMedia;
		} else {
			frame = 0;
		}
		
		return frame;
	}
	
	protected void setCameraByVMDFrame(double frame) {
		if(mCamera != null) {
			CameraPair cp = mCamera.findCamera((float) frame, mCameraPair);
			CameraIndex c = mCamera.interpolateLinear(cp, (float)frame, mCameraIndex);
			if(c != null) {
				setCamera(c.length, c.location, c.rotation, c.view_angle, mWidth, mHeight);
			}
//		} else {
//			setCameraToCenter(mMiku.get(0));
		}
	}
	
	protected void setCamera(float d, float [] pos, float [] rot, float angle, int width, int height) {
        // Projection Matrix
		float s = (float) Math.sin(angle * Math.PI / 360);
        Matrix.setIdentityM(mPMatrix , 0);
        if(mAngle == 90) {
        	Matrix.frustumM(mPMatrix, 0, -s, s, -s * height / width, s * height / width, 1f, 3000f);
        } else {
        	Matrix.frustumM(mPMatrix, 0, -s * width / height, s * width / height, -s, s, 1f, 3000f);
        }
        Matrix.scaleM(mPMatrix, 0, 1, 1, -1);		// to right-handed
        Matrix.rotateM(mPMatrix, 0, mAngle, 0, 0, -1);	// rotation        
    
        // camera
        Matrix.translateM(mPMatrix, 0, 0, 0, -d);
        Matrix.rotateM(mPMatrix, 0, rot[2], 0, 0, 1f);
        Matrix.rotateM(mPMatrix, 0, rot[0], 1f, 0, 0);
        Matrix.rotateM(mPMatrix, 0, rot[1], 0, 1f, 0);
        Matrix.translateM(mPMatrix, 0, -pos[0], -pos[1], -pos[2]);
        
        // model-view matrix (is null)
		Matrix.setIdentityM(mMVMatrix, 0);
	}

	protected void setDefaultCamera() {
        if(mAngle == 0) {
            mCameraPos[0] = 0;
            mCameraPos[1] = 10;
            mCameraPos[2] = 0;
            mCameraRot[0] = 0;
            mCameraRot[1] = 0;
            mCameraRot[2] = 0;
            setCamera(-35f, mCameraPos, mCameraRot, 45, mWidth, mHeight);        	
        	/*
            mCameraPos[0] = 0;
            mCameraPos[1] = 13;
            mCameraPos[2] = 0;
            mCameraRot[0] = 0;
            mCameraRot[1] = 0;
            mCameraRot[2] = 0;
            setCamera(-38f, mCameraPos, mCameraRot, 45, mWidth, mHeight);        	
            */
        } else {
            mCameraPos[0] = 0;
            mCameraPos[1] = 10;
            mCameraPos[2] = 0;
            mCameraRot[0] = 0;
            mCameraRot[1] = 0;
            mCameraRot[2] = 0;
            setCamera(-30f, mCameraPos, mCameraRot, 45, mWidth, mHeight);        	
        }
	}
	
	protected void setCameraToCenter(Miku miku) {
		Bone b = miku.getBone().get(0);
		if(mCenter == null) {
			mCenter = new ArrayList<MotionIndex>();
			for(int i = 0; i < 30; i++) {
				MotionIndex m = new MotionIndex();
				m.location = new float[3];
				m.location[0] = b.matrix[12];
				m.location[1] = b.matrix[13];
				m.location[2] = b.matrix[14];
				mCenter.add(m);
			}
		}
		MotionIndex m = mCenter.get(0);
		mCenter.remove(0);
		m.location[0] = b.matrix[12];
		m.location[1] = b.matrix[13];
		m.location[2] = b.matrix[14];			
		mCenter.add(m);

		mCameraPos[0] = mCameraPos[1] = mCameraPos[2] = 0;
		float z = 0;
		for(int i = 0; i < mCenter.size(); i++) {
	        mCameraPos[0] += mCenter.get(i).location[0];
	        mCameraPos[1] += mCenter.get(i).location[1];
	        z             += mCenter.get(i).location[2];
		}
		mCameraPos[0] /= mCenter.size();
		mCameraPos[1] = 13;
		mCameraPos[2] = 0;
        mCameraRot[0] = 0;
        mCameraRot[1] = 0;
        mCameraRot[2] = 0;
		z             /= mCenter.size();
        setCamera(z - 18f, mCameraPos, mCameraRot, 45, mWidth, mHeight);
	}

}
