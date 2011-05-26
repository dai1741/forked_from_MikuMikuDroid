package jp.gauzau.MikuMikuDroid;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import android.util.Log;

public class MikuModel {
	// model configuration
	public transient String						mFileName;	
	public transient boolean					mAnimation;
	public transient int						mMaxBone;
	public transient CubeArea					mCube;
	public transient boolean					mIsTextureLoaded;
	public transient boolean					mIsOneSkinning;
	public transient String						mBase;
	
	// model data
	public transient FloatBuffer				mToonCoordBuffer;
	public transient ShortBuffer				mIndexBuffer;
	public transient IntBuffer					mIndexBufferI;
	public transient FloatBuffer				mAllBuffer;
	public transient ShortBuffer				mWeightBuffer;
	
	public transient ArrayList<Bone>			mBone;
	public transient ArrayList<Material>		mMaterial;
	public transient ArrayList<Face>			mFace;
	public transient ArrayList<IK>				mIK;
	public transient ArrayList<RigidBody>		mRigidBody;
	public transient ArrayList<Joint>			mJoint;
	public transient ArrayList<String>			mToonFileName;
	
	// generated data
	public transient ArrayList<Material>		mRendarList;
	public transient Face						mFaceBase;
	
	public transient HashMap<String, TexInfo>	mTexture;
	public transient ArrayList<Integer>			mToon;
	public transient ArrayList<Integer>			mLoDIndex;

	public MikuModel() {
		
	}
	
	public MikuModel(String base, ModelFile pmd, int max_bone, boolean animation) {
		init(base, pmd, max_bone, animation);
	}
	
	public MikuModel(String base, ModelBuilder pmd, int max_bone, boolean animation) {
		init(base, pmd, max_bone, animation);
	}

	public MikuModel(String base, ModelFile pmd, int max_bone) {
		init(base, pmd, max_bone, true);
	}

	public void init(String base, ModelFile pmd, int max_bone, boolean animation) {
		mBase			= base;
		mFileName       = pmd.getFileName();
		mMaxBone		= max_bone;
		mAnimation		= animation;
		mIsTextureLoaded= false;
		mAllBuffer		= pmd.getVertexBuffer();
		mIndexBuffer	= pmd.getIndexBufferS();
		mWeightBuffer	= pmd.getWeightBuffer();
		mBone			= pmd.getBone();
		mMaterial		= pmd.getMaterial();
		mFace			= pmd.getFace();
		mIK				= pmd.getIK();
		mRigidBody		= pmd.getRigidBody();
		mJoint			= pmd.getJoint();
		mToonFileName	= pmd.getToonFileName();
		mIsOneSkinning	= pmd.isOneSkinning();
		if(mIsOneSkinning) {
			Log.d("MikuModel", pmd.getFileName() + " has only one skinnings.");			
		}

		if (animation) {
			reconstructFace();
			if(mBone.size() <= mMaxBone) {
				buildBoneRenameIndexAll();
				mRendarList = mMaterial;
				if(mIsOneSkinning) {
					clusterVertex();
				}
			} else {
				reconstructMaterial(mMaxBone);
			}
		} else {
			mRendarList = mMaterial;
			clusterVertex();
			mFaceBase = null;
		}
		mWeightBuffer = null;	// no more use
		pmd.recycle();
	}
	
	public void init(String base, ModelBuilder mb, int max_bone, boolean animation) {
		mBase			= base;
		mFileName       = mb.getFileName();
		mMaxBone		= max_bone;
		mAnimation		= animation;
		mIsTextureLoaded= false;
		mBone			= mb.mBone;
		mMaterial		= mb.mMaterial;
		mFace			= mb.mFace;
		mIK				= mb.mIK;
		mRigidBody		= mb.mRigidBody;
		mJoint			= mb.mJoint;
		mToonFileName	= mb.mToonFileName;
		mIsOneSkinning	= mb.mIsOneSkinning;
		if(mIsOneSkinning) {
			Log.d("MikuModel", mFileName + " has only one skinnings.");			
		}
		
		// assume bg
		mAllBuffer = mb.mVertBuffer.asFloatBuffer();
		mIndexBufferI = mb.mIndexBuffer.asIntBuffer();
		mRendarList = mMaterial;
		clusterVertex();
		mFaceBase = null;
	}

