package jp.gauzau.MikuMikuDroid;

import java.util.ArrayList;

public class Face {

	public String name;
	public int face_vert_count;
	public byte face_type;
	public ArrayList<FaceVertData> face_vert_data;
	public ArrayList<FaceIndex> motion;
	public int current_motion;

}
