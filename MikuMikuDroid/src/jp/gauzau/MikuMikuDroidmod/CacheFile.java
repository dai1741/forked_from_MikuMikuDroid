package jp.gauzau.MikuMikuDroidmod;

import java.io.File;

public class CacheFile {
	private String mCacheBase;
	private String mExt;
	private int    mHash;
	
	public CacheFile(String base, String ext) {
		mCacheBase = base;
		mExt = ext;
		mHash = 0;
		new File(base + "/.cache/").mkdirs();
	}

	public void addFile(String s) {
		addFile(new File(s));
	}
	
	public void addFile(File f) {
		mHash += f.hashCode();
	}
	
	public String getCacheFileName() {
		return String.format("%s/.cache/%08x.%s", mCacheBase, mHash, mExt);
	}
	
	public boolean hasCache() {
		return new File(getCacheFileName()).exists();
	}

}
