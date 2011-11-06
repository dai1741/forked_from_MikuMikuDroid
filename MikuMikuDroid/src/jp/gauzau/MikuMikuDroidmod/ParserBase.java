package jp.gauzau.MikuMikuDroidmod;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ParserBase {
	private final byte buf[] = new byte[512]; // temp buffer
	private MappedByteBuffer mBB;
	private RandomAccessFile mRaf;

	public ParserBase(String file) throws IOException {
		mRaf = new RandomAccessFile(file, "r");
		mBB = mRaf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, mRaf.length());
		mBB.position(0);
		mBB.order(ByteOrder.LITTLE_ENDIAN);

	}

	protected final int getInt() {
		return mBB.getInt();
	}

	protected final short getShort() {
		return mBB.getShort();
	}
	
	protected final byte getByte() {
		return mBB.get();
	}

	protected final void getBytes(byte dst[], int size) {
		mBB.get(dst, 0, size);
	}

	protected final float getFloat() {
		return mBB.getFloat();
	}

	protected final void getFloat(float[] f) {
		for (int i = 0; i < f.length; i++) {
			f[i] = mBB.getFloat();
		}
	}
	
	protected final int position() {
		return mBB.position();
	}

	protected final void position(int pos) {
		mBB.position(pos);
	}

	protected final String getString(int i) {
		mBB.get(buf, 0, i);
		for (int n = 0; n < i; n++) {
			if (buf[n] == '\0') {
				try {
					return new String(buf, 0, n, "SJIS");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "";
				}
			}
		}
		try {
			return new String(buf, 0, i, "SJIS");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}

	protected final static String toString(byte[] bb) {
		for (int n = 0; n < bb.length; n++) {
			if (bb[n] == '\0') {
				try {
					return new String(bb, 0, n, "SJIS");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "";
				}
			}
		}
		try {
			return new String(bb, 0, bb.length, "SJIS");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	
	protected final byte[] getStringBytes(byte[] tmp, int i) {
		mBB.get(tmp, 0, i);
		for(int j = 0; j < i; j++) {
			if(tmp[j] == '\0') { // null
				for(int k = j; k < i; k++) {
					tmp[k] = '\0';
				}
				return tmp;
			}
		}
		return tmp;
	}
	
	protected final boolean isEof() {
		return mBB.remaining() == 0;
	}

	protected boolean isExist(String file) {
		File f = new File(file);
		return f.exists();
	}
	
	protected void close() {
		try {
			mRaf.close();
			mBB = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
