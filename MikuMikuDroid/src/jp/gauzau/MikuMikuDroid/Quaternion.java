package jp.gauzau.MikuMikuDroid;

public class Quaternion {
	
	private static double[] mQuatwork = new double[4];

	static public void createFromAngleAxis(double[] r, double angle, float[] axis) {
		double halfAngle = 0.5f * angle;
		double sin = Math.sin(halfAngle);
		r[3] = Math.cos(halfAngle);
		r[0] = sin * axis[0];
		r[1] = sin * axis[1];
		r[2] = sin * axis[2];
	}

	static public void setIndentity(double[] r) {
		r[0] = r[1] = r[2] = 0;
		r[3] = 1;
	}

	static public void mul(double[] res, double[] r, double[] q) {
		double w = r[3], x = r[0], y = r[1], z = r[2];
		double qw = q[3], qx = q[0], qy = q[1], qz = q[2];
		res[0] = x * qw + y * qz - z * qy + w * qx;
		res[1] = -x * qz + y * qw + z * qx + w * qy;
		res[2] = x * qy - y * qx + z * qw + w * qz;
		res[3] = -x * qx - y * qy - z * qz + w * qw;
	}

	static public void normalize(double[] res, double[] q) {
		float scale = (float) (1.0f / Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]));
	
		res[0] = q[0] * scale;
		res[1] = q[1] * scale;
		res[2] = q[2] * scale;
		res[3] = q[3] * scale;
	}

	static public void scale(double[] res, double scale) {
		double angle = Math.acos(res[3]);
	//	double sin_scale = Math.sin(angle * scale) / Math.sin(res[3]);
		double sin_scale = Math.sin(angle * scale) / Math.sin(angle);
		if(!Double.isNaN(sin_scale)) {
			res[0] *= sin_scale;
			res[1] *= sin_scale;
			res[2] *= sin_scale;
			res[3]  = Math.cos(angle * scale);			
		} else {
			setIndentity(res);
		}
	}
	
	public static void mulScale(double[] res, double[] r, double[] q, double scale) {
		System.arraycopy(q, 0, Quaternion.mQuatwork, 0, 4);
		scale(Quaternion.mQuatwork, scale);
		mul(res, r, Quaternion.mQuatwork);
	}


	static public void limit(double[] res, double[] q, float[] min, float[] max) {
		for(int i = 0; i < 3; i++) {
			double angle = Math.sinh(q[i]);
			if(q[3] < 0) {
				if(angle >= 0) {
					angle =  Math.PI - angle;
				} else {
					angle = -Math.PI - angle;
				}
			}
			if(angle < min[i]) {
				angle = Math.max(min[i], -Math.PI);
			} else if(angle >= max[i]) {
				angle = Math.min(max[i],  Math.PI);
			}
			res[i] = Math.sin(angle);
			if(q[3] < 0) {
				res[i] = - res[i];
			}
		}
		
		res[3] = Math.sqrt(1 - res[0] * res[0] - res[1] * res[1] - res[2] * res[2]);
		if(q[3] < 0) {
			res[3] = -res[3];
		}
	}

	static public void toMatrix(float mat[], float quat[]) {
		float x2 = quat[0] * quat[0] * 2.0f;
		float y2 = quat[1] * quat[1] * 2.0f;
		float z2 = quat[2] * quat[2] * 2.0f;
		float xy = quat[0] * quat[1] * 2.0f;
		float yz = quat[1] * quat[2] * 2.0f;
		float zx = quat[2] * quat[0] * 2.0f;
		float xw = quat[0] * quat[3] * 2.0f;
		float yw = quat[1] * quat[3] * 2.0f;
		float zw = quat[2] * quat[3] * 2.0f;
	
		mat[0] = 1.0f - y2 - z2;
		mat[1] = xy + zw;
		mat[2] = zx - yw;
		mat[4] = xy - zw;
		mat[5] = 1.0f - z2 - x2;
		mat[6] = yz + xw;
		mat[8] = zx + yw;
		mat[9] = yz - xw;
		mat[10] = 1.0f - x2 - y2;
	
		mat[3] = mat[7] = mat[11] = mat[12] = mat[13] = mat[14] = 0.0f;
		mat[15] = 1.0f;
	}

	static public void toMatrixPreserveTranslate(float mat[], double quat[]) {
		double x2 = quat[0] * quat[0] * 2.0f;
		double y2 = quat[1] * quat[1] * 2.0f;
		double z2 = quat[2] * quat[2] * 2.0f;
		double xy = quat[0] * quat[1] * 2.0f;
		double yz = quat[1] * quat[2] * 2.0f;
		double zx = quat[2] * quat[0] * 2.0f;
		double xw = quat[0] * quat[3] * 2.0f;
		double yw = quat[1] * quat[3] * 2.0f;
		double zw = quat[2] * quat[3] * 2.0f;
	
		mat[0] = (float) (1.0f - y2 - z2);
		mat[1] = (float) (xy + zw);
		mat[2] = (float) (zx - yw);
		mat[4] = (float) (xy - zw);
		mat[5] = (float) (1.0f - z2 - x2);
		mat[6] = (float) (yz + xw);
		mat[8] = (float) (zx + yw);
		mat[9] = (float) (yz - xw);
		mat[10] = (float) (1.0f - x2 - y2);
	
		mat[3] = mat[7] = mat[11] = /* mat[12] = mat[13] = mat[14] = */0.0f;
		mat[15] = 1.0f;
	}

}
