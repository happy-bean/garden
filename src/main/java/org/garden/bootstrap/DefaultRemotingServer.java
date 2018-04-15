package org.garden.bootstrap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.log4j.Logger;
import org.garden.core.constants.CodeInfo;
import org.garden.handler.UpStreamHandler;
import org.garden.remoting.CommandDecoder;
import org.garden.remoting.CommandEncoder;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author wgt
 * @date 2018-04-11
 * @description 默认远程服务
 **/
public class DefaultRemotingServer implements RemotingServer {

    private static final Logger LOGGER = Logger.getLogger(DefaultRemotingServer.class);

    private UpStreamHandler upStreamHandler;

    private String ip;

    private int port;

    NioEventLoopGroup boss = new NioEventLoopGroup();
    NioEventLoopGroup worker = new NioEventLoopGroup();

    public DefaultRemotingServer(UpStreamHandler upStreamHandler) {
        this.upStreamHandler = upStreamHandler;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void start(String ip, int port) {
        this.ip = ip;
        this.port = port;

        ServerBootstrap serverBootstrap = new ServerBootstrap();


        serverBootstrap.group(boss, worker);
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 1000);
        serverBootstrap.channel(NioServerSocketChannel.class);

        /**
         * 监听的ip地址及端口
         */
        SocketAddress socketAddress = new InetSocketAddress(ip, port);
        serverBootstrap.localAddress(socketAddress);

        /**
         * 设置解码器及业务处理器
         */
        serverBootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new CommandEncoder());
                ch.pipeline().addLast(new CommandDecoder(CodeInfo.MAX_MSG_SIZE, 2, 4, 0, 0, false));
                ch.pipeline().addLast(new UpStreamHandler(upStreamHandler.getPaxosCoreComponent()));
            }
        });

        try {
            ChannelFuture channelFuture = serverBootstrap.bind().sync();
            LOGGER.info("start remoting server succFlag:" + channelFuture.isSuccess());

        } catch (InterruptedException e) {
            LOGGER.error("start remoting server err,ip[" + ip + "],port[" + port + "]", e);
        }

    }

    @Override
    public void stop() {
       boss.shutdownGracefully();
       worker.shutdownGracefully();
    }
}
