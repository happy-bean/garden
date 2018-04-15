package org.garden.core.paxos;

import org.garden.core.election.ElectionResponse;

import java.util.List;

/**
 * @author wgt
 * @date 2018-04-06
 * @description 提供paxos存储相关服务
 **/
public interface PaxosStoreInf {

    /**
     * 保存接收到的第二次选举当前成员最大数和值
     */
    ElectionResponse saveAcceptSecondPhaseMaxNumAndValueForCurrentMember(long electionRound, long num, Object value);

    /**
     * 保存接收到的第一次选举当前成员最大数
     */
    ElectionResponse saveAcceptFirstPhaseMaxNumForCurrentMember(long num);

    /**
     * 清空当前成员接收到的二阶段提议值
     *
     * @return
     */
    boolean cleanAcceptSecondPhaseValueForCurrentMember();

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
     * 设置当前选举节点
     *
     * @param paxosMember 选举成员信息
     */
    void setCurrentPaxosMember(PaxosMember paxosMember);

    /**
     * 保存当前成员接受到的最大提议号
     *
     * @param otherMemberList
     */
    void setOtherPaxosMemberList(List<PaxosMember> otherMemberList);

    /**
     * 保存选举结果设置当前状态
     */
    boolean saveElectionResultAndSetStatus(Long electionRound, Long realNum, Object realValue);

    /**
     * 保存第一次选举提议号和选举阶段
     */
    boolean saveFirstPhaseNumAndProposalRoundForProposer(Long electionRound, Long proposalRound, Long proposalNum);

    /**
     * 更新当前节点状态
     */
    boolean updateCurrentMemberStatus(Integer expectStatus, Integer status);

    /**
     * 更新当前leader信息
     */
    boolean updateCurrentMemberLeaderMember(PaxosMember leaderMember);

}

