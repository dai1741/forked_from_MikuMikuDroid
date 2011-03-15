package jp.gauzau.MikuMikuDroid;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

import android.util.Log;

public class VMDParser extends ParserBase {
	private String  mFileName;
	private boolean mIsVmd;
	private String mModelName;
	private int mMaxFrame;
	private HashMap<String, ArrayList<MotionIndex>> mMotionHash;
	private HashMap<String, ArrayList<FaceIndex>> mFaceHash;
	private ArrayList<CameraIndex> mCamera;

	public VMDParser(String file) throws IOException {
		super(file);
		mFileName = file;
		mMaxFrame = 0;
		parseVMDHeader();
		if(mIsVmd) {
			parseVMDFrame();
			parseVMDFace();
			parseVMDCamera();			
		}
	}
	
	private void parseVMDCamera() {
		int num = getInt();
		Log.d("VMDParser", String.format("Camera: %d", num));
		if (num > 0) {
			mCamera = new ArrayList<CameraIndex>(num);
			for (int i = 0; i < num; i++) {
				CameraIndex m = new CameraIndex();
				m.frame_no = getInt();
				m.length = getFloat();

				m.location = new float[3];
				getFloat(m.location);

				m.rotation = new float[3];
				m.rotation[0] = (float) (getFloat() * 360 / (2 * Math.PI));
				m.rotation[1] = (float) (getFloat() * 360 / (2 * Math.PI));
				m.rotation[2] = (float) (getFloat() * 360 / (2 * Math.PI));

				m.interp = new byte[24];
				getBytes(m.interp, 24);

				m.view_angle = getInt();
				m.perspective = getByte(); // 0: on, 1:0ff

				mCamera.add(m);
				if (m.frame_no > mMaxFrame) {
					mMaxFrame = m.frame_no;
				}
			}

			// sorted by frame_no
			Collections.sort(mCamera, new Comparator<CameraIndex>() {
				public int compare(CameraIndex m0, CameraIndex m1) {
					return m0.frame_no - m1.frame_no;
				}
			});
		} else {
			mCamera = null;
		}
	}

	private void parseVMDFace() {
		int num = getInt();
		Log.d("VMDParser", String.format("Face num: %d", num));

		if (num > 0) {
			// initialize face hash
			mFaceHash = new HashMap<String, ArrayList<FaceIndex> >();

			// parser
			for (int i = 0; i < num; i++) {
				FaceIndex m = new FaceIndex();

				String name = getString(15);
				m.frame_no = getInt();
				m.weight = getFloat();

				ArrayList<FaceIndex> fi = mFaceHash.get(name);
				if(fi == null) {
					fi = new ArrayList<FaceIndex>();
					
					// default face
					FaceIndex d = new FaceIndex();
					d.frame_no = -1;
					d.weight = 0;
					fi.add(d);

					mFaceHash.put(name, fi);
				}
				fi.add(m);
			}

			// sorted by frame_no
			for (Entry<String, ArrayList<FaceIndex>> f : mFaceHash.entrySet()) {
				Collections.sort(f.getValue(), new Comparator<FaceIndex>() {
					public int compare(FaceIndex m0, FaceIndex m1) {
						return m0.frame_no - m1.frame_no;
					}
				});
			}
		} else {
			mFaceHash = null;
		}

	}

	private void parseVMDFrame() {
		int num = getInt();
		Log.d("VMDParser", String.format("Animation: %d", num));
		ByteBuffer bone_name = ByteBuffer.allocate(15);
		byte[] name = new byte[15];

		if (num > 0) {
			// initialize bone hash
			HashMap<ByteBuffer, ArrayList<MotionIndex>> mh = new HashMap<ByteBuffer, ArrayList<MotionIndex> >();
			mMotionHash = new HashMap<String, ArrayList<MotionIndex> >();

			// parse
			for (int i = 0; i < num; i++) {
				MotionIndex m = new MotionIndex();
				m.location    = new float[3];
				m.rotation    = new float[4];
				m.interp      = new byte[16];

				getStringBytes(name, 15);
				bone_name.position(0);
				bone_name.put(name);
				bone_name.position(0);
				m.frame_no = getInt();
				getFloat(m.location);
				getFloat(m.rotation);
				getBytes(m.interp, 16);
				position(position() + 48);
				
				ArrayList<MotionIndex> mi = mh.get(bone_name);
				if(mi == null) {
					mi = new ArrayList<MotionIndex>();

					// set default
					MotionIndex d = new MotionIndex();
					d.location = new float[3];
					d.rotation = new float[4];
					
					d.frame_no = -1;
					d.location[0] = d.location[1] = d.location[2] = 0;
					d.rotation[0] = d.rotation[1] = d.rotation[2] = 0;
					d.rotation[3] = 1;
					d.interp = null;
					mi.add(d);
					
					mh.put(bone_name, mi);
					bone_name = ByteBuffer.allocate(15);
				}
				mi.add(m);
				if (m.frame_no > mMaxFrame) {
					mMaxFrame = m.frame_no;
				}
			}

			// sorted by frame_no
			for (Entry<ByteBuffer, ArrayList<MotionIndex>> b : mh.entrySet()) {
				Collections.sort(b.getValue(), new Comparator<MotionIndex>() {
					public int compare(MotionIndex m0, MotionIndex m1) {
						return m0.frame_no - m1.frame_no;
					}
				});
				b.getKey().position(0);
				b.getKey().get(name);
				mMotionHash.put(toString(name), b.getValue());
			}
		} else {
			mMotionHash = null;
		}
	}
	
	private void parseVMDHeader() {
		String magic = getString(30);
		Log.d("VMDParser", "MAGIC: " + magic);
		if (magic.equals("Vocaloid Motion Data 0002")) {
			mIsVmd = true;
			mModelName = getString(20);
		} else {
			mIsVmd = false;
		}

	}

	public String getModelName() {
		return mModelName;
	}

	public boolean isVmd() {
		return mIsVmd;
	}

	public int maxFrame() {
		return mMaxFrame;
	}
	
	public HashMap<String, ArrayList<MotionIndex>> getMotion() {
		return mMotionHash;
	}
	
	public HashMap<String, ArrayList<FaceIndex>> getFace() {
		return mFaceHash;
	}
	
	public ArrayList<CameraIndex> getCamera() {
		return mCamera;
	}
	
	public String getFileName() {
		return mFileName;
	}
}
