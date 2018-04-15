package com.garden.enums;

/**
 * @author wgt
 * @date 2018-03-10
 * @description 事件发生时Zookeeper的状态
 **/
public enum KeeperState {

    /**
     * 未知状态，不再使用，服务器不会产生此状态
     */
    Unknown(-1),

    /**
     * 断开
     */
    Disconnected(0),

    /**
     * 未同步连接，不再使用，服务器不会产生此状态
     */
    NoSyncConnected(1),

    /**
     * 同步连接状态
     */
    SyncConnected(3),

    /**
     * 认证失败状态
     */
    AuthFailed(4),

    /**
     * 只读连接状态
     */
    ConnectedReadOnly(5),

    /**
     * SASL认证通过状态
     */
    SaslAuthenticated(6),

    /**
     * 过期状态
     */
    Expired(-112);

    /**
     * 代表状态的整形值
     */
    private final int intValue;

    /**
     * 构造函数
     */
    KeeperState(int intValue) {
        this.intValue = intValue;
    }

    /**
     * 返回整形值
     */
    public int getIntValue() {
        return intValue;
    }

    /**
     * 从整形值构造相应的状态
     */
    public static KeeperState fromInt(int intValue) {
        switch (intValue) {
            case -1:
                return KeeperState.Unknown;
            case 0:
                return KeeperState.Disconnected;
            case 1:
                return KeeperState.NoSyncConnected;
            case 3:
                return KeeperState.SyncConnected;
            case 4:
                return KeeperState.AuthFailed;
            case 5:
                return KeeperState.ConnectedReadOnly;
            case 6:
                return KeeperState.SaslAuthenticated;
            case -112:
                return KeeperState.Expired;
            default:
                throw new RuntimeException("Invalid integer value for conversion to KeeperState");
        }
    }
}
