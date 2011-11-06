package jp.gauzau.MikuMikuDroidmod;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import android.util.Log;

public class XParser extends ParserBase {
	private String  mFileName;
	private boolean mIsX;
	private ArrayList<ByteBuffer> mVertex;
	private ArrayList<ByteBuffer> mIndex;
	private ArrayList<Material> mMaterial;
	private ArrayList<Bone>		mBone;
	private ArrayList<String> mToonFileName;

	private ByteBuffer mRawFace;
	private int mRawFaceNum;
	private int mVertBase;
	private int mVertNum;
	private int mIdxBase;
	
	private IntBuffer	mIndexBuffer;
	private FloatBuffer	mVertBuffer;
	private ModelBuilder mMb;
	
	// temp buffer
	private float[] mD0 = new float[3];
	private float[] mD1 = new float[3];

	public XParser(String base, String file, float scale) throws IOException {
		super(file);
		mFileName = file;
		mIsX = false;
		File f = new File(mFileName);
		String path = f.getParent() + "/";

		try {
			parseXHeader();
			if(mIsX) {
				parseXMesh(base, path, scale);
				createBuffers(base, mFileName);

			}
		} catch (IOException e) {
			e.printStackTrace();
			mIsX = false;
		}
	}
	
	public boolean isX() {
		return mIsX;
	}
	
	public ModelBuilder getModelBuilder() {
		return mMb;
	}

	private void parseXHeader() {
		String s = getString(16);
		mIsX =  s.equalsIgnoreCase("xof 0302txt 0064") ||
				s.equalsIgnoreCase("xof 0302txt 0032") ||
				s.equalsIgnoreCase("xof 0303txt 0064") ||
				s.equalsIgnoreCase("xof 0303txt 0032");
		nextLine();
	}
	
	private void parseXMesh(String base, String path, float scale) throws IOException {
		mVertex = new ArrayList<ByteBuffer>();
		mIndex = new ArrayList<ByteBuffer>();
		mMaterial = new ArrayList<Material>();
		mVertBase = 0;
		mIdxBase = 0;
		boolean has_normal = false;

		// find mesh body
		int acc = 0;
		while(skipToBody("Mesh")) {
			parseVertex(base, scale, acc);
			parseFace(base);

			Token t = new Token();
			getToken(t);
			while(t.type != '}') {
				if(t.type == 's') {
					if(t.s.equalsIgnoreCase("MeshMaterialList")) {
						nextLine();
						parseMaterial(base, path, acc);
					} else if(t.s.equalsIgnoreCase("MeshTextureCoords")) {
						nextLine();
						parseTexCoord(acc);
					} else if(t.s.equalsIgnoreCase("MeshNormals")) {
						nextLine();
						parseNormal(base, acc);
						has_normal = true;
					} else {
						skipBody();
					}					
				}
				getToken(t);	// next header
			}
			mVertBase += mVertNum;
			if(!has_normal) {
				calcNormals(acc);
			}
			has_normal = false;
			acc++;
			if(mRawFace != null) {
				LargeBuffer.close(mRawFace);
				mRawFace = null;
			}
		}
	}
	
	private void createBuffers(String base, String modelf) throws IOException {
		mMb = new ModelBuilder(modelf);
		
		createVertBuffer(mMb);
		createIndexBuffer(mMb);
		
		setDummyBone("�Z���^�[");
		
		mToonFileName = new ArrayList<String>(11);
		mToonFileName.add(0, base + "Data/toon0.bmp");
		for (int i = 0; i < 10; i++) {
			String str = String.format(base + "Data/toon%02d.bmp", i + 1);
			mToonFileName.add(i + 1, str);
		}
		
		mMb.mMaterial = mMaterial;
		mMb.mBone = mBone;
		mMb.mToonFileName = mToonFileName;
		mMb.mIsOneSkinning = true;
	}
	
