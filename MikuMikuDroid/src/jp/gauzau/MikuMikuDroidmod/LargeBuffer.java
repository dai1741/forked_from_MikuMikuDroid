package jp.gauzau.MikuMikuDroidmod;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

import android.util.Log;

public class LargeBuffer {
	static HashMap<String, RandomAccessFile> mRAF = new HashMap<String, RandomAccessFile>();
	static HashMap<String, String> mTMP = new HashMap<String, String>();
	
	static ByteBuffer open(String name) throws IOException {
		return open(name, "r", 0);
	}
	
	static ByteBuffer open(String name, String mode, int size) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(name, mode);
		MappedByteBuffer bb = raf.getChannel().map(mode.equals("rw") ? FileChannel.MapMode.READ_WRITE : FileChannel.MapMode.READ_ONLY, 0, size == 0 ? raf.length() : size);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		mRAF.put(bb.toString(), raf);

		return bb;
	}
	
	static ByteBuffer openTemp(String name, int size) throws IOException {
		ByteBuffer bb = open(name, "rw", size);
		mTMP.put(bb.toString(), name);

		return bb;
	}
	
	static void close(ByteBuffer bb) throws IOException {
		bb.clear();
		RandomAccessFile raf = mRAF.get(bb.toString());
		if(raf != null) {
			raf.close();
			mRAF.remove(bb.toString());
			String name = mTMP.get(bb.toString());
			if(name != null) {
				mTMP.remove(bb.toString());
				File file = new File(name);
				if(file.delete() == false) {
					Log.d("LargeBuffer", "failed to delete file " + name);
				} else {
					file = new File(name);
					if(file.exists()) {
						Log.d("LargeBuffer", "failed to delete file " + name);						
					}
				}
			}
		} else {
			Log.d("LargeBuffer", "Random Access File not found.");			
		}
	}	
}
