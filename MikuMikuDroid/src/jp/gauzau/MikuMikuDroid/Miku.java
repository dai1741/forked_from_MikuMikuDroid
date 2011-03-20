package jp.gauzau.MikuMikuDroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

import android.opengl.Matrix;
import android.util.Log;

public class Miku {
	// model data
	public MikuModel mModel;

	// motion data
	public MikuMotion mMotion;

	// temporary data
	private MotionPair mMpWork = new MotionPair();
	private Motion mMwork = new Motion();
	private float effecterVecs[] = new float[4];
	private float effecterInvs[] = new float[4];
	private float targetVecs[] = new float[4];
	private float targetInvs[] = new float[4];
	private float axis[] = new float[3];
	private float mMatworks[] = new float[16];
	private double[] mQuatworks = new double[4];

	private FacePair mFacePair = new FacePair();
	private FaceIndex mFaceIndex = new FaceIndex();

	public Miku(MikuModel model) {
		mModel = model;
		mMwork.location = new float[3];
		mMwork.rotation = new float[4];
		physicsInitializer();
	}

	public void attachMotion(MikuMotion mm) {
		mMotion = mm;
		mm.attachModel(mModel.mBone, mModel.mFace);
		if(mModel.mIK != null && mm.getIKMotion() == null) {
			// preCalcIK();
			Log.d("Miku", "IK calcuration");
			preCalcKeyFrameIK();			
		}
	}

	public void setBonePosByVMDFrame(float i) {
		ArrayList<Bone> ba = mModel.mBone;
		if(ba != null) {
			int max = ba.size();

			for (int r = 0; r < max; r++) {
				Bone b = ba.get(r);
				setBoneMatrix(b, i);
			}

			if(mModel.mIK != null && mMotion.getIKMotion() == null) {
				ccdIK();
			}
			// fakePhysics(i);

			for (int r = 0; r < max; r++) {
				Bone b = ba.get(r);
				updateBoneMatrix(b);
			}

			for (int r = 0; r < max; r++) {
				Bone b = ba.get(r);
				Matrix.translateM(b.matrix, 0, -b.head_pos[0], -b.head_pos[1], -b.head_pos[2]);
				b.updated = false;
			}
		}
	}
	
	public void setFaceByVMDFrame(float i) {
		if (mModel.mFaceBase != null) {
			initFace(mModel.mFaceBase);

			for (Face f : mModel.mFace) {
				setFace(f, i);
			}

			updateVertexFace(mModel.mFaceBase);
		}
	}

	
	
	
	private void initFace(Face f) {
		for (int i = 0; i < f.face_vert_count; i++) {
			f.face_vert_base[i*3+0] = f.face_vert_offset[i*3+0];
			f.face_vert_base[i*3+1] = f.face_vert_offset[i*3+1];
			f.face_vert_base[i*3+2] = f.face_vert_offset[i*3+2];
			f.face_vert_updated[i] = false;
		}
	}

	private void setFace(Face f, float i) {
		FacePair mp = mMotion.findFace(f, i, mFacePair);
		FaceIndex m = mMotion.interpolateLinear(mp, i, mFaceIndex);
		if (m != null && m.weight > 0) {
			for (int r = 0; r < f.face_vert_count; r++) {
				int baseidx = f.face_vert_index[r];
				mModel.mFaceBase.face_vert_base[baseidx*3+0] += f.face_vert_offset[r*3+0] * m.weight;
				mModel.mFaceBase.face_vert_base[baseidx*3+1] += f.face_vert_offset[r*3+1] * m.weight;
				mModel.mFaceBase.face_vert_base[baseidx*3+2] += f.face_vert_offset[r*3+2] * m.weight;
				mModel.mFaceBase.face_vert_updated[baseidx]   = true;
			}
		}
	}

	private void updateVertexFace(Face f) {
		for (int r = 0; r < f.face_vert_count; r++) {
			if (f.face_vert_updated[r] || !f.face_vert_cleared[r]) {
				mModel.mAllBuffer.position(f.face_vert_index[r]);
				mModel.mAllBuffer.put(f.face_vert_base, r*3, 3);
				f.face_vert_cleared[r] = !f.face_vert_updated[r];
			}
		}
		mModel.mAllBuffer.position(0);
	}