	private void createVertBuffer(ModelBuilder mb) throws IOException {
		int size = 0;
		for(ByteBuffer av: mVertex) {
			size += av.capacity() / 8 / 4;
		}
		mVertBuffer = mb.createVertBuffer(size);
		for(ByteBuffer av: mVertex) {
			mVertBuffer.put(av.asFloatBuffer());
			LargeBuffer.close(av);
		}
		mVertBuffer.position(0);
	}
	
	private void createIndexBuffer(ModelBuilder mb) throws IOException {
		int size = 0;
		for(ByteBuffer ai: mIndex) {
			size += ai.capacity() / 4;
		}
		mIndexBuffer = mb.createIndexBuffer(size);
		for(int i = 0; i < mIndex.size(); i++) {
			ByteBuffer ai = mIndex.get(i);
			mIndexBuffer.put(ai.asIntBuffer());
			LargeBuffer.close(ai);
		}
		mIndexBuffer.position(0);
	}
	
	private void parseVertex(String base, float scale, int pos) throws IOException {
		Token t = new Token();
		int n = (int) getToken(t);
		nextLine();
		
		ByteBuffer avb = LargeBuffer.openTemp(base + "/.cache/XFILE_V" + Integer.valueOf(pos) + ".tmp", n * 8 * 4);
		FloatBuffer av = avb.asFloatBuffer();
		
		for(int i = 0; i < n; i++) {
			av.position(i * 8);
			av.put(getToken(t) * scale);
			getToken(t);	// must be ','
			av.put(getToken(t) * scale);
			getToken(t);	// must be ','
			av.put(getToken(t) * scale);
			nextLine();
		}
		mVertex.add(pos, avb);
		mVertNum = n;
		Log.d("XParser", String.format("Vertex: %d, total %d", n, mVertBase + mVertNum));
	}
	
	private void parseFace(String base) throws IOException {
		Token t = new Token();
		
		int n = (int) getToken(t);
		nextLine();
		
		mRawFace = LargeBuffer.openTemp(base + "/.cache/XFILE_F.tmp", n * 4 * 5);
		IntBuffer rfi = mRawFace.asIntBuffer();
		mRawFaceNum = 0;
		int[] data = new int[5];
		for(int i = 0; i < n; i++) {
			getToken(t);
			int m = (int) t.num;
			data[0] = m;
			mRawFaceNum += m == 3 ? m : 6;
			getToken(t);	// must be ';'
			for(int j = 0; j < m; j++) {
				getToken(t);
				data[j + 1] = (int) t.num;
				getToken(t);	// must be ',' or ';'
			}
			rfi.put(data);
			getToken(t);	// must be ','
		}
		Log.d("XParser", String.format("Face: %d", n));		
	}
	
