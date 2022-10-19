package kr.uracle.ums.fpc.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import kr.uracle.ums.fpc.dbms.SqlSessionManager;
import kr.uracle.ums.fpc.vo.module.HistoryVo;

public class HistoryManager {
	private final Logger ERROR_LOGGER = LoggerFactory.getLogger("ERROR");
	private final Logger HISTORY_LOGGER = LoggerFactory.getLogger("HISTORY");
	
	private final Gson gson = new Gson();
		
	public static HistoryManager getInstance() {
		return HistoryManagerInstance.instance;
	}
	
	private static class HistoryManagerInstance{
		private static final HistoryManager instance = new HistoryManager();
	}
	
	public String recordHistory(HistoryVo vo) {
		String rsltMsg = null;
		switch(vo.getHISTORY_TYPE()) {
			case DB:
				rsltMsg = recordInDbms(vo);
				break;
			case LOG:
				rsltMsg = recordInLog(vo);
				break;
		}
		
		return rsltMsg;
	}
		
	public String recordInDbms(HistoryVo vo) {
		SqlSessionFactory f =SqlSessionManager.getInstance().getSqlSessionFactory();
		try(SqlSession session = f.openSession(true)){
			String queryId = vo.getEXECUTE_INFO();
			if(StringUtils.isBlank(queryId))queryId = "mybatis.pitcher.history.recordHistory";
			int cnt = session.insert(queryId, vo);
			if(cnt <= 0) return "DBMS에 히스토리 등록 실패";
		}catch(Exception e){
			ERROR_LOGGER.error("히스토리 DB 등록 중 에러 발생",e);
			return e.getMessage();
		}
		
		return null;
	}
	
	public String recordInLog(HistoryVo vo) {
		HISTORY_LOGGER.info(gson.toJson(vo));
		return null;
	}
}
