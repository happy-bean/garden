package org.garden.core.paxos;

import java.util.Comparator;

/**
 * @author wgt
 * @date 2018-04-12
 * @description
 **/
public class PaxosMemberStatusComparator implements Comparator<PaxosMember> {

    @Override
    public int compare(PaxosMember o1, PaxosMember o2) {
        if (o1.getIsUp()) {
            return -1;
        } else if (!o1.getIsUp()) {
            return 1;
        } else {
            return 0;
        }

    }

}
