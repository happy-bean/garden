package org.garden.bootstrap;

/**
 * @author wgt
 * @date 2018-04-11
 * @description 远程服务
 **/
public interface RemotingServer {

    void start(String ip, int port);

    void stop();

}