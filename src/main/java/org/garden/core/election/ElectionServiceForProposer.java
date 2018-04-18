package org.garden.core.election;

import com.alibaba.fastjson.JSON;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.garden.core.constants.CodeInfo;
import org.garden.core.paxos.*;
import org.garden.enums.PaxosMemberStatus;
import org.garden.exchange.ExchangeClient;
import org.garden.handler.UpStreamHandler;
import org.garden.remoting.Command;
import org.garden.remoting.Response;
import org.garden.remoting.ResponseFuture;
import org.garden.task.HeartBeatProcessor;
import org.garden.util.RequestAndResponseUtil;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author wgt
 * @date 2018-04-06
 * @description 提议者处理选举算法服务类
 **/
public class ElectionServiceForProposer implements ElectionForProposer {

    private static final Logger LOGGER = Logger.getLogger(ElectionServiceForProposer.class);

    private HeartBeatProcessor heartBeatProcessor;

    private PaxosStoreInf paxosStore;

    public ElectionServiceForProposer(){

    }

    public ElectionServiceForProposer(UpStreamHandler upStreamHandler,HeartBeatProcessor heartBeatProcessor){
        paxosStore = new DefaultPaxosStore();
        paxosStore.setCurrentPaxosMember(upStreamHandler.getPaxosCoreComponent().getCurrentPaxosMember());
        paxosStore.setOtherPaxosMemberList(upStreamHandler.getPaxosCoreComponent().getOtherPaxosMemberList());
        this.heartBeatProcessor =heartBeatProcessor;
    }

