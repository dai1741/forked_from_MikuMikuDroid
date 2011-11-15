package jp.gauzau.MikuMikuDroidmod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class MikuMikuDroid extends Activity implements SensorEventListener {
	// View
	private MMGLSurfaceView mMMGLSurfaceView;
	private RelativeLayout mRelativeLayout;
	private SeekBar mSeekBar;
	private Button mPlayPauseButton;
    private Button mRewindButton;
    private Button mCameraResetButton;
	
	// Model
	private CoreLogic mCoreLogic;
	
	// Sensor
	SensorManager	mSM = null;
	Sensor			mAx = null;
	Sensor			mMg = null;
	float[]			mAxV = new float[3];
	float[]			mMgV = new float[3];

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
//		mSM = (SensorManager)getSystemService(SENSOR_SERVICE);
//		mAx = mSM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//		mMg = mSM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		mCoreLogic = new CoreLogic(this) {
			@Override
			public void onInitialize() {
				MikuMikuDroid.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						AsyncExec<CoreLogic> ae = new AsyncExec<CoreLogic>(MikuMikuDroid.this) {
							@Override
							protected boolean exec(CoreLogic target) {
								try {
									mCoreLogic.restoreState();
									final int max = target.getDulation();
									mSeekBar.post(new Runnable() {
										@Override
										public void run() {
											mSeekBar.setMax(max);
										}
									});
								} catch (OutOfMemoryError e) {
									return false;
								}

								return true;
							}
							
							@Override
							public void post() {
								if(mFail.size() != 0) {
									Toast.makeText(MikuMikuDroid.this, "Out of Memory. Abort.", Toast.LENGTH_LONG).show();										
								}
								
							}
						};
						ae.setMax(1);
						if(mCoreLogic.restoresState()) {
    						ae.setMessage("Restoring Previous state...");
    						ae.execute(mCoreLogic);
						}
					}
				});
			}
			
			@Override
			public void onDraw(final int pos) {
				MikuMikuDroid.this.mSeekBar.post(new Runnable() {
					@Override
					public void run() {
						MikuMikuDroid.this.mSeekBar.setProgress(pos);
					}
				});
			}

            @Override
            protected void onCompletion() {
                super.onCompletion();
                mPlayPauseButton.post(new Runnable() {
                    public void run() {
                        mPlayPauseButton.setBackgroundResource(R.drawable.ic_media_play);
                    }
                });
            }

            @Override
            protected boolean restoresState() {
                return PreferenceManager.getDefaultSharedPreferences(MikuMikuDroid.this)
                        .getBoolean(getResources().getString(
                                        R.string.pref_key_save_last_state), true);
            }
            
            
		};
		mCoreLogic.setScreenAngle(0);
        
        int bgType = SettingsHelper.getBgType(this);