	private void parseMaterial(String base, String path, int pos) throws IOException {
		Token t = new Token();
		
		int mn = (int) getToken(t);
		nextLine();
		int fn = (int) getToken(t);
		nextLine();

		// initialize material map
		ArrayList<ArrayList<Integer>> mmap = new ArrayList<ArrayList<Integer>>(mn);
		for(int i = 0; i < mn; i++) {
			mmap.add(new ArrayList<Integer>(0));
		}
		
		// read material map
		for(int i = 0; i < fn; i++) {
			int idx = (int) getToken(t);
			getToken(t);	// must be ','
			mmap.get(idx).add(i);
		}
		nextLine();
		
		// read and reconstruct material and index
		int acc = mIdxBase;		
		ByteBuffer aib = LargeBuffer.openTemp(base + "/.cache/XFILE_I" + Integer.valueOf(pos) + ".tmp", mRawFaceNum * 4);
		IntBuffer idx = aib.asIntBuffer();

		ArrayList<Material> mat = mMaterial;
		int[] data = new int[5];
		IntBuffer rfi = mRawFace.asIntBuffer();
		for(int i = 0; i < mn; i++) {
			Material m = new Material();
			
			m.diffuse_color = new float[4];
			m.specular_color = new float[4];
			m.emmisive_color = new float[4];
			
			getToken(t); // must be token 'Material'
			getToken(t); // must be token '{'
			m.diffuse_color[0] = getToken(t);
			getToken(t);	// must be ';'
			m.diffuse_color[1] = getToken(t);
			getToken(t);	// must be ';'
			m.diffuse_color[2] = getToken(t);
			getToken(t);	// must be ';'
			m.diffuse_color[3] = getToken(t);
			getToken(t);	// must be ';'
			getToken(t);	// must be ';'

			m.power = getToken(t);
			getToken(t);	// must be ';'

			m.specular_color[0] = getToken(t);
			getToken(t);	// must be ';'
			m.specular_color[1] = getToken(t);
			getToken(t);	// must be ';'
			m.specular_color[2] = getToken(t);
			m.specular_color[3] = 0;
			getToken(t);	// must be ';'
			getToken(t);	// must be ';'

			m.emmisive_color[0] = getToken(t);
			getToken(t);	// must be ';'
			m.emmisive_color[1] = getToken(t);
			getToken(t);	// must be ';'
			m.emmisive_color[2] = getToken(t);
			m.emmisive_color[3] = 0;
			getToken(t);	// must be ';'
			getToken(t);	// must be ';'

			// has Texture
			getToken(t);
			if(t.type == 's' && t.s.equals("TextureFilename")) {
				getToken(t);	// must be '{'
				getToken(t);
				m.texture = path + t.s;
				getToken(t); // must be ';'
				getToken(t); // must be '}'
				getToken(t); // must be '}'
				Log.d("XParser", String.format("Texture: %s", m.texture));
			} else {
				// must be '}'
				m.texture = null;
			}
			m.sphere = null;
			
			m.face_vert_offset = acc;
			ArrayList<Integer> rf = mmap.get(i);
			for(int j = 0; j < rf.size(); j++) {
				rfi.position(rf.get(j) * 5);
				rfi.get(data);
				if(data[0] == 3) {
					acc += 3;
					idx.put(data[1] + mVertBase);
					idx.put(data[2] + mVertBase);
					idx.put(data[3] + mVertBase);
				} else if(data[0] == 4){	// must be face.size() == 4
					acc += 6;
					idx.put(data[1] + mVertBase);
					idx.put(data[2] + mVertBase);
					idx.put(data[3] + mVertBase);
					
					idx.put(data[4] + mVertBase);
					idx.put(data[1] + mVertBase);
					idx.put(data[3] + mVertBase);
				} else {
					Log.d("XParser", "Illegal face");
				}
			}
			m.face_vert_count = acc - m.face_vert_offset;
			m.toon_index = 1;
			mat.add(m);
		}
		mIdxBase = acc;
		mIndex.add(pos, aib);
		
		getToken(t); // must be '}'
		Log.d("XParser", String.format("Material: %d %d, index total %d", mn, fn, mIdxBase));
	}
	
	private void parseTexCoord(int pos) throws IOException {
		Token t = new Token();

		int n = (int) getToken(t);
		getToken(t);
		
		if(mVertNum == n) {
			ByteBuffer vtxb = mVertex.get(pos);
			FloatBuffer vtx = vtxb.asFloatBuffer();
			for(int i = 0; i < n; i++) {
				vtx.position(i * 8 + 6);
				vtx.put(getToken(t));
				getToken(t);	// must be ';'
				vtx.put(getToken(t));
				nextLine();
			}
			getToken(t);	// must be '}'
			Log.d("XParser", String.format("TexCoord: %d", n));
		} else {
			mIsX = false;
		}
	}
	
