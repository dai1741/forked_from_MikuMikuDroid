package jp.gauzau.MikuMikuDroidmod;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

public interface ModelFile {
	public boolean isPmd();
	
	public FloatBuffer getVertexBuffer();
	
	public IntBuffer getIndexBufferI();

	public ShortBuffer getIndexBufferS();

	public ShortBuffer getWeightBuffer();

	public ArrayList<Vertex> getVertex();

	public ArrayList<Integer> getIndex();

	public ArrayList<Material> getMaterial();

	public ArrayList<Bone> getBone();

	public ArrayList<String> getToonFileName();

	public ArrayList<IK> getIK();

	public ArrayList<Face> getFace();
	
	public ArrayList<RigidBody> getRigidBody();
	
	public ArrayList<Joint> getJoint();
	
	public String getFileName();
	
	public boolean isOneSkinning();

	public void recycle();

	public void recycleVertex();

}
