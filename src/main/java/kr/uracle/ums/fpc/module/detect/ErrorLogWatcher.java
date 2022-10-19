package kr.uracle.ums.fpc.module.detect;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import kr.uracle.ums.fpc.core.Detect;
import kr.uracle.ums.fpc.vo.config.AlarmConfigVo;
import kr.uracle.ums.fpc.vo.config.ModuleConfigaVo;

public class ErrorLogWatcher extends Detect{
	private final Set<Path> keySet = new HashSet<Path>(10);
	private WatchService watchService = null;
	
	public ErrorLogWatcher(ModuleConfigaVo MODULE_CONFIG, AlarmConfigVo ALARM_CONFIG) {
		super(MODULE_CONFIG, ALARM_CONFIG);

	}

	@Override
	public boolean initialize() {
		try {
			watchService = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			LOGGER.info("{}",e);
			return false;
		}
		return true;
	}

	@Override
	public List<Path> process(Path path) throws Exception {
		List<Path> targetFileList = new ArrayList<Path>();
		if(keySet.contains(path) == false) {
			path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
			keySet.add(path);
		}
		WatchKey key = watchService.take();
		for (WatchEvent<?> event : key.pollEvents()) {
			WatchEvent.Kind<?> kind = event.kind();
	        if (kind == StandardWatchEventKinds.OVERFLOW) {
	            continue;
	        }
	        WatchEvent<Path> ev = (WatchEvent<Path>)event;
	        Path eventFile = ev.context();
	        targetFileList.add(eventFile);
	        String msg = getSendMsg(eventFile, 0);
	        if(StringUtils.isBlank(msg))continue;
	        sendAlarm(msg);
        }
		return targetFileList;
	}
	
	private String getSendMsg(Path path, int offset) {
		
		return null;
	}

}




























