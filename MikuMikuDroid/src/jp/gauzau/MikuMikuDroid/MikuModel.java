package jp.gauzau.MikuMikuDroid;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
	public transient FloatBuffer				mWeightBuffer;
	public transient IntBuffer					mIndexBuffer;
	public transient FloatBuffer				mAllBuffer;
	
	public transient ArrayList<Bone>			mBone;
	public transient ArrayList<Material>		mMaterial;
	public transient ArrayList<Face>			mFace;
	public transient ArrayList<IK>				mIK;
	public transient ArrayList<RigidBody>		mRigidBody;
	public transient ArrayList<Joint>			mJoint;
	public transient ArrayList<String>			mToonFileName;
	
	// generated data
	public transient ArrayList<Material>		mRendarList;
//	public transient int[]						mIndexMaps;
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

//		mMaterial = mergeMaterials(mMaterial);
//		makeLoDIndex(pmd);
		makeVertexBuffers(pmd);
//		makeIndexSortedVertexBuffers(pmd);
		if (animation) {
			reconstructFace();
			if(mBone.size() <= mMaxBone) {
				buildBoneRenameIndexAll(pmd, mMaxBone);
				mRendarList = mMaterial;
				if(mIsOneSkinning) {
					clusterVertex();
				}
				pmd.recycleVertex();
			} else {
				pmd.recycleVertex();
				reconstructMaterial(pmd, mMaxBone);
			}
		} else {
			mRendarList = mMaterial;
			clusterVertex();
			mFaceBase = null;
			pmd.recycleVertex();
		}
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
		mIndexBuffer = mb.mIndexBuffer.asIntBuffer();
		mRendarList = mMaterial;
		clusterVertex();
		mFaceBase = null;
	}
	
	private void makeVertexBuffers(ModelFile pmd) {
		// vertex, normal, texture buffer
		ByteBuffer abb = ByteBuffer.allocateDirect(pmd.getVertex().size() * 8 * 4);
		abb.order(ByteOrder.nativeOrder());
		mAllBuffer = abb.asFloatBuffer();
	
		// weight buffer
		ByteBuffer wbb = ByteBuffer.allocateDirect(pmd.getVertex().size() * 2 * 4);
		wbb.order(ByteOrder.nativeOrder());
		mWeightBuffer = wbb.asFloatBuffer();
	
		// index buffer
		ByteBuffer ibb = ByteBuffer.allocateDirect(pmd.getIndex().size() * 4);
		ibb.order(ByteOrder.nativeOrder());
		mIndexBuffer = ibb.asIntBuffer();
	
		// reference cube
		mCube = new CubeArea();
	
		// sort vertex by index order
		ArrayList<Integer> index;
		index = pmd.getIndex();
		for (Integer idx : index) {
			mIndexBuffer.put(idx);
		}
		for(Vertex ver: pmd.getVertex()) {
			// vertex, normal, texture
			mAllBuffer.put(ver.pos);
			mAllBuffer.put(ver.normal);
			mAllBuffer.put(ver.uv);

			// weight
			mWeightBuffer.put(ver.bone_weight / 100.0f);
			mWeightBuffer.put((100 - ver.bone_weight) / 100.0f);

			// update cube
			mCube.set(ver.pos);
		}
		mIndexBuffer.position(0);
		mWeightBuffer.position(0);
		mAllBuffer.position(0);
	
		mCube.logOutput("PMDParser");
	}
	
	private void makeIndexSortedVertexBuffers(ModelFile pmd) {
		int[] index_map = new int[pmd.getVertex().size()];
		for (int i = 0; i < index_map.length; i++) {
			index_map[i] = -1; // not mapped yet
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
		ByteBuffer ibb = ByteBuffer.allocateDirect(pmd.getIndex().size() * 4);
		ibb.order(ByteOrder.nativeOrder());
		mIndexBuffer = ibb.asIntBuffer();
	
		// reference cube
		mCube = new CubeArea();
	
		// sort vertex by index order
		ArrayList<Integer> index;
		index = pmd.getIndex();
		for (Integer idx : index) {
			if (index_map[idx] < 0) { // not mapped yet
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
				index_map[idx] = vc++;
			}
	
			mIndexBuffer.put(index_map[idx]);
		}
		mIndexBuffer.position(0);
		mWeightBuffer.position(0);
		mAllBuffer.position(0);
	
		mCube.logOutput("PMDParser");
	}

	private ArrayList<Material> mergeMaterials(ArrayList<Material> ma) {
		ArrayList<Material> man = new ArrayList<Material>();
		Material prev = null;
		for(Material m: ma) {
			if(prev != null) {
				if(prev.equals(m)) {
					Material n = new Material(prev);
					n.face_vert_count += m.face_vert_count;
					prev = n;					
				} else {
					man.add(prev);
					prev = m;
				}
			} else {
				prev = m;
			}
		}
		man.add(prev);
		return man;
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
			for(int i = 0; i < m.face_vert_count;
				i += m.area.initialSet(mIndexBuffer, m.face_vert_offset + i, i + inc > m.face_vert_count ? m.face_vert_count - i : inc));
			m.area.recycle();
		}
	}
	
	private void makeLoDIndex(ModelFile pmd) {
		HashSet<Integer> red = new HashSet<Integer>();
		HashSet<Integer> eff = new HashSet<Integer>();
		HashMap<Integer, Integer> vset = new HashMap<Integer, Integer>();
		
		ArrayList<Integer> idx = pmd.getIndex();
		for(int i = 0; i < idx.size(); i += 3) {
			int v0 = idx.get(i + 0);
			int v1 = idx.get(i + 1);
			int v2 = idx.get(i + 2);
			
			boolean r0 = red.contains(v0);
			boolean r1 = red.contains(v1);
			boolean r2 = red.contains(v2);
			
			if(r0 || r1 || r2) {
				if(!r0) {
					eff.add(v0);
				}
				if(!r1) {
					eff.add(v1);
				}
				if(!r2) {
					eff.add(v2);
				}
			} else {
				boolean e0 = eff.contains(v0);
				boolean e1 = eff.contains(v1);
				boolean e2 = eff.contains(v2);
				
				if(!e0 && !e1) {
					red.add(v0);
					red.add(v1);
					vset.put(v0, v1);
					
					eff.add(v2);
				} else if(!e1 && !e2) {
					red.add(v1);
					red.add(v2);
					vset.put(v1, v2);
					
					eff.add(v0);
				} else if(!e2 && !e0) {
					red.add(v2);
					red.add(v0);
					vset.put(v2, v0);
					
					eff.add(v1);
				}
			}
		}
	
		mLoDIndex = new ArrayList<Integer>();
		for(Material mat: pmd.getMaterial()) {
			mat.lod_face_vert_offset = mLoDIndex.size();
			for(int i = mat.face_vert_offset; i < mat.face_vert_offset + mat.face_vert_count; i += 3) {
				int v0 = idx.get(i + 0);
				int v1 = idx.get(i + 1);
				int v2 = idx.get(i + 2);
				
				boolean r0 = red.contains(v0);
				boolean r1 = red.contains(v1);
				boolean r2 = red.contains(v2);
				
				if(r0 && !r1 && !r2) {
					Integer ren = vset.get(v0);
					if(ren != null) {
						mLoDIndex.add(ren);
					} else {
						mLoDIndex.add(v0);
					}
					mLoDIndex.add(v1);
					mLoDIndex.add(v2);					
				} else if(!r0 && r1 && !r2) {
					mLoDIndex.add(v0);
					Integer ren = vset.get(v1);
					if(ren != null) {
						mLoDIndex.add(ren);
					} else {
						mLoDIndex.add(v1);
					}
					mLoDIndex.add(v2);					
				} else if(!r0 && !r1 && r2) {
					mLoDIndex.add(v0);
					mLoDIndex.add(v1);
					Integer ren = vset.get(v2);
					if(ren != null) {
						mLoDIndex.add(ren);
					} else {
						mLoDIndex.add(v2);
					}
				} else if(!r0 && !r1 && !r2) {
					mLoDIndex.add(v0);
					mLoDIndex.add(v1);
					mLoDIndex.add(v2);
				}
			}
			mat.lod_face_vert_count = mLoDIndex.size() - mat.lod_face_vert_offset;
			Log.d("MikuModel", String.format("Material     offset %d, count %d", mat.face_vert_offset, mat.face_vert_count));
			Log.d("MikuModel", String.format("Material LoD offset %d, count %d", mat.lod_face_vert_offset, mat.lod_face_vert_count));
		}
	}


	private void reconstructMaterial(ModelFile parser, int max_bone) {
		mRendarList = new ArrayList<Material>();
		HashMap<HashMap<Integer, Integer>, ByteBuffer> rename_pool = new HashMap<HashMap<Integer, Integer>, ByteBuffer>();
		for (Material mat : mMaterial) {
			reconstructMaterial1(parser, mat, 0, rename_pool, max_bone);
		}
	}
	
	private void reconstructMaterial1(ModelFile pmd, Material mat, int offset, HashMap<HashMap<Integer, Integer>, ByteBuffer> rename_pool, int max_bone) {
		ArrayList<Vertex> ver = pmd.getVertex();
		HashMap<Integer, Integer> rename = new HashMap<Integer, Integer>();
		int acc = 0;
		for (int j = offset; j < mat.face_vert_count; j += 3) {
			acc = renameBone1(pmd, rename, mat.face_vert_offset + j + 0, ver, acc);
			acc = renameBone1(pmd, rename, mat.face_vert_offset + j + 1, ver, acc);
			acc = renameBone1(pmd, rename, mat.face_vert_offset + j + 2, ver, acc);
			if (acc > max_bone) {
				Material mat_new = buildNewMaterial(pmd, mat, offset, j, rename, rename_pool, max_bone);
				mRendarList.add(mat_new);
				reconstructMaterial1(pmd, mat, j, rename_pool, max_bone);
				return;
			}
		}
		Material mat_new = buildNewMaterial(pmd, mat, offset, mat.face_vert_count, rename, rename_pool, max_bone);
		mRendarList.add(mat_new);
	}


	private Material buildNewMaterial(ModelFile pmd, Material mat_orig, int offset, int count, HashMap<Integer, Integer> rename, HashMap<HashMap<Integer, Integer>, ByteBuffer> rename_pool, int max_bone) {
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
				buildBoneRenamedWeightBuffers(pmd, mat, rename, max_bone);
				
				map.putAll(rename);
				rename_pool.put(map, bb);
//				Log.d("MikuModel", "Reuse buffer");
				return mat;
			}
		}
		
		// allocate new buffer
