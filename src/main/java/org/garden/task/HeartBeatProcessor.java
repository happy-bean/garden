package org.garden.task;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.garden.bootstrap.DefaultExChrangeServer;
import org.garden.bootstrap.ExchangeServer;
import org.garden.core.constants.CodeInfo;
import org.garden.core.paxos.PaxosCore;
import org.garden.core.paxos.PaxosCoreComponent;
import org.garden.core.paxos.PaxosMember;
import org.garden.exchange.DefaultExChangeClient;
import org.garden.exchange.ExchangeClient;
import org.garden.handler.UpStreamHandler;
import org.garden.remoting.Command;
import org.garden.util.RequestAndResponseUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author wgt
 * @date 2018-03-26
 * @description 心跳执行类
 **/
public class HeartBeatProcessor {

    private static final Logger LOGGER = Logger.getLogger(HeartBeatProcessor.class);

    private PaxosCore member = new PaxosCoreComponent();

    /**
     * 默认心跳检查时间
     */
    private static final long DEFAULT_HEARTBEAT_TIME_MILLISECONDS = 1000;

    private ScheduledExecutorService scheduledExecutorService;

    private ExchangeServer heatbeatExchangeServer;

    // 重入锁（ReentrantLock）是一种递归无阻塞的同步机制
    private Lock lock = new ReentrantLock();

    /**
     * 成员ip+port构成唯一标识 成员节点列表
     * <p>
     * 与其它结点的socket连接
     */
    private Map<String, ExchangeClient> exchangeClientCacheMap;

    //结点接收到远程通信指令处理类
    private UpStreamHandler upStreamHandler;

    public HeartBeatProcessor() {

    }

    public void stop() {
        scheduledExecutorService.shutdownNow();
    }

    public void start(UpStreamHandler upStreamHandler) {
        if (upStreamHandler == null) {
            throw new IllegalArgumentException("upstreamHandler不能为空");
        }

        this.upStreamHandler = upStreamHandler;
        PaxosMember paxosMember = upStreamHandler.getPaxosCoreComponent().getCurrentPaxosMember();
        List<PaxosMember> paxosMemberList = upStreamHandler.getPaxosCoreComponent().getOtherPaxosMemberList();
        member.setCurrentPaxosMember(paxosMember);
        member.setOtherPaxosMemberList(paxosMemberList);
        PaxosMember currentMember = member.getCurrentPaxosMember();
        List<PaxosMember> otherMemberList = member.getOtherPaxosMemberList();

        //如果无其它结点就不需要有心跳了
        System.out.println(otherMemberList.toString());
        if (CollectionUtils.isEmpty(otherMemberList)) {
            return;
        }
        exchangeClientCacheMap = new HashMap<String, ExchangeClient>();

        //开启本地监听心跳server
        LOGGER.info("start heart...");
        heatbeatExchangeServer = new DefaultExChrangeServer(upStreamHandler);
        int heartbeatPort = this.getHeartbeatPortByPort(currentMember.getPort());
        heatbeatExchangeServer.start(currentMember.getIp(), heartbeatPort);

        //开启心跳定时器
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(new HeartBeatTask(), 1000, DEFAULT_HEARTBEAT_TIME_MILLISECONDS,
                TimeUnit.MILLISECONDS);

    }

