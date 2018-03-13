package org.garden.conf;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @author wgt
 * @date 2018-03-07
 * @description 服务器注册配置类
 **/
public class GardenChannelManager {

    /**
     * 存储每一个客户端接入进来的channel
     */
    public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

}