	private void fakePhysics(float i) {
		if(mModel.mRigidBody != null) {
			physicsFollowBone();
			physicsFakeExec(i);
			physicsCheckCollision();
			physicsMoveBone();			
		}
	}
	
	private void physicsInitializer() {
		float gravity[] = new float[4];
		gravity[0] = 0; gravity[1] = -1; gravity[2] = 0; gravity[3] = 1;
		
		ArrayList<RigidBody> rba = mModel.mRigidBody;
		if(rba != null) {
			for(int i = 0; i < rba.size(); i++) {
				RigidBody rb = rba.get(i);
				if(rb.bone_index >= 0) {
					Bone base = mModel.mBone.get(rb.bone_index);
					rb.cur_location[0] = base.head_pos[0] + rb.location[0];
					rb.cur_location[1] = base.head_pos[1] + rb.location[1];
					rb.cur_location[2] = base.head_pos[2] + rb.location[2];
					rb.cur_location[3] = 1;
					calcPendulumA(rb.cur_a, base, rb.cur_location, gravity, 1);
					calcPendulumA(rb.tmp_a, base, rb.cur_location, gravity, 1);
				} else {
					rb.cur_location[0] = rb.location[0];
					rb.cur_location[1] = rb.location[1];
					rb.cur_location[2] = rb.location[2];
					rb.cur_location[3] = 1;
					Quaternion.setIndentity(rb.cur_a);
					Quaternion.setIndentity(rb.tmp_a);
				}
				Quaternion.setIndentity(rb.cur_r);
				Quaternion.setIndentity(rb.cur_v);
				Quaternion.setIndentity(rb.tmp_r);
				Quaternion.setIndentity(rb.tmp_v);
				Quaternion.setIndentity(rb.prev_r);			
			}			
		}
	}

	private void physicsFollowBone() {
		float time = 0.1f;	// must be fixed
		
		ArrayList<RigidBody> rba = mModel.mRigidBody;
		for(int i = 0; i < rba.size(); i++) {
			RigidBody rb = rba.get(i);
			if(rb.type == 0) { // follow bone
				
			} else if(rb.bone_index >= 0) {			// follow previous fake physics
				Bone b = mModel.mBone.get(rb.bone_index);
				
				// calculate v, a from previous position
				System.arraycopy(rb.cur_r, 0, rb.prev_r, 0, 4);
				Quaternion.mulScale(rb.tmp_v, rb.cur_v, rb.cur_a, time);
				Quaternion.mulScale(rb.tmp_r, rb.cur_r, rb.cur_v, time);
//				quaternionLimit(rb.tmp_r, rb.tmp_r, j.const_rotation_1, j.const_rotation_2);

				Quaternion.toMatrixPreserveTranslate(b.matrix_current, rb.tmp_r);
			}
		}		
	}

	private void physicsFakeExec(float i) {
		float time = 0.1f;	// must be fixed
		
		float gravity[] = new float[4];
		gravity[0] = 0; gravity[1] = -1; gravity[2] = 0; gravity[3] = 1;	// must add F
		
		ArrayList<Joint> ja = mModel.mJoint;
		for(int idx = 0; idx < ja.size(); idx++) {
			Joint rb = ja.get(idx);
			RigidBody target = mModel.mRigidBody.get(rb.rigidbody_b);
			if(target.type != 0 && target.bone_index >= 0) { // physics simulation
				Bone base = mModel.mBone.get(target.bone_index);

//				Log.d("Miku", String.format("Physics %d Bone %d: pos %f, %f, %f",
//						rb.rigidbody_b, target.bone_index, target.cur_location[0], target.cur_location[1], target.cur_location[2]));

				// calculate v, a from current position
				float[] current = getCurrentMatrix(base);
				targetVecs[0] = target.location[0];
				targetVecs[1] = target.location[1];
				targetVecs[2] = target.location[2];
				targetVecs[3] = 1;
				Matrix.multiplyMV(target.cur_location, 0, current, 0, targetVecs, 0);

				calcPendulumA(target.tmp_a, base, target.cur_location, gravity, 1.0f);
//				Log.d("Miku", String.format("  a2 %f, %f, %f %f", target.tmp_a[0], target.tmp_a[1], target.tmp_a[2], target.tmp_a[3]));
//				Log.d("Miku", String.format("  a1 %f, %f, %f %f", target.cur_a[0], target.cur_a[1], target.cur_a[2], target.cur_a[3]));
				
				Quaternion.mulScale(mQuatworks, target.cur_v, target.tmp_a, time);
//				Log.d("Miku", String.format("  v1 %f, %f, %f %f", mQuatworks[0], mQuatworks[1], mQuatworks[2], mQuatworks[3]));
//				Log.d("Miku", String.format("  v2 %f, %f, %f %f", target.tmp_v[0], target.tmp_v[1], target.tmp_v[2], target.tmp_v[3]));
				Quaternion.mul(target.cur_v, mQuatworks, target.tmp_v);
				Quaternion.scale(target.cur_v, 0.5f);
				
				Quaternion.mulScale(mQuatworks, target.cur_r, target.tmp_v, time);
//				Log.d("Miku", String.format("  r1 %f, %f, %f %f", mQuatworks[0], mQuatworks[1], mQuatworks[2], mQuatworks[3]));
//				Log.d("Miku", String.format("  r2 %f, %f, %f %f", target.tmp_r[0], target.tmp_r[1], target.tmp_r[2], target.tmp_r[3]));
				Quaternion.mul(target.cur_r, mQuatworks, target.tmp_r);
				Quaternion.scale(target.cur_r, 0.5f);
				Quaternion.limit(target.cur_r, target.cur_r, rb.const_rotation_1, rb.const_rotation_2);
//				System.arraycopy(target.tmp_v, 0, target.cur_v, 0, 4);
//				System.arraycopy(target.tmp_r, 0, target.cur_r, 0, 4);
				

			}
		}		
	}

