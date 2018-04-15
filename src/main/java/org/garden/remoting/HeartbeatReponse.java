package org.garden.remoting;

/**
 * @author wgt
 * @date 2018-04-11
 * @description 心跳响应
 **/
public class HeartbeatReponse {
    /**
     * 心跳返回状态
     */
    private byte status;

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }
}
