package kr.uracle.ums.fpc.module.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.opencsv.CSVWriter;

import kr.uracle.ums.fpc.vo.config.AlarmConfigVo;
import kr.uracle.ums.fpc.vo.config.ModuleConfigaVo;
import kr.uracle.ums.fpc.vo.module.TaskVo;
import kr.uracle.ums.sdk.UmsPotalClient;
import kr.uracle.ums.sdk.vo.ResultVo;
import kr.uracle.ums.sdk.vo.TargetUserKind;
import kr.uracle.ums.sdk.vo.TransType;
import kr.uracle.ums.sdk.vo.UmsPotalParamVo;
import kr.uracle.ums.fpc.core.Module;

/**
 * @author URACLE KKB
 * @see : 일반 파일을 UMS CSV 파일로 변경하여 UMS CSV 발송 요청 처리
 * @see : 사용자 지정 설정(PARAM_MAP)에 HEADER_LIST : CSV헤더 목록(LIST) 필수 지정, CSV 헤더 구성
 * @see : 사용자 지정 설정(PARAM_MAP)에 URL : UMS CSV 발송 URL 필수 지정 값
 * @see : 사용자 지정 설정(PARAM_MAP)에 TRANS_TYPE : REAL/BATCH 실시간 혹은 배치 여부 - BATCH 권장
 * @see : 사용자 지정 설정(PARAM_MAP)에 DELIMETER : 구분자 지정, 파일 데이터 부 파싱 구분자(기본값: |)
 * @see : 사용자 지정 설정(PARAM_MAP)에 ERROR_PATH : 절대경로 설정 정보 지정 시 에러 파일 지정 경로로 이동 시킴(기본값:PRCS_NAME_ERROR)
 * @see : 사용자 지정 설정(PARAM_MAP)에 CSV_PATH : 절대경로 설정 정보 지정 시 CSV 파일 지정 경로로 이동 시킴(기본값:PRCS_NAME_CSV)
 */
public class UmsCsvSender extends Module{
	
	private final UmsPotalClient umsPotalClient = new UmsPotalClient();
	
	private List<String> HEADER_LIST;
	
	private String DELIMETER = "\\|";
	private String CSV_PATH; 
	private String URL;
	private String TRANS_TYPE;
		
	private String CSV_FILE_PATH;
	
	private final Gson gson = new Gson();
		
	public UmsCsvSender(ModuleConfigaVo MODULE_CONFIG, AlarmConfigVo ALARM_CONFIG) {
		super(MODULE_CONFIG, ALARM_CONFIG);
	}

	@Override
	public boolean initialize()  throws Exception{

		// 파일 구분자 취득
		Object dObject = PARAM_MAP.get("DELIMITER");
		DELIMETER = ObjectUtils.isEmpty(dObject)?DELIMETER:dObject.toString();
		
		//CSV 파일 옮길 경로 취득
		Object cObject = PARAM_MAP.get("CSV_PATH");
		String csvPath = ObjectUtils.isEmpty(cObject)?"":cObject.toString();
		if(Paths.get(csvPath).isAbsolute() == false || StringUtils.isBlank(csvPath)) {
			throw new Exception("CSV_PATH  설정을 확인해 주세요.(절대 경로만 지원)");
		}
		if(csvPath.endsWith(File.separator) ==false)csvPath += File.separator;
		CSV_PATH = csvPath;
		
		Object hObject = PARAM_MAP.get("HEADER_LIST");
		if((hObject instanceof List<?>)==false) {
			throw new Exception("HEADER_LIST 설정이 누락됨");
		}
		
		if(ObjectUtils.isEmpty(PARAM_MAP.get("URL")) ||StringUtils.isBlank(PARAM_MAP.get("URL").toString())) {
			throw new Exception("URL 설정이 누락됨");
		}
		
		HEADER_LIST = (List<String>)hObject;
		URL = PARAM_MAP.get("URL").toString();
		TRANS_TYPE = PARAM_MAP.get("TRANS_TYPE")!=null? PARAM_MAP.get("TRANS_TYPE").toString():"BATCH";
		return true;
	}

