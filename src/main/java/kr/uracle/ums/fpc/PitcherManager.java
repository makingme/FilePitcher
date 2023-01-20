package kr.uracle.ums.fpc;


import kr.uracle.ums.fpc.core.Pitcher;
import kr.uracle.ums.fpc.dbms.SqlSessionManager;
import kr.uracle.ums.fpc.tcpchecker.TcpAliveConManager;
import kr.uracle.ums.fpc.tps.TpsManager;
import kr.uracle.ums.fpc.utils.ConfigManager;
import kr.uracle.ums.fpc.vo.config.*;
import kr.uracle.ums.sdk.util.UmsAlarmSender;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PitcherManager extends Thread{

	private final static Logger LOGGER = LoggerFactory.getLogger(PitcherManager.class);
	private final static Logger ERROR = LoggerFactory.getLogger("ERROR");
	
	private static boolean isRun = true;

	private final static Pattern PATTERN  =  Pattern.compile("(\\#\\{)([가-힣a-zA-Z0-9\\[\\]\\/?.,;:|\\)*~`!^\\-_+<>@\\#$%&\\=\\(\\'\\\"]+)(\\})");
	
	private final List<Pitcher> PITCHER_LIST = new ArrayList<Pitcher>(10);
	
	private final RootConfigVo ROOT_CONFIG;

	private final AlarmConfigVo ALARM_CONFIG;

	public PitcherManager(RootConfigVo rootConfigBean) {
		this.ROOT_CONFIG = rootConfigBean;
		this.ALARM_CONFIG = ROOT_CONFIG.getALARM();
	}
	
	// 등록된 Pitcher 기동
	public boolean startUp() {
		// 등록된 Pitcher 설정만큼 Loop
		for(Entry<String, PitcherConfigVo> e : ROOT_CONFIG.getPITCHERS().entrySet()) {
			// Pitcher명 취득
			String name = e.getKey();
			// Pitcher 설정 취득
			PitcherConfigVo pxConfig = e.getValue();
			// Pitcher 인스턴스 생성
			Pitcher px = new Pitcher(name, ROOT_CONFIG, pxConfig);
			// Pitcher 초기화
			if(px.initailize() == false) {
				LOGGER.error("{} 초기화 중 에러 발생으로 기동 중지", name);
				return false;
			}
			// Pitcher 기동
			px.start();
			// Pitcher 관리를 위한 Pitcher관리 목록에 등록
			PITCHER_LIST.add(px);
		}
		
		return true;
	}

	// Pitcher관리목록에 등록된 Pitcher 상태와 처리 소용 시간 모니터링
	public List<String> monitoring(){
		// 현재 시간 취득
		long now = System.currentTimeMillis();
		// 알람 목록 인스턴스 생성
		List<String> alarmMsgList = new ArrayList<>(PITCHER_LIST.size());
		// 관리목록 Loop
		for(Pitcher p: PITCHER_LIST) {
			// Pitcher 상태가 종료일 경우 알람목록에 추가
			if( p.getState() == State.TERMINATED) {
				alarmMsgList.add(p.getName()+" 종료 됨");
				LOGGER.warn("{} 종료 됨, 확인 필요", p.getName());
				continue;
			}
			
			// Pitcher 쓰레드 행 여부 체크
			String msg = p.checkThreadHang(now);
			if(msg != null){
				alarmMsgList.add(msg);
				LOGGER.warn(msg);
				continue;
			}
			
			// Pitcher 상태 출력
			LOGGER.debug(p.getName()+" 현재 작업 상태: "+p.getPitcherState());
		}
		return alarmMsgList;
	}
	
	// 마스터 체크
	public boolean masterCheck(boolean isMaster, String duplexFile, long EXPIRY_TIME){
		// 현재 시간 추출
		long now = System.currentTimeMillis();
		try {
			// Path 인스턴스 생성
			Path dPath = Paths.get(duplexFile);
			// 마스터 체크 파일 유무 체크
			if(!Files.exists(dPath)){
				LOGGER.warn("마스터 체크 파일 부재로 마스터 체크 파일 신규 생성({})", dPath.toString());
				Files.createFile(dPath);
			}
			
			// 본래 마스터인 경우 마스터 체크 파일 수정 시간 변경
			if(isMaster){
				Files.setLastModifiedTime(dPath, FileTime.fromMillis(now));
				return true;
			}
			// 본래 마스터가 아닌 경우 파일 수정 시간 변동 여부 체크
			FileTime f = Files.getLastModifiedTime(dPath);
			// 변동 시간이 지정 시간보다 오래 되었다면 마스터 프로세스 이상으로 간주하여 마스터로 동작
			if(now - f.toMillis() > EXPIRY_TIME){
				isMaster = true;
			}
			
		}catch (Exception e){
			ERROR.error("파일 처리기 마스터 여부 체크 중 에러 발생:",e);
			return false;
		}
		return isMaster;
	}

	public void setMaster(boolean isMaster){
		for(Pitcher p: PITCHER_LIST) {
			p.changeMaster(isMaster);
		}
	}

	@Override
	public void run() {
		LOGGER.info("종료 요청 시그널에 따른 프로세스 종료");
		isRun = false;
		for(Pitcher px : PITCHER_LIST) {
			px.close();
		}
		TcpAliveConManager.getInstance().destroy();
		TpsManager.getInstance().stopScheduler();
	}


	public static void main(String[] args) {
		// 메인 쓰레드 이름 변경
		currentThread().setName("CONFIG LOADER");

		// 프로그램 변수 : 설정 파일 경로 정보
		String configPath = args.length >0 ?args[0]:"";
		// 프로그램 변수 : 설정 파일 인코딩 정보
		String charSet = args.length>1?args[1]:null;
		
		// 설정 매니저 로딩
		ConfigManager configManager = new ConfigManager(configPath, charSet);
		boolean isOk = configManager.load();
		if(isOk == false) {
			System.exit(0);
		}
		
		// 설정 빈 가져오기
		RootConfigVo rootConfig = configManager.getRootConfig();
	
		// 모니터링 설정 가져오기
		UmsMonitoringConfigVo monitConfig = rootConfig.getUMS_MONIT();
		
		//DataSource 초기화
		String resource = rootConfig.getMYBATIS_PATH();

		// 메인 쓰레드 이름 변경
		currentThread().setName("DATASOURCE LOADER");
		// MYBATIS 경로 정보가 있을 때만 실행
		if(StringUtils.isNotBlank(resource)) {
			Configurations configs = new Configurations();
			try {
				XMLConfiguration xmlConfig = configs.xml(resource);

				// Default DataSource Key 추출
				String firstKey = xmlConfig.getString("environments[@default]");
				// Default Datasource Key 등록
				if(StringUtils.isNotBlank(firstKey))SqlSessionManager.getInstance().setFIRST_KEY(firstKey);
				
				// 데이터 소스 등록
				for(final HierarchicalConfiguration idConfig: xmlConfig.configurationsAt("environments.environment")){
					// 데이터 소스 ID 추출
					String environmentId = idConfig.getString("[@id]");
					// 데이터 소스 타입 추출
					String dataSourceType = idConfig.getString("dataSource[@type]");
					
					// 소스ID, 소스 타입 빈값 체크
					if(StringUtils.isBlank(environmentId) || StringUtils.isBlank(dataSourceType)){
						continue;
					}
					
					// 데이터 소스 로딩
					isOk=SqlSessionManager.getInstance().load(resource, environmentId);
					// 로딩 실패 시 중단
					if(isOk == false){
						break;
					}
					LOGGER.info("#########################################################");
					LOGGER.info("ENVIRONMENT:{} Dadasource Pool(DATASOURCE:{}) Load", environmentId, dataSourceType);
					LOGGER.info("#########################################################");
				}
			}catch(Exception e) {
				isOk = false;
				LOGGER.error("DataSource 설정 중 에러 발생", e);
			}
		}

		// 문제가 있다면 종료
		if(isOk == false)  System.exit(0);

		// UMS 모니터링 정보가 있다면 모니터링 관련 모듈 기동
		if(monitConfig != null) {
			// UMS 모니터링 서버 체크 TCP 모듈 기동
			System.out.println("###########################################################################################");
			System.out.println("###########################################################################################");
			TcpAliveConManager.getInstance().init(null, rootConfig.getUMS_IPADREESS(), monitConfig.getCYCLE_TIME());
			System.out.println("###########################################################################################");
			System.out.println("###########################################################################################");
			// UMS 모니터링 전송 쓰레드 기동
			TpsManager.initialize(monitConfig);
			System.out.println("###########################################################################################");
			System.out.println("###########################################################################################");
		}
				
		// 매니저 Instance 생성
		PitcherManager manager = new PitcherManager(rootConfig);
		// JVM Hook Add 
		Runtime.getRuntime().addShutdownHook(manager);
		
		// 설정된 Pitcher 기동
		isOk = manager.startUp();
		if(isOk == false) System.exit(0);

		// 이중화 설정 가져오기
		DuplexConfigVo duplexConfigVo = rootConfig.getDUPLEX();
		// 프로세스 마스터 여부
		boolean MASTER = duplexConfigVo.isMASTER();
		// 이중화 사용 여부
		final boolean DUPLEX_ACTIVATION = duplexConfigVo.isACTIVATION();
		// 마스터 체크 파일 정보
		final String DUPLEX_FILE = duplexConfigVo.getDUPLEXING_FILE();
		// 마스터 만료 시간
		long DUPLEX_EXPIRY = duplexConfigVo.getEXPIRY_TIME()>0?duplexConfigVo.getEXPIRY_TIME():(3*60*1000);

		// 메인 쓰레드 이름 변경
		currentThread().setName("TOTAL MONITORING");

		// 알람 설정 추출
		AlarmConfigVo ALARM_CONFIG = rootConfig.getALARM();
		// 호스트 정보 추출
		String hostInfo = monitConfig !=null? monitConfig.getSERVER_ID(): System.getenv("APP_HOST");
		// 모니터링 주기
		final long  SLEEP_TIME = rootConfig.getPITCHER_MONIT_CYCLE()>0?rootConfig.getPITCHER_MONIT_CYCLE():(1*60*1000);
		if(SLEEP_TIME >= DUPLEX_EXPIRY){
			DUPLEX_EXPIRY = (SLEEP_TIME*2)+2000;
			LOGGER.info("PITCHER 모니터링 체크 간격이 마스터(이중화) 유효시간 설정 값보다 큼에 따라 마스터 유효 시간이 조정 됨(DUPLEX_EXPIRY = {})", DUPLEX_EXPIRY);
		}
		
		while(isRun) {
			if(MASTER){
				LOGGER.debug(hostInfo+" 파일 처리기가 현재 마스터로 동작 중");
			}else{
				LOGGER.debug(hostInfo+" 파일 처리기가 현재 슬레이브로 대기 중");
			}
			if(DUPLEX_ACTIVATION){
				// 마스터 여부 체크
				boolean isMaster = manager.masterCheck(MASTER, DUPLEX_FILE, DUPLEX_EXPIRY);
				
				if(isMaster != MASTER){
					// 매니저가 관리하는 모든 Pitcher 슬레이브로 변경
					manager.setMaster(isMaster);
					
					if(MASTER){
						// 마스터가 대기 상태로 변경
						if(ALARM_CONFIG.isACTIVATION())UmsAlarmSender.getInstance().sendAlarm(ALARM_CONFIG.getSEND_CHANNEL() , ALARM_CONFIG.getAPI_PATH(), hostInfo+" 파일 처리기(마스터)가 대기(슬레이브) 상태로 전환 됨");
						LOGGER.info(hostInfo+" 파일 처리기가 대기로 변경");
					}else{
						// 슬레이브가 활성화 상태로 변경
						if(ALARM_CONFIG.isACTIVATION())UmsAlarmSender.getInstance().sendAlarm(ALARM_CONFIG.getSEND_CHANNEL() , ALARM_CONFIG.getAPI_PATH(), hostInfo+" 파일 처리기(슬레이브)가 활성화(마스터) 상태로 전환 됨");
						LOGGER.info(hostInfo+" 파일 처리기가 마스터로 변경");
					}
					
				}
				// 마스터 변경
				MASTER = isMaster;
			}
			
			// 매니저가 관리하는 모든 Pitcher 상태 모니터링 - Alive, Hang
			List<String> alramMsgList = manager.monitoring();
			for(String msg : alramMsgList){
				if(ALARM_CONFIG.isACTIVATION()){
					UmsAlarmSender.getInstance().sendAlarm(ALARM_CONFIG.getSEND_CHANNEL() , ALARM_CONFIG.getAPI_PATH(), msg);
				}else{
					ERROR.error(msg);
				}
			}

			try {
				Thread.sleep(SLEEP_TIME);
			} catch (InterruptedException e) {
				LOGGER.error("Pitcher 모니터링 중 에러", e);
				System.exit(0);
			}
		}
		
	}
	
	
}