	private void parseNormal(String base, int pos) throws IOException {
		Token t = new Token();

		int n = (int) getToken(t);
		getToken(t);

		IntBuffer idx = mIndex.get(pos).asIntBuffer();
		FloatBuffer vtx = mVertex.get(pos).asFloatBuffer();
		float[] nm = new float[3];
		float[] nmo = new float[3];
		
		// initialize
		nm[0] = nm[1] = nm[2] = 0;
		for(int i = 0; i < mVertNum; i++) {
			vtx.position(i * 8 + 3);
			vtx.put(nm);
		}
		
		// read normals
		ByteBuffer norb = LargeBuffer.openTemp(base + "/.cache/XFILE_N.tmp", n * 4 * 3);
		FloatBuffer nor = norb.asFloatBuffer();
		for(int i = 0; i < n; i++) {
			nor.put(getToken(t));
			getToken(t);	// must be ';'
			nor.put(getToken(t));
			getToken(t);	// must be ';'
			nor.put(getToken(t));
			nextLine();
		}
		nor.position(0);

		// read faces
		int m = (int) getToken(t);
		getToken(t);	// must be ;
		for(int i = 0; i < m; i++) {
			int f = (int) getToken(t);
			getToken(t);
			for(int j = 0; j < f; j++) {
				int v = idx.get();
				vtx.position(v * 8 + 3);
				vtx.get(nm);
				
				nor.position((int) getToken(t) * 3);
				nor.get(nmo);

				Vector.add(nm, nm, nmo);
				
				vtx.position(v * 8 + 3);
				vtx.put(nm);
				
				getToken(t);	// must be , or ;
			}
			if(f > 3) {	// must be 4
				idx.position(idx.position() + 2);
			}
			nextLine();
		}

		// normalize
		for(int i = 0; i < mVertNum; i++) {
			vtx.position(i * 8 + 3);
			vtx.get(nm);
			Vector.normalize(nm);
			vtx.position(i * 8 + 3);
			vtx.put(nm);
		}

		getToken(t);	// must be '}'
		LargeBuffer.close(norb);
		Log.d("XParser", String.format("Normal: %d %d", n, m));
	}

	private void calcNormals(int pos) {
		IntBuffer rfi = mRawFace.asIntBuffer();
		FloatBuffer vtx = mVertex.get(pos).asFloatBuffer();
		float[] nm = new float[3];
		float[] nmo = new float[3];
		
		// initialize
		nm[0] = nm[1] = nm[2] = 0;
		for(int i = 0; i < mVertNum; i++) {
			vtx.position(i * 8 + 3);
			vtx.put(nm);
		}
		
		// read faces
		int[] data = new int[5];
		float[] v0 = new float[3];
		float[] v1 = new float[3];
		float[] v2 = new float[3];
		for(int i = 0; i < rfi.capacity() / 5; i++) {
			rfi.get(data);
			vtx.position(data[1] * 8);
			vtx.get(v0);
			vtx.position(data[2] * 8);
			vtx.get(v1);
			vtx.position(data[3] * 8);
			vtx.get(v2);
			
			calcNormal(nmo, v0, v1, v2);
			
			for(int j = 0; j < data[0]; j++) {
				vtx.position(data[j + 1] * 8 + 3);
				vtx.get(nm);
				
				Vector.add(nm, nm, nmo);
				
				vtx.position(data[j + 1] * 8 + 3);
				vtx.put(nm);
			}
		}

		// normalize
		for(int i = 0; i < mVertNum; i++) {
			vtx.position(i * 8 + 3);
			vtx.get(nm);
			Vector.normalize(nm);
			vtx.position(i * 8 + 3);
			vtx.put(nm);
		}
	}

	private void calcNormal(float[] dst, float[] v0, float[] v1, float[] v2) {
		Vector.sub(mD0, v0, v1);
		Vector.sub(mD1, v2, v1);
		Vector.cross(dst, mD1, mD0);
		Vector.normalize(dst);
	}
	
	private void setDummyBone(String name) {
		mBone = new ArrayList<Bone>(1);
		Bone b = new Bone();
		
		b.name_bytes = new byte[20];
		b.name = name;
		b.parent = -1;
		b.tail = -1;
		b.type = 0;
		b.ik = 0;

		b.head_pos = new float[4];
		b.head_pos[0] = 0;
		b.head_pos[1] = 0;
		b.head_pos[2] = 0;
		b.head_pos[3] = 1; // for IK (Miku:getCurrentPosition:Matrix.multiplyMV(v, 0, d, 0, b.head_pos, 0)

		b.motion = null;
		b.quaternion = new double[4]; // for skin-mesh preCalkIK
		b.matrix = new float[16]; // for skin-mesh animation
		b.matrix_current = new float[16]; // for temporary (current b matrix that is not include parent rotation
		b.updated = false; // whether matrix is updated by VMD or not
		b.is_leg = false;

		mBone.add(b);
	}
	
