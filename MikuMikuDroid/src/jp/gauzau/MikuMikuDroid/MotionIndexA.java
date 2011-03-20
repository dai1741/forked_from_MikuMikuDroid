package jp.gauzau.MikuMikuDroid;

public class MotionIndexA {
	public int[] frame_no;
	public float[] location;
	public float[] rotation;
	public byte[] interp;
	
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
}
