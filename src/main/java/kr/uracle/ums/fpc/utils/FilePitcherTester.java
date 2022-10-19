package kr.uracle.ums.fpc.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilePitcherTester {
	protected final static Logger logger = LoggerFactory.getLogger(FilePitcherTester.class);
	
	private static DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");;
	
	public static void makeZeroSizeFile(Path p, int count) {		
		for(int i =0; i<count; i++) {
			String fileName = "zero_"+i+"_"+DATE_TIME_FORMAT.format(LocalDateTime.now())+".txt";
			try {
				Files.createFile(Paths.get(p.toString()+File.separator+fileName));
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
		}
		logger.info("{} 경로에 파일 {}개 생성 완료", p, count);
	}
	public static void makeSuccessFile(Path destinationPath, Path copyTarget, int count) {		
		for(int i =0; i<count; i++) {
			String fileName = "success_"+(i+1)+"_"+DATE_TIME_FORMAT.format(LocalDateTime.now())+".txt";
			try {
				Files.copy(copyTarget, Paths.get(destinationPath.toString()+File.separator+fileName));
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
		}
		logger.info("{} 경로에 파일 {}개 생성 완료", destinationPath, count);
	}
	
	public static void main(String[] args) {
		String path  ="D:\\TEST\\PITCHEREX\\DUMMY";
		String count = "10000";
		String command = "SUCCESS";
		if(args.length >2 ) {
			path =  args[0];
			count = args[1];
			command = args[2];
		}
		Path p = Paths.get(path+File.separator+command);
		if(p.isAbsolute() == false) {
			logger.error("지정 경로는 절대 경로만 가능:{}", p);
			return;
		}
		if(Files.exists(p) == false) {
			try {
				Files.createDirectories(p);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		
		switch(command.toUpperCase()) {
			case "ZERO":
				FilePitcherTester.makeZeroSizeFile(p, Integer.valueOf(count));
				break;
			case "SUCCESS":
				Path copyTarget = Paths.get("D:\\TEST\\PITCHEREX\\test.txt");
				FilePitcherTester.makeSuccessFile(p, copyTarget,Integer.valueOf(count));
				break;
		}
		
	}
}
