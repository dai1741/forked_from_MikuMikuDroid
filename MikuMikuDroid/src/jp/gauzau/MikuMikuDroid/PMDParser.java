package jp.gauzau.MikuMikuDroid;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;

public class PMDParser extends ParserBase {
	private boolean mIsPmd;
	private String mModelName;
	private String mDescription;
	private int mVertexNum;
	private ArrayList<Vertex> mVertex;
	private int mIndexNum;
	private ArrayList<Integer> mIndex;
	private int mMaterialNum;
	private ArrayList<Material> mMaterial;
	private int mBoneNum;
	private ArrayList<Bone> mBone;

	private int mIKNum;
	private ArrayList<IK> mIK;
	private short mFaceNum;
	private ArrayList<Face> mFace;
	private byte mSkinDispNum;
	private ArrayList<Short> mSkinDisp;
	private byte mBoneDispNameNum;
	private ArrayList<String> mBoneDispName;
	private int mBoneDispNum;
	private ArrayList<BoneDisp> mBoneDisp;
	private byte mHasEnglishName;
	private String mEnglishModelName;
	private String mEnglishComment;
	private int mEnglishBoneListNum;
	private ArrayList<String> mEnglishBoneName;
	private int mEnglishSkinListNum;
	private ArrayList<String> mEnglishSkinName;
	private ArrayList<String> mToonFileName;
	private ArrayList<String> mEnglishBoneDispName;
	private byte mEnglishBoneDispNameNum;
	private int mRigidBodyNum;
	private ArrayList<RigidBody> mRigidBody;
	private int mJointNum;
	private ArrayList<Joint> mJoint;

