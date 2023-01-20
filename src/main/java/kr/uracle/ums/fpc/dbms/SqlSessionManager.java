package kr.uracle.ums.fpc.dbms;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlSessionManager {
	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	private String FIRST_KEY = null;
	
	private final Map<String, SqlSessionFactory> sqlSessionFactoryMap = new LinkedHashMap<String, SqlSessionFactory>(5);
	
	public static SqlSessionManager getInstance() {
		return Singleton.instance;
	}
	
	private static class Singleton{
		private static final SqlSessionManager instance = new SqlSessionManager();
	}
	
	public boolean load(InputStream inputStream, String id) {
		try {
			sqlSessionFactoryMap.put(id, new SqlSessionFactoryBuilder().build(inputStream, id));	
		}catch(Exception e) {
			LOGGER.error(id+" environment 로드 중 에러 발생", e);
			return false;
		}
		if(FIRST_KEY == null) FIRST_KEY = id;
		return true;
	}

	public boolean load(String resource, String id) {
		try(InputStream inputStream = new FileInputStream(resource)) {
			sqlSessionFactoryMap.put(id, new SqlSessionFactoryBuilder().build(inputStream, id));
		}catch(Exception e) {
			LOGGER.error(id+" environment 로드 중 에러 발생", e);
			return false;
		}
		if(FIRST_KEY == null) FIRST_KEY = id;
		return true;
	}
	
	public SqlSessionFactory getSqlSessionFactory() {
		if(FIRST_KEY == null) return null;
		return sqlSessionFactoryMap.get(FIRST_KEY);
	}
	
	public SqlSessionFactory getSqlSessionFactory(String id) {
		if(id == null) return getSqlSessionFactory();
		return sqlSessionFactoryMap.get(id);
	}
	
	public Map<String, SqlSessionFactory> getSqlSessionFactoryMap() {
		return sqlSessionFactoryMap;
	}
	
	public String getFIRST_KEY() { return FIRST_KEY; }
	public void setFIRST_KEY(String fIRST_KEY) { FIRST_KEY = fIRST_KEY;	}
	
}
