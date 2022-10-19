package kr.uracle.ums.fpc.tcpchecker;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

/**
 * Created by Y.B.H(mium2) on 17. 8. 9..
 * 해당 TCP 메니저는 등록된 Legacy 시스템을 주기적으로 감시하여 Loadbalancing과 Failover처리를 한다.
 */
public class TcpAliveConManager{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private Gson gson = new Gson();

    private List<String> aliveHostNameList = new ArrayList<String>();
    private Set<String> aliveHostNameSet = new HashSet<String>();
    private Map<String,TcpAliveHostBean> allConHostMap = new HashMap<String, TcpAliveHostBean>();
    private List<String> badHostNameList = new ArrayList<String>();

    private int lastSendSocketNo = 0;
    private long connetionRetryTime = 1*60*1000;

    private TcpAliveConThread tcpAliveConThread = null;

    private static TcpAliveConManager instance = null;

    private TcpAliveConManager(){}

    private String localhostName = "";

    private boolean isLocalhostAlive = false;

    public static TcpAliveConManager getInstance() {
        if(instance==null){
            instance = new TcpAliveConManager();
        }
        return instance;
    }

    /**
     * 감시대상 네거시 호스트 정보와 감시 주기 시간을 설정 하여 구동
     * @param localHostName
     * @param failoverHostNames
     * @param checkLoopTime
     */
    public void init(String localHostName, List<String> failoverHostNames, long checkLoopTime){
        if(localHostName!=null) {
            this.localhostName = localHostName;
            failoverHostNames.add(localHostName);
        }
        connetionRetryTime = checkLoopTime;
        for(String hostName : failoverHostNames){
            int findIndex = hostName.indexOf("://");
            if(findIndex>0){
                String protocol = hostName.substring(0,findIndex);  // http or https일 경우는 포트를 명시 하지 않을 경우 default 값 80 or 443으로 셋팅한다.
                String hostAddress = hostName.substring(findIndex+3); // +3 이유 "://" 다음 문자 부터 가져오기 위해.
                String chkProtocol = protocol.trim().toLowerCase();

                String onlyHostName = hostAddress;
                //http or https는 컨텍스트 루트가 있을 수 있고 포트가 없을 수 있다.
                int ctxRootChkIndex = hostAddress.indexOf("/");
                if(ctxRootChkIndex>0){
                    onlyHostName = hostAddress.substring(0,ctxRootChkIndex);
                }

                String aliveChkIP = "";
                int aliveChkPort = 0;
                int portChkIndex = onlyHostName.indexOf(":");
                // 포트가 없을 경우 http, https는 기본포트로 셋팅
                if(portChkIndex<0){
                    if(chkProtocol.equals("http")){
                        aliveChkIP = onlyHostName;
                        aliveChkPort = 80;
                    }else if(chkProtocol.equals("https")){
                        aliveChkIP = onlyHostName;
                        aliveChkPort = 443;
                    }else{
                        badHostNameList.add(hostName);
                    }
                }else{
                    try {
                        String[] conAddrsssArr = onlyHostName.split(":");
                        aliveChkIP = conAddrsssArr[0];
                        aliveChkPort = Integer.parseInt(conAddrsssArr[1]);
                    }catch (Exception e){
                        badHostNameList.add(hostName);
                        e.printStackTrace();
                    }
                }

                try{
                    //분리해 낸 아이피 포트 검증
                    if(!aliveChkIP.equals("") && aliveChkPort!=0) {
                        SocketAddress socketAddress = new InetSocketAddress(aliveChkIP, aliveChkPort);
                        TcpAliveHostBean tcpAliveHostBean = new TcpAliveHostBean();
                        tcpAliveHostBean.setProtocol(protocol);
                        tcpAliveHostBean.setSocketAddress(socketAddress);
                        // 체크해야할 socketAddress을 등록한다.
                        allConHostMap.put(hostName,tcpAliveHostBean);
                    }else{
                        badHostNameList.add(hostName);
                    }
                }catch (Exception e){
                    badHostNameList.add(hostName);
                    e.printStackTrace();
                }

            }else{
                badHostNameList.add(hostName);
            }
        }
        logger.info("#### allConHostMap.size():{}", allConHostMap.size());
        if(allConHostMap.size()>0) {
            tcpAliveConThread = new TcpAliveConThread(this);
            tcpAliveConThread.start();
        }
    }

