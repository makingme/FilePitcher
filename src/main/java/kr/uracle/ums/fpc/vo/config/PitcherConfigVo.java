package kr.uracle.ums.fpc.vo.config;

import kr.uracle.ums.fpc.enums.HISTORY_KIND;

public class PitcherConfigVo {
			
	private String DETECT_PATH;
	private String PROCESS_PATH;
	private String SUCCESS_PATH;
	private String ERROR_PATH;
	private int MAX_THREAD = 10;
	private Long CYCLE;
	private HISTORY_KIND HISTORY_TYPE = HISTORY_KIND.DB;
	private String HISTORY_INFO;

	private String SAVE_DIRECTORY;

	private ModuleConfigaVo DETECTION;
	private ModuleConfigaVo FILTER;
	private ModuleConfigaVo PREMODULE;
	private ModuleConfigaVo MAINMODULE;
	private ModuleConfigaVo POSTMODULE;

	public String getDETECT_PATH() { return DETECT_PATH; }
	public void setDETECT_PATH(String PATH) { this.DETECT_PATH = PATH; }
	
	public String getPROCESS_PATH() { return PROCESS_PATH; }
	public void setPROCESS_PATH(String pROCESS_PATH) { PROCESS_PATH = pROCESS_PATH;	}
	
	public String getSUCCESS_PATH() { return SUCCESS_PATH;	}
	public void setSUCCESS_PATH(String sUCCESS_PATH) { SUCCESS_PATH = sUCCESS_PATH;	}
	
	public String getERROR_PATH() { return ERROR_PATH;	}
	public void setERROR_PATH(String eRROR_PATH) {	ERROR_PATH = eRROR_PATH; }
	
	public int getMAX_THREAD() { return MAX_THREAD;	}
	public void setMAX_THREAD(int mAX_THREAD) { MAX_THREAD = mAX_THREAD; }	
	
	public Long getCYCLE() { return CYCLE; }
	public void setCYCLE(Long cYCLE) { CYCLE = cYCLE; }	
	
	public HISTORY_KIND getHISTORY_TYPE() { return HISTORY_TYPE;	}
	public void setHISTORY_TYPE(HISTORY_KIND lOG_TYPE) { HISTORY_TYPE = lOG_TYPE;	}
	
	public String getHISTORY_INFO() { return HISTORY_INFO;	}
	public void setHISTORY_INFO(String lOG_INFO) { HISTORY_INFO = lOG_INFO;	}

	public String getSAVE_DIRECTORY() { return SAVE_DIRECTORY;	}
	public void setSAVE_DIRECTORY(String SAVE_DIRECTORY) { this.SAVE_DIRECTORY = SAVE_DIRECTORY; }

	public ModuleConfigaVo getDETECTION() { return DETECTION; }
	public void setDETECTION(ModuleConfigaVo dETECTION) { DETECTION = dETECTION; }
	
	public ModuleConfigaVo getFILTER() { return FILTER; }
	public void setFILTER(ModuleConfigaVo fILTER) { FILTER = fILTER; }
	
	public ModuleConfigaVo getPREMODULE() { return PREMODULE;	}
	public void setPREMODULE(ModuleConfigaVo PREMODULE) { this.PREMODULE = PREMODULE; }
	
	public ModuleConfigaVo getMAINMODULE() { return MAINMODULE; }
	public void setMAINMODULE(ModuleConfigaVo MAINMODULE) { this.MAINMODULE = MAINMODULE; }
	
	public ModuleConfigaVo getPOSTMODULE() { return POSTMODULE; }
	public void setPOSTMODULE(ModuleConfigaVo POSTMODULE) { this.POSTMODULE = POSTMODULE; }
	
}

