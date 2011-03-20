package jp.gauzau.MikuMikuDroid;

public class FaceIndexA {
	public int[] frame_no;
	public float[] weight;
	
	public FaceIndexA(int n) {
		frame_no = new int[n];
		weight   = new float[n];
	}
}
