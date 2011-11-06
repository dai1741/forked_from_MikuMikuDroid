package jp.gauzau.MikuMikuDroidmod;

import java.io.Serializable;

public class CameraIndex implements Serializable {
	private static final long serialVersionUID = -123458049479150457L;
	public int frame_no;
	public float length;
	public float[] location = new float[3];
	public float[] rotation = new float[3];
	public byte[] interp;
	public float view_angle;
	public byte perspective;

}
