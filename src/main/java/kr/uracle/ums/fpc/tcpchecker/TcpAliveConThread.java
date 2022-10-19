package kr.uracle.ums.fpc.tcpchecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Set;

/**
 * Created by Y.B.H(mium2) on 17. 8. 21..
 */
public class TcpAliveConThread extends Thread{

    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private boolean T_RUNNING = true;
    private final TcpAliveConManager tcpAliveConManager;

    public TcpAliveConThread(TcpAliveConManager _tcpAliveConManager){
        this.tcpAliveConManager = _tcpAliveConManager;
    }

    @Override
    public void run() {
        logger.info("### TCP ALIVIE CHECKER START~~!");

        while(T_RUNNING) {
            Set<Map.Entry<String,TcpAliveHostBean>> reConMapSet = tcpAliveConManager.getAllConHostMap().entrySet();
            for(Map.Entry<String,TcpAliveHostBean> reConMapEntry : reConMapSet) {
                Socket socket = new Socket();
                String hostName = reConMapEntry.getKey();
                TcpAliveHostBean tcpAliveHostBean = reConMapEntry.getValue();
                // Timeout required - it's in milliseconds
                int timeout = 2000;
                try {
                    socket.connect(tcpAliveHostBean.getSocketAddress(), timeout);
                    if(socket.isConnected()) {
                        socket.close();
                    }
                    if(!tcpAliveConManager.getAliveHostNameSet().contains(hostName)){
                        tcpAliveConManager.successConSocketAddress(hostName,tcpAliveHostBean.getSocketAddress());
                    }

                } catch (SocketTimeoutException exception) {
                	logger.debug("SocketTimeoutException [" + hostName + "] : " + tcpAliveHostBean.getSocketAddress().toString() + ". " + exception.toString());
                    tcpAliveConManager.failConSocketAddress(hostName, tcpAliveHostBean.getSocketAddress());
                } catch (IOException exception) {
                	logger.debug("IOException - Unable to connect to [" + hostName + "] : " + tcpAliveHostBean.getSocketAddress().toString() + ". " + exception.toString());
                	tcpAliveConManager.failConSocketAddress(hostName, tcpAliveHostBean.getSocketAddress());                    
                }
            }

            try {
                Thread.sleep(tcpAliveConManager.getConnetionRetryTime());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }



    public void setT_RUNNING(boolean t_RUNNING) {
        T_RUNNING = t_RUNNING;
    }
}
