package com.garden.enums;

/**
 * @author wgt
 * @date 2018-03-10
 * @description 事件类型
 **/
public enum EventType { // 事件类型

    /**
     * 无
     */
    None(-1),

    /**
     * 结点创建
     */
    NodeCreated(1),

    /**
     * 结点删除
     */
    NodeDeleted(2),

    /**
     * 结点数据变化
     */
    NodeDataChanged(3),

    /**
     * 结点子节点变化
     */
    NodeChildrenChanged(4);

    /**
     * 代表事件类型的整形
     */
    private final int intValue;

    /**
     * 构造函数
     */
    EventType(int intValue) {
        this.intValue = intValue;
    }

    /**
     * 返回整形
     */
    public int getIntValue() {
        return intValue;
    }

    /**
     * 从整形构造相应的事件
     */
    public static EventType fromInt(int intValue) {
        switch (intValue) {
            case -1:
                return EventType.None;
            case 1:
                return EventType.NodeCreated;
            case 2:
                return EventType.NodeDeleted;
            case 3:
                return EventType.NodeDataChanged;
            case 4:
                return EventType.NodeChildrenChanged;

            default:
                throw new RuntimeException("Invalid integer value for conversion to EventType");
        }
    }
}
