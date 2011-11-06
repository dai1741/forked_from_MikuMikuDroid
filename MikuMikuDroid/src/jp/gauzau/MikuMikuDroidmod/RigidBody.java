package jp.gauzau.MikuMikuDroidmod;

public class RigidBody {
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
	
	public transient int btrb;

}
