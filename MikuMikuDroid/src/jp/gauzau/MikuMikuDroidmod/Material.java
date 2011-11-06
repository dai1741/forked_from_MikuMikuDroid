package jp.gauzau.MikuMikuDroidmod;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Material {
	public float diffuse_color[];
	public float power;
	public float specular_color[];
	public float emmisive_color[];
	public byte toon_index;
	public byte edge_flag;
	public int face_vert_count;
	public String texture;

	public int face_vert_offset;
	public String sphere;
	public int bone_num;
	public ByteBuffer weight;
	public int[] bone_inv_map;
	public SphereArea area;

	public int lod_face_vert_offset;
	public int lod_face_vert_count;
	
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
		bone_num			= mat.bone_num;
		weight				= mat.weight;
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
		bone_num			= 0;
		weight				= null;
		area				= null;
		lod_face_vert_count	= 0;
		lod_face_vert_offset= 0;
	}
	
	public boolean equals(Material m) {
		boolean t, s;
		if(texture == null && m.texture == null) {
			t = true;
		} else if(texture == null || m.texture == null) {
			t = false;
		} else if(texture.equals(m.texture)) {
			t = true;
		} else {
			t = false;
		}
		if(sphere == null && m.sphere == null) {
			s = true;
		} else if(sphere == null || m.sphere == null) {
			s = false;
		} else if(sphere.equals(m.sphere)) {
			s = true;
		} else {
			s = false;
		}
		
		if(Arrays.equals(emmisive_color, m.emmisive_color) &&
		   Arrays.equals(diffuse_color, m.diffuse_color) &&
		   Arrays.equals(specular_color, m.specular_color) &&
		   power == m.power &&
		   toon_index == m.toon_index &&
		   s && t) {
			return true;
		} else {
			return false;
		}
	}
}
