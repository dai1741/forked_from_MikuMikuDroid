package jp.gauzau.MikuMikuDroid;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import android.util.Log;

public class XParser extends ParserBase implements ModelFile {
	private String  mFileName;
	private boolean mIsX;
	private ArrayList<Vertex> mVertex;
	private ArrayList<Integer> mIndex;
	private ArrayList<Material> mMaterial;
	private ArrayList<Bone>		mBone;
	private ArrayList<String> mToonFileName;

	private ArrayList<ArrayList<Integer>> mRawFace;
	private int mVertBase;
	private int mVertNum;
	private int mIdxBase;

	public XParser(String base, String file, float scale) throws IOException {
		super(file);
		mFileName = file;
		mIsX = false;
		File f = new File(file);
		String path = f.getParent() + "/";

		try {
			parseXHeader();
			if(mIsX) {
				parseXMesh(path, scale);
				setDummyBone("ÉZÉìÉ^Å[");
				
				mToonFileName = new ArrayList<String>(11);
				mToonFileName.add(0, base + "Data/toon0.bmp");
				for (int i = 0; i < 10; i++) {
					String str = String.format(base + "Data/toon%02d.bmp", i + 1);
					mToonFileName.add(i + 1, str);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			mIsX = false;
		}
	}
	
	public boolean isPmd() {
		return mIsX;
	}
	
	
	public FloatBuffer getVertexBuffer() {
		return null;
	}
	
	public IntBuffer getIndexBufferI() {
		return null;
	}

	public ShortBuffer getIndexBufferS() {
		return null;
	}

	public ShortBuffer getWeightBuffer() {
		return null;
	}

	public ArrayList<Vertex> getVertex() {
		return mVertex;
	}

	public ArrayList<Integer> getIndex() {
		return mIndex;
	}

	public ArrayList<Material> getMaterial() {
		return mMaterial;
	}

	public ArrayList<Bone> getBone() {
		return mBone;
	}

	public ArrayList<String> getToonFileName() {
		return mToonFileName;
	}

	public ArrayList<IK> getIK() {
		return null;
	}

	public ArrayList<Face> getFace() {
		return null;
	}
	
	public ArrayList<RigidBody> getRigidBody() {
		return null;
	}
	
	public ArrayList<Joint> getJoint() {
		return null;
	}
	
	public String getFileName() {
		return mFileName;
	}
	
	public boolean isOneSkinning() {
		return true;
	}

	public void recycle() {
		mVertex = null;
		mIndex = null;

		close();
	}

	public void recycleVertex() {
		for (Vertex v : mVertex) {
			v.normal = null;
			v.pos = null;
			v.uv = null;
		}
	}

	private void parseXHeader() {
		String s = getString(16);
		mIsX =  s.equalsIgnoreCase("xof 0302txt 0064") ||
				s.equalsIgnoreCase("xof 0302txt 0032") ||
				s.equalsIgnoreCase("xof 0303txt 0064") ||
				s.equalsIgnoreCase("xof 0303txt 0032");
		nextLine();
	}
	
	private void parseXMesh(String path, float scale) throws IOException {
		mVertex = new ArrayList<Vertex>();
		mIndex = new ArrayList<Integer>();
		mMaterial = new ArrayList<Material>(0);
		mVertBase = 0;
		mIdxBase = 0;

		// find mesh body
		while(skipToBody("Mesh")) {
			parseVertex(scale);
			parseFace();

			Token t = new Token();
			getToken(t);
			while(!(t.type == '}')) {
				if(t.s.equals("MeshMaterialList")) {
					nextLine();
					parseMaterial(path);
				} else if(t.s.equals("MeshTextureCoords")) {
					nextLine();
					parseTexCoord();
				} else {
					break;
				}
				getToken(t);
			}
			mVertBase += mVertNum;
		}
	}
	
	private void parseVertex(float scale) throws IOException {
		Token t = new Token();
		int n = (int) getToken(t);
		nextLine();
		
		for(int i = 0; i < n; i++) {
			Vertex v = new Vertex();
			v.pos = new float[3];
			v.normal = new float[3];
			v.uv = new float[2];

			v.pos[0] = getToken(t) * scale;
			getToken(t);	// must be ','
			v.pos[1] = getToken(t) * scale;
			getToken(t);	// must be ','
			v.pos[2] = getToken(t) * scale;
			getToken(t);	// must be ','
			
			v.bone_num_0 = 0;
			v.bone_num_1 = 0;
			v.bone_weight = 100;
			v.edge_flag = 0;

			mVertex.add(v);
			nextLine();
		}
		mVertNum = n;
		Log.d("XParser", String.format("Vertex: %d, total %d", n, mVertBase + mVertNum));
	}
	
	private void parseFace() throws IOException {
		Token t = new Token();
		
		int n = (int) getToken(t);
		nextLine();
		
		mRawFace = new ArrayList<ArrayList<Integer>>(n);
		for(int i = 0; i < n; i++) {
			getToken(t);
			int m = (int) t.num;
			getToken(t);	// must be ';'
			ArrayList<Integer> f = new ArrayList<Integer>(m);
			for(int j = 0; j < m; j++) {
				getToken(t);
				f.add((int) t.num);
				getToken(t);	// must be ',' or ';'
			}
			mRawFace.add(f);
			getToken(t);	// must be ','
		}
		Log.d("XParser", String.format("Face: %d", n));		
	}
	
	private void parseMaterial(String path) throws IOException {
		Token t = new Token();
		
		int mn = (int) getToken(t);
		nextLine();
		int fn = (int) getToken(t);
		nextLine();
		if(fn != mRawFace.size()) {
			mIsX = false;
			return ;
		}
		
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
				Log.d("XParser", String.format("Texture: %s", m.texture));
				getToken(t); // must be ';'
				getToken(t); // must be '}'
				getToken(t); // must be '}'
			} else {
				// must be '}'
				m.texture = null;
			}
			m.sphere = null;
			
			m.face_vert_offset = acc;
			ArrayList<Integer> rf = mmap.get(i);
			for(int j = 0; j < rf.size(); j++) {
				ArrayList<Integer> face = mRawFace.get(rf.get(j));
				if(face.size() == 3) {
					acc += 3;
					mIndex.add(face.get(0) + mVertBase);
					mIndex.add(face.get(1) + mVertBase);
					mIndex.add(face.get(2) + mVertBase);
				} else if(face.size() == 4){	// must be face.size() == 4
					acc += 6;
					mIndex.add(face.get(0) + mVertBase);
					mIndex.add(face.get(1) + mVertBase);
					mIndex.add(face.get(2) + mVertBase);
					
					mIndex.add(face.get(0) + mVertBase);
					mIndex.add(face.get(2) + mVertBase);
					mIndex.add(face.get(3) + mVertBase);
				} else {
					Log.d("XParser", "Illegal face");
				}
			}
			m.face_vert_count = acc - m.face_vert_offset;
			mMaterial.add(m);
		}
		mIdxBase = acc;
		
		getToken(t); // must be '}'
		Log.d("XParser", String.format("Material: %d %d, index total %d", mn, fn, mIdxBase));
	}
	
	private void parseTexCoord() throws IOException {
		Token t = new Token();

		int n = (int) getToken(t);
		getToken(t);
		
		if(mVertNum == n) {
			for(int i = 0; i < n; i++) {
				Vertex v = mVertex.get(i + mVertBase);
				v.uv[0] = getToken(t);
				getToken(t);	// must be ';'
				v.uv[1] = getToken(t);
				nextLine();
			}
			getToken(t);	// must be '}'
			Log.d("XParser", String.format("TexCoord: %d", n));
		} else {
			mIsX = false;
		}
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


}
