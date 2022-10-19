package kr.uracle.ums.fpc.dbms;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MyBatisConfigClassLoader extends ClassLoader{

	@Override
	public InputStream getResourceAsStream(String name) {
		try {
			return new FileInputStream(name);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

}
