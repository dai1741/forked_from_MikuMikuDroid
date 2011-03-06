package jp.gauzau.MikuMikuDroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.PowerManager;
import android.util.Log;

public class CoreLogic {
	// model / music data
	private ArrayList<Miku>		mMiku;
	private Miku				mMikuStage;
	private MikuMotion			mCamera;
	private MediaPlayer			mMedia;
	private String				mMediaName;
	private long				mPrevTime;
	private long				mStartTime;
	private double				mFPS;
	
	private float[]				mPMatrix = new float[16];
	private float[]				mMVMatrix = new float[16];
	
	// configurations
	private int					mBoneNum = 0;
	private Context				mCtx;
	private int					mWidth;
	private int					mHeight;
	private int					mAngle;

	// temporary data
	private CameraIndex			mCameraIndex = new CameraIndex();
	private CameraPair			mCameraPair  = new CameraPair();
	
	class Selection {
		public String []		item;
		public StringSelecter	task;
	}
	
	interface StringSelecter {
		public String select(int idx);
	}

	public CoreLogic(Context ctx) {
		clearMember();
		
		mCtx = ctx;
	}
	
	public void setGLConfig(int boneNum) {
		if(mBoneNum == 0) {
			mBoneNum = boneNum;
			onInitialize();
		}
	}
	
	public void onInitialize() {}

	// ///////////////////////////////////////////////////////////
	// Model configurations
	public void loadModel(String modelf) throws IOException {
		// model
		PMDParser pmd = new PMDParser(modelf);
		MikuModel model = new MikuModel(pmd, 256, mBoneNum, false);
		
		// Create Miku
		Miku miku = new Miku(model);
				
		// add Miku
		mMiku.add(miku);
	}
	
	public void loadMotion(String motionf) throws IOException {
		// motion
		VMDParser vmd = new VMDParser(motionf);
		MikuMotion motion = null;

		// check IK cache
		String vmc = motionf.replaceFirst(".vmd", "_mmcache.vmc");
		try {
			ObjectInputStream oi = new ObjectInputStream(new FileInputStream(vmc));
			motion = (MikuMotion)oi.readObject();
			motion.attachVMD(vmd);
		} catch (Exception e) {
			motion = new MikuMotion(vmd);
		}

		// Create Miku
		if(mMiku != null) {
			Miku miku = mMiku.get(mMiku.size() - 1);
			miku.attachMotion(motion);
			miku.setBonePosByVMDFrame(0);
			miku.setFaceByVMDFrame(0);
			
			// store IK chache
			File f = new File(vmc);
			if(!f.exists()) {
				ObjectOutputStream oi = new ObjectOutputStream(new FileOutputStream(vmc));
				oi.writeObject(motion);
			}
		}
	}
	
	public synchronized void loadModelMotion(String modelf, String motionf) throws IOException {
		// check cache
		/*
		String pmc = file.replaceFirst(".pmd", "_mmcache.pmc");
		MikuModel model = null;
		try {
			ObjectInputStream oi = new ObjectInputStream(new FileInputStream(pmc));
			model = (MikuModel)oi.readObject();
			oi.close();
		} catch (Exception e) {
			Log.d("MRB", e.toString());
			e.printStackTrace();
			PMDParser pmd = new PMDParser(file);
			model = new MikuModel(pmd, 256, mBoneNum, true);
			ObjectOutputStream oi = new ObjectOutputStream(new FileOutputStream(pmc));
			oi.writeObject(model);
			oi.close();
		}
		*/

		// model
		PMDParser pmd = new PMDParser(modelf);
		MikuModel model = new MikuModel(pmd, 256, mBoneNum, true);
		
		// motion
		VMDParser vmd = new VMDParser(motionf);
		MikuMotion motion = null;

		// check IK cache
		String vmc = motionf.replaceFirst(".vmd", "_mmcache.vmc");
		try {
			ObjectInputStream oi = new ObjectInputStream(new FileInputStream(vmc));
			motion = (MikuMotion)oi.readObject();
			motion.attachVMD(vmd);
		} catch (Exception e) {
			motion = new MikuMotion(vmd);
		}

		// Create Miku
		Miku miku = new Miku(model);
		miku.attachMotion(motion);
		miku.setBonePosByVMDFrame(0);
		miku.setFaceByVMDFrame(0);
		
		// store IK chache
		File f = new File(vmc);
		if(!f.exists()) {
			ObjectOutputStream oi = new ObjectOutputStream(new FileOutputStream(vmc));
			oi.writeObject(motion);
		}
		
		// add Miku
		mMiku.add(miku);
	}

	public synchronized void loadStage(String file) throws IOException {
		mMikuStage = null;
		PMDParser pmd = new PMDParser(file);
		MikuModel model = new MikuModel(pmd, 256, mBoneNum, false);
		mMikuStage = new Miku(model);
	}

	public double getFPS() {
		return mFPS;
	}

