package jp.gauzau.MikuMikuDroid;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11Ext;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

public class Miku {
	private boolean mAnimation;
	private int mRenameNum;
	private int mRenameBone;

	private FloatBuffer mToonCoordBuffer;
	private FloatBuffer mWeightBuffer;
	private ShortBuffer mIndexBuffer;
	private FloatBuffer mAllBuffer;

	private float mBoneMatrix[];

	private PMDParser mPMD;
	private VMDParser mVMD;

	private ArrayList<Material> mRendarList;
	private HashMap<String, TexBitmap> mTexture;
	private ArrayList<TexBitmap> mToon;

	private float mMatworks[] = new float[16];
	private MotionPair mMpWork = new MotionPair();
	private Motion mMwork = new Motion();
	private int mIndexMaps[];
	private CubeArea mCube;
	private float effecterVecs[] = new float[4];
	private float effecterInvs[] = new float[4];
	private float targetVecs[] = new float[4];
	private float targetInvs[] = new float[4];
	private float axis[] = new float[3];
	private float mMatworks2[] = new float[16];
	private float[] mInvSrcs = new float[16];
	private float[] mInvTmps = new float[12];
	private float[] mInvDsts = new float[16];
	private double[] mQuatworks = new double[4];
	private Face mFaceBase;
	private FacePair mFacePair = new FacePair();
	private FaceIndex mFaceIndex = new FaceIndex();

	public Miku(PMDParser parser, int rename_num, int rename_bone, boolean animation) {
		init(parser, rename_num, rename_bone, animation);
	}

	public Miku(PMDParser parser, int rename_num, int rename_bone) {
		init(parser, rename_num, rename_bone, true);
	}

	public void init(PMDParser parser, int rename_num, int rename_bone, boolean animation) {
		mMwork.location = new float[3];
		mMwork.rotation = new float[4];
		mPMD			= parser;
		mRenameNum		= rename_num;
		mRenameBone		= rename_bone;
		mAnimation		= animation;
		mBoneMatrix		= new float[16 * mRenameBone];
		makeIndexSortedBuffers();
		if (animation) {
			reconstructFace();
			parser.recycleVertex();
			reconstructMaterial(mRenameBone);
		} else {
			mFaceBase = null;
			parser.recycleVertex();
			buildBoneNoMotionRenameIndex();
		}
		parser.recycle();
	}

	private void reconstructFace() {
		mFaceBase = null;

		// find base face
		if (mPMD.getFace() != null) {
			for (Face s : mPMD.getFace()) {
				if (s.face_type == 0) {
					mFaceBase = s;
					break;
				}
			}

			// update base face
			for (FaceVertData fvd : mFaceBase.face_vert_data) {
				// vertex is sorted by makeIndexSortedBuffers() in stride 8
				fvd.face_vert_index = mIndexMaps[fvd.face_vert_index] * 8;
			}
		}
	}

	private void makeIndexSortedBuffers() {
		mIndexMaps = new int[mPMD.numVertex()];
		for (int i = 0; i < mIndexMaps.length; i++) {
			mIndexMaps[i] = -1; // not mapped yet
		}
		int vc = 0;

		// vertex, normal, texture buffer
		ByteBuffer abb = ByteBuffer.allocateDirect(mPMD.numVertex() * 8 * 4);
		abb.order(ByteOrder.nativeOrder());
		mAllBuffer = abb.asFloatBuffer();

		// weight buffer
		ByteBuffer wbb = ByteBuffer.allocateDirect(mPMD.numVertex() * 2 * 4);
		wbb.order(ByteOrder.nativeOrder());
		mWeightBuffer = wbb.asFloatBuffer();

		// index buffer
		ByteBuffer ibb = ByteBuffer.allocateDirect(mPMD.getIndex().size() * 2);
		ibb.order(ByteOrder.nativeOrder());
		mIndexBuffer = ibb.asShortBuffer();

		// reference cube
		mCube = new CubeArea();

		// sort vertex by index order
		for (Integer idx : mPMD.getIndex()) {
			if (mIndexMaps[idx] < 0) { // not mapped yet
				Vertex ver = mPMD.getVertex().get(idx);

				// vertex, normal, texture
				mAllBuffer.put(ver.pos);
				mAllBuffer.put(ver.normal);
				mAllBuffer.put(ver.uv);

				// weight
				mWeightBuffer.put(ver.bone_weight / 100.0f);
				mWeightBuffer.put((100 - ver.bone_weight) / 100.0f);

				// update cube
				mCube.set(ver.pos);

				// update map
				mIndexMaps[idx] = vc++;
			}

			mIndexBuffer.put((short) mIndexMaps[idx]);
		}
		mIndexBuffer.position(0);
		mWeightBuffer.position(0);
		mAllBuffer.position(0);

		mCube.logOutput("Miku");
	}

	private void reconstructMaterial(int max_bone) {
		mRendarList = new ArrayList<Material>();
		for (Material mat : mPMD.getMaterial()) {
			reconstructMaterial1(mat, 0, max_bone);
		}
	}

