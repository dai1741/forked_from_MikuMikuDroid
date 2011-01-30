package jp.gauzau.MikuMikuDroid;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface SerializableExt {
	public <T> T create();
	public void read(ObjectInputStream is) throws IOException, ClassNotFoundException;
	public void write(ObjectOutputStream os) throws IOException;

}
