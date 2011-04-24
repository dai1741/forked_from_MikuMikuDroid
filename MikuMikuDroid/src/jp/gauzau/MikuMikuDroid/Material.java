package jp.gauzau.MikuMikuDroid;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

	public transient int face_vert_offset;
	public transient String sphere;
	public transient HashMap<Integer, Integer> rename_hash;
	public transient int bone_num;
	public transient ByteBuffer weight;
	public transient int[] bone_inv_map;
	public transient SphereArea area;

	public transient int lod_face_vert_offset;
	public transient int lod_face_vert_count;
	
	public Material(Material mat) {
		diffuse_color		= mat.diffuse_color;
		power				= mat.power;
		specular_color		= mat.specular_color;
		emmisive_color		= mat.emmisive_color;
		toon_index			= mat.toon_index;
		edge_flag			= mat.edge_flag;
		face_vert_count		= mat.face_vert_count;
		texture				= mat.texture;
		face_vert_offset	= mat.face_vert_offset;
		sphere				= mat.sphere;
		rename_hash			= mat.rename_hash;
		bone_num	= mat.bone_num;
		weight		= mat.weight;
		area				= mat.area;
		lod_face_vert_count	= mat.lod_face_vert_count;
		lod_face_vert_offset= mat.lod_face_vert_offset;
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
		face_vert_offset	= 0;
		sphere				= null;
		rename_hash			= null;
		bone_num			= 0;
		weight				= null;
		area				= null;
		lod_face_vert_count	= 0;
		lod_face_vert_offset= 0;
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
		
		os.writeInt(face_vert_offset);
		ObjRW.writeString(os, sphere);
		os.writeInt(bone_num);
//		ObjRW.writeIntA(os, rename_map);

		// rename_index
		if(weight == null) {
			os.writeInt(0);
		} else {
			os.writeInt(weight.capacity());
			weight.position(0);
			for(int i = 0; i < weight.capacity(); i++) {
				os.writeByte(weight.get());
			}
			weight.position(0);
		}
		
		ObjRW.writeIntA(os, bone_inv_map);
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
		
		face_vert_offset= is.readInt();
		sphere			= ObjRW.readString(is);
		bone_num= is.readInt();
//		rename_map		= ObjRW.readIntA(is);
		
		// rename_index
		int len = is.readInt();
		if(len == 0) {
			weight = null;
		} else {
			ByteBuffer bb = ByteBuffer.allocateDirect(len/2);
			bb.order(ByteOrder.nativeOrder());
			weight = bb;
			for(int i = 0; i < weight.capacity(); i++) {
				weight.put(is.readByte());
			}
			weight.position(0);
		}
		
		bone_inv_map = ObjRW.readIntA(is);
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
