package jp.gauzau.MikuMikuDroid;

import java.io.Serializable;

public class CameraIndex implements Serializable {
	private static final long serialVersionUID = -123458049479150457L;
	public int frame_no;
	public float length;
	public float[] location;
	public float[] rotation;
	public byte[] interp;
	public float view_angle;
	public byte perspective;

}
