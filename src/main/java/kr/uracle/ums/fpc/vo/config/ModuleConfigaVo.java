package kr.uracle.ums.fpc.vo.config;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import kr.uracle.ums.fpc.enums.PATH_KIND;


public class ModuleConfigaVo {

	private String NAME;
	private String CLASS_NAME;

	private String SERVER_ID;
	private String DBMS_ID;
	private boolean SUCCESS_ALRAM = false;
	private boolean ERROR_ALRAM = false;
	private Map<PATH_KIND, Path> PATH_MAP = new HashMap<PATH_KIND, Path>(10);
	private Map<String, Object> PARAM_MAP = new HashMap<String, Object>(20);
	
	public String getNAME() { return NAME;	}
	public void setNAME(String nAME) { NAME = nAME;	}
	
	public String getCLASS_NAME() { return CLASS_NAME;	}
	public void setCLASS_NAME(String cLASS_NAME) {	CLASS_NAME = cLASS_NAME; }

	public String getSERVER_ID() {return SERVER_ID;}
	public void setSERVER_ID(String SERVER_ID) {this.SERVER_ID = SERVER_ID;}

	public String getDBMS_ID() {return DBMS_ID;}
	public void setDBMS_ID(String DBMS_ID) {this.DBMS_ID = DBMS_ID;}

	public boolean isSUCCESS_ALRAM() { return SUCCESS_ALRAM; }
	public void setSUCCESS_ALRAM(boolean sUCCESS_ALRAM) { SUCCESS_ALRAM = sUCCESS_ALRAM; }
	
	public boolean isERROR_ALRAM() { return ERROR_ALRAM;	}
	public void setERROR_ALRAM(boolean FAIL_ALRAM) { this.ERROR_ALRAM = FAIL_ALRAM; }
	
	public Map<PATH_KIND, Path> getPATH_MAP() { return PATH_MAP; }
	public void setPATH_MAP(Map<PATH_KIND, Path> pATH_MAP) { PATH_MAP = pATH_MAP;	}
	
	public Path PATH_MAP(PATH_KIND pathKind) {
		return PATH_MAP.get(pathKind);
	}
	
	public Map<String, Object> getPARAM_MAP() { return PARAM_MAP; }
	public void setPARAM_MAP(Map<String, Object> pARAM_MAP) { PARAM_MAP = pARAM_MAP; }
}
