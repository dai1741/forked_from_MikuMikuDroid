package jp.gauzau.MikuMikuDroid;

import java.io.Serializable;

public class Bone implements Serializable {
	private static final long serialVersionUID = 7842056430910376644L;
	public String name;
	public byte[] name_bytes;
	public short parent;
	public short tail;
	public byte type;
	public short ik;
	public float[] head_pos;
	public boolean is_leg;
	
	public transient MotionIndexA motion;
	public transient int current_motion;
	public transient float[] matrix;
	public transient float[] matrix_current;
	public transient double[] quaternion;
	public transient boolean updated;

}
