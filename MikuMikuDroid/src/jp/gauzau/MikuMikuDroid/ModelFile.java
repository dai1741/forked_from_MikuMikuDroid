package jp.gauzau.MikuMikuDroid;

import java.util.ArrayList;

public interface ModelFile {
	public boolean isPmd();

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
