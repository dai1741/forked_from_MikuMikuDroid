package jp.gauzau.MikuMikuDroid;

import java.util.ArrayList;

import android.opengl.Matrix;
import android.opengl.Visibility;

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

		public void set(int idx) {
			if(mIdx == null) {
				mIdx = new ArrayList<Integer>();
			}
			mIdx.add(idx);
			Vertex v = mVtx.get(idx);
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
		public int					mBone;
		public ArrayList<Sphere>	mSph = new ArrayList<Sphere>();
		float[]						mSphIdx = null;
	};
	
	ArrayList<Vertex>	mVtx;
	ArrayList<Sphere>	mSph = new ArrayList<Sphere>();
	float[] mSphere = null;
	private int[] mRes;
	
	public SphereArea(ArrayList<Vertex> v) {
		mVtx = v;
	}

	public void initialSet(ArrayList<Integer> idx, int pos, int size) {
		Sphere s = new Sphere();
		for(int i = 0; i < size; i++) {
			s.set(idx.get(pos + i));
		}
		s.calcR();
//		Log.d("SphereArea", String.format("Sphere %f x %f x %f, r = %f", s.mCx / s.mCn, s.mCy / s.mCn, s.mCz / s.mCn, s.mCr));
		mSph.add(s);
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
	
	public int getRenderIndex(float[] mat, int[] rendar) {
		if(mRes == null) {
			mRes = new int[mSph.size()];
		}
		int n = Visibility.frustumCullSpheres(mat, 0, getSphere(), 0, mSph.size(), mRes, 0, mRes.length);
		int cnt = 0;
		if(n > 0) {
			int ofs = 0;
			for(int i = 0; i < n; i++) {
				ofs = 0;
				for(int j = 0; j < mRes[i]; j ++) {
					ofs += mSph.get(j).mCn;
				}
				if(i > 0 && mRes[i - 1] + 1 == mRes[i]) {
					rendar[(cnt - 1) * 2 + 1] += mSph.get(mRes[i]).mCn;
				} else {
					rendar[cnt * 2 + 0] = ofs;
					rendar[cnt * 2 + 1] = mSph.get(mRes[i]).mCn;
					cnt++;
				}
				/*
				rendar[cnt * 2 + 0] = ofs;
				rendar[cnt * 2 + 1] = mSph.get(mRes[i]).mCn;
				cnt++;
				*/
			}
			/*
			Arrays.sort(mRes);
			int cnt = 0;
			for(int i = 0; i < mRes[n - 1]; i++) {
				if(mRes[cnt] == i) {
					rendar[cnt * 2 + 0] = ofs;
					rendar[cnt * 2 + 1] = mSph.get(i).mCn;
					cnt++;
				}
				ofs += mSph.get(i).mCn;
			}
			*/
		}

		return cnt;
	}
	
	public void recycle() {
		for(Sphere s: mSph) {
			s.recycle();
		}
		mVtx = null;
	}
	
	private float[] getSphere() {
		if(mSphere == null) {
			int cnt = 0;
			mSphere = new float[mSph.size() * 4];
			for(Sphere sph: mSph) {
				mSphere[cnt++] = sph.mCx / sph.mCn;
				mSphere[cnt++] = sph.mCy / sph.mCn;
				mSphere[cnt++] = sph.mCz / sph.mCn;
				mSphere[cnt++] = sph.mCr;
			}
		}
		return mSphere;
	}
	
	public void logOutput() {
//		Log.d("SphereArea", String.format("root node %d", mSph.size()));
	}
}