	private void reconstructMaterial1(Material mat, int offset, int max_bone) {
		Material mat_new = new Material(mat);
		mat_new.face_vart_offset = mat.face_vart_offset + offset;

		ArrayList<Vertex> ver = mPMD.getVertex();
		HashMap<Integer, Integer> rename = new HashMap<Integer, Integer>();
		int acc = 0;
		for (int j = offset; j < mat.face_vert_count; j += 3) {
			acc = renameBone1(rename, mat.face_vart_offset + j + 0, ver, acc);
			acc = renameBone1(rename, mat.face_vart_offset + j + 1, ver, acc);
			acc = renameBone1(rename, mat.face_vart_offset + j + 2, ver, acc);
			if (acc > max_bone) {
				mat_new.face_vert_count = j - offset;
				mat_new.rename_hash = rename;
				buildBoneRenameMap(mat_new, rename, max_bone);
				buildBoneRenameInvMap(mat_new, rename, max_bone);
				buildBoneRenameIndex(mat_new, max_bone);
				mRendarList.add(mat_new);

				Log.d("Miku", "rename Bone for Material #" + String.valueOf(mat_new.face_vart_offset) + ", bones " + String.valueOf(acc));
				for (Entry<Integer, Integer> b : rename.entrySet()) {
					Log.d("Miku", String.format("ID %d: bone %d", b.getValue(), b.getKey()));
				}

				reconstructMaterial1(mat, j, max_bone);
				return;
			}
		}
		mat_new.face_vert_count = mat.face_vert_count - offset;
		mat_new.rename_hash = rename;
		buildBoneRenameMap(mat_new, rename, max_bone);
		buildBoneRenameInvMap(mat_new, rename, max_bone);
		buildBoneRenameIndex(mat_new, max_bone);
		mRendarList.add(mat_new);

		Log.d("Miku", "rename Bone for Material #" + String.valueOf(mat_new.face_vart_offset) + ", bones " + String.valueOf(acc));
		for (Entry<Integer, Integer> b : rename.entrySet()) {
			Log.d("Miku", String.format("ID %d: bone %d", b.getValue(), b.getKey()));
		}
	}

	private void buildBoneRenameMap(Material mat, HashMap<Integer, Integer> rename, int max_bone) {
		mat.rename_map = new int[mRenameNum];
		for (int i = 0; i < mRenameNum; i++) {
			mat.rename_map[i] = 0; // initialize
		}
		for (Entry<Integer, Integer> b : rename.entrySet()) {
			if (b.getValue() < max_bone) {
				mat.rename_map[b.getKey()] = b.getValue();
			}
		}
	}

	private void buildBoneRenameInvMap(Material mat, HashMap<Integer, Integer> rename, int max_bone) {
		mat.rename_inv_map = new int[mRenameBone];
		for (int i = 0; i < mRenameBone; i++) {
			mat.rename_inv_map[i] = -1; // initialize
		}
		for (Entry<Integer, Integer> b : rename.entrySet()) {
			if (b.getValue() < max_bone) {
				mat.rename_inv_map[b.getValue()] = b.getKey();
			}
		}
	}

	private void buildBoneRenameIndex(Material mat, int max_bone) {
		ByteBuffer rbb = ByteBuffer.allocateDirect(mPMD.numVertex() * 3);
		rbb.order(ByteOrder.nativeOrder());
		mat.rename_index = rbb;

		for (int i = 0; i < mIndexMaps.length; i++) {
			Vertex ver = mPMD.getVertex().get(i);
			if (mIndexMaps[i] >= 0) {
				mat.rename_index.position(mIndexMaps[i] * 3);
				mat.rename_index.put((byte) mat.rename_map[ver.bone_num_0]);
				mat.rename_index.put((byte) mat.rename_map[ver.bone_num_1]);
				mat.rename_index.put(ver.bone_weight);
			}
		}

		mat.rename_index.position(0);
	}

	private void buildBoneNoMotionRenameIndex() {
		ByteBuffer rbb = ByteBuffer.allocateDirect(mPMD.numVertex() * 3);
		rbb.order(ByteOrder.nativeOrder());

		for (int i = 0; i < mPMD.numVertex(); i++) {
			rbb.put((byte) 0);
			rbb.put((byte) 0);
			rbb.put((byte) 100);
		}
		rbb.position(0);

		for (Material m : mPMD.getMaterial()) {
			m.rename_index = rbb;
		}
	}

	private int renameBone1(HashMap<Integer, Integer> rename, int veridx, ArrayList<Vertex> ver, int acc) {
		int idx = ver.get(mPMD.getIndex().get(veridx)).bone_num_0;
		Integer i = rename.get(idx);
		if (i == null) {
			rename.put(idx, acc++);
		}
		idx = ver.get(mPMD.getIndex().get(veridx)).bone_num_1;
		i = rename.get(idx);
		if (i == null) {
			rename.put(idx, acc++);
		}

		return acc;
	}

	public void draw(GL10 gl) {
		GL11Ext gl11Ext = (GL11Ext) gl;

		if (mAnimation) {
			gl.glMatrixMode(GL11Ext.GL_MATRIX_PALETTE_OES);
		}

		ArrayList<Material> rendar = mAnimation ? mRendarList : mPMD.getMaterial();
		for (Material mat : rendar) {
			if (mAnimation) {
				for (Entry<Integer, Integer> ren : mat.rename_hash.entrySet()) {
					if (ren.getValue() < mRenameBone) {
						gl11Ext.glCurrentPaletteMatrixOES(ren.getValue());
						gl11Ext.glLoadPaletteFromModelViewMatrixOES();
						gl.glMultMatrixf(mPMD.getBone().get(ren.getKey()).matrix, 0);
					}
				}
				// gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
				gl11Ext.glMatrixIndexPointerOES(2, GL10.GL_UNSIGNED_BYTE, 3, mat.rename_index);
			}

			// Toon texture
			gl.glActiveTexture(GL10.GL_TEXTURE0);
			gl.glEnable(GL10.GL_TEXTURE_2D);
			gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mToon.get(mat.toon_index).tex);

			if (mat.texture != null) {
				gl.glActiveTexture(GL10.GL_TEXTURE1);
				gl.glEnable(GL10.GL_TEXTURE_2D);
				gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
				gl.glBindTexture(GL10.GL_TEXTURE_2D, mTexture.get(mat.texture).tex);
			} else {
				gl.glActiveTexture(GL10.GL_TEXTURE1);
				gl.glDisable(GL10.GL_TEXTURE_2D);
			}

			// gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, mat.face_color, 0);
			// gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, mat.specular_color, 0);
			// gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, mat.emmisive_color, 0);
			// gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, mat.power);

			gl.glColor4f(mat.diffuse_color[0], mat.diffuse_color[1], mat.diffuse_color[2], mat.diffuse_color[3]);
			mIndexBuffer.position(mat.face_vart_offset);
			gl.glDrawElements(GL10.GL_TRIANGLES, mat.face_vert_count, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
		}
		mIndexBuffer.position(0);
	}

