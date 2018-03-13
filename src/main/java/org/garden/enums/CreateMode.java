package org.garden.enums;

/**
 * @author wgt
 * @date 2018-03-09
 * @description 四种节点类型
 **/
public enum CreateMode {

    /**
     * 永久节点
     */
    PERSISTENT(0, false, false),

    /**
     * 临时节点
     */
    PERSISTENT_SEQUENTIAL(2, false, true),

    /**
     * 永久节点、序列化
     */
    EPHEMERAL(1, true, false),

    /**
     * 临时节点、序列化
     */
    EPHEMERAL_SEQUENTIAL(3, true, true);

    CreateMode(int i, boolean b, boolean b1) {
    }
}

