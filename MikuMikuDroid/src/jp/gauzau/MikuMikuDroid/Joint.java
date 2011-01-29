package jp.gauzau.MikuMikuDroid;

import java.io.Serializable;

public class Joint implements Serializable {
	private static final long serialVersionUID = -386158105135399782L;
	public String name;
	public int rigidbody_a;
	public int rigidbody_b;
	public float[] position;
	public float[] rotation;
	public float[] const_position_1;
	public float[] const_position_2;
	public float[] const_rotation_1;
	public float[] const_rotation_2;
	public float[] spring_position;
	public float[] spring_rotation;

}