    /**
     * 一阶段选举
     *
     * @param round
     * @param num 提议号
     * @param value 提议值
     * @return 返回结果，代表是否可以进行二阶段，如果可以的话提议值是多少
     */
    @Override
    public Pair<Boolean/* 是否可以进行下个阶段选举 */, Object/* 可以进行下阶段选举的提议值 */> proposalFirstPhase(long round, long num, Object value) {
        String logStr = "round[" + round + "],num[" + num + "],value[" + value + "]";

        LOGGER.info("begin proposalFirstPhase," + logStr);

        //获取其它结点成员
        List<PaxosMember> otherMemberList = paxosStore.getOtherPaxosMemberList();

        if (CollectionUtils.isEmpty(otherMemberList)) {
            //集群只有当前结点自己一个结点，选举失败
            LOGGER.info("end proposalFirstPhase no otherMemberList found," + logStr);
            return new Pair<Boolean, Object>(false, null);
        }

        //当前结点自己先保存下自己的提议号
        ElectionResponse saveRes = paxosStore.saveAcceptFirstPhaseMaxNumForCurrentMember(num);

        if (CodeInfo.DENY_CODE.equals(saveRes.getCode())) {
            LOGGER.info("end proposalFirstPhase err,can not save maxFirstPhaseNum," + logStr);
            return new Pair<Boolean, Object>(false, null);
        }

        //开始向其它结点提议
        LOGGER.info("try to send firstPhase protocal to otherMemberList," + logStr);
        List<ElectionResponse> acceptResponseList = new ArrayList<ElectionResponse>();

        // 由于接受者已经处于选举完成状态，如果这个类型返回结果超过半数则以返回集合中最大编号的实际值为leader
        List<ElectionResponse> denyForHasLeaderList = new ArrayList<ElectionResponse>();

        List<ElectionResponse> allElectionResponseListHasElectionRound = new ArrayList<ElectionResponse>();

        //异步提交发送第一阶段请求
        List<ElectionResponse> responseResList = this.sendElectionRequestToOtherMemberList(otherMemberList, round, num, value,
                CodeInfo.REQ_TYPE_ELECTION_FIRST_PHASE);

        for (ElectionResponse electionResponse : responseResList) {
            if (CodeInfo.ACCEPT_CODE.equals(electionResponse.getCode())) {
                acceptResponseList.add(electionResponse);
            } else if (CodeInfo.DENY_CODE_FOR_HAS_LEADER.equals(electionResponse.getCode())) {
                denyForHasLeaderList.add(electionResponse);
            }

            if (electionResponse.getElectionRound() != null) {
                allElectionResponseListHasElectionRound.add(electionResponse);
            }
        }

        //看处于选举完成状态的接受者数目是否大于一半，是的话，取出最大提议轮数的实际值为leader

        int clusterNodesNum = paxosStore.getCurrentPaxosMember().getClusterNodesNum();
        int denyNumForHasLeader = 0;
        long maxElectionRound = -1;
        Object maxElectFinishRealValue = null;
        Long maxElectionRealNum = null;
        for (ElectionResponse response : denyForHasLeaderList) {
            denyNumForHasLeader++;
            Long electionRonud = response.getElectionRound();
            if (electionRonud != null && electionRonud > maxElectionRound) {
                maxElectionRound = electionRonud;
                maxElectFinishRealValue = response.getRealValue();
                maxElectionRealNum = response.getRealNum();
            }
        }

        // 这里当前结点也算一票
        denyNumForHasLeader = denyNumForHasLeader + 1;
        if (denyNumForHasLeader > (clusterNodesNum / 2)) {
            LOGGER.info("end send proposalFirstPhase protocal,deny num too larger,use realNum[" + maxElectionRealNum + "],and realValue["
                    + maxElectFinishRealValue + "],round[" + maxElectionRound + "]," + logStr);
            paxosStore.saveElectionResultAndSetStatus(maxElectionRound, maxElectionRealNum, maxElectFinishRealValue);
            return new Pair<Boolean, Object>(false, null);
        }

        /**
         * 4.决定是否发起下个阶段选举<br/>
         * 要求接受提议者数目大于半数，提议值取当前接收集合中已经被第二阶段接受的最大编号值
         */
        // 这里接收票数要加上本结点自己这一票
        int acceptNum = acceptResponseList.size() + 1;
        if (!this.acceptNumLargerThanHalf(acceptNum, clusterNodesNum)) {
            LOGGER.info("end send firstPhase protocal acceptNum less,give up," + logStr);

            //sort the allMemberList
            Collections.sort(allElectionResponseListHasElectionRound, new PaxosElectionResponseComparator());

            Long maxElectionRoundForAcceptor = null;
            Long maxProposalNumFromAcceptor = null;
            if (!CollectionUtils.isEmpty(allElectionResponseListHasElectionRound)) {
                ElectionResponse lastMaxElectionResponse = allElectionResponseListHasElectionRound
                        .get(allElectionResponseListHasElectionRound.size() - 1);
                maxElectionRoundForAcceptor = lastMaxElectionResponse.getElectionRound();
                maxProposalNumFromAcceptor = lastMaxElectionResponse.getMaxProposalNum();
            }

            if (maxElectionRoundForAcceptor != null && maxProposalNumFromAcceptor != null) {
                Pair<Long, Long> roundAndNumProposorShouldSave = this
                        .getProposalRoundAndNumByMaxNumFromAcceptor(maxProposalNumFromAcceptor);
                paxosStore.saveFirstPhaseNumAndProposalRoundForProposer(maxElectionRoundForAcceptor,
                        roundAndNumProposorShouldSave.getValue0(), roundAndNumProposorShouldSave.getValue1());
            }

            return new Pair<Boolean, Object>(false, null);
        }

        /**
         * 超过半数，取接受集合中最大编号的提议值作为二阶段提议值
         */
        long maxNumForAcceptSecondPahse = -1;
        Object valueForAcceptSecondPahse = null;

        for (int i = 0; i < acceptResponseList.size(); i++) {
            ElectionResponse electionResponse = acceptResponseList.get(i);
            if (electionResponse.getMaxAcceptNum() != null && electionResponse.getMaxAcceptNum() > maxNumForAcceptSecondPahse) {
                maxNumForAcceptSecondPahse = electionResponse.getMaxAcceptNum();
                valueForAcceptSecondPahse = electionResponse.getMaxAcceptValue();
            }
        }

        Object resValue = value;
        if (valueForAcceptSecondPahse != null) {
            resValue = valueForAcceptSecondPahse;
        }

        return new Pair<Boolean, Object>(true, resValue);
    }

