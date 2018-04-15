package org.garden.remoting;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author wgt
 * @date 2018-04-11
 * @description 通信指令解码器
 **/
public class CommandDecoder extends LengthFieldBasedFrameDecoder {

    public CommandDecoder(int maxFrameLength, int lengthFieldOffset,
                                  int lengthFieldLength, int lengthAdjustment,
                                  int initialBytesToStrip, boolean failFast) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength,
                lengthAdjustment, initialBytesToStrip, failFast);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in)
            throws Exception {
        if (in == null) {
            return null;
        }

        byte version = in.readByte();
        byte type = in.readByte();
        int length = in.readInt();
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return null;
        }
        ByteBuf buf = in.readBytes(length);
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String body = new String(req, "UTF-8");

        Command command = new Command();
        command.setVersion(version);
        command.setCommandType(type);
        command.setBody(body);
        return command;
    }
}
