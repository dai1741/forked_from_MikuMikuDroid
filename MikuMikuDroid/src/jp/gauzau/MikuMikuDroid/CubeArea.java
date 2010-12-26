package jp.gauzau.MikuMikuDroid;

import android.util.Log;

public class CubeArea {
	private float mXMax;
	private float mXMin;
	private float mYMax;
	private float mYMin;
	private float mZMax;
	private float mZMin;

	public CubeArea () {
		mXMax = 0;
		mXMin = 0;
		mYMax = 0;
		mYMin = 0;
		mZMax = 0;
		mZMin = 0;
	}

	public void set(float[] v) {
		
		if(mXMax < v[0]) {
			mXMax = v[0];
		}
		if(mXMin > v[0]) {
			mXMin = v[0];
		}
		if(mYMax < v[1]) {
			mYMax = v[1];
		}
		if(mYMin > v[1]) {
			mYMin = v[1];
		}
		if(mZMax < v[2]) {
			mZMax = v[2];
		}
		if(mZMin > v[2]) {
			mZMin = v[2];
		}
	}
	
	public void logOutput(String tag) {
		Log.d(tag, String.format("Cube Area X: %f - %f, Y: %f - %f, Z: %f - %f", mXMin, mXMax, mYMin, mYMax, mZMin, mZMax));
	}

}
