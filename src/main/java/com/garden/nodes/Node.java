package com.garden.nodes;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * @author wgt
 * @date 2018-03-13
 * @description 节点信息
 **/
public class Node implements Serializable{

    /**
     * 节点id
     */
    private long nodeId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点健康状态
     */
    private String health;

    /**
     * 身份  leader slave
     */
    private String identity;


    private InetSocketAddress inetSocketAddress;

    /**
     * 上次更新时间
     */
    private long time;

    public static enum Health {

        GREEN("green"),
        YELLOW("yellow"),
        RED("red");

        private String status;

        private Health(String status) {

            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }


    public static class dentity {
        static public final String UNKNOWN_STATE = "unknown";
        static public final String LOOKING_STATE = "leaderelection";
        static public final String LEADING_STATE = "leading";
        static public final String FOLLOWING_STATE = "following";
        static public final String OBSERVING_STATE = "observing";
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHealth() {
        return health;
    }

    public void setHealth(Health health) {
        this.health = health.getStatus();
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setHealth(String health) {
        this.health = health;
    }

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }

    public void setInetSocketAddress(InetSocketAddress inetSocketAddress) {
        this.inetSocketAddress = inetSocketAddress;
    }
}