	private void calcPendulumA(double[] quat, Bone b, float[] location, float[] force, double delta) {
		float[] current = getCurrentMatrix(b);
		effecterVecs[0] = current[12] + force[0];
		effecterVecs[1] = current[13] + force[1];
		effecterVecs[2] = current[14] + force[2];
		effecterVecs[3] = 1;
		
		Vector.invertM(mMatworks, 0, current, 0);
		Matrix.multiplyMV(effecterInvs, 0, mMatworks, 0, effecterVecs, 0);
		Matrix.multiplyMV(targetInvs, 0, mMatworks, 0, location, 0);
		//Log.d("Miku", String.format("  eff %f, %f, %f", effecterInvs[0], effecterInvs[1], effecterInvs[2]));
		//Log.d("Miku", String.format("  tar %f, %f, %f", targetInvs[0], targetInvs[1], targetInvs[2]));

		// calculate rotation angle/axis
		Vector.normalize(effecterInvs);
		Vector.normalize(targetInvs);
		double angle = Math.acos(Math.abs(Vector.dot(effecterInvs, targetInvs)));
//		double angle = Math.acos(dot(effecterInvs, targetInvs));
		angle *= delta;	// must add friction

		if (!Double.isNaN(angle)) {
			Vector.cross(axis, targetInvs, effecterInvs);
			Vector.normalize(axis);
			if (!Double.isNaN(axis[0]) && !Double.isNaN(axis[1]) && !Double.isNaN(axis[2])) {
				Quaternion.createFromAngleAxis(quat, angle, axis);
			} else {
				Quaternion.setIndentity(quat);
			}
		} else {
			Quaternion.setIndentity(quat);
		}
	}
	
