package org.garden.conf;

import org.garden.nodes.Node;

import java.util.*;

/**
 * @author wgt
 * @date 2018-03-12
 * @description 节点配置
 **/
public class NodesManager {


    private static Nodes<Long, Node> nodeGroup = new Hashtable<Long,Node>();


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
    public static Nodes<Long, Node> delNode() {

        return nodeGroup;
    }


    static abstract class Nodes<K, V> implements Map<K, V> {

    }
}
