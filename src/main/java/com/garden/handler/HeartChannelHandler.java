
package com.garden.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;
import com.garden.conf.GardenChannelManager;

/**
 * @author wgt
 * @date 2018-03-10
 * @description 心跳检测
 **/
public class HeartChannelHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger LOGGER = Logger.getLogger(HeartChannelHandler.class);

    /**
     * 客户端与服务器创建连接是调用
     *
     * @param ctx
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        GardenChannelManager.channelGroup.add(ctx.channel());
        LOGGER.info("客户端与服务端连接开启...");
    }

    /**
     * 客户端与服务端断开连接时调用
     *
     * @param ctx
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        GardenChannelManager.channelGroup.remove(ctx.channel());
        LOGGER.info("客户端与服务端连接关闭");
    }

    /**
     * 服务端接收客户端发送过来的数据结束后调用
     *
     * @param ctx
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 工程出现异常时调用
     *
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 服务端处理客户端websocket请求的核心方法
     *
     * @param ctx
     * @param msg
     */
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        LOGGER.info("来自客户端消息" + msg.toString());
        System.out.println(msg.toString());
    }

    /**
     * 处理心跳业务
     */
    private void handPingsocketFrame(ChannelHandlerContext ctx, PingWebSocketFrame frame) {
        //判断是否是ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
    }

    /**
     * 服务端向客户端响应消息
     *
     * @param ctx
     * @param request
     * @param response
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request, DefaultFullHttpResponse response) {
        if (response.getStatus().code() != 200) {
            ByteBuf byteBuf = Unpooled.copiedBuffer(response.getStatus().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(byteBuf);
            byteBuf.release();
        }
        //服务端向客户端发送数据
        ChannelFuture future = ctx.channel().writeAndFlush(response);
        if (response.getStatus().code() != 200) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {

    }
}