	private void reconstructFace() {
		mFaceBase = null;
	
		// find base face
		if (mFace != null) {
			for (Face s : mFace) {
				if (s.face_type == 0) {
					mFaceBase = s;
					break;
				}
			}
	
			// update base face
			for(int i = 0; i < mFaceBase.face_vert_count; i++) {
				mFaceBase.face_vert_index[i] = mFaceBase.face_vert_index[i] * 8;
			}
		}
	}

	private void clusterVertex() {
		// cluster vertices
		for(Material m: mRendarList) {
			
			// initialize: each 3 vertices becomes one cluster
			m.area = new SphereArea(mAllBuffer, m.weight, mBone);
			int inc = 900;	// or 300?
			if(mIndexBufferI != null) {
				for(int i = 0; i < m.face_vert_count;
				i += m.area.initialSet(mIndexBufferI, m.face_vert_offset + i, i + inc > m.face_vert_count ? m.face_vert_count - i : inc));				
			} else {
				for(int i = 0; i < m.face_vert_count;
				i += m.area.initialSet(mIndexBuffer, m.face_vert_offset + i, i + inc > m.face_vert_count ? m.face_vert_count - i : inc));								
			}
			m.area.recycle();
		}
	}

	private void reconstructMaterial(int max_bone) {
		mRendarList = new ArrayList<Material>();
		HashMap<HashMap<Integer, Integer>, ByteBuffer> rename_pool = new HashMap<HashMap<Integer, Integer>, ByteBuffer>();
		for (Material mat : mMaterial) {
			reconstructMaterial1(mat, 0, rename_pool, max_bone);
		}
	}
	
	private void reconstructMaterial1(Material mat, int offset, HashMap<HashMap<Integer, Integer>, ByteBuffer> rename_pool, int max_bone) {
		HashMap<Integer, Integer> rename = new HashMap<Integer, Integer>();
		int acc = 0;
		for (int j = offset; j < mat.face_vert_count; j += 3) {
			int acc_prev = acc;
			acc = renameBone1(rename, mat.face_vert_offset + j + 0, acc);
			acc = renameBone1(rename, mat.face_vert_offset + j + 1, acc);
			acc = renameBone1(rename, mat.face_vert_offset + j + 2, acc);
			if (acc > max_bone) {
				Material mat_new = buildNewMaterial(mat, offset, j, rename, rename_pool, acc_prev);
				mRendarList.add(mat_new);
				reconstructMaterial1(mat, j, rename_pool, max_bone);
				return;
			}
		}
		Material mat_new = buildNewMaterial(mat, offset, mat.face_vert_count, rename, rename_pool, max_bone);
		mRendarList.add(mat_new);
	}


	private Material buildNewMaterial(Material mat_orig, int offset, int count, HashMap<Integer, Integer> rename, HashMap<HashMap<Integer, Integer>, ByteBuffer> rename_pool, int max_bone) {
		Material mat = new Material(mat_orig);
		mat.face_vert_offset = mat_orig.face_vert_offset + offset;
		mat.face_vert_count  = count - offset;
		mat.bone_num = rename.size();
		
		// find unoverwrapped hash
		for(Entry<HashMap<Integer, Integer>, ByteBuffer> pool: rename_pool.entrySet()) {
			HashMap<Integer, Integer> map = pool.getKey();
			ByteBuffer bb = pool.getValue();
			
			// check mapped
			for(Entry<Integer, Integer> entry: rename.entrySet()) {
				Integer i = map.get(entry.getKey());
				if(i != null) {
					bb = null;
				}
			}
			
			// find free byte buffer
			if(bb != null) {
				rename_pool.remove(map);
				mat.weight = bb;
				buildBoneRenamedWeightBuffers(mat, rename, max_bone);
				
				map.putAll(rename);
				rename_pool.put(map, bb);
//				Log.d("MikuModel", "Reuse buffer");
				return mat;
			}
		}
		
		// allocate new buffer
//		Log.d("MikuModel", "Allocate new buffer");
		allocateWeightBuffer(mat, rename);
		buildBoneRenamedWeightBuffers(mat, rename, max_bone);
		
		HashMap<Integer, Integer> new_map = new HashMap<Integer, Integer>(rename);
		rename_pool.put(new_map, mat.weight);
		
		/*
		Log.d("Miku", "rename Bone for Material #" + String.valueOf(mat_new.face_vart_offset) + ", bones " + String.valueOf(acc));
		for (Entry<Integer, Integer> b : rename.entrySet()) {
			Log.d("Miku", String.format("ID %d: bone %d", b.getValue(), b.getKey()));
		}
		*/
		return mat;
	}
	
