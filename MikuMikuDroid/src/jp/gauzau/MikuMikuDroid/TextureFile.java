package jp.gauzau.MikuMikuDroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

public class TextureFile {
	
	static public void createCache(String base, String file, int scale) {
		if (file.endsWith(".tga")) {
			// delete previous cache
			String df = file.replaceFirst(".tga", "_mmcache.png");
			File d = new File(df);
			if (d.exists()) {
				d.delete();
			}
			
			createTgaCache(base, file, scale);
		} else {
			createBitmapCache(base, file, scale);
		}
	}
	
	static public void loadTexture(String base, String file, int scale) {
		if (file.endsWith(".tga")) {
			loadTgaTextureCached(base, file, scale);
		} else {
			loadBitmapTextureCached(base, file, scale);
		}
	}
	
	static private void loadTgaTextureCached(String base, String file, int scale) {
		CacheFile cp = new CacheFile(base, "png");
		cp.addFile(file);
		
		if(cp.hasCache()) {
			Bitmap bmp = loadBitmap(cp.getCacheFileName(), scale);
			if(bmp != null) {
				GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
				bmp.recycle();				
			}
		}
	}
	
	static private void loadBitmapTextureCached(String base, String file, int scale) {
		Bitmap bmp = loadBitmap(file, scale);
		if(bmp != null) {
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
			bmp.recycle();			
		}
	}

	
	static private void createTgaCache(String base, String file, int scale) {
		CacheFile cp = new CacheFile(base, "png");
		cp.addFile(file);

		if (!cp.hasCache()) {
			try {
				RandomAccessFile raf = new RandomAccessFile(file, "r");
				MappedByteBuffer m = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length());
				m.position(0);
				m.order(ByteOrder.LITTLE_ENDIAN);

				// read header
				m.position(2);
				int type = m.get();
				if (type != 2 && type != 10) {
					Log.d("TextureFile", String.format("Unsupported TGA TYPE: %d", type));
					Log.d("TextureFile", String.format("fail to create png from tga: %s", file));
					raf.close();
					return ;
				}
				m.position(12);
				short w = (short) m.getShort();
				short h = (short) m.getShort();
				byte depth = m.get();
				byte mode = m.get();

				// create bitmap
				if (depth == 24 || depth == 32) {
					Log.d("TextureFile", String.format("Creating texture cache for TGA: %s", file));
					Bitmap bmp = createBitmapfromTga(m, w, h, depth, type, mode);
					if(bmp != null) {
						OutputStream os = new FileOutputStream(cp.getCacheFileName());
						bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
						os.close();
					}
				} else {
					Log.d("TextureFile", String.format("Unsupported TGA depth: %d", depth));
					Log.d("TextureFile", String.format("fail to create png from tga: %s", file));					
				}
				
				raf.close();
			} catch (Exception e) {
				e.printStackTrace();
				Log.d("TextureFile", String.format("fail to create png from tga: %s", file));
			}
		}
	}
	
	private static Bitmap createBitmapfromTga(MappedByteBuffer m, short w, short h, byte depth, int type, byte mode) {
		Bitmap bmp = null;
		if (depth == 24) {
			bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		} else {
			bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		}
		
		if(bmp != null) {
			if (type == 2) { // no compression
				createBitmapFromNonCompressedTga(bmp, m, w, h, depth, mode);
			} else { // RLE compression
				createBitmapFromRLETga(bmp, m, w, h, depth, mode);
			}
		}
		
		return bmp;
	}

	private static void createBitmapFromNonCompressedTga(Bitmap bmp, MappedByteBuffer m, short w, short h, byte depth, byte mode) {
		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				int color = getPixel(m, depth);
				int x, y;
				if ((mode & 0x10) != 0) {
					x = w - j - 1;
				} else {
					x = j;
				}
				if ((mode & 0x20) != 0) {
					y = i;
				} else {
					y = h - i - 1;
				}
				bmp.setPixel(x, y, color);
			}
		}
	}

	private static void createBitmapFromRLETga(Bitmap bmp, MappedByteBuffer m, short w, short h, byte depth, byte mode) {
		int pos = 0;
		while (pos < w * h) {
			int header = 0x000000ff & m.get();
			if (header < 128) { // normal pixel
				for (int i = 0; i < header + 1; i++) {
					int color = getPixel(m, depth);
					setPixel(bmp, pos++, w, h, mode, color, 1);
				}
			} else { // RLE pixel
				header -= 127;
				int color = getPixel(m, depth);
				for (int i = 0; i < header; i++) {
					setPixel(bmp, pos++, w, h, mode, color, 1);
				}
			}
		}
	}

	static private void createBitmapCache(String base, String file, int scale) {
	}
	
	private static void setPixel(Bitmap bmp, int pos, short w, short h, int mode, int color, int scale) {
		int x, y;
		x = pos % (w * scale);
		y = pos / (w * scale);
		if ((mode & 0x10) != 0) {
			x = (w * scale) - x - 1;
		}
		if ((mode & 0x20) == 0) {
			y = (h * scale) - y - 1;
		}

		if ((x % scale) == 0 && (y % scale) == 0) {
			bmp.setPixel(x / scale, y / scale, color);
		}
	}

	private static int getPixel(MappedByteBuffer m, byte depth) {
		byte blue = m.get();
		byte green = m.get();
		byte red = m.get();
		byte alpha;
		if (depth == 32) {
			alpha = m.get();
		} else {
			alpha = -1;
		}
		int color = alpha;
		color = (color << 8) | (0x000000ff & red);
		color = (color << 8) | (0x000000ff & green);
		color = (color << 8) | (0x000000ff & blue);

		return color;
	}

	static private Bitmap loadBitmap(String file, int scale) {
		Options opt = new Options();
		// opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
		opt.inSampleSize = scale;
		return BitmapFactory.decodeFile(file, opt);		
	}
	
}