	public void drawGLES20(int bone, int blend, int texen, int color, int spec, int pow, int amb /* , float[] mvmatrix */) {
		ArrayList<Material> rendar = mAnimation ? mRendarList : mPMD.getMaterial();
		ArrayList<Bone> bs = mPMD.getBone();

		int max = rendar.size();
		for (int r = 0; r < max; r++) {
			Material mat = rendar.get(r);
			if (mAnimation) {
				for (int j = 0; j < mRenameBone; j++) {
					int inv = mat.rename_inv_map[j];
					if (inv >= 0) {
						Bone b = bs.get(inv);
						System.arraycopy(b.matrix, 0, mBoneMatrix, j * 16, 16);
					}
				}
				GLES20.glUniformMatrix4fv(bone, mat.rename_hash.size(), false, mBoneMatrix, 0);

				GLES20.glEnableVertexAttribArray(blend);
				GLES20.glVertexAttribPointer(blend, 3, GLES20.GL_UNSIGNED_BYTE, false, 0, mat.rename_index);
				checkGlError("drawGLES20 VertexAttribPointer blend");
			}

			// Toon texture
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mToon.get(mat.toon_index).tex);

			if (mat.texture != null) {
				GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture.get(mat.texture).tex);
				GLES20.glUniform1i(texen, 1);
			} else {
				GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
				GLES20.glUniform1i(texen, 0);
			}
			checkGlError("on DrawGLES20");

