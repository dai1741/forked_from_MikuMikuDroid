package jp.gauzau.MikuMikuDroidmod;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import android.opengl.Matrix;
import android.opengl.Visibility;

public class SphereArea {
	private class Sphere {
		/*
		public Sphere s1 = null;
		public Sphere s2 = null;
		*/
		
		public ArrayList<Integer> mIdx = null;
		public float mCx = 0;
		public float mCy = 0;
		public float mCz = 0;
		public float mCr = 0;
		public int   mCn = 0;
		public int   mOfs;
		public boolean mComplex = false;
		
		private float[] mPos = new float[3];
		
		public Sphere(int ofs) {
			mOfs = ofs;
		}

		public void set(int idx) {
			if(mIdx == null) {
				mIdx = new ArrayList<Integer>();
			}
			mIdx.add(idx);
			getVertex(idx, mPos);
			mCx += mPos[0];
			mCy += mPos[1];
			mCz += mPos[2];
			mCn++;
		}
		
		public void setComprex(boolean c) {
			mComplex = c;
		}
		
		public void calcR() {
			float mx = mCx / mCn;
			float my = mCy / mCn;
			float mz = mCz / mCn;
			
			for(Integer idx: mIdx) {
				getVertex(idx, mPos);
				float x = mx - mPos[0];
				float y = my - mPos[1];
				float z = mz - mPos[2];
				float d = Matrix.length(x, y, z);
				mCr = Math.max(d, mCr);
			}
		}
		
		private void getVertex(int pos, float[] ver) {
			mVtx.position(pos * 8);
			mVtx.get(ver);
		}
		
		public float distance(Sphere s) {
			float x = mCx / mCn - s.mCx / s.mCn;
			float y = mCy / mCn - s.mCy / s.mCn;
			float z = mCz / mCn - s.mCz / s.mCn;
			return Matrix.length(x, y, z);
		}
		
		public Sphere merge(Sphere s) {
			Sphere sph = new Sphere(mOfs);
			/*
			sph.s1 = this;
			sph.s2 = s;
			*/
			sph.mCx = mCx + s.mCx;
			sph.mCy = mCy + s.mCy;
			sph.mCz = mCz + s.mCz;
			sph.mCn = mCn + s.mCn;
			
			return sph;
		}
		
		public void recycle() {
			mIdx = null;
		}

	};
	
	public class SphereBone {
		private Bone				mCurBone;
		private ArrayList<Sphere>	mSph = new ArrayList<Sphere>();
		private ArrayList<Sphere>	mSphC = new ArrayList<Sphere>();
		private float[]				mSphere = null;
		private int[]				mRes;
		private int[]				mRender;
		
		public SphereBone(Bone bone) {
			mCurBone = bone;
		}
		
		public void add(Sphere s) {
			mSph.add(s);
		}
		
		public void addComplex(Sphere s) {
			mSphC.add(s);
		}
		
		public void calcR() {
			for(Sphere s: mSph) {
				s.calcR();
			}
		}
		
		public void fix() {
			mRes = new int[mSph.size()];
			mRender = new int[(mSph.size() + mSphC.size()) * 2];
			
			int cnt = 0;
			mSphere = new float[mSph.size() * 4];
			for(Sphere s: mSph) {
				s.calcR();
				mSphere[cnt++] = s.mCx / s.mCn;
				mSphere[cnt++] = s.mCy / s.mCn;
				mSphere[cnt++] = s.mCz / s.mCn;
				mSphere[cnt++] = s.mCr;
//				Log.d("SphereArea", String.format("Sphere %f x %f x %f, r = %f, n= %d", s.mCx / s.mCn, s.mCy / s.mCn, s.mCz / s.mCn, s.mCr, s.mCn));
				s.recycle();
			}
			for(Sphere s: mSphC) {
				s.recycle();
			}
		}
		
		public int[] getRenderIndex() {
			return mRender;
		}
		
		public int makeRenderIndex(float[] mat) {
			int n;
			if(mCurBone != null) {
				Matrix.multiplyMM(mMwork, 0, mat, 0, mCurBone.matrix, 0);
				n = Visibility.frustumCullSpheres(mMwork, 0, mSphere, 0, mSph.size(), mRes, 0, mRes.length);				
			} else {
				n = Visibility.frustumCullSpheres(mat, 0, mSphere, 0, mSph.size(), mRes, 0, mRes.length);				
			}
			
			int cnt = 0;
			int nxt = 0;
			if(n > 0) {
				for(int i = 0; i < n; i++) {
					Sphere s = mSph.get(mRes[i]);
					if(i > 0 && s.mOfs == nxt) {
						mRender[(cnt - 1) * 2 + 1] += s.mCn;
					} else {
						mRender[cnt * 2 + 0] = s.mOfs;
						mRender[cnt * 2 + 1] = s.mCn;
						nxt = s.mOfs + s.mCn;
						cnt++;
					}
				}
			}
			
			for(int i = 0; i < mSphC.size(); i++) {
				mRender[(cnt + i) * 2 + 0] = mSphC.get(i).mOfs;
				mRender[(cnt + i) * 2 + 1] = mSphC.get(i).mCn;
			}

			return cnt + mSphC.size();
		}
		
