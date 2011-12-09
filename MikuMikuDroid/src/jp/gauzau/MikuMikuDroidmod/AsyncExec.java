package jp.gauzau.MikuMikuDroidmod;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public abstract class AsyncExec<T> extends AsyncTask <T, Integer, Void> {
	private ProgressDialog	mProg;
	protected ArrayList<T>	mFail;

	public AsyncExec(Context ctx) {
		mProg = new ProgressDialog(ctx);
		mProg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProg.setCancelable(false);
		/*
		mProg.setCancelable(true);
		mProg.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				cancel(true);
			}
		});
		*/
		mFail = new ArrayList<T>();
	}
	
	public AsyncExec(ProgressDialog prog) {
		mProg = prog;
		mFail = new ArrayList<T>();
	}
	
	public void setMax(int max) {
		mProg.setMax(max);
	}
	
	public void setMessage(String msg) {
		mProg.setMessage(msg);
	}
	
	protected abstract boolean exec(T target);
	
	protected void post() {}
	
	@Override
	protected void onPreExecute() {
		mProg.show();
	}
	
	@Override
	protected Void doInBackground(T... params) {
		for(int i = 0; i < params.length; i++) {
			if(isCancelled()) {
				break;
			}			
			if(!exec(params[i])) {
				mFail.add(params[i]);
			}
			publishProgress(i);
		}
		return null;
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		mProg.setProgress(progress[0]);
	}
	
	@Override
	protected void onPostExecute(Void v) {
		mProg.dismiss();
		mProg = null;
		post();
	}
};


