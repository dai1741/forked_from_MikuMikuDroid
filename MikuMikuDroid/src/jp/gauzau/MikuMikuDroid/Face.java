package jp.gauzau.MikuMikuDroid;

import java.io.Serializable;
import java.util.ArrayList;

public class Face implements Serializable {
	private static final long serialVersionUID = 3872043545105902643L;
	public String name;
	public int face_vert_count;
	public byte face_type;
	public int[] face_vert_index;
	public float[] face_vert_offset;
	public float[] face_vert_base;
	public boolean[] face_vert_cleared;
	public boolean[] face_vert_updated;
	
	public transient FaceIndexA motion;
	public transient int current_motion;

}
