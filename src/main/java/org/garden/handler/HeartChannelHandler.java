
package org.garden.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.garden.conf.GardenChannelManager;

import java.util.Date;

/**
 * @author wgt
 * @date 2018-03-10
 * @description 心跳检测
 **/
public class HeartChannelHandler extends SimpleChannelInboundHandler<Object> {

    /**
     * 客户端与服务器创建连接是调用
     *
     * @param ctx
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        GardenChannelManager.channelGroup.add(ctx.channel());
        System.out.println("客户端与服务端连接开启...");
    }

    /**
     * 客户端与服务端断开连接时调用
     *
     * @param ctx
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        GardenChannelManager.channelGroup.remove(ctx.channel());
        System.out.println("客户端与服务端连接关闭");
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
    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        //处理客户端向服务端发起http握手请求的业务
        if (msg instanceof FullHttpMessage) {
            handHttpRequest(ctx, (FullHttpRequest) msg);
        }

        //处理websocket连接业务
        else if (msg instanceof WebSocketFrame) {
            handWebsocketFrame(ctx, (WebSocketFrame) msg);
        }
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
     * 处理客户端与服务端之间websocket业务
     *
     * @param ctx
     * @param frame
     */
    private void handWebsocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        //判断是否是关闭websocket的指令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
        }

        //判断是否是ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        //判断是否是二进制消息，如果是，抛出异常
        if (!(frame instanceof TextWebSocketFrame)) {
            System.out.println("不支持二进制消息");
            throw new RuntimeException(this.getClass().getName() + "不支持消息");
        }

        //返回应答消息
        //获取客户端向服务端发送的消息
        String request = ((TextWebSocketFrame) frame).text();
        System.out.println("服务端收到客户端的消息:" + request);
        TextWebSocketFrame webSocketFrame = new TextWebSocketFrame(new Date().toString() + ctx.channel().id() + "==>>" + request);
        //群发，服务端向每个客户端发消息
        GardenChannelManager.channelGroup.writeAndFlush(webSocketFrame);
    }

    /**
     * 处理客户端向服务端发起的http握手请求的业务
     *
     * @param ctx
     * @param request
     */
    private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.getDecoderResult().isSuccess() || !("websocket".equals(request.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        //工厂类对象
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL, null, false);
        handshaker = factory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), request);
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
}