	@Override
	public boolean process(TaskVo taskVo) throws Exception {
		Path path = taskVo.getTARGET_PATH();
		
		Map<String, String> commonMap = new HashMap<String, String>();
		String rsltMsg = parseFile(path, commonMap);
		
		if(StringUtils.isNotBlank(rsltMsg)) {
			taskVo.setRESULT_MESSAGE(rsltMsg);
			return false;
		}
		
		// 파일 헤더 내용 JSON STRING으로 변환
		UmsPotalParamVo umsVo;
		try {
			String jsonStr =gson.toJson(commonMap);
			umsVo = gson.fromJson(jsonStr, UmsPotalParamVo.class);
		}catch(JsonSyntaxException e) {
			rsltMsg = "파일 헤더부 데이터 포맷 이상 JSON 변환 불가 - 실패 처리, 헤더부 :"+ commonMap +" , 에러메시지:"+e.getMessage();
			taskVo.setRESULT_MESSAGE(rsltMsg);
			return false;
		}

		umsVo.setTARGET_USER_TYPE(TargetUserKind.NC);
		umsVo.setUMS_URL(URL);
		umsVo.setREQ_TRAN_TYPE(TRANS_TYPE.equalsIgnoreCase("REAL")?TransType.REAL:TransType.BATCH);
		if(StringUtils.isBlank(commonMap.get("MSG_TYPE"))) umsVo.setMSG_TYPE("A");
		umsVo.setCSVFILE_ABS_SRC(CSV_FILE_PATH);
		
		ResultVo resultVo = umsPotalClient.umsSend(umsVo, 30);
		boolean isOk = resultVo.getRESULTCODE().equals("0000");
		if(isOk == false) {
			rsltMsg = "결과코드:"+resultVo.getRESULTCODE()+" - "+resultVo.getRESULTMSG();
			taskVo.setRESULT_MESSAGE(rsltMsg);
		}

		return isOk;
	}
	
	private String parseFile(Path p, Map<String, String> commonMap) throws Exception{

		// UMS CSV 발송을 위한 CSV 파일
		if(StringUtils.isBlank(CSV_PATH)) {
			CSV_PATH = p.getParent().getParent()+File.separator+ PROCESS_NAME +"_CSV"+File.separator;
		}
		
		Path derectory = Paths.get(CSV_PATH);
		if(Files.exists(derectory) == false) {
			Files.createDirectories(derectory);
		}			

		
		CSV_FILE_PATH = CSV_PATH+getCsvFileName(p);
		if(Files.exists(Paths.get(CSV_FILE_PATH))) {
			return CSV_FILE_PATH+" - 동일 파일이 존재 합니다";
		}
		
		try(BufferedReader reader = Files.newBufferedReader(p);
				CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(CSV_FILE_PATH)), ',', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)
		){
			// CSV에 헤더 정보 쓰기
			String[] csvHeader = new String[HEADER_LIST.size()];
			int index =0;
			for(String s : HEADER_LIST) {
				csvHeader[index] = "#{"+s+"}";
				index +=1;
			}
			
			writer.writeNext(csvHeader);
						
			String line = null;
			boolean isHeader = true;
			while((line = reader.readLine()) != null) {
				// 개행 전 까지 헤더 영역
				if(StringUtils.isBlank(line)) {
					isHeader = false;
					continue;
				}
				if(isHeader) {
					Map<String, String> map = gson.fromJson(line, new TypeToken<Map<String, String>>(){}.getType());
					if(ObjectUtils.isEmpty(map)) {
						return p +" 파일 처리 실패 - 헤더 정보 없음 : "+line ;
					}
					commonMap.putAll(map);
					
				}else {
					String[] array = line.split(DELIMETER);
					writer.writeNext(array);
				}
			}
			writer.flush();
			
		}catch(IOException e) {
			e.printStackTrace();
			return p +" 파일 처리 중 에러 발생:"+e.getMessage();
		}catch(JsonSyntaxException | IllegalStateException e) {
			e.printStackTrace();
			return p +" 파일 CSV로 변환 중 에러 발생:"+e.getMessage();
		}
		
		return null;
	}
	
	private String getCsvFileName(Path p) {
		String FILE_NAME = p.getFileName().toString();
		return FILE_NAME.substring(0, FILE_NAME.lastIndexOf("."))+".csv";
	}
	
}
