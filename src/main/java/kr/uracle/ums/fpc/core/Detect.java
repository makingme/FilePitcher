package kr.uracle.ums.fpc.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.enums.PATH_KIND;
import kr.uracle.ums.fpc.utils.AlarmManager;
import kr.uracle.ums.fpc.vo.config.AlarmConfigVo;
import kr.uracle.ums.fpc.vo.config.ModuleConfigaVo;


/**
 * @author : URACLE KKB
 * @see : Detect 모듈 FLOW
 * @see : 1. initailize() 호출
 * @see : 2. process(Path path)호출 
 * @see : 3. return 값에 따른 후 처리
 * @see : --------------------------------------------------------------------
 * @see : 필터 모듈 제공 멤버변수
 * @see : LOGGER 		- Logger 	|	로거 인스턴스 		멤버 변수
 * @see : ERROR_LOGGER	- Logger 	|	에러 로거 인스턴스 	멤버 변수
 * @see : PRCS_NAME 	- String	|	프로세스 명 	멤버 변수
 * @see : PROCCESS_PATH - String	|	처리 중 디렉토리 경로 정보 	멤버 변수
 * @see : SUCCESS_PATH 	- String	|	성공 디렉토리 경로 정보 	멤버 변수
 * @see : ERROR_PATH 	- String	|	에러 디렉토리 경로 정보 	멤버 변수
 * @see : PARAM_MAP 	- Map		|	사용자 지정 설정 정보		멤버 변수
 * @see : --------------------------------------------------------------------
 */
public abstract class Detect {
	 
	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	protected final Logger ERROR_LOGGER = LoggerFactory.getLogger("ERROR");
	
	protected final String PROCESS_NAME;
	protected String PROCESS_PATH;
	protected String SUCCESS_PATH;
	protected String ERROR_PATH;
	
	protected final Map<String, Object> PARAM_MAP;
	
	protected final AlarmConfigVo ALARM_CONFIG;
	
	protected final boolean SUCCESS_ALARM;
	protected final boolean ERROR_ALARM;
	

	/**
	 * @param MODULE_CONFIG	: 모듈 설정 정보 - 모듈명, 알람여부, 구현클래스명, 지정 변수맵
	 * @param ALARM_CONFIG  : 알람 설정 정보 - 발송채널, 발송 UMS URL PATH, 알람 고정 문구(PREFIX)
	 */
	public Detect(ModuleConfigaVo MODULE_CONFIG, AlarmConfigVo ALARM_CONFIG) {
		this.PROCESS_NAME = MODULE_CONFIG.getNAME()==null?this.getClass().getSimpleName():MODULE_CONFIG.getNAME();
		
		this.PROCESS_PATH = MODULE_CONFIG.PATH_MAP(PATH_KIND.PROCESS).toString()+File.separator;
		this.SUCCESS_PATH = MODULE_CONFIG.PATH_MAP(PATH_KIND.SUCCESS).toString()+File.separator;
		this.ERROR_PATH = MODULE_CONFIG.PATH_MAP(PATH_KIND.ERROR).toString()+File.separator;
		
		this.PARAM_MAP = MODULE_CONFIG.getPARAM_MAP();
		this.ALARM_CONFIG = ALARM_CONFIG;
		
		SUCCESS_ALARM = MODULE_CONFIG.isSUCCESS_ALRAM();
		ERROR_ALARM = MODULE_CONFIG.isERROR_ALRAM();
	}
	
	/**
	 * @see 필수 값 및 사용자 지정 멤버 변수 초기화, 별도 초기화 멤버 없으면 TRUE RETURN
	 * @return 초기화 성공 여부-초기화 멤버 없으면 TRUE RETURN
	 */
	abstract public boolean initialize() throws Exception;
	
	
	/**
	 * @see	:	탐색 디렉토리가 미 존재 시 자동 생성 함
	 * @see	:	에러 처리 지원함, throw Exception 메시지 내용으로 알람 발송 지원
	 * @see	:		SUCCESS_ALRAM, ERROR_ALRAM 설정 값에 따라 파일 탐지/에러 발생 시 알람 발송 지원
	 * @param	:	path : 탐색 경로(절대 경로)
	 * @return	:	탐색된 파일 PATH 목록, NULL RETURN 시 에러로 간주, 탐색 파일 없을 경우 SIZE 0인 INSTANCE RETURN
	 */
	abstract public List<Path> process(Path path)throws Exception; 
	
	public List<Path> detect(Path path){
		List<Path> pathList;
		// 지정 경로 디렉토리 없으면 생성
		if(Files.exists(path) == false) {
			try {
				Files.createDirectories(path);
			} catch (IOException e) {
				ERROR_LOGGER.error(path+" 생성 중 에러 발생", e);
				if(ERROR_ALARM) {
					String msg = PROCESS_NAME +", "+path+" 디렉토리 생성 중 에러:"+e.getMessage();
					sendAlarm(msg);
				}
				return null;
			}
		}
		// 에러 캣치 처리
		try {
			pathList = process(path);			
			if(pathList == null) {
				String msg = PROCESS_NAME +", "+path+" 감시 중 에러: Unkown Error";
				ERROR_LOGGER.error(msg);
				if(ERROR_ALARM) sendAlarm(msg);
				return null; 
			}

		}catch(Exception e) {
			ERROR_LOGGER.error(path+" 감시 중 에러", e);
			if(ERROR_ALARM) {
				String msg = PROCESS_NAME +", "+path+" 감시 중 에러:"+e.getMessage();
				sendAlarm(msg);
			}
			return null;
		}
					
		if(pathList.size()>0 && SUCCESS_ALARM) {
			String msg = PROCESS_NAME +", "+path+" 경로, 파일 유입("+pathList.size()+")";
			sendAlarm(msg);
		}
		
		return pathList;
	}
	
	public void sendAlarm(String msg) {
		if(ALARM_CONFIG.isACTIVATION()){
			boolean isOk = AlarmManager.getInstance().sendAlarm(ALARM_CONFIG.getAPI_PATH(), ALARM_CONFIG.getPREFIX_MESSAGE(), msg, ALARM_CONFIG.getSEND_CHANNEL());
			if(isOk == false) {
				ERROR_LOGGER.error("알람 발송 실패 :{}", msg);
			}
		}else{
			LOGGER.info("ARAM ACIVVATION 값이 FALSE로 설정되어 알람 미 발송:{}", msg);
		}
	}
	
	String getPROCESS_NAME() { return PROCESS_NAME; }
}

