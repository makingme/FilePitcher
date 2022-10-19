package kr.uracle.ums.fpc.utils;

import com.google.gson.Gson;
import kr.uracle.ums.fpc.dbms.SqlSessionManager;
import kr.uracle.ums.fpc.vo.module.HistoryVo;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryManager {
	private final Logger ERROR_LOGGER = LoggerFactory.getLogger("ERROR");
	private final Logger HISTORY_LOGGER = LoggerFactory.getLogger("HISTORY");
	
	private final Gson gson = new Gson();

	private final String QUERY_ID = "mybatis.pitcher.history.recordHistory";
		
	public static HistoryManager getInstance() {
		return HistoryManagerInstance.instance;
	}
	
	private static class HistoryManagerInstance{
		private static final HistoryManager instance = new HistoryManager();
	}
	
	public void recordHistory(HistoryVo vo, String dbmsId) {
		HISTORY_LOGGER.info(gson.toJson(vo));
		SqlSessionFactory f =SqlSessionManager.getInstance().getSqlSessionFactory(dbmsId);
		try(SqlSession session = f.openSession(true)){
			int cnt = session.insert(QUERY_ID, vo);
			if(cnt <= 0) ERROR_LOGGER.error("DBMS에 히스토리 등록 실패 - INSERT 결과 실패(INSERT RETURN COUNT IS ZERO)");
		}catch(Exception e){
			ERROR_LOGGER.error("히스토리 DB 등록 중 에러 발생",e);
		}
	}
}
