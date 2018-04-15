package org.garden.exchange;

import org.garden.remoting.Command;
import org.garden.remoting.ResponseFuture;

/**
 * @author wgt
 * @date 2018-04-12
 * @description 一个client实例代表一条与server连接的socket通道
 **/
public interface ExchangeClient {

     Object sendSync(Command req) throws Exception;

     ResponseFuture sendAsyncSync(Command req) throws Exception;

     boolean connect(String ip, Integer port);

     void stop();

     long getLastLiveTime();

     void setLastLiveTime(long time);

     boolean isStoped();

}