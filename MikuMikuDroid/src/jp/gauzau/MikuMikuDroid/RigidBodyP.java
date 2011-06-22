package jp.gauzau.MikuMikuDroid;

import java.io.Serializable;

public class RigidBodyP implements Serializable {
	private static final long serialVersionUID = -7562525045135372564L;
	public String name;
	public short bone_index;
	public byte group_index;
	public short group_target;
	public byte shape;
	public float[] size;
	public float[] location;
	public float[] rotation;
	public float weight;
	public float v_dim;
	public float r_dim;
	public float recoil;
	public float friction;
	public byte type;
	
	public transient float[] cur_location;
	public transient double[] cur_r;
	public transient double[] cur_v;
	public transient double[] cur_a;
	public transient double[] tmp_r;
	public transient double[] tmp_v;
	public transient double[] tmp_a;
	public transient double[] prev_r;
	
//	public transient RigidBody btrb;

}
