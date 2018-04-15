package org.garden.remoting;

import java.io.Serializable;

/**
 * @author wgt
 * @date 2018-04-11
 * @description 操作请求的响应
 **/
public class Response implements Serializable {

    private static final long serialVersionUID = 167831268098L;

    /**
     * 响应id=8字
     */
    private Long reqId;

    /**
     * 响应类型 CodeInfo
     * 1字节
     */
    private byte type;

    /**
     * 响应结果
     */
    private String data;

    public Long getReqId() {
        return reqId;
    }

    public void setReqId(Long reqId) {
        this.reqId = reqId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

}

