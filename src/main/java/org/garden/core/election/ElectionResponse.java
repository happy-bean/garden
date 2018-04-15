package org.garden.core.election;

import org.garden.remoting.ResponseFuture;

/**
 * @author wgt
 * @date 2018-03-26
 * @description 对于选举请求的响应
 **/
public class ElectionResponse extends BaseElectionResponse {

    private static final long serialVersionUID = 5646576891L;

    /**
     * 响应code
     * 请求code约定:
     * accept表示接受该一阶段请求(值需要提议者根据相应规则决定)，deny表示拒绝该提议
     */
    private String code;

    /**
     * 该接收者已经接收到的最大提议号
     */
    private Long maxProposalNum;

    /**
     * 该接收者已经accept第二阶段的最大提议号
     */
    private Long maxAcceptNum;

    /**
     * 该接收者已经accept第二阶段最大提议号对应的value
     */
    private Object maxAcceptValue;

    private Long realNum;

    private Object realValue;

    private Long electionRound;

    /**
     * 异步结果
     */
    private ResponseFuture responseFuture;

    public ElectionResponse() {

    }

    public ElectionResponse(String code, Long maxProposalNum, Long maxAcceptNum, Object maxAcceptValue) {
        this.code = code;
        this.maxProposalNum = maxProposalNum;
        this.maxAcceptNum = maxAcceptNum;
        this.maxAcceptValue = maxAcceptValue;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getMaxProposalNum() {
        return maxProposalNum;
    }

    public void setMaxProposalNum(Long maxProposalNum) {
        this.maxProposalNum = maxProposalNum;
    }

    public Long getMaxAcceptNum() {
        return maxAcceptNum;
    }

    public void setMaxAcceptNum(Long maxAcceptNum) {
        this.maxAcceptNum = maxAcceptNum;
    }

    public Object getMaxAcceptValue() {
        return maxAcceptValue;
    }

    public void setMaxAcceptValue(Object maxAcceptValue) {
        this.maxAcceptValue = maxAcceptValue;
    }

    public Long getRealNum() {
        return realNum;
    }

    public void setRealNum(Long realNum) {
        this.realNum = realNum;
    }

    public Object getRealValue() {
        return realValue;
    }

    public void setRealValue(Object realValue) {
        this.realValue = realValue;
    }

    public Long getElectionRound() {
        return electionRound;
    }

    public void setElectionRound(Long electionRound) {
        this.electionRound = electionRound;
    }

    public ResponseFuture getResponseFuture() {
        return responseFuture;
    }

    public void setResponseFuture(ResponseFuture responseFuture) {
        this.responseFuture = responseFuture;
    }

}
