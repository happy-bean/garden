package org.garden.util;

import org.garden.core.paxos.PaxosMember;
import org.garden.enums.PaxosMemberRole;
import org.garden.enums.PaxosMemberStatus;

import java.util.List;

/**
 * @author wgt
 * @date 2018-04-07
 * @description 选举工具类
 **/
public class ElectionUtil {

    public static void fillLeaderToMemberList(PaxosMember currentMember, List<PaxosMember> otherMemberList,
                                              String electionRes) {
        String currentIpAndPort = currentMember.getIpAndPort();
        if (currentIpAndPort.equals(electionRes)) {
            currentMember.setRole(PaxosMemberRole.LEADER);
            currentMember.setLeaderMember(currentMember);
        }

        /**
         * 选举结果leader是另外一个结点，则找到这个member设置为leaderMember
         */
        for (PaxosMember paxosMember : otherMemberList) {
            if (paxosMember.getIpAndPort().equals(electionRes)) {
                paxosMember.setRole(PaxosMemberRole.LEADER);
                paxosMember.setStatusValue(PaxosMemberStatus.NORMAL.getStatus());
                currentMember.setLeaderMember(paxosMember);
                break;
            }
        }
    }
}
