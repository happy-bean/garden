package org.garden.bootstrap;

import org.garden.handler.UpStreamHandler;

/**
 * @author wgt
 * @date 2018-04-11
 * @description 默认通信服务
 **/
public class DefaultExChrangeServer implements ExchangeServer {

    private UpStreamHandler upStreamHandler;

    private RemotingServer remotingServer;

    public DefaultExChrangeServer(UpStreamHandler upStreamHandler) {
        this.upStreamHandler = upStreamHandler;
    }

    @Override
    public void start(String ip, Integer port) {
        remotingServer = new DefaultRemotingServer(upStreamHandler);
        remotingServer.start(ip, port);
    }

    @Override
    public void stop() {
        remotingServer.stop();
    }

}
