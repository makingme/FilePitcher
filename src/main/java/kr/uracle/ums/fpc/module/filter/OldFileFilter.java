package kr.uracle.ums.fpc.module.filter;

import java.nio.file.Path;

import kr.uracle.ums.fpc.core.Filter;
import kr.uracle.ums.fpc.vo.config.AlarmConfigVo;
import kr.uracle.ums.fpc.vo.config.ModuleConfigaVo;

/**
 * @author : URACLE KKB
 * @see : 파일 경로 목록 중 EXPIRY_TIME 기준 시간 초과로 지정 경로에 머문 파일을 필터하여 삭제함
 * @see : 사용자 지정 설정(PARAM_MAP)에 EXPIRY_TIME : 1000(mils) 설정 정보 지정 시 파일 탐색 시간 시간 변동 가능(기본: 7*24*60*60*1000 = 7일)
 */
public class OldFileFilter extends Filter{

	private long EXPIRY_TIME = 7*24*60*60*1000;

	public OldFileFilter(ModuleConfigaVo MODULE_CONFIG, AlarmConfigVo ALARM_CONFIG) {
		super(MODULE_CONFIG, ALARM_CONFIG);
	}
	
	@Override
	public boolean initialize() throws Exception{
		Object eObj = PARAM_MAP.get("EXPIRY_TIME");
		if(eObj != null) {
			long eTime = Long.parseLong(eObj.toString().replaceAll("\\D", ""));
			if(eTime != 0)EXPIRY_TIME = eTime;
		}
		return true;
	}

	@Override
	public boolean process(Path path) throws Exception {
		long now = System.currentTimeMillis();
		long lastModiTime = path.toFile().lastModified();
		return (now - lastModiTime) <= EXPIRY_TIME;
	}
	

}
