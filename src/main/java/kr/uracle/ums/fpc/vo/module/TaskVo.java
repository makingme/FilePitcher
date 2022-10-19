package kr.uracle.ums.fpc.vo.module;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TaskVo {

	private Path TARGET_PATH;
	private String RESULT_MESSAGE = null;
	private Map<String, Object> FREE_MAP = new HashMap<String, Object>(3);
		
	public Path getTARGET_PATH() { return TARGET_PATH;	}
	public void setTARGET_PATH(Path tARGET_PATH) { TARGET_PATH = tARGET_PATH; }

	public String getRESULT_MESSAGE() { return RESULT_MESSAGE; }
	public void setRESULT_MESSAGE(String rESULT_MESSAGE) { RESULT_MESSAGE = rESULT_MESSAGE;	}
	
	public Map<String, Object> getFREE_MAP() { return FREE_MAP;	}
	public void setFREE_MAP(Map<String, Object> fREE_MAP) { FREE_MAP = fREE_MAP; }
	
	public void FREE_MAP(String key, Object value) {
		FREE_MAP.put(key, value);
	}
	
	public Object FREE_MAP(String key) {
		return FREE_MAP.get(key);
	}

}