		public void cluster() {
			Sphere s1 = null;
			Sphere s2 = null;
			float md = Float.MAX_VALUE;

			while(mSph.size() > 1) {
				s1 = null;
				s2 = null;
				md = Float.MAX_VALUE;
				for(Sphere st1: mSph) {
					for(Sphere st2: mSph) {
						if(st1 != st2) {
							float d = st1.distance(st2);
							if(d < md) {
								s1 = st1;
								s2 = st2;
							}
						}
					}
				}
				
				mSph.add(s1.merge(s2));
				mSph.remove(s1);
				mSph.remove(s2);
			}
		}
	};
	
	private FloatBuffer					mVtx;
	private ByteBuffer					mWeight;
	private ArrayList<Bone>				mBone;
	private HashMap<Bone, SphereBone>	mSphB = new HashMap<Bone, SphereBone>();
	private SphereBone[]				mSphV;
	private float[]						mMwork = new float[16];
	
	public SphereArea(FloatBuffer v, ByteBuffer w, ArrayList<Bone> b) {
		mVtx  = v;
		mWeight = w;
		mBone = b;
	}

	public int initialSet(IntBuffer idx, int pos, int size) {
		Bone b = getBone(idx.get(pos));
		SphereBone sb = mSphB.get(b);
		if(sb == null) {
			sb = new SphereBone(b);
			mSphB.put(b, sb);
		}
		
		int i;
		Sphere s = new Sphere(pos);
		for(i = 0; i < size; i += 3) {
			int idx_pos = idx.get(pos + i);
			Bone bc = getBone(idx_pos);
			if(bc == b || s.mComplex) {
				addVertex(s, idx, pos + i    , b);
				addVertex(s, idx, pos + i + 1, b);
				addVertex(s, idx, pos + i + 2, b);
			} else {
				break;
			}
		}
		
		if(s.mComplex) {
			sb.addComplex(s);
		} else {
			sb.add(s);			
		}
		
		return i;
	}
	
	public int initialSet(ShortBuffer idx, int pos, int size) {
		Bone b = getBone(0x0000ffff & (int)idx.get(pos));
		SphereBone sb = mSphB.get(b);
		if(sb == null) {
			sb = new SphereBone(b);
			mSphB.put(b, sb);
		}
		
		int i;
		Sphere s = new Sphere(pos);
		for(i = 0; i < size; i += 3) {
			int idx_pos = (0x0000ffff & (int)idx.get(pos + i));
			Bone bc = getBone(idx_pos);
			if(bc == b || s.mComplex) {
				addVertex(s, idx, pos + i    , b);
				addVertex(s, idx, pos + i + 1, b);
				addVertex(s, idx, pos + i + 2, b);
			} else {
				break;
			}
		}
		
		if(s.mComplex) {
			sb.addComplex(s);
		} else {
			sb.add(s);			
		}
		
		return i;
	}

	private void addVertex(Sphere s, IntBuffer idxA, int idx, Bone b) {
		int idx_pos = idxA.get(idx);
		Bone ba = getBone(idx_pos);
		if(ba != b) {
			s.setComprex(true);
		}
		s.set(idx_pos);
	}
	
	private void addVertex(Sphere s, ShortBuffer idxA, int idx, Bone b) {
		int idx_pos = (0x0000ffff & (int)idxA.get(idx));
		Bone ba = getBone(idx_pos);
		if(ba != b) {
			s.setComprex(true);
		}
		s.set(idx_pos);
	}
	
	private Bone getBone(int pos) {
		if(mWeight != null) {
			return mBone.get(mWeight.get(pos * 3));
		} else {
			return mBone.get(0);
		}
	}
	
	public SphereBone[] getSphereBone() {
		return mSphV;
	}
	
	public void recycle() {
		mSphV = new SphereBone[mSphB.size()];
		int i = 0;
		for(Entry<Bone, SphereBone> s: mSphB.entrySet()) {
			s.getValue().fix();
			mSphV[i++] = s.getValue();
		}
		mVtx	= null;
		mBone	= null;
		mSphB	= null;
	}

	
	public void logOutput() {
//		Log.d("SphereArea", String.format("root node %d", mSph.size()));
	}
}
