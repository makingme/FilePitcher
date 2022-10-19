package kr.uracle.ums.fpc.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import kr.uracle.ums.fpc.enums.PATH_KIND;
import kr.uracle.ums.fpc.vo.config.*;
import kr.uracle.ums.sdk.util.UmsAlarmSender;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Map.Entry;


public class ConfigManager {	

	private final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
	
	private final Gson gson = new Gson();
	private String baseFilePath ="./conf/config.json";
	private String baseCharset = "UTF-8";
	
	private RootConfigVo rootConfig;
	
	public ConfigManager() {
		this.baseFilePath = System.getenv("PITCHER_CONFIG") != null ? System.getenv("PITCHER_CONFIG") : this.baseFilePath;
		this.baseCharset = System.getenv("PITCHER_CHARSET") != null ? System.getenv("PITCHER_CHARSET") : this.baseCharset;
	}
	
	public ConfigManager(String baseFilePath, String charset) {
		if(StringUtils.isBlank(baseFilePath)) baseFilePath =this.baseFilePath;
		if(StringUtils.isBlank(charset))  charset =this.baseCharset;
		
		this.baseFilePath =  baseFilePath;
		this.baseCharset = charset;
	}
	
	public boolean load() { return load(baseFilePath, baseCharset); }
	public boolean load(String filePath, String charSet) {
		// 파일 내용 담을 변수
		String fileContent = null;
		try{
			// 설정 파일 Instance
			File resource=new File(filePath);
			
			// 설정 파일 이 없다면 기동 중지
			if(!resource.exists()) {
				LOGGER.error("{} 파일을 찾을 수 없습니다.", filePath);
				return false;
			}

			// 설정 파일 IO
			try( InputStream is = new FileInputStream(resource);
					BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName(charSet)))	) {
				
				StringBuilder sb=new StringBuilder();
				String val=null;
				while((val=br.readLine()) !=null) sb.append(val);
				fileContent = sb.toString();
			}
			
			// 설정 파일 내용이 없다면 기동 중지
			if(StringUtils.isBlank(fileContent)) {
				LOGGER.error("{}설정 파일에 내용이 없음으로 인해 설정 로드 실패", filePath);
				return false;
			}
			
			// RootConfigVo 추출
			rootConfig = gson.fromJson(fileContent,  new TypeToken<RootConfigVo>(){}.getType());
			if(rootConfig == null) {
				LOGGER.error("{}설정 파일에 내용이 없음으로 인해 설정 로드 실패", filePath);
				return false;
			}
			
			// PITCHER 설정 유무 확인
			if(ObjectUtils.isEmpty(rootConfig.getPITCHERS())) {
				LOGGER.error("{}설정 파일에 PITCHER 설정이 없음, 설정 로드 실패", filePath);
				return false;
			}
			
			// BASE_PATH 값 유효성 확인
			String basePath = rootConfig.getBASE_PATH();
			if(StringUtils.isBlank(basePath)) {
				LOGGER.error("BASE_PATH 설정 값 누락");
				return false;
			}
			// BASE_PATH 절대 경로 여부 체크
			if(Paths.get(basePath).isAbsolute() == false) {
				LOGGER.error("BASE_PATH 설정 값({})은 절대경로 만 가능, 설정 로드 실패", basePath);
				return false;
			}
			// BASE_PATH 파일 구분자 추가
			if(basePath.endsWith(File.separator) == false)  {
				basePath+=File.separator;
				rootConfig.setBASE_PATH(basePath+File.separator);
			}
			
			// 이중화 설정 추출
			DuplexConfigVo duplexConfig = rootConfig.getDUPLEX();
			
			// 이중화 파일 경로 추출
			String duplexFile = duplexConfig.getDUPLEXING_FILE();
			// 
			if(duplexConfig.isACTIVATION() && StringUtils.isBlank(duplexFile)) {
				LOGGER.error("이중화 파일 설정이 비어 있음, 설정 로드 실패");
				return false; 
			}
			if(Paths.get(duplexFile).isAbsolute() == false) {
				if(duplexFile.startsWith(File.separator))duplexFile = duplexFile.substring(1);
				duplexConfig.setDUPLEXING_FILE(basePath+duplexFile);
			}
			
			long duplex_expiryTime = duplexConfig.getEXPIRY_TIME();
			duplexConfig.setEXPIRY_TIME(duplex_expiryTime > 0 ? duplex_expiryTime : 60 * 1000);
			
