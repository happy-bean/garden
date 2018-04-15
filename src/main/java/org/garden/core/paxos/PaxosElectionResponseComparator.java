package org.garden.core.paxos;

import org.garden.core.election.ElectionResponse;

import java.util.Comparator;

/**
 * @author wgt
 * @date 2018-04-12
 * @description
 **/
public class PaxosElectionResponseComparator implements Comparator<ElectionResponse> {

    @Override
    public int compare(ElectionResponse m1, ElectionResponse m2) {
        if (m1.getElectionRound() > m2.getElectionRound()) {
            return 1;
        } else if (m1.getElectionRound() < m2.getElectionRound()) {
            return -1;
        }

        if (m1.getMaxAcceptNum() > m2.getMaxAcceptNum()) {
            return 1;
        } else if (m1.getMaxAcceptNum() < m2.getMaxAcceptNum()) {
            return -1;
        } else {
            return 0;
        }
    }

}
