package jp.gauzau.MikuMikuDroid;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;

public class Material implements Serializable {
	private static final long serialVersionUID = 2851797827233142586L;
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
		index				= mat.index;
		sphere				= mat.sphere;
		rename_map			= mat.rename_map;
		rename_hash			= mat.rename_hash;
		rename_index		= mat.rename_index;
	}

	public Material() {
		diffuse_color			= null;
		power				= 0;
		specular_color		= null;
		emmisive_color		= null;
		toon_index			= 0;
		edge_flag			= 0;
		face_vert_count		= 0;
		texture				= null;
		face_vart_offset	= 0;
		index				= null;
		sphere				= null;
		rename_map			= null;
		rename_hash			= null;
		rename_index		= null;
	}

	public float diffuse_color[];
	public float power;
	public float specular_color[];
	public float emmisive_color[];
	public byte toon_index;
	public byte edge_flag;
	public int face_vert_count;
	public String texture;

	public int face_vart_offset;
	public ShortBuffer index;
	public String sphere;
	public int[] rename_map;
	public HashMap<Integer, Integer> rename_hash;
	public ByteBuffer rename_index;
	public int[] rename_inv_map;
}