	private void allocateWeightBuffer(Material mat, HashMap<Integer, Integer> rename) {
		ByteBuffer rbb = ByteBuffer.allocateDirect(mWeightBuffer.capacity());
		rbb.order(ByteOrder.nativeOrder());
		mat.weight = rbb;
	}
	
	private int[] buildBoneRenameMap(Material mat, HashMap<Integer, Integer> rename, int max_bone) {
		int[] rename_map = new int[mBone.size()];
		for (int i = 0; i < mBone.size(); i++) {
			rename_map[i] = 0; // initialize
		}
		for (Entry<Integer, Integer> b : rename.entrySet()) {
			if (b.getValue() < max_bone) {
				rename_map[b.getKey()] = b.getValue();
			}
		}
		
		return rename_map;
	}

	private void buildBoneRenameInvMap(Material mat, HashMap<Integer, Integer> rename, int max_bone) {
		mat.bone_inv_map = new int[mMaxBone];
		for (int i = 0; i < mMaxBone; i++) {
			mat.bone_inv_map[i] = -1; // initialize
		}
		for (Entry<Integer, Integer> b : rename.entrySet()) {
			if (b.getValue() < max_bone) {
				mat.bone_inv_map[b.getValue()] = b.getKey();
			}
		}
	}

	private void buildBoneRenamedWeightBuffers(Material mat, HashMap<Integer, Integer> rename, int max_bone) {
		buildBoneRenameInvMap(mat, rename, max_bone);
		
		int[] map = buildBoneRenameMap(mat, rename, max_bone);
		
		short[] weight = new short[3];
		for (int i = mat.face_vert_offset; i < mat.face_vert_offset + mat.face_vert_count; i++) {
			int pos = (0x0000ffff & mIndexBuffer.get(i));
			mWeightBuffer.position(pos * 3);
			mWeightBuffer.get(weight);
			mat.weight.position(pos * 3);
			mat.weight.put((byte) map[weight[0]]);
			mat.weight.put((byte) map[weight[1]]);
			mat.weight.put((byte) weight[2]);
		}
	
		mat.weight.position(0);
	}
	
	private void buildBoneRenameIndexAll() {
		ByteBuffer rbb = ByteBuffer.allocateDirect(mWeightBuffer.capacity());
		rbb.order(ByteOrder.nativeOrder());

		mWeightBuffer.position(0);
		for (int i = 0; i < mWeightBuffer.capacity(); i++) {
			rbb.put((byte) mWeightBuffer.get());
		}
		rbb.position(0);
	
		for (Material m : mMaterial) {
			m.weight = rbb;
			m.bone_inv_map = null;
			m.bone_num = mBone.size();
		}
	}

	private int renameBone1(HashMap<Integer, Integer> rename, int veridx, int acc) {
		int pos = (0x0000ffff & mIndexBuffer.get(veridx));
		mWeightBuffer.position(pos * 3);
		short bone_num_0 = mWeightBuffer.get();
		short bone_num_1 = mWeightBuffer.get();
		
		Integer i = rename.get((int)bone_num_0);
		if (i == null) {
			rename.put((int) bone_num_0, acc++);
		}
		i = rename.get((int)bone_num_1);
		if (i == null) {
			rename.put((int) bone_num_1, acc++);
		}
	
		return acc;
	}

	public void calcToonTexCoord(float[] light_dir) {
		ByteBuffer tbb = ByteBuffer.allocateDirect(mAllBuffer.capacity() / 8 * 2 * 4);
		tbb.order(ByteOrder.nativeOrder());
		mToonCoordBuffer = tbb.asFloatBuffer();

		float vn[] = new float[6];
		for (int i = 0; i < mAllBuffer.capacity() / 8; i++) {
			mAllBuffer.position(i * 8);
			mAllBuffer.get(vn);

			float p = (vn[3] * light_dir[0] + vn[4] * light_dir[1] + vn[5] * light_dir[2]);
			mToonCoordBuffer.put(0.5f); // u
			mToonCoordBuffer.put(p); // v
		}
		mToonCoordBuffer.position(0);
		mAllBuffer.position(0);
	}
}