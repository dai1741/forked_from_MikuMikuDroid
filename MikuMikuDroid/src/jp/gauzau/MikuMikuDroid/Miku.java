package jp.gauzau.MikuMikuDroid;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

import android.opengl.Matrix;
import android.util.Log;

public class Miku {
	public class RenderSet {
		public String shader;
		public String target;
		public RenderSet(String s, String t) {
			shader = s;
			target = t;
		}
	}
	
	// use NDK
	private boolean mIsArm = false;
	
	// model data
	public MikuModel mModel;

	// motion data
	public MikuMotion mMotion;
	
	// render senario
	public ArrayList<RenderSet> mRenderSenario = new ArrayList<RenderSet>();

	// temporary data
	private MotionPair mMpWork = new MotionPair();
	private MotionIndex mMwork = new MotionIndex();
	private float effecterVecs[] = new float[4];
	private float effecterInvs[] = new float[4];
	private float targetVecs[] = new float[4];
	private float targetInvs[] = new float[4];
	private float axis[] = new float[3];
	private float mMatworks[] = new float[16];
	private double[] mQuatworks = new double[4];
	private float tmpVecs[] = new float[3];

	private FacePair mFacePair = new FacePair();
	private FaceIndex mFaceIndex = new FaceIndex();

	private Bone mZeroBone;

	public Miku(MikuModel model) {
		mModel = model;
		mMwork.location = new float[3];
		mMwork.rotation = new float[4];
		mIsArm = CoreLogic.isArm();
		
		// for physics simulation
		mZeroBone = new Bone();
		mZeroBone.matrix = new float[16];
		mZeroBone.head_pos = new float[3];
		Matrix.setIdentityM(mZeroBone.matrix, 0);
		mZeroBone.head_pos[0] = 0; mZeroBone.head_pos[1] = 0; mZeroBone.head_pos[2] = 0;
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
	
	public void addRenderSenario(String s, String t) {
		mRenderSenario.add(new RenderSet(s, t));
	}

	public void setBonePosByVMDFramePre(float i, float step, boolean initPhysics) {
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

			for (int r = 0; r < max; r++) {
				Bone b = ba.get(r);
				updateBoneMatrix(b);
			}
			
			if(mIsArm) {
				if(initPhysics) {
					initializePhysics();
				}
				solvePhysicsPre();				
			}
		}
	}
	
	public void setBonePosByVMDFramePost() {
		ArrayList<Bone> ba = mModel.mBone;
		if(ba != null) {
			int max = ba.size();
			
			if(mIsArm) {
				solvePhysicsPost();
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
			if(mIsArm) {	// use native code for ARM machine
				initFaceNative(mModel.mAllBuffer, mModel.mFaceBase.face_vert_count, mModel.mFaceBase.face_vert_index_native, mModel.mFaceBase.face_vert_offset_native);
				for (Face f : mModel.mFace) {
					setFaceForNative(f, i);
				}				
			} else {		// use Java code
				initFace(mModel.mFaceBase);

				for (Face f : mModel.mFace) {
					setFace(f, i);
				}

				updateVertexFace(mModel.mFaceBase);
			}
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
		FaceIndex m = mMotion.interpolateLinear(mp, f.motion, i, mFaceIndex);
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
	
	private void setFaceForNative(Face f, float i) {
		FacePair mp = mMotion.findFace(f, i, mFacePair);
		FaceIndex m = mMotion.interpolateLinear(mp, f.motion, i, mFaceIndex);
		if (m != null && m.weight > 0) {
			setFaceNative(mModel.mAllBuffer, mModel.mFaceBase.face_vert_index_native, f.face_vert_count, f.face_vert_index_native, f.face_vert_offset_native, m.weight);
		}
	}
	
	native private void initFaceNative(FloatBuffer vertex, int count, IntBuffer index, FloatBuffer offset);
	
	native private void setFaceNative(FloatBuffer vertex, IntBuffer pointer, int count, IntBuffer index, FloatBuffer offset, float weight);

	native private int btAddRigidBody(int type, int shape, float w, float h, float d, float[] pos, float[] rot, float[] head_pos, float[] bone, float mass, float v_dim, float r_dim, float recoil, float friction, byte group_index, short group_target);

	native private int btAddJoint(int rb1, int rb2, float[] pos, float[] rot, float[] p1, float[] p2, float[] r1, float[] r2, float[] sp, float[] sr);

	native private float btGetOpenGLMatrix(int rb, float[] matrix, float[] pos, float[] rot);

	native private float btSetOpenGLMatrix(int rb, float[] matrix, float[] pos, float[] rot);
	
	private void initializePhysics() {
		///////////////////////////////////////////
		// MAKE RIGID BODIES
		ArrayList<RigidBody> rba = mModel.mRigidBody;
		if(rba != null) {
			for(int i = 0; i < rba.size(); i++) {
				RigidBody rb = rba.get(i);
				Bone b = rb.bone_index == -1 ? mZeroBone : mModel.mBone.get(rb.bone_index);
				rb.btrb = btAddRigidBody(rb.type, rb.shape,
						rb.size[0], rb.size[1], rb.size[2],
						rb.location, rb.rotation, b.head_pos, b.matrix,
						rb.weight, rb.v_dim, rb.r_dim, rb.recoil, rb.friction,
						rb.group_index, rb.group_target);				
			}
		}
		
		
		///////////////////////////////////////////
		// MAKE JOINTS
		ArrayList<Joint> ja = mModel.mJoint;
		if(ja != null) {
			for(int i = 0; i < ja.size(); i++) {
				Joint j = ja.get(i);
				int rb1 = rba.get(j.rigidbody_a).btrb;
				int rb2 = rba.get(j.rigidbody_b).btrb;
				
				j.btcst = btAddJoint(rb1, rb2, j.position, j.rotation, j.const_position_1, j.const_position_2,
						j.const_rotation_1, j.const_rotation_2, j.spring_position, j.spring_rotation);
			}
		}
	}
	
	private void solvePhysicsPre() {
		if(mModel.mRigidBody != null) {
			for(int i = 0; i < mModel.mRigidBody.size(); i++) {
				RigidBody rb = mModel.mRigidBody.get(i);
				if(rb.bone_index >= 0 && rb.type == 0) {
					Bone b = mModel.mBone.get(rb.bone_index);
					btSetOpenGLMatrix(rb.btrb, b.matrix, rb.location, rb.rotation);
				}
			}
		}
	}

	private void solvePhysicsPost() {
		if(mModel.mRigidBody != null) {			
			for(int i = 0; i < mModel.mRigidBody.size(); i++) {
				RigidBody rb = mModel.mRigidBody.get(i);
				if(rb.bone_index >= 0 && rb.type != 0) {
					Bone b = mModel.mBone.get(rb.bone_index);
					if(rb.type == 1) {
						btGetOpenGLMatrix(rb.btrb, b.matrix, rb.location, rb.rotation);
					} else { // rb.type == 2
						btGetOpenGLMatrix(rb.btrb, b.matrix_current, rb.location, rb.rotation);
						for(int j = 0; j < 12; j++) {
							b.matrix[j] = b.matrix_current[j];
						}
					}
					b.updated = true;
				}
			}
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
		MotionIndex m = mMotion.interpolateLinear(mp, b.motion, idx, mMwork);
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