    //判断是否超过半数
    private boolean acceptNumLargerThanHalf(long acceptNum, long clusterNodesNum) {
        if (acceptNum > (clusterNodesNum / 2)) {
            return true;
        }

        return false;
    }

    private List<ElectionResponse> sendElectionRequestToOtherMemberList(List<PaxosMember> otherMemberList, long round, long num,
                                                                        Object value, int phase) {

        // sort
        Collections.sort(otherMemberList, new PaxosMemberStatusComparator());
        List<ResponseFuture> futureList = new ArrayList<ResponseFuture>();

        String phaseDesc = "";

        //判断是第几阶段
        if (CodeInfo.REQ_TYPE_ELECTION_FIRST_PHASE == phase) {
            phaseDesc = "firstPhase";
        } else if (CodeInfo.REQ_TYPE_ELECTION_SECOND_PHASE == phase) {
            phaseDesc = "secondPhase";
        } else {
            //不支持的选举阶段参数
            throw new IllegalArgumentException("nonsupport this election phase,sendElectionRequestToOtherMemberList");
        }

        for (PaxosMember paxosMember : otherMemberList) {

            //判断节点是否宕机
            if (!paxosMember.getIsUp()) {
                LOGGER.info("err send electionReq to ip[" + paxosMember.getIp() + "],port[" + paxosMember.getPort() + "],member is not up");
                continue;
            }

            ExchangeClient exchangeClient = this.getExchangeClient(paxosMember);

            if (exchangeClient == null) {
                LOGGER.error("cannot get exchangeClient for member,ip[" + paxosMember.getIp() + "],port[" + paxosMember.getPort() + "]");
                continue;
            }

            String curLogStr = "accepter round[" + round + "],num[" + num + "],value[" + value + "],ip[" + paxosMember.getIp() + "],port["
                    + paxosMember.getPort() + "]";

            try {
                ResponseFuture responseFuture = null;
                if (CodeInfo.REQ_TYPE_ELECTION_FIRST_PHASE == phase) {
                    //第一阶段建议
                    responseFuture = this.sendFirstPhasePropotal(exchangeClient, round, num, value, paxosMember);
                } else if (CodeInfo.REQ_TYPE_ELECTION_SECOND_PHASE == phase) {
                    //第二阶段建议
                    responseFuture = this.sendSecondPhasePropotal(exchangeClient, round, num, value, paxosMember);
                } else {
                    //不支持的选举阶段参数
                    throw new IllegalArgumentException("nonsupport this election phase,sendElectionRequestToOtherMemberList");
                }
                futureList.add(responseFuture);
            } catch (Exception e) {
                LOGGER.error("send " + phaseDesc + " to acceptor err," + curLogStr, e);
            }
        }

        /**
         * 开始解析结果
         */
        List<ElectionResponse> resList = new ArrayList<ElectionResponse>();
        for (ResponseFuture responseFuture : futureList) {
            try {
                LOGGER.info("responseFuture"+responseFuture.get());
                Object res = responseFuture.get();
                Response remotingResponse = null;
                if (res != null) {
                    remotingResponse = (Response) res;
                }

                if (remotingResponse == null) {
                    LOGGER.error("no response found for firstPahse");
                    continue;
                }

                ElectionResponse electionResponse = JSON.parseObject(remotingResponse.getData(), ElectionResponse.class);
                resList.add(electionResponse);
            } catch (Exception e) {
                LOGGER.error("get response err", e);
            }
        }

        return resList;
    }

    private Pair<Long, Long> getProposalRoundAndNumByMaxNumFromAcceptor(final long maxNumFromAcceptro) {
        PaxosMember currentMember = paxosStore.getCurrentPaxosMember();
        //选举节点数
        int clusterMemberNum = currentMember.getClusterNodesNum();
        int currentMemberUniqueProposalSeq = currentMember.getElectionInfo().getCurrentMemberUniqueProposalSeq();
        long currentProposalRound = currentMember.getElectionInfo().getProposalRound();
        return ElectionNumberGenerator.getElectionNumberByMaxNumber(clusterMemberNum, currentMemberUniqueProposalSeq, currentProposalRound,
                maxNumFromAcceptro);

    }

