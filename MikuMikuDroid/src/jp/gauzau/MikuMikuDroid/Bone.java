package jp.gauzau.MikuMikuDroid;

import java.util.ArrayList;

public class Bone {

	public short parent;
	public short tail;
	public byte type;
	public short ik;
	public float[] head_pos;
	public ArrayList<MotionIndex> motion;
	public float[] matrix;
	public double[] quaternion;
	public boolean updated;
	public int current_motion;
	public float[] matrix_current;
	public boolean is_leg;
	public byte[] name_bytes;

}
