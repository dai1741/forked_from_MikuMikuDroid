package jp.gauzau.MikuMikuDroid;

import java.io.IOException;

import android.media.MediaPlayer;
import android.opengl.GLSurfaceView.Renderer;

public interface MikuRendererInterface extends Renderer {

	public double getFPS();

	public void setMedia(MediaPlayer media);

	public void loadModel(String file) throws IOException;

	public void loadMotion(String file) throws IOException;

	public void loadStage(String stage) throws IOException;

	public void loadCamera(String camera) throws IOException;

	public void clear();

	public void setScreenAngle(int angle);

}
