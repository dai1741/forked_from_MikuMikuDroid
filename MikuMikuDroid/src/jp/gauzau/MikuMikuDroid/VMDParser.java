package jp.gauzau.MikuMikuDroid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.util.Log;

public class VMDParser extends ParserBase {
	private boolean mIsVmd;
	private String mModelName;
	private int mNumFrameData;
	private PMDParser mPMD;
	private int mMaxFrame;
	private int mNumFace;
	private HashMap<String, Face> mFaceHash;
	private int mNumCamera;
	private ArrayList<CameraIndex> mCamera;
	private int mCameraCurrent;


	public VMDParser(String file, PMDParser pmd) throws IOException {
		super(file);
		mPMD = pmd;
		mMaxFrame = 0;
		parseVMDHeader();
		parseVMDFrame();
		parseVMDFace();
		parseVMDCamera();
	}
	
	public VMDParser(String file) throws IOException {	// for camera
		super(file);
		mPMD = null;
		mMaxFrame = 0;
		parseVMDHeader();
		parseVMDFrame();
		parseVMDFace();
		parseVMDCamera();
	}

	private void parseVMDCamera() {
		mNumCamera = getInt();
		Log.d("VMDParser", String.format("Camera: %d", mNumCamera));
		if(mNumCamera > 0) {
			mCamera = new ArrayList <CameraIndex> (mNumCamera);
			for(int i = 0; i < mNumCamera; i++) {
				CameraIndex m = new CameraIndex();
				m.frame_no    = getInt();
				m.length      = getFloat();
				
				m.location = new float[3];
				getFloat(m.location);
				
				m.rotation = new float[3];
				m.rotation[0] = (float) (getFloat() * 360 / (2 * Math.PI));
				m.rotation[1] = (float) (getFloat() * 360 / (2 * Math.PI));
				m.rotation[2] = (float) (getFloat() * 360 / (2 * Math.PI));
				
				m.interp = new byte[24];
				getBytes(m.interp, 24);
				
				m.view_angle  = getInt();
				m.perspective = getByte();	// 0: on, 1:0ff
				
				/*
				Log.d("VMDParser", String.format("Camera frame %d: length %f, location = (%f, %f, %f), rotation = (%f, %f, %f)", 
						m.frame_no, m.length, m.location[0], m.location[1], m.location[2],
						m.rotation[0], m.rotation[1], m.rotation[2]));
				Log.d("VMDParser", String.format("Camera frame %d: angle %d, perspective %d", m.frame_no, m.view_angle, m.perspective));
				*/
				
				mCamera.add(m);
				if(m.frame_no > mMaxFrame) {
					mMaxFrame = m.frame_no;
				}
			}	
			
			// sorted by frame_no
			Collections.sort(mCamera, new Comparator<CameraIndex>() {
				public int compare(CameraIndex m0, CameraIndex m1) {
					return m0.frame_no - m1.frame_no;
				}
			});
		}
	}

	private void parseVMDFace() {
		mNumFace = getInt();
		Log.d("VMDParser", String.format("Face num: %d", mNumFace));

		if(mNumFace > 0) {
			// initialize face hash
			mFaceHash = new HashMap<String, Face>();
			if(mPMD != null) {
				for(Face f: mPMD.getFace()) {
					mFaceHash.put(f.name, f);
					f.motion = null;
				}
			}
			
			// parser
			for(int i = 0; i < mNumFace; i++) {
				FaceIndex m = new FaceIndex();

				String name = getString(15);
				m.frame_no = getInt();
				m.weight   = getFloat();
				
				Face f = mFaceHash.get(name);
				if(f != null) {
					if(f.motion == null) {
						f.motion = new ArrayList<FaceIndex>();
						
						// default face
						FaceIndex d = new FaceIndex();
						d.frame_no = -1;
						d.weight = 0;
						f.motion.add(d);
						
						f.motion.add(m);
						f.current_motion = 0;
					} else { // VMD motion entry is *not* sorted by frame_no
						f.motion.add(m);
					}
//					Log.d("VMDParser", String.format("Face %s: frame = %d, weight = %f", m.name, m.frame_no, m.weight));
				} else {
//					Log.d("VMDParser", "find face " + m.name + " that does not exist in PMD.");
				}
			}
			
			// sorted by frame_no
			if(mPMD != null) {
				for(Face f: mPMD.getFace()) {
					if(f.motion != null) {
						Collections.sort(f.motion, new Comparator<FaceIndex>() {
							public int compare(FaceIndex m0, FaceIndex m1) {
								return m0.frame_no - m1.frame_no;
							}
						});
					}
				}
			}
		}

	}

