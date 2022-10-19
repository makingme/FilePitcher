package kr.uracle.ums.fpc.tcpchecker;

import java.net.SocketAddress;

/**
 * Created by Y.B.H(mium2) on 17. 12. 13..
 */
public class TcpAliveHostBean {
    private String protocol = "http";
    private SocketAddress socketAddress;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public void setSocketAddress(SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }
}
