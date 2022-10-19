package kr.uracle.ums.fpc.module.pre;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import org.apache.commons.lang3.ObjectUtils;

import kr.uracle.ums.fpc.core.Module;
import kr.uracle.ums.fpc.vo.config.AlarmConfigVo;
import kr.uracle.ums.fpc.vo.config.ModuleConfigaVo;
import kr.uracle.ums.fpc.vo.module.TaskVo;

/**
 * @author : URACLE KKB
 * @see : 파일 본 처리 전 처리 중 폴터로 파일을 이동 시키는 전처리기
 * @see : 사용자 지정 설정(PARAM_MAP)에 PROCCESS_PATH : 절대경로 설정 정보 지정 시 처리 파일을 지정 경로로 이동 시킴(기본값: PRCS_NAME_PROCESS)
 */
public class RcvToPrcsFileMover extends Module{
	
	private DateTimeFormatter DATE_TIME_FORMAT = null;
		
	public RcvToPrcsFileMover(ModuleConfigaVo MODULE_CONFIG, AlarmConfigVo ALARM_CONFIG) {
		super(MODULE_CONFIG, ALARM_CONFIG);
	}
	
	@Override
	public boolean initialize() throws Exception{
		//NAMING PATTERN
		Object nObj = PARAM_MAP.get("DATE_PATTEN");
		if(ObjectUtils.isNotEmpty(nObj)) {
			try {
				DATE_TIME_FORMAT = DateTimeFormatter.ofPattern(nObj.toString());				
			}catch(IllegalArgumentException e) {
				LOGGER.error("{} - 잘못된 데이터 포맷 형식", nObj);
				e.printStackTrace();
				throw new Exception("DATE_PATTEN - 잘못된 데이터 포맷 형식 설정");
			}
		}
		return true;
	}

	@Override
	public boolean process(TaskVo taskVo) {
		
		Path path = taskVo.getTARGET_PATH();
		
		if(PROCESS_PATH.endsWith(File.separator) == false) PROCESS_PATH += File.separator;
		String fileName = path.getFileName().toString();
		if(DATE_TIME_FORMAT != null) fileName = getNewFileName(fileName);
		
		Path targetPath = Paths.get(PROCESS_PATH +fileName);
		
		try {
			Path derectory = Paths.get(PROCESS_PATH);
			if(Files.exists(derectory) == false) {
				Files.createDirectories(derectory);
				LOGGER.warn("{} 디렉토리 생성", PROCESS_PATH);
			}
			
			taskVo.setTARGET_PATH(targetPath);
			Files.move(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
			
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error(path +" 파일 전처리-프로세스 디렉토리로- 이동 중에 에러 발생",  e);
			taskVo.setRESULT_MESSAGE(path+" - 파일 전처리-프로세스 디렉토리로- 이동 중에 에러 발생:"+e.getMessage());
			return false;
		}
		
	}

	
	private String getNewFileName(String FILE_NAME) {
		return FILE_NAME.substring(0, FILE_NAME.lastIndexOf("."))+"_"+DATE_TIME_FORMAT.format(LocalDateTime.now())+FILE_NAME.substring(FILE_NAME.lastIndexOf("."));
	}
}
