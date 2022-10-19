package kr.uracle.ums.fpc.core;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.enums.PATH_KIND;
import kr.uracle.ums.fpc.enums.PitcherState;
import kr.uracle.ums.fpc.tps.TpsManager;
import kr.uracle.ums.fpc.vo.config.AlarmConfigVo;
import kr.uracle.ums.fpc.vo.config.ModuleConfigaVo;
import kr.uracle.ums.fpc.vo.config.PitcherConfigVo;
import kr.uracle.ums.fpc.vo.config.RootConfigVo;
import kr.uracle.ums.fpc.vo.module.TaskVo;
import kr.uracle.ums.fpc.vo.module.HistoryVo;

public class Pitcher extends Thread{
	
	private final Map<PATH_KIND, Path> PATH_MAP = new HashMap<PATH_KIND, Path>(10);
	
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	private final Logger ERROR_LOGGER = LoggerFactory.getLogger("ERROR");
	
	private PitcherState state = PitcherState.READY;

	private boolean isRun = true;
	private boolean isMaster = false;
	
	private final PitcherConfigVo PITCHER_CONFIG;
	private final AlarmConfigVo ALARM_CONFIG;

	private final HistoryVo HISTORY_VO = new HistoryVo();

	private final String PATTERN_OF_DATE;

	private final int MAX_THREAD;
	private long CYCLE_TIME = 30*1000;
	private final int MAX_ERROR_COUNT = 10;
	
	private long startTime = 0;
	private long leadTime =0;
	
	protected List<ModuleManager> moduleManagerList;
			
	private Detect detect = null;
	private Filter filter = null;
	private Module preModule = null;
	private Module mainModule = null;
	private Module postModule = null;
	
	private ModuleConfigaVo preConfig = null;
	private ModuleConfigaVo mainConfig = null;
	private ModuleConfigaVo postConfig = null;
	
	public Pitcher(String name, RootConfigVo rootConfigBean, PitcherConfigVo config) {
		setName(name);
		this.isMaster = rootConfigBean.getDUPLEX().isMASTER();
		this.ALARM_CONFIG = rootConfigBean.getALARM();
		this.PITCHER_CONFIG = config;

		PATTERN_OF_DATE = config.getSAVE_DIRECTORY();

		HISTORY_VO.setHISTORY_TYPE(config.getHISTORY_TYPE());
		HISTORY_VO.setEXECUTE_INFO(config.getHISTORY_INFO());
		
		MAX_THREAD = config.getMAX_THREAD() <=0 ? 5: config.getMAX_THREAD();
	}

