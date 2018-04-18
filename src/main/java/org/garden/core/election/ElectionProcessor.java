package org.garden.core.election;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.garden.core.paxos.*;
import org.garden.enums.PaxosMemberRole;
import org.garden.enums.PaxosMemberStatus;
import org.garden.handler.UpStreamHandler;
import org.garden.task.HeartBeatProcessor;
import org.garden.util.PaxosConflictionUtil;
import org.javatuples.Pair;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author wgt
 * @date 2018-04-13
 * @description 选举处理器
 * 当前结点如果状态不是处于［正常］已选举完成状态，在一段时间后需要发起提议
 **/
public class ElectionProcessor {

    private static final Logger LOGGER = Logger.getLogger(ElectionProcessor.class);

    //默认定时时间
    private static final long DEFAULT_INTERVEL = 1000;

    //选举超时时间，超过这个时间该结点才重新生成提议号发起下次提议,30秒
    private static final long ELECTION_TIME_OUT = 5 * 6 * 1000;

    //定时任务线程池
    private ScheduledExecutorService scheduledExecutorService;

    //paxos核心部件
    private PaxosCore memberService = new PaxosCoreComponent();

    //提议者处理选举算法服务类
    private ElectionForProposer electionService;

    //上一次提议
    private volatile long lastTimeProposal;

    private Lock lock = new ReentrantLock();

    //提供paxos存储相关服务
    private PaxosStoreInf paxosStore = new DefaultPaxosStore();

    public ElectionProcessor() {

    }

    public void start(UpStreamHandler upStreamHandler,HeartBeatProcessor heartBeatProcessor) {
        //检查
        if (upStreamHandler == null) {
            throw new IllegalArgumentException("upstreamHandler is null");
        } else {

            electionService = new ElectionServiceForProposer(upStreamHandler,heartBeatProcessor);


            PaxosMember paxosMember = upStreamHandler.getPaxosCoreComponent().getCurrentPaxosMember();
            List<PaxosMember> paxosMemberList = upStreamHandler.getPaxosCoreComponent().getOtherPaxosMemberList();
            memberService.setCurrentPaxosMember(paxosMember);
            memberService.setOtherPaxosMemberList(paxosMemberList);
            paxosStore.setCurrentPaxosMember(paxosMember);
            paxosStore.setOtherPaxosMemberList(paxosMemberList);
        }

        //开启定时查看与leader连接状态,如果连不上了，则进行选举
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(new ElectionCheckTask(), 1000, DEFAULT_INTERVEL, TimeUnit.MILLISECONDS);
    }

    class ElectionCheckTask implements Runnable {

        @Override
        public void run() {
            LOGGER.info("begin election check");

            /**
             * 1.开始处理结点的status状态，两个方面<br/>
             * a.当前结点非leader，则在其有leader情况下需要判断leader是否连的通，连不通则将当前结点状态置为INIT，以例进行选举<br/>
             * b.当前是leader，则需要判断与其它结点是否连得上，连不上就设置当前leader状态为INIT
             */
            PaxosMember currentMember = memberService.getCurrentPaxosMember();
            if (currentMember.getRole() == PaxosMemberRole.LEADER) {
                processStatusForLeader();
            } else {
                processStatusForNotLeader();
            }

            //处理选举
            long electionIntervalBetweenRound = PaxosConflictionUtil.electionIntervalBetweenRound;
            LOGGER.info("next electionInterval[" + electionIntervalBetweenRound + "]");
            if (currentMember.getRole() == PaxosMemberRole.LEADER) {
                //当前结点是leader的话不需要主动发起选举
                LOGGER.info("local member is leader");
                return;
            }
            processElection();

            LOGGER.info("end election check");
        }

    }

    private void processStatusForLeader() {
        PaxosMember currentMember = memberService.getCurrentPaxosMember();
        if (currentMember.getRole() != PaxosMemberRole.LEADER) {
            return;
        }

        List<PaxosMember> otherMemberList = paxosStore.getOtherPaxosMemberList();
        if (CollectionUtils.isEmpty(otherMemberList)) {
            return;
        }

        int liveMemberNum = 0;
        for (PaxosMember paxosMember : otherMemberList) {
            if (paxosMember.getIsUp()) {
                liveMemberNum++;
            }
        }

        if (liveMemberNum == 0) {
            //与其它结点都连不上，则设置当前leader结点状态为INIT，准备下一轮选举
            LOGGER.info("processForLeader leader cannot connect all other members,so set the status to init");
            paxosStore.updateCurrentMemberStatus(null, PaxosMemberStatus.INIT.getStatus());
        }
    }

    /**
     * 设置不是leader状态
     */
    private void processStatusForNotLeader() {
        PaxosMember currentMember = paxosStore.getCurrentPaxosMember();
        PaxosMember leaderMember = currentMember.getLeaderMember();
        if (leaderMember == null || !leaderMember.getIsUp()) {
            //不能连通leader
            paxosStore.updateCurrentMemberStatus(PaxosMemberStatus.NORMAL.getStatus(), PaxosMemberStatus.INIT.getStatus());
            return;
        }

        if (leaderMember != null && leaderMember.getIsUp()) {
            //能连通leader
            paxosStore.updateCurrentMemberStatus(null, PaxosMemberStatus.NORMAL.getStatus());
        }
    }