//		Log.d("MikuModel", "Allocate new buffer");
		buildNewBoneRenameHash(pmd, mat, rename);
		buildBoneRenamedWeightBuffers(pmd, mat, rename, max_bone);
		
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
	
	private void buildNewBoneRenameHash(ModelFile pmd, Material mat, HashMap<Integer, Integer> rename) {
		ByteBuffer rbb = ByteBuffer.allocateDirect(pmd.getVertex().size() * 3);
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

	private void buildBoneRenamedWeightBuffers(ModelFile pmd, Material mat, HashMap<Integer, Integer> rename, int max_bone) {
		buildBoneRenameInvMap(mat, rename, max_bone);
		
		int[] map = buildBoneRenameMap(mat, rename, max_bone);
		
		for (int i = mat.face_vert_offset; i < mat.face_vert_offset + mat.face_vert_count; i++) {
			int pos = pmd.getIndex().get(i);
			Vertex ver = pmd.getVertex().get(pos);
			mat.weight.position(pos * 3);
			mat.weight.put((byte) map[ver.bone_num_0]);
			mat.weight.put((byte) map[ver.bone_num_1]);
			mat.weight.put(ver.bone_weight);
		}
	
		mat.weight.position(0);
	}
	
	private void buildBoneRenameIndexAll(ModelFile pmd, int max_bone) {
		ByteBuffer rbb = ByteBuffer.allocateDirect(pmd.getVertex().size() * 3);
		rbb.order(ByteOrder.nativeOrder());
	
		for (int i = 0; i < pmd.getVertex().size(); i++) {
			int pos = pmd.getIndex().get(i);
			Vertex v = pmd.getVertex().get(pos);
			rbb.position(pos * 3);
			rbb.put((byte) v.bone_num_0);
			rbb.put((byte) v.bone_num_1);
			rbb.put((byte) v.bone_weight);				
		}
		rbb.position(0);
	
		for (Material m : mMaterial) {
			m.weight = rbb;
			m.bone_inv_map = null;
			m.bone_num = pmd.getBone().size();
		}
	}

	private int renameBone1(ModelFile pmd, HashMap<Integer, Integer> rename, int veridx, ArrayList<Vertex> ver, int acc) {
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
}