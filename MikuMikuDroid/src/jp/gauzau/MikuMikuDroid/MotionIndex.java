package jp.gauzau.MikuMikuDroid;

import java.io.Serializable;

public class MotionIndex implements Serializable {
	private static final long serialVersionUID = 7084067791386148510L;
	public int frame_no;
	public int position;
	public float[] location;
	public float[] rotation;
	public byte[] interp;
	// public String bone_name;

}
