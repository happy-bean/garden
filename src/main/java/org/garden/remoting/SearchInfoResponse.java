package org.garden.remoting;

import java.io.Serializable;

/**
 * @author wgt
 * @date 2018-04-11
 * @description 查找信息响应
 **/
public class SearchInfoResponse implements Serializable {

    private static final long serialVersionUID = 4467579801L;

    String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
