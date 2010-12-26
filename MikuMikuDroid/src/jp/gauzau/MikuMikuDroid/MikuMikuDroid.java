package jp.gauzau.MikuMikuDroid;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MikuMikuDroid extends Activity implements OnClickListener {
    private MMGLSurfaceView	mMMGLSurfaceView;
    
	private String[]		mFiles;
	private int				mOpenType;
	private MediaPlayer     mMedia;
	private String mModelName;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mMMGLSurfaceView	= new MMGLSurfaceView(this);
        setContentView(mMMGLSurfaceView);
        
        mMMGLSurfaceView.setScreenAngle(0);
        
		File files = new File("/sdcard/MikuMikuDroid/Data/toon0.bmp");
		if(files.exists() == false) {
	    	Builder ad;
			ad = new AlertDialog.Builder(this);
			ad.setTitle("Setup alert");
			ad.setMessage("Please put toon??.bmp files in MikuMikuDance Ver5.x on /sdcard/MikuMikuDroid/Data/ .");
			ad.setPositiveButton("OK", null);
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
        if(mMedia != null) {
        	mMedia.pause();
        }
        mMMGLSurfaceView.onPause();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	boolean ret = super.onCreateOptionsMenu(menu);

    	menu.add(0 , Menu.FIRST ,    Menu.NONE , "Load Model");
    	menu.add(0 , Menu.FIRST + 1 ,Menu.NONE , "Load Camera");
    	menu.add(0 , Menu.FIRST + 2 ,Menu.NONE , "Load Music");
    	menu.add(0 , Menu.FIRST + 3 ,Menu.NONE , "Play/Pause");
    	menu.add(0 , Menu.FIRST + 4 ,Menu.NONE , "Rewind");
    	menu.add(0 , Menu.FIRST + 5 ,Menu.NONE , "Initialize");

    	return ret;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Builder ad;
    	File files;
    	switch(item.getItemId()) {
    	
    	case (Menu.FIRST + 0):
    		ad = new AlertDialog.Builder(this);
			files = new File("/sdcard/MikuMikuDroid/UserFile/Model/");
			mOpenType = 0;
			mFiles = files.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".pmd");
				}
			});
			if(mFiles == null) {
	    		ad.setTitle("File not found");
	    		ad.setMessage("Please put PMD files on /sdcard/MikuMikuDroid/UserFile/Model/ .");
	    		ad.setPositiveButton("OK", null);
			} else {
	    		ad.setTitle("Open Model File");
				Arrays.sort(mFiles);
				for(int i = 0; i < mFiles.length; i++) {
					mFiles[i] = mFiles[i].replaceFirst(".pmd", "");
				}
				ad.setItems(mFiles, this);
			}
			ad.show();
    	break;
    	    	
    	case (Menu.FIRST + 1):
    		ad = new AlertDialog.Builder(this);
			files = new File("/sdcard/MikuMikuDroid/UserFile/Motion/");
			mOpenType = 1;
			mFiles = files.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".vmd");
				}
			});
			if(mFiles == null) {
	    		ad.setTitle("File not found");
	    		ad.setMessage("Please put VMD files on /sdcard/MikuMikuDroid/UserFile/Motion/ .");
	    		ad.setPositiveButton("OK", null);
			} else {
				ad.setTitle("Open Camera Motion File");
				Arrays.sort(mFiles);
				for(int i = 0; i < mFiles.length; i++) {
					mFiles[i] = mFiles[i].replaceFirst(".vmd", "");
				}
				ad.setItems(mFiles, this);
			}
			ad.show();
    	break;
    	
    	case (Menu.FIRST + 2):
    		ad = new AlertDialog.Builder(this);
			files = new File("/sdcard/MikuMikuDroid/UserFile/Wave/");
			mOpenType = 2;
			mFiles = files.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".mp3");
				}
			});
			if(mFiles == null) {
	    		ad.setTitle("File not found");
	    		ad.setMessage("Please put sound files on /sdcard/MikuMikuDroid/UserFile/Wave/ .");
	    		ad.setPositiveButton("OK", null);
			} else {
				ad.setTitle("Open BGM");
				Arrays.sort(mFiles);
				ad.setItems(mFiles, this);
			}
			ad.show();
    	break;
    	case (Menu.FIRST + 3):
    		if(mMedia != null) {
    			if(mMedia.isPlaying()) {
    				mMedia.pause();
    			} else {
    				mMedia.start();
    			}
    		}
    		break;
    	case (Menu.FIRST + 4):
    		if(mMedia != null) {
    			mMedia.seekTo(0);
    		}
    	break;
    		
    	case (Menu.FIRST + 5):
    		if(mMedia != null) {
    			mMedia.stop();
    		}
    		mMMGLSurfaceView.clear();
    	break;
    	
    	default: ;
    	}

    	return super.onOptionsItemSelected(item);
    }

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		if(mOpenType == 0) {
			mModelName = mFiles[arg1];
    		Builder ad = new AlertDialog.Builder(this);
			File files = new File("/sdcard/MikuMikuDroid/UserFile/Motion/");
			mOpenType = 3;
			String [] file = files.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".vmd");
				}
			});
			ad.setTitle("Open Motion File");
			if(file == null) {
				mFiles = new String[1];
			} else {
				Arrays.sort(file);
				mFiles = new String[file.length+1];
				for(int i = file.length; i > 0; i--) {
					mFiles[i] = file[i-1].replaceFirst(".vmd", "");
				}
			}
			mFiles[0] = "Load as Background";
			ad.setItems(mFiles, this);
			ad.show();
		} else if(mOpenType == 1) {
			mMMGLSurfaceView.loadCamera("/sdcard/MikuMikuDroid/UserFile/Motion/" + mFiles[arg1] + ".vmd");
		} else if(mOpenType == 2) {
			Uri uri = Uri.parse("file:///sdcard/MikuMikuDroid/UserFile/Wave/" + mFiles[arg1]);
			mMedia = MediaPlayer.create(this, uri);
			mMMGLSurfaceView.setMedia(mMedia);
		} else if(mOpenType == 3) {
			if(arg1 == 0) {
				mMMGLSurfaceView.loadStage("/sdcard/MikuMikuDroid/UserFile/Model/" + mModelName + ".pmd");
			} else {
				mMMGLSurfaceView.loadModel("/sdcard/MikuMikuDroid/UserFile/Model/" + mModelName + ".pmd");
				mMMGLSurfaceView.loadMotion("/sdcard/MikuMikuDroid/UserFile/Motion/" + mFiles[arg1] + ".vmd");
			}
		}
	}
	
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    }
}