	public boolean initailize(){
		// 파일 처리 핸들러 관리 리스트
		moduleManagerList = new ArrayList<ModuleManager>(MAX_THREAD);
		
		// 수행 주기 설정
		if(PITCHER_CONFIG.getCYCLE() != null && PITCHER_CONFIG.getCYCLE() > 0) {
			CYCLE_TIME = PITCHER_CONFIG.getCYCLE();
		}
		
		PATH_MAP.put(PATH_KIND.DETECT, Paths.get(PITCHER_CONFIG.getDETECT_PATH()));
		PATH_MAP.put(PATH_KIND.PROCESS, Paths.get(PITCHER_CONFIG.getPROCESS_PATH()));
		PATH_MAP.put(PATH_KIND.SUCCESS, Paths.get(PITCHER_CONFIG.getSUCCESS_PATH()));
		PATH_MAP.put(PATH_KIND.ERROR, Paths.get(PITCHER_CONFIG.getERROR_PATH()));
		
		HISTORY_VO.setSERVER_ID(getHostName());
		// DETECT 생성
		ModuleConfigaVo detectConfig = PITCHER_CONFIG.getDETECTION();
		if(ObjectUtils.isEmpty(detectConfig) || StringUtils.isBlank(detectConfig.getCLASS_NAME())) {
			LOGGER.error("DETECTION_CLASS 설정이 누락됨");
			return false;
		}
		detectConfig.setPATH_MAP(PATH_MAP);
		detect = generateModule(detectConfig, ALARM_CONFIG, Detect.class);
		if(detect == null) return false;
		try {
			if(detect.initialize() == false)return false;
		}catch(Exception e) {
			LOGGER.error("["+detect.getPROCESS_NAME()+"] DETECTOR 초기화 중 에러 발생", e);
			return false;
		}
		LOGGER.info("[{}] DETECTOR 초기화 완료:", detect.getPROCESS_NAME());
		
		// 필터 생성
		ModuleConfigaVo filterConfig = PITCHER_CONFIG.getFILTER();
		if(ObjectUtils.isNotEmpty(filterConfig) && StringUtils.isNotBlank(filterConfig.getCLASS_NAME())) {
			filterConfig.setPATH_MAP(PATH_MAP);
			filter = generateModule(filterConfig, ALARM_CONFIG,  Filter.class);
			if(filter == null) return false;
			try {
				if(filter.initialize() == false)return false;
			}catch(Exception e) {
				LOGGER.error("["+filter.getPROCESS_NAME()+"] FILTER 초기화 중 에러 발생",  e);
				return false;
			}
			LOGGER.info("[{}] FILTER 초기화 완료:", filter.getPROCESS_NAME());
		}
		
		boolean isOk = false;
		// 전처리기 생성
		preConfig = PITCHER_CONFIG.getPREMODULE();
		if(ObjectUtils.isNotEmpty(preConfig) && StringUtils.isNotBlank(preConfig.getCLASS_NAME())) {
			preConfig.setPATH_MAP(PATH_MAP);
			preModule = generateModule(preConfig, ALARM_CONFIG, Module.class);
			if(preModule == null) return false;

			try {
				isOk = preModule.initialize();
			} catch (Exception e) {
				LOGGER.error("["+preModule.getPROCESS_NAME()+"] PRE-MODULE 초기화 중 에러 발생", e);
				return false;
			}
			if(isOk == false) {
				LOGGER.error("PREMODULE 초기화 실패로 기동 중지");
				return false;
			}
			LOGGER.info("[{}] PREMODULE 초기화 완료:", preModule.getPROCESS_NAME());
		}
		
		// 본처리기 생성
		mainConfig = PITCHER_CONFIG.getMAINMODULE();
		if(ObjectUtils.isNotEmpty(mainConfig) && StringUtils.isNotBlank(mainConfig.getCLASS_NAME())) {
			mainConfig.setPATH_MAP(PATH_MAP);
			mainModule = generateModule(mainConfig, ALARM_CONFIG, Module.class);
			if(mainModule == null) return false;

			try {
				isOk = mainModule.initialize();
			} catch (Exception e) {
				LOGGER.error("["+mainModule.getPROCESS_NAME()+"] MAIN-MODULE 초기화 중 에러 발생", e);
				return false;
			}
			if(isOk == false) {
				LOGGER.error("MAINMODULE 초기화 실패로 기동 중지");
				return false;
			}
			LOGGER.info("[{}] MAINMODULE 초기화 완료:", mainModule.getPROCESS_NAME());
		}
		
		// 후처리기 생성
		postConfig = PITCHER_CONFIG.getPOSTMODULE();
		if(ObjectUtils.isNotEmpty(postConfig) && StringUtils.isNotBlank(postConfig.getCLASS_NAME())) {
			postConfig.setPATH_MAP(PATH_MAP);
			postModule = generateModule(postConfig, ALARM_CONFIG, Module.class);
			if(postModule == null) return false;

			try {
				isOk = postModule.initialize();
			} catch (Exception e) {
				LOGGER.error("["+postModule.getPROCESS_NAME()+"] POST-MODULE 초기화 중 에러 발생", e);
				return false;
			}
			if(isOk == false) {
				LOGGER.error("POSTMODULE 초기화 실패로 기동 중지");
				return false;
			}
			LOGGER.info("[{}] POSTMODULE 초기화 완료:", postModule.getPROCESS_NAME());
		}
		
		return true;
	}
	
	public void close() {
		isRun = false;
	}
	
