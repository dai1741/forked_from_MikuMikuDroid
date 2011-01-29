package jp.gauzau.MikuMikuDroid;

import java.io.Serializable;
import java.util.ArrayList;

public class Face implements Serializable {
	private static final long serialVersionUID = 3872043545105902643L;
	public String name;
	public int face_vert_count;
	public byte face_type;
	public ArrayList<FaceVertData> face_vert_data;
	
	public transient ArrayList<FaceIndex> motion;
	public transient int current_motion;

}
