package kr.uracle.ums.fpc.core;

import kr.uracle.ums.fpc.enums.WorkState;
import kr.uracle.ums.fpc.vo.config.AlarmConfigVo;
import kr.uracle.ums.fpc.vo.config.ModuleConfigaVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleExecutorPool {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final String SERVER_ID;
    private final String DBMS_ID;
    private final String POOL_NAME;
    private final int MAX_THREAD;

    private final ModuleConfigaVo PRE_CONFIG;
    private final ModuleConfigaVo MAIN_CONFIG;
    private final ModuleConfigaVo POST_CONFIG;
    private final AlarmConfigVo ALARM_CONFIG;
    private final Map<ModuleExecutor, Long> EXECUTOR_MAP;

    public ModuleExecutorPool(String SERVER_ID, String DBMS_ID, String POOL_NAME, int THREAD_COUNT, Map<String, ModuleConfigaVo> MODULECONFIG_MAP, AlarmConfigVo ALARM_CONFIG) {
        this.SERVER_ID = SERVER_ID;
        this.DBMS_ID = DBMS_ID;
        this.POOL_NAME = POOL_NAME;
        this.MAX_THREAD = THREAD_COUNT;
        this.EXECUTOR_MAP = new ConcurrentHashMap<ModuleExecutor, Long>(MAX_THREAD);
        this.PRE_CONFIG = MODULECONFIG_MAP.get("PRE");
        this.MAIN_CONFIG = MODULECONFIG_MAP.get("MAIN");
        this.POST_CONFIG = MODULECONFIG_MAP.get("POST");;
        this.ALARM_CONFIG = ALARM_CONFIG;
    }

    public boolean initialize() throws Exception {
        if(PRE_CONFIG ==null && MAIN_CONFIG == null && POST_CONFIG == null){
            LOGGER.error("{} POOL에 지정된 MODULE CONFIG가 없음", POOL_NAME);
            return false;
        }

        for(int i = 1; i<=MAX_THREAD; i++){
            Module preModule = null;
            Module mainModule = null;
            Module postModule = null;
            if(PRE_CONFIG  != null) {
                preModule  = generateModule(PRE_CONFIG, ALARM_CONFIG, Module.class);
                if(preModule.initialize() == false){
                    LOGGER.error("{} POOL에서 {} PRE MODULE 초기화 실패", POOL_NAME, PRE_CONFIG.getCLASS_NAME());
                    return false;
                }
            }
            if(MAIN_CONFIG != null) {
                mainModule = generateModule(MAIN_CONFIG, ALARM_CONFIG, Module.class);
                if(mainModule.initialize() == false){
                    LOGGER.error("{} POOL에서 {} MAIN MODULE 초기화 실패", POOL_NAME, MAIN_CONFIG.getCLASS_NAME());
                    return false;
                }
            }
            if(POST_CONFIG != null) {
                postModule = generateModule(POST_CONFIG, ALARM_CONFIG, Module.class);
                if(postModule.initialize() == false){
                    LOGGER.error("{} POOL에서 {} POST MODULE 초기화 실패", POOL_NAME, POST_CONFIG.getCLASS_NAME());
                    return false;
                }
            }
            ModuleExecutor executor = new ModuleExecutor(i, SERVER_ID, DBMS_ID, preModule, mainModule, postModule);

            if(enroll(executor) == false)return false;
        }
        return true;
    }
    public void execute(){
        for(ModuleExecutor executor : EXECUTOR_MAP.keySet()){
            executor.start();
        }
    }
    public void closeAll(){
        for(ModuleExecutor executor : EXECUTOR_MAP.keySet()){
            executor.close();
        }
    }
    public boolean enroll(ModuleExecutor executor){
        if(EXECUTOR_MAP.size()>= MAX_THREAD) {
            LOGGER.info("{} POOL이 포화 상태 임으로 등록 실패", POOL_NAME);
            return false;
        }
        EXECUTOR_MAP.put(executor, System.currentTimeMillis());
        return true;
    }

    public void release(ModuleExecutor executor){
        if(executor != null){
            executor.close();
            EXECUTOR_MAP.remove(executor);
            LOGGER.info("{} 이 {} POOL으로 부터 RELEASE 됨", executor.getName(), this.POOL_NAME);
        }
    }

    public ModuleExecutor getExecutor(){
        for(ModuleExecutor executor : EXECUTOR_MAP.keySet()){
            if(executor.getWorkState() == WorkState.EMPTY){
                return executor;
            }
        }
        LOGGER.info("{}에 현재 가용 ModuleExecutor 가 없음", this.POOL_NAME);
        return null;
    }

    private <T> T generateModule(ModuleConfigaVo modulConfig, AlarmConfigVo alarmConfig, Class<T> clazz) {
        try {
            Class<?> targetClass = Class.forName(modulConfig.getCLASS_NAME());
            Constructor<?> ctor = targetClass.getDeclaredConstructor(ModuleConfigaVo.class, AlarmConfigVo.class);
            return clazz.cast(ctor.newInstance(modulConfig, alarmConfig));
        } catch (Exception e) {
            LOGGER.error( modulConfig.getCLASS_NAME()+" 생성 중 에러 발생", e);
            e.printStackTrace();
            return null;
        }
    }
}
