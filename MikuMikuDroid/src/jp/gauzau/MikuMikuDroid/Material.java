package jp.gauzau.MikuMikuDroid;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.HashMap;

public class Material implements Serializable, SerializableExt {
	private static final long serialVersionUID = 2851797827233142586L;

	public transient float diffuse_color[];
	public transient float power;
	public transient float specular_color[];
	public transient float emmisive_color[];
	public transient byte toon_index;
	public transient byte edge_flag;
	public transient int face_vert_count;
	public transient String texture;

	public transient int face_vart_offset;
	public transient String sphere;
	public transient int[] rename_map;
	public transient HashMap<Integer, Integer> rename_hash;
	public transient int rename_hash_size;
	public transient ShortBuffer rename_index;
	public transient int[] rename_inv_map;
	
	public Material(Material mat) {
		diffuse_color		= mat.diffuse_color;
		power				= mat.power;
		specular_color		= mat.specular_color;
		emmisive_color		= mat.emmisive_color;
		toon_index			= mat.toon_index;
		edge_flag			= mat.edge_flag;
		face_vert_count		= mat.face_vert_count;
		texture				= mat.texture;
		face_vart_offset	= mat.face_vart_offset;
		sphere				= mat.sphere;
		rename_map			= mat.rename_map;
		rename_hash			= mat.rename_hash;
		rename_hash_size	= mat.rename_hash_size;
		rename_index		= mat.rename_index;
	}

	public Material() {
		diffuse_color		= null;
		power				= 0;
		specular_color		= null;
		emmisive_color		= null;
		toon_index			= 0;
		edge_flag			= 0;
		face_vert_count		= 0;
		texture				= null;
		face_vart_offset	= 0;
		sphere				= null;
		rename_map			= null;
		rename_hash			= null;
		rename_hash_size	= 0;
		rename_index		= null;
	}

	public Material create() {
		return new Material();
	}
	
	public void write(ObjectOutputStream os) throws IOException {
		ObjRW.writeFloatA(os, diffuse_color);
		os.writeFloat(power);
		ObjRW.writeFloatA(os, specular_color);
		ObjRW.writeFloatA(os, emmisive_color);
		os.writeByte(toon_index);
		os.writeByte(edge_flag);
		os.writeInt(face_vert_count);
		ObjRW.writeString(os, texture);
		
		os.writeInt(face_vart_offset);
		ObjRW.writeString(os, sphere);
		os.writeInt(rename_hash_size);
//		ObjRW.writeIntA(os, rename_map);

		// rename_index
		if(rename_index == null) {
			os.writeInt(0);
		} else {
			os.writeInt(rename_index.capacity());
			rename_index.position(0);
			for(int i = 0; i < rename_index.capacity(); i++) {
				os.writeShort(rename_index.get());
			}
			rename_index.position(0);
		}
		
		ObjRW.writeIntA(os, rename_inv_map);
		os.reset();
		os.flush();
	}
	
	public void read(ObjectInputStream is) throws IOException {
		diffuse_color	= ObjRW.readFloatA(is);
		power			= is.readFloat();
		specular_color	= ObjRW.readFloatA(is);
		emmisive_color	= ObjRW.readFloatA(is);
		toon_index		= is.readByte();
		edge_flag		= is.readByte();
		face_vert_count	= is.readInt();
		texture			= ObjRW.readString(is);
		
		face_vart_offset= is.readInt();
		sphere			= ObjRW.readString(is);
		rename_hash_size= is.readInt();
//		rename_map		= ObjRW.readIntA(is);
		
		// rename_index
		int len = is.readInt();
		if(len == 0) {
			rename_index = null;
		} else {
			ByteBuffer bb = ByteBuffer.allocateDirect(len/2);
			bb.order(ByteOrder.nativeOrder());
			rename_index = bb.asShortBuffer();
			for(int i = 0; i < rename_index.capacity(); i++) {
				rename_index.put(is.readShort());
			}
			rename_index.position(0);
		}
		
		rename_inv_map = ObjRW.readIntA(is);
	}
	
	private void writeObject(ObjectOutputStream os) throws IOException {
		os.defaultWriteObject();
		write(os);
	}

	private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
		is.defaultReadObject();
		read(is);
	}
}
