package org.garden.core.election;

import org.javatuples.Pair;

/**
 * @author wgt
 * @date 2018-04-06
 * @description 提议者处理选举算法服务类
 **/
public interface ElectionForProposer {

    /**
     * 一阶段选举 pair 元组
     *
     * @param round
     * @param num
     *            提议号
     * @param value
     *            提议值
     * @return 返回结果，代表是否可以进行二阶段，如果可以的话提议值是多少
     */
     Pair<Boolean/* 是否可以进行下个阶段选举 */, Object/* 可以进行下阶段选举的提议值 */> proposalFirstPhase(long round, long num, Object value);

    /**
     * 二阶段选举
     *
     * @param round
     * @param num
     *            提议号
     * @param value
     *            提议值
     * @return 返回结果，如果大于一半都接受的话，之后需要进行广播此次选举的值
     */
     boolean proposalSecondPhase(long round, long num, Object value);

    /**
     * 处理二阶段选举完成后，广播选举结果
     *
     * @param electionSuccessFlag
     * @param electionRound 当前选举轮次
     * @param realNum
     *            促成选举结果的提议号
     * @param realValue
     *            选举结果
     * @return
     */
     boolean processAfterElectionSecondPhase(boolean electionSuccessFlag, Long electionRound, Long realNum, Object realValue,
                                                   boolean shouldSendResultToOther);

}