			float w = 0.6f;
			float wi = 0.6f;
			GLES20.glUniform4f(color, mat.diffuse_color[0] * wi, mat.diffuse_color[1] * wi, mat.diffuse_color[2] * wi, mat.diffuse_color[3]);
			GLES20.glUniform4f(amb, mat.emmisive_color[0] * w, mat.emmisive_color[1] * w, mat.emmisive_color[2] * w, mat.emmisive_color[3]);
			if (pow >= 0) {
				GLES20.glUniform4f(spec, mat.specular_color[0] * w, mat.specular_color[1] * w, mat.specular_color[2] * w, mat.specular_color[3]);
				GLES20.glUniform1f(pow, mat.power);
			}
			// GLES20.glBlendColor(0, 0, 0, mat.face_color[3]);
			mIndexBuffer.position(mat.face_vart_offset);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, mat.face_vert_count, GLES20.GL_UNSIGNED_SHORT, mIndexBuffer);
			checkGlError("glDrawElements");
		}
		mIndexBuffer.position(0);
	}

	public void setFaceByVMDFrame(float i) {
		if (mFaceBase != null) {
			initFace(mFaceBase);

			for (Face f : mPMD.getFace()) {
				setFace(f, i);
			}

			updateVertexFace(mFaceBase);
		}
	}

	private void initFace(Face f) {
		for (int i = 0; i < f.face_vert_count; i++) {
			FaceVertData fvd = f.face_vert_data.get(i);

			fvd.base[0] = fvd.offset[0];
			fvd.base[1] = fvd.offset[1];
			fvd.base[2] = fvd.offset[2];
			fvd.updated = false;
		}
	}

	private void setFace(Face f, float i) {
		FacePair mp = mVMD.findFace(f, i, mFacePair);
		FaceIndex m = mVMD.interpolateLinear(mp, i, mFaceIndex);
		if (m != null && m.weight > 0) {
			for (int r = 0; r < f.face_vert_count; r++) {
				FaceVertData fvd = f.face_vert_data.get(r);

				FaceVertData base = mFaceBase.face_vert_data.get(fvd.face_vert_index);
				base.base[0] += fvd.offset[0] * m.weight;
				base.base[1] += fvd.offset[1] * m.weight;
				base.base[2] += fvd.offset[2] * m.weight;
				base.updated = true;
			}
		}
	}

	private void updateVertexFace(Face f) {
		for (int r = 0; r < f.face_vert_count; r++) {
			FaceVertData fvd = f.face_vert_data.get(r);

			if (fvd.updated || !fvd.cleared) {
				mAllBuffer.position(fvd.face_vert_index);
				mAllBuffer.put(fvd.base, 0, 3);
				fvd.cleared = !fvd.updated;
			}
		}
		mAllBuffer.position(0);
	}

	public void initBoneManager(VMDParser parser) {
		mVMD = parser;
		// preCalcIK();
		preCalcKeyFrameIK();
	}

	public void setBonePosByVMDFrame(float i) {
		ArrayList<Bone> ba = mPMD.getBone();
		int max = ba.size();

		for (int r = 0; r < max; r++) {
			Bone b = ba.get(r);
			setBoneMatrix(b, i);
		}

		// ccdIK();

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

	public void preCalcIK() {
		HashMap<Short, ArrayList<MotionIndex>> mhash = new HashMap<Short, ArrayList<MotionIndex>>();

		for (int frame = 0; frame < mVMD.maxFrame(); frame += 5) {
			for (Bone b : mPMD.getBone()) {
				setBoneMatrix(b, frame);
			}

			ccdIK();

			float[] location = new float[3];
			location[0] = location[1] = location[2] = 0;

			for (IK ik : mPMD.getIK()) {
				for (int i = 0; i < ik.ik_chain_length; i++) {
					Bone c = mPMD.getBone().get(ik.ik_child_bone_index[i]);
					MotionIndex cm = new MotionIndex();
					cm.frame_no = frame;
					cm.location = location;
					cm.rotation = new float[4];
					cm.rotation[0] = (float) c.quaternion[0]; // calc in ccdIK
					cm.rotation[1] = (float) c.quaternion[1];
					cm.rotation[2] = (float) c.quaternion[2];
					cm.rotation[3] = (float) c.quaternion[3];
					cm.interp = null;
					cm.position = 0;

					ArrayList<MotionIndex> mi = mhash.get(ik.ik_child_bone_index[i]);
					if (mi == null) {
						mi = new ArrayList<MotionIndex>();
						mhash.put(ik.ik_child_bone_index[i], mi);
					}
					mi.add(cm);
				}
			}

		}

		for (Entry<Short, ArrayList<MotionIndex>> entry : mhash.entrySet()) {
			Bone b = mPMD.getBone().get(entry.getKey());
			b.motion = entry.getValue();
			b.current_motion = 0;
		}
	}

	public void preCalcKeyFrameIK() {
		float[] location = new float[3];
		location[0] = location[1] = location[2] = 0;

		for (IK ik : mPMD.getIK()) {
			// find parents
			HashMap<Integer, Bone> parents = new HashMap<Integer, Bone>();
			int target = ik.ik_target_bone_index;
			while (target != -1) {
				Bone b = mPMD.getBone().get(target);
				parents.put(target, b);
				target = b.parent;
			}

			int effecter = ik.ik_bone_index;
			while (effecter != -1) {
				Bone b = parents.get(effecter);
				if (b != null) {
					parents.remove(effecter);
				} else {
					b = mPMD.getBone().get(effecter);
					parents.put(effecter, b);
				}
				effecter = b.parent;
			}

			// gather frames
			HashMap<Integer, Integer> frames = new HashMap<Integer, Integer>();
			for (Entry<Integer, Bone> bones : parents.entrySet()) {
				if (bones.getValue().motion != null) {
					for (MotionIndex frame : bones.getValue().motion) {
						frames.put(frame.frame_no, frame.frame_no);
					}
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
				for (Bone b : mPMD.getBone()) {
					setBoneMatrix(b, frame);
				}

				ccdIK1(ik);

				for (int i = 0; i < ik.ik_chain_length; i++) {
					Bone c = mPMD.getBone().get(ik.ik_child_bone_index[i]);
					MotionIndex cm = new MotionIndex();
					cm.frame_no = frame;
					cm.location = location;
					cm.rotation = new float[4];
					cm.rotation[0] = (float) c.quaternion[0]; // calc in ccdIK
					cm.rotation[1] = (float) c.quaternion[1];
					cm.rotation[2] = (float) c.quaternion[2];
					cm.rotation[3] = (float) c.quaternion[3];
					cm.interp = null;
					cm.position = 0;

					ArrayList<MotionIndex> mi = mhash.get(ik.ik_child_bone_index[i]);
					if (mi == null) {
						mi = new ArrayList<MotionIndex>();
						mhash.put(ik.ik_child_bone_index[i], mi);
					}
					mi.add(cm);
				}
			}

			// set motions to bones
			for (Entry<Short, ArrayList<MotionIndex>> entry : mhash.entrySet()) {
				Bone b = mPMD.getBone().get(entry.getKey());
				b.motion = entry.getValue();
				b.current_motion = 0;
			}
		}
	}

	private void setBoneMatrix(Bone b, float idx) {
		MotionPair mp = mVMD.findMotion(b, idx, mMpWork);
		Motion m = mVMD.interpolateLinear(mp, idx, mMwork);
		if (m != null) {
			b.quaternion[0] = m.rotation[0];
			b.quaternion[1] = m.rotation[1];
			b.quaternion[2] = m.rotation[2];
			b.quaternion[3] = m.rotation[3];
			quaternionToMatrix(b.matrix_current, m.rotation);

			if (b.parent == -1) {
				b.matrix_current[12] = m.location[0] + b.head_pos[0];
				b.matrix_current[13] = m.location[1] + b.head_pos[1];
				b.matrix_current[14] = m.location[2] + b.head_pos[2];
			} else {
				Bone p = mPMD.getBone().get(b.parent);
				b.matrix_current[12] = m.location[0] + (b.head_pos[0] - p.head_pos[0]);
				b.matrix_current[13] = m.location[1] + (b.head_pos[1] - p.head_pos[1]);
				b.matrix_current[14] = m.location[2] + (b.head_pos[2] - p.head_pos[2]);
			}
		} else {
			// no VMD info so assume that no rotation and translation are specified
			Matrix.setIdentityM(b.matrix_current, 0);
			quaternionSetIndentity(b.quaternion);
			if (b.parent == -1) {
				Matrix.translateM(b.matrix_current, 0, b.head_pos[0], b.head_pos[1], b.head_pos[2]);
			} else {
				Bone p = mPMD.getBone().get(b.parent);
				Matrix.translateM(b.matrix_current, 0, b.head_pos[0], b.head_pos[1], b.head_pos[2]);
				Matrix.translateM(b.matrix_current, 0, -p.head_pos[0], -p.head_pos[1], -p.head_pos[2]);
			}
		}
	}

	private void updateBoneMatrix(Bone b) {
		if (b.updated == false) {
			if (b.parent != -1) {
				Bone p = mPMD.getBone().get(b.parent);
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
		for (IK ik : mPMD.getIK()) {
			ccdIK1(ik);
		}
	}

	private void ccdIK1(IK ik) {
		Bone effecter = mPMD.getBone().get(ik.ik_bone_index);
		Bone target = mPMD.getBone().get(ik.ik_target_bone_index);

		getCurrentPosition(effecterVecs, effecter);

		for (int i = 0; i < ik.iterations; i++) {
			for (int j = 0; j < ik.ik_chain_length; j++) {
				Bone b = mPMD.getBone().get(ik.ik_child_bone_index[j]);

				clearUpdateFlags(b, target);
				getCurrentPosition(targetVecs, target);

				if (b.is_leg) {
					if (i == 0) {
						Bone base = mPMD.getBone().get(ik.ik_child_bone_index[ik.ik_chain_length - 1]);
						getCurrentPosition(targetInvs, b);
						getCurrentPosition(effecterInvs, base);

						double eff_len = Matrix.length(effecterVecs[0] - effecterInvs[0], effecterVecs[1] - effecterInvs[1], effecterVecs[2] - effecterInvs[2]);
						double b_len = Matrix.length(targetInvs[0] - effecterInvs[0], targetInvs[1] - effecterInvs[1], targetInvs[2] - effecterInvs[2]);
						double t_len = Matrix.length(targetVecs[0] - targetInvs[0], targetVecs[1] - targetInvs[1], targetVecs[2] - targetInvs[2]);

						double angle = Math.acos((eff_len * eff_len - b_len * b_len - t_len * t_len) / (2 * b_len * t_len));
						if (!Double.isNaN(angle)) {
							axis[0] = -1;
							axis[1] = axis[2] = 0;
							makeQuat(mQuatworks, angle, axis);
							quaternionMul(b.quaternion, b.quaternion, mQuatworks);
							quaternionToMatrixPreserveTranslate(b.matrix_current, b.quaternion);
						}
					}
					continue;
				}

				if (Matrix.length(targetVecs[0] - effecterVecs[0], targetVecs[1] - effecterVecs[1], targetVecs[2] - effecterVecs[2]) < 0.001f) {
					// clear all
					for (Bone c : mPMD.getBone()) {
						c.updated = false;
					}
					return;
				}

				float[] current = getCurrentMatrix(b);
				invertM(mMatworks2, 0, current, 0);
				Matrix.multiplyMV(effecterInvs, 0, mMatworks2, 0, effecterVecs, 0);
				Matrix.multiplyMV(targetInvs, 0, mMatworks2, 0, targetVecs, 0);

				// calculate rotation angle/axis
				normalize(effecterInvs);
				normalize(targetInvs);
				double angle = Math.acos(dot(effecterInvs, targetInvs));
				angle *= ik.control_weight;

				if (!Double.isNaN(angle)) {
					cross(axis, targetInvs, effecterInvs);
					normalize(axis);

					// rotateM(mMatworks, 0, b.matrix_current, 0, degree, axis[0], axis[1], axis[2]);
					// System.arraycopy(mMatworks, 0, b.matrix_current, 0, 16);
					if (!Double.isNaN(axis[0]) && !Double.isNaN(axis[1]) && !Double.isNaN(axis[2])) {
						makeQuat(mQuatworks, angle, axis);
						quaternionMul(b.quaternion, b.quaternion, mQuatworks);
						quaternionToMatrixPreserveTranslate(b.matrix_current, b.quaternion);
					}
				}
			}
		}
		// clear all
		for (Bone b : mPMD.getBone()) {
			b.updated = false;
		}
	}

	private void clearUpdateFlags(Bone root, Bone b) {
		while (root != b) {
			b.updated = false;
			if (b.parent != -1) {
				b = mPMD.getBone().get(b.parent);
			} else {
				return;
			}
		}
		root.updated = false;
	}

	public void makeQuat(double[] quat, double angle, float[] axis) {
		double s = Math.sin(angle / 2);

		quat[0] = s * axis[0];
		quat[1] = s * axis[1];
		quat[2] = s * axis[2];
		quat[3] = Math.cos(angle / 2);
	}

	public void cross(float[] d, float[] v1, float[] v2) {
		d[0] = v1[1] * v2[2] - v1[2] * v2[1];
		d[1] = v1[2] * v2[0] - v1[0] * v2[2];
		d[2] = v1[0] * v2[1] - v1[1] * v2[0];
	}

	public void normalize(float[] v) {
		float d = Matrix.length(v[0], v[1], v[2]);
		v[0] /= d;
		v[1] /= d;
		v[2] /= d;
	}

	public float dot(float[] v1, float[] v2) {
		return v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
	}

	public boolean invertM(float[] mInv, int mInvOffset, float[] m, int mOffset) {
		// Invert a 4 x 4 matrix using Cramer's Rule

		// transpose matrix
		Matrix.transposeM(mInvSrcs, 0, m, mOffset);

		// calculate pairs for first 8 elements (cofactors)
		mInvTmps[0] = mInvSrcs[10] * mInvSrcs[15];
		mInvTmps[1] = mInvSrcs[11] * mInvSrcs[14];
		mInvTmps[2] = mInvSrcs[9] * mInvSrcs[15];
		mInvTmps[3] = mInvSrcs[11] * mInvSrcs[13];
		mInvTmps[4] = mInvSrcs[9] * mInvSrcs[14];
		mInvTmps[5] = mInvSrcs[10] * mInvSrcs[13];
		mInvTmps[6] = mInvSrcs[8] * mInvSrcs[15];
		mInvTmps[7] = mInvSrcs[11] * mInvSrcs[12];
		mInvTmps[8] = mInvSrcs[8] * mInvSrcs[14];
		mInvTmps[9] = mInvSrcs[10] * mInvSrcs[12];
		mInvTmps[10] = mInvSrcs[8] * mInvSrcs[13];
		mInvTmps[11] = mInvSrcs[9] * mInvSrcs[12];

		// calculate first 8 elements (cofactors)
		mInvDsts[0] = mInvTmps[0] * mInvSrcs[5] + mInvTmps[3] * mInvSrcs[6] + mInvTmps[4] * mInvSrcs[7];
		mInvDsts[0] -= mInvTmps[1] * mInvSrcs[5] + mInvTmps[2] * mInvSrcs[6] + mInvTmps[5] * mInvSrcs[7];
		mInvDsts[1] = mInvTmps[1] * mInvSrcs[4] + mInvTmps[6] * mInvSrcs[6] + mInvTmps[9] * mInvSrcs[7];
		mInvDsts[1] -= mInvTmps[0] * mInvSrcs[4] + mInvTmps[7] * mInvSrcs[6] + mInvTmps[8] * mInvSrcs[7];
		mInvDsts[2] = mInvTmps[2] * mInvSrcs[4] + mInvTmps[7] * mInvSrcs[5] + mInvTmps[10] * mInvSrcs[7];
		mInvDsts[2] -= mInvTmps[3] * mInvSrcs[4] + mInvTmps[6] * mInvSrcs[5] + mInvTmps[11] * mInvSrcs[7];
		mInvDsts[3] = mInvTmps[5] * mInvSrcs[4] + mInvTmps[8] * mInvSrcs[5] + mInvTmps[11] * mInvSrcs[6];
		mInvDsts[3] -= mInvTmps[4] * mInvSrcs[4] + mInvTmps[9] * mInvSrcs[5] + mInvTmps[10] * mInvSrcs[6];
		mInvDsts[4] = mInvTmps[1] * mInvSrcs[1] + mInvTmps[2] * mInvSrcs[2] + mInvTmps[5] * mInvSrcs[3];
		mInvDsts[4] -= mInvTmps[0] * mInvSrcs[1] + mInvTmps[3] * mInvSrcs[2] + mInvTmps[4] * mInvSrcs[3];
		mInvDsts[5] = mInvTmps[0] * mInvSrcs[0] + mInvTmps[7] * mInvSrcs[2] + mInvTmps[8] * mInvSrcs[3];
		mInvDsts[5] -= mInvTmps[1] * mInvSrcs[0] + mInvTmps[6] * mInvSrcs[2] + mInvTmps[9] * mInvSrcs[3];
		mInvDsts[6] = mInvTmps[3] * mInvSrcs[0] + mInvTmps[6] * mInvSrcs[1] + mInvTmps[11] * mInvSrcs[3];
		mInvDsts[6] -= mInvTmps[2] * mInvSrcs[0] + mInvTmps[7] * mInvSrcs[1] + mInvTmps[10] * mInvSrcs[3];
		mInvDsts[7] = mInvTmps[4] * mInvSrcs[0] + mInvTmps[9] * mInvSrcs[1] + mInvTmps[10] * mInvSrcs[2];
		mInvDsts[7] -= mInvTmps[5] * mInvSrcs[0] + mInvTmps[8] * mInvSrcs[1] + mInvTmps[11] * mInvSrcs[2];

		// calculate pairs for second 8 elements (cofactors)
		mInvTmps[0] = mInvSrcs[2] * mInvSrcs[7];
		mInvTmps[1] = mInvSrcs[3] * mInvSrcs[6];
		mInvTmps[2] = mInvSrcs[1] * mInvSrcs[7];
		mInvTmps[3] = mInvSrcs[3] * mInvSrcs[5];
		mInvTmps[4] = mInvSrcs[1] * mInvSrcs[6];
		mInvTmps[5] = mInvSrcs[2] * mInvSrcs[5];
		mInvTmps[6] = mInvSrcs[0] * mInvSrcs[7];
		mInvTmps[7] = mInvSrcs[3] * mInvSrcs[4];
		mInvTmps[8] = mInvSrcs[0] * mInvSrcs[6];
		mInvTmps[9] = mInvSrcs[2] * mInvSrcs[4];
		mInvTmps[10] = mInvSrcs[0] * mInvSrcs[5];
		mInvTmps[11] = mInvSrcs[1] * mInvSrcs[4];

		// calculate second 8 elements (cofactors)
		mInvDsts[8] = mInvTmps[0] * mInvSrcs[13] + mInvTmps[3] * mInvSrcs[14] + mInvTmps[4] * mInvSrcs[15];
		mInvDsts[8] -= mInvTmps[1] * mInvSrcs[13] + mInvTmps[2] * mInvSrcs[14] + mInvTmps[5] * mInvSrcs[15];
		mInvDsts[9] = mInvTmps[1] * mInvSrcs[12] + mInvTmps[6] * mInvSrcs[14] + mInvTmps[9] * mInvSrcs[15];
		mInvDsts[9] -= mInvTmps[0] * mInvSrcs[12] + mInvTmps[7] * mInvSrcs[14] + mInvTmps[8] * mInvSrcs[15];
		mInvDsts[10] = mInvTmps[2] * mInvSrcs[12] + mInvTmps[7] * mInvSrcs[13] + mInvTmps[10] * mInvSrcs[15];
		mInvDsts[10] -= mInvTmps[3] * mInvSrcs[12] + mInvTmps[6] * mInvSrcs[13] + mInvTmps[11] * mInvSrcs[15];
		mInvDsts[11] = mInvTmps[5] * mInvSrcs[12] + mInvTmps[8] * mInvSrcs[13] + mInvTmps[11] * mInvSrcs[14];
		mInvDsts[11] -= mInvTmps[4] * mInvSrcs[12] + mInvTmps[9] * mInvSrcs[13] + mInvTmps[10] * mInvSrcs[14];
		mInvDsts[12] = mInvTmps[2] * mInvSrcs[10] + mInvTmps[5] * mInvSrcs[11] + mInvTmps[1] * mInvSrcs[9];
		mInvDsts[12] -= mInvTmps[4] * mInvSrcs[11] + mInvTmps[0] * mInvSrcs[9] + mInvTmps[3] * mInvSrcs[10];
		mInvDsts[13] = mInvTmps[8] * mInvSrcs[11] + mInvTmps[0] * mInvSrcs[8] + mInvTmps[7] * mInvSrcs[10];
		mInvDsts[13] -= mInvTmps[6] * mInvSrcs[10] + mInvTmps[9] * mInvSrcs[11] + mInvTmps[1] * mInvSrcs[8];
		mInvDsts[14] = mInvTmps[6] * mInvSrcs[9] + mInvTmps[11] * mInvSrcs[11] + mInvTmps[3] * mInvSrcs[8];
		mInvDsts[14] -= mInvTmps[10] * mInvSrcs[11] + mInvTmps[2] * mInvSrcs[8] + mInvTmps[7] * mInvSrcs[9];
		mInvDsts[15] = mInvTmps[10] * mInvSrcs[10] + mInvTmps[4] * mInvSrcs[8] + mInvTmps[9] * mInvSrcs[9];
		mInvDsts[15] -= mInvTmps[8] * mInvSrcs[9] + mInvTmps[11] * mInvSrcs[10] + mInvTmps[5] * mInvSrcs[8];

		// calculate determinant
		float det = mInvSrcs[0] * mInvDsts[0] + mInvSrcs[1] * mInvDsts[1] + mInvSrcs[2] * mInvDsts[2] + mInvSrcs[3] * mInvDsts[3];

		// calculate matrix inverse
		det = 1 / det;
		for (int j = 0; j < 16; j++)
			mInv[j + mInvOffset] = mInvDsts[j] * det;

		return true;
	}

	private void getCurrentPosition(float v[], Bone b) {
		float[] current = getCurrentMatrix(b);
		Matrix.setIdentityM(mMatworks, 0);
		Matrix.translateM(mMatworks, 0, -b.head_pos[0], -b.head_pos[1], -b.head_pos[2]); // may be removed, multiply by (0, 0, 0)
		Matrix.multiplyMM(mMatworks2, 0, current, 0, mMatworks, 0);
		Matrix.multiplyMV(v, 0, mMatworks2, 0, b.head_pos, 0);
	}

	private float[] getCurrentMatrix(Bone b) {
		updateBoneMatrix(b);
		return b.matrix;
	}

	public void quaternionCreateFromAngleAxis(double[] r, double angle, double[] axis) {
		double halfAngle = 0.5f * angle;
		double sin = Math.sin(halfAngle);
		r[3] = Math.cos(halfAngle);
		r[0] = sin * axis[0];
		r[1] = sin * axis[1];
		r[2] = sin * axis[2];
	}

	public void quaternionSetIndentity(double[] r) {
		r[0] = r[1] = r[2] = 0;
		r[3] = 1;
	}

	public void quaternionMul(double[] res, double[] r, double[] q) {
		double w = r[3], x = r[0], y = r[1], z = r[2];
		double qw = q[3], qx = q[0], qy = q[1], qz = q[2];
		res[0] = x * qw + y * qz - z * qy + w * qx;
		res[1] = -x * qz + y * qw + z * qx + w * qy;
		res[2] = x * qy - y * qx + z * qw + w * qz;
		res[3] = -x * qx - y * qy - z * qz + w * qw;
	}

	public void quaternionNormalize(double[] pvec4Out, double[] pvec4Src) {
		float fSqr = (float) (1.0f / Math.sqrt(pvec4Src[0] * pvec4Src[0] + pvec4Src[1] * pvec4Src[1] + pvec4Src[2] * pvec4Src[2] + pvec4Src[3] * pvec4Src[3]));

		pvec4Out[0] = pvec4Src[0] * fSqr;
		pvec4Out[1] = pvec4Src[1] * fSqr;
		pvec4Out[2] = pvec4Src[2] * fSqr;
		pvec4Out[3] = pvec4Src[3] * fSqr;
	}

	public void quaternionToMatrix(float mat[], float quat[]) {
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

	public void quaternionToMatrixPreserveTranslate(float mat[], double quat[]) {
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

	public void calcToonTexCoord(float x, float y, float z) {
		ByteBuffer tbb = ByteBuffer.allocateDirect(mPMD.numVertex() * 2 * 4);
		tbb.order(ByteOrder.nativeOrder());
		mToonCoordBuffer = tbb.asFloatBuffer();

		float vn[] = new float[6];
		for (int i = 0; i < mPMD.numVertex(); i++) {
			mAllBuffer.position(i * 8);
			mAllBuffer.get(vn);

			float dx, dy, dz, a;

			// Calculate translate effects: assume that light is at (0, 0, 0)
			dx = x + vn[0];
			dy = y + vn[1];
			dz = z + vn[2];

			// normalize
			a = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			dx /= a;
			dy /= a;
			dz /= a;

			// calculate an inner product as a texture coordinate
			float p = (vn[3] * dx + vn[4] * dy + vn[5] * dz);
			// Log.d("Miku", String.format("Vertex=%2.2fx%2.2fx%2.2f, Normal=%2.2fx%2.2fx%2.2f", dx, dy, dz, n.x, n.y, n.z));
			// Log.d("Miku", "V: " + String.valueOf(p));

			mToonCoordBuffer.put(0.5f); // u
			mToonCoordBuffer.put(p); // v
		}
		mToonCoordBuffer.position(0);
		mAllBuffer.position(0);
	}

	public void readAndBindTexture(GL10 gl) {
		gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 1);

		mTexture = new HashMap<String, TexBitmap>();
		for (int i = 0; i < mPMD.numMaterial(); i++) {
			Material mat = mPMD.getMaterial().get(i);
			if (mat.texture != null) {
				if (mTexture.get(mat.texture) == null) {
					// read
					TexBitmap tb = new TexBitmap();

					tb.bmp = loadPicture(mat.texture, 2);
					Log.d("Miku",
							mat.texture + ": " + String.valueOf(tb.bmp.getWidth()) + "x" + String.valueOf(tb.bmp.getHeight()) + " at row size "
									+ String.valueOf(tb.bmp.getRowBytes()) + "byte in " + tb.bmp.getConfig().name());
					mTexture.put(mat.texture, tb);

					// bind
					int tex[] = new int[1];
					gl.glGenTextures(1, tex, 0);
					tb.tex = tex[0];
					gl.glBindTexture(GL10.GL_TEXTURE_2D, tb.tex);
					if (tb.bmp.hasAlpha()) { // workaround
						ByteBuffer buf = ByteBuffer.allocateDirect(tb.bmp.getWidth() * tb.bmp.getHeight() * 4);
						for (int y = 0; y < tb.bmp.getHeight(); y++) {
							for (int x = 0; x < tb.bmp.getWidth(); x++) {
								int pixel = tb.bmp.getPixel(x, y);
								buf.put((byte) ((pixel >> 16) & 0xff));
								buf.put((byte) ((pixel >> 8) & 0xff));
								buf.put((byte) ((pixel >> 0) & 0xff));
								buf.put((byte) ((pixel >> 24) & 0xff));
							}
						}
						buf.position(0);
						gl.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, tb.bmp.getWidth(), tb.bmp.getHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
						buf = null;
					} else {
						GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, tb.bmp, 0);
					}

					int err = gl.glGetError();
					if (err != 0) {
						Log.d("Miku", GLU.gluErrorString(err));
					}
				}
			}
		}
	}

	public void readAndBindTextureGLES20() {
		GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
		mTexture = new HashMap<String, TexBitmap>();
		for (int i = 0; i < mPMD.numMaterial(); i++) {
			Material mat = mPMD.getMaterial().get(i);
			if (mat.texture != null) {
				if (mTexture.get(mat.texture) == null) {
					// read
					TexBitmap tb = new TexBitmap();

					tb.bmp = loadPicture(mat.texture, 2);
					Log.d("Miku",
							mat.texture + ": " + String.valueOf(tb.bmp.getWidth()) + "x" + String.valueOf(tb.bmp.getHeight()) + " at row size "
									+ String.valueOf(tb.bmp.getRowBytes()) + "byte in " + tb.bmp.getConfig().name());
					mTexture.put(mat.texture, tb);

					// bind
					int tex[] = new int[1];
					GLES20.glGenTextures(1, tex, 0);
					tb.tex = tex[0];

					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tb.tex);
					GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
					GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
					GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
					// GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST_MIPMAP_NEAREST);
					GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
					if (tb.bmp.hasAlpha()) { // workaround
						ByteBuffer buf = ByteBuffer.allocateDirect(tb.bmp.getWidth() * tb.bmp.getHeight() * 4);
						for (int y = 0; y < tb.bmp.getHeight(); y++) {
							for (int x = 0; x < tb.bmp.getWidth(); x++) {
								int pixel = tb.bmp.getPixel(x, y);
								buf.put((byte) ((pixel >> 16) & 0xff));
								buf.put((byte) ((pixel >> 8) & 0xff));
								buf.put((byte) ((pixel >> 0) & 0xff));
								buf.put((byte) ((pixel >> 24) & 0xff));
							}
						}
						buf.position(0);
						GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, tb.bmp.getWidth(), tb.bmp.getHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
						buf = null;
					} else {
						GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, tb.bmp, 0);
					}
					// GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

					tb.bmp.recycle();
					int err = GLES20.glGetError();
					if (err != 0) {
						Log.d("Miku", GLU.gluErrorString(err));
					}
				}
			}
		}
	}

	public void readToonTexture() {
		mToon = new ArrayList<TexBitmap>();
		for (int i = 0; i < 11; i++) {
			TexBitmap tb = new TexBitmap();
			tb.bmp = loadPicture(mPMD.getToonFileName(i), 1);
			Log.d("Miku",
					mPMD.getToonFileName(i) + ": " + String.valueOf(tb.bmp.getWidth()) + "x" + String.valueOf(tb.bmp.getHeight()) + " at row size "
							+ String.valueOf(tb.bmp.getRowBytes()) + "byte in " + tb.bmp.getConfig().name());
			mToon.add(tb);
		}
	}

	public void bindToonTexture(GL10 gl) {
		int tex[] = new int[11];
		gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 1);
		gl.glGenTextures(11, tex, 0);

		for (int i = 0; i < 11; i++) {
			TexBitmap tb = mToon.get(i);
			tb.tex = tex[i];

			gl.glBindTexture(GL10.GL_TEXTURE_2D, tb.tex);
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, tb.bmp, 0);
		}
	}

	public void bindToonTextureGLES20() {
		int tex[] = new int[11];
		GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
		GLES20.glGenTextures(11, tex, 0);

		for (int i = 0; i < 11; i++) {
			TexBitmap tb = mToon.get(i);
			tb.tex = tex[i];

			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tb.tex);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, tb.bmp, 0);
			tb.bmp.recycle();
		}
	}

	public Bitmap loadPicture(String file, int scale) {
		Bitmap bmp = null;
		if (file.endsWith(".tga")) {
			try {
				bmp = TgaBitmapFactory.decodeFileCached(file, scale);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			BitmapFactory.Options opt = new BitmapFactory.Options();
			// opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
			opt.inSampleSize = scale;
			bmp = BitmapFactory.decodeFile(file, opt);
		}

		return bmp;
	}

	public void bindBufferGLES20(int vertex, int normal) {
		GLES20.glEnableVertexAttribArray(vertex);
		mAllBuffer.position(0);
		GLES20.glVertexAttribPointer(vertex, 4, GLES20.GL_FLOAT, false, 8 * 4, mAllBuffer);
		checkGlError("drawGLES20 VertexAttribPointer vertex");

		GLES20.glEnableVertexAttribArray(normal);
		mAllBuffer.position(4);
		GLES20.glVertexAttribPointer(normal, 4, GLES20.GL_FLOAT, false, 8 * 4, mAllBuffer);
		checkGlError("drawGLES20 VertexAttribPointer normal");
		mAllBuffer.position(0);
	}

	public void bindBuffer(GL10 gl) {
		GL11Ext gl11Ext = (GL11Ext) gl;

		mAllBuffer.position(0);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 8 * 4, mAllBuffer);

		mAllBuffer.position(3);
		gl.glNormalPointer(GL10.GL_FLOAT, 8 * 4, mAllBuffer);

		gl11Ext.glWeightPointerOES(2, GL10.GL_FLOAT, 0, mWeightBuffer);

		gl.glClientActiveTexture(GL10.GL_TEXTURE0);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mToonCoordBuffer);

		mAllBuffer.position(6);
		gl.glClientActiveTexture(GL10.GL_TEXTURE1);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 8 * 4, mAllBuffer);

		mAllBuffer.position(0);

	}

	private void checkGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e("Miku", op + ": glError " + error);
			throw new RuntimeException(op + ": glError " + error);
		}
	}

	public ArrayList<Bone> getBone() {
		return mPMD.getBone();
	}
}
