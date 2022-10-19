package kr.uracle.ums.fpc.module.filter;


import java.nio.file.Files;
import java.nio.file.Path;

import kr.uracle.ums.fpc.core.Filter;
import kr.uracle.ums.fpc.vo.config.AlarmConfigVo;
import kr.uracle.ums.fpc.vo.config.ModuleConfigaVo;

/**
 * @author : URACLE KKB
 * @see : 파일 경로 목록 중 사이즈가 0 인 파일을 필터하여 PITCHER 설정의 ERROR_PATH 로 이동 시킴
 */
public class ZeroFileFilter extends Filter{
		
	public ZeroFileFilter(ModuleConfigaVo MODULE_CONFIG, AlarmConfigVo ALARM_CONFIG) {
		super(MODULE_CONFIG, ALARM_CONFIG);
	}
	
	@Override
	public boolean initialize() throws Exception{
		return true;
	}

	@Override
	public boolean process(Path path) throws Exception {
		// 파일 사이즈가 0 보다 작거나 같다면 필터링 대상 
		if(Files.size(path) <= 0) {
			// 필터링 파일 일 경우 fasle return
			return false;
		}
		return true;
	}

}
