package jp.gauzau.MikuMikuDroid;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;

public class MikuModel {
	public boolean mAnimation;
	public int mRenameNum;
	public int mRenameBone;
	public CubeArea mCube;
	
	public FloatBuffer mToonCoordBuffer;
	public FloatBuffer mWeightBuffer;
	public ShortBuffer mIndexBuffer;
	public FloatBuffer mAllBuffer;
	
	public ArrayList<Bone> mBone;
	public ArrayList<Material> mMaterial;
	public ArrayList<Face> mFace;
	public ArrayList<IK> mIK;
	public ArrayList<RigidBody> mRigidBody;
	public ArrayList<Joint> mJoint;
	public ArrayList<String> mToonFileName;
	public ArrayList<Material> mRendarList;
	public HashMap<String, TexBitmap> mTexture;
	public ArrayList<TexBitmap> mToon;
	public int[] mIndexMaps;
	public Face mFaceBase;
	
	public MikuModel(PMDParser pmd, int rename_num, int rename_bone, boolean animation) {
		init(pmd, rename_num, rename_bone, animation);
	}

	public MikuModel(PMDParser pmd, int rename_num, int rename_bone) {
		init(pmd, rename_num, rename_bone, true);
	}

	public void init(PMDParser pmd, int rename_num, int rename_bone, boolean animation) {
		mRenameNum		= rename_num;
		mRenameBone		= rename_bone;
		mAnimation		= animation;
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
			for (FaceVertData fvd : mFaceBase.face_vert_data) {
				// vertex is sorted by makeIndexSortedBuffers() in stride 8
				fvd.face_vert_index = mIndexMaps[fvd.face_vert_index] * 8;
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
		for (Material mat : mMaterial) {
			reconstructMaterial1(parser, mat, 0, max_bone);
		}
	}
	
	void reconstructMaterial1(PMDParser pmd, Material mat, int offset, int max_bone) {
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
				mat_new.rename_hash = rename;
				buildBoneRenameMap(mat_new, rename, max_bone);
				buildBoneRenameInvMap(mat_new, rename, max_bone);
				buildBoneRenameIndex(pmd, mat_new, max_bone);
				mRendarList.add(mat_new);

				Log.d("Miku", "rename Bone for Material #" + String.valueOf(mat_new.face_vart_offset) + ", bones " + String.valueOf(acc));
				for (Entry<Integer, Integer> b : rename.entrySet()) {
					Log.d("Miku", String.format("ID %d: bone %d", b.getValue(), b.getKey()));
				}

				reconstructMaterial1(pmd, mat, j, max_bone);
				return;
			}
		}
		mat_new.face_vert_count = mat.face_vert_count - offset;
		mat_new.rename_hash = rename;
		buildBoneRenameMap(mat_new, rename, max_bone);
		buildBoneRenameInvMap(mat_new, rename, max_bone);
		buildBoneRenameIndex(pmd, mat_new, max_bone);
		mRendarList.add(mat_new);

//		Log.d("Miku", "rename Bone for Material #" + String.valueOf(mat_new.face_vart_offset) + ", bones " + String.valueOf(acc));
//		for (Entry<Integer, Integer> b : rename.entrySet()) {
//			Log.d("Miku", String.format("ID %d: bone %d", b.getValue(), b.getKey()));
//		}
	}


	void buildBoneRenameMap(Material mat, HashMap<Integer, Integer> rename, int max_bone) {
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

	void buildBoneRenameInvMap(Material mat, HashMap<Integer, Integer> rename, int max_bone) {
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

	void buildBoneRenameIndex(PMDParser pmd, Material mat, int max_bone) {
		ByteBuffer rbb = ByteBuffer.allocateDirect(pmd.getVertex().size() * 3);
		rbb.order(ByteOrder.nativeOrder());
		mat.rename_index = rbb;
	
		for (int i = 0; i < mIndexMaps.length; i++) {
			Vertex ver = pmd.getVertex().get(i);
			if (mIndexMaps[i] >= 0) {
				mat.rename_index.position(mIndexMaps[i] * 3);
				mat.rename_index.put((byte) mat.rename_map[ver.bone_num_0]);
				mat.rename_index.put((byte) mat.rename_map[ver.bone_num_1]);
				mat.rename_index.put(ver.bone_weight);
			}
		}
	
		mat.rename_index.position(0);
	}

	void buildBoneNoMotionRenameIndex(PMDParser pmd) {
		ByteBuffer rbb = ByteBuffer.allocateDirect(pmd.getVertex().size() * 3);
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

	public void calcToonTexCoord(float x, float y, float z) {
		ByteBuffer tbb = ByteBuffer.allocateDirect(mAllBuffer.capacity() / 8 * 2 * 4);
		tbb.order(ByteOrder.nativeOrder());
		mToonCoordBuffer = tbb.asFloatBuffer();

		float vn[] = new float[6];
		for (int i = 0; i < mAllBuffer.capacity() / 8; i++) {
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
		for (int i = 0; i < mMaterial.size(); i++) {
			Material mat = mMaterial.get(i);
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
		for (int i = 0; i < mMaterial.size(); i++) {
			Material mat = mMaterial.get(i);
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
			tb.bmp = loadPicture(mToonFileName.get(i), 1);
			Log.d("Miku",
					mToonFileName.get(i) + ": " + String.valueOf(tb.bmp.getWidth()) + "x" + String.valueOf(tb.bmp.getHeight()) + " at row size "
							+ String.valueOf(tb.bmp.getRowBytes()) + "byte in " + tb.bmp.getConfig().name());
			mToon.add(tb);
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
			Options opt = new Options();
			// opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
			opt.inSampleSize = scale;
			bmp = BitmapFactory.decodeFile(file, opt);
		}
	
		return bmp;
	}


}