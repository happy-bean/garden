package com.garden.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;

/**
 * @author wgt
 * @date 2018-03-12
 * @description
 **/
public class ServerCenterHandler extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        /*
         * 使用ObjectDecoder和ObjectEncoder
         * 因为双向都有写数据和读数据，所以这里需要两个都设置
         * 如果只读，那么只需要ObjectDecoder即可
         */
        socketChannel.pipeline().addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
        socketChannel.pipeline().addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
        socketChannel.pipeline().addLast("pong", new IdleStateHandler(60, 20, 60 * 10, TimeUnit.SECONDS));
        socketChannel.pipeline().addLast("heart", new HeartChannelHandler());
    }
}
