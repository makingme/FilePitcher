package kr.uracle.ums.fpc.vo.config;

import java.util.ArrayList;
import java.util.List;

import kr.uracle.ums.sdk.util.UmsAlarmSender.CHANNEL;

public class AlarmConfigVo {

	private boolean ACTIVATION = false;
	private CHANNEL SEND_CHANNEL;
	private String API_PATH;
	private String PREFIX_MESSAGE = "";
	private List<String> TARGETS = new ArrayList<String>(10);
		
	
	public boolean isACTIVATION() { return ACTIVATION;	}
	public void setACTIVATION(boolean aCTIVATION) { ACTIVATION = aCTIVATION; }
	
	public CHANNEL getSEND_CHANNEL() { return SEND_CHANNEL;	}
	public void setSEND_CHANNEL(CHANNEL sEND_CHANNEL) { SEND_CHANNEL = sEND_CHANNEL; }
	
	public String getAPI_PATH() { return API_PATH; }
	public void setAPI_PATH(String uRL) { API_PATH = uRL;	}
	
	public String getPREFIX_MESSAGE() { return PREFIX_MESSAGE;	}
	public void setPREFIX_MESSAGE(String pREFIX_MESSAGE) { PREFIX_MESSAGE = pREFIX_MESSAGE;	}	
	
	public List<String> getTARGETS() { return TARGETS; }
	public void setTARGETS(List<String> tARGETS) { TARGETS = tARGETS; }
	
	
}
