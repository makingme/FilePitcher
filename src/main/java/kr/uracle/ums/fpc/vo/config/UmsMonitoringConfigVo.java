package kr.uracle.ums.fpc.vo.config;


public class UmsMonitoringConfigVo {

	// 프로그램 ID - 필수
	private String PROGRAM_ID;
	// 서버 ID - 필수
	private String SERVER_ID;
	// 서버 명 - 옵션
	private String SERVER_NAME;
	// 동작 주기 - 옵션
	private long CYCLE_TIME = 60*1000;
	
	private String API_PATH;

	public String getPROGRAM_ID() { return PROGRAM_ID; }
	public void setPROGRAM_ID(String pROGRAM_ID) { PROGRAM_ID = pROGRAM_ID;	}
	
	public String getSERVER_ID() { return SERVER_ID; }
	public void setSERVER_ID(String sERVER_ID) { SERVER_ID = sERVER_ID;	}
	
	public String getSERVER_NAME() { return SERVER_NAME; }
	public void setSERVER_NAME(String sERVER_NAME) { SERVER_NAME = sERVER_NAME;	}
	
	public long getCYCLE_TIME() { return CYCLE_TIME; }
	public void setCYCLE_TIME(long CYCLE_TIME) { this.CYCLE_TIME = CYCLE_TIME; }
		
	public String getAPI_PATH() { return API_PATH;	}
	public void setAPI_PATH(String aPI_PATH) { API_PATH = aPI_PATH;	}
	
}
