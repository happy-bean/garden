package org.garden.balance.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.garden.remoting.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    LoadBalance loadBalance = new LoadBalance();
    /**
     * 服务端处理客户端http请求的核心方法
     *
     * @param ctx
     * @param msg
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

        handHttpRequest(ctx, msg);
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
            diapathcer(ctx, msg);
        }
    }

    /**
     * 路由分发
     */
    private void diapathcer(ChannelHandlerContext ctx, Object msg) throws UnsupportedEncodingException {

        FullHttpRequest request = (FullHttpRequest) msg;

        System.out.println("uri=" + request.uri());
        System.out.println("method=" + request.method());
        System.out.println("protocolVersion=" + request.protocolVersion());

        System.out.println(request.headers().iterator().next().getKey());


        try {
            int hashCode = "garden".hashCode();
            String basePath = "http://" + loadBalance.getServer(hashCode);
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

}
