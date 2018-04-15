package org.garden.core.election;

/**
 * @author wgt
 * @date 2018-04-11
 * @description 选举结果响应
 **/
public class ElectionResultResponse extends BaseElectionResponse {

    private static final long serialVersionUID = 8978972187081L;

    /**
     * 状态码,-1-失败,1-成功
     */
    private Integer code;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