	private boolean skipBody() {
		int level = 0;
		Token t = new Token();
		while(true) {
			if(!getTokenEof(t)) {
				return false;
			}
			if(!nextLineEof()) {
				return false;
			}
			if(t.type == '{') {
				level++;
			} else if(t.type == '}') {
				level--;
				if(level == 0) {
					return true;
				}
			}
		}
	}
	
	private boolean skipToBody(String b) {
		Token t = new Token();
		do {
			if(!getTokenEof(t)) {
				return false;
			}
			if(!nextLineEof()) {
				return false;
			}
		} while(!(t.type == 's' && t.s.equals(b)));
		return true;
	}
	
	private boolean isWhiteSpace(byte b) {
		return b == ' ' || b == '\t' || b == '\r' || b == '\n';
	}
	
	private boolean isSeparator(byte b) {
		return b == ';' || b == ',' || b == '{' || b == '}';
	}
	
	private boolean isEndOfToken(byte b) {
		return isWhiteSpace(b) || isSeparator(b);
	}
	
	private void nextLine() {
		while(getByte() != '\n') ;
	}
	
	private boolean nextLineEof() {
		while(!isEof()) {
			if(getByte() == '\n') {
				return true;
			}
		}
		return false;
	}
	
	private void skipWhiteSpace() {
		while(isWhiteSpace(getByte()));
		position(position() - 1);
	}
	
	private boolean skipWhiteSpaceEof() {
		while(!isEof()) {
			if(!isWhiteSpace(getByte())) {
				position(position() - 1);
				return true;
			}
		}
		return false;
	}
	
	private class Token {
		public byte type;
		public float num;
		public String s;
	}
	
	private float getToken(Token t) {
		skipWhiteSpace();
		return getToken1(t);
	}
	
	private boolean getTokenEof(Token t) {
		if(!skipWhiteSpaceEof()) {
			return false;
		}
		getToken1(t);
		return true;
	}

	private float getToken1(Token t) {
		int pos0 = position();
		byte c = getByte();
		if(isSeparator(c)) {
			t.type = c;
			return 0;
		} else if(c == '\"') {
			while(getByte() != '\"') ;
			int pos1 = position();
			position(pos0 + 1);
			t.type = 's';
			t.s = getString(pos1 - pos0 - 2);
			position(pos1);
			return 0;
		} else if(Character.isDigit(c) || c == '-') {
			t.type = '0';
			boolean minus = c == '-';
			if(c == '-') {
				t.num = 0;
			} else {
				t.num = c - '0';
			}
			int pos1 = -1;
			while(!isEndOfToken(c = getByte())) {
				if(c == '.') {
					pos1 = position();
				} else {
					t.num = t.num * 10 + c - '0';
				}
			}
			if(minus) {
				t.num *= -1;
			}
			if(pos1 >= 0) {
				t.num /= Math.pow(10.0, position() - pos1 - 1);
			}
			position(position() - 1);
			return t.num;
		} else {	// string
			while(!isEndOfToken(getByte()));
			int pos1 = position();
			position(pos0);
			t.type = 's';
			t.s = getString(pos1 - pos0 - 1);
			return 0;
		}		
	}
	
	/*
	private void printToken(Token t) {
		if(t.type == ';') {
			Log.d("XParser", "Token ;");
		} else if(t.type == ',') {
			Log.d("XParser", "Token ,");
		} else if(t.type == '{') {
			Log.d("XParser", "Token {");
		} else if(t.type == '}') {
			Log.d("XParser", "Token }");
		} else if(t.type == 's') {
			Log.d("XParser", "Token String " + t.s);			
		} else {
			Log.d("XParser", "Token Number " + String.valueOf(t.num));
		}
	}
	*/
}
