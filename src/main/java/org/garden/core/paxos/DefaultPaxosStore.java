package org.garden.core.paxos;

import org.apache.log4j.Logger;
import org.garden.core.constants.CodeInfo;
import org.garden.core.data.DataStoreInf;
import org.garden.core.data.FileDataStore;
import org.garden.core.election.ElectionInfo;
import org.garden.core.election.ElectionResponse;
import org.garden.enums.PaxosMemberStatus;
import org.garden.util.BizSerialAndDeSerialUtil;
import org.garden.util.ElectionUtil;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author wgt
 * @date 2018-04-06
 * @description 提供paxos存储相关服务
 **/
public class DefaultPaxosStore implements PaxosStoreInf {

    private static final Logger LOGGER = Logger.getLogger(DefaultPaxosStore.class);

    //选举成员信息
    private static PaxosMember currentMember;

    //选举成员信息集合
    private static List<PaxosMember> otherMemberList;

    private Lock lock = new ReentrantLock();

    private DataStoreInf dataStore = new FileDataStore();

    //保存接收到的第一次选举当前成员最大数
    @Override
    public ElectionResponse saveAcceptFirstPhaseMaxNumForCurrentMember(long num) {
        lock.lock();
        try {
            ElectionInfo electionInfo = currentMember.getElectionInfo();
            if (electionInfo == null) {
                throw new IllegalArgumentException("当前成员electionInfo不能为空");
            }

            //最大提议号
            long maxProposalNumForAcceptor = electionInfo.getMaxAcceptFirstPhaseNum();
            //接受到的最大号
            long maxAcceptNumForAcceptor = electionInfo.getMaxAcceptSecondPhaseNum();
            Object maxAcceptValueForAcceptor = electionInfo.getMaxAcceptSecondPhaseValue();

            //
            if (num < maxProposalNumForAcceptor) {
                LOGGER.warn("current maxAcceptFirstPahseNum is[" + maxProposalNumForAcceptor + "],and num is [" + num
                        + "],so give up this election");
                return new ElectionResponse(CodeInfo.DENY_CODE, maxProposalNumForAcceptor, maxAcceptNumForAcceptor,
                        maxAcceptValueForAcceptor);
            }

            electionInfo.setMaxAcceptFirstPhaseNum(num);

            //持久化到本地
            byte[] res = BizSerialAndDeSerialUtil.objectToBytesByByJson(electionInfo);
            dataStore.writeToStore(res);
            return new ElectionResponse(CodeInfo.ACCEPT_CODE, maxProposalNumForAcceptor, maxAcceptNumForAcceptor, maxAcceptValueForAcceptor);
        } finally {
            lock.unlock();
        }
    }

