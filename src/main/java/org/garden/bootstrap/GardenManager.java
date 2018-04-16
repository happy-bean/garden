package org.garden.bootstrap;

import com.alibaba.fastjson.JSON;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.garden.Garden;
import org.garden.conf.GardenConfig;
import org.garden.core.constants.CodeInfo;
import org.garden.core.election.ElectionInfo;
import org.garden.core.election.ElectionInfoLoader;
import org.garden.core.election.ElectionProcessor;
import org.garden.core.paxos.PaxosCore;
import org.garden.core.paxos.PaxosCoreComponent;
import org.garden.core.paxos.PaxosMember;
import org.garden.core.paxos.PaxosMemberComparator;
import org.garden.enums.PaxosMemberRole;
import org.garden.enums.PaxosMemberStatus;
import org.garden.handler.UpStreamHandler;
import org.garden.task.HeartBeatProcessor;
import org.garden.util.HostUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author wgt
 * @date 2018-03-26
 * @description 启动中心
 **/
public class GardenManager {

    private static final Logger LOGGER = Logger.getLogger(GardenManager.class);

    /**
     * 初始化
     */
    public void init() throws Exception {

        // 根据配置信息初始化member
        String ip = GardenConfig.serverIp;

        // 选举端口
        int port = GardenConfig.serverPort;

        //集群节点hosts
        String nodesStr = GardenConfig.nodeHosts;

        //初始化本机member 信息
        PaxosMember currentMember = this.initPaxosMember(ip, port, true);
        currentMember.setMemberName(GardenConfig.serverNodeName);

        //初始化成员列表
        List<PaxosMember> clusterMemberList = this.parseNodesToPaxosMember(nodesStr);
        currentMember.setClusterMemberList(clusterMemberList);
        LOGGER.info("clusterMemberList is:" + JSON.toJSONString(clusterMemberList));

        //设置集群结点总数
        int clusterNodesNum = clusterMemberList == null ? 0 : clusterMemberList.size();
        currentMember.setClusterNodesNum(clusterNodesNum);

        //设置选举信息
        ElectionInfo electionInfo = new ElectionInfo();
        electionInfo.setClusterNodesNum(clusterNodesNum);

        //获取当前提议节点序列号
        int currentMemberUniqueProposalSeq = this.processProposalUniqueProposalSeq(clusterMemberList);
        electionInfo.setCurrentMemberUniqueProposalSeq(currentMemberUniqueProposalSeq);
        LOGGER.info("currentMemberUniqueProposalSeq is[" + currentMemberUniqueProposalSeq + "]");

        currentMember.setElectionInfo(electionInfo);


        //设置paxosCore 注册到paxosMember服务中
        PaxosCore paxosCore = new PaxosCoreComponent(); //
        paxosCore.setCurrentPaxosMember(currentMember);
        paxosCore.setOtherPaxosMemberList(this.getOtherPaxMemberList(clusterMemberList));

        //加载选举信息
        ElectionInfoLoader electionInfoLoader = new ElectionInfoLoader(paxosCore);
        electionInfoLoader.loadElectionInfo();

        //开启心跳处理器
        HeartBeatProcessor heartBeatProcessor = new HeartBeatProcessor();
        UpStreamHandler upStreamHandler = new UpStreamHandler(paxosCore);
        heartBeatProcessor.start(upStreamHandler);

        System.out.println("========");
        //开启选举处理器
        UpStreamHandler upStreamHandlerForElection = new UpStreamHandler(paxosCore);
        ElectionProcessor electionProcessor = new ElectionProcessor();
        electionProcessor.start(upStreamHandlerForElection,heartBeatProcessor);

    }

    /**
     * 初始化选举成员节点信息
     *
     * @param ip              ip地址
     * @param port            端口号
     * @param isCurrentMember 是否是当前节点
     * @return
     */
    private PaxosMember initPaxosMember(String ip, int port, boolean isCurrentMember) {
        PaxosMember paxosMember = new PaxosMember();
        paxosMember.setIp(ip);
        paxosMember.setPort(port);
        paxosMember.setIsUp(true);
        paxosMember.setMemberName(GardenConfig.serverNodeName);
        paxosMember.setStatusValue(PaxosMemberStatus.INIT.getStatus());
        paxosMember.setIsCurrentMember(isCurrentMember);

        //判断是否是当前本机节点 如果是默认使用配置属性值
        if (isCurrentMember) {
            paxosMember.setRole(initPaxosMemberRole(GardenConfig.nodeRole));
        } else {
            paxosMember.setRole(PaxosMemberRole.LOOKING);
        }
        paxosMember.setIpAndPort(ip + CodeInfo.IP_AND_PORT_SPLIT + port);
        return paxosMember;
    }

