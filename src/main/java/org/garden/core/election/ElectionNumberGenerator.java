package org.garden.core.election;

import org.javatuples.Pair;

/**
 * @author wgt
 * @date 2018-04-12
 * @description 选举编号
 **/
public class ElectionNumberGenerator {

    public static final String SPLIT = ":";

    public static long getElectionNumberByParam(int clusterMemberNum, int currentMemberUniqueProposalSeq, long currentProposalRound) {
        long num = (currentProposalRound - 1) * clusterMemberNum + currentMemberUniqueProposalSeq;
        return num;
    }

    public static Pair<Long/* currentProposalRound */, Long/* electionNumer */> getElectionNumberByMaxNumber(final int clusterMemberNum,
                                                                                                             final int currentMemberUniqueProposalSeq, final long currentProposalRound, final long paramElectionNumer) {

        long resElectionNumber = getElectionNumberByParam(clusterMemberNum, currentMemberUniqueProposalSeq, currentProposalRound);
        long proposalRound = currentProposalRound;
        while (resElectionNumber < paramElectionNumer) {
            proposalRound += 1;
            resElectionNumber = getElectionNumberByParam(clusterMemberNum, currentMemberUniqueProposalSeq, proposalRound);
        }

        return new Pair<Long, Long>(proposalRound, resElectionNumber);
    }

}
