package jp.gauzau.MikuMikuDroid;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class MikuMotion implements Serializable {
	private static final long serialVersionUID = -6980450493306185580L;
	
	//	private static final long serialVersionUID = -7581376490687593200L;
	public  transient String mFileName;
	private transient int mMaxFrame;
	private transient HashMap<String, MotionIndexA> mMotion;
	private transient HashMap<String, FaceIndexA> mFace;
	private transient ArrayList<CameraIndex> mCamera;
	private transient int mCameraCurrent;

	private transient HashMap<String, MotionIndexA> mIKMotion;

	public MikuMotion(VMDParser vmd) {
		mCameraCurrent	= 0;
		mIKMotion		= null;
		attachVMD(vmd);
	}
	
	public void attachVMD(VMDParser vmd) {
		mFileName       = vmd.getFileName();
		
		mMotion = new HashMap<String, MotionIndexA>();
		for(Entry<String, ArrayList<MotionIndex>> m: vmd.getMotion().entrySet()) {
			mMotion.put(m.getKey(), toMotionIndexA(m.getValue()));
		}
		
		mFace = new HashMap<String, FaceIndexA>();
		for(Entry<String, ArrayList<FaceIndex>> f: vmd.getFace().entrySet()) {
			mFace.put(f.getKey(), toFaceIndexA(f.getValue()));
		}
		mCamera			= vmd.getCamera();
		mMaxFrame		= vmd.maxFrame();		
	}
	
	public void attachModel(ArrayList<Bone> ba, ArrayList<Face> fa) {
		if(ba != null && mMotion != null) {
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
		}
		
		if(fa != null && mFace != null) {
			for(Face f: fa) {
				f.motion = mFace.get(f.name);
				f.current_motion = 0;
			}
		}
	}
	
	public static MotionIndexA toMotionIndexA(ArrayList<MotionIndex> m) {
		if(m != null) {
			boolean nullp = m.get(1).interp == null;
			MotionIndexA mia = new MotionIndexA(m.size(), !nullp);
			
			for(int i = 0; i < m.size(); i++) {
				MotionIndex mi = m.get(i);
				mia.frame_no[i] = mi.frame_no;
				System.arraycopy(mi.location, 0, mia.location, i*3, 3);
				System.arraycopy(mi.rotation, 0, mia.rotation, i*4, 4);
				if(mia.interp != null) {
					if(mi.interp != null) {
						System.arraycopy(mi.interp, 0, mia.interp, i*16, 16);
					} else {
						mia.interp[i * 16] = -1;	// magic number indicates null
					}
				}
			}
			
			return mia;			
		} else {
			return null;
		}
	}
	
	public static FaceIndexA toFaceIndexA(ArrayList<FaceIndex> m) {
		if(m != null) {
			FaceIndexA mia = new FaceIndexA(m.size());
			
			for(int i = 0; i < m.size(); i++) {
				FaceIndex mi = m.get(i);
				mia.frame_no[i] = mi.frame_no;
				mia.weight[i]   = mi.weight;
			}
			
			return mia;			
		} else {
			return null;
		}
	}
	

	public int maxFrame() {
		return mMaxFrame;
	}
	
	public HashMap<String, MotionIndexA> getMotion() {
		return mMotion;
	}
	
	public HashMap<String, FaceIndexA> getFace() {
		return mFace;
	}
	
	public HashMap<String, MotionIndexA> getIKMotion() {
		return mIKMotion;
	}
	
	public void setIKMotion(HashMap<String, ArrayList<MotionIndex>> m) {
		mIKMotion = new HashMap<String, MotionIndexA>();
		for(Entry<String, ArrayList<MotionIndex>> mi: m.entrySet()) {
			mIKMotion.put(mi.getKey(), toMotionIndexA(mi.getValue()));
		}
//		mIKMotion = toMotionIndexA(m);
	}
	
	public MotionPair findMotion(Bone b, float frame, MotionPair mp) {
		if (b != null && b.motion != null) {
			int[] frame_no = b.motion.frame_no;
			mp.m0 = 0;
			mp.m1 = b.motion.frame_no.length - 1;
			if(frame >= frame_no[mp.m1]) {
				mp.m0 = mp.m1;
				b.current_motion = mp.m1;
				mp.m1 = -1;
				return mp;
			}

			while(true) {
				int center = (mp.m0 + mp.m1) / 2;
				if(center == mp.m0) {
					b.current_motion = center;
					return mp;
				}
				if(frame_no[center] == frame) {
					mp.m0 = center;
					mp.m1 = -1;
					b.current_motion = center;
					return mp;
				} else if(frame_no[center] > frame) {
					mp.m1 = center;
				} else {
					mp.m0 = center;
				}
			}
		}
		return null;
	}

	public Motion interpolateLinear(MotionPair mp, MotionIndexA mi, float frame, Motion m) {
		if (mp == null) {
			return null;
		} else if (mp.m1 == -1) {
			System.arraycopy(mi.location, mp.m0 * 3, m.location, 0, 3);
			System.arraycopy(mi.rotation, mp.m0 * 4, m.rotation, 0, 4);
			return m;
		} else {
			int dif = mi.frame_no[mp.m1] - mi.frame_no[mp.m0];
			float a0 = frame - mi.frame_no[mp.m0];
			float ratio = a0 / dif;

			if (mi.interp == null || mi.interp[mp.m0 * 16] == -1) { // calcurated in preCalcIK
				float t = ratio;
				m.location[0] = mi.location[mp.m0 * 3 + 0] + (mi.location[mp.m1 * 3 + 0] - mi.location[mp.m0 * 3 + 0]) * t;
				m.location[1] = mi.location[mp.m0 * 3 + 1] + (mi.location[mp.m1 * 3 + 1] - mi.location[mp.m0 * 3 + 1]) * t;
				m.location[2] = mi.location[mp.m0 * 3 + 2] + (mi.location[mp.m1 * 3 + 2] - mi.location[mp.m0 * 3 + 2]) * t;
				slerp(m.rotation, mi.rotation, mi.rotation, mp.m0 * 4, mp.m1 * 4, t);
			} else {
				double t = bazier(mi.interp, mp.m0 * 16, 4, ratio);
				m.location[0] = (float) (mi.location[mp.m0 * 3 + 0] + (mi.location[mp.m1 * 3 + 0] - mi.location[mp.m0 * 3 + 0]) * t);
				t = bazier(mi.interp, mp.m0 * 16 + 1, 4, ratio);
				m.location[1] = (float) (mi.location[mp.m0 * 3 + 1] + (mi.location[mp.m1 * 3 + 1] - mi.location[mp.m0 * 3 + 1]) * t);
				t = bazier(mi.interp, mp.m0 * 16 + 2, 4, ratio);
				m.location[2] = (float) (mi.location[mp.m0 * 3 + 2] + (mi.location[mp.m1 * 3 + 2] - mi.location[mp.m0 * 3 + 2]) * t);

				slerp(m.rotation, mi.rotation, mi.rotation, mp.m0 * 4, mp.m1 * 4, bazier(mi.interp, mp.m0 * 16 + 3, 4, ratio));
			}

			return m;
		}
	}

	public FacePair findFace(Face b, float frame, FacePair mp) {
		if (b != null && b.motion != null) {
			int[] frame_no = b.motion.frame_no;
			mp.m0 = 0;
			mp.m1 = b.motion.frame_no.length - 1;
			if(frame >= frame_no[mp.m1]) {
				mp.m0 = mp.m1;
				b.current_motion = mp.m1;
				mp.m1 = -1;
				return mp;
			}

			while(true) {
				int center = (mp.m0 + mp.m1) / 2;
				if(center == mp.m0) {
					b.current_motion = center;
					return mp;
				}
				if(frame_no[center] == frame) {
					mp.m0 = center;
					mp.m1 = -1;
					b.current_motion = center;
					return mp;
				} else if(frame_no[center] > frame) {
					mp.m1 = center;
				} else {
					mp.m0 = center;
				}
			}
		}
		return null;
	}


	public FaceIndex interpolateLinear(FacePair mp, FaceIndexA mi, float frame, FaceIndex m) {
		if (mp == null) {
			return null;
		} else if (mp.m1 == -1) {
			m.frame_no = mi.frame_no[mp.m0];
			m.weight   = mi.weight[mp.m0];
			return m;
		} else {
			int dif = mi.frame_no[mp.m1] - mi.frame_no[mp.m0];
			float a0 = frame - mi.frame_no[mp.m0];
			float ratio = a0 / dif;

			m.weight = mi.weight[mp.m0] + (mi.weight[mp.m1] - mi.weight[mp.m0]) * ratio;

			return m;
		}
	}


	public CameraPair findCamera(float frame, CameraPair mp) {
		if (mCamera != null) {
			int m0 = 0;
			int m1 = mCamera.size() - 1;
			mp.m0 = mCamera.get(m0);
			mp.m1 = mCamera.get(m1);
			if(frame >= mp.m1.frame_no) {
				mp.m0 = mp.m1;
				mp.m1 = null;
				mCameraCurrent = m1;
				return mp;
			}

			while(true) {
				int center = (m0 + m1) / 2;
				if(center == m0) {
					mCameraCurrent = center;
					return mp;
				}
				CameraIndex m = mCamera.get(center);
				if(m.frame_no == frame) {
					mp.m0 = m;
					mp.m1 = null;
					mCameraCurrent = center;
					return mp;
				} else if(m.frame_no > frame) {
					mp.m1 = m;
					m1 = center;
				} else {
					mp.m0 = m;
					m0 = center;
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
	
	private void lerp(float p[], float q[], float r[], int m0, int m1, float t) {
		double qr = q[m0 + 0] * r[m1 + 0] + q[m0 + 1] * r[m1 + 1] + q[m0 + 2] * r[m1 + 2] + q[m0 + 3] * r[m1 + 3];
		double ss = 1.0 - t;

		if (qr > 0) {
			p[0] = (float) (q[m0 + 0] * ss + r[m1 + 0] * t);
			p[1] = (float) (q[m0 + 1] * ss + r[m1 + 1] * t);
			p[2] = (float) (q[m0 + 2] * ss + r[m1 + 2] * t);
			p[3] = (float) (q[m0 + 3] * ss + r[m1 + 3] * t);
		} else {
			p[0] = (float) (q[m0 + 0] * ss - r[m1 + 0] * t);
			p[1] = (float) (q[m0 + 1] * ss - r[m1 + 1] * t);
			p[2] = (float) (q[m0 + 2] * ss - r[m1 + 2] * t);
			p[3] = (float) (q[m0 + 3] * ss - r[m1 + 3] * t);

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
	
	private void slerp(float p[], float[] q, float[] r, int m0, int m1, double t) {
		double qr = q[m0 + 0] * r[m1 + 0] + q[m0 + 1] * r[m1 + 1] + q[m0 + 2] * r[m1 + 2] + q[m0 + 3] * r[m1 + 3];
		double ss = 1.0 - qr * qr;

		if (qr < 0) {
			qr = -qr;

			double sp = Math.sqrt(ss);
			double ph = Math.acos(qr);
			double pt = ph * t;
			double t1 = Math.sin(pt) / sp;
			double t0 = Math.sin(ph - pt) / sp;

			if (Double.isNaN(t0) || Double.isNaN(t1)) {
				p[0] = q[m0 + 0];
				p[1] = q[m0 + 1];
				p[2] = q[m0 + 2];
				p[3] = q[m0 + 3];
			} else {
				p[0] = (float) (q[m0 + 0] * t0 - r[m1 + 0] * t1);
				p[1] = (float) (q[m0 + 1] * t0 - r[m1 + 1] * t1);
				p[2] = (float) (q[m0 + 2] * t0 - r[m1 + 2] * t1);
				p[3] = (float) (q[m0 + 3] * t0 - r[m1 + 3] * t1);
			}

		} else {
			double sp = Math.sqrt(ss);
			double ph = Math.acos(qr);
			double pt = ph * t;
			double t1 = Math.sin(pt) / sp;
			double t0 = Math.sin(ph - pt) / sp;

			if (Double.isNaN(t0) || Double.isNaN(t1)) {
				p[0] = q[m0 + 0];
				p[1] = q[m0 + 1];
				p[2] = q[m0 + 2];
				p[3] = q[m0 + 3];
			} else {
				p[0] = (float) (q[m0 + 0] * t0 + r[m1 + 0] * t1);
				p[1] = (float) (q[m0 + 1] * t0 + r[m1 + 1] * t1);
				p[2] = (float) (q[m0 + 2] * t0 + r[m1 + 2] * t1);
				p[3] = (float) (q[m0 + 3] * t0 + r[m1 + 3] * t1);
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
	
	private void writeObject(ObjectOutputStream os) throws IOException {
		os.defaultWriteObject();
		if(mIKMotion == null) {
			os.writeInt(0);
		} else {
			os.writeInt(mIKMotion.size());
			for(Entry<String, MotionIndexA> i: mIKMotion.entrySet()) {
				os.writeUTF(i.getKey());
				i.getValue().write(os);
			}			
		}
	}

	private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
		is.defaultReadObject();
		int hsize = is.readInt();
		if(hsize == 0) {
			mIKMotion = null;
		} else {
			mIKMotion = new HashMap<String, MotionIndexA>(hsize);
			for(int i = 0; i < hsize; i++) {
				String name = is.readUTF();
				MotionIndexA mi = new MotionIndexA();
				mi.read(is);
				mIKMotion.put(name, mi);
			}			
		}
	}
}
