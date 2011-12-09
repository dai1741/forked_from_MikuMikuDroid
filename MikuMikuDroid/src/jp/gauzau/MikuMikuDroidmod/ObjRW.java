package jp.gauzau.MikuMikuDroidmod;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class ObjRW {
	public static String readString(ObjectInputStream is) throws IOException {
		if(is.readBoolean()) {
			return is.readUTF();
		} else {
			return null;
		}
	}
	public static int[] readIntA(ObjectInputStream is) throws IOException {
		int len = is.readInt();
		if(len == 0) {
			return null;
		} else {
			int[] d = new int[len];
			for(int i = 0; i < len; i++) {
				d[i] = is.readInt();
			}
			return d;
		}
	}
	
	public static float[] readFloatA(ObjectInputStream is) throws IOException {
		int len = is.readInt();
		if(len == 0) {
			return null;
		} else {
			float[] d = new float[len];
			for(int i = 0; i < len; i++) {
				d[i] = is.readFloat();
			}
			return d;
		}
	}
	
	public static double[] readDoubleA(ObjectInputStream is) throws IOException {
		int len = is.readInt();
		if(len == 0) {
			return null;
		} else {
			double[] d = new double[len];
			for(int i = 0; i < len; i++) {
				d[i] = is.readDouble();
			}
			return d;
		}
	}
	
	public static <T extends SerializableExt> ArrayList<T> readArrayList(ObjectInputStream is, T c) throws IOException, ClassNotFoundException {
		int len = is.readInt();
		if(len == 0) {
			return null;
		} else {
			ArrayList<T> da = new ArrayList<T>();
			da.ensureCapacity(len);
			for(int i = 0; i < len; i++) {
				T data = c.create();
				data.read(is);
				da.add(data);
			}
			return da;
		}
	}
	
	public static void writeString(ObjectOutputStream os, String s) throws IOException {
		if(s == null) {
			os.writeBoolean(false);
		} else {
			os.writeBoolean(true);
			os.writeUTF(s);
		}
	}
	
	public static void writeIntA(ObjectOutputStream os, int[] d) throws IOException {
		if(d == null) {
			os.writeInt(0);
		} else {
			os.writeInt(d.length);
			for(int i = 0; i < d.length; i++) {
				os.writeInt(d[i]);
			}
		}
	}
	
	public static void writeFloatA(ObjectOutputStream os, float[] d) throws IOException {
		if(d == null) {
			os.writeInt(0);
		} else {
			os.writeInt(d.length);
			for(int i = 0; i < d.length; i++) {
				os.writeFloat(d[i]);
			}
		}
	}
	
	public static void writeDoubleA(ObjectOutputStream os, double[] d) throws IOException {
		if(d == null) {
			os.writeInt(0);
		} else {
			os.writeInt(d.length);
			for(int i = 0; i < d.length; i++) {
				os.writeDouble(d[i]);
			}
		}
	}
	
	public static <T extends SerializableExt> void writeArrayList(ObjectOutputStream os, ArrayList<T> c) throws IOException {
		if(c == null) {
			os.writeInt(0);
		} else {
			os.writeInt(c.size());
			for(int i = 0; i < c.size(); i++) {
				T d = c.get(i);
				d.write(os);
			}
		}
	}
}
