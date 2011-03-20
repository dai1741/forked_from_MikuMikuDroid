package jp.gauzau.MikuMikuDroid;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class MotionIndexA implements Serializable {
	private static final long serialVersionUID = -1960035825451794029L;
	
	public int[] frame_no;
	public float[] location;
	public float[] rotation;
	public byte[] interp;
	
	public MotionIndexA() {
		frame_no = null;
		location = null;
		rotation = null;
		interp   = null;
	}
	
	public MotionIndexA(int n, boolean allocInterp) {
		frame_no = new int[n];
		location = new float[n*3];
		rotation = new float[n*4];
		if(allocInterp) {
			interp   = new byte[n*16];			
			interp[0] = -1;
		} else {
			interp   = null;
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
		if(interp == null) {
			os.writeBoolean(false);
		} else {
			os.writeBoolean(true);
			os.write(interp, 0, frame_no.length * 16);
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
			interp = new byte[s * 16];
			is.read(interp, 0, s * 16);
		} else {
			interp = null;
		}
	}
}