    /**
     * 연결가능 한 네거시 시스템 중 라운드로빈 방식으로 로드밸런싱 하며 접속할 서버 호스트 정보를 보내준다.
     * @return
     * @throws Exception
     */
    public synchronized String getConHostName() throws Exception{
        if(isLocalhostAlive){
            return localhostName;
        }

        if(aliveHostNameList.size()==0){
            throw new Exception("There is no session is connected UMS.");
        }

        int newConHostNo = 0;
        if(aliveHostNameList.size()>1){
            newConHostNo =(lastSendSocketNo)%aliveHostNameList.size();
            lastSendSocketNo = newConHostNo+1;
        }
        logger.trace("#### 연결할 호스트 NO :" + newConHostNo + "    가장 최근 연결 한 호스트 번호:" + lastSendSocketNo + "    연결 가능한 호스트 사이즈 :" + aliveHostNameList.size());

        String newConHostName = aliveHostNameList.get(newConHostNo);

        return newConHostName;
    }

    public TcpAliveHostBean getHostInfoBean(String hostName){
        return allConHostMap.get(hostName);
    }

    // 연결 성공시 처리
    protected synchronized void successConSocketAddress(String hostName, SocketAddress socketAddress){
        if(localhostName.equals(hostName)){
            isLocalhostAlive = true;
        }else {
            aliveHostNameList.add(hostName);
            aliveHostNameSet.add(hostName);
        }
    }

    // 연결 실패시 처리
    protected synchronized void failConSocketAddress(String hostName, SocketAddress socketAddress){
        if(localhostName.equals(hostName)){
            isLocalhostAlive = false;
        }else {
            aliveHostNameSet.remove(hostName);
            aliveHostNameList.remove(hostName);
        }

        if(aliveHostNameSet.size()!=aliveHostNameList.size()){
            // 동기화 처리 작업 수행 하도록 함.
            logger.warn("!!!!!!  ALIVE HOST 동기화 작업 수행~!!!!!!!!!");
            aliveHostNameList.clear();
            logger.debug("### aliveHostNameList size: " + aliveHostNameList.size());
            for(String aliveHostName : aliveHostNameSet){
                aliveHostNameList.add(aliveHostName);
            }
        }
    }

    /**
     * 감시를 요청 한 네거시 시스템 호스트정보가 올바르지 않은 리스트를 통지한다.
     * @return
     */
    public List<String> getBadHostNameList() {
        return badHostNameList;
    }

    /**
     * 사용 가능한 네거시 시스템 호스트 리스트 정보를 알려준다
     * @return
     */
    public List<String> getAliveHostNameList() {
        List<String> cloneAliveHostNames = new ArrayList<String>();
        cloneAliveHostNames.addAll(aliveHostNameList);

        return cloneAliveHostNames;
    }

    protected Set<String> getAliveHostNameSet() {
        return aliveHostNameSet;
    }

    /**
     * 감시 대상 네거시 호스트정보를 관리 하는 맵 정보
     * @return
     */
    protected Map<String, TcpAliveHostBean> getAllConHostMap() {
        return allConHostMap;
    }

    /**
     * 감시주기 정보 확인
     * @return
     */
    public long getConnetionRetryTime() {
        return connetionRetryTime;
    }

    /**
     * 현재 할당 가능한 네거시 시스템 호스트 정보를 프린트 한다.
     */
    public void tcpAliveInfoPrint() {
        logger.info("### BAD HOSTS : {}",gson.toJson(badHostNameList));
        logger.info("### Alive HOSTS : {}",gson.toJson(aliveHostNameList));
    }

    /**
     * 어플리케이션 종료시 감시쓰레드를 종료한다.
     */
    public void destroy(){
        // 쓰레드 종료 시킨다.
        tcpAliveConThread.interrupt();
        tcpAliveConThread.setT_RUNNING(false);
    }
}

