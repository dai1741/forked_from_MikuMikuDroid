package jp.gauzau.MikuMikuDroid;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class MotionIndexA implements Serializable {
	private static final long serialVersionUID = 1086710533548677117L;
	
	public int[] frame_no;
	public float[] location;
	public float[] rotation;
	public byte[]  interp_x;
	public byte[]  interp_y;
	public byte[]  interp_z;
	public byte[]  interp_a;
	
	// for GLSL
	public int[]   texture = new int[6];
	public byte[]  location_b;
	public byte[]  rotation_b;
	
	public MotionIndexA() {
		frame_no = null;
		location = null;
		rotation = null;
		interp_x = null;
		interp_y = null;
		interp_z = null;
		interp_a = null;
		
		location_b = null;
		rotation_b = null;
	}
	
	public MotionIndexA(int n, boolean allocInterp) {
		frame_no = new int[n];
		location = new float[n*3];
		rotation = new float[n*4];
		location_b = new byte[n*4];	// x, y, z, frame_no for GPU skinning
		rotation_b = new byte[n*4]; // quat for GPU skinning
		if(allocInterp) {
			interp_x = new byte[n*4];			
			interp_x[0] = -1;
			interp_y = new byte[n*4];			
			interp_y[0] = -1;
			interp_z = new byte[n*4];			
			interp_z[0] = -1;
			interp_a = new byte[n*4];			
			interp_a[0] = -1;
		} else {
			interp_x = null;
			interp_y = null;
			interp_z = null;
			interp_a = null;
		}
	}
	
	public void write(ObjectOutputStream os) throws IOException {
		os.writeInt(frame_no.length);

		for(int i = 0; i < frame_no.length; i++) {
			os.writeInt(frame_no[i]);			
		}

		for(int i = 0; i < frame_no.length * 3; i++) {
			os.writeFloat(location[i]);
		}
		for(int i = 0; i < frame_no.length * 4; i++) {
			os.writeFloat(rotation[i]);
		}
		if(interp_x == null) {
			os.writeBoolean(false);
		} else {
			os.writeBoolean(true);
			os.write(interp_x, 0, frame_no.length * 4);
			os.write(interp_y, 0, frame_no.length * 4);
			os.write(interp_z, 0, frame_no.length * 4);
			os.write(interp_a, 0, frame_no.length * 4);
		}
	}

	public void read(ObjectInputStream is) throws IOException, ClassNotFoundException {
		int s = is.readInt();
		frame_no = new int[s];
		location = new float[s * 3];
		rotation = new float[s * 4];
		for(int i = 0; i < s; i++) {
			frame_no[i] = is.readInt();
		}
		for(int i = 0; i < s * 3; i++) {
			location[i] = is.readFloat();
		}
		for(int i = 0; i < s * 4; i++) {
			rotation[i] = is.readFloat();
		}
		if(is.readBoolean()) {
			interp_x = new byte[s * 4];
			is.read(interp_x, 0, s * 4);
			interp_y = new byte[s * 4];
			is.read(interp_y, 0, s * 4);
			interp_z = new byte[s * 4];
			is.read(interp_z, 0, s * 4);
			interp_a = new byte[s * 4];
			is.read(interp_a, 0, s * 4);
		} else {
			interp_x = null;
			interp_y = null;
			interp_z = null;
			interp_a = null;
		}
	}
}
