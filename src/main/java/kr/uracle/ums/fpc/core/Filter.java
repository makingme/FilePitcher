package kr.uracle.ums.fpc.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import kr.uracle.ums.fpc.enums.PATH_KIND;
import kr.uracle.ums.fpc.utils.AlarmManager;
import kr.uracle.ums.fpc.utils.HistoryManager;
import kr.uracle.ums.fpc.vo.config.AlarmConfigVo;
import kr.uracle.ums.fpc.vo.config.ModuleConfigaVo;
import kr.uracle.ums.fpc.vo.module.HistoryVo;


/**
 * @author : URACLE KKB
 * @see : 필터 모듈 FLOW
 * @see : 1. initailize() 호출
 * @see : 2. process(Path path)호출 
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
 * @see : 필터 모듈 제공 Method
 * @see : void moveFile(Path 대상파일, String 이동경로정보, String 새이름-NULLABLE) throws IOException
 */
public abstract class Filter {
	
	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	protected final Logger ERROR_LOGGER = LoggerFactory.getLogger("ERROR");
	
	protected final Gson gson = new Gson();
	
	protected final String PROCESS_NAME;
	protected String PROCESS_PATH;
	protected String SUCCESS_PATH;
	protected String ERROR_PATH;
	
	protected final Map<String, Object> PARAM_MAP;
		
	protected final boolean SUCCESS_ALARM;
	protected final boolean ERROR_ALARM;

	private String YYYYMMDD;
	
	private final AlarmConfigVo ALARM_CONFIG;

	/**
	 * @param MODULE_CONFIG	: 모듈 설정 정보 - 모듈명, 알람여부, 구현클래스명, 지정 변수맵
	 * @param ALARM_CONFIG  : 알람 설정 정보 - 발송채널, 발송 UMS URL PATH, 알람 고정 문구(PREFIX)
	 */
	public Filter(ModuleConfigaVo MODULE_CONFIG, AlarmConfigVo ALARM_CONFIG) {
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
	 * @return 초기화 성공 여부
	 */
	abstract public boolean initialize()throws Exception;
	
	/**
	 * @param path : Detect 탐색된 파일 PATH 정보
	 * @return	정상 여부 - 필터 처리 시 false return
	 */
	abstract public boolean process(Path path) throws Exception;
	
	
	public boolean filtering(List<Path> pathList, HistoryVo historyVo, String yyyyMMdd){
		YYYYMMDD = yyyyMMdd;

		int totalSize = pathList.size();
		boolean isOk = false;
		Iterator<Path> iter = pathList.iterator();
		
		historyVo.setPROGRAM_ID(PROCESS_NAME);
		
		List<Path> filterList = new ArrayList<Path>();
		Path path  =  null;
		while(iter.hasNext()) {
			try {
				path = iter.next();
				isOk = process(path);
				if(isOk == false) {
					filterList.add(path);
					iter.remove();
				}
			}catch(Exception e) {
				ERROR_LOGGER.error(path+" 필터 중 에러 발생", e);
				if(ERROR_ALARM) {
					sendAlarm(path  +" 필터 처리 중 에러 발생:"+e.getMessage());
				}
				return false;
			}
		}

		// 필터 처리 여부 확인
		int diffCnt = totalSize - pathList.size();
		if(diffCnt>0 && SUCCESS_ALARM)sendAlarm((diffCnt)  +"개 파일 필터 처리 됨");
		
		for(Path p : filterList) {
			fileMove(p, ERROR_PATH);
			historyVo.setTARGET(path.toString());
			historyVo.setSTATE("FILTERING");
			historyVo.setDESCRIPT("필터 처리 됨");
			makeHistory(historyVo);
			LOGGER.info("{} 파일 필터 처리 됨", p.toString());
		}

		return true;
	}
	
	private void makeHistory(HistoryVo historyVo) {
		String logMsg = HistoryManager.getInstance().recordHistory(historyVo);
		if(StringUtils.isNotBlank(logMsg)) LOGGER.info("히스토리에러:{}, LOG={}",logMsg, gson.toJson(historyVo));
	}
	
	private void sendAlarm(String msg) {
		if(ALARM_CONFIG.isACTIVATION()){
			boolean isOk = AlarmManager.getInstance().sendAlarm(ALARM_CONFIG.getAPI_PATH(), ALARM_CONFIG.getPREFIX_MESSAGE(), msg, ALARM_CONFIG.getSEND_CHANNEL());
			if(isOk == false) {
				ERROR_LOGGER.error("알람 발송 실패 :{}", msg);
			}
		}else{
			LOGGER.info("ARAM ACIVVATION 값이 FALSE로 설정되어 알람 미 발송:{}", msg);
		}
	}

	/**
	 * @param path		:	이동 대상 파일
	 * @param destination	: 	이동 경로 정보
	 */
	private void fileMove(Path path, String destination) {
		if(destination.endsWith(File.separator) ==false)destination += File.separator;
		if(StringUtils.isNotBlank(YYYYMMDD))destination+= YYYYMMDD;
		try{
			Path directory = Paths.get(destination);
			if(Files.exists(directory) == false) Files.createDirectories(directory);

			Path movePath = Paths.get(destination+path.getFileName());
			Files.move(path, movePath, StandardCopyOption.REPLACE_EXISTING);
		}catch(Exception e) {
			ERROR_LOGGER.error(path.toString()+"이동 중 에러 발생",e);
		}
	}

	public String getPROCESS_NAME() { return PROCESS_NAME; }
	public boolean isSUCCESS_ALARM() { return SUCCESS_ALARM; }
	public boolean isERROR_ALARM() { return ERROR_ALARM; }
	
	public String getPROCESS_PATH() { return PROCESS_PATH; }
	public void setPROCESS_PATH(String pROCCESS_PATH) { PROCESS_PATH = pROCCESS_PATH;	}

	public String getSUCCESS_PATH() { return SUCCESS_PATH;	}
	public void setSUCCESS_PATH(String sUCCESS_PATH) { SUCCESS_PATH = sUCCESS_PATH;	}

	public String getERROR_PATH() { return ERROR_PATH;	}
	public void setERROR_PATH(String eRROR_PATH) { ERROR_PATH = eRROR_PATH;	}
}
