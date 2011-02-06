package jp.gauzau.MikuMikuDroid;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MikuMikuDroid extends Activity {
	// View
	private MMGLSurfaceView mMMGLSurfaceView;
	
	// Model
	private CoreLogic mCoreLogic;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCoreLogic = new CoreLogic(this.getApplicationContext());
		mCoreLogic.setScreenAngle(0);

		mMMGLSurfaceView = new MMGLSurfaceView(this, mCoreLogic);
		setContentView(mMMGLSurfaceView);

		if (mCoreLogic.checkFileIsPrepared() == false) {
			Builder ad;
			ad = new AlertDialog.Builder(this);
			ad.setTitle(R.string.setup_alert_title);
			ad.setMessage(R.string.setup_alert_text);
			ad.setPositiveButton(R.string.select_ok, null);
			ad.show();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mMMGLSurfaceView.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mCoreLogic.pause();
		mMMGLSurfaceView.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);

		menu.add(0, Menu.FIRST,     Menu.NONE, R.string.menu_load_model);
		menu.add(0, Menu.FIRST + 1, Menu.NONE, R.string.menu_load_camera);
		menu.add(0, Menu.FIRST + 2, Menu.NONE, R.string.menu_load_music);
		menu.add(0, Menu.FIRST + 3, Menu.NONE, R.string.menu_play_pause);
		menu.add(0, Menu.FIRST + 4, Menu.NONE, R.string.menu_rewind);
		menu.add(0, Menu.FIRST + 5, Menu.NONE, R.string.menu_initialize);

		return ret;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case (Menu.FIRST + 0):
			final CoreLogic.Selection sc0 = mCoreLogic.getModelSelector();
			openSelectDialog(sc0.item, R.string.menu_load_model, R.string.setup_alert_pmd,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final String model = sc0.task.select(which);
						final CoreLogic.Selection sc = mCoreLogic.getMotionSelector();
						openSelectDialog(sc.item, R.string.menu_load_motion, R.string.setup_alert_vmd,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, final int which) {
									final String motion = sc.task.select(which);
									new AsyncTask<Void, Void, Void>() {
										@Override
										protected Void doInBackground(Void... params) {
											try {
												if(which == 0) {
													mCoreLogic.loadStage(model);
												} else {
													mCoreLogic.loadModelMotion(model, motion);											
												}
											} catch (IOException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}										
											return null;
										}
									}.execute();
								}
						});
					}
				});
			break;

		case (Menu.FIRST + 1):
			final CoreLogic.Selection sc1 = mCoreLogic.getCameraSelector();
			openSelectDialog(sc1.item, R.string.menu_load_camera, R.string.setup_alert_vmd,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final String camera = sc1.task.select(which);
						new AsyncTask <Void, Void, Void>() {
							@Override
							protected Void doInBackground(Void... params) {
								try {
									mCoreLogic.loadCamera(camera);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								return null;
							}
							
						}.execute();
					}
				});
			break;

		case (Menu.FIRST + 2):
			final CoreLogic.Selection sc2 = mCoreLogic.getMediaSelector();
			openSelectDialog(sc2.item, R.string.menu_load_music, R.string.setup_alert_music,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final String media = sc2.task.select(which);
						new AsyncTask <Void, Void, Void>() {
							@Override
							protected Void doInBackground(Void... params) {
								mCoreLogic.loadMedia(media);
								return null;
							}
							
						}.execute();
					}
				});
			break;
			
		case (Menu.FIRST + 3):
			mCoreLogic.toggleStartStop();
			break;
			
		case (Menu.FIRST + 4):
			mCoreLogic.rewind();
			break;

		case (Menu.FIRST + 5):
			mCoreLogic.clear();
			break;

		default:
			;
		}

		return super.onOptionsItemSelected(item);
	}
	
	private void openSelectDialog(String[] item, int title, int alert, DialogInterface.OnClickListener task) {
		Builder ad = new AlertDialog.Builder(this);
		if (item == null) {
			ad.setTitle(R.string.setup_alert_title);
			ad.setMessage(alert);
			ad.setPositiveButton(R.string.select_ok, null);
		} else {
			ad.setTitle(title);
			ad.setItems(item, task);
		}
		ad.show();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mCoreLogic.storeState();
	}
}