	private void physicsCheckCollision() {
		float gravity[] = new float[4];
		gravity[0] = 0; gravity[1] = -1; gravity[2] = 0; gravity[3] = 1;	// must add F

		// clear all
		for (Bone b : mModel.mBone) {
			b.updated = false;
		}
		
		float vec[] = new float[4];
		vec[3] = 1;
		ArrayList<RigidBody> rba = mModel.mRigidBody;
		for(int i = 0; i < rba.size(); i++) {
			RigidBody rb = rba.get(i);
			if(rb.type != 0 && rb.bone_index >= 0) { // follow bone
				Bone b = mModel.mBone.get(rb.bone_index);
				Quaternion.toMatrixPreserveTranslate(b.matrix_current, rb.cur_r);
			}
		}
		for(int i = 0; i < rba.size(); i++) {
			RigidBody rb = rba.get(i);
			if(rb.type != 0 && rb.bone_index >= 0) { // follow bone
				Bone b = mModel.mBone.get(rb.bone_index);
				float[] current = getCurrentMatrix(b);
				vec[0] = rb.location[0];
				vec[1] = rb.location[1];
				vec[2] = rb.location[2];
				Matrix.multiplyMV(rb.cur_location, 0, current, 0, vec, 0);
			}
		}
		
		// check collision
		ArrayList<Joint> ja = mModel.mJoint;
		for(int idx = 0; idx < ja.size(); idx++) {
			Joint j = ja.get(idx);
			RigidBody target = mModel.mRigidBody.get(j.rigidbody_b);
			if(target.type != 0 && target.bone_index >= 0) { // physics simulation
				for(int i = 0; i < rba.size(); i++) {
					if(i == j.rigidbody_b) {
						continue;
					}
					RigidBody rb = rba.get(i);
					
					float len = Matrix.length(
							rb.cur_location[0] - target.cur_location[0],
							rb.cur_location[1] - target.cur_location[1],
							rb.cur_location[2] - target.cur_location[2]);
					
//					if(len < rb.size[0] + target.size[0]) {	// collision
					if(len < Math.max(rb.size[0], Math.max(rb.size[1], rb.size[2])) +
							 Math.max(target.size[0], Math.max(target.size[1], target.size[2]))) { // collision
						System.arraycopy(rb.prev_r, 0, rb.cur_r, 0, 4);
//						rb.cur_v[3] = - rb.cur_v[3];
						break;
					}
				}
			}
		}		
		
		// clear all
		for (Bone b : mModel.mBone) {
			b.updated = false;
		}

	}

	private void physicsMoveBone() {
		float gravity[] = new float[4];
		gravity[0] = 0; gravity[1] = -1; gravity[2] = 0; gravity[3] = 1;	// must add F

		// clear all
		for (Bone b : mModel.mBone) {
			b.updated = false;
		}
		
		float vec[] = new float[4];
		vec[3] = 1;
		ArrayList<RigidBody> rba = mModel.mRigidBody;
		for(int i = 0; i < rba.size(); i++) {
			RigidBody rb = rba.get(i);
			if(rb.type != 0 && rb.bone_index >= 0) { // follow bone
				Bone b = mModel.mBone.get(rb.bone_index);
				Quaternion.toMatrixPreserveTranslate(b.matrix_current, rb.cur_r);
				Quaternion.scale(rb.cur_v, 1 - rb.r_dim);
			}
		}
		for(int i = 0; i < rba.size(); i++) {
			RigidBody rb = rba.get(i);
			if(rb.type != 0 && rb.bone_index >= 0) { // follow bone
				Bone b = mModel.mBone.get(rb.bone_index);
				updateBoneMatrix(b);
				vec[0] = rb.location[0];
				vec[1] = rb.location[1];
				vec[2] = rb.location[2];
				Matrix.multiplyMV(rb.cur_location, 0, b.matrix, 0, vec, 0);
				calcPendulumA(rb.cur_a, b, rb.cur_location, gravity, 1.0f);
			}
		}
		
		// clear all
		for (Bone b : mModel.mBone) {
			b.updated = false;
		}
	}

