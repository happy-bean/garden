package org.garden.core.paxos;

import java.util.Comparator;

/**
 * @author wgt
 * @date 2018-04-16
 * @description
 **/
public class PaxosMemberComparator implements Comparator<PaxosMember> {

    @Override
    public int compare(PaxosMember m1, PaxosMember m2) {
        int wM1 = m1.getWeights();
        int wM2 = m1.getWeights();

        if (wM1 > wM2) {
            return 1;
        } else if (wM1 < wM2) {
            return -1;
        } else {
            return 0;
        }
    }

}