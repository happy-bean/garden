package org.garden.core.paxos;

import org.garden.core.election.BaseElectionResponse;
import org.garden.core.election.ElectionInfo;
import org.garden.core.election.ElectionResponse;

import java.util.List;

/**
 * @author wgt
 * @date 2018-03-26
 * @description paxos核心
 **/
public interface PaxosCore {

    /**
     * 获取当前成员
     *
     * @return
     */
    PaxosMember getCurrentPaxosMember();

    /**
     * 获取除当前成员外集群其它成员
     *
     * @return
     */
    List<PaxosMember> getOtherPaxosMemberList();

    /**
     * 设置最近选举成员
     */
    void setCurrentPaxosMember(PaxosMember paxosMember);

    /**
     * 保存当前成员接受到的最大提议号
     *
     * @param otherMemberList
     */
    void setOtherPaxosMemberList(List<PaxosMember> otherMemberList);

    /**
     * 保存第一次接收到的最大提议号
     * */
    ElectionResponse saveAcceptFirstPhaseMaxNumForCurrentMember(long num);

    /**
     * 保存第二次接收到的最大提议号
     * */
    ElectionResponse saveAcceptSecondPhaseMaxNumAndValueForCurrentMember(long round, long num, Object value);

    /**
     * 获取选举结果
     * */
    ElectionInfo getElectionInfoForCurrentMember();

    /**
     * 处理二阶段选举完成后，广播选举结果
     *
     * @param electionSuccessFlag
     * @param electionRound       当前选举轮次
     * @param realNum             促成选举结果的提议号
     * @param realValue           选举结果
     * @return
     */
    boolean processAfterElectionSecondPhase(boolean electionSuccessFlag, Long electionRound, Long realNum, Object realValue,
                                            boolean shouldSendResultToOther);

    /**
     * 处理选举请求含接收一阶段请求,接收阶二请求及接收广播选举结果，内部会根据业务类型type进行分发s
     *
     * @param type CodeInfo.REQ_TYPE_ELECTION_FIRST_PHASE, //一阶段请求type
     *             CodeInfo.REQ_TYPE_ELECTION_SECOND_PHASE,//二阶段请求type
     *             REQ_TYPE_ELECTION_RESULT_TO_LEANER,//广播选举结果给学习者请求type
     */
    BaseElectionResponse processElectionRequestForAcceptor(byte type, String electionRequestData);

}