//        if(SettingsHelper.bgUsesWindowAlpha(bgType)) setTheme(android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
//        else setTheme(android.R.style.Theme_NoTitleBar_Fullscreen);

		mRelativeLayout = new RelativeLayout(this);
		mRelativeLayout.setVerticalGravity(Gravity.BOTTOM);
		mMMGLSurfaceView = new MMGLSurfaceView(this, mCoreLogic, bgType);
	
		LayoutParams p = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		mSeekBar = new SeekBar(this);
		mSeekBar.setLayoutParams(p);
		mSeekBar.setId(1024);
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			private boolean mIsPlaying = false;

			@Override
			public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
				if(fromUser) {
					mCoreLogic.seekTo(progress);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				if(mCoreLogic.isPlaying()) {
					mCoreLogic.pause();
					mIsPlaying = true;
				}
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if(mIsPlaying) {
					mCoreLogic.toggleStartStop();
					mIsPlaying = false;
				}
			}
			
		});
		
		p = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		p.addRule(RelativeLayout.CENTER_HORIZONTAL);
		p.addRule(RelativeLayout.ABOVE, mSeekBar.getId());
		p.setMargins(5, 5, 5, 60);
		mPlayPauseButton = new Button(this);
		mPlayPauseButton.setLayoutParams(p);
		mPlayPauseButton.setBackgroundResource(R.drawable.ic_media_play);
		mPlayPauseButton.setId(mSeekBar.getId() + 1);
		mPlayPauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mCoreLogic.toggleStartStop()) {
					mPlayPauseButton.setBackgroundResource(R.drawable.ic_media_pause);
				} else {
					mPlayPauseButton.setBackgroundResource(R.drawable.ic_media_play);					
				}
			}
		});

		p = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		p.addRule(RelativeLayout.ABOVE, mSeekBar.getId());
		p.addRule(RelativeLayout.LEFT_OF, mPlayPauseButton.getId());
		p.setMargins(5, 5, 60, 60);
		mRewindButton = new Button(this);
		mRewindButton.setLayoutParams(p);
		mRewindButton.setBackgroundResource(R.drawable.ic_media_previous);
		mRewindButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mCoreLogic.rewind();
			}
		});

        p = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        p.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        p.setMargins(5, 5, 5, 5);
        mCameraResetButton = new Button(this);
        mCameraResetButton.setLayoutParams(p);
        mCameraResetButton.setBackgroundResource(R.drawable.ic_media_camera_reset);
        mCameraResetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MikuMikuDroid.this.mGestureListener.reset();
            }
        });
        
        toggleUiViewVisibilities();

		mRelativeLayout.addView(mMMGLSurfaceView);
		if(bgType == SettingsHelper.BG_CAMERA) {
		    mRelativeLayout.addView(new CameraPreviewView(this));
		}
		mRelativeLayout.addView(mSeekBar);
		mRelativeLayout.addView(mPlayPauseButton);
        mRelativeLayout.addView(mRewindButton);
        mRelativeLayout.addView(mCameraResetButton);
		setContentView(mRelativeLayout);

		if (mCoreLogic.checkFileIsPrepared() == false) {
			Builder ad;
			ad = new AlertDialog.Builder(this);
			ad.setTitle(R.string.setup_alert_title);
			ad.setMessage(R.string.setup_alert_text);
			ad.setPositiveButton(R.string.select_ok, null);
			ad.show();
		}
		
		mGestureListener = getGestureListener();
		mGestureDetector = new GestureDetector(this, mGestureListener, null, true);
		mScaleGestureDetector = new ScaleGestureDetector(this, mGestureListener);
	}

    private GestureDetector mGestureDetector;  
    private ScaleGestureDetector mScaleGestureDetector;
	
	@Override
	protected void onResume() {
		super.onResume();
		mMMGLSurfaceView.onResume();
		if(mAx != null && mMg != null) {
			mSM.registerListener(this, mAx, SensorManager.SENSOR_DELAY_GAME);
			mSM.registerListener(this, mMg, SensorManager.SENSOR_DELAY_GAME);			
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mCoreLogic.pause();
		mMMGLSurfaceView.onPause();
		if(mAx != null && mMg != null) {
			mSM.unregisterListener(this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);

		menu.add(0, Menu.FIRST,     Menu.NONE, R.string.menu_load_model);
		menu.add(0, Menu.FIRST + 1, Menu.NONE, R.string.menu_load_camera);
		menu.add(0, Menu.FIRST + 2, Menu.NONE, R.string.menu_load_music);
		menu.add(0, Menu.FIRST + 3, Menu.NONE, R.string.menu_take_picture);
		menu.add(0, Menu.FIRST + 4, Menu.NONE, R.string.menu_initialize);
        menu.add(0, Menu.FIRST + 5, Menu.NONE, R.string.menu_play_pause);
		menu.add(0, Menu.FIRST + 6, Menu.NONE, R.string.menu_toggle_physics);
        menu.add(0, Menu.FIRST + 7, Menu.NONE, R.string.menu_toggle_repeat);
        menu.add(0, Menu.FIRST + 8, Menu.NONE, R.string.menu_settings);

		return ret;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case (Menu.FIRST + 0):
			final File[] sc0 = mCoreLogic.getModelSelector();
			openSelectDialog(sc0, R.string.menu_load_model, R.string.setup_alert_pmd, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String model = sc0[which].getPath();
					
					// read as background if not .pmd
					if(!model.endsWith(".pmd")) {
						if(model.endsWith(".x")) { // accessory
							AsyncExec<CoreLogic> ae = new AsyncExec<CoreLogic>(MikuMikuDroid.this) {
								@Override
								protected boolean exec(CoreLogic target) {
									try {
										mCoreLogic.loadAccessory(model);
										mCoreLogic.storeState();
									} catch (OutOfMemoryError e) {
										return false;
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									return true;
								}
								
								@Override
								public void post() {
									if(mFail.size() != 0) {
										Toast.makeText(MikuMikuDroid.this, "Out of Memory. Abort.", Toast.LENGTH_LONG).show();										
									}
								}
							};
							ae.setMax(1);
							ae.setMessage("Loading Model/Motion...");
							ae.execute(mCoreLogic);
						} else {
							mMMGLSurfaceView.deleteTexture(mCoreLogic.loadBG(model));							
						}
						return ;
					}
					
					final File[] sc = mCoreLogic.getMotionSelector();
					openMotionSelectDialog(sc, R.string.menu_load_motion, R.string.setup_alert_vmd, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, final int which) {
							final String motion = which == 0 ? null : sc[which-1].getPath();
							AsyncExec<CoreLogic> ae = new AsyncExec<CoreLogic>(MikuMikuDroid.this) {
								@Override
								protected boolean exec(CoreLogic target) {
									try {
										if(which == 0) {
											MikuModel m = target.loadStage(model);
											if(m != null) {
												ArrayList<MikuModel> mm = new ArrayList<MikuModel>(1);
												mm.add(m);
												mMMGLSurfaceView.deleteTextures(mm);
											}
										} else {
											target.loadModelMotion(model, motion);
											final int max = target.getDulation();
											mSeekBar.post(new Runnable() {
												@Override
												public void run() {
													mSeekBar.setMax(max);
												}
											});
										}
										mCoreLogic.storeState();
									} catch (OutOfMemoryError e) {
										return false;
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									return true;
								}
								
								@Override
								public void post() {
									if(mFail.size() != 0) {
										Toast.makeText(MikuMikuDroid.this, "Out of Memory. Abort.", Toast.LENGTH_LONG).show();										
									}
								}
							};
							ae.setMax(1);
							ae.setMessage("Loading Model/Motion...");
							ae.execute(mCoreLogic);
						}
					});
				}
			});
			break;

		case (Menu.FIRST + 1):
			final File[] sc1 = mCoreLogic.getCameraSelector();
			openSelectDialog(sc1, R.string.menu_load_camera, R.string.setup_alert_vmd, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String camera = sc1[which].getPath();
					new AsyncTask <Void, Void, Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							try {
								mCoreLogic.loadCamera(camera);
								mCoreLogic.storeState();
								final int max = mCoreLogic.getDulation();
								mSeekBar.post(new Runnable() {
									@Override
									public void run() {
										mSeekBar.setMax(max);
									}
								});
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
			final File[] sc2 = mCoreLogic.getMediaSelector();
			openSelectDialog(sc2, R.string.menu_load_music, R.string.setup_alert_music, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String media = "file://" + sc2[which].getPath();
					new AsyncTask <Void, Void, Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							mCoreLogic.loadMedia(media);
							mCoreLogic.storeState();
							final int max = mCoreLogic.getDulation();
							mSeekBar.post(new Runnable() {
								@Override
								public void run() {
									mSeekBar.setMax(max);
								}
							});
							return null;
						}
					}.execute();
				}
			});
			break;
			
		case (Menu.FIRST + 3):
		    new AsyncTask<Void, Void, Void>() {
		        @Override
                public Void doInBackground(Void... params) {
                    Bitmap b = mMMGLSurfaceView.getCurrentFrameBitmap();
                    String prefix = "pict";
                    if (!mCoreLogic.getMiku().isEmpty()) {
                        prefix = new File(mCoreLogic.getMiku().get(0).mModel.mFileName)
                                .getName();
                        prefix = prefix.replaceAll("^\\.|\\..+$", "").replaceAll(
                                "[:;/\\\\\\|,*?\"<>]", "");
                    }
                    final String path = mCoreLogic.getBase() + "MMDroidPicture/";
                    //TODO: correct file path
                    new File(path).mkdir();
                    File fileToSave = new File(path
                            + prefix
                            + String.format("-%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS.png",
                                    new Date()));
                    OutputStream os;
                    final boolean[] succeeded = new boolean[1];
                    try {
                        if (fileToSave.exists()) throw new IOException();
                        os = new FileOutputStream(fileToSave);
                        b.compress(Bitmap.CompressFormat.PNG, 100, os);
                        os.close();
                        succeeded[0] = true;
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        succeeded[0] = false;
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast toast = Toast.makeText(MikuMikuDroid.this,
                            String.format(getResources().getString(
                                    R.string.toast_picture_taken), path),
                            Toast.LENGTH_LONG);
                            if (!succeeded[0]) toast.setText(R.string.toast_picture_failed);
                            toast.show();
                        }
                    });
                    return null;
                }
            }.execute();
            break;

		case (Menu.FIRST + 4):
			mMMGLSurfaceView.deleteTextures(mCoreLogic.clear());
		    mGestureListener.reset();
			break;
            
        case (Menu.FIRST + 5):
            mCoreLogic.toggleStartStop();
            break;
			
		case (Menu.FIRST + 6):
			mCoreLogic.togglePhysics();
			break;
            
        case (Menu.FIRST + 7):
            mCoreLogic.toggleRepeating();
            break;

        case (Menu.FIRST + 8):
            startActivity(new Intent(this, SettingsActivity.class));
            break;

		default:
			;
		}

		return super.onOptionsItemSelected(item);
	}
	
	private void openSelectDialog(File[] item, int title, int alert, DialogInterface.OnClickListener task) {
		Builder ad = new AlertDialog.Builder(this);
		if (item == null) {
			ad.setTitle(R.string.setup_alert_title);
			ad.setMessage(alert);
			ad.setPositiveButton(R.string.select_ok, null);
		} else {
			ad.setTitle(title);
			String[] is = new String[item.length];
			for(int i = 0; i < item.length; i++) {
				is[i] = item[i].getName();
				int idx = is[i].lastIndexOf(".");
				is[i] = is[i].substring(0, idx);
			}
			ad.setItems(is, task);
		}
		ad.show();
	}

	private void openMotionSelectDialog(File[] item, int title, int alert, DialogInterface.OnClickListener task) {
		Builder ad = new AlertDialog.Builder(this);
		if (item == null) {
			ad.setTitle(R.string.setup_alert_title);
			ad.setMessage(alert);
			ad.setPositiveButton(R.string.select_ok, null);
		} else {
			ad.setTitle(title);
			String[] is = new String[item.length+1];
			is[0] = "Load as Background";
			for(int i = 1; i < is.length; i++) {
				is[i] = item[i-1].getName();
				int idx = is[i].lastIndexOf(".");
				is[i] = is[i].substring(0, idx);
			}
			ad.setItems(is, task);
		}
		ad.show();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
	    mGestureDetector.onTouchEvent(event);
	    mScaleGestureDetector.onTouchEvent(event);
		return false;
	}
    
	private CameraLocrotscaleGestureListener mGestureListener;
    private CameraLocrotscaleGestureListener getGestureListener() {
        
        return new CameraLocrotscaleGestureListener(mCoreLogic) {

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (super.onSingleTapUp(e)) return true;
                
                if (mCoreLogic.isPlaying()) {
                    mPlayPauseButton.setBackgroundResource(R.drawable.ic_media_pause);
                } else {
                    mPlayPauseButton.setBackgroundResource(R.drawable.ic_media_play);
                }
                
                toggleUiViewVisibilities();
                mRelativeLayout.requestLayout();
                return false;
            }
        };
    }
    
    private void toggleUiViewVisibilities() {
        toggleViewVisibility(mSeekBar);
        toggleViewVisibility(mPlayPauseButton);
        toggleViewVisibility(mRewindButton);
        toggleViewVisibility(mCameraResetButton);
    }
    
    private static void toggleViewVisibility(View view) {
        view.setVisibility(view.getVisibility() == View.VISIBLE
                ? View.INVISIBLE
                : View.VISIBLE);
    }
	
	@Override
	public void onSaveInstanceState(Bundle bundle) {
		mCoreLogic.storeState();
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

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
			return;
		}
	
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			System.arraycopy(event.values, 0, mAxV, 0, 3);
		} else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			System.arraycopy(event.values, 0, mMgV, 0, 3);			
		}
		
		SensorManager.getRotationMatrix(mCoreLogic.getRotationMatrix(), null, mAxV, mMgV);
	}
}