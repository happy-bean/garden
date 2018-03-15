package org.garden.conf;

import org.garden.nodes.Node;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wgt
 * @date 2018-03-12
 * @description 节点配置
 **/
public class NodesManager {

    /**
     * 节点集合
     */
    private static Nodes<Long, Node> nodeGroup = new Nodes<>();

    /**
     * 新增节点信息
     */
    public static Nodes<Long, Node> addNode(Node node) {
        nodeGroup.put(node.getNodeId(), node);
        return nodeGroup;
    }

    /**
     * 删除节点信息
     */
    public static Map<Long, Node> delNode(Node node) {
        nodeGroup.remove(node.getNodeId(), node);
        return nodeGroup;
    }

    /**
     * 更新节点
     */
    public static Nodes<Long, Node> updNode(Node node) {
        nodeGroup.replace(node.getNodeId(), node);
        return nodeGroup;
    }

    public static Nodes<Long, Node> getNodeGroup() {
        return nodeGroup;
    }

    static class Nodes<K, V> extends ConcurrentHashMap<K, V> {

    }
}
