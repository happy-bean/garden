package com.garden.conn;

import com.garden.nodes.Node;

import java.io.Serializable;

/**
 * @author wgt
 * @date 2018-03-20
 * @description 允许我们传递标题和相关记录
 **/
public class Packet implements Serializable{

    /**
     * 请求头
     */
    private RequestHeader requestHeader;

    /**
     * 响应头
     */
    private ResponseHeader responseHeader;

    /**
     * 节点信息
     */
    private Node node;

    private Byte[] contents;

    public RequestHeader getRequestHeader() {
        return requestHeader;
    }

    public void setRequestHeader(RequestHeader requestHeader) {
        this.requestHeader = requestHeader;
    }

    public ResponseHeader getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(ResponseHeader responseHeader) {
        this.responseHeader = responseHeader;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Byte[] getContents() {
        return contents;
    }

    public void setContents(Byte[] contents) {
        this.contents = contents;
    }
}
