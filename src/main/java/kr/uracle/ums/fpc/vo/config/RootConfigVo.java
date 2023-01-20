package kr.uracle.ums.fpc.vo.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RootConfigVo {		
	
	private String BASE_PATH;
	
	private String MYBATIS_PATH = null;
	
	private List<String> UMS_IPADREESS = new ArrayList<String>(5);
	
	private long PITCHER_MONIT_CYCLE = 1*60*1000;
	
	private UmsMonitoringConfigVo UMS_MONIT;
	
	private DuplexConfigVo DUPLEX;
	
	private Map<String, PitcherConfigVo> PITCHERS = new HashMap<String, PitcherConfigVo>(5);
		
	private AlarmConfigVo ALARM;
		
	public String getBASE_PATH() { return BASE_PATH; }
	public void setBASE_PATH(String bASE_PATH) { BASE_PATH = bASE_PATH;	}
	
	public String getMYBATIS_PATH() { return MYBATIS_PATH;	}
	public void setMYBATIS_PATH(String mYBATIS_PATH) { MYBATIS_PATH = mYBATIS_PATH;	}
	
	public List<String> getUMS_IPADREESS() { return UMS_IPADREESS;	}
	public void setUMS_IPADREESS(List<String> uMS_IPADREESS) { UMS_IPADREESS = uMS_IPADREESS;	}

	public long getPITCHER_MONIT_CYCLE() {return PITCHER_MONIT_CYCLE;}
	public void setPITCHER_MONIT_CYCLE(long PITCHER_MONIT_CYCLE) {this.PITCHER_MONIT_CYCLE = PITCHER_MONIT_CYCLE;}
	
	public UmsMonitoringConfigVo getUMS_MONIT() { return UMS_MONIT; }
	public void setUMS_MONIT(UmsMonitoringConfigVo uMS_MONIT) { UMS_MONIT = uMS_MONIT; }
	
	public DuplexConfigVo getDUPLEX() { return DUPLEX; }
	public void setDUPLEX(DuplexConfigVo dUPLEX) { DUPLEX = dUPLEX; }
		
	public Map<String, PitcherConfigVo> getPITCHERS() { return PITCHERS; }
	public void setPITCHERS(Map<String, PitcherConfigVo> PITCHERS) { this.PITCHERS = PITCHERS; }
	
	public AlarmConfigVo getALARM() { return ALARM; }
	public void setALARM(AlarmConfigVo aLARM) { ALARM = aLARM; }
			
}
