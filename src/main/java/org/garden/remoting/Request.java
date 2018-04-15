package org.garden.remoting;

import com.alibaba.fastjson.JSON;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author wgt
 * @date 2018-04-11
 * @description
 **/
public class Request implements Serializable {

    private static final long serialVersionUID = 7868980981L;

    public static AtomicLong reqIncrement = new AtomicLong(0);

    private Long reqId;

    /**
     * 操作类型,1位
     */
    private byte type;

    /**
     * 数据内容
     */
    private String data;

    public Request() {

    }

    public Request(long reqId) {
        this.reqId = reqId;
    }

    public Long getReqId() {
        return reqId;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public static long getAndIncreaseReq() {
        return reqIncrement.getAndIncrement();
    }

    public void setReqId(Long reqId) {
        this.reqId = reqId;
    }

    public static Request parseCommandToRemotingRequest(String bizReq) {
        return JSON.parseObject(bizReq, Request.class);
    }

    public static Response parseCommandToRemotingResponse(String bizReq) {
        return JSON.parseObject(bizReq, Response.class);
    }

}
