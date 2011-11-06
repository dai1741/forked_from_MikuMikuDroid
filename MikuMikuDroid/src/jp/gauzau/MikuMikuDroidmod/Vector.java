package jp.gauzau.MikuMikuDroidmod;

import android.opengl.Matrix;

public class Vector {
	
	private static float[] mInvSrcs = new float[16];
	private static float[] mInvTmps = new float[12];
	private static float[] mInvDsts = new float[16];

	public static void add(float[] d, float[] v1, float[] v2) {
		for(int i = 0; i < d.length; i++) {
			d[i] = v1[i] + v2[i];
		}
	}
	
	public static void sub(float[] d, float[] v1, float[] v2) {
		for(int i = 0; i < d.length; i++) {
			d[i] = v1[i] - v2[i];
		}
	}
	
	public static void cross(float[] d, float[] v1, float[] v2) {
		d[0] = v1[1] * v2[2] - v1[2] * v2[1];
		d[1] = v1[2] * v2[0] - v1[0] * v2[2];
		d[2] = v1[0] * v2[1] - v1[1] * v2[0];
	}

	public static void normalize(float[] v) {
		float d = Matrix.length(v[0], v[1], v[2]);
		v[0] /= d;
		v[1] /= d;
		v[2] /= d;
	}

	public static float dot(float[] v1, float[] v2) {
		return v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
	}
	
	public static void min(float[] v, float min) {
		for(int i = 0; i < v.length; i++) {
			v[i] = v[i] < min ? v[i] : min;
		}
	}