	public void run() {
		int errorCnt = 1;
		startTime = 0;
		Path  TARGET_PATH= PATH_MAP.get(PATH_KIND.DETECT);
		while(isRun) {
			state = PitcherState.STANDBY;
			if(errorCnt > MAX_ERROR_COUNT) errorCnt = MAX_ERROR_COUNT;

			try {
				sleep(CYCLE_TIME * errorCnt);
			} catch (InterruptedException e) {
				LOGGER.info("대기 중 에러",e);
				e.printStackTrace();
			}

			// 마스터가 아니면 휴식, 매니저가 마스터 파일을 감시하여 이상 시 슬레이브에서 마스터로 전환 함
			if(isMaster == false) {
				LOGGER.info("현재 슬레이브로 {} 동안 휴식", CYCLE_TIME* errorCnt);
				continue;
			}
			if(createDirectory() == false) {
				continue;
			}
			
			// 수행 시작 시간 - 매니저에서 Hang 여부 판단을 위해 기록해둠 
			startTime = System.currentTimeMillis();
			state = PitcherState.DETECTION;
			
			List<Path> pathList = detect.detect(TARGET_PATH);
			// 탐색 중 에러 발생
			if(pathList == null ) {
				errorCnt +=1;
				LOGGER.debug("탐색 수행 중 에러 발생으로 {} 동안 휴식", CYCLE_TIME * errorCnt);
				state = PitcherState.DONE;
				continue;
			}
			
			// 탐색 파일이 없으면 휴식
			if(pathList.size() <= 0) {
				LOGGER.debug("{} 탐색된 신규 파일 없음, {} 동안 휴식", TARGET_PATH, CYCLE_TIME * errorCnt);
				//에러 횟수 초기화
				errorCnt = 1;
				leadTime = System.currentTimeMillis() - startTime;
				state = PitcherState.DONE;
				continue;
			}

			String yyyyMMdd = StringUtils.isNotBlank(PATTERN_OF_DATE)?LocalDateTime.now().format(DateTimeFormatter.ofPattern(PATTERN_OF_DATE))+ File.separator: null;

			// 지정 필터 수행
			if(filter != null) {
				state = PitcherState.FILTERING;
				boolean isOk  = filter.filtering(pathList, HISTORY_VO, yyyyMMdd);
				if(isOk == false) {
					errorCnt +=1;
					LOGGER.info("필터 수행 중 에러 발생으로 {} 동안 휴식", CYCLE_TIME * errorCnt);
					continue;
				}
			}
			
			int totalCnt = pathList.size();			
			if(totalCnt <= 0) {
				LOGGER.debug("필터 후 {} 처리 파일 없음, {} 동안 휴식", TARGET_PATH, CYCLE_TIME);
				errorCnt = 1;
				leadTime = System.currentTimeMillis() - startTime;
				state = PitcherState.DONE;
				continue;
			}
			TpsManager.getInstance().addInputCnt(totalCnt);

			Predicate<ModuleManager> codition = m -> m == null || m.isAlive() ==false;
			while(pathList.size() > 0) {
				// 만료된 핸들러 제거
				moduleManagerList.removeIf(codition);
				
				// 현재 가용 쓰레드 갯수 확인
				int freeManagerCnt = MAX_THREAD - moduleManagerList.size();
				if(freeManagerCnt <= 0) {
					final long waitForfree = 100;
					try {
						LOGGER.debug("가용 모듈 매니저 없음으로 대기:{}ms", waitForfree);
						sleep(waitForfree);
					} catch (InterruptedException e) {
						LOGGER.info("가용 모들 매니저 생성 대기 중 에러", e);
					}
					continue;
				}
				
				// 가용 가능한 쓰레드 갯수 만큼 파일 처리
				int fileCnt = pathList.size();
				if(fileCnt > freeManagerCnt) fileCnt =  freeManagerCnt;
				
				// 핸들러 파일 처리
				TpsManager.getInstance().addProcessCnt(fileCnt);
				for(int i =0; i<fileCnt ; i++) {
					Path p = pathList.remove(0);
					final TaskVo taskVo = new TaskVo();
					taskVo.setTARGET_PATH(p);
					
					final HistoryVo historyVo = new HistoryVo();
					historyVo.setSERVER_ID(HISTORY_VO.getSERVER_ID());
					historyVo.setHISTORY_TYPE(HISTORY_VO.getHISTORY_TYPE());
					historyVo.setEXECUTE_INFO(HISTORY_VO.getEXECUTE_INFO());
					ModuleManager manager = new ModuleManager(taskVo, historyVo, yyyyMMdd);
					if(mainModule != null) {
						Module newMain = generateModule(mainConfig, ALARM_CONFIG, Module.class);
						manager.setMainModule(newMain);
					}
					
					if(postModule != null) {
						Module newPost = generateModule(postConfig, ALARM_CONFIG, Module.class);
						manager.setPostModule(newPost);
					}
					
					if(preModule != null) {
						Module newPre = generateModule(preConfig, ALARM_CONFIG, Module.class);
						boolean isLastModule = (mainModule == null && postModule == null);
						if(manager.executeModule(newPre, isLastModule) == false) continue;
					}
										
					moduleManagerList.add(manager);
					manager.start();
				}
			}

			//에러 횟수 초기화
			errorCnt = 1;
			leadTime = System.currentTimeMillis() - startTime;
			LOGGER.info("파일 처리 완료, 처리 파일:{}개, 처리 시간:{}ms", totalCnt, leadTime);
			state = PitcherState.DONE;
		}
	}

	private <T> T generateModule(ModuleConfigaVo modulConfig, AlarmConfigVo alarmConfig, Class<T> clazz) {
		try {
			Class<?> targetClass = Class.forName(modulConfig.getCLASS_NAME());
			Constructor<?> ctor = targetClass.getDeclaredConstructor(ModuleConfigaVo.class, AlarmConfigVo.class);
			return clazz.cast(ctor.newInstance(modulConfig, alarmConfig));
		} catch (Exception e) {
			LOGGER.error( modulConfig.getCLASS_NAME()+" 생성 중 에러 발생", e);
			e.printStackTrace();
			return null;
		}
	}
		
	public void changeMaster(boolean isMaster) { this.isMaster = isMaster;	}
	
	public long getStartTime() { return startTime; }
	
	public long getLeadTime() { return leadTime;}
	
	public PitcherState getPitcherState() { return state; }
	
	
	private boolean createDirectory() {
		for(Entry<PATH_KIND, Path> element : PATH_MAP.entrySet()) {
			Path p = element.getValue();
			try {
				if(Files.isDirectory(p) == false) Files.createDirectories(p);
			} catch (IOException e) {
				LOGGER.error(p+" 디렉토리 점검 중 에러 발생", e);
				return false;
			}
		}
		return true;
	}
	
	private String getHostName() {
		String hostname = null;
		hostname = System.getenv("COMPUTERNAME");
		if(hostname != null) return hostname;
		hostname = System.getenv("HOSTNAME");
		if(hostname != null) return hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			LOGGER.info("HOST NAME 추출 중 에러 발생",e);
		}
		if(hostname != null) return hostname;
		return "UnkownHost:"+System.currentTimeMillis();
	}
	
}