    /**
     * 断线重连
     *
     * @param paxosMember 选举成员
     */
    private void reconnect(PaxosMember paxosMember) {
        lock.lock();

        try {

            // 获取对方心跳ip及端口
            String ip = paxosMember.getIp();
            // 心跳端口
            int heatbeatPort = getHeartbeatPortByPort(paxosMember.getPort());
            LOGGER.debug("begin heartbeat connect to server,ip[" + ip + "],heatbeatPort[" + heatbeatPort + "]");

            //如果之前有连接，则需要先关闭
            String exchangeClientKey = this.getExchangeClientCacheKey(ip, heatbeatPort);
            ExchangeClient exchangeClient = exchangeClientCacheMap.get(exchangeClientKey);

            //如果该节点不存在 或者已经停止 将其从节点列表移除
            if (exchangeClient != null && !exchangeClient.isStoped()) {
                exchangeClient.stop();
                exchangeClientCacheMap.remove(exchangeClientKey);
            }

            //创建新的exchangeClient并重新连接
            UpStreamHandler freshUpstreamHandler = new UpStreamHandler(upStreamHandler.getPaxosCoreComponent());
            //一个client实例代表一条与server连接的socket通道
            ExchangeClient exchangeClientFresh = new DefaultExChangeClient(freshUpstreamHandler);
            //尝试连接
            boolean connectSuccFlag = exchangeClientFresh.connect(ip, heatbeatPort);
            if (!connectSuccFlag) {
                LOGGER.info("can not connect to server,ip[" + ip + "],heatbeatPort[" + heatbeatPort + "]");
                paxosMember.setIsUp(false);
            } else {
                // 连接成功
                paxosMember.setIsUp(true);
                exchangeClientCacheMap.put(exchangeClientKey, exchangeClientFresh);
                LOGGER.info("end heartbeat succ connect to server,ip[" + ip + "],port[" + heatbeatPort + "]");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取连接客户端不存在时
     *
     * @param paxosMember
     */
    public ExchangeClient getExchangeClientReconnectWhenNotExist(PaxosMember paxosMember) {
        String ip = paxosMember.getIp();
        int port = paxosMember.getPort();
        String key = getExchangeClientCacheKey(ip, port);
        ExchangeClient exchangeClient = exchangeClientCacheMap.get(key);
        if (exchangeClient != null) {
            return exchangeClient;
        }

        //没有进行重连然后返回该连接
        LOGGER.info("getExchangeClientReconnectWhenNotExist not found exchangeClient and try to reconnect,ip[" + ip + "],port[" + port
                + "]");
        this.reconnect(paxosMember);

        exchangeClient = exchangeClientCacheMap.get(key);
        if (exchangeClient == null) {
            return null;
        }
        return exchangeClient;
    }

    //心跳任务
    class HeartBeatTask implements Runnable {

        @Override
        public void run() {

            LOGGER.debug("begin heartbeat task");

            List<PaxosMember> otherMemberList = member.getOtherPaxosMemberList();

            for (PaxosMember paxosMember : otherMemberList) {
                LOGGER.debug("heart ip[" + paxosMember.getIp() + "],port[" + paxosMember.getPort() + "] status["
                        + paxosMember.getIsUp() + "]");
                if (!paxosMember.getIsUp()) {
                    //对方结点状态如果是异常的话，需要重连
                    reconnect(paxosMember);
                } else {
                    // 如果与其它结点是活跃的情况下，需要发送心跳
                    this.processLiveTime(paxosMember);

                }

            }
            LOGGER.debug("end heartbeat task");
        }

        /**
         * 处理心跳保活
         *
         * @param paxosMember
         */
        private void processLiveTime(PaxosMember paxosMember) {

            //获取对应exchangeClient的key
            String ip = paxosMember.getIp();
            int heartbeatPort = getHeartbeatPortByPort(paxosMember.getPort());
            String exchangeClientKey = getExchangeClientCacheKey(ip, heartbeatPort);

            //获取对应exchangeClient
            ExchangeClient exchangeClient = exchangeClientCacheMap.get(exchangeClientKey);
            if (exchangeClient == null || exchangeClient.isStoped()) {
                //如果当前连接已经关闭，则重连
                reconnect(paxosMember);
                return;
            }

            //判断当前连接是否超过
            long lastLiveTime = exchangeClient.getLastLiveTime();
            long currentTime = System.currentTimeMillis();
            //默认连接保活超时时间，如果超过这个时间还连不通，则关闭当前连接，然后重连 5秒
            if (currentTime - lastLiveTime > CodeInfo.DEFAULT_CHANNEL_LIVE_TIME_OUT_MILLISECONDS) {
                //如果超过保活最长时间则进行重连
                reconnect(paxosMember);
                return;
            }

            //还未到连接保存超时时间，发送心跳
            Command heartbeatReqCommand = RequestAndResponseUtil.composeHeartbeatRequestCommand();
            try {
                exchangeClient.sendAsyncSync(heartbeatReqCommand);
            } catch (Exception e) {
                LOGGER.debug("send heartbeat err,remote ip[" + ip + "],remote heartbeatPort[" + heartbeatPort + "]");
            }
        }

    }

    /**
     * 获取心跳端口
     *
     * @param port 端口号
     * @return
     */
    private int getHeartbeatPortByPort(int port) {
        return port;
    }

    /**
     * 获取连接缓存key
     *
     * @param ip
     * @param port
     * @return
     */
    private String getExchangeClientCacheKey(String ip, int port) {
        return ip + CodeInfo.IP_AND_PORT_SPLIT + port;
    }

}
