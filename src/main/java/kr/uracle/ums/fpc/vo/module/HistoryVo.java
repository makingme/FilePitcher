package kr.uracle.ums.fpc.vo.module;

import kr.uracle.ums.fpc.enums.HISTORY_KIND;

public class HistoryVo {
	
	private HISTORY_KIND HISTORY_TYPE;
	private String EXECUTE_INFO;
	private String PROGRAM_ID;
	private String SERVER_ID;
	private String TARGET;
	private String STATE;
	private String DESCRIPT;

	public HISTORY_KIND getHISTORY_TYPE() {return HISTORY_TYPE;}
	public void setHISTORY_TYPE(HISTORY_KIND HISTORY_TYPE) {this.HISTORY_TYPE = HISTORY_TYPE;}

	public String getEXECUTE_INFO() {return EXECUTE_INFO;}
	public void setEXECUTE_INFO(String EXECUTE_INFO) {this.EXECUTE_INFO = EXECUTE_INFO;}

	public String getPROGRAM_ID() {return PROGRAM_ID;}
	public void setPROGRAM_ID(String PROGRAM_ID) {this.PROGRAM_ID = PROGRAM_ID;}

	public String getSERVER_ID() {return SERVER_ID;}
	public void setSERVER_ID(String SERVER_ID) {this.SERVER_ID = SERVER_ID;}

	public String getTARGET() {return TARGET;}
	public void setTARGET(String TARGET) {this.TARGET = TARGET;}

	public String getSTATE() {return STATE;}
	public void setSTATE(String STATE) {this.STATE = STATE;}

	public String getDESCRIPT() {return DESCRIPT;}
	public void setDESCRIPT(String DESCRIPT) {this.DESCRIPT = DESCRIPT;}

}