    private ExchangeClient getExchangeClient(PaxosMember member) {
        return heartBeatProcessor.getExchangeClientReconnectWhenNotExist(member);
    }

    private ResponseFuture sendFirstPhasePropotal(ExchangeClient exchangeClient, long round, long num, Object value, PaxosMember member)
            throws Exception {
        Command firstPhaseReqCommand = RequestAndResponseUtil.composeElectionRequest(round, CodeInfo.FIRST_PHASE_SEQ, num, value);

        return exchangeClient.sendAsyncSync(firstPhaseReqCommand);
    }

    public ElectionResponse parseResponse(Response res, String logStr) {
        LOGGER.info("send firstPhase protocal to," + logStr + ",acceptor res[" + JSON.toJSONString(res) + "]");
        if (res == null || !(res instanceof Response)) {
            LOGGER.error("return res err for send firstPhase protocal,res[" + res + "]," + logStr);
            return new ElectionResponse(CodeInfo.SEND_ERR_CODE, null, null, null);
        }

        /**
         * 解析结果
         */
        Response remotingResponse = (Response) res;
        String protocalResponse = remotingResponse.getData();
        if (StringUtils.isEmpty(protocalResponse)) {
            LOGGER.error("return protocalResponse cannot be null for send firstPhase protocal,res[" + res + "]," + logStr);
            return new ElectionResponse(CodeInfo.SEND_ERR_CODE, null, null, null);
        }

        ElectionResponse electionResponse = JSON.parseObject(protocalResponse, ElectionResponse.class);
        if (electionResponse == null) {
            LOGGER.error("return protocalResponse cannot convert to electionResponse for send firstPhase protocal,res[" + res + "],"
                    + logStr);
            return new ElectionResponse(CodeInfo.SEND_ERR_CODE, null, null, null);
        }

        LOGGER.info("end send firstPhase protocal to," + logStr + ",acceptor res[" + JSON.toJSONString(res) + "]");
        return electionResponse;
    }

    //发起第二轮提议
    private ResponseFuture sendSecondPhasePropotal(ExchangeClient exchangeClient, long round, long num, Object value, PaxosMember member) {
        String ip = member.getIp();
        int port = member.getPort();
        String logStr = "ip[" + ip + "],port[" + port + "],num[" + num + "],value[" + value + "]";

        try {
            Command secondPhaseReqCommand = RequestAndResponseUtil.composeElectionRequest(round, CodeInfo.SECOND_PHASE_SEQ, num,
                    value);

            return exchangeClient.sendAsyncSync(secondPhaseReqCommand);
        } catch (Exception e) {
            LOGGER.error("err,send secondPhase protocal to," + logStr, e);
            return null;
        }

    }

    @Override
    public boolean proposalSecondPhase(long round, long num, Object value) {
        String logStr = "num[" + num + "],value[" + value + "],round[" + round + "]";

        LOGGER.info("begin secondPhase," + logStr);

        //获取其它结点成员
        List<PaxosMember> otherMemberList = paxosStore.getOtherPaxosMemberList();
        if (CollectionUtils.isEmpty(otherMemberList)) {
            //集群只有当前结点自己一个结点，选举失败
            LOGGER.info("end secondPhase no otherMemberList found," + logStr);
            return false;
        }

        //当前结点自己先保存下自己的提议号
        ElectionResponse saveRes = paxosStore.saveAcceptSecondPhaseMaxNumAndValueForCurrentMember(round, num, value);
        if (saveRes.getCode().equals(CodeInfo.DENY_CODE)) {
            LOGGER.info("end secondPhase err,can not save maxFirstPhaseNum," + logStr);
            return false;
        }

        //开始向其它结点第二阶段提议
        LOGGER.info("try to send secondPhase accept to otherMemberList," + logStr);
        List<PaxosMember> acceptProtocalMemberList = new ArrayList<PaxosMember>();
        List<ElectionResponse> acceptResponseList = new ArrayList<ElectionResponse>();

        List<ElectionResponse> electionResponseResList = this.sendElectionRequestToOtherMemberList(otherMemberList, round, num, value,
                CodeInfo.REQ_TYPE_ELECTION_SECOND_PHASE);
        for (ElectionResponse electionResponse : electionResponseResList) {
            if (electionResponse != null && CodeInfo.ACCEPT_CODE.equals(electionResponse.getCode())) {
                acceptResponseList.add(electionResponse);
            }
        }
        LOGGER.info("finish send secondPhase accept to otherMemberList,acceptCount[" + acceptProtocalMemberList.size() + "],list["
                + acceptProtocalMemberList + "]," + logStr);

        /**
         * 4.决定是否发起下个阶段选举<br/>
         * 要求接受提议者数目大于半数，提议值取当前接收集合中已经被第二阶段接受的最大编号值
         */
        int clusterNodesNum = paxosStore.getCurrentPaxosMember().getClusterNodesNum();
        // 这里接收票数要加上本结点自己这一票
        int acceptNum = acceptResponseList.size() + 1;
        if (!this.acceptNumLargerThanHalf(acceptNum, clusterNodesNum)) {
            LOGGER.info("end send secondPhase protocal acceptNum less,give up," + logStr);
            return false;
        }

        return true;
    }