	public synchronized void loadMedia(String media) {
		mMediaName = media;
		Uri uri = Uri.parse(media);
		mMedia = MediaPlayer.create(mCtx, uri);
		if(mMedia != null) {
			mMedia.setWakeMode(mCtx, PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE);			
		}
	}

	public synchronized void loadCamera(String camera) throws IOException {
		mCamera = new MikuMotion(new VMDParser(camera));
	}

	public synchronized void clear() {
		clearMember();
		
		SharedPreferences sp = mCtx.getSharedPreferences("default", 0);
		SharedPreferences.Editor ed = sp.edit();
		ed.clear();
		ed.commit();
	}
	
	private void clearMember() {
		mMiku = new ArrayList<Miku>();
		mMikuStage = null;
		mPrevTime = 0;
		mStartTime = 0;
		mCamera = null;
		if(mMedia != null) {
			mMedia.stop();
			mMedia.release();			
		}
		mMedia = null;
		mAngle = 0;

		setDefaultCamera();
	}

	public synchronized void setScreenAngle(int angle) {
		mAngle = angle;
		if (mCamera == null) {
			setDefaultCamera();
		}
	}
	
	public synchronized void applyCurrentMotion() {
		double frame;
		frame = nowFrames(32767);

		if (mMiku != null) {
			for (Miku miku : mMiku) {
				if(miku.hasMotion()) {
					miku.setBonePosByVMDFrame((float) frame);
					miku.setFaceByVMDFrame((float) frame);					
				}
			}
		}
		setCameraByVMDFrame(frame);
	}
	
	public void pause() {
		if (mMedia != null) {
			mMedia.pause();
		}		
	}

	public boolean toggleStartStop() {
		if (mMedia != null) {
			if (mMedia.isPlaying()) {
				mMedia.pause();
				return false;
			} else {
				mMedia.start();
				return true;
			}
		}
		return false;
	}

	public void rewind() {
		if (mMedia != null) {
			mMedia.seekTo(0);
		}		
	}