	private void parseVMDFrame() {
		mNumFrameData = getInt();
		Log.d("VMDParser", String.format("Animation: %d", mNumFrameData));
		byte [] bone_name = new byte[15];

		if(mNumFrameData > 0) {
			// parse
			for(int i = 0; i < mNumFrameData; i++) {
				getBytes(15, bone_name);
				
				MotionIndex m = new MotionIndex();
				m.frame_no    = getInt();
				m.position    = position();
				
				m.location = new float[3];
				getFloat(m.location);
				
				m.rotation = new float[4];
				getFloat(m.rotation);
				
				m.interp = new byte[16];
				getBytes(m.interp, 16);
				position(position() + 48);
				
				Bone b = mPMD == null ? null : findBone(mPMD.getBone(), bone_name);
				if(b != null) {
					if(b.motion == null) {
						b.motion = new ArrayList<MotionIndex>();
						
						// set default
						MotionIndex d = new MotionIndex();
						d.frame_no = -1;
						d.location = new float[3];
						d.location[0] = d.location[1] = d.location[2] = 0;
						                           
						d.rotation = new float[4];
						d.rotation[0] = d.rotation[1] = d.rotation[2] = 0;
						d.rotation[3] = 1;
						d.interp = null;
						b.motion.add(d);
						
						b.motion.add(m);
						b.current_motion = 0;
					} else { // VMD motion entry is *not* sorted by frame_no
						b.motion.add(m);
					}
				} else {
//					Log.d("VMDParser", "find bone " + m.bone_name + " that does not exist in PMD.");
				}
				if(m.frame_no > mMaxFrame) {
					mMaxFrame = m.frame_no;
				}
			}
			
			// sorted by frame_no
			if(mPMD != null) {
				for(Bone b: mPMD.getBone()) {
					if(b.motion != null) {
						Collections.sort(b.motion, new Comparator<MotionIndex>() {
							public int compare(MotionIndex m0, MotionIndex m1) {
								return m0.frame_no - m1.frame_no;
							}
						});
					}
				}
			}
		}
	}
	
	private Bone findBone(ArrayList<Bone> bone, byte[] name) {
		int max = bone.size();
		for(int i = 0; i < max; i++) {
			byte [] bn = bone.get(i).name_bytes;
			int j = 0;
			for(j = 0; j < bn.length; j++) {
				if(bn[j] != name[j]) {
					break;
				}
				if(bn[j] == '\0' || j == bn.length - 1) {
					return bone.get(i);
				}
			}
		}
		return null;
	}

	private void parseVMDHeader() {
		String magic = getString(30);
		Log.d("VMDParser", "MAGIC: " + magic);
		if(magic.equals("Vocaloid Motion Data 0002")) {
			mIsVmd = true;
			mModelName = getString(20);
		} else {
			mIsVmd = false;
		}
		
	}