	private void preCalcKeyFrameIK() {
		float[] location = new float[3];
		location[0] = location[1] = location[2] = 0;
		
		HashMap<String, ArrayList<MotionIndex>> mhs = new HashMap<String, ArrayList<MotionIndex>>();			

		for (IK ik : mModel.mIK) {
			// find parents
			HashMap<Integer, Bone> parents = new HashMap<Integer, Bone>();
			int target = ik.ik_target_bone_index;
			while (target != -1) {
				Bone b = mModel.mBone.get(target);
				parents.put(target, b);
				target = b.parent;
			}

			int effecter = ik.ik_bone_index;
			while (effecter != -1) {
				Bone b = parents.get(effecter);
				if (b != null) {
					parents.remove(effecter);
				} else {
					b = mModel.mBone.get(effecter);
					parents.put(effecter, b);
				}
				effecter = b.parent;
			}

			// gather frames
			HashMap<Integer, Integer> frames = new HashMap<Integer, Integer>();
			for (Entry<Integer, Bone> bones : parents.entrySet()) {
				if (bones.getValue().motion != null) {
					for(int i = 0; i < bones.getValue().motion.frame_no.length; i++) {
						frames.put(bones.getValue().motion.frame_no[i], bones.getValue().motion.frame_no[i]);
					}
					/*
					for (MotionIndex frame : bones.getValue().motion) {
						frames.put(frame.frame_no, frame.frame_no);
					}
					*/
				}
			}

			ArrayList<Integer> framesInteger = new ArrayList<Integer>();
			for (Entry<Integer, Integer> ff : frames.entrySet()) {
				framesInteger.add(ff.getKey());
			}
			Collections.sort(framesInteger, new Comparator<Integer>() {
				public int compare(Integer m0, Integer m1) {
					return m0 - m1;
				}
			});

			// calc IK
			HashMap<Short, ArrayList<MotionIndex>> mhash = new HashMap<Short, ArrayList<MotionIndex>>();
			for (Integer frame : framesInteger) {
				for (Bone b : mModel.mBone) {
					setBoneMatrix(b, frame);
				}

				ccdIK1(ik);

				for (int i = 0; i < ik.ik_chain_length; i++) {
					Bone c = mModel.mBone.get(ik.ik_child_bone_index[i]);
					MotionIndex cm = new MotionIndex();
					cm.frame_no = frame;
					cm.location = location;
					cm.rotation = new float[4];
					cm.rotation[0] = (float) c.quaternion[0]; // calc in ccdIK
					cm.rotation[1] = (float) c.quaternion[1];
					cm.rotation[2] = (float) c.quaternion[2];
					cm.rotation[3] = (float) c.quaternion[3];
					cm.interp = null;

					ArrayList<MotionIndex> mi = mhash.get(ik.ik_child_bone_index[i]);
					if (mi == null) {
						mi = new ArrayList<MotionIndex>();
						mhash.put(ik.ik_child_bone_index[i], mi);
					}
					mi.add(cm);
				}
			}

			// set motions to bones and motion
			for (Entry<Short, ArrayList<MotionIndex>> entry : mhash.entrySet()) {
				Bone b = mModel.mBone.get(entry.getKey());
				b.motion = MikuMotion.toMotionIndexA(entry.getValue());
				b.current_motion = 0;
				mhs.put(b.name, entry.getValue());
			}
		}
		mMotion.setIKMotion(mhs);
	}

	private void setBoneMatrix(Bone b, float idx) {
		MotionPair mp = mMotion.findMotion(b, idx, mMpWork);
		Motion m = mMotion.interpolateLinear(mp, b.motion, idx, mMwork);
		if (m != null) {
			b.quaternion[0] = m.rotation[0];
			b.quaternion[1] = m.rotation[1];
			b.quaternion[2] = m.rotation[2];
			b.quaternion[3] = m.rotation[3];
			Quaternion.toMatrix(b.matrix_current, m.rotation);

			if (b.parent == -1) {
				b.matrix_current[12] = m.location[0] + b.head_pos[0];
				b.matrix_current[13] = m.location[1] + b.head_pos[1];
				b.matrix_current[14] = m.location[2] + b.head_pos[2];
			} else {
				Bone p = mModel.mBone.get(b.parent);
				b.matrix_current[12] = m.location[0] + (b.head_pos[0] - p.head_pos[0]);
				b.matrix_current[13] = m.location[1] + (b.head_pos[1] - p.head_pos[1]);
				b.matrix_current[14] = m.location[2] + (b.head_pos[2] - p.head_pos[2]);
			}
		} else {
			// no VMD info so assume that no rotation and translation are specified
			Matrix.setIdentityM(b.matrix_current, 0);
			Quaternion.setIndentity(b.quaternion);
			if (b.parent == -1) {
				Matrix.translateM(b.matrix_current, 0, b.head_pos[0], b.head_pos[1], b.head_pos[2]);
			} else {
				Bone p = mModel.mBone.get(b.parent);
				Matrix.translateM(b.matrix_current, 0, b.head_pos[0], b.head_pos[1], b.head_pos[2]);
				Matrix.translateM(b.matrix_current, 0, -p.head_pos[0], -p.head_pos[1], -p.head_pos[2]);
			}
		}
	}

	private void updateBoneMatrix(Bone b) {
		if (b.updated == false) {
			if (b.parent != -1) {
				Bone p = mModel.mBone.get(b.parent);
				updateBoneMatrix(p);
				Matrix.multiplyMM(b.matrix, 0, p.matrix, 0, b.matrix_current, 0);
			} else {
				for (int i = 0; i < 16; i++) {
					b.matrix[i] = b.matrix_current[i];
				}
			}
			b.updated = true;
		}
	}