    //保存接收到的第二次选举当前成员最大数和值
    @Override
    public ElectionResponse saveAcceptSecondPhaseMaxNumAndValueForCurrentMember(long electionRoundParam, long num, Object value) {
        lock.lock();
        try {
            ElectionInfo electionInfo = currentMember.getElectionInfo();
            if (electionInfo == null) {
                //当前选举信息不能为空
                throw new IllegalArgumentException("current electionInfo is null");
            }

            long maxProposalNumForAcceptor = electionInfo.getMaxAcceptFirstPhaseNum();
            long maxAcceptNumOfSendPhaseForAcceptor = electionInfo.getMaxAcceptSecondPhaseNum();
            Object maxAcceptValueOfSendPhaseForAcceptor = electionInfo.getMaxAcceptSecondPhaseValue();
            if (electionRoundParam < electionInfo.getElectionRound()) {
                LOGGER.warn("electionRoundParam less than current electionRound,current electionRound is["
                        + electionInfo.getElectionRound() + "],and electionRoundParam is [" + electionRoundParam
                        + "],so give up this election");
                return new ElectionResponse(CodeInfo.DENY_CODE, maxProposalNumForAcceptor, maxAcceptNumOfSendPhaseForAcceptor,
                        maxAcceptValueOfSendPhaseForAcceptor);
            }

            /**
             * 是否小于已经接收到的一阶段最大提议号
             */
            if (num < maxProposalNumForAcceptor) {
                LOGGER.warn("num less than MaxProposalNumForAcceptor,MaxProposalNumForAcceptor is[" + maxProposalNumForAcceptor
                        + "],and num is [" + num + "],so give up this election");
                return new ElectionResponse(CodeInfo.DENY_CODE, maxProposalNumForAcceptor, maxAcceptNumOfSendPhaseForAcceptor,
                        maxAcceptValueOfSendPhaseForAcceptor);
            }

            /**
             * 是否小于已经接收到的二阶段最大提议号
             */
            if (num < electionInfo.getMaxAcceptSecondPhaseNum()) {
                LOGGER.warn("num less than MaxAcceptNumForAcceptor,MaxAcceptNumForAcceptor is["
                        + electionInfo.getMaxAcceptSecondPhaseNum() + "],and num is [" + num + "],so give up this election");
                return new ElectionResponse(CodeInfo.DENY_CODE, maxProposalNumForAcceptor, maxAcceptNumOfSendPhaseForAcceptor,
                        maxAcceptValueOfSendPhaseForAcceptor);
            }

            electionInfo.setMaxAcceptSecondPhaseNum(num);
            electionInfo.setMaxAcceptSecondPhaseValue(value);

            /**
             * 持久化
             */
            byte[] res = BizSerialAndDeSerialUtil.objectToBytesByByJson(electionInfo);
            dataStore.writeToStore(res);
            return new ElectionResponse(CodeInfo.ACCEPT_CODE, maxProposalNumForAcceptor, num, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public PaxosMember getCurrentPaxosMember() {
        return currentMember;
    }

    @Override
    public List<PaxosMember> getOtherPaxosMemberList() {
        return otherMemberList;
    }

    @Override
    public void setCurrentPaxosMember(PaxosMember paxosMember) {
        this.currentMember = paxosMember;
    }

    @Override
    public void setOtherPaxosMemberList(List<PaxosMember> otherMemberList) {
        this.otherMemberList = otherMemberList;
    }

    @Override
    public boolean saveElectionResultAndSetStatus(Long electionRound, Long realNum, Object realValue) {
        lock.lock();
        try {
            String logStr = "electionRound[" + electionRound + "],realNum[" + realNum + "],realValue[" + realValue + "]";

            // 超过半数，则进行先保存当前结点实际的选举结果值
            ElectionInfo electionInfo = currentMember.getElectionInfo();
            if (electionInfo == null) {
                LOGGER.error("saveRealValueAndNum err,not found electionInfo," + logStr);
                return false;
            }

            // 保存选举结果时要求当前的选举轮数必须小于等于传入的参数选举轮数
            if (electionInfo.getElectionRound() > electionRound) {
                LOGGER.error("saveRealValueAndNum err,current round larger than param,current round[" + electionInfo.getElectionRound()
                        + "]" + logStr);
                return false;
            }

            electionInfo.setRealNum(realNum);
            electionInfo.setRealValue(realValue);
            electionInfo.setElectionRoundByValue(electionRound);

            // 重置提议号及提议者提议的次数为初始值
//			electionInfo.setProposalNumForProposer(CodeInfo.INIT_PROPOSAL_NUM);
//			electionInfo.setProposalRoundByValue(CodeInfo.INIT_PROPOSAL_ROUND);

            // 设置当前结点状态为已选举完成
            this.updateCurrentMemberStatus(null, PaxosMemberStatus.NORMAL.getStatus());

            // 设置当前二阶段接收值为null,下次重新选举时可以接受新值
            electionInfo.setMaxAcceptSecondPhaseValue(null);

            // 看选举结果值是否是当前结点，是的话，设置当前结点角色为leader
            this.fillLeaderMember(realValue.toString());

            //持久化
            byte[] res = BizSerialAndDeSerialUtil.objectToBytesByByJson(electionInfo);
            dataStore.writeToStore(res);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 找出leader结点
     *
     * @param electionRes
     * @return
     */
    private void fillLeaderMember(String electionRes) {
        ElectionUtil.fillLeaderToMemberList(currentMember, otherMemberList, electionRes);
    }

    //保存第一次选举提议号和选举阶段
    @Override
    public boolean saveFirstPhaseNumAndProposalRoundForProposer(Long electionRound, Long proposalRound, Long proposalNum) {
        if (proposalRound == null || proposalNum == null) {
            return false;
        }

        ElectionInfo electionInfo = getCurrentPaxosMember().getElectionInfo();
        long electionRoundOfCurrent = electionInfo.getElectionRound();
        if (electionRoundOfCurrent > electionRound) {
            LOGGER.error("current election is larger than param electionRound,current electionRound[" + electionRoundOfCurrent
                    + "],param electionRound[" + electionRound + "]");
            return false;
        }

        long proposalNumOfCurrent = electionInfo.getProposalNumForProposer();
        if (proposalNumOfCurrent > proposalNum) {
            LOGGER.error("current proposalNum is larger than param proposalNum,current proposalNumOfCurrent[" + proposalNumOfCurrent
                    + "],param proposalNum[" + proposalNum + "]");
            return false;
        }

        long proposalRoundOfCurrent = electionInfo.getProposalRound();
        if (proposalRoundOfCurrent > proposalRound) {
            LOGGER.error("current proposalRound is larger than param proposalRound,current proposalRoundOfCurrent["
                    + proposalRoundOfCurrent + "],param proposalRound[" + proposalRound + "]");
            return false;
        }

        lock.lock();
        try {
            electionInfo.setProposalRoundByValue(proposalRound);
            electionInfo.setProposalNumForProposer(proposalNum);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 清空当前成员接收到的二阶段提议值
     */
    @Override
    public boolean cleanAcceptSecondPhaseValueForCurrentMember() {
        lock.lock();

        try {
            ElectionInfo electionInfo = getCurrentPaxosMember().getElectionInfo();
            electionInfo.setMaxAcceptSecondPhaseValue(null);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 更新当前节点状态
     */
    @Override
    public boolean updateCurrentMemberStatus(Integer expectStatus, Integer updateStatus) {
        lock.lock();
        try {

            if (updateStatus == null) {
                throw new IllegalArgumentException("要更新状态不能为空");
            }

            int oldStatus = currentMember.getStatus().get();
            if (expectStatus != null && oldStatus != expectStatus) {
                LOGGER.error("updateCurrentMemberStatus err,oldStatus not eq expectStatus,expectStatus[" + expectStatus + "],oldStatus["
                        + oldStatus + "],updateStatus[" + updateStatus + "]");
                return false;
            }

            getCurrentPaxosMember().setStatusValue(updateStatus);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 更新当前leader信息
     */
    @Override
    public boolean updateCurrentMemberLeaderMember(PaxosMember leaderMember) {
        lock.lock();
        try {
            getCurrentPaxosMember().setLeaderMember(leaderMember);
            return true;
        } finally {
            lock.unlock();
        }
    }
}
