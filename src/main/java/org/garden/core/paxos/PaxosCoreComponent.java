package org.garden.core.paxos;

import org.apache.log4j.Logger;
import org.garden.core.constants.CodeInfo;
import org.garden.core.election.*;
import org.garden.util.BizSerialAndDeSerialUtil;
import org.garden.util.PaxosConflictionUtil;

import java.util.List;

/**
 * @author wgt
 * @date 2018-04-01
 * @description paxos核心部件
 **/
public class PaxosCoreComponent implements PaxosCore{

    private static final Logger LOGGER = Logger.getLogger(PaxosCoreComponent.class);

    //提议者处理选举算法服务类
    private ElectionForProposer electionService = new ElectionServiceForProposer();

    //提供paxos存储相关服务
    private PaxosStoreInf paxosStoreInf = new DefaultPaxosStore();

    private ElectionForAcceptor electionServiceForAcceptor = new ElectionServiceForAcceptor();

    //获取当前成员
    @Override
    public PaxosMember getCurrentPaxosMember() {
        return paxosStoreInf.getCurrentPaxosMember();
    }

    //获取除当前成员外集群其它成员
    @Override
    public List<PaxosMember> getOtherPaxosMemberList() {
        return paxosStoreInf.getOtherPaxosMemberList();
    }

    //设置当前节点信息
    @Override
    public void setCurrentPaxosMember(PaxosMember paxosMember) {
        paxosStoreInf.setCurrentPaxosMember(paxosMember);
    }

    //保存当前成员
    @Override
    public void setOtherPaxosMemberList(List<PaxosMember> otherMemberList) {
        paxosStoreInf.setOtherPaxosMemberList(otherMemberList);
    }

    @Override
    public ElectionResponse saveAcceptFirstPhaseMaxNumForCurrentMember(long num) {
        return paxosStoreInf.saveAcceptFirstPhaseMaxNumForCurrentMember(num);
    }

    @Override
    public ElectionInfo getElectionInfoForCurrentMember() {
        return getCurrentPaxosMember().getElectionInfo();
    }

    @Override
    public ElectionResponse saveAcceptSecondPhaseMaxNumAndValueForCurrentMember(long electionRound, long num, Object value) {
        return paxosStoreInf.saveAcceptSecondPhaseMaxNumAndValueForCurrentMember(electionRound, num, value);
    }

    /**
     * 处理二阶段选举完成后，广播选举结果
     *
     * @param electionSuccessFlag
     * @param electionRound
     *            当前选举轮次
     * @param realNum
     *            促成选举结果的提议号
     * @param realValue
     *            选举结果
     * @return
     */
    @Override
    public boolean processAfterElectionSecondPhase(boolean electionSuccessFlag, Long electionRound, Long realNum, Object realValue,
                                                   boolean shouldSendResultToOther) {
        return electionService.processAfterElectionSecondPhase(electionSuccessFlag, electionRound, realNum, realValue,
                shouldSendResultToOther);
    }

    /**
     * 处理选举请求含接收一阶段请求,接收阶二请求及接收广播选举结果，内部会根据业务类型type进行分发s
     *
     * @param type
    */
    @Override
    public BaseElectionResponse processElectionRequestForAcceptor(byte type, String electionRequestData) {
        BaseElectionResponse electionResponse;
        if (CodeInfo.REQ_TYPE_ELECTION_FIRST_PHASE == type || CodeInfo.REQ_TYPE_ELECTION_SECOND_PHASE == type) {
            ElectionRequest electionRequest = BizSerialAndDeSerialUtil.parseElectionRequest(electionRequestData);
            electionResponse = this.processElection(type, electionRequest);
        } else if (CodeInfo.REQ_TYPE_ELECTION_RESULT_TO_LEANER == type) {
            /**
             * 接收选举结果
             */
            ElectionResultRequest electionResultRequest = BizSerialAndDeSerialUtil.parseElectionResultRequest(electionRequestData);
            electionResponse = electionServiceForAcceptor.processElectionResultRequest(electionResultRequest);

            /**
             * 选举成功后,之前的选举间隔时间调回来
             */
            PaxosConflictionUtil.electionIntervalBetweenRound = PaxosConflictionUtil.DEFALT_INTERVAL;
        } else {
            throw new IllegalArgumentException("unsupport request bizType,type[" + type + "]," + electionRequestData);
        }

        return electionResponse;
    }

    private BaseElectionResponse processElection(byte type, ElectionRequest electionRequest) {
        BaseElectionResponse electionResponse;
        if (CodeInfo.REQ_TYPE_ELECTION_FIRST_PHASE == type) {
            electionResponse = electionServiceForAcceptor.processElectionRequestFirstPhase(electionRequest);
        } else if (CodeInfo.REQ_TYPE_ELECTION_SECOND_PHASE == type) {
            electionResponse = electionServiceForAcceptor.processElectionRequestSecondPhase(electionRequest);
        } else {
            throw new IllegalArgumentException("不支持的选举类型");
        }

        return electionResponse;

    }
}
