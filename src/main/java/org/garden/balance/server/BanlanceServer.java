package org.garden.balance.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

/**
 * @author wgt
 * @date 2018-04-18
 * @description 负载均衡服务
 **/
public class BanlanceServer implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BanlanceServer.class);

    private static final int PORT = 8888;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;

    //程序初始方法入口注解，提示spring这个程序先执行这里
    @PostConstruct
    public void startServer() {
        bossGroup = new NioEventLoopGroup();
        workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new ChannelHandler());

            LOGGER.info("banlance server start ...");
            Channel channel = bootstrap.bind(PORT).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            LOGGER.error("banlance server start failed !", e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    @Override
    public void close() throws Exception {
        LOGGER.info("applaction shutdown");
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workGroup != null) {
            workGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new BanlanceServer().startServer();
    }
}