	public static boolean invertM(float[] mInv, int mInvOffset, float[] m, int mOffset) {
		// Invert a 4 x 4 matrix using Cramer's Rule
	
		// transpose matrix
		Matrix.transposeM(Vector.mInvSrcs, 0, m, mOffset);
	
		// calculate pairs for first 8 elements (cofactors)
		Vector.mInvTmps[0] = Vector.mInvSrcs[10] * Vector.mInvSrcs[15];
		Vector.mInvTmps[1] = Vector.mInvSrcs[11] * Vector.mInvSrcs[14];
		Vector.mInvTmps[2] = Vector.mInvSrcs[9] * Vector.mInvSrcs[15];
		Vector.mInvTmps[3] = Vector.mInvSrcs[11] * Vector.mInvSrcs[13];
		Vector.mInvTmps[4] = Vector.mInvSrcs[9] * Vector.mInvSrcs[14];
		Vector.mInvTmps[5] = Vector.mInvSrcs[10] * Vector.mInvSrcs[13];
		Vector.mInvTmps[6] = Vector.mInvSrcs[8] * Vector.mInvSrcs[15];
		Vector.mInvTmps[7] = Vector.mInvSrcs[11] * Vector.mInvSrcs[12];
		Vector.mInvTmps[8] = Vector.mInvSrcs[8] * Vector.mInvSrcs[14];
		Vector.mInvTmps[9] = Vector.mInvSrcs[10] * Vector.mInvSrcs[12];
		Vector.mInvTmps[10] = Vector.mInvSrcs[8] * Vector.mInvSrcs[13];
		Vector.mInvTmps[11] = Vector.mInvSrcs[9] * Vector.mInvSrcs[12];
	
		// calculate first 8 elements (cofactors)
		Vector.mInvDsts[0] = Vector.mInvTmps[0] * Vector.mInvSrcs[5] + Vector.mInvTmps[3] * Vector.mInvSrcs[6] + Vector.mInvTmps[4] * Vector.mInvSrcs[7];
		Vector.mInvDsts[0] -= Vector.mInvTmps[1] * Vector.mInvSrcs[5] + Vector.mInvTmps[2] * Vector.mInvSrcs[6] + Vector.mInvTmps[5] * Vector.mInvSrcs[7];
		Vector.mInvDsts[1] = Vector.mInvTmps[1] * Vector.mInvSrcs[4] + Vector.mInvTmps[6] * Vector.mInvSrcs[6] + Vector.mInvTmps[9] * Vector.mInvSrcs[7];
		Vector.mInvDsts[1] -= Vector.mInvTmps[0] * Vector.mInvSrcs[4] + Vector.mInvTmps[7] * Vector.mInvSrcs[6] + Vector.mInvTmps[8] * Vector.mInvSrcs[7];
		Vector.mInvDsts[2] = Vector.mInvTmps[2] * Vector.mInvSrcs[4] + Vector.mInvTmps[7] * Vector.mInvSrcs[5] + Vector.mInvTmps[10] * Vector.mInvSrcs[7];
		Vector.mInvDsts[2] -= Vector.mInvTmps[3] * Vector.mInvSrcs[4] + Vector.mInvTmps[6] * Vector.mInvSrcs[5] + Vector.mInvTmps[11] * Vector.mInvSrcs[7];
		Vector.mInvDsts[3] = Vector.mInvTmps[5] * Vector.mInvSrcs[4] + Vector.mInvTmps[8] * Vector.mInvSrcs[5] + Vector.mInvTmps[11] * Vector.mInvSrcs[6];
		Vector.mInvDsts[3] -= Vector.mInvTmps[4] * Vector.mInvSrcs[4] + Vector.mInvTmps[9] * Vector.mInvSrcs[5] + Vector.mInvTmps[10] * Vector.mInvSrcs[6];
		Vector.mInvDsts[4] = Vector.mInvTmps[1] * Vector.mInvSrcs[1] + Vector.mInvTmps[2] * Vector.mInvSrcs[2] + Vector.mInvTmps[5] * Vector.mInvSrcs[3];
		Vector.mInvDsts[4] -= Vector.mInvTmps[0] * Vector.mInvSrcs[1] + Vector.mInvTmps[3] * Vector.mInvSrcs[2] + Vector.mInvTmps[4] * Vector.mInvSrcs[3];
		Vector.mInvDsts[5] = Vector.mInvTmps[0] * Vector.mInvSrcs[0] + Vector.mInvTmps[7] * Vector.mInvSrcs[2] + Vector.mInvTmps[8] * Vector.mInvSrcs[3];
		Vector.mInvDsts[5] -= Vector.mInvTmps[1] * Vector.mInvSrcs[0] + Vector.mInvTmps[6] * Vector.mInvSrcs[2] + Vector.mInvTmps[9] * Vector.mInvSrcs[3];
		Vector.mInvDsts[6] = Vector.mInvTmps[3] * Vector.mInvSrcs[0] + Vector.mInvTmps[6] * Vector.mInvSrcs[1] + Vector.mInvTmps[11] * Vector.mInvSrcs[3];
		Vector.mInvDsts[6] -= Vector.mInvTmps[2] * Vector.mInvSrcs[0] + Vector.mInvTmps[7] * Vector.mInvSrcs[1] + Vector.mInvTmps[10] * Vector.mInvSrcs[3];
		Vector.mInvDsts[7] = Vector.mInvTmps[4] * Vector.mInvSrcs[0] + Vector.mInvTmps[9] * Vector.mInvSrcs[1] + Vector.mInvTmps[10] * Vector.mInvSrcs[2];
		Vector.mInvDsts[7] -= Vector.mInvTmps[5] * Vector.mInvSrcs[0] + Vector.mInvTmps[8] * Vector.mInvSrcs[1] + Vector.mInvTmps[11] * Vector.mInvSrcs[2];
	
		// calculate pairs for second 8 elements (cofactors)
		Vector.mInvTmps[0] = Vector.mInvSrcs[2] * Vector.mInvSrcs[7];
		Vector.mInvTmps[1] = Vector.mInvSrcs[3] * Vector.mInvSrcs[6];
		Vector.mInvTmps[2] = Vector.mInvSrcs[1] * Vector.mInvSrcs[7];
		Vector.mInvTmps[3] = Vector.mInvSrcs[3] * Vector.mInvSrcs[5];
		Vector.mInvTmps[4] = Vector.mInvSrcs[1] * Vector.mInvSrcs[6];
		Vector.mInvTmps[5] = Vector.mInvSrcs[2] * Vector.mInvSrcs[5];
		Vector.mInvTmps[6] = Vector.mInvSrcs[0] * Vector.mInvSrcs[7];
		Vector.mInvTmps[7] = Vector.mInvSrcs[3] * Vector.mInvSrcs[4];
		Vector.mInvTmps[8] = Vector.mInvSrcs[0] * Vector.mInvSrcs[6];
		Vector.mInvTmps[9] = Vector.mInvSrcs[2] * Vector.mInvSrcs[4];
		Vector.mInvTmps[10] = Vector.mInvSrcs[0] * Vector.mInvSrcs[5];
		Vector.mInvTmps[11] = Vector.mInvSrcs[1] * Vector.mInvSrcs[4];
	
		// calculate second 8 elements (cofactors)
		Vector.mInvDsts[8] = Vector.mInvTmps[0] * Vector.mInvSrcs[13] + Vector.mInvTmps[3] * Vector.mInvSrcs[14] + Vector.mInvTmps[4] * Vector.mInvSrcs[15];
		Vector.mInvDsts[8] -= Vector.mInvTmps[1] * Vector.mInvSrcs[13] + Vector.mInvTmps[2] * Vector.mInvSrcs[14] + Vector.mInvTmps[5] * Vector.mInvSrcs[15];
		Vector.mInvDsts[9] = Vector.mInvTmps[1] * Vector.mInvSrcs[12] + Vector.mInvTmps[6] * Vector.mInvSrcs[14] + Vector.mInvTmps[9] * Vector.mInvSrcs[15];
		Vector.mInvDsts[9] -= Vector.mInvTmps[0] * Vector.mInvSrcs[12] + Vector.mInvTmps[7] * Vector.mInvSrcs[14] + Vector.mInvTmps[8] * Vector.mInvSrcs[15];
		Vector.mInvDsts[10] = Vector.mInvTmps[2] * Vector.mInvSrcs[12] + Vector.mInvTmps[7] * Vector.mInvSrcs[13] + Vector.mInvTmps[10] * Vector.mInvSrcs[15];
		Vector.mInvDsts[10] -= Vector.mInvTmps[3] * Vector.mInvSrcs[12] + Vector.mInvTmps[6] * Vector.mInvSrcs[13] + Vector.mInvTmps[11] * Vector.mInvSrcs[15];
		Vector.mInvDsts[11] = Vector.mInvTmps[5] * Vector.mInvSrcs[12] + Vector.mInvTmps[8] * Vector.mInvSrcs[13] + Vector.mInvTmps[11] * Vector.mInvSrcs[14];
		Vector.mInvDsts[11] -= Vector.mInvTmps[4] * Vector.mInvSrcs[12] + Vector.mInvTmps[9] * Vector.mInvSrcs[13] + Vector.mInvTmps[10] * Vector.mInvSrcs[14];
		Vector.mInvDsts[12] = Vector.mInvTmps[2] * Vector.mInvSrcs[10] + Vector.mInvTmps[5] * Vector.mInvSrcs[11] + Vector.mInvTmps[1] * Vector.mInvSrcs[9];
		Vector.mInvDsts[12] -= Vector.mInvTmps[4] * Vector.mInvSrcs[11] + Vector.mInvTmps[0] * Vector.mInvSrcs[9] + Vector.mInvTmps[3] * Vector.mInvSrcs[10];
		Vector.mInvDsts[13] = Vector.mInvTmps[8] * Vector.mInvSrcs[11] + Vector.mInvTmps[0] * Vector.mInvSrcs[8] + Vector.mInvTmps[7] * Vector.mInvSrcs[10];
		Vector.mInvDsts[13] -= Vector.mInvTmps[6] * Vector.mInvSrcs[10] + Vector.mInvTmps[9] * Vector.mInvSrcs[11] + Vector.mInvTmps[1] * Vector.mInvSrcs[8];
		Vector.mInvDsts[14] = Vector.mInvTmps[6] * Vector.mInvSrcs[9] + Vector.mInvTmps[11] * Vector.mInvSrcs[11] + Vector.mInvTmps[3] * Vector.mInvSrcs[8];
		Vector.mInvDsts[14] -= Vector.mInvTmps[10] * Vector.mInvSrcs[11] + Vector.mInvTmps[2] * Vector.mInvSrcs[8] + Vector.mInvTmps[7] * Vector.mInvSrcs[9];
		Vector.mInvDsts[15] = Vector.mInvTmps[10] * Vector.mInvSrcs[10] + Vector.mInvTmps[4] * Vector.mInvSrcs[8] + Vector.mInvTmps[9] * Vector.mInvSrcs[9];
		Vector.mInvDsts[15] -= Vector.mInvTmps[8] * Vector.mInvSrcs[9] + Vector.mInvTmps[11] * Vector.mInvSrcs[10] + Vector.mInvTmps[5] * Vector.mInvSrcs[8];
	
		// calculate determinant
		float det = Vector.mInvSrcs[0] * Vector.mInvDsts[0] + Vector.mInvSrcs[1] * Vector.mInvDsts[1] + Vector.mInvSrcs[2] * Vector.mInvDsts[2] + Vector.mInvSrcs[3] * Vector.mInvDsts[3];
	
		// calculate matrix inverse
		det = 1 / det;
		for (int j = 0; j < 16; j++)
			mInv[j + mInvOffset] = Vector.mInvDsts[j] * det;
	
		return true;
	}

}