	public PMDParser(String file) throws IOException {
		super(file);
		mIsPmd = false;
		File f = new File(file);
		String path = f.getParent() + "/";

		try {
			parsePMDHeader();
			parsePMDVertexList();
			parsePMDIndexList();
			parsePMDMaterialList(path);
			parsePMDBoneList();
			parsePMDIKList();
			parsePMDFaceList();
			parsePMDSkinDisp();
			parsePMDBoneDispName();
			parsePMDBoneDisp();
			if (!isEof()) {
				parsePMDEnglish();
				parsePMDToonFileName(path);
				parsePMDRigidBody();
				parsePMDJoint();
			} else {
				mToonFileName = new ArrayList<String>(11);
				mToonFileName.add(0, "/sdcard/MikuMikuDroid/Data/toon0.bmp");
				for (int i = 0; i < 10; i++) {
					String str = String.format("/sdcard/MikuMikuDroid/Data/toon%02d.bmp", i + 1);
					Log.d("PMDParser", str);
					mToonFileName.add(i + 1, str);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
			mIsPmd = false;
		}
	}

	private void parsePMDJoint() {
		mJointNum = getInt();
		Log.d("PMDParser", "Joint: " + String.valueOf(mJointNum));
		mJoint = new ArrayList<Joint>(mJointNum);
		for(int i = 0; i < mJointNum; i++) {
			Joint j = new Joint();
			j.name				= getString(20);
			j.rigidbody_a		= getInt();
			j.rigidbody_b		= getInt();
			j.position			= new float[3];
			j.rotation			= new float[3];
			j.const_position_1	= new float[3];
			j.const_position_2	= new float[3];
			j.const_rotation_1	= new float[3];
			j.const_rotation_2	= new float[3];
			j.spring_position	= new float[3];
			j.spring_rotation	= new float[3];
			
			getFloat(j.position);
			getFloat(j.rotation);
			getFloat(j.const_position_1);
			getFloat(j.const_position_2);
			getFloat(j.const_rotation_1);
			getFloat(j.const_rotation_2);
			getFloat(j.spring_position);
			getFloat(j.spring_rotation);
			
			mJoint.add(j);
		}
	}

	private void parsePMDRigidBody() {
		mRigidBodyNum = getInt();
		Log.d("PMDParser", "RigidBody: " + String.valueOf(mRigidBodyNum));
		mRigidBody = new ArrayList<RigidBody>(mRigidBodyNum);
		for(int i = 0; i < mRigidBodyNum; i++) {
			RigidBody rb = new RigidBody();
			
			rb.name			= getString(20);
			rb.bone_index	= getShort();
			rb.group_index	= getByte();
			rb.group_target = getShort();
			rb.shape		= getByte();
			rb.size			= new float[3];		// w, h, d
			rb.location		= new float[3];		// x, y, z
			rb.rotation		= new float[3];
			getFloat(rb.size);
			getFloat(rb.location);
			getFloat(rb.rotation);
			rb.weight		= getFloat();
			rb.v_dim		= getFloat();
			rb.r_dim		= getFloat();
			rb.recoil		= getFloat();
			rb.friction		= getFloat();
			rb.type			= getByte();

			// for physics simulation
			rb.cur_location	= new float[3];
			rb.cur_rotation = new float[3];
			rb.cur_v		= new float[3];
			System.arraycopy(rb.location, 0, rb.cur_location, 0, 3);
			System.arraycopy(rb.rotation, 0, rb.cur_rotation, 0, 3);
			
			mRigidBody.add(rb);
		}
		
	}

	private void parsePMDEnglish() throws IOException {
		mHasEnglishName = getByte();
		Log.d("PMDParser", "HasEnglishName: " + String.valueOf(mHasEnglishName));
		if (mHasEnglishName == 1) {
			parsePMDEnglishName();
			parsePMDEnglishBoneList();
			parsePMDEnglishSkinList();
			parsePMDEnglishBoneDispName();
		}

	}

	private void parsePMDEnglishBoneDispName() throws IOException {
		mEnglishBoneDispNameNum = mBoneDispNameNum;
		mEnglishBoneDispName = new ArrayList<String>(mEnglishBoneDispNameNum);
		for (int i = 0; i < mEnglishBoneDispNameNum; i++) {
			String str = getString(50);
			mEnglishBoneDispName.add(i, str);
			Log.d("PMDParser", "EnglishBoneDispName: " + str);
		}
	}

	private void parsePMDToonFileName(String path) throws IOException {
		mToonFileName = new ArrayList<String>(11);
		mToonFileName.add(0, "/sdcard/MikuMikuDroid/Data/toon0.bmp");
		for (int i = 0; i < 10; i++) {
			String str = getString(100);
			if (isExist(path + str)) {
				mToonFileName.add(i + 1, path + str);
			} else {
				mToonFileName.add(i + 1, "/sdcard/MikuMikuDroid/Data/" + str);
			}
			Log.d("PMDParser", "ToonFile: " + str);
		}
	}

	private void parsePMDEnglishSkinList() throws IOException {
		mEnglishSkinListNum = mSkinDispNum;
		Log.d("PMDParser", "EnglishSkinName: " + String.valueOf(mEnglishSkinListNum));
		mEnglishSkinName = new ArrayList<String>(mEnglishSkinListNum);
		for (int i = 0; i < mEnglishSkinListNum; i++) {
			String str = getString(20);
			mEnglishSkinName.add(i, str);
		}
	}

	private void parsePMDEnglishBoneList() throws IOException {
		mEnglishBoneListNum = mBoneNum;
		Log.d("PMDParser", "EnglishBoneName: " + String.valueOf(mEnglishBoneListNum));
		mEnglishBoneName = new ArrayList<String>(mEnglishBoneListNum);
		for (int i = 0; i < mEnglishBoneListNum; i++) {
			String str = getString(20);
			mEnglishBoneName.add(i, str);
		}
	}

	private void parsePMDEnglishName() throws IOException {
		mEnglishModelName = getString(20);
		mEnglishComment = getString(256);
		Log.d("PMDParser", "EnglishModelName: " + mEnglishModelName);
		Log.d("PMDParser", "EnglishComment: " + mEnglishComment);
	}

	private void parsePMDBoneDisp() {
		mBoneDispNum = getInt();
		Log.d("PMDParser", "BoneDisp: " + String.valueOf(mBoneDispNum));
		if (mBoneDispNum > 0) {
			mBoneDisp = new ArrayList<BoneDisp>(mBoneDispNum);
			if (mBoneDisp == null) {
				mIsPmd = false;
				return;
			}
			for (int i = 0; i < mBoneDispNum; i++) {
				BoneDisp bd = new BoneDisp();

				bd.bone_index = getShort();
				bd.bone_disp_frame_index = getByte();

				mBoneDisp.add(i, bd);
			}
		} else {
			mBoneDisp = null;
		}
	}

	private void parsePMDBoneDispName() {
		mBoneDispNameNum = getByte();
		Log.d("PMDParser", "BoneDispName: " + String.valueOf(mBoneDispNameNum));
		if (mBoneDispNameNum > 0) {
			mBoneDispName = new ArrayList<String>(mBoneDispNameNum);
			if (mBoneDispName == null) {
				mIsPmd = false;
				return;
			}
			for (int i = 0; i < mBoneDispNameNum; i++) {
				String str = getString(50);
				mBoneDispName.add(i, str);
			}
		} else {
			mBoneDispName = null;
		}
	}

	private void parsePMDSkinDisp() {
		mSkinDispNum = getByte();
		Log.d("PMDParser", "SkinDisp: " + String.valueOf(mSkinDispNum));
		if (mSkinDispNum > 0) {
			mSkinDisp = new ArrayList<Short>(mSkinDispNum);
			if (mSkinDisp == null) {
				mIsPmd = false;
				return;
			}
			for (int i = 0; i < mSkinDispNum; i++) {
				short idx = getShort();
				mSkinDisp.add(i, idx);
			}
		} else {
			mSkinDisp = null;
		}
	}

	private void parsePMDFaceList() {
		mFaceNum = getShort();
		Log.d("PMDParser", "Face: " + String.valueOf(mFaceNum));
		if (mFaceNum > 0) {
			mFace = new ArrayList<Face>(mFaceNum);
			if (mFace == null) {
				mIsPmd = false;
				return;
			}
			for (int i = 0; i < mFaceNum; i++) {
				Face face = new Face();

				face.name = getString(20);
				face.face_vert_count = getInt();
				face.face_type = getByte();

				face.face_vert_data = new ArrayList<FaceVertData>(face.face_vert_count);
				for (int j = 0; j < face.face_vert_count; j++) {
					FaceVertData fvd = new FaceVertData();
					fvd.face_vert_index = getInt();
					fvd.offset = new float[3];
					fvd.base = new float[3];
					fvd.cleared = true;
					getFloat(fvd.offset);

					face.face_vert_data.add(j, fvd);
				}

				mFace.add(i, face);
			}
		} else {
			mFace = null;
		}
	}

	private void parsePMDIKList() {
		// the number of Vertexes
		mIKNum = getShort();
		Log.d("PMDParser", "IK: " + String.valueOf(mIKNum));
		if (mIKNum > 0) {
			mIK = new ArrayList<IK>(mIKNum);
			if (mIK == null) {
				mIsPmd = false;
				return;
			}
			for (int i = 0; i < mIKNum; i++) {
				IK ik = new IK();

				ik.ik_bone_index = getShort();
				ik.ik_target_bone_index = getShort();
				ik.ik_chain_length = getByte();
				ik.iterations = getShort();
				ik.control_weight = getFloat();

				ik.ik_child_bone_index = new Short[ik.ik_chain_length];
				for (int j = 0; j < ik.ik_chain_length; j++) {
					ik.ik_child_bone_index[j] = getShort();
				}

				mIK.add(i, ik);
			}
		} else {
			mIK = null;
		}
	}

	private void parsePMDBoneList() {
		// the number of Vertexes
		mBoneNum = getShort();
		Log.d("PMDParser", "BONE: " + String.valueOf(mBoneNum));
		if (mBoneNum > 0) {
			mBone = new ArrayList<Bone>(mBoneNum);
			if (mBone == null) {
				mIsPmd = false;
				return;
			}
			for (int i = 0; i < mBoneNum; i++) {
				Bone bone = new Bone();

				bone.name_bytes = getBytes(20, new byte[20]);
				String name = toString(bone.name_bytes);
				bone.parent = getShort();
				bone.tail = getShort();
				bone.type = getByte();
				bone.ik = getShort();

				bone.head_pos = new float[4];
				bone.head_pos[0] = getFloat();
				bone.head_pos[1] = getFloat();
				bone.head_pos[2] = getFloat();
				bone.head_pos[3] = 1; // for IK (Miku:getCurrentPosition:Matrix.multiplyMV(v, 0, d, 0, b.head_pos, 0)

				bone.motion = null;
				bone.quaternion = new double[4]; // for skin-mesh preCalkIK
				bone.matrix = new float[16]; // for skin-mesh animation
				bone.matrix_current = new float[16]; // for temporary (current bone matrix that is not include parent rotation
				bone.updated = false; // whether matrix is updated by VMD or not
				bone.is_leg = name.contains("‚Ð‚´");

				if (bone.tail != -1) {
					mBone.add(i, bone);
				}
			}
		} else {
			mBone = null;
		}
	}

	private void parsePMDMaterialList(String path) {
		// the number of Vertexes
		mMaterialNum = getInt();
		Log.d("PMDParser", "MATERIAL: " + String.valueOf(mMaterialNum));
		if (mMaterialNum > 0) {
			mMaterial = new ArrayList<Material>(mMaterialNum);
			if (mMaterial == null) {
				mIsPmd = false;
				return;
			}
			int acc = 0;
			for (int i = 0; i < mMaterialNum; i++) {
				Material material = new Material();

				material.diffuse_color = new float[4];
				getFloat(material.diffuse_color);

				material.power = getFloat();

				material.specular_color = new float[4];
				material.specular_color[0] = getFloat();
				material.specular_color[1] = getFloat();
				material.specular_color[2] = getFloat();
				material.specular_color[3] = 0f;

				material.emmisive_color = new float[4];
				material.emmisive_color[0] = getFloat();
				material.emmisive_color[1] = getFloat();
				material.emmisive_color[2] = getFloat();
				material.emmisive_color[3] = 0f;

				material.toon_index = getByte();
				material.toon_index += 1; // 0xFF to toon0.bmp, 0x00 to toon01.bmp, 0x01 to toon02.bmp...
				material.edge_flag = getByte();
				material.face_vert_count = getInt();
				material.texture = getString(20);
				if (material.texture.length() == 0) {
					material.texture = null;
					material.sphere = null;
				} else {
					String sp[] = material.texture.split("\\*");
					if (sp.length == 2) {
						material.texture = path + sp[0];
						material.sphere = path + sp[1];
					} else {
						material.texture = path + material.texture;
						material.sphere = null;
					}
				}

				material.face_vart_offset = acc;

				acc = acc + material.face_vert_count;
				mMaterial.add(i, material);
				if (material.texture != null) {
					Log.d("PMDParser", "TEXTURE" + String.valueOf(i) + "=\"" + material.texture + "\"");
				}
			}
			Log.d("PMDParser", "CHECKSUM IN MATERIAL: " + String.valueOf(acc));
		} else {
			mMaterial = null;
		}

	}

	private void parsePMDIndexList() {
		// the number of Vertexes
		mIndexNum = getInt();
		Log.d("PMDParser", "INDEX: " + String.valueOf(mIndexNum));
		if (mIndexNum > 0) {
			mIndex = new ArrayList<Integer>(mIndexNum);
			if (mIndex == null) {
				mIsPmd = false;
				return;
			}
			for (int i = 0; i < mIndexNum; i++) {
				mIndex.add(i, (0x0000ffff & getShort()));
			}
		} else {
			mIndex = null;
		}
	}

	private void parsePMDVertexList() {
		// the number of Vertexes
		mVertexNum = getInt();
		Log.d("PMDParser", "VERTEX: " + String.valueOf(mVertexNum));
		if (mVertexNum > 0) {
			mVertex = new ArrayList<Vertex>(mVertexNum);
			if (mVertex == null) {
				mIsPmd = false;
				return;
			}
			for (int i = 0; i < mVertexNum; i++) {
				Vertex vertex = new Vertex();
				vertex.pos = new float[3];
				vertex.normal = new float[3];
				vertex.uv = new float[2];

				getFloat(vertex.pos);
				getFloat(vertex.normal);
				getFloat(vertex.uv);
				vertex.bone_num_0 = getShort();
				vertex.bone_num_1 = getShort();

				vertex.bone_weight = getByte();
				vertex.edge_flag = getByte();

				if (vertex.bone_weight < 50) { // swap to make bone_num_0 as main bone
					short tmp = vertex.bone_num_0;
					vertex.bone_num_0 = vertex.bone_num_1;
					vertex.bone_num_1 = tmp;
					vertex.bone_weight = (byte) (100 - vertex.bone_weight);
				}

				mVertex.add(i, vertex);
			}
		} else {
			mVertex = null;
		}
	}

	private void parsePMDHeader() {
		// Magic
		String s = getString(3);
		Log.d("PMDParser", "MAGIC: " + s);
		if (s.equals("Pmd")) {
			mIsPmd = true;
		}

		// Version
		float f = getFloat();
		Log.d("PMDParser", "VERSION: " + String.valueOf(f));

		// Model Name
		mModelName = getString(20);
		Log.d("PMDParser", "MODEL NAME: " + mModelName);

		// description
		mDescription = getString(256);
		Log.d("PMDParser", "DESCRIPTION: " + mDescription);
	}

	public boolean isPmd() {
		return mIsPmd;
	}

	public int numVertex() {
		return mVertexNum;
	}

	public ArrayList<Vertex> getVertex() {
		return mVertex;
	}

	public int numIndex() {
		return mIndexNum;
	}

	public ArrayList<Integer> getIndex() {
		return mIndex;
	}

	public int numMaterial() {
		return mMaterialNum;
	}

	public ArrayList<Material> getMaterial() {
		return mMaterial;
	}

	public int numBone() {
		return mBoneNum;
	}

	public ArrayList<Bone> getBone() {
		return mBone;
	}

	public String getToonFileName(int i) {
		return mToonFileName.get(i);
	}

	public ArrayList<IK> getIK() {
		return mIK;
	}

	public ArrayList<Face> getFace() {
		return mFace;
	}

	public void recycle() {
		mModelName = null;
		mDescription = null;
		mVertex = null;
		mIndex = null;

		mSkinDisp = null;
		mEnglishModelName = null;
		mEnglishComment = null;
		mEnglishBoneName = null;
		mEnglishSkinName = null;
		mEnglishBoneDispName = null;
	}

	public void recycleVertex() {
		for (Vertex v : mVertex) {
			v.normal = null;
			v.pos = null;
			v.uv = null;
		}
	}
}