	public void storeState() {
		SharedPreferences sp = mCtx.getSharedPreferences("default", Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = sp.edit();
		
		// model & motion
		if(mMiku != null) {
			ed.putInt("ModelNum", mMiku.size());
			for(int i = 0; i < mMiku.size(); i++) {
				Miku m = mMiku.get(i);
				ed.putString(String.format("Model%d", i), m.mModel.mFileName);
				ed.putString(String.format("Motion%d", i), m.mMotion.mFileName);
			}
		} else {
			ed.putInt("ModelNum", 0);
		}
		
		// stage
		if(mMikuStage != null) {
			ed.putString("Stage", mMikuStage.mModel.mFileName);
		}
		
		// camera
		if(mCamera != null) {
			ed.putString("Camera", mCamera.mFileName);
		}
		
		// music
		if(mMedia != null) {
			ed.putString("Music", mMediaName);
			ed.putInt("Position", mMedia.getCurrentPosition());
		}
		
		ed.commit();
		Log.d("CoreLogic", "Store State");
	}
	
	public void restoreState() {
		SharedPreferences sp = mCtx.getSharedPreferences("default", Context.MODE_PRIVATE);
		
		try {
			// model & motion
			int num = sp.getInt("ModelNum", 0);
			
			String model[] = new String[num];
			String motion[] = new String[num];
			for(int i = 0; i < num; i++) {
				model[i]  = sp.getString(String.format("Model%d", i), null);
				motion[i] = sp.getString(String.format("Motion%d", i), null);
			}

			String stage = sp.getString("Stage", null);
			String camera = sp.getString("Camera", null);
			String music = sp.getString("Music", null);
			int pos = sp.getInt("Position", 0);
			
			// crear preferences
			Editor ed = sp.edit();
			ed.clear();
			ed.commit();
			
			// load data
			for(int i = 0; i < num; i++) {
				loadModelMotion(model[i], motion[i]);
			}
			
			if(stage != null) {
				loadStage(stage);
			}			

			if(camera != null) {
				loadCamera(camera);
			}			

			if(music != null) {
				loadMedia(music);
				if(mMedia != null) {
					mMedia.seekTo(pos);					
				}
			}

			// restore
			storeState();
		
		} catch(IOException e) {
			Editor ed = sp.edit();
			ed.clear();
			ed.commit();
		}
		

	}

	public void setScreenSize(int width, int height) {
		mWidth	= width;
		mHeight	= height;
	}

	public ArrayList<Miku> getMiku() {
		return mMiku;
	}
	
	public void returnMiku(Miku miku) {
		
	}
	
	public Miku getMikuStage() {
		return mMikuStage;
	}
	
	public void returnMikuStage(Miku stage) {
		
	}
	
	public float[] getProjectionMatrix() {
		return mPMatrix;
	}
	
	public boolean checkFileIsPrepared() {
		File files = new File("/sdcard/MikuMikuDroid/Data/toon0.bmp");
		return files.exists();
	}
	
	public Selection getModelSelector() {
		final Selection sc = new Selection();
		sc.item = listFiles("/sdcard/MikuMikuDroid/UserFile/Model/", ".pmd");
		sc.task = new StringSelecter() {
			public String select(int idx) {
				return "/sdcard/MikuMikuDroid/UserFile/Model/" + sc.item[idx] + ".pmd";
			}
		};
		return sc;
	}

	public Selection getMotionSelector() {
		final Selection sc = new Selection();
		sc.item = listFiles("/sdcard/MikuMikuDroid/UserFile/Motion/", ".vmd");
		String[] items;
		if(sc.item == null) {
			items = new String[1];
		} else {
			items = new String[sc.item.length + 1];
		}
		for (int i = 0; i < sc.item.length; i++) {
			items[i + 1] = sc.item[i].replaceFirst(".vmd", "");
		}
		items[0] = "Load as Background";
		sc.item = items;

		sc.task = new StringSelecter() {
			public String select(int idx) {
				return "/sdcard/MikuMikuDroid/UserFile/Motion/" + sc.item[idx] + ".vmd";
			}
		};
		return sc;
	}

	public Selection getCameraSelector() {
		final Selection sc = new Selection();
		sc.item = listFiles("/sdcard/MikuMikuDroid/UserFile/Motion/", ".vmd");
		sc.task = new StringSelecter() {
			public String select(int idx) {
				return "/sdcard/MikuMikuDroid/UserFile/Motion/" + sc.item[idx] + ".vmd";
			}
		};
		return sc;
	}
	
	public Selection getMediaSelector() {
		final Selection sc = new Selection();
		sc.item = listFiles("/sdcard/MikuMikuDroid/UserFile/Wave/", ".mp3");
		sc.task = new StringSelecter() {
			public String select(int idx) {
				return "file:///sdcard/MikuMikuDroid/UserFile/Wave/" + sc.item[idx] + ".mp3";
			}
		};
		return sc;
	}

	private String[] listFiles(String dir, final String ext) {
		File files = new File(dir);
		String[] item = files.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(ext);
			}
		});
		if(item != null) {
			Arrays.sort(item);
			for (int i = 0; i < item.length; i++) {
				item[i] = item[i].replaceFirst(ext, "");
			}
		}
		
		return item;
	}
	
	// ///////////////////////////////////////////////////////////
	// Some common methods


	protected double nowFrames(int max_frame) {
		double frame;
		if (mMedia != null) {
			long timeMedia = mMedia.getCurrentPosition();
			long timeLocal = System.currentTimeMillis();
			if (Math.abs(timeLocal - mStartTime - timeMedia) > 500 || mMedia.isPlaying() == false) {
				mStartTime = timeLocal - timeMedia;
			} else {
				timeMedia = timeLocal - mStartTime;
			}
			frame = ((float) timeMedia * 30.0 / 1000.0);
			if (frame > max_frame) {
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
		if (mCamera != null) {
			CameraPair cp = mCamera.findCamera((float) frame, mCameraPair);
			CameraIndex c = mCamera.interpolateLinear(cp, (float) frame, mCameraIndex);
			if (c != null) {
				setCamera(c.length, c.location, c.rotation, c.view_angle, mWidth, mHeight);
			}
		}
	}

	protected void setCamera(float d, float[] pos, float[] rot, float angle, int width, int height) {
		// Projection Matrix
		float s = (float) Math.sin(angle * Math.PI / 360);
		Matrix.setIdentityM(mPMatrix, 0);
		if (mAngle == 90) {
			Matrix.frustumM(mPMatrix, 0, -s, s, -s * height / width, s * height / width, 1f, 3000f);
		} else {
			Matrix.frustumM(mPMatrix, 0, -s * width / height, s * width / height, -s, s, 1f, 3000f);
		}
		Matrix.scaleM(mPMatrix, 0, 1, 1, -1); // to right-handed
		Matrix.rotateM(mPMatrix, 0, mAngle, 0, 0, -1); // rotation

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
		if (mAngle == 0) {
			mCameraIndex.location[0] = 0;
			mCameraIndex.location[1] = 10; // 13
			mCameraIndex.location[2] = 0;
			mCameraIndex.rotation[0] = 0;
			mCameraIndex.rotation[1] = 0;
			mCameraIndex.rotation[2] = 0;
			setCamera(-35f, mCameraIndex.location, mCameraIndex.rotation, 45, mWidth, mHeight); // -38f
		} else {
			mCameraIndex.location[0] = 0;
			mCameraIndex.location[1] = 10;
			mCameraIndex.location[2] = 0;
			mCameraIndex.rotation[0] = 0;
			mCameraIndex.rotation[1] = 0;
			mCameraIndex.rotation[2] = 0;
			setCamera(-30f, mCameraIndex.location, mCameraIndex.rotation, 45, mWidth, mHeight);
		}
	}
}
