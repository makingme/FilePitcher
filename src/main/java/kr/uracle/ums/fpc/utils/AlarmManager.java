package kr.uracle.ums.fpc.utils;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import kr.uracle.ums.fpc.tcpchecker.TcpAliveConManager;
import kr.uracle.ums.sdk.util.UmsAlarmSender;
import kr.uracle.ums.sdk.util.UmsAlarmSender.CHANNEL;

public class AlarmManager {
	private static final AlarmManager instance = new AlarmManager();
	
	public static AlarmManager getInstance() {
		return instance;
	}
	
	public boolean sendAlarm(String api_path, String prefixMsg, String msg, CHANNEL sendChannel) {
		try {
			String ums_host_url = TcpAliveConManager.getInstance().getConHostName();
			if(ums_host_url.endsWith(File.separator)==false)ums_host_url+=File.separator;
			if(api_path.startsWith(File.separator)) api_path = api_path.substring(1);
			
			if(StringUtils.isNotBlank(prefixMsg)) msg = prefixMsg + msg;
			UmsAlarmSender.getInstance().sendAlarm(sendChannel, ums_host_url+api_path , msg);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
}