    /**
     * 初始化节点角色信息
     *
     * @param role 角色
     * @return
     */
    private PaxosMemberRole initPaxosMemberRole(String role) {

        PaxosMemberRole paxosMemberRole = null;

        switch (role) {
            case "looking":
                paxosMemberRole = PaxosMemberRole.LOOKING;
                break;
            case "observer":
                paxosMemberRole = PaxosMemberRole.OBSERVER;
                break;
            default:
                break;
        }
        return paxosMemberRole;
    }

    /**
     * 将nodes转换为选举节点
     *
     * @param nodes 节点hosts
     * @return
     */
    private List<PaxosMember> parseNodesToPaxosMember(String nodes) throws Exception {
        if (StringUtils.isEmpty(nodes)) {
            return null;
        }

        List<PaxosMember> paxosMemberList = new ArrayList<PaxosMember>();
        try {
            String[] nodeArr = nodes.split(",");
            if (nodeArr == null || nodeArr.length <= 0) {
                return null;
            }

            for (String nodeStr : nodeArr) {
                String[] nodeIpAndPort = nodeStr.split(":");
                if (nodeIpAndPort == null || nodeIpAndPort.length != 2) {
                    throw new IllegalAccessException("garden.nodes.hosts format exception");
                }

                String ip = nodeIpAndPort[0];
                Integer port = HostUtil.parsePort(nodeIpAndPort[1].split("\\[")[0]);
                LOGGER.info("found node,ip:" + ip + ",port:" + port);

                //初始时，默认其它结点是有效的及状态为初始INIT状态
                PaxosMember paxosMember = this.initPaxosMember(ip, port, false);
                paxosMember.setWeights(Integer.valueOf(nodeIpAndPort[1].split("\\[")[1].replace("]","")));
                paxosMemberList.add(paxosMember);

                if (nodeStr.equals(nodeArr[0])) {
                    //这个结点就是当前结点，需要标识下
                    LOGGER.info("nodes ip and port eq current ip and port,ignore,ip:" + ip + ",port:" + port);
                    paxosMember.setIsCurrentMember(true);
                }

            }

            return paxosMemberList;
        } catch (Exception e) {
            LOGGER.error("parseNodes to paxosMember err", e);
            throw e;
        }

    }

    // 返回当前结点的提议唯一序列
    private int processProposalUniqueProposalSeq(List<PaxosMember> clusterMemberList) {
        //检查是否集群结点为空或1个结点
        if (CollectionUtils.isEmpty(clusterMemberList) || clusterMemberList.size() == 1) {
            // 集群中结点为空，或只有一个结点，提议序列唯一值可以取1
            return 1;
        }

        //有多个结点，将所有结点ip+port进行排序，设置每个结点一个提议时产生提议号的唯一提议序号标识
        Collections.sort(clusterMemberList, new PaxosMemberComparator());

        //排序完成后，只要返回当前结点的提议唯一序列即可
        Integer proposalUniqueSeqForCurrentMember = null;
        for (int i = 0; i < clusterMemberList.size(); i++) {
            PaxosMember paxosMember = clusterMemberList.get(i);
            LOGGER.info("member,ip[" + paxosMember.getIp() + "],port[" + paxosMember.getPort() + "],which proposalUniqueSeq is[" + i + "]");
            if (paxosMember.getIsCurrentMember()) {
                proposalUniqueSeqForCurrentMember = i;
            }
        }

        if (proposalUniqueSeqForCurrentMember == null) {
            throw new IllegalArgumentException("not found,member list:[" + clusterMemberList.toString() + "]");
        }

        return proposalUniqueSeqForCurrentMember;

    }

    //获取其它选举节点列表-除本节点以外的节点
    private List<PaxosMember> getOtherPaxMemberList(List<PaxosMember> clusterMemberList) {
        if (CollectionUtils.isEmpty(clusterMemberList)) {
            return null;
        }

        List<PaxosMember> otherMemberList = new ArrayList<PaxosMember>();
        for (PaxosMember paxosMember : clusterMemberList) {
            if (!paxosMember.getIsCurrentMember()) {
                otherMemberList.add(paxosMember);
            }
        }

        return otherMemberList;
    }

}
