package kr.uracle.ums.fpc;


import kr.uracle.ums.fpc.core.Pitcher;
import kr.uracle.ums.fpc.dbms.SqlSessionManager;
import kr.uracle.ums.fpc.tcpchecker.TcpAliveConManager;
import kr.uracle.ums.fpc.tps.TpsManager;
import kr.uracle.ums.fpc.utils.ConfigManager;
import kr.uracle.ums.fpc.vo.config.*;
import kr.uracle.ums.sdk.util.UmsAlarmSender;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class PitcherManager extends Thread{

	private final static Logger LOGGER = LoggerFactory.getLogger(PitcherManager.class);
	private final static Logger ERROR = LoggerFactory.getLogger("ERROR");
	
	private final List<Pitcher> pitcherExList = new ArrayList<Pitcher>(10);

	private final RootConfigVo ROOT_CONFIG;

	private final AlarmConfigVo ALARM_CONFIG;

	public PitcherManager(RootConfigVo rootConfigBean) {
		this.ROOT_CONFIG = rootConfigBean;
		ALARM_CONFIG = ROOT_CONFIG.getALARM();
	}
	
	public boolean startUp() {
		for(Entry<String, PitcherConfigVo> e : ROOT_CONFIG.getPITCHERS().entrySet()) {
			String name = e.getKey();
			PitcherConfigVo config = e.getValue();
			Pitcher px = new Pitcher(name, ROOT_CONFIG, config);
			boolean isOk = px.initailize();
			if(isOk == false) {
				LOGGER.error("{} 초기화 중 에러 발생으로 기동 중지", name);
				return false;
			}
			px.start();
			pitcherExList.add(px);
		}
		
		return true;
	}

	public List<String> monitoring(){
		long now = System.currentTimeMillis();
		List<String> alarmMsgList = new ArrayList<>(pitcherExList.size());
		for(Pitcher p: pitcherExList) {
			if( p.getState() == State.TERMINATED) {
				alarmMsgList.add(p.getName()+" 종료 됨");
				LOGGER.warn("{} 종료 됨, 확인 필요", p.getName());
			}else {
				long diffTime = now - p.getStartTime();
				if(diffTime > 60*6000){
					alarmMsgList.add(p.getName()+" 처리 지연 중, 현재 처리 소요 시간:"+ diffTime);
					LOGGER.info("{} 처리 지연 중, 현재 처리 소요 시간:{}", p.getName(), diffTime);
				}else{
					LOGGER.info(p.getName()+" STATE: "+p.getPitcherState());
				}
			}
		}
		return alarmMsgList;
	}
	public boolean masterCheck(boolean isMaster, String duplexFile, long EXPIRY_TIME){
		long now = System.currentTimeMillis();
		try {
			Path dPath = Paths.get(duplexFile);
			if(isMaster){
				Files.setLastModifiedTime(dPath, FileTime.fromMillis(now));
			}else{
				FileTime f = Files.getLastModifiedTime(dPath);
				if(now - f.toMillis() > EXPIRY_TIME){
					isMaster = true;
				}
			}
		}catch (Exception e){
			ERROR.error("파일 처리기 마스터 여부 체크 중 에러 발생:",e);
			return false;
		}
		return isMaster;
	}

	public void setMaster(boolean isMaster){
		for(Pitcher p: pitcherExList) {
			p.changeMaster(isMaster);
		}
	}

	@Override
	public void run() {
		LOGGER.info("종료 요청 시그널에 따른 종료");
		for(Pitcher px : pitcherExList) {
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

		// MYBATIS 경로 정보가 있을 때만 실행
		if(StringUtils.isNotBlank(resource)) {
			Configurations configs = new Configurations();
			try {
				XMLConfiguration xmlConfig = configs.xml(resource);
				// Default DataSource Key 추출
				String firstKey = xmlConfig.getString("environments[@default]");
				// Default Datasource Key 등록
				if(StringUtils.isNotBlank(firstKey))SqlSessionManager.getInstance().setFIRST_KEY(firstKey);
				// 등록할 Datasource 설정 정보 추출
				List<Object>idList = xmlConfig.getList("environments.environment[@id]");
				// Datasource type  정보 추출
				String dsType =  xmlConfig.getString("environments.environment.dataSource[@type]");

				// Datasource binding & regist
				for(Object o : idList) {
					String id = o!=null? o.toString(): null;
					if(id != null) {
						InputStream inputStream = new FileInputStream(resource);
						if((isOk=SqlSessionManager.getInstance().load(inputStream, o.toString())) == false) {
							break;
						}
						LOGGER.info("#########################################################");
						LOGGER.info("{} Dadasource Pool({}) Load", id, dsType);
						LOGGER.info("#########################################################");
					}
				}
			}catch(Exception e) {
				isOk = false;
				LOGGER.error("DataSource 설정 중 에러 발생", e);
			}
		}

		// 문제가 있다면 종료
		if(isOk == false)  System.exit(0);

		// 모니터링 정보가 있따면 모니터링 관련 모듈 기동
		if(monitConfig != null) {
			// 모니터링 서버 체크 TCP 모듈 기동
			System.out.println("###########################################################################################");
			System.out.println("###########################################################################################");
			TcpAliveConManager.getInstance().init(null, rootConfig.getUMS_IPADREESS(), monitConfig.getCYCLE_TIME());
			System.out.println("###########################################################################################");
			System.out.println("###########################################################################################");
			// 모니터링 전송 쓰레드 기동
			TpsManager.initialize(monitConfig);
			System.out.println("###########################################################################################");
			System.out.println("###########################################################################################");
		}
				
		// 매니저 Instance 생성
		PitcherManager manager = new PitcherManager(rootConfig);
		// JVM Hook Add 
		Runtime.getRuntime().addShutdownHook(manager);
		
		// Pitchers Start
		isOk = manager.startUp();
		if(isOk == false) System.exit(0);

		// 이중화 설정 가져오기
		DuplexConfigVo duplexConfigVo = rootConfig.getDUPLEX();
		boolean MASTER = duplexConfigVo.isMASTER();
		final boolean DUPLEX_ACTIVATION = duplexConfigVo.isACTIVATION();
		final String DUPLEX_FILE = duplexConfigVo.getDUPLEXING_FILE();
		final long DUPLEX_EXPIRY = duplexConfigVo.getEXPIRY_TIME();

		// 메인 쓰레드 이름 변경
		currentThread().setName("TOTAL MONITORING");

		// 알람 설정 추출
		AlarmConfigVo ALARM_CONFIG = rootConfig.getALARM();
		// 호스트 정보 추출
		String hostInfo = monitConfig !=null? monitConfig.getSERVER_ID(): System.getenv("APP_HOST");
		// 모니터링 주기
		final long  SLEEP_TIME = monitConfig!=null?monitConfig.getCYCLE_TIME()*10:60000;
		while(true) {
			try {
				Thread.sleep(SLEEP_TIME);
			} catch (InterruptedException e) {
				LOGGER.error("Pitcher 모니터링 중 에러", e);
				System.exit(0);
			}
			
			// 이중화 
			if(DUPLEX_ACTIVATION){
				// 마스터 여부 체크
				boolean isMaster = manager.masterCheck(MASTER, DUPLEX_FILE, DUPLEX_EXPIRY);
				// 슬레이브가 마스터가 된 경우
				if(isMaster == true && MASTER == false){
					// 매니저가 관리하는 모든 Pitcher 마스터로 변경
					manager.setMaster(true);
					// 알람 활성화 시 알람 발송
					if(ALARM_CONFIG.isACTIVATION()) UmsAlarmSender.getInstance().sendAlarm(ALARM_CONFIG.getSEND_CHANNEL() , ALARM_CONFIG.getAPI_PATH(), hostInfo+" 파일 처리기 마스터로 변경");
				}
				// 마스터가 슬레이브가 된 경우
				if(isMaster == false && MASTER == true){
					// 매니저가 관리하는 모든 Pitcher 슬레이브로 변경
					manager.setMaster(false);
					// 알람 활성화 시 알람 발송
					if(ALARM_CONFIG.isACTIVATION()) UmsAlarmSender.getInstance().sendAlarm(ALARM_CONFIG.getSEND_CHANNEL() , ALARM_CONFIG.getAPI_PATH(), hostInfo+" 파일 처리기 대기로 변경");
				}
				// 마스터 상태 변경
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
		}
		
	}
}
