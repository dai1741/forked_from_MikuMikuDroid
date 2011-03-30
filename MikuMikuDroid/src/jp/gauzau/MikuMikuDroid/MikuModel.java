package jp.gauzau.MikuMikuDroid;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class MikuModel implements Serializable, SerializableExt {
	private static final long serialVersionUID = -9127943692220369080L;
	
	// model configuration
	public transient String mFileName;	
	public transient boolean mAnimation;
	public transient int mRenameNum;
	public transient int mRenameBone;
	public transient CubeArea mCube;
	public transient boolean mIsTextureLoaded;
	public transient String mBase;
	
	// model data
	public transient FloatBuffer mToonCoordBuffer;
	public transient FloatBuffer mWeightBuffer;
	public transient ShortBuffer mIndexBuffer;
	public transient FloatBuffer mAllBuffer;
	
	public transient ArrayList<Bone> mBone;
	public transient ArrayList<Material> mMaterial;
	public transient ArrayList<Face> mFace;
	public transient ArrayList<IK> mIK;
	public transient ArrayList<RigidBody> mRigidBody;
	public transient ArrayList<Joint> mJoint;
	public transient ArrayList<String> mToonFileName;
	
	// generated data
	public transient ArrayList<Material> mRendarList;
	public transient int[] mIndexMaps;
	public transient Face mFaceBase;
	
	public transient HashMap<String, Integer> mTexture;
	public transient ArrayList<Integer> mToon;

	public MikuModel() {
		
	}
	
	public MikuModel(String base, PMDParser pmd, int rename_num, int rename_bone, boolean animation) {
		init(base, pmd, rename_num, rename_bone, animation);
	}

	public MikuModel(String base, PMDParser pmd, int rename_num, int rename_bone) {
		init(base, pmd, rename_num, rename_bone, true);
	}

	public void init(String base, PMDParser pmd, int rename_num, int rename_bone, boolean animation) {
		mBase			= base;
		mFileName       = pmd.getFileName();
		mRenameNum		= rename_num;
		mRenameBone		= rename_bone;
		mAnimation		= animation;
		mIsTextureLoaded= false;
		mBone			= pmd.getBone();
		mMaterial		= pmd.getMaterial();
		mFace			= pmd.getFace();
		mIK				= pmd.getIK();
		mRigidBody		= pmd.getRigidBody();
		mJoint			= pmd.getJoint();
		mToonFileName	= pmd.getToonFileName();
		
		makeIndexSortedBuffers(pmd);
		if (animation) {
			reconstructFace();
			pmd.recycleVertex();
			reconstructMaterial(pmd, mRenameBone);
			
			// release unused data
			for(Material rb: mRendarList) {
				rb.rename_hash_size = rb.rename_hash.size();
				rb.rename_hash = null;
				rb.rename_map = null;
			}
		} else {
			mFaceBase = null;
			pmd.recycleVertex();
			buildBoneNoMotionRenameIndex(pmd);
		}
		pmd.recycle();
	}

	void reconstructFace() {
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
				mFaceBase.face_vert_index[i] = mIndexMaps[mFaceBase.face_vert_index[i]] * 8;
			}
		}
	}

	void makeIndexSortedBuffers(PMDParser pmd) {
		mIndexMaps = new int[pmd.getVertex().size()];
		for (int i = 0; i < mIndexMaps.length; i++) {
			mIndexMaps[i] = -1; // not mapped yet
		}
		int vc = 0;
	
		// vertex, normal, texture buffer
		ByteBuffer abb = ByteBuffer.allocateDirect(pmd.getVertex().size() * 8 * 4);
		abb.order(ByteOrder.nativeOrder());
		mAllBuffer = abb.asFloatBuffer();
	
		// weight buffer
		ByteBuffer wbb = ByteBuffer.allocateDirect(pmd.getVertex().size() * 2 * 4);
		wbb.order(ByteOrder.nativeOrder());
		mWeightBuffer = wbb.asFloatBuffer();
	
		// index buffer
		ByteBuffer ibb = ByteBuffer.allocateDirect(pmd.getIndex().size() * 2);
		ibb.order(ByteOrder.nativeOrder());
		mIndexBuffer = ibb.asShortBuffer();
	
		// reference cube
		mCube = new CubeArea();
	
		// sort vertex by index order
		for (Integer idx : pmd.getIndex()) {
			if (mIndexMaps[idx] < 0) { // not mapped yet
				Vertex ver = pmd.getVertex().get(idx);
	
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

	void reconstructMaterial(PMDParser parser, int max_bone) {
		mRendarList = new ArrayList<Material>();
		HashMap<HashMap<Integer, Integer>, ByteBuffer> rename_pool = new HashMap<HashMap<Integer, Integer>, ByteBuffer>();
		for (Material mat : mMaterial) {
			reconstructMaterial1(parser, mat, 0, rename_pool, max_bone);
		}
	}
	
	void reconstructMaterial1(PMDParser pmd, Material mat, int offset, HashMap<HashMap<Integer, Integer>, ByteBuffer> rename_pool, int max_bone) {
		Material mat_new = new Material(mat);
		mat_new.face_vart_offset = mat.face_vart_offset + offset;

		ArrayList<Vertex> ver = pmd.getVertex();
		HashMap<Integer, Integer> rename = new HashMap<Integer, Integer>();
		int acc = 0;
		for (int j = offset; j < mat.face_vert_count; j += 3) {
			acc = renameBone1(pmd, rename, mat.face_vart_offset + j + 0, ver, acc);
			acc = renameBone1(pmd, rename, mat.face_vart_offset + j + 1, ver, acc);
			acc = renameBone1(pmd, rename, mat.face_vart_offset + j + 2, ver, acc);
			if (acc > max_bone) {
				mat_new.face_vert_count = j - offset;
				buildBoneRenameHash(pmd, mat_new, rename, rename_pool, max_bone);
				mRendarList.add(mat_new);
				/*
				Log.d("Miku", "rename Bone for Material #" + String.valueOf(mat_new.face_vart_offset) + ", bones " + String.valueOf(acc));
				for (Entry<Integer, Integer> b : rename.entrySet()) {
					Log.d("Miku", String.format("ID %d: bone %d", b.getValue(), b.getKey()));
				}
				*/

				reconstructMaterial1(pmd, mat, j, rename_pool, max_bone);
				return;
			}
		}
		mat_new.face_vert_count = mat.face_vert_count - offset;
		buildBoneRenameHash(pmd, mat_new, rename, rename_pool, max_bone);
		mRendarList.add(mat_new);

		/*
		Log.d("Miku", "rename Bone for Material #" + String.valueOf(mat_new.face_vart_offset) + ", bones " + String.valueOf(acc));
		for (Entry<Integer, Integer> b : rename.entrySet()) {
			Log.d("Miku", String.format("ID %d: bone %d", b.getValue(), b.getKey()));
		}
		*/
	}


	void buildBoneRenameHash(PMDParser pmd, Material mat, HashMap<Integer, Integer> rename, HashMap<HashMap<Integer, Integer>, ByteBuffer> rename_pool, int max_bone) {
		
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
				
				mat.rename_hash = rename;
				mat.rename_index = bb;
				buildBoneRenameMap(mat, max_bone);
				buildBoneRenameInvMap(mat, max_bone);
				buildBoneRenameIndex(pmd, mat, max_bone);
				
				map.putAll(rename);
				rename_pool.put(map, bb);
//				Log.d("MikuModel", "Reuse buffer");
				return ;
			}
		}
		
		// allocate new buffer
//		Log.d("MikuModel", "Allocate new buffer");
		buildNewBoneRenameHash(pmd, mat, rename);
		buildBoneRenameMap(mat, max_bone);
		buildBoneRenameInvMap(mat, max_bone);
		buildBoneRenameIndex(pmd, mat, max_bone);
		
		HashMap<Integer, Integer> new_map = new HashMap<Integer, Integer>(mat.rename_hash);
		rename_pool.put(new_map, mat.rename_index);
		
		return ;
	}
	
	void buildNewBoneRenameHash(PMDParser pmd, Material mat, HashMap<Integer, Integer> rename) {
		mat.rename_hash = rename;
		
		ByteBuffer rbb = ByteBuffer.allocateDirect(pmd.getVertex().size() * 3 * 2);
		rbb.order(ByteOrder.nativeOrder());
		mat.rename_index = rbb;
	}
	
	void buildBoneRenameMap(Material mat, int max_bone) {
		mat.rename_map = new int[mRenameNum];
		for (int i = 0; i < mRenameNum; i++) {
			mat.rename_map[i] = 0; // initialize
		}
		for (Entry<Integer, Integer> b : mat.rename_hash.entrySet()) {
			if (b.getValue() < max_bone) {
				mat.rename_map[b.getKey()] = b.getValue();
			}
		}
	}

	void buildBoneRenameInvMap(Material mat, int max_bone) {
		mat.rename_inv_map = new int[mRenameBone];
		for (int i = 0; i < mRenameBone; i++) {
			mat.rename_inv_map[i] = -1; // initialize
		}
		for (Entry<Integer, Integer> b : mat.rename_hash.entrySet()) {
			if (b.getValue() < max_bone) {
				mat.rename_inv_map[b.getValue()] = b.getKey();
			}
		}
	}

	void buildBoneRenameIndex(PMDParser pmd, Material mat, int max_bone) {
		for (int i = mat.face_vart_offset; i < mat.face_vart_offset + mat.face_vert_count; i++) {
			int pos = pmd.getIndex().get(i);
			if (mIndexMaps[pos] >= 0) {
				Vertex ver = pmd.getVertex().get(pos);
				mat.rename_index.position(mIndexMaps[pos] * 3);
				mat.rename_index.put((byte) mat.rename_map[ver.bone_num_0]);
				mat.rename_index.put((byte) mat.rename_map[ver.bone_num_1]);
				mat.rename_index.put(ver.bone_weight);
			}
		}
	
		mat.rename_index.position(0);
	}

	void buildBoneNoMotionRenameIndex(PMDParser pmd) {
		ByteBuffer rbb = ByteBuffer.allocateDirect(pmd.getVertex().size() * 3 * 2);
		rbb.order(ByteOrder.nativeOrder());
	
		for (int i = 0; i < pmd.getVertex().size(); i++) {
			rbb.put((byte) 0);
			rbb.put((byte) 0);
			rbb.put((byte) 100);
		}
		rbb.position(0);
	
		for (Material m : mMaterial) {
			m.rename_index = rbb;
		}
	}

	int renameBone1(PMDParser pmd, HashMap<Integer, Integer> rename, int veridx, ArrayList<Vertex> ver, int acc) {
		int idx = ver.get(pmd.getIndex().get(veridx)).bone_num_0;
		Integer i = rename.get(idx);
		if (i == null) {
			rename.put(idx, acc++);
		}
		idx = ver.get(pmd.getIndex().get(veridx)).bone_num_1;
		i = rename.get(idx);
		if (i == null) {
			rename.put(idx, acc++);
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

	public MikuModel create() {
		return new MikuModel();
	}
	
	public void read(ObjectInputStream is) throws IOException, ClassNotFoundException {
		mFileName  = is.readUTF();
		mAnimation = is.readBoolean();
		mRenameNum = is.readInt();
		mRenameBone = is.readInt();

		// all
		int len = is.readInt();
		ByteBuffer tmp = ByteBuffer.allocateDirect(len*4);
		tmp.order(ByteOrder.nativeOrder());
		mAllBuffer = tmp.asFloatBuffer();
		for(int i = 0; i < len; i++) {
			mAllBuffer.put(is.readFloat());
		}
		mAllBuffer.position(0);
		
		// weight
		len = is.readInt();
		tmp = ByteBuffer.allocateDirect(len*4);
		tmp.order(ByteOrder.nativeOrder());
		mWeightBuffer = tmp.asFloatBuffer();
		for(int i = 0; i < len; i++) {
			mWeightBuffer.put(is.readByte());
		}
		mWeightBuffer.position(0);
		
		// index
		len = is.readInt();
		tmp = ByteBuffer.allocateDirect(len*2);
		tmp.order(ByteOrder.nativeOrder());
		mIndexBuffer = tmp.asShortBuffer();
		for(int i = 0; i < len; i++) {
			mIndexBuffer.put(is.readShort());
		}
		mIndexBuffer.position(0);

		mBone = (ArrayList<Bone>)is.readObject();
		mMaterial = ObjRW.readArrayList(is, new Material());
		mFace = (ArrayList<Face>)is.readObject();
		mIK = (ArrayList<IK>)is.readObject();
		mRigidBody = (ArrayList<RigidBody>)is.readObject();
		mJoint = (ArrayList<Joint>)is.readObject();
		mToonFileName = (ArrayList<String>)is.readObject();
		
		mRendarList = ObjRW.readArrayList(is, new Material());
		mIndexMaps = ObjRW.readIntA(is);
		mFaceBase = (Face)is.readObject();
		
		// re-allocate memory
		for(Bone b: mBone) {
			b.matrix = new float[16];
			b.matrix_current = new float[16];
			b.quaternion = new double[4];
		}
		for(RigidBody r: mRigidBody) {
			r.cur_location = new float[4];
			r.cur_r = new double[4];
			r.cur_v = new double[4];
			r.cur_a = new double[4];
			r.tmp_r = new double[4];
			r.tmp_v = new double[4];
			r.tmp_a = new double[4];
			r.prev_r = new double[4];
		}
	}
	
	
	public void write(ObjectOutputStream os) throws IOException {
		os.writeUTF(mFileName);
		os.writeBoolean(mAnimation);
		os.writeInt(mRenameNum);
		os.writeInt(mRenameBone);

		// buffers
		os.writeInt(mAllBuffer.capacity());
		mAllBuffer.position(0);
		for(int i = 0; i < mAllBuffer.capacity(); i++) {
			os.writeFloat(mAllBuffer.get());
		}
		mAllBuffer.position(0);
		
		os.writeInt(mWeightBuffer.capacity());
		mWeightBuffer.position(0);
		for(int i = 0; i < mWeightBuffer.capacity(); i++) {
			os.writeByte((byte) mWeightBuffer.get());
		}
		mWeightBuffer.position(0);

		os.writeInt(mIndexBuffer.capacity());
		mIndexBuffer.position(0);
		for(int i = 0; i < mIndexBuffer.capacity(); i++) {
			os.writeShort(mIndexBuffer.get());
		}
		mIndexBuffer.position(0);
		os.reset();
		os.flush();
		
		os.writeObject(mBone);
		ObjRW.writeArrayList(os, mMaterial);
		os.writeObject(mFace);
		os.writeObject(mIK);
		os.writeObject(mRigidBody);
		os.writeObject(mJoint);
		os.writeObject(mToonFileName);
		
		ObjRW.writeArrayList(os, mRendarList);
		ObjRW.writeIntA(os, mIndexMaps);
		os.writeObject(mFaceBase);
	}

	private void writeObject(ObjectOutputStream os) throws IOException {
		os.defaultWriteObject();
		write(os);
	}

	private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
		is.defaultReadObject();
		read(is);
	}
	
}