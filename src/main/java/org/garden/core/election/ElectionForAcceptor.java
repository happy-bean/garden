package org.garden.core.election;

/**
 * @author wgt
 * @date 2018-04-10
 * @description 接收者处理选举类
 **/
public  interface ElectionForAcceptor {

    /**
     * 接收者处理第一阶段请求提议
     *
     * @param electionRequest
     * @return
     */
     BaseElectionResponse processElectionRequestFirstPhase(ElectionRequest electionRequest);

    /**
     * 接收者处理第二阶段请求提议
     *
     * @param electionRequest
     * @return
     */
     BaseElectionResponse processElectionRequestSecondPhase(ElectionRequest electionRequest);

    /**
     * 接受者接受到选举结果的处理
     *
     * @param request
     * @return
     */
     BaseElectionResponse processElectionResultRequest(ElectionResultRequest request);

}
