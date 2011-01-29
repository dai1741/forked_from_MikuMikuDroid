package jp.gauzau.MikuMikuDroid;

import java.io.Serializable;

public class FaceVertData implements Serializable {
	private static final long serialVersionUID = -7685611311850201290L;
	public int face_vert_index;
	public float[] offset;
	public float[] base;
	public boolean updated;
	public boolean cleared;

}
