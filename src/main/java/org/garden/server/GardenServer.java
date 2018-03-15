package org.garden.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.garden.handler.ServerCenterHandler;

/**
 * @author wgt
 * @date 2018-03-10
 * @description 服务端
 **/
public class GardenServer extends Thread {

    /**
     * 默认 9700 端口
     */
    private final static int PORT = 9700;

    private  EventLoopGroup bossGroup = new NioEventLoopGroup();
    private  EventLoopGroup workGroup = new NioEventLoopGroup();

    @Override
    public void run() {
        try {
            startup();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 启动服务
     */
    public  void startup() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new ServerCenterHandler());
        Channel channel = bootstrap.bind(PORT).sync().channel();
        channel.closeFuture().sync();

    }

    /**
     * 关闭资源
     */
    public  void close() {
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
    }

}
