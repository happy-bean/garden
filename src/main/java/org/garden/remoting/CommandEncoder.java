package org.garden.remoting;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.garden.core.constants.CodeInfo;

import java.nio.charset.Charset;

/**
 * @author wgt
 * @date 2018-04-11
 * @description 通信指令编码器
 **/
public class CommandEncoder extends MessageToByteEncoder<Command> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Command msg, ByteBuf out) throws Exception {
        if (null == msg) {
            throw new Exception("msg is null");
        }

        String body = msg.getBody();
        byte[] bodyBytes = body.getBytes(Charset.forName("utf-8"));

        // 版本号
        out.writeByte(CodeInfo.VERSION);

        // 通信类型,请求或响应
        out.writeByte(msg.getCommandType());

        // 消息体长度
        out.writeInt(bodyBytes.length);

        // 消息体
        out.writeBytes(bodyBytes);
    }

}