    /**
     * 执行选举
     *
     * @param
     */
    private void processElection() {
        long curTime = System.currentTimeMillis();
        if ((curTime - lastTimeProposal) < PaxosConflictionUtil.electionIntervalBetweenRound) {
            // 未到下轮选举时间
            return;
        }

        //当前结点如果是选举中则返回，并且未超时
        PaxosMember currentMember = memberService.getCurrentPaxosMember();
        if (currentMember.getStatus().intValue() == PaxosMemberStatus.ELECTIONING.getStatus() && ((curTime - lastTimeProposal) < ELECTION_TIME_OUT)) {
            //该结点处于选举中，且未超时，则不再进行选举
            return;
        }

        if (currentMember.getLeaderMember() == null || !currentMember.getLeaderMember().getIsUp()) {
            //leader不存在，或不可用，进行选举
            beginProposal();
        }
    }

    private void beginProposal() {
        lock.lock();

        long oldTime = System.currentTimeMillis();
        try {

            LOGGER.info("begin proposal in task");

            //将当前结点状态改为选举中
            PaxosMember currentMember = memberService.getCurrentPaxosMember();
            currentMember.setStatusValue(PaxosMemberStatus.ELECTIONING.getStatus());

            int clusterMemberNum = currentMember.getClusterNodesNum();
            int currentMemberUniqueProposalSeq = currentMember.getElectionInfo().getCurrentMemberUniqueProposalSeq();
            long currentProposalRound = currentMember.getElectionInfo().getAndIncreaseProposalRound();
            long num = ElectionNumberGenerator.getElectionNumberByParam(clusterMemberNum, currentMemberUniqueProposalSeq,
                    currentProposalRound);

            Object value = currentMember.getIpAndPort();

            lastTimeProposal = System.currentTimeMillis();

            // 一阶段提交
            long electionRound = currentMember.getElectionInfo().getElectionRound();
            // 提议时使用当前已选举轮数+1
            electionRound = electionRound + 1;
            String logStr = "electionRound[" + electionRound + "],num[" + num + "],value[" + value + "]";
            LOGGER.info("begin proposalFirstPhase," + logStr);

            Pair<Boolean, Object> firstPhaseRes = electionService.proposalFirstPhase(electionRound, num, value);
            if (!firstPhaseRes.getValue0()) {
                PaxosConflictionUtil.electionIntervalBetweenRound = PaxosConflictionUtil.getRandomElectionIntervalTime();
                LOGGER.info("election be rejected in firstPhase," + logStr);
                paxosStore.updateCurrentMemberStatus(PaxosMemberStatus.ELECTIONING.getStatus(), PaxosMemberStatus.INIT.getStatus());
                return;
            }
            LOGGER.info("end proposalFirstPhase,result is[" + firstPhaseRes.getValue0() + "],param:" + logStr);

            //二阶段提交
            LOGGER.info("begin proposalSecondPhase," + logStr);
            Object valueForSecondPhase = firstPhaseRes.getValue1();
            boolean secondPhaseRes = electionService.proposalSecondPhase(electionRound, num, valueForSecondPhase);
            if (!secondPhaseRes) {
                LOGGER.info("election be rejected in secondPhase," + logStr);
                paxosStore.updateCurrentMemberStatus(PaxosMemberStatus.ELECTIONING.getStatus(), PaxosMemberStatus.INIT.getStatus());
                return;
            }
            LOGGER.info("end proposalSecondPhase,result is[" + secondPhaseRes + "],param:" + logStr);

            //处理二阶段结果
            electionService.processAfterElectionSecondPhase(secondPhaseRes, electionRound, num, valueForSecondPhase, true);

            //选举成功后,之前的选举间隔时间调回来
            PaxosConflictionUtil.electionIntervalBetweenRound = PaxosConflictionUtil.DEFALT_INTERVAL;

            long cost = System.currentTimeMillis() - oldTime;

            String log = "electionRound[" + electionRound + "],realNum[" + num + "],realValue[" + valueForSecondPhase + "]," + "host["
                    + currentMember.getIp() + "],port[" + currentMember.getPort() + "],election finish,cost[" + cost + "],";
            writeLog(log);
        } finally {
            lock.unlock();
        }

        LOGGER.info("end proposal in task");
    }

    private static void writeLog(String data) {
        RandomAccessFile accessFile = null;
        try {
            accessFile = new RandomAccessFile("/Users/zhenzuo.zzz/Documents/temp/out.log", "rw");
            long length = accessFile.length();
            accessFile.seek(length);

            accessFile.writeBytes(data + "\n");
        } catch (Exception e) {
            LOGGER.error("log result err", e);
        } finally {
            if (accessFile != null) {
                try {
                    accessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void stop() {
        scheduledExecutorService.shutdownNow();
    }

    /**
     * 获取上一次提议
     *
     * @param
     * @return
     */
    public long getLastTimeProposal() {
        return lastTimeProposal;
    }

    /**
     * 设置上一次提议
     *
     * @param lastTimeProposal 上一次提议
     */
    public void setLastTimeProposal(long lastTimeProposal) {
        this.lastTimeProposal = lastTimeProposal;
    }

}
