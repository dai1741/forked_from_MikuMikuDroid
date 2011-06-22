package jp.gauzau.MikuMikuDroid;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class ModelBuilder {
	private static final String		XC_HEADER = "XC01";	// version
	
	private String					mFileName;
	
	public ByteBuffer				mIndexBuffer;
	public ByteBuffer				mVertBuffer;
	
	public ArrayList<Bone>			mBone;
	public ArrayList<Material>		mMaterial;
	public ArrayList<Face>			mFace;
	public ArrayList<IK>			mIK;
	public ArrayList<RigidBodyP>		mRigidBody;
	public ArrayList<Joint>			mJoint;
	public ArrayList<String>		mToonFileName;
	public boolean					mIsOneSkinning;

	public ModelBuilder(String s) {
		mFileName = s;
	}
	
	public String getFileName() {
		return mFileName;
	}
	
	public FloatBuffer createVertBuffer(int n) {
		// vertex, normal, texture buffer
		mVertBuffer = ByteBuffer.allocateDirect(n * 8 * 4);
		mVertBuffer.order(ByteOrder.nativeOrder());
		mVertBuffer.rewind();
		
		return mVertBuffer.asFloatBuffer();
	}
	
	public IntBuffer createIndexBuffer(int n) {
		mIndexBuffer = ByteBuffer.allocateDirect(n * 4);
		mIndexBuffer.order(ByteOrder.nativeOrder());
		mIndexBuffer.rewind();
		
		return mIndexBuffer.asIntBuffer();
	}
	
	public void writeToFile(String name) throws FileNotFoundException, IOException {
		mVertBuffer.position(0);
		mIndexBuffer.position(0);
		
		RandomAccessFile raf = new RandomAccessFile(name, "rw");
		MappedByteBuffer bb = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, size());

		writeHeader(bb);	// header
		writeBuffer(bb, mVertBuffer);
		writeBuffer(bb, mIndexBuffer);
		writeMaterials(bb, mMaterial);
		writeBones(bb, mBone);
		for(String s: mToonFileName) {
			writeString(bb, s);
		}
		bb.put((byte) (mIsOneSkinning ? 1 : 0));
		raf.close();
		mVertBuffer.rewind();
		mIndexBuffer.rewind();
	}
	
	public boolean readFromFile(String name) throws FileNotFoundException, IOException {
		RandomAccessFile raf = new RandomAccessFile(name, "r");
		MappedByteBuffer bb = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length());
		
		if(readHeader(bb)) {
			mVertBuffer		= readBuffer(bb);
			mIndexBuffer	= readBuffer(bb);
			mMaterial		= readMaterials(bb);
			mBone			= readBones(bb);
			mToonFileName	= new ArrayList<String>(11);
			for(int i = 0; i < 11; i++) {
				mToonFileName.add(readString(bb));
			}
			mIsOneSkinning = bb.get() == 0 ? false : true;			
		} else {
			raf.close();
			return false;
		}
		
		raf.close();
		return true;
	}
	
	
	
	private int size() {
		return (mVertBuffer.capacity() + mIndexBuffer.capacity()) * 2;
	}
	
	private ByteBuffer readBuffer(MappedByteBuffer bb) {
		int limit = bb.limit();
		int size = bb.getInt();
		
		ByteBuffer b = ByteBuffer.allocateDirect(size);
		b.order(ByteOrder.nativeOrder());
		bb.limit(bb.position() + size);
		b.put(bb);
		bb.limit(limit);
		b.rewind();
		
		return b;
	}
	
	private void writeBuffer(MappedByteBuffer bb, ByteBuffer b) {
		b.rewind();
		bb.putInt(b.capacity());
		bb.put(b);
	}
	
	private ArrayList<Material> readMaterials(MappedByteBuffer bb) {
		int size = bb.getInt();
		ArrayList<Material> ma = new ArrayList<Material>(size);
		for(int i = 0; i < size; i++) {
			ma.add(readMaterial(bb));
		}
		return ma;
	}

	private void writeMaterials(MappedByteBuffer bb, ArrayList<Material> ma) {
		bb.putInt(ma.size());
		for(Material m: ma) {
			writeMaterial(bb, m);
		}
	}

	private Material readMaterial(MappedByteBuffer bb) {
		Material m = new Material();
		
		m.diffuse_color = readFloat(bb, 4);
		m.power = bb.getFloat();
		m.specular_color = readFloat(bb, 4);
		m.emmisive_color = readFloat(bb, 4);
		m.toon_index = bb.get();
		m.edge_flag  = bb.get();
		m.face_vert_count = bb.getInt();
		m.face_vert_offset = bb.getInt();
		m.texture = readString(bb);
		m.sphere  = readString(bb);
		
		return m;
	}
	
	private void writeMaterial(MappedByteBuffer bb, Material m) {
		writeFloat(bb, m.diffuse_color);
		bb.putFloat(m.power);
		writeFloat(bb, m.specular_color);
		writeFloat(bb, m.emmisive_color);
		bb.put(m.toon_index);
		bb.put(m.edge_flag);
		bb.putInt(m.face_vert_count);
		bb.putInt(m.face_vert_offset);
		writeString(bb, m.texture);
		writeString(bb, m.sphere);
	}
	
	private ArrayList<Bone> readBones(MappedByteBuffer bb) {
		int size = bb.getInt();
		ArrayList<Bone> ba = new ArrayList<Bone>(size);
		for(int i = 0; i < size; i++) {
			ba.add(readBone(bb));
		}
		return ba;
	}

	private void writeBones(MappedByteBuffer bb, ArrayList<Bone> ba) {
		bb.putInt(ba.size());
		for(Bone b: ba) {
			writeBone(bb, b);
		}
	}
	
	private Bone readBone(MappedByteBuffer bb) {
		Bone b = new Bone();
		b.name_bytes = new byte[20];
		
		b.name = readString(bb);
		bb.get(b.name_bytes);
		b.parent = bb.getShort();
		b.tail = bb.getShort();
		b.type = bb.get();
		b.ik = bb.getShort();
		b.head_pos = readFloat(bb, 4);
		
		b.quaternion = new double[4]; // for skin-mesh preCalkIK
		b.matrix = new float[16]; // for skin-mesh animation
		b.matrix_current = new float[16]; // for temporary (current bone matrix that is not include parent rotation
		b.updated = false; // whether matrix is updated by VMD or not
		b.is_leg = b.name.contains("‚Ð‚´");
		
		return b;
	}
	
	private void writeBone(MappedByteBuffer bb, Bone b) {
		writeString(bb, b.name);
		bb.put(b.name_bytes);
		bb.putShort(b.parent);
		bb.putShort(b.tail);
		bb.put(b.type);
		bb.putShort(b.ik);
		writeFloat(bb, b.head_pos);
	}
	
	private void writeFloat(MappedByteBuffer bb, float[] f) {
		for(int i = 0; i < f.length; i++) {
			bb.putFloat(f[i]);
		}
	}
	
	private float[] readFloat(MappedByteBuffer bb, int size) {
		float[] f = new float[size];
		for(int i = 0; i < size; i ++) {
			f[i] = bb.getFloat();
		}
		return f;
	}
	
	private String readString(MappedByteBuffer bb) {
		int size = bb.getInt();
		if(size == 0) {
			return null;
		} else {
			byte[] sb = new byte[size];
			bb.get(sb);
			return new String(sb);
		}
	}
	
	private void writeString(MappedByteBuffer bb, String s) {
		if(s == null) {
			bb.putInt(0);
		} else {
			byte[] b = s.getBytes();
			bb.putInt(b.length);
			bb.put(b);
		}
	}
	
	private boolean readHeader(MappedByteBuffer bb) {
		byte[] sb = new byte[XC_HEADER.length()];
		bb.get(sb);
		return new String(sb).compareToIgnoreCase(XC_HEADER) == 0;
	}
	
	private void writeHeader(MappedByteBuffer bb) {
		bb.put(new String(XC_HEADER).getBytes());
	}
}
