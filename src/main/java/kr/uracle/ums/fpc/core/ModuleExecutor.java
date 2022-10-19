package kr.uracle.ums.fpc.core;

import com.google.gson.Gson;
import kr.uracle.ums.fpc.enums.WorkState;
import kr.uracle.ums.fpc.utils.HistoryManager;
import kr.uracle.ums.fpc.vo.module.HistoryVo;
import kr.uracle.ums.fpc.vo.module.TaskVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ModuleExecutor extends Thread{
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	private final Logger ERROR_LOGGER = LoggerFactory.getLogger("ERROR");

	private final long WAIT_TIME = 3*1000;

	private final Gson gson = new Gson();

	private final int INDEX;
	private final String SERVER_ID;
	private final String DBMS_ID;
	private final Module PRE_MODULE;
	private final Module MAIN_MOULE;
	private final Module POST_MODULE;

	private boolean isRun = true;

	private WorkState workState = WorkState.EMPTY;
	private TaskVo taskVo = null;
	private String YYYYMMDD = null;

	public ModuleExecutor(int index, String SERVER_ID, String DBMS_ID, Module PRE_MODULE, Module MAIN_MOULE, Module POST_MODULE) {
		this.setName(index+"번째 모듈 수행자");
		this.INDEX = index;
		this.SERVER_ID = SERVER_ID;
		this.DBMS_ID = DBMS_ID;
		this.PRE_MODULE =PRE_MODULE;
		this.MAIN_MOULE =MAIN_MOULE;
		this.POST_MODULE =POST_MODULE;
	}

	public boolean putWork(Path p, String DIRECTORY_PATTERN){
		if(workState != WorkState.EMPTY) return false;
		this.taskVo = new TaskVo(p);
		this.YYYYMMDD = DIRECTORY_PATTERN;
		workState = WorkState.FULL;
		notify();
		return true;
	}

	private void clearWork(){
		this.taskVo = null;
		this.YYYYMMDD = null;
		workState = WorkState.EMPTY;
	}

	public void close(){
		isRun = false;
	}

	@Override
	public void run() {
		while (isRun){
			if(workState == WorkState.EMPTY){
				LOGGER.debug(this.getName() + " - 일감 없음으로 대기");
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			workState = WorkState.PROCESSING;
			boolean isOk = false;
			boolean isLast = false;
			if(PRE_MODULE != null){
				isLast = (MAIN_MOULE == null && POST_MODULE == null);
				isOk = executeModule(PRE_MODULE, isLast);
			}

			if(MAIN_MOULE != null) {
				isLast = POST_MODULE == null;
				isOk = executeModule(MAIN_MOULE, isLast);
			}

			if(POST_MODULE != null) {
				isOk = executeModule(POST_MODULE, true);
			}

			clearWork();
		}

	}


	public boolean executeModule(Module module, boolean isLastModule) {

		String rsltMsg = "UNKWON ERROR MESSAGE";
		boolean isOk = module.handle(taskVo);

		// 수행 결과가 실패거나 마지막 모듈 수행이라면 결과 처리
		if(isOk == false || isLastModule){
			final String fileName = taskVo.getTARGET_PATH() == null ? "" : taskVo.getTARGET_PATH().getFileName().toString();
			rsltMsg = isOk?fileName+" - 파일 처리 성공" :taskVo.getRESULT_MESSAGE();
			fileMove(taskVo.getTARGET_PATH(), isOk?module.getSUCCESS_PATH():module.getERROR_PATH());
			makeHistory(module.getPROCESS_NAME(), fileName, isOk?"SUCCESS":"ERROR",  rsltMsg);
			if((module.isSUCCESS_ALARM()&&isOk)||(module.isFAIL_ALRAM() && isOk==false)){
				module.sendAlarm(rsltMsg);
			}
			LOGGER.info("{} - {} 처리 {}:{}", module.getPROCESS_NAME(), fileName, isOk?"SUCCESS":"ERROR", rsltMsg);
		}
		return isOk;
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
		HistoryVo historyVo = new HistoryVo();
		historyVo.setSERVER_ID(SERVER_ID);
		historyVo.setPROGRAM_ID(PROGRAM_ID);
		historyVo.setTARGET(TARGET_INFO);
		historyVo.setSTATE(STATE);
		historyVo.setDESCRIPT(MSG);
		HistoryManager.getInstance().recordHistory(historyVo, DBMS_ID);
	}

	public WorkState getWorkState() {return workState;}
}
