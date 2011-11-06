package jp.gauzau.MikuMikuDroidmod;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class MotionIndex implements Serializable {
	private static final long serialVersionUID = 7084067791386148510L;
	public transient int frame_no;
	public transient float[] location;
	public transient float[] rotation;
	public transient byte[] interp;

	public void write(ObjectOutputStream os) throws IOException {
		os.writeInt(frame_no);
		for(int i = 0; i < 3; i++) {
			os.writeFloat(location[i]);			
		}
		for(int i = 0; i < 4; i++) {
			os.writeFloat(rotation[i]);
		}
		if(interp == null) {
			os.writeBoolean(false);
		} else {
			os.writeBoolean(true);
			os.write(interp, 0, 16);
		}
	}

	public void read(ObjectInputStream is) throws IOException, ClassNotFoundException {
		frame_no = is.readInt();
		location = new float[3];
		rotation = new float[4];
		for(int i = 0; i < 3; i++) {
			location[i] = is.readFloat();
		}
		for(int i = 0; i < 4; i++) {
			rotation[i] = is.readFloat();
		}
		if(is.readBoolean()) {
			interp = new byte[16];
			is.read(interp, 0, 16);
		} else {
			interp = null;
		}
	}

}
