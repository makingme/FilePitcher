package kr.uracle.ums.fpc.core;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import kr.uracle.ums.fpc.utils.HistoryManager;
import kr.uracle.ums.fpc.vo.module.TaskVo;
import kr.uracle.ums.fpc.vo.module.HistoryVo;

public class ModuleManager extends Thread{
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	private final Logger ERROR_LOGGER = LoggerFactory.getLogger("ERROR");
	
	private final Gson gson = new Gson();
	private final TaskVo taskVo;

	private final String YYYYMMDD;

	private final HistoryVo historyVo;

	private Module mainModule = null;
	private Module postModule = null;

	public ModuleManager(TaskVo taskVo, HistoryVo historyVo, String yyyyMMdd) {
		this.taskVo = taskVo;
		this.historyVo = historyVo;
		YYYYMMDD = yyyyMMdd;
	}
	
	@Override
	public void run() {

		boolean isOk = false;
		if(mainModule != null) {
			boolean isLast = postModule == null;
			isOk = executeModule(mainModule, isLast);
			if(isOk == false || isLast) return;
		}
		
		if(postModule != null) {
			isOk = executeModule(postModule, true);
		}
		close();
	}
	public void close() {
		mainModule = null;
		postModule = null;

	}

	public boolean executeModule(Module module, boolean isLastModule) {
		boolean isOk = false;
		String rsltMsg = "UNKWON ERROR MESSAGE";
		try {
			isOk = module.initialize();
		}catch(Exception e) {
			ERROR_LOGGER.error(module.getPROCESS_NAME()+" 모듈 초기화 중 에러 발생",  e);
			rsltMsg = e.getMessage();
		}
		
		if(isOk == false) {
			processResult(taskVo, module, rsltMsg, "ERROR");
			return false;
		}
		
		isOk = module.handle(taskVo);

		if(isOk == false) {
			rsltMsg = taskVo.getRESULT_MESSAGE();
			processResult(taskVo, module, rsltMsg, "ERROR");
			return false;
		}else {
			if(isLastModule == false) return true;
			rsltMsg =  (taskVo.getTARGET_PATH()== null ? "" : taskVo.getTARGET_PATH().getFileName().toString())+" - 파일 처리 성공";
			processResult(taskVo, module, rsltMsg, "SUCCESS");
			if(module.isSUCCESS_ALARM()) module.sendAlarm(rsltMsg);
		}
		
		return true;
	}

	private void processResult(TaskVo taskVo, Module module, String rsltMsg, String state){
		final Path targetPath  = taskVo.getTARGET_PATH();
		final String fileName = targetPath == null ? "" : targetPath.getFileName().toString();
		fileMove(targetPath, module.getERROR_PATH());

		makeHistory(module.getPROCESS_NAME(), fileName, "ERROR",  rsltMsg);
		if(module.isFAIL_ALRAM()) module.sendAlarm(rsltMsg);
		LOGGER.info("{} - {} 처리 실패:{}", module.getPROCESS_NAME(), targetPath, rsltMsg);
	}
		
	private void fileMove(Path path, String destination) {
		if(destination.endsWith(File.separator) ==false)destination += File.separator;
		if(StringUtils.isNotBlank(YYYYMMDD))destination+= YYYYMMDD;
		try{
			Path directory = Paths.get(destination);
			if(Files.exists(directory) == false) Files.createDirectories(directory);
			
			Path movePath = Paths.get(destination+path.getFileName());
			Files.move(path, movePath, StandardCopyOption.REPLACE_EXISTING);
			taskVo.setTARGET_PATH(movePath);
		}catch(Exception e) {
			ERROR_LOGGER.error(path.toString()+"이동 중 에러 발생",e);
		}
	}
	
	private void makeHistory(String PROGRAM_ID, String TARGET_INFO, String STATE, String MSG) {
		historyVo.setPROGRAM_ID(PROGRAM_ID);
		historyVo.setTARGET(TARGET_INFO);
		historyVo.setSTATE(STATE);
		historyVo.setDESCRIPT(MSG);
		String logMsg = HistoryManager.getInstance().recordHistory(historyVo);
		if(StringUtils.isNotBlank(logMsg)) LOGGER.info("히스토리에러:{}, LOG={}",logMsg, gson.toJson(historyVo));
	}
	
	
	public void setMainModule(Module mainModule) { this.mainModule = mainModule; }
	public void setPostModule(Module postModule) { this.postModule = postModule; }
	

}
