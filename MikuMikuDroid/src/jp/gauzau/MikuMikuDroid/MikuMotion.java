package jp.gauzau.MikuMikuDroid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class MikuMotion implements Serializable {
	private static final long serialVersionUID = -7581376490687593200L;
	private transient int mMaxFrame;
	private transient HashMap<String, ArrayList<MotionIndex>> mMotion;
	private transient HashMap<String, ArrayList<FaceIndex>> mFace;
	private transient ArrayList<CameraIndex> mCamera;
	private transient int mCameraCurrent;

	private HashMap<String, ArrayList<MotionIndex>> mIKMotion;

	public MikuMotion(VMDParser vmd) {
		mCameraCurrent	= 0;
		mIKMotion		= null;
		attachVMD(vmd);
	}
	
	public void attachVMD(VMDParser vmd) {
		mMotion			= vmd.getMotion();
		mFace			= vmd.getFace();
		mCamera			= vmd.getCamera();
		mMaxFrame		= vmd.maxFrame();		
	}
	
	public void attachModel(ArrayList<Bone> ba, ArrayList<Face> fa) {
		for(Bone b: ba) {
			if(mIKMotion != null) {
				b.motion = mIKMotion.get(b.name);
				if(b.motion == null) {
					b.motion = mMotion.get(b.name);					
				}
			} else {
				b.motion = mMotion.get(b.name);				
			}
			b.current_motion = 0;
		}
		for(Face f: fa) {
			f.motion = mFace.get(f.name);
			f.current_motion = 0;
		}
	}
	
	public int maxFrame() {
		return mMaxFrame;
	}
	
	public HashMap<String, ArrayList<MotionIndex>> getMotion() {
		return mMotion;
	}
	
	public HashMap<String, ArrayList<FaceIndex>> getFace() {
		return mFace;
	}
	
	public HashMap<String, ArrayList<MotionIndex>> getIKMotion() {
		return mIKMotion;
	}
	
	public void setIKMotion(HashMap<String, ArrayList<MotionIndex>> m) {
		mIKMotion = m;
	}
	
	public MotionPair findMotion(Bone b, float frame, MotionPair mp) {
		if (b != null && b.motion != null) {
			mp.m0 = null;
			mp.m1 = null;
			for (int i = b.current_motion; i < b.motion.size(); i++) {
				MotionIndex m = b.motion.get(i);
				if (m.frame_no == frame) {
					mp.m0 = m;
					mp.m1 = null;
					b.current_motion = i;
					return mp;
				} else if (m.frame_no < frame) {
					mp.m0 = m;
					b.current_motion = i;
				} else if (m.frame_no > frame) {
					mp.m1 = m;
					if (mp.m0 != null) {
						return mp;
					} else {
						break;
					}
				}
			}
			if (mp.m0 != null && mp.m1 == null) { // the end of motion
				return mp;
			}
			if (frame > b.motion.size()) {
				mp.m0 = b.motion.get(b.motion.size() - 1);
				mp.m1 = null;
				return mp;
			}
			for (int i = 0; i < b.motion.size(); i++) {
				MotionIndex m = b.motion.get(i);

				if (m.frame_no == frame) {
					mp.m0 = m;
					mp.m1 = null;
					b.current_motion = b.motion.indexOf(m);
					return mp;
				} else if (m.frame_no < frame) {
					mp.m0 = m;
					b.current_motion = b.motion.indexOf(m);
				} else if (m.frame_no > frame) {
					mp.m1 = m;
					return mp;
				}
			}
		}
		return null;
	}

	public Motion interpolateLinear(MotionPair mp, float frame, Motion m) {
		if (mp == null) {
			return null;
		} else if (mp.m1 == null) {
			System.arraycopy(mp.m0.location, 0, m.location, 0, 3);
			System.arraycopy(mp.m0.rotation, 0, m.rotation, 0, 4);
			return m;
		} else {
			int dif = mp.m1.frame_no - mp.m0.frame_no;
			float a0 = frame - mp.m0.frame_no;
			float ratio = a0 / dif;

			if (mp.m0.interp == null) { // calcurated in preCalcIK
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

	public FacePair findFace(Face f, float frame, FacePair mp) {
		if (f != null && f.motion != null) {
			mp.m0 = null;
			mp.m1 = null;
			for (int i = f.current_motion; i < f.motion.size(); i++) {
				FaceIndex m = f.motion.get(i);
				if (m.frame_no == frame) {
					mp.m0 = m;
					mp.m1 = null;
					f.current_motion = i;
					return mp;
				} else if (m.frame_no < frame) {
					mp.m0 = m;
					f.current_motion = i;
				} else if (m.frame_no > frame) {
					mp.m1 = m;
					if (mp.m0 != null) {
						return mp;
					} else {
						break;
					}
				}
			}
			if (mp.m0 != null && mp.m1 == null) { // the end of motion
				return mp;
			}
			if (frame > f.motion.size()) {
				mp.m0 = f.motion.get(f.motion.size() - 1);
				mp.m1 = null;
				return mp;
			}
			for (int i = 0; i < f.motion.size(); i++) {
				FaceIndex m = f.motion.get(i);

				if (m.frame_no == frame) {
					mp.m0 = m;
					mp.m1 = null;
					f.current_motion = f.motion.indexOf(m);
					return mp;
				} else if (m.frame_no < frame) {
					mp.m0 = m;
					f.current_motion = f.motion.indexOf(m);
				} else if (m.frame_no > frame) {
					mp.m1 = m;
					return mp;
				}
			}
		}

		return null;
	}

	public FaceIndex interpolateLinear(FacePair mp, float frame, FaceIndex m) {
		if (mp == null) {
			return null;
		} else if (mp.m1 == null) {
			return mp.m0;
		} else {
			int dif = mp.m1.frame_no - mp.m0.frame_no;
			float a0 = frame - mp.m0.frame_no;
			float ratio = a0 / dif;

			m.weight = mp.m0.weight + (mp.m1.weight - mp.m0.weight) * ratio;

			return m;
		}
	}

	public CameraPair findCamera(float frame, CameraPair mp) {
		if (mCamera != null) {
			mp.m0 = null;
			mp.m1 = null;
			for (int i = mCameraCurrent; i < mCamera.size(); i++) {
				CameraIndex c = mCamera.get(i);
				if (c.frame_no == frame) {
					mp.m0 = c;
					mp.m1 = null;
					mCameraCurrent = i;
					return mp;
				} else if (c.frame_no < frame) {
					mp.m0 = c;
					mCameraCurrent = i;
				} else if (c.frame_no > frame) {
					mp.m1 = c;
					if (mp.m0 != null) {
						return mp;
					} else {
						break;
					}
				}
			}
			if (mp.m0 != null && mp.m1 == null) { // the end of motion
				return mp;
			}
			if (frame > mCamera.size()) {
				mp.m0 = mCamera.get(mCamera.size() - 1);
				mp.m1 = null;
				return mp;
			}

			for (int i = 0; i < mCamera.size(); i++) {
				CameraIndex c = mCamera.get(i);
				if (c.frame_no == frame) {
					mp.m0 = c;
					mp.m1 = null;
					mCameraCurrent = mCamera.indexOf(c);
					return mp;
				} else if (c.frame_no < frame) {
					mp.m0 = c;
					mCameraCurrent = mCamera.indexOf(c);
				} else if (c.frame_no > frame) {
					mp.m1 = c;
					return mp;
				}
			}

		}
		return null;
	}

	public CameraIndex interpolateLinear(CameraPair mp, float frame, CameraIndex m) {
		if (mp == null) {
			return null;
		} else if (mp.m1 == null) {
			return mp.m0;
		} else {
			int dif = mp.m1.frame_no - mp.m0.frame_no;
			if (dif <= 1) { // assume that scene is changed
				System.arraycopy(mp.m0.location, 0, m.location, 0, 3);
				System.arraycopy(mp.m0.rotation, 0, m.rotation, 0, 3);
				m.length = mp.m0.length;
				m.view_angle = mp.m0.view_angle;
			} else {
				float a0 = frame - mp.m0.frame_no;
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

		if (qr > 0) {
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

		if (qr < 0) {
			qr = -qr;

			double sp = Math.sqrt(ss);
			double ph = Math.acos(qr);
			double pt = ph * t;
			double t1 = Math.sin(pt) / sp;
			double t0 = Math.sin(ph - pt) / sp;

			if (Double.isNaN(t0) || Double.isNaN(t1)) {
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

			if (Double.isNaN(t0) || Double.isNaN(t1)) {
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
		double xa = ip[ofs] / 256;
		double xb = ip[size * 2 + ofs] / 256;
		double ya = ip[size + ofs] / 256;
		double yb = ip[size * 3 + ofs] / 256;

		double min = 0;
		double max = 1;

		double ct = t;
		while (true) {
			double x11 = xa * ct;
			double x12 = xa + (xb - xa) * ct;
			double x13 = xb + (1 - xb) * ct;

			double x21 = x11 + (x12 - x11) * ct;
			double x22 = x12 + (x13 - x12) * ct;

			double x3 = x21 + (x22 - x21) * ct;

			if (Math.abs(x3 - t) < 0.0001) {
				double y11 = ya * ct;
				double y12 = ya + (yb - ya) * ct;
				double y13 = yb + (1 - yb) * ct;

				double y21 = y11 + (y12 - y11) * ct;
				double y22 = y12 + (y13 - y12) * ct;

				double y3 = y21 + (y22 - y21) * ct;

				return y3;
			} else if (x3 < t) {
				min = ct;
			} else {
				max = ct;
			}
			ct = min * 0.5 + max * 0.5;
		}
	}
	
}
