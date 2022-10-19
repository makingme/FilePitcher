package kr.uracle.ums.fpc.core;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.enums.PATH_KIND;
import kr.uracle.ums.fpc.utils.AlarmManager;
import kr.uracle.ums.fpc.vo.config.AlarmConfigVo;
import kr.uracle.ums.fpc.vo.config.ModuleConfigaVo;
import kr.uracle.ums.fpc.vo.module.TaskVo;


/**
 * @author : URACLE KKB
 * @see : 모듈 FLOW
 * @see : 1. initailize() 호출
 * @see : 2. process(TaskVo taskVo) 호출
 * @see : 3. return 값에 따른 후 처리
 * @see : --------------------------------------------------------------------
 * @see : 필터 모듈 제공 멤버변수
 * @see : LOGGER 		- Logger 	|	로거 인스턴스 	멤버 변수
 * @see : ERROR_LOGGER	- Logger 	|	에러 로거 인스턴스 	멤버 변수
 * @see : PRCS_NAME 	- String	|	프로세스 명 	멤버 변수
 * @see : PROCCESS_PATH - String	|	처리 중 디렉토리 경로 정보 	멤버 변수
 * @see : SUCCESS_PATH 	- String	|	성공 디렉토리 경로 정보 	멤버 변수
 * @see : ERROR_PATH 	- String	|	에러 디렉토리 경로 정보 	멤버 변수
 * @see : PARAM_MAP 	- Map		|	사용자 지정 설정 정보		멤버 변수
 * @see : --------------------------------------------------------------------
 */
public abstract class Module {
	
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
	public Module(ModuleConfigaVo MODULE_CONFIG, AlarmConfigVo ALARM_CONFIG) {
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
	 * @see :		필수 값 및 사용자 지정 멤버 변수 초기화
	 * @return :	에러메시지
	 */
	abstract public boolean initialize() throws Exception;
	
	
	/**
	 * @see	:		TaskVo - TARGET_PATH(처리 대상 Path 정조), RESULT_MESSAGE(에러 메시지 - 지정 시 히스토리 처리에 사용 됨), FREE_MAP(PROCESS SCOPE 공유 멤버)
	 * @see	:		실패 시 false 리턴하며, TaskVo.setRESULT_MESSAGE("에러메시지")로 에러(실패) 메시지를 지정한다.
	 * @param :		TaskVo
	 * @param :		PROCCESS_PATH
	 * @return :	성공 여부
	 */
	abstract public boolean process(TaskVo taskVo) throws Exception;
	
	public boolean handle(TaskVo taskVo) {
		boolean isOk = false;
		try {
			isOk = process(taskVo);
		}catch(Exception e) {
			ERROR_LOGGER.error(PROCESS_NAME +" 수행 중 에러", e);
			taskVo.setRESULT_MESSAGE(e.getMessage());
			return false;
		}

		return isOk;
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
	
	public String getPROCESS_NAME() { return PROCESS_NAME; }
	public boolean isSUCCESS_ALARM() { return SUCCESS_ALARM; }
	public boolean isFAIL_ALRAM() { return ERROR_ALARM;	}
	
	public String getPROCESS_PATH() { return PROCESS_PATH; }
	public void setPROCESS_PATH(String pROCCESS_PATH) { PROCESS_PATH = pROCCESS_PATH;	}

	public String getSUCCESS_PATH() { return SUCCESS_PATH;	}
	public void setSUCCESS_PATH(String sUCCESS_PATH) { SUCCESS_PATH = sUCCESS_PATH;	}

	public String getERROR_PATH() { return ERROR_PATH;	}
	public void setERROR_PATH(String eRROR_PATH) { ERROR_PATH = eRROR_PATH;	}
	
	
}
