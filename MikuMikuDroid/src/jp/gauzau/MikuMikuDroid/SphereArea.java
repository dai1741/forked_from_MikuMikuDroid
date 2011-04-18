package jp.gauzau.MikuMikuDroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import android.opengl.Matrix;
import android.opengl.Visibility;
import android.util.Log;

public class SphereArea {
	private class Sphere {
		public Sphere s1 = null;
		public Sphere s2 = null;
		
		public ArrayList<Integer> mIdx = null;
		public float mCx = 0;
		public float mCy = 0;
		public float mCz = 0;
		public float mCr = 0;
		public int   mCn = 0;

		public void set(int idx, Vertex v) {
			if(mIdx == null) {
				mIdx = new ArrayList<Integer>();
			}
			mIdx.add(idx);
			mCx += v.pos[0];
			mCy += v.pos[1];
			mCz += v.pos[2];
			mCn++;
		}
		
		public void calcR() {
			float mx = mCx / mCn;
			float my = mCy / mCn;
			float mz = mCz / mCn;
			
			for(Integer idx: mIdx) {
				Vertex v = mVtx.get(idx);
				float x = mx - v.pos[0];
				float y = my - v.pos[1];
				float z = mz - v.pos[2];
				float d = Matrix.length(x, y, z);
				mCr = Math.max(d, mCr);
			}
		}
		
		public float distance(Sphere s) {
			float x = mCx / mCn - s.mCx / s.mCn;
			float y = mCy / mCn - s.mCy / s.mCn;
			float z = mCz / mCn - s.mCz / s.mCn;
			return Matrix.length(x, y, z);
		}
		
		public Sphere merge(Sphere s) {
			Sphere sph = new Sphere();
			sph.s1 = this;
			sph.s2 = s;
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
		private float[]				mSphere = null;
		private int[]				mRes;
		private int[]				mRender;
		
		public SphereBone(Bone bone) {
			mCurBone = bone;
		}
		
		public void add(Sphere s) {
			mSph.add(s);
		}
		
		public void calcR() {
			for(Sphere s: mSph) {
				s.calcR();
			}
		}
		
		public void fix() {
			mRes = new int[mSph.size()];
			mRender = new int[mSph.size() * 2];
			
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
			if(n > 0) {
				int ofs = 0;
				for(int i = 0; i < n; i++) {
					ofs = 0;
					for(int j = 0; j < mRes[i]; j ++) {
						ofs += mSph.get(j).mCn;
					}
					if(i > 0 && mRes[i - 1] + 1 == mRes[i]) {
						mRender[(cnt - 1) * 2 + 1] += mSph.get(mRes[i]).mCn;
					} else {
						mRender[cnt * 2 + 0] = ofs;
						mRender[cnt * 2 + 1] = mSph.get(mRes[i]).mCn;
						cnt++;
					}
				}
			}

			return cnt;
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
	
	private ArrayList<Vertex>			mVtx;
	private ArrayList<Bone>				mBone;
	private HashMap<Bone, SphereBone>	mSphB = new HashMap<Bone, SphereBone>();
	private SphereBone[]				mSphV;
	private float[]						mMwork = new float[16];
	
	public SphereArea(ArrayList<Vertex> v, ArrayList<Bone> b) {
		mVtx  = v;
		mBone = b;
	}

	public int initialSet(ArrayList<Integer> idx, int pos, int size) {
		Bone b = mBone.get(mVtx.get(idx.get(pos)).bone_num_0);
		SphereBone sb = mSphB.get(b);
		if(sb == null) {
			sb = new SphereBone(b);
			mSphB.put(b, sb);
		}
		
		int i;
		Sphere s = new Sphere();
		for(i = 0; i < size; i++) {
			int idx_pos = idx.get(pos + i);
			Vertex v = mVtx.get(idx_pos);
			Bone bc = mBone.get(v.bone_num_0);
			if(bc == b) {
				s.set(idx_pos, v);
			} else {
				i++;
				break;
			}
		}
		sb.add(s);
		
		return i;
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
