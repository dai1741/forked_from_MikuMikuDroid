package jp.gauzau.MikuMikuDroidmod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class MikuMikuDroid extends Activity implements SensorEventListener {
	private static final String TAG = "MikuMikuDroid";
	
    // View
	private MMGLSurfaceView mMMGLSurfaceView;
	private RelativeLayout mRelativeLayout;
	private SeekBar mSeekBar;
	private Button mPlayPauseButton;
    private Button mRewindButton;
    private Button mCameraResetButton;
    private CameraPreviewView mCameraPreviewView;
    private RelativeLayout mPictureLayout;
	
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
        
        int bgType = SettingsHelper.getBgType(this);
//        if(SettingsHelper.bgUsesWindowAlpha(bgType)) setTheme(android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
//        else setTheme(android.R.style.Theme_NoTitleBar_Fullscreen);
        
        if (SettingsHelper.bgUsesWindowAlpha(bgType)
                && !(this instanceof TranslucentMikuMikuDroid)) {
            Intent i = new Intent(this, TranslucentMikuMikuDroid.class);
            startActivity(i);
            finish();
            return;
        }

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
    						ae.setMessage(getResources()
    						        .getString(R.string.dialog_restore_state));
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
        
        mPictureLayout = new RelativeLayout(this);
        mPictureLayout.setVisibility(View.INVISIBLE);
        
        toggleUiViewVisibilities();

		mRelativeLayout.addView(mMMGLSurfaceView);
		if(bgType == SettingsHelper.BG_CAMERA) {
		    mCameraPreviewView = new CameraPreviewView(this);
		    mRelativeLayout.addView(mCameraPreviewView);
		}
		mRelativeLayout.addView(mSeekBar);
		mRelativeLayout.addView(mPlayPauseButton);
        mRelativeLayout.addView(mRewindButton);
        mRelativeLayout.addView(mCameraResetButton);
        mRelativeLayout.addView(mPictureLayout);
		setContentView(mRelativeLayout);

		if (mCoreLogic.checkFileIsPrepared() == false
		        || !mMMGLSurfaceView.isGLES20Available()) {
			Builder ad;
			ad = new AlertDialog.Builder(this);
			ad.setTitle(R.string.setup_alert_title);
			ad.setMessage(!mCoreLogic.checkFileIsPrepared()
			        ? R.string.setup_alert_text
			        : R.string.dialog_gles20_unavailable);
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
		
		// ensure taken picture saved if any
		if (mPictureTask != null) mPictureTask.cancel(false);
		if (mImageSavedLatch != null && mImageSavedLatch.getCount() > 0) {
		    Toast.makeText(this, R.string.toast_picture_busy, Toast.LENGTH_LONG).show();
		    try {
                mImageSavedLatch.await(4, TimeUnit.SECONDS);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);

		menu.add(0, Menu.FIRST,     Menu.NONE, R.string.menu_load_model);
		menu.add(0, Menu.FIRST + 1, Menu.NONE, R.string.menu_load_camera);
		menu.add(0, Menu.FIRST + 2, Menu.NONE, R.string.menu_load_music);
		menu.add(1, Menu.FIRST + 3, Menu.NONE, R.string.menu_take_picture);
		menu.add(0, Menu.FIRST + 4, Menu.NONE, R.string.menu_initialize);
        menu.add(0, Menu.FIRST + 5, Menu.NONE, R.string.menu_play_pause);
		menu.add(0, Menu.FIRST + 6, Menu.NONE, R.string.menu_toggle_physics);
        menu.add(0, Menu.FIRST + 7, Menu.NONE, R.string.menu_toggle_repeat);
        menu.add(0, Menu.FIRST + 8, Menu.NONE, R.string.menu_settings);

		return ret;
	}
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
	    menu.setGroupEnabled(1, !mTakingPicture);
        return super.onPrepareOptionsMenu(menu);
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
							ae.setMessage(getResources().getString(R.string.dialog_load_model_motion));
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
							ae.setMessage(getResources().getString(R.string.dialog_load_model_motion));
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
		    takePicture();
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
			is[0] = getResources().getString(R.string.dialog_load_as_bg);
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
		if (mCoreLogic != null) mCoreLogic.storeState();
		// mCoreLogic == null only when background image is transparent
		// and the activity finishes immediately
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

    private volatile boolean mTakingPicture; // true until the picture views are dismissed
    private volatile CountDownLatch mImageSavedLatch;
    private volatile AsyncTask<?, ?, ?> mPictureTask;
	
	private void takePicture() {
        final Toast toast = Toast.makeText(MikuMikuDroid.this,
                R.string.toast_picture_busy, Toast.LENGTH_LONG);
	    if (mTakingPicture) {
	        toast.setDuration(Toast.LENGTH_SHORT);
	        toast.show();
	        return;
	    }
        mTakingPicture = true;
        mImageSavedLatch = new CountDownLatch(1);
        
        mPictureTask = new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                Log.d(TAG, "Taking picture: start");
                final Bitmap[] cameraBitmap = new Bitmap[1];
                final CountDownLatch cameraLatch = new CountDownLatch(1);
                if(mCameraPreviewView != null) {
                    mCameraPreviewView.mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
                        
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            cameraBitmap[0] = BitmapFactory.decodeByteArray(
                                    data, 0, data.length);
                            cameraLatch.countDown();
                            Log.d(TAG, "Taking picture: photo loaded");
                        }
                    });
                }
                Bitmap b = mMMGLSurfaceView.getCurrentFrameBitmap();
                Log.d(TAG, "Taking picture: frame loaded");
                if(mCameraPreviewView != null) {
                    try {
                        cameraLatch.await();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Bitmap temp = b;
                    int w = mCoreLogic.getScreenWidth();
                    int h = mCoreLogic.getScreenHeight();
                    b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(b);
                    canvas.save();
                    
                    int degrees = CameraPreviewView
                            .getCameraDisplayOrientation(MikuMikuDroid.this);
                    float scale = (float) w
                            / (degrees % 180 != 0
                                    ? cameraBitmap[0].getHeight()
                                    : cameraBitmap[0].getWidth());
                    Log.d(TAG, "Camera rotation degrees: " + degrees + ", scale:"
                            + scale);
                    canvas.rotate(degrees, w / 2, h / 2);
                    int offset = (h - w) / 2 * ((degrees - 180) % 180) / 90;
                    canvas.translate(offset, -offset);
                    // this traslate works fine so far, but I don't understand why.
                    canvas.scale(scale, scale);
                    
                    canvas.drawBitmap(cameraBitmap[0], 0, 0, null);
                    cameraBitmap[0].recycle();
                    
                    canvas.restore();
                    canvas.drawBitmap(temp, 0, 0, null);
                    temp.recycle();
                    Log.d(TAG, "Taking picture: merged photo and MMD frame");
                }
                showAndSavePicture(b, toast);
                
                return null;
            }
        }.execute();
    }
	
	private void showAndSavePicture(final Bitmap b, final Toast toast) {
        String prefix = "MMDpict";
        // prefix will be replaced with the first model file name if any
        if (!mCoreLogic.getMiku().isEmpty()) {
            prefix = new File(mCoreLogic.getMiku().get(0).mModel.mFileName)
                    .getName();
            prefix = prefix.replaceAll("^\\.|\\..+$", "").replaceAll(
                    "[:;/\\\\\\|,*?\"<>]", "");
        }
        final String path = mCoreLogic.getBase() + "MMDroidPicture/";
        //TODO: make folder selectable in settings
        new File(path).mkdir();
        final File fileToSave = new File(path + prefix
                + String.format("-%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS.png",
                        new Date()));
        
        final CountDownLatch imageRecycleLatch = new CountDownLatch(1);
        final boolean[] discarded = new boolean[1];
        runOnUiThread(new Runnable() {
            public void run() {
                if (MikuMikuDroid.this.isFinishing() || mPictureTask.isCancelled()) {
                    imageRecycleLatch.countDown();
                    mTakingPicture = false;
                    return;
                }
                ImageView iv = new ImageView(MikuMikuDroid.this);
                iv.setImageBitmap(b);
                LayoutParams p = new LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT);
                p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                final Button delButton = new Button(MikuMikuDroid.this);
                delButton.setText(R.string.button_discard_picture);
                delButton.setLayoutParams(p);
                
                p = new LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT);
                p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                p.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                final Button galleryButton = new Button(MikuMikuDroid.this);
                galleryButton.setText(R.string.button_open_gallery);
                galleryButton.setLayoutParams(p);

                View.OnClickListener l = new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (v.equals(delButton)) {
                            new AsyncTask<Void, Void, Void>() {

                                @Override
                                protected Void doInBackground(Void... params) {
                                    try {
                                        mImageSavedLatch.await();
                                    }
                                    catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    fileToSave.delete();
                                    return null;
                                }
                                
                            }.execute();
                            discarded[0] = true;
                            toast.setText(R.string.toast_picture_discarded);
                            toast.show();
                        }
                        mPictureLayout.removeAllViews();
                        mPictureLayout.setVisibility(View.INVISIBLE);
                        if (mCameraPreviewView != null) {
                            mCameraPreviewView.mCamera.startPreview();
                        }
                        imageRecycleLatch.countDown();
                        mTakingPicture = false;
                    }
                };
                iv.setOnClickListener(l);
                delButton.setOnClickListener(l);
                galleryButton.setOnClickListener(new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        try {
                            // !!! WAITING IN UI THREAD !!!
                            mImageSavedLatch.await();
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(Uri.fromFile(fileToSave), "image/png");
                        startActivity(i);
                    }
                });
                mPictureLayout.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.picture_bg_repeat));
                mPictureLayout.addView(iv);
                mPictureLayout.addView(delButton);
                mPictureLayout.addView(galleryButton);
                mPictureLayout.setVisibility(View.VISIBLE);
            }
        });
        OutputStream os = null;
        final boolean[] succeeded = new boolean[1];
        try {
            if (fileToSave.exists()) throw new IOException();
            os = new FileOutputStream(fileToSave);
            b.compress(Bitmap.CompressFormat.PNG, 100, os);
            succeeded[0] = true;
        }
        catch (IOException e) {
            e.printStackTrace();
            succeeded[0] = false;
        }
        finally {
            if (os != null) try { os.close(); } catch (IOException e1) {}
            mImageSavedLatch.countDown();
        }
        runOnUiThread(new Runnable() {
            public void run() {
                if (discarded[0]) return;
                
                if (succeeded[0]) toast.setText(String.format(getResources().getString(
                        R.string.toast_picture_taken), path));
                else toast.setText(R.string.toast_picture_failed);
                toast.show();
            }
        });
        try {
            imageRecycleLatch.await();
            b.recycle();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
	}
}