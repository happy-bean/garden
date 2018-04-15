
package com.garden.client;

import com.garden.conn.RequestHeader;
import com.garden.enums.KeeperState;
import com.garden.nodes.Node;
import com.garden.server.GardenThread;
import com.garden.conn.Packet;
import com.garden.nodes.NodesManager;
import org.garden.util.SerializeUtil;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * @author wgt
 * @date 2018-03-10
 * @description 这个类管理客户端的socket I/O。
 **/

public class ClientCnxn {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCnxn.class);

    private static final int SET_WATCHES_MAX_LENGTH = 128 * 1024;

    private static final String GD_SASL_CLIENT_USERNAME = "garden.sasl.client.username";

    static class AuthData {
        AuthData(String scheme, byte data[]) {
            this.scheme = scheme;
            this.data = data;
        }

        String scheme;

        byte data[];
    }

    /**
     * 哪些已经发送出去的目前正在等待响应的包
     */
    private final LinkedList<Packet> pendingQueue = new LinkedList<Packet>();


    /**
     * 那些需要发送的包
     */
    private final LinkedList<Packet> outgoingQueue = new LinkedList<Packet>();

    /**
     * 节点信息
     */
    public volatile static LinkedList<Node> nodeLinkedList
            = new LinkedList<>();

    /**
     * 超时时间
     */
    private int connectTimeout = 10;

    /**
     * 读取超时时间
     */
    private int readTimeout = 10;

    /**
     * 会话超时时间
     */
    private final int sessionTimeout = 10;

    /**
     * 会话ID
     */
    private long sessionId;


    //启动发送和事件处理线程
    public void start() {
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            JobDetail job = JobBuilder.newJob(SendHeart.class)
                    .withIdentity("job", "ping-group").build();

            // 休眠时长可指定时间单位，此处使用秒作为单位（withIntervalInSeconds）
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger", "ping-group").startNow()
                    .withSchedule(simpleSchedule().withIntervalInSeconds(3).repeatForever())
                    .build();

            scheduler.scheduleJob(job, trigger);

            // scheduler.shutdown();

        } catch (SchedulerException se) {
            LOGGER.error("heartbeat send failed ...", se);
        }

    }

    /**
     * 该类为传出的请求队列提供服务并生成心跳。
     */
    class SendHeart implements Job {

        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
            Map<String, Node> map = NodesManager.getNodeGroup();
            Node localNode = NodesManager.getLocalNode();

            Packet packet = new Packet();

            packet.setNode(localNode);

            packet.setRequestHeader(RequestHeader.HEART);

            //向集群发送心跳 发送本节点信息
            for (Node node : map.values()) {

                byte[] data = SerializeUtil.serialize(packet);

                //创建数据报。
                InetAddress inetAddress = node.getInetSocketAddress().getAddress();

                int port = node.getInetSocketAddress().getPort();
                DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);

                DatagramSocket datagramSocket = null;

                try {
                    datagramSocket = new DatagramSocket();
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                //向服务端发送数据
                try {
                    datagramSocket.send(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class EventThread extends GardenThread {
        private final LinkedBlockingQueue<Object> waitingEvents =
                new LinkedBlockingQueue<Object>();


        /**
         * This is really the queued session state until the event
         * thread actually processes the event and hands it to the watcher.
         * But for all intents and purposes this is the state.
         */

        private volatile KeeperState sessionState = KeeperState.Disconnected;

        private volatile boolean wasKilled = false;
        private volatile boolean isRunning = false;

        EventThread() {
            super(makeThreadName("-EventThread"));
            setDaemon(true);
        }


    }

    private static String makeThreadName(String suffix) {
        String name = Thread.currentThread().getName().
                replaceAll("-EventThread", "");
        return name + suffix;
    }

}
