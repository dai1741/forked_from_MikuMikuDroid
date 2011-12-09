package jp.gauzau.MikuMikuDroidmod;

import java.io.Serializable;

public class IK implements Serializable {
	private static final long serialVersionUID = -6685081356124138988L;
	public int ik_bone_index;
	public int ik_target_bone_index;
	public byte ik_chain_length;
	public int iterations;
	public float control_weight;
	public Short[] ik_child_bone_index;

}
