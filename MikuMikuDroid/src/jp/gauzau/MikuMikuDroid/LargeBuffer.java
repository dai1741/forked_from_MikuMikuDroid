package jp.gauzau.MikuMikuDroid;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

public class LargeBuffer {
	static HashMap<ByteBuffer, RandomAccessFile> mRAF = new HashMap<ByteBuffer, RandomAccessFile>();
	static HashMap<ByteBuffer, String> mTMP = new HashMap<ByteBuffer, String>();
	
	static ByteBuffer open(String name) throws IOException {
		return open(name, "r", 0);
	}
	
	static ByteBuffer open(String name, String mode, int size) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(name, mode);
		MappedByteBuffer bb = raf.getChannel().map(mode.equals("rw") ? FileChannel.MapMode.READ_WRITE : FileChannel.MapMode.READ_ONLY, 0, size == 0 ? raf.length() : size);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		mRAF.put(bb, raf);

		return bb;
	}
	
	static ByteBuffer openTemp(String name, int size) throws IOException {
		ByteBuffer bb = open(name, "rw", size);
		mTMP.put(bb, name);

		return bb;
	}
	
	static void close(ByteBuffer bb) throws IOException {
		RandomAccessFile raf = mRAF.get(bb);
		if(raf != null) {
			raf.close();
			mRAF.remove(bb);
			String name = mTMP.get(bb);
			if(name != null) {
				new File(name).delete();
				mTMP.remove(bb);
			}
		}
	}

}