    private boolean sendElectionResultToLearners(Long electionRound, Long realNum, Object realValue, List<PaxosMember> otherNodeList,
                                                 String logStr) {
        if (CollectionUtils.isEmpty(otherNodeList)) {
            LOGGER.error("sendElectionResultToLearners err," + logStr);
            return false;
        }

        Command electionResultReqCommand = RequestAndResponseUtil.composeElectionResultRequest(electionRound, realNum, realValue);

        for (PaxosMember paxosMember : otherNodeList) {
            String ip = paxosMember.getIp();
            int port = paxosMember.getPort();
            LOGGER.info("begin send electionReulst to ip[" + ip + "],port[" + port + "]");

            ExchangeClient exchangeClient = heartBeatProcessor.getExchangeClientReconnectWhenNotExist(paxosMember);
            if (exchangeClient == null) {
                LOGGER.error("not foudn exchangeClient when sendElectionResult to ip[" + paxosMember.getIp() + "],port["
                        + paxosMember.getPort() + "],`" + logStr);
                continue;
            }

            try {
                exchangeClient.sendAsyncSync(electionResultReqCommand);
                LOGGER.info("end send electionReulst succ to ip[" + ip + "],port[" + port + "]");
            } catch (Exception e) {
                LOGGER.error("send electionResult err to ip[" + ip + "],port[" + port + "]," + logStr, e);
            }
        }

        return true;
    }

    @Override
    public boolean processAfterElectionSecondPhase(boolean electionSuccessFlag, Long electionRound, Long realNum, Object realValue,
                                                   boolean shouldSendResult) {
        String logStr = "electionRound[" + electionRound + "],realNum[" + realNum + "],realValue[" + realValue + "],electionSuccessFlag["
                + electionSuccessFlag + "],shouldSendResult[" + shouldSendResult + "]";
        LOGGER.info("begin processAfterElectionSecondPhase," + logStr);

        if (!electionSuccessFlag) {
            /**
             * 选举失败，设置当前结点状态为初始状态，下次可以再提议
             */
            paxosStore.updateCurrentMemberStatus(PaxosMemberStatus.ELECTIONING.getStatus(), PaxosMemberStatus.INIT.getStatus());
            return false;
        }

        boolean saveElectionRes = paxosStore.saveElectionResultAndSetStatus(electionRound, realNum, realValue);
        if (!saveElectionRes) {
            LOGGER.error("processAfterElectionSecondPhase saveElectionResultAndSetStatus err," + logStr);
            return false;
        }

        if (!shouldSendResult) {
            /**
             * 不需要广播，返回
             */
            return true;
        }

        /**
         * 广播
         */
        boolean sendRealValueToLearners = this.sendElectionResultToLearners(electionRound, realNum, realValue,
                paxosStore.getOtherPaxosMemberList(), logStr);
        if (!sendRealValueToLearners) {
            LOGGER.error("processAfterElectionSecondPhase sendRealValueToLearners err," + logStr);
            return false;
        }

        return true;
    }

}

