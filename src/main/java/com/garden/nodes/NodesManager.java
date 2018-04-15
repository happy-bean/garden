package com.garden.nodes;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wgt
 * @date 2018-03-12
 * @description 节点配置
 **/
public class NodesManager {

    private static Node localNode;

    /**
     * 节点集合
     */
    private static Nodes<String, Node> nodeGroup = new Nodes<>();

    /***/
    private static ConcurrentHashMap<String, InetSocketAddress> nodesAddress = new ConcurrentHashMap<>();

    /**
     * 新增节点信息
     */
    public synchronized static Nodes<String, Node> addNode(Node node) {
        nodeGroup.put(node.getInetSocketAddress().getHostString(), node);
        return nodeGroup;
    }

    /**
     * 删除节点信息
     */
    public synchronized static Nodes<String, Node> delNode(Node node) {
        nodeGroup.remove(node.getInetSocketAddress().getHostString(), node);
        return nodeGroup;
    }

    /**
     * 更新节点
     */
    public synchronized static Nodes<String, Node> updNode(Node node) {
        nodeGroup.replace(node.getInetSocketAddress().getHostString(), node);
        return nodeGroup;
    }

    public static Nodes<String, Node> getNodeGroup() {
        return nodeGroup;
    }

    /**
     * 初始化本机节点
     */
    public synchronized static void initLocalNode(Node node) {
        localNode = node;
    }

    /**
     * 获取本机节点
     */
    public static Node getLocalNode() {
        return localNode;
    }

    static class Nodes<K, V> extends ConcurrentHashMap<K, V> {

    }

    static class NodeAddress<K, V> extends ConcurrentHashMap<K, V> {

    }
}
