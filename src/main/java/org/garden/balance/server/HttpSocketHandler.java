package org.garden.balance.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import static com.sun.deploy.net.HttpRequest.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author wgt
 * @date 2018-03-29
 * @description 处理http请求
 **/
//特别注意这个注解@Sharable，默认的4版本不能自动导入匹配的包，需要手动加入
//地址是import io.netty.channel.ChannelHandler.Sharable;
@ChannelHandler.Sharable
public class HttpSocketHandler extends SimpleChannelInboundHandler<Object> {

    public static Logger LOGGER = LoggerFactory.getLogger(HttpSocketHandler.class);


    private WebSocketServerHandshaker handshaker;

    private static final String WEB_SOCKET_URL = "ws://localhost:9030/websocket";


    LoadBalance loadBalance = new LoadBalance();

    /**
     * 服务端处理客户端http请求的核心方法
     *
     * @param ctx
     * @param msg
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

        //处理客户端向服务端发起http请求的业务
        if (msg instanceof FullHttpMessage) {
            handHttpRequest(ctx, msg);
        }

        //处理websocket连接业务
        else if (msg instanceof WebSocketFrame) {
            handWebsocketFrame(ctx, (WebSocketFrame) msg);
        }
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 处理客户端向服务端发起的http请求的业务
     *
     * @param ctx
     * @param msg
     */
    private void handHttpRequest(ChannelHandlerContext ctx, Object msg) throws IOException {

        //判断是否是http请求
        if (!(msg instanceof FullHttpRequest)) {

            String content = "only support http request";
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                    OK, Unpooled.wrappedBuffer(content.getBytes()));

            response.headers().set(CONTENT_TYPE, "text/plain");
            response.headers().set(CONTENT_LENGTH,
                    response.content().readableBytes());

            ctx.writeAndFlush(response);
        } else {
            FullHttpRequest request = (FullHttpRequest) msg;

            if (request.decoderResult().isSuccess() && ("websocket".equals(request.headers().get("Upgrade")))) {
                //工厂类对象
                WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL, null, false);
                handshaker = factory.newHandshaker((FullHttpRequest) msg);
                if (handshaker == null) {
                    WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                } else {
                    handshaker.handshake(ctx.channel(), (FullHttpRequest) msg);
                }
                return;
            }

            diapathcer(ctx, msg);
        }
    }

    /**
     * 路由分发
     */
    private void diapathcer(ChannelHandlerContext ctx, Object msg) throws UnsupportedEncodingException {

        FullHttpRequest request = (FullHttpRequest) msg;
        try {
            int hashCode = "garden".hashCode();
            String ipAndPort = loadBalance.getServer(hashCode);
            NodeHealth.addConcurrent(ipAndPort.split(":")[0]);
            String basePath = "http://" + ipAndPort;
            String urlString = basePath + request.uri();
            URL url = new URL(urlString);
            LOGGER.info("fetching >" + url.toString());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            String methodName = request.method().name();
            LOGGER.info("method>" + methodName);
            con.setRequestMethod(methodName);
            con.setDoOutput(true);
            con.setDoInput(true);
            HttpURLConnection.setFollowRedirects(false);
            con.setUseCaches(true);
            Iterator<Map.Entry<String, String>> iterator = request.headers().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                LOGGER.info("headers>" + entry.getKey() + ":" + entry.getValue());
                con.setRequestProperty(entry.getKey(), entry.getValue());
            }
            con.connect();

            if (HttpMethod.GET == request.method()) {

                if (msg instanceof HttpContent) {
                    HttpContent content = (HttpContent) msg;
                    ByteBuf buf = content.content();
                    String reqStr = buf.toString(io.netty.util.CharsetUtil.UTF_8);
                    BufferedOutputStream proxyToWebBuf = new BufferedOutputStream(con.getOutputStream());
                    proxyToWebBuf.write(reqStr.getBytes());
                    proxyToWebBuf.flush();
                    proxyToWebBuf.close();
                }

            }

            int statusCode = con.getResponseCode();
            BufferedInputStream webToProxyBuf = new BufferedInputStream(con.getInputStream());

            ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
            //buff用于存放循环读取的临时数据
            byte[] buff = new byte[100];
            int rc = 0;
            while ((rc = webToProxyBuf.read(buff, 0, 100)) > 0) {
                swapStream.write(buff, 0, rc);
            }

            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                    OK, Unpooled.wrappedBuffer(swapStream.toByteArray()));
            response.setStatus(HttpResponseStatus.valueOf(statusCode));


            for (Iterator<Map.Entry<String, List<String>>> i = con.getHeaderFields().entrySet().iterator(); i
                    .hasNext(); ) {
                Map.Entry<String, List<String>> mapEntry = i.next();
                if (mapEntry.getKey() != null) {
                    response.headers().set(mapEntry.getKey().toString(), (mapEntry.getValue()).get(0).toString());
                }
            }

            ctx.writeAndFlush(response);

        } catch (Exception e) {
            LOGGER.error("diapathcer error:", e);
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
            LOGGER.info("不支持二进制消息");
            return;
        }

        //获取客户端向服务端发送的消息
        String request = ((TextWebSocketFrame) frame).text();
        LOGGER.info(request);

        //返回应答消息
        //获取客户端向服务端发送的消息
        Map<String, Object> map = new HashMap<>();
        map.put("times", NodeHealth.getTimeList());

        Map<String, LinkedHashMap<String, Integer>> mapMap = NodeHealth.getConcurrentHashMap();

        map.put("conmap", NodeHealth.getConcurrentHashMap());
        String respStr = JSON.toJSONString(map, SerializerFeature.DisableCircularReferenceDetect);
        TextWebSocketFrame webSocketFrame = new TextWebSocketFrame(respStr);
        //返回当前客户端
        ctx.writeAndFlush(webSocketFrame);
    }


}
