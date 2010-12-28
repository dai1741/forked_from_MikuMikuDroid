package jp.gauzau.MikuMikuDroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class TgaBitmapFactory {
	
	public static Bitmap decodeFileCached(String file, int scale) throws IOException {
		String png = file.replaceFirst(".tga", "_mmcache.png");
		File f = new File(png);
		if(!f.exists()) {
			createCache(file, png);
		}
		
    	BitmapFactory.Options opt = new BitmapFactory.Options();
    	opt.inSampleSize = scale;
    	return BitmapFactory.decodeFile(png, opt);
	}
	
	private static void createCache(String file, String png) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		MappedByteBuffer m = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length());
		m.position(0);
		m.order(ByteOrder.LITTLE_ENDIAN);
		
		// read header
		m.position(2);
		int type = m.get();
		if(type != 2 && type != 10) {
			Log.d("TgaBitmapFactory", String.format("Unsupported TYPE: %d", type));
		}
		m.position(12);
		short w = (short) m.getShort();
		short h = (short) m.getShort();
		byte  depth = m.get();
		byte  mode  = m.get();
//		Log.d("TgaBitmapFactory", String.format("%s = %d x %d in depth %d", file, w, h, depth));
		
		Bitmap bmp = null;
		if(depth == 24 || depth == 32) {
			if(depth == 24) {
				bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
//				bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);				
			} else {
				bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);				
			}
			
			if(type == 2) { // no compression
				for(int i = 0; i < h; i ++) {
					for(int j = 0; j < w; j ++) {
						int color = getPixel(m, depth);
						int x, y;
						if((mode & 0x10) != 0) {
							x = w - j - 1;
						} else {
							x = j;
						}
						if((mode & 0x20) != 0) {
							y = i;
						} else {
							y = h - i - 1;
						}
						bmp.setPixel(x, y, color);
					}
				}				
			} else {	// RLE compression
				int pos = 0;
				while(pos < w * h) {
					int header = 0x000000ff & m.get();
					if(header < 128) {		// normal pixel
						for(int i = 0; i < header + 1; i++) {
							int color = getPixel(m, depth);
							setPixel(bmp, pos++, w, h, mode, color, 1);
						}
					} else {				// RLE pixel
						header -= 127;
						int color = getPixel(m, depth);
						for(int i = 0; i < header; i++) {
							setPixel(bmp, pos++, w, h, mode, color, 1);
						}
					}
				}
			}
			
			OutputStream os = new FileOutputStream(png);
			bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
			os.close();
		}
				
		raf = null;
		m = null;
	}

	private static void setPixel(Bitmap bmp, int pos, short w, short h, int mode, int color, int scale) {
		int x, y;
		x = pos % (w * scale);
		y = pos / (w * scale);
		if((mode & 0x10) != 0) {
			x = (w * scale) - x - 1;
		}
		if((mode & 0x20) == 0) {
			y = (h * scale) - y - 1;
		}
		
		if((x % scale) == 0 && (y % scale) == 0) {
			bmp.setPixel(x / scale, y / scale, color);
		}
	}

	private static int getPixel(MappedByteBuffer m, byte depth) {
		byte blue = m.get();
		byte green = m.get();
		byte red = m.get();
		byte alpha;
		if(depth == 32) {
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

}