	public int numFrameData() {
		return mNumFrameData;
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

	public MotionPair findMotion(Bone b, float frame, MotionPair mp)  {
		if(b != null && b.motion != null) {
			mp.m0 = null;
			mp.m1 = null;
			for(int i = b.current_motion; i < b.motion.size(); i++) {
				MotionIndex m = b.motion.get(i);
				if(m.frame_no == frame) {
					mp.m0 = m;
					mp.m1 = null;
					b.current_motion = i;
					return mp;
				} else if(m.frame_no < frame) {
					mp.m0 = m;
					b.current_motion = i;
				} else if(m.frame_no > frame) {
					mp.m1 = m;
					if(mp.m0 != null) {
						return mp;
					} else {
						break;
					}
				}
			}
			if(mp.m0 != null && mp.m1 == null) {	// the end of motion
				return mp;
			}
			if(frame > b.motion.size()) {
				mp.m0 = b.motion.get(b.motion.size()-1);
				mp.m1 = null;
				return mp;
			}
			for(int i = 0; i < b.motion.size(); i++) {
				MotionIndex m = b.motion.get(i);
				
				if(m.frame_no == frame) {
					mp.m0 = m;
					mp.m1 = null;
					b.current_motion = b.motion.indexOf(m);
					return mp;
				} else if(m.frame_no < frame) {
					mp.m0 = m;
					b.current_motion = b.motion.indexOf(m);
				} else if(m.frame_no > frame) {
					mp.m1 = m;
					return mp;
				}
			}
		}
		return null;
	}
	
	public Motion interpolateLinear(MotionPair mp, float frame, Motion m) {
		if(mp == null) {
			return null;
		} else if(mp.m1 == null) {
			System.arraycopy(mp.m0.location, 0, m.location, 0, 3);
			System.arraycopy(mp.m0.rotation, 0, m.rotation, 0, 4);
			return m;
		} else {
			int dif  = mp.m1.frame_no - mp.m0.frame_no;
			float a0   = frame - mp.m0.frame_no;
			float ratio = a0 / dif;
			
			if(mp.m0.interp == null) {	// calcurated in preCalcIK
				float t = ratio;
				m.location[0] = mp.m0.location[0] + (mp.m1.location[0] - mp.m0.location[0]) * t;
				m.location[1] = mp.m0.location[1] + (mp.m1.location[1] - mp.m0.location[1]) * t;
				m.location[2] = mp.m0.location[2] + (mp.m1.location[2] - mp.m0.location[2]) * t;
				lerp(m.rotation, mp.m0.rotation, mp.m1.rotation, t);
			} else {
				double t = bazier(mp.m0.interp, 0, 4, ratio);
				m.location[0] = (float) (mp.m0.location[0] + (mp.m1.location[0] - mp.m0.location[0]) * t);
				t = bazier(mp.m0.interp, 1, 4, ratio);
				m.location[1] = (float) (mp.m0.location[1] + (mp.m1.location[1] - mp.m0.location[1]) * t);
				t = bazier(mp.m0.interp, 2, 4, ratio);
				m.location[2] = (float) (mp.m0.location[2] + (mp.m1.location[2] - mp.m0.location[2]) * t);
				
				slerp(m.rotation, mp.m0.rotation, mp.m1.rotation, bazier(mp.m0.interp, 3, 4, ratio));				
			}

			return m;
		}
	}

	public FacePair findFace(Face f, float frame, FacePair mp)  {
		if(f != null && f.motion != null) {
			mp.m0 = null;
			mp.m1 = null;
			for(int i = f.current_motion; i < f.motion.size(); i++) {
				FaceIndex m = f.motion.get(i);
				if(m.frame_no == frame) {
					mp.m0 = m;
					mp.m1 = null;
					f.current_motion = i;
					return mp;
				} else if(m.frame_no < frame) {
					mp.m0 = m;
					f.current_motion = i;
				} else if(m.frame_no > frame) {
					mp.m1 = m;
					if(mp.m0 != null) {
						return mp;
					} else {
						break;
					}
				}
			}
			if(mp.m0 != null && mp.m1 == null) {	// the end of motion
				return mp;
			}
			if(frame > f.motion.size()) {
				mp.m0 = f.motion.get(f.motion.size()-1);
				mp.m1 = null;
				return mp;
			}
			for(int i = 0; i < f.motion.size(); i++) {
				FaceIndex m = f.motion.get(i);
				
				if(m.frame_no == frame) {
					mp.m0 = m;
					mp.m1 = null;
					f.current_motion = f.motion.indexOf(m);
					return mp;
				} else if(m.frame_no < frame) {
					mp.m0 = m;
					f.current_motion = f.motion.indexOf(m);
				} else if(m.frame_no > frame) {
					mp.m1 = m;
					return mp;
				}
			}
		}

		return null;
	}
	
	public FaceIndex interpolateLinear(FacePair mp, float frame, FaceIndex m) {
		if(mp == null) {
			return null;
		} else if(mp.m1 == null) {
			return mp.m0;
		} else {
			int dif  = mp.m1.frame_no - mp.m0.frame_no;
			float a0   = frame - mp.m0.frame_no;
			float ratio = a0 / dif;
			
			m.weight = mp.m0.weight + (mp.m1.weight - mp.m0.weight) * ratio;

			return m;
		}
	}
	
	public CameraPair findCamera(float frame, CameraPair mp)  {
		if(mCamera != null) {
			mp.m0 = null;
			mp.m1 = null;
			for(int i = mCameraCurrent; i < mCamera.size(); i++) {
				CameraIndex c = mCamera.get(i);
				if(c.frame_no == frame) {
					mp.m0 = c;
					mp.m1 = null;
					mCameraCurrent = i;
					return mp;
				} else if(c.frame_no < frame) {
					mp.m0 = c;
					mCameraCurrent = i;
				} else if(c.frame_no > frame) {
					mp.m1 = c;
					if(mp.m0 != null) {
						return mp;
					} else {
						break;
					}
				}
			}
			if(mp.m0 != null && mp.m1 == null) {	// the end of motion
				return mp;
			}
			if(frame > mCamera.size()) {
				mp.m0 = mCamera.get(mCamera.size()-1);
				mp.m1 = null;
				return mp;
			}

//			for(CameraIndex c: mCamera) {
			for(int i = 0; i < mCamera.size(); i++) {
				CameraIndex c = mCamera.get(i);
				if(c.frame_no == frame) {
					mp.m0 = c;
					mp.m1 = null;
					mCameraCurrent = mCamera.indexOf(c);
					return mp;
				} else if(c.frame_no < frame) {
					mp.m0 = c;
					mCameraCurrent = mCamera.indexOf(c);
				} else if(c.frame_no > frame) {
					mp.m1 = c;
					return mp;
				}
			}

		}
		return null;
	}
	
	public CameraIndex interpolateLinear(CameraPair mp, float frame, CameraIndex m) {
		if(mp == null) {
			return null;
		} else if(mp.m1 == null) {
			return mp.m0;
		} else {
			int dif  = mp.m1.frame_no - mp.m0.frame_no;
			if(dif <= 1) {	// assume that scene is changed
				System.arraycopy(mp.m0.location, 0, m.location, 0, 3);
				System.arraycopy(mp.m0.rotation, 0, m.rotation, 0, 3);
				m.length = mp.m0.length;
				m.view_angle = mp.m0.view_angle;
			} else {
				float a0   = frame - mp.m0.frame_no;
				float ratio = a0 / dif;

				double t = bazier(mp.m0.interp, 0, 6, ratio);
				m.location[0] = (float) (mp.m0.location[0] + (mp.m1.location[0] - mp.m0.location[0]) * t);
				t = bazier(mp.m0.interp, 1, 6, ratio);
				m.location[1] = (float) (mp.m0.location[1] + (mp.m1.location[1] - mp.m0.location[1]) * t);
				t = bazier(mp.m0.interp, 2, 6, ratio);
				m.location[2] = (float) (mp.m0.location[2] + (mp.m1.location[2] - mp.m0.location[2]) * t);
				
				t = bazier(mp.m0.interp, 3, 6, ratio);
				m.rotation[0] = (float) (mp.m0.rotation[0] + (mp.m1.rotation[0] - mp.m0.rotation[0]) * t);
				m.rotation[1] = (float) (mp.m0.rotation[1] + (mp.m1.rotation[1] - mp.m0.rotation[1]) * t);
				m.rotation[2] = (float) (mp.m0.rotation[2] + (mp.m1.rotation[2] - mp.m0.rotation[2]) * t);

				t = bazier(mp.m0.interp, 4, 6, ratio);
				m.length = (float) (mp.m0.length + (mp.m1.length - mp.m0.length) * t);
				
				t = bazier(mp.m0.interp, 5, 6, ratio);
				m.view_angle = (float) (mp.m0.view_angle + (mp.m1.view_angle - mp.m0.view_angle) * t);

			}
			
			return m;
		}
	}
	
	private void lerp(float p[], float q[], float r[], float t) {
		  double qr = q[0] * r[0] + q[1] * r[1] + q[2] * r[2] + q[3] * r[3];
		  double ss = 1.0 - t;
		  
		  if(qr > 0) {
			    p[0] = (float) (q[0] * ss + r[0] * t);
			    p[1] = (float) (q[1] * ss + r[1] * t);
			    p[2] = (float) (q[2] * ss + r[2] * t);
			    p[3] = (float) (q[3] * ss + r[3] * t);
		  } else {
			    p[0] = (float) (q[0] * ss - r[0] * t);
			    p[1] = (float) (q[1] * ss - r[1] * t);
			    p[2] = (float) (q[2] * ss - r[2] * t);
			    p[3] = (float) (q[3] * ss - r[3] * t);
			  
		  }
	}
	
	private void slerp(float p[], float q[], float r[], double t) {
		  double qr = q[0] * r[0] + q[1] * r[1] + q[2] * r[2] + q[3] * r[3];
		  double ss = 1.0 - qr * qr;

		  if(qr < 0) {
			  qr = -qr;
			  
			  double sp = Math.sqrt(ss);
			  double ph = Math.acos(qr);
			  double pt = ph * t;
			  double t1 = Math.sin(pt) / sp;
			  double t0 = Math.sin(ph - pt) / sp;
			  
			  if(Double.isNaN(t0) || Double.isNaN(t1)) {
				    p[0] = q[0];
				    p[1] = q[1];
				    p[2] = q[2];
				    p[3] = q[3];					  
			  } else {
				  p[0] = (float) (q[0] * t0 - r[0] * t1);
				  p[1] = (float) (q[1] * t0 - r[1] * t1);
				  p[2] = (float) (q[2] * t0 - r[2] * t1);
				  p[3] = (float) (q[3] * t0 - r[3] * t1);
			  }

		  } else {
			  double sp = Math.sqrt(ss);
			  double ph = Math.acos(qr);
			  double pt = ph * t;
			  double t1 = Math.sin(pt) / sp;
			  double t0 = Math.sin(ph - pt) / sp;

			  if(Double.isNaN(t0) || Double.isNaN(t1)) {
				    p[0] = q[0];
				    p[1] = q[1];
				    p[2] = q[2];
				    p[3] = q[3];					  
			  } else {
				  p[0] = (float) (q[0] * t0 + r[0] * t1);
				  p[1] = (float) (q[1] * t0 + r[1] * t1);
				  p[2] = (float) (q[2] * t0 + r[2] * t1);
				  p[3] = (float) (q[3] * t0 + r[3] * t1);				  
			  }
		  }
	}
	
	private double bazier(byte[] ip, int ofs, int size, float t) {
		double xa = ip[       ofs] / 256;
		double xb = ip[size*2+ofs] / 256;
		double ya = ip[size  +ofs] / 256;
		double yb = ip[size*3+ofs] / 256;
		
		double min = 0;
		double max = 1;
		
		double ct = t;
		while(true) {
			double x11 = xa * ct;
			double x12 = xa + (xb - xa) * ct;
			double x13 = xb + ( 1 - xb) * ct;
			
			double x21 = x11 + (x12 - x11) * ct;
			double x22 = x12 + (x13 - x12) * ct;
			
			double x3  = x21 + (x22 - x21) * ct;
			
			if(Math.abs(x3 - t) < 0.0001) {
				double y11 = ya * ct;
				double y12 = ya + (yb - ya) * ct;
				double y13 = yb + ( 1 - yb) * ct;
				
				double y21 = y11 + (y12 - y11) * ct;
				double y22 = y12 + (y13 - y12) * ct;
				
				double y3  = y21 + (y22 - y21) * ct;
				
				return y3;
			} else if(x3 < t) {
				min = ct;
			} else {
				max = ct;
			}
			ct = min * 0.5 + max * 0.5;
		}
	}
}
