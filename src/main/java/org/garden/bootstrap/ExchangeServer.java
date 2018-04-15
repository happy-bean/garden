package org.garden.bootstrap;

/**
 * @author wgt
 * @date 2018-04-11
 * @description 通信服务
 **/
public interface ExchangeServer {

     void start(String ip,Integer port);

     void stop();

}
