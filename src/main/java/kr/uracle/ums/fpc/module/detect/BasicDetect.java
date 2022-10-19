package kr.uracle.ums.fpc.module.detect;

import kr.uracle.ums.fpc.core.Detect;
import kr.uracle.ums.fpc.vo.config.AlarmConfigVo;
import kr.uracle.ums.fpc.vo.config.ModuleConfigaVo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author : URACLE KKB
 * @see : 지정 경로의 파일 탐색 - 쓰기 가능한 상태이며, 지정경로에 WATCH_TIME(15초) 이상 사이즈가 동일한 파일 탐색
 * @see : 사용자 지정 설정(PARAM_MAP)에 WATCH_TIME : 1000(mils) 설정 정보 지정 시 파일 탐색 시간 시간 변동 가능(최소: 15*1000)
 */
public class BasicDetect extends Detect{
	// 파일 탐색 대상 관리 MAP
	private final Map<File, String> fileHistory = new HashMap<File, String>();
	// 감시 주기 초기화
	private long WATCH_TIME = 15*1000;
	// 패턴 정보
	private String PATTERN;
	
	public BasicDetect(ModuleConfigaVo MODULE_CONFIG, AlarmConfigVo ALARM_CONFIG) {
		super(MODULE_CONFIG, ALARM_CONFIG);
	}

	@Override
	public boolean initialize() throws Exception{
		// 감시 주기 설정 값 가져오기 - 정규식으로 지정해야함
		Object wObj = PARAM_MAP.get("WATCH_TIME");
		if(wObj != null) {
			long wTime = Long.parseLong(wObj.toString().replaceAll("\\D", ""));
			if(wTime != 0)WATCH_TIME = wTime;
		}
		//패턴 지정 안할 경우 파일명에 . 이포함된 모든 파일 대상화
		PATTERN = PARAM_MAP.get("PATTERN")!=null?PARAM_MAP.get("PATTERN").toString():".+";
		return true;
	}
	
	@Override
	public List<Path> process(Path path)throws Exception {
		// 최종 탐색 파일 전달 LIST
		List<Path> targetFileList = new ArrayList<Path>();
		// 파일 탐색
		try(Stream<Path> pathStream = Files.walk(path)){
			List<Path> list = pathStream.filter(p -> p.toFile().isFile() && Pattern.matches(PATTERN, p.getFileName().toString())).collect(Collectors.toList());
			// 지정 경로의 파일이 없으면 빈 인스턴스 리턴
			if(ObjectUtils.isEmpty(list)) {
				// 파일 히스토리에 정보가 있다면 별도의 방법으로 파일이 지워짐으로 간주 - 로그 출력
				if(fileHistory.size() > 0) {
					for(Entry<File, String> element : fileHistory.entrySet()) {
						File f = element.getKey();
						LOGGER.warn("{} 파일이 사라짐", f.getName());
					}
				}
				return targetFileList;
			}

			long now = System.currentTimeMillis();
			int totalCnt = list.size();
			int writeCnt = 0;
			int newCnt = 0;
			int remainCnt =0;
			int prcsCnt = 0;
			for(Path p : list) {
				// 쓰기 불가능 파일은 생성 중 파일로 대상 제외
				if(Files.isWritable(p) == false) {
					writeCnt++;
					continue;
				}

				// 파일 히스트로 확인
				String fileInfo = fileHistory.get(p.toFile());

				// 신규 유입 파일은 히스토리에 등록 후 스킵
				if(StringUtils.isBlank(fileInfo)){
					fileHistory.put(p.toFile(), now+"_"+p.toFile().length());
					newCnt++;
					continue;
				}
				// 파일 히스토리 정보 파싱
				long regTime = Long.parseLong(fileInfo.split("_")[0]);
				long fileSize = Long.parseLong(fileInfo.split("_")[1]);

				// 파일 사이즈가 변경되었다면 대상 제외
				if(p.toFile().length() != fileSize) {
					fileHistory.put(p.toFile(), regTime+"_"+p.toFile().length());
					remainCnt++;
					continue;
				}

				// 기준 시간 미달 파일은 대상 제외
				if(WATCH_TIME > (now - regTime)) {
					remainCnt++;
					continue;
				}

				// 기준 시간, 사이즈 변동 만족 파일 히스토리에서 삭제
				fileHistory.remove(p.toFile());

				// 모든 기준 조건 만족 파일 타겟(처리파일) 목록에 추가
				targetFileList.add(p);
				prcsCnt++;
			}
			// 현재 상태 로그 출력
			LOGGER.info("총 파일:{}, 쓰는중:{}, 신규파일:{}, 감시파일:{}, 처리파일:{}", totalCnt, writeCnt, newCnt, remainCnt, prcsCnt);
		}
		// 타겟 정보 리턴
		return targetFileList;
	}

	public static void main(String[] args) {
		Detect d = new BasicDetect(new ModuleConfigaVo(), new AlarmConfigVo());
		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					while(true){
						Path path = Paths.get("D:\\TEST\\PITCHER\\RECEIVE\\");
						List<Path> targetFileList = d.process(path);
						for(Path p: targetFileList) {
							System.out.println("처리 파일:"+p.toString());
						}
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}catch (Exception e) {

				}
			}
		};
		Thread t = new Thread(r);
		t.setDaemon(false);
		t.start();
	}
}