			// UMS 모니터링 설정
			UmsMonitoringConfigVo monitConfig = rootConfig.getUMS_MONIT();
			if(StringUtils.isBlank(monitConfig.getPROGRAM_ID()) || ObjectUtils.isEmpty(rootConfig.getUMS_IPADREESS())) {
				rootConfig.setUMS_MONIT(null);
			}
			
			if(StringUtils.isBlank(monitConfig.getSERVER_ID())) {
				monitConfig.setSERVER_ID(InetAddress.getLocalHost().getHostName()+"_"+monitConfig.getPROGRAM_ID());
			}
			
			if(StringUtils.isBlank(monitConfig.getSERVER_NAME())) {
				monitConfig.setSERVER_NAME(monitConfig.getSERVER_ID());
			}
			
			// 알람 설정
			AlarmConfigVo alarmConfig = rootConfig.getALARM();
			if(StringUtils.isBlank(alarmConfig.getAPI_PATH())) alarmConfig.setAPI_PATH("api/monit/alarmSendApi.ums");
			if(alarmConfig.getSEND_CHANNEL() ==null)alarmConfig.setSEND_CHANNEL(UmsAlarmSender.CHANNEL.SMS);
			for(Entry<String, PitcherConfigVo> element :rootConfig.getPITCHERS().entrySet()) {
				String name = element.getKey();
				PitcherConfigVo pitcherConfigBean = element.getValue();
				String datePattern = pitcherConfigBean.getSAVE_DIRECTORY();
				if(StringUtils.isNotBlank(datePattern)){
					try{
						DateTimeFormatter.ofPattern(datePattern);
					}catch(IllegalArgumentException e){
						LOGGER.error("지정한 데이터 패턴이 옳바르지 않음:{}", datePattern);
						return false;
					}
				}

				// Fetch 감시 경로 설정
				String detectPath = makeUpPath(basePath, pitcherConfigBean.getDETECT_PATH(), name, PATH_KIND.DETECT);
				pitcherConfigBean.setDETECT_PATH(detectPath);
				
				// Fetch 처리 경로 설정
				String proccessPath = makeUpPath(basePath, pitcherConfigBean.getPROCESS_PATH(), name, PATH_KIND.PROCESS);
				pitcherConfigBean.setPROCESS_PATH(proccessPath);
				
				// Fetch 성공 경로 설정
				String successPath = makeUpPath(basePath, pitcherConfigBean.getSUCCESS_PATH(), name, PATH_KIND.SUCCESS);
				pitcherConfigBean.setSUCCESS_PATH(successPath);
				
				// Fetch 에러 경로 설정
				String errorPath = makeUpPath(basePath, pitcherConfigBean.getERROR_PATH(), name, PATH_KIND.ERROR);
				pitcherConfigBean.setERROR_PATH(errorPath);
			}
			
			LOGGER.info("{}설정 파일 정상 로드", filePath);
		}catch (FileNotFoundException e) {
			LOGGER.error("RootManager 설정 파일({})이 없습니다.", filePath);
		}catch (IOException e) {
			LOGGER.error("RootManager 설정 파일({}) 처리중 IO 에러 발생", filePath);
		}catch(JsonSyntaxException e) {
			LOGGER.error("RootManager 설정 파일({}) JSON 포맷 이상", filePath);
		}catch(Exception e) {
			LOGGER.error("RootManager 설정 파일("+filePath+") 로딩 중 에러 발생", e);
		}
		return true;
	}
	
	private String makeUpPath(String basePath, String path, String name, PATH_KIND path_kind) {
		if(StringUtils.isBlank(path) ) {
			path = basePath+ name + File.separator+path_kind.toString();
		}
		
		if(Paths.get(path).isAbsolute() == false ) {
			path = basePath + path;
		}
		
		if(path.endsWith(File.separator) == false) path += File.separator;
		
		return path;
	}
	
	private String loadJsonFile(String filePath, String charSet) throws IOException {
		String fileContent = null;
		File resource=new File(filePath);
		if(!resource.exists()) {
			LOGGER.error("{} 파일을 찾을 수 없습니다.", filePath);
			return null;
		}
		try( InputStream is = new FileInputStream(resource);
				BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName(charSet)))	) {
			
			StringBuilder sb=new StringBuilder();
			String val=null;
			while((val=br.readLine()) !=null) sb.append(val);
			fileContent = sb.toString();
		}
		return fileContent;
	}
		
	public RootConfigVo getRootConfig() { return rootConfig; }

	public String getBaseFilePath() { return baseFilePath; }

}