	private void ccdIK() {
		for (IK ik : mModel.mIK) {
			ccdIK1(ik);
		}
	}

	private void ccdIK1(IK ik) {
		Bone effecter = mModel.mBone.get(ik.ik_bone_index);
		Bone target = mModel.mBone.get(ik.ik_target_bone_index);

		getCurrentPosition(effecterVecs, effecter);

		for (int i = 0; i < ik.iterations; i++) {
			for (int j = 0; j < ik.ik_chain_length; j++) {
				Bone b = mModel.mBone.get(ik.ik_child_bone_index[j]);

				clearUpdateFlags(b, target);
				getCurrentPosition(targetVecs, target);

				if (b.is_leg) {
					if (i == 0) {
						Bone base = mModel.mBone.get(ik.ik_child_bone_index[ik.ik_chain_length - 1]);
						getCurrentPosition(targetInvs, b);
						getCurrentPosition(effecterInvs, base);

						double eff_len = Matrix.length(effecterVecs[0] - effecterInvs[0], effecterVecs[1] - effecterInvs[1], effecterVecs[2] - effecterInvs[2]);
						double b_len = Matrix.length(targetInvs[0] - effecterInvs[0], targetInvs[1] - effecterInvs[1], targetInvs[2] - effecterInvs[2]);
						double t_len = Matrix.length(targetVecs[0] - targetInvs[0], targetVecs[1] - targetInvs[1], targetVecs[2] - targetInvs[2]);

						double angle = Math.acos((eff_len * eff_len - b_len * b_len - t_len * t_len) / (2 * b_len * t_len));
						if (!Double.isNaN(angle)) {
							axis[0] = -1;
							axis[1] = axis[2] = 0;
							Quaternion.createFromAngleAxis(mQuatworks, angle, axis);
							Quaternion.mul(b.quaternion, b.quaternion, mQuatworks);
							Quaternion.toMatrixPreserveTranslate(b.matrix_current, b.quaternion);
						}
					}
					continue;
				}

				if (Matrix.length(targetVecs[0] - effecterVecs[0], targetVecs[1] - effecterVecs[1], targetVecs[2] - effecterVecs[2]) < 0.001f) {
					// clear all
					for (Bone c : mModel.mBone) {
						c.updated = false;
					}
					return;
				}

				float[] current = getCurrentMatrix(b);
				Vector.invertM(mMatworks, 0, current, 0);
				Matrix.multiplyMV(effecterInvs, 0, mMatworks, 0, effecterVecs, 0);
				Matrix.multiplyMV(targetInvs, 0, mMatworks, 0, targetVecs, 0);

				// calculate rotation angle/axis
				Vector.normalize(effecterInvs);
				Vector.normalize(targetInvs);
				double angle = Math.acos(Vector.dot(effecterInvs, targetInvs));
				angle *= ik.control_weight;

				if (!Double.isNaN(angle)) {
					Vector.cross(axis, targetInvs, effecterInvs);
					Vector.normalize(axis);

					// rotateM(mMatworks, 0, b.matrix_current, 0, degree, axis[0], axis[1], axis[2]);
					// System.arraycopy(mMatworks, 0, b.matrix_current, 0, 16);
					if (!Double.isNaN(axis[0]) && !Double.isNaN(axis[1]) && !Double.isNaN(axis[2])) {
						Quaternion.createFromAngleAxis(mQuatworks, angle, axis);
						Quaternion.mul(b.quaternion, b.quaternion, mQuatworks);
						Quaternion.toMatrixPreserveTranslate(b.matrix_current, b.quaternion);
					}
				}
			}
		}
		// clear all
		for (Bone b : mModel.mBone) {
			b.updated = false;
		}
	}

	private void clearUpdateFlags(Bone root, Bone b) {
		while (root != b) {
			b.updated = false;
			if (b.parent != -1) {
				b = mModel.mBone.get(b.parent);
			} else {
				return;
			}
		}
		root.updated = false;
	}

	private void getCurrentPosition(float v[], Bone b) {
		float[] current = getCurrentMatrix(b);
		System.arraycopy(current, 12, v, 0, 3);
		v[3] = 1;
	}

	private float[] getCurrentMatrix(Bone b) {
		updateBoneMatrix(b);
		return b.matrix;
	}

	public boolean hasMotion() {
		return mMotion != null;
	}

}
