package org.garden.nodes;

/**
 * @author wgt
 * @date 2018-03-13
 * @description 节点信息
 **/
public class Node {

    /**
     * 节点id
     * */
    private long nodeId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点地址
     */
    private String hosts;

    /**
     * 节点健康状态
     */
    private String health;

    /**
     * 身份  leader slave
     */
    private String identity;

    /**
     * 上次更新时间
     */
    private long time;

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

    public String getHosts() {
        return hosts;
    }

    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    public String getHealth() {
        return health;
    }

    public void setHealth(String health) {
        this.health = health;
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
}