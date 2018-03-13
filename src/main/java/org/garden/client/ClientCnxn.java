package org.garden.client;

import org.garden.AsyncCallback;
import org.garden.Garden;
import org.garden.Record;
import org.garden.Garden.WatchRegistration;
import org.garden.enums.EventType;
import org.garden.enums.KeeperState;
import org.garden.io.ByteBufferInputStream;
import org.garden.server.GardenThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author wgt
 * @date 2018-03-10
 * @description 这个类管理客户端的socket I/O。ClientCnxn维护一个可用服务器列表可以根据需要透明地切换服务器
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
     * 是ArrayList 的一个线程安全的变体，其中所有可变操作（add、set等等）都是通过对底层数组进行一次新的复制来实现的
     */
    private final CopyOnWriteArraySet<AuthData> authInfo = new CopyOnWriteArraySet<AuthData>();

    /**
     * 哪些已经发送出去的目前正在等待响应的包
     */
    private final LinkedList<Packet> pendingQueue = new LinkedList<Packet>();

    /**
     * 那些需要发送的包
     */
    private final LinkedList<Packet> outgoingQueue = new LinkedList<Packet>();

    /**
     * 超时时间
     */
    private int connectTimeout;

    /**
     * 客户端与服务器协商的超时时间，以毫秒为单位。这是真正的超时时间，而不是客户端的超时请求
     */
    private volatile int negotiatedSessionTimeout;

    /**
     * 读取超时时间
     */
    private int readTimeout;

    /**
     * 会话超时时间
     */
    private final int sessionTimeout;

    /**
     * garden
     */
    private final Garden garden;

    /**
     * 客户端监视器管理器
     */
    private final ClientWatchManager watcher;

    /**
     * 会话ID
     */
    private long sessionId;

    /**
     * 会话密钥
     */
    private byte sessionPasswd[] = new byte[16];

    /**
     * 如果是真的话，连接就被允许进入r-o模式。
     * 这个字段的值 在会话创建握手过程中，除了其他数据外，还发送了其他数据。
     * 如果 另一边的服务器被分区，它会接受只有只读的客户。
     */
    private boolean readOnly;

    final String chrootPath;

    /**
     * 发送线程
     */
    final SendThread sendThread;

    /**
     * 事件回调线程
     */
    final EventThread eventThread;

    /**
     * 在调用close时设置为true。
     * 连接到我们的连接 在关闭服务器的时候不在试图重新连接到服务器连接
     * （客户端将会话断开连接到服务器，作为关闭的一部分操作)
     */
    private volatile boolean closing = false;

    /**
     * 一组客户端可以连接的gd主机
     */
    private final HostProvider hostProvider;

    /**
     * 第一次和读写服务器建立连接时设置为true，之后不再改变。
     * 这个值用来处理客户端没有sessionId连接只读模式服务器的场景.
     * 客户端从只读服务器收到一个假的sessionId,这个sessionId对于其他服务器是无效的。所以
     * 当客户端寻找一个读写服务器时，它在连接握手时发送0代替假的sessionId，建立一个新的，有效的会话
     * 如果这个属性是false(这就意味着之前没有找到过读写服务器)则表示非0的sessionId是假的否则就是有效的
     */
    volatile boolean seenRwServerBefore = false;

    public ZooKeeperSaslClient zooKeeperSaslClient;

    public long getSessionId() {
        return sessionId;
    }

    public byte[] getSessionPasswd() {
        return sessionPasswd;
    }

    public int getSessionTimeout() {
        return negotiatedSessionTimeout;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        SocketAddress local = sendThread.getClientCnxnSocket().getLocalSocketAddress();
        SocketAddress remote = sendThread.getClientCnxnSocket().getRemoteSocketAddress();
        sb
                .append("sessionid:0x").append(Long.toHexString(getSessionId()))
                .append(" local:").append(local)
                .append(" remoteserver:").append(remote)
                .append(" lastZxid:").append(lastZxid)
                .append(" xid:").append(xid)
                .append(" sent:").append(sendThread.getClientCnxnSocket().getSentCount())
                .append(" recv:").append(sendThread.getClientCnxnSocket().getRecvCount())
                .append(" queuedpkts:").append(outgoingQueue.size())
                .append(" pendingresp:").append(pendingQueue.size())
                .append(" queuedevents:").append(eventThread.waitingEvents.size());

        return sb.toString();
    }

    /**
     * 允许我们传递标题和相关记录
     */
    static class Packet {

        RequestHeader requestHeader;

        ReplyHeader replyHeader;

        Record request;

        Record response;

        ByteBuffer byteBuffer;

        /**
         * Client's view of the path (may differ due to chroot)
         **/
        String clientPath;
        /**
         * Servers's view of the path (may differ due to chroot)
         **/
        String serverPath;

        boolean finished;

        AsyncCallback callback;

        Object ctx;

        WatchRegistration watchRegistration;

        public boolean readOnly;

        WatchDeregistration watchDeregistration;

        /**
         * Convenience ctor
         */
        Packet(RequestHeader requestHeader, ReplyHeader replyHeader,
               Record request, Record response,
               Garden.WatchRegistration watchRegistration) {
            this(requestHeader, replyHeader, request, response,
                    watchRegistration, false);
        }

        Packet(RequestHeader requestHeader, ReplyHeader replyHeader,
               Record request, Record response,
               WatchRegistration watchRegistration, boolean readOnly) {

            this.requestHeader = requestHeader;
            this.replyHeader = replyHeader;
            this.request = request;
            this.response = response;
            this.readOnly = readOnly;
            this.watchRegistration = watchRegistration;
        }

        public void createBB() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BinaryOutputArchive boa = BinaryOutputArchive.getArchive(baos);
                boa.writeInt(-1, "len"); // We'll fill this in later
                if (requestHeader != null) {
                    requestHeader.serialize(boa, "header");
                }
                if (request instanceof ConnectRequest) {
                    request.serialize(boa, "connect");
                    // append "am-I-allowed-to-be-readonly" flag
                    boa.writeBool(readOnly, "readOnly");
                } else if (request != null) {
                    request.serialize(boa, "request");
                }
                baos.close();
                this.bb = ByteBuffer.wrap(baos.toByteArray());
                this.bb.putInt(this.bb.capacity() - 4);
                this.bb.rewind();
            } catch (IOException e) {
                LOG.warn("Ignoring unexpected exception", e);
            }
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("clientPath:" + clientPath);
            stringBuilder.append("serverPath:" + serverPath);
            stringBuilder.append("finished:" + finished);
            stringBuilder.append("header:" + requestHeader);
            stringBuilder.append("replyHeader:" + replyHeader);
            stringBuilder.append("request:" + request);
            stringBuilder.append("response:" + response);

            return stringBuilder.toString().replaceAll("\r*\n+", " ");
        }
    }

    /**
     * 创建一个连接对象。真正的网路连接直到需要的时候才建立。start()方法在执行构造方法后一定要调用
     * 这个构造方法在ZooKeeper的初始化时调用，用于初始化一个客户端网路管理器
     */
    public ClientCnxn(String chrootPath, HostProvider hostProvider, int sessionTimeout, Garden garden,
                      ClientWatchManager watcher, ClientCnxnSocket clientCnxnSocket, boolean canBeReadOnly)
            throws IOException {
        this(chrootPath, hostProvider, sessionTimeout, garden, watcher,
                clientCnxnSocket, 0, new byte[16], canBeReadOnly);
    }


    public ClientCnxn(String chrootPath, HostProvider hostProvider, int sessionTimeout, Garden garden,
                      ClientWatchManager watcher, ClientCnxnSocket clientCnxnSocket,
                      long sessionId, byte[] sessionPasswd, boolean canBeReadOnly) {
        //客户端实例
        this.garden = garden;
        // 客户端Watcher管理器
        this.watcher = watcher;
        //sessionId
        this.sessionId = sessionId;
        //会话密钥
        this.sessionPasswd = sessionPasswd;
        //会话超时时间
        this.sessionTimeout = sessionTimeout;
        //服务器地址列表管理器
        this.hostProvider = hostProvider;
        //根路径
        this.chrootPath = chrootPath;
        // 连接超时时间是会话超时时间和服务器数量的比值
        connectTimeout = sessionTimeout / hostProvider.size();
        // 读超时时间是会话超时时间的2/3
        readTimeout = sessionTimeout * 2 / 3;

        readOnly = canBeReadOnly;
        //创建发送和事件处理线程
        sendThread = new SendThread(clientCnxnSocket);
        eventThread = new EventThread();

    }


    public static boolean getDisableAutoResetWatch() {
        return disableAutoWatchReset;
    }

    public static void setDisableAutoResetWatch(boolean b) {
        disableAutoWatchReset = b;
    }

    //启动发送和事件处理线程
    public void start() {
        sendThread.start();
        eventThread.start();
    }

    /**
     * This class services the outgoing request queue and generates the heart
     * beats. It also spawns the ReadThread.
     */
    class SendThread extends GardenThread {
        private long lastPingSentNs;
        private final ClientCnxnSocket clientCnxnSocket;
        private Random r = new Random(System.nanoTime());
        private boolean isFirstConnect = true;

        void readResponse(ByteBuffer incomingBuffer) throws IOException {
            ByteBufferInputStream bbis = new ByteBufferInputStream(
                    incomingBuffer);
            BinaryInputArchive bbia = BinaryInputArchive.getArchive(bbis);
            ReplyHeader replyHdr = new ReplyHeader();

            replyHdr.deserialize(bbia, "header");
            if (replyHdr.getXid() == -2) {
                // -2 is the xid for pings
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got ping response for sessionid: 0x"
                            + Long.toHexString(sessionId)
                            + " after "
                            + ((System.nanoTime() - lastPingSentNs) / 1000000)
                            + "ms");
                }
                return;
            }
            if (replyHdr.getXid() == -4) {
                // -4 is the xid for AuthPacket
                if(replyHdr.getErr() == KeeperException.Code.AUTHFAILED.intValue()) {
                    state = States.AUTH_FAILED;
                    eventThread.queueEvent( new WatchedEvent(Watcher.Event.EventType.None,
                            Watcher.Event.KeeperState.AuthFailed, null) );
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got auth sessionid:0x"
                            + Long.toHexString(sessionId));
                }
                return;
            }
            if (replyHdr.getXid() == -1) {
                // -1 means notification
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got notification sessionid:0x"
                            + Long.toHexString(sessionId));
                }
                WatcherEvent event = new WatcherEvent();
                event.deserialize(bbia, "response");

                // convert from a server path to a client path
                if (chrootPath != null) {
                    String serverPath = event.getPath();
                    if(serverPath.compareTo(chrootPath)==0)
                        event.setPath("/");
                    else if (serverPath.length() > chrootPath.length())
                        event.setPath(serverPath.substring(chrootPath.length()));
                    else {
                        LOG.warn("Got server path " + event.getPath()
                                + " which is too short for chroot path "
                                + chrootPath);
                    }
                }

                WatchedEvent we = new WatchedEvent(event);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got " + we + " for sessionid 0x"
                            + Long.toHexString(sessionId));
                }

                eventThread.queueEvent( we );
                return;
            }

            // If SASL authentication is currently in progress, construct and
            // send a response packet immediately, rather than queuing a
            // response as with other packets.
            if (tunnelAuthInProgress()) {
                GetSASLRequest request = new GetSASLRequest();
                request.deserialize(bbia,"token");
                zooKeeperSaslClient.respondToServer(request.getToken(),
                        ClientCnxn.this);
                return;
            }

            Packet packet;
            synchronized (pendingQueue) {
                if (pendingQueue.size() == 0) {
                    throw new IOException("Nothing in the queue, but got "
                            + replyHdr.getXid());
                }
                packet = pendingQueue.remove();
            }
            /*
             * Since requests are processed in order, we better get a response
             * to the first request!
             */
            try {
                if (packet.requestHeader.getXid() != replyHdr.getXid()) {
                    packet.replyHeader.setErr(
                            KeeperException.Code.CONNECTIONLOSS.intValue());
                    throw new IOException("Xid out of order. Got Xid "
                            + replyHdr.getXid() + " with err " +
                            + replyHdr.getErr() +
                            " expected Xid "
                            + packet.requestHeader.getXid()
                            + " for a packet with details: "
                            + packet );
                }

                packet.replyHeader.setXid(replyHdr.getXid());
                packet.replyHeader.setErr(replyHdr.getErr());
                packet.replyHeader.setZxid(replyHdr.getZxid());
                if (replyHdr.getZxid() > 0) {
                    lastZxid = replyHdr.getZxid();
                }
                if (packet.response != null && replyHdr.getErr() == 0) {
                    packet.response.deserialize(bbia, "response");
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading reply sessionid:0x"
                            + Long.toHexString(sessionId) + ", packet:: " + packet);
                }
            } finally {
                finishPacket(packet);
            }
        }

        SendThread(ClientCnxnSocket clientCnxnSocket) {
            super(makeThreadName("-SendThread()"));
            state = States.CONNECTING;
            this.clientCnxnSocket = clientCnxnSocket;
            setDaemon(true);
        }

        // TODO: can not name this method getState since Thread.getState()
        // already exists
        // It would be cleaner to make class SendThread an implementation of
        // Runnable
        /**
         * Used by ClientCnxnSocket
         *
         * @return
         */
        ZooKeeper.States getZkState() {
            return state;
        }

        ClientCnxnSocket getClientCnxnSocket() {
            return clientCnxnSocket;
        }

        /**
         * Setup session, previous watches, authentication.
         */
        void primeConnection() throws IOException {
            LOGGER.info("Socket connection established, initiating session, client: {}, server: {}",
                    clientCnxnSocket.getLocalSocketAddress(),
                    clientCnxnSocket.getRemoteSocketAddress());
            isFirstConnect = false;
            long sessId = (seenRwServerBefore) ? sessionId : 0;
            ConnectRequest conReq = new ConnectRequest(0, lastZxid,
                    sessionTimeout, sessId, sessionPasswd);
            // We add backwards since we are pushing into the front
            // Only send if there's a pending watch
            // TODO: here we have the only remaining use of zooKeeper in
            // this class. It's to be eliminated!
            if (!clientConfig.getBoolean(ZKClientConfig.DISABLE_AUTO_WATCH_RESET)) {
                List<String> dataWatches = zooKeeper.getDataWatches();
                List<String> existWatches = zooKeeper.getExistWatches();
                List<String> childWatches = zooKeeper.getChildWatches();
                if (!dataWatches.isEmpty()
                        || !existWatches.isEmpty() || !childWatches.isEmpty()) {
                    Iterator<String> dataWatchesIter = prependChroot(dataWatches).iterator();
                    Iterator<String> existWatchesIter = prependChroot(existWatches).iterator();
                    Iterator<String> childWatchesIter = prependChroot(childWatches).iterator();
                    long setWatchesLastZxid = lastZxid;

                    while (dataWatchesIter.hasNext()
                            || existWatchesIter.hasNext() || childWatchesIter.hasNext()) {
                        List<String> dataWatchesBatch = new ArrayList<String>();
                        List<String> existWatchesBatch = new ArrayList<String>();
                        List<String> childWatchesBatch = new ArrayList<String>();
                        int batchLength = 0;

                        // Note, we may exceed our max length by a bit when we add the last
                        // watch in the batch. This isn't ideal, but it makes the code simpler.
                        while (batchLength < SET_WATCHES_MAX_LENGTH) {
                            final String watch;
                            if (dataWatchesIter.hasNext()) {
                                watch = dataWatchesIter.next();
                                dataWatchesBatch.add(watch);
                            } else if (existWatchesIter.hasNext()) {
                                watch = existWatchesIter.next();
                                existWatchesBatch.add(watch);
                            } else if (childWatchesIter.hasNext()) {
                                watch = childWatchesIter.next();
                                childWatchesBatch.add(watch);
                            } else {
                                break;
                            }
                            batchLength += watch.length();
                        }

                        SetWatches sw = new SetWatches(setWatchesLastZxid,
                                dataWatchesBatch,
                                existWatchesBatch,
                                childWatchesBatch);
                        RequestHeader header = new RequestHeader(-8, OpCode.setWatches);
                        Packet packet = new Packet(header, new ReplyHeader(), sw, null, null);
                        outgoingQueue.addFirst(packet);
                    }
                }
            }

            for (AuthData id : authInfo) {
                outgoingQueue.addFirst(new Packet(new RequestHeader(-4,
                        OpCode.auth), null, new AuthPacket(0, id.scheme,
                        id.data), null, null));
            }
            outgoingQueue.addFirst(new Packet(null, null, conReq,
                    null, null, readOnly));
            clientCnxnSocket.connectionPrimed();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Session establishment request sent on "
                        + clientCnxnSocket.getRemoteSocketAddress());
            }
        }

        private List<String> prependChroot(List<String> paths) {
            if (chrootPath != null && !paths.isEmpty()) {
                for (int i = 0; i < paths.size(); ++i) {
                    String clientPath = paths.get(i);
                    String serverPath;
                    // handle clientPath = "/"
                    if (clientPath.length() == 1) {
                        serverPath = chrootPath;
                    } else {
                        serverPath = chrootPath + clientPath;
                    }
                    paths.set(i, serverPath);
                }
            }
            return paths;
        }

        private void sendPing() {
            lastPingSentNs = System.nanoTime();
            RequestHeader h = new RequestHeader(-2, OpCode.ping);
            queuePacket(h, null, null, null, null, null, null, null, null);
        }

        private InetSocketAddress rwServerAddress = null;

        private final static int minPingRwTimeout = 100;

        private final static int maxPingRwTimeout = 60000;

        private int pingRwTimeout = minPingRwTimeout;

        // Set to true if and only if constructor of ZooKeeperSaslClient
        // throws a LoginException: see startConnect() below.
        private boolean saslLoginFailed = false;

        private void startConnect(InetSocketAddress addr) throws IOException {
            // initializing it for new connection
            saslLoginFailed = false;
            if(!isFirstConnect){
                try {
                    Thread.sleep(r.nextInt(1000));
                } catch (InterruptedException e) {
                    LOG.warn("Unexpected exception", e);
                }
            }
            state = States.CONNECTING;

            String hostPort = addr.getHostString() + ":" + addr.getPort();
            MDC.put("myid", hostPort);
            setName(getName().replaceAll("\\(.*\\)", "(" + hostPort + ")"));
            if (clientConfig.isSaslClientEnabled()) {
                try {
                    if (zooKeeperSaslClient != null) {
                        zooKeeperSaslClient.shutdown();
                    }
                    zooKeeperSaslClient = new ZooKeeperSaslClient(getServerPrincipal(addr), clientConfig);
                } catch (LoginException e) {
                    // An authentication error occurred when the SASL client tried to initialize:
                    // for Kerberos this means that the client failed to authenticate with the KDC.
                    // This is different from an authentication error that occurs during communication
                    // with the Zookeeper server, which is handled below.
                    LOG.warn("SASL configuration failed: " + e + " Will continue connection to Zookeeper server without "
                            + "SASL authentication, if Zookeeper server allows it.");
                    eventThread.queueEvent(new WatchedEvent(
                            Watcher.Event.EventType.None,
                            Watcher.Event.KeeperState.AuthFailed, null));
                    saslLoginFailed = true;
                }
            }
            logStartConnect(addr);

            clientCnxnSocket.connect(addr);
        }

        private String getServerPrincipal(InetSocketAddress addr) {
            String principalUserName = clientConfig.getProperty(ZKClientConfig.ZK_SASL_CLIENT_USERNAME,
                    ZKClientConfig.ZK_SASL_CLIENT_USERNAME_DEFAULT);
            String serverPrincipal = principalUserName + "/" + addr.getHostString();
            return serverPrincipal;
        }

        private void logStartConnect(InetSocketAddress addr) {
            String msg = "Opening socket connection to server " + addr;
            if (zooKeeperSaslClient != null) {
                msg += ". " + zooKeeperSaslClient.getConfigStatus();
            }
            LOG.info(msg);
        }

        private static final String RETRY_CONN_MSG =
                ", closing socket connection and attempting reconnect";
        @Override
        public void run() {
            clientCnxnSocket.introduce(this, sessionId, outgoingQueue);
            clientCnxnSocket.updateNow();
            clientCnxnSocket.updateLastSendAndHeard();
            int to;
            long lastPingRwServer = Time.currentElapsedTime();
            final int MAX_SEND_PING_INTERVAL = 10000; //10 seconds
            InetSocketAddress serverAddress = null;
            while (state.isAlive()) {
                try {
                    if (!clientCnxnSocket.isConnected()) {
                        // don't re-establish connection if we are closing
                        if (closing) {
                            break;
                        }
                        if (rwServerAddress != null) {
                            serverAddress = rwServerAddress;
                            rwServerAddress = null;
                        } else {
                            serverAddress = hostProvider.next(1000);
                        }
                        startConnect(serverAddress);
                        clientCnxnSocket.updateLastSendAndHeard();
                    }

                    if (state.isConnected()) {
                        // determine whether we need to send an AuthFailed event.
                        if (zooKeeperSaslClient != null) {
                            boolean sendAuthEvent = false;
                            if (zooKeeperSaslClient.getSaslState() == ZooKeeperSaslClient.SaslState.INITIAL) {
                                try {
                                    zooKeeperSaslClient.initialize(ClientCnxn.this);
                                } catch (SaslException e) {
                                    LOG.error("SASL authentication with Zookeeper Quorum member failed: " + e);
                                    state = States.AUTH_FAILED;
                                    sendAuthEvent = true;
                                }
                            }
                            KeeperState authState = zooKeeperSaslClient.getKeeperState();
                            if (authState != null) {
                                if (authState == KeeperState.AuthFailed) {
                                    // An authentication error occurred during authentication with the Zookeeper Server.
                                    state = States.AUTH_FAILED;
                                    sendAuthEvent = true;
                                } else {
                                    if (authState == KeeperState.SaslAuthenticated) {
                                        sendAuthEvent = true;
                                    }
                                }
                            }

                            if (sendAuthEvent == true) {
                                eventThread.queueEvent(new WatchedEvent(
                                        Watcher.Event.EventType.None,
                                        authState,null));
                            }
                        }
                        to = readTimeout - clientCnxnSocket.getIdleRecv();
                    } else {
                        to = connectTimeout - clientCnxnSocket.getIdleRecv();
                    }

                    if (to <= 0) {
                        String warnInfo;
                        warnInfo = "Client session timed out, have not heard from server in "
                                + clientCnxnSocket.getIdleRecv()
                                + "ms"
                                + " for sessionid 0x"
                                + Long.toHexString(sessionId);
                        LOG.warn(warnInfo);
                        throw new SessionTimeoutException(warnInfo);
                    }
                    if (state.isConnected()) {
                        //1000(1 second) is to prevent race condition missing to send the second ping
                        //also make sure not to send too many pings when readTimeout is small
                        int timeToNextPing = readTimeout / 2 - clientCnxnSocket.getIdleSend() -
                                ((clientCnxnSocket.getIdleSend() > 1000) ? 1000 : 0);
                        //send a ping request either time is due or no packet sent out within MAX_SEND_PING_INTERVAL
                        if (timeToNextPing <= 0 || clientCnxnSocket.getIdleSend() > MAX_SEND_PING_INTERVAL) {
                            sendPing();
                            clientCnxnSocket.updateLastSend();
                        } else {
                            if (timeToNextPing < to) {
                                to = timeToNextPing;
                            }
                        }
                    }

                    // If we are in read-only mode, seek for read/write server
                    if (state == States.CONNECTEDREADONLY) {
                        long now = Time.currentElapsedTime();
                        int idlePingRwServer = (int) (now - lastPingRwServer);
                        if (idlePingRwServer >= pingRwTimeout) {
                            lastPingRwServer = now;
                            idlePingRwServer = 0;
                            pingRwTimeout =
                                    Math.min(2*pingRwTimeout, maxPingRwTimeout);
                            pingRwServer();
                        }
                        to = Math.min(to, pingRwTimeout - idlePingRwServer);
                    }

                    clientCnxnSocket.doTransport(to, pendingQueue, ClientCnxn.this);
                } catch (Throwable e) {
                    if (closing) {
                        if (LOG.isDebugEnabled()) {
                            // closing so this is expected
                            LOG.debug("An exception was thrown while closing send thread for session 0x"
                                    + Long.toHexString(getSessionId())
                                    + " : " + e.getMessage());
                        }
                        break;
                    } else {
                        // this is ugly, you have a better way speak up
                        if (e instanceof SessionExpiredException) {
                            LOG.info(e.getMessage() + ", closing socket connection");
                        } else if (e instanceof SessionTimeoutException) {
                            LOG.info(e.getMessage() + RETRY_CONN_MSG);
                        } else if (e instanceof EndOfStreamException) {
                            LOG.info(e.getMessage() + RETRY_CONN_MSG);
                        } else if (e instanceof RWServerFoundException) {
                            LOG.info(e.getMessage());
                        } else if (e instanceof SocketException) {
                            LOG.info("Socket error occurred: {}: {}", serverAddress, e.getMessage());
                        } else {
                            LOG.warn("Session 0x{} for server {}, unexpected error{}",
                                    Long.toHexString(getSessionId()),
                                    serverAddress,
                                    RETRY_CONN_MSG,
                                    e);
                        }
                        // At this point, there might still be new packets appended to outgoingQueue.
                        // they will be handled in next connection or cleared up if closed.
                        cleanup();
                        if (state.isAlive()) {
                            eventThread.queueEvent(new WatchedEvent(
                                    Event.EventType.None,
                                    Event.KeeperState.Disconnected,
                                    null));
                        }
                        clientCnxnSocket.updateNow();
                        clientCnxnSocket.updateLastSendAndHeard();
                    }
                }
            }
            synchronized (state) {
                // When it comes to this point, it guarantees that later queued
                // packet to outgoingQueue will be notified of death.
                cleanup();
            }
            clientCnxnSocket.close();
            if (state.isAlive()) {
                eventThread.queueEvent(new WatchedEvent(Event.EventType.None,
                        Event.KeeperState.Disconnected, null));
            }
            ZooTrace.logTraceMessage(LOG, ZooTrace.getTextTraceLevel(),
                    "SendThread exited loop for session: 0x"
                            + Long.toHexString(getSessionId()));
        }

        private void pingRwServer() throws RWServerFoundException {
            String result = null;
            InetSocketAddress addr = hostProvider.next(0);
            LOGGER.info("Checking server " + addr + " for being r/w." +
                    " Timeout " + pingRwTimeout);

            Socket sock = null;
            BufferedReader br = null;
            try {
                sock = new Socket(addr.getHostString(), addr.getPort());
                sock.setSoLinger(false, -1);
                sock.setSoTimeout(1000);
                sock.setTcpNoDelay(true);
                sock.getOutputStream().write("isro".getBytes());
                sock.getOutputStream().flush();
                sock.shutdownOutput();
                br = new BufferedReader(
                        new InputStreamReader(sock.getInputStream()));
                result = br.readLine();
            } catch (ConnectException e) {
                // ignore, this just means server is not up
            } catch (IOException e) {
                // some unexpected error, warn about it
                LOGGER.warn("Exception while seeking for r/w server " +
                        e.getMessage(), e);
            } finally {
                if (sock != null) {
                    try {
                        sock.close();
                    } catch (IOException e) {
                        LOGGER.warn("Unexpected exception", e);
                    }
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        LOGGER.warn("Unexpected exception", e);
                    }
                }
            }

            if ("rw".equals(result)) {
                pingRwTimeout = minPingRwTimeout;
                // save the found address so that it's used during the next
                // connection attempt
                rwServerAddress = addr;
                throw new RWServerFoundException("Majority server found at "
                        + addr.getHostString() + ":" + addr.getPort());
            }
        }

        private void cleanup() {
            clientCnxnSocket.cleanup();
            synchronized (pendingQueue) {
                for (Packet p : pendingQueue) {
                    conLossPacket(p);
                }
                pendingQueue.clear();
            }
            // We can't call outgoingQueue.clear() here because
            // between iterating and clear up there might be new
            // packets added in queuePacket().
            Iterator<Packet> iter = outgoingQueue.iterator();
            while (iter.hasNext()) {
                Packet p = iter.next();
                conLossPacket(p);
                iter.remove();
            }
        }

        /**
         * Callback invoked by the ClientCnxnSocket once a connection has been
         * established.
         *
         * @param _negotiatedSessionTimeout
         * @param _sessionId
         * @param _sessionPasswd
         * @param isRO
         * @throws IOException
         */
        void onConnected(int _negotiatedSessionTimeout, long _sessionId,
                         byte[] _sessionPasswd, boolean isRO) throws IOException {
            negotiatedSessionTimeout = _negotiatedSessionTimeout;
            if (negotiatedSessionTimeout <= 0) {
                state = States.CLOSED;

                eventThread.queueEvent(new WatchedEvent(
                        Watcher.Event.EventType.None,
                        Watcher.Event.KeeperState.Expired, null));
                eventThread.queueEventOfDeath();

                String warnInfo;
                warnInfo = "Unable to reconnect to ZooKeeper service, session 0x"
                        + Long.toHexString(sessionId) + " has expired";
                LOG.warn(warnInfo);
                throw new SessionExpiredException(warnInfo);
            }
            if (!readOnly && isRO) {
                LOG.error("Read/write client got connected to read-only server");
            }
            readTimeout = negotiatedSessionTimeout * 2 / 3;
            connectTimeout = negotiatedSessionTimeout / hostProvider.size();
            hostProvider.onConnected();
            sessionId = _sessionId;
            sessionPasswd = _sessionPasswd;
            state = (isRO) ?
                    States.CONNECTEDREADONLY : States.CONNECTED;
            seenRwServerBefore |= !isRO;
            LOG.info("Session establishment complete on server "
                    + clientCnxnSocket.getRemoteSocketAddress()
                    + ", sessionid = 0x" + Long.toHexString(sessionId)
                    + ", negotiated timeout = " + negotiatedSessionTimeout
                    + (isRO ? " (READ-ONLY mode)" : ""));
            KeeperState eventState = (isRO) ?
                    KeeperState.ConnectedReadOnly : KeeperState.SyncConnected;
            eventThread.queueEvent(new WatchedEvent(
                    Watcher.Event.EventType.None,
                    eventState, null));
        }

        void close() {
            state = States.CLOSED;
            clientCnxnSocket.onClosing();
        }

        void testableCloseSocket() throws IOException {
            clientCnxnSocket.testableCloseSocket();
        }

        public boolean tunnelAuthInProgress() {
            // 1. SASL client is disabled.
            if (!clientConfig.isSaslClientEnabled()) {
                return false;
            }

            // 2. SASL login failed.
            if (saslLoginFailed == true) {
                return false;
            }

            // 3. SendThread has not created the authenticating object yet,
            // therefore authentication is (at the earliest stage of being) in progress.
            if (zooKeeperSaslClient == null) {
                return true;
            }

            // 4. authenticating object exists, so ask it for its progress.
            return zooKeeperSaslClient.clientTunneledAuthenticationInProgress();
        }

        public void sendPacket(Packet p) throws IOException {
            clientCnxnSocket.sendPacket(p);
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

        public void queueEvent(WatchedEvent event) {
            queueEvent(event, null);
        }

        private void queueEvent(WatchedEvent event,
                                Set<Watcher> materializedWatchers) {
            if (event.getType() == EventType.None
                    && sessionState == event.getState()) {
                return;
            }
            sessionState = event.getState();
            final Set<Watcher> watchers;
            if (materializedWatchers == null) {
                // materialize the watchers based on the event
                watchers = watcher.materialize(event.getState(),
                        event.getType(), event.getPath());
            } else {
                watchers = new HashSet<Watcher>();
                watchers.addAll(materializedWatchers);
            }
            WatcherSetEventPair pair = new WatcherSetEventPair(watchers, event);
            // queue the pair (watch set & event) for later processing
            waitingEvents.add(pair);
        }
    }

    private static String makeThreadName(String suffix) {
        String name = Thread.currentThread().getName().
                replaceAll("-EventThread", "");
        return name + suffix;
    }
}