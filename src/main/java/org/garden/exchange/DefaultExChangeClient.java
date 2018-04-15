package org.garden.exchange;

import org.garden.handler.UpStreamHandler;
import org.garden.remoting.*;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wgt
 * @date 2018-04-12
 * @description
 **/
public class DefaultExChangeClient implements ExchangeClient {

    private RemotingClient remotingClient;

    /**
     * 上次这条以通信的心跳更新时间
     */
    private long lastLiveTime;

    /**
     * 在java.util.concurrent.atomic包下，
     * 有AtomicBoolean , AtomicInteger, AtomicLong, AtomicReference等类，
     * 它们的基本特性就是在多线程环境下，执行这些类实例包含的方法时，具有排他性，
     * 即当某个线程进入方法，执行其中的指令时，不会被其他线程打断，而别的线程就像自旋锁一样，
     * 一直等到该方法执行完成，才由JVM从等待队列中选择一个另一个线程进入
     * */
    private AtomicBoolean stopedFlag = new AtomicBoolean(false);

    //结点接收到远程通信指令处理类 handler
    private UpStreamHandler upStreamHandler;

    public DefaultExChangeClient(UpStreamHandler upStreamHandler) {
        lastLiveTime = System.currentTimeMillis();
        this.upStreamHandler = upStreamHandler;
    }

    //RemotingCommand 远程通信指令消息定义
    @Override
    public Object sendSync(Command req) throws Exception {
        //注册future
        DefaultFuture defaultFuture = new DefaultFuture(req, this);

        //异步发送消息
        remotingClient.send(req);

        //阻塞同步获取结果
        return defaultFuture.get();
    }

    //连接
    @Override
    public boolean connect(String ip, Integer port) {

        //远程连接客户端  upStreamHandler结点接收到远程通信指令处理类
        remotingClient = new DefaultRemotingClient(upStreamHandler);
        return remotingClient.connect(ip, port);
    }

    @Override
    public void stop() {
        remotingClient.stop();
        stopedFlag.set(true);
    }

    @Override
    public long getLastLiveTime() {
        return lastLiveTime;
    }

    @Override
    public void setLastLiveTime(long lastLiveTime) {
        this.lastLiveTime = lastLiveTime;
    }

    @Override
    public boolean isStoped() {
        return stopedFlag.get();
    }

    @Override
    public ResponseFuture sendAsyncSync(Command req) throws Exception {
        /**
         * 1.注册future
         */
        DefaultFuture defaultFuture = new DefaultFuture(req, this);

        /**
         * 2.异步发送消息
         */
        remotingClient.send(req);

        return defaultFuture;
    }

}