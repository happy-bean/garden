package org.garden.core.paxos;

import org.garden.core.election.ElectionInfo;
import org.garden.enums.PaxosMemberRole;
import org.garden.enums.PaxosMemberStatus;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wgt
 * @date 2018-03-26
 * @description 选举成员信息
 **/
public class PaxosMember implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 是否本结点
	 */
	private Boolean isCurrentMember;

	/**
	 * 成员唯一名称
	 */
	private String memberName;

	/**
	 * 成员ip地址
	 */
	private String ip;

	/**
	 * ip端口号
	 */
	private Integer port;

	/**
	 * 成员状态,0-初始化，1-选举中 2-表示选举完成
	 *
	 * 使用Integer的时候，必须加上synchronized保证不会出现并发线程同时访问的情况，
	 * 而在AtomicInteger中却不用加上synchronized，在这里AtomicInteger是提供原子操作的
	 */
	private AtomicInteger status=new AtomicInteger(PaxosMemberStatus.INIT.getStatus());

	/**
	 * 是否可用，表示该结点有没有挂掉，true为可用,false挂了
	 */
	private Boolean isUp;

	/**
	 * 角色，选举完成后主要区分为leader跟follower
	 * 1-leader,2-follower
	 */
	private volatile PaxosMemberRole role;

	/**
	 * 对应leader
	 */
	private PaxosMember leaderMember;

	/**
	 * 该成员选举信息，即有该成员作为提议者的信息也有该成员作为接收者的信息
	 */
	private ElectionInfo electionInfo;

	/**
	 * 整个集群结点集合，包含当前结点
	 */
	private List<PaxosMember> clusterMemberList;

	/**
	 * 整个集群结点数
	 */
	private Integer clusterNodesNum;

	private String ipAndPort;

	public Boolean getIsCurrentMember() {
		return isCurrentMember;
	}

	public void setIsCurrentMember(Boolean isCurrentMember) {
		this.isCurrentMember = isCurrentMember;
	}

	public String getMemberName() {
		return memberName;
	}

	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public AtomicInteger getStatus() {
		return status;
	}

	public void setStatusValue(int statusValue) {
		status.set(statusValue);
	}

	public Boolean getIsUp() {
		return isUp;
	}

	public void setIsUp(Boolean isUp) {
		this.isUp = isUp;
	}

	public PaxosMemberRole getRole() {
		return role;
	}

	public void setRole(PaxosMemberRole role) {
		this.role = role;
	}

	public PaxosMember getLeaderMember() {
		return leaderMember;
	}

	public void setLeaderMember(PaxosMember leaderMember) {
		this.leaderMember = leaderMember;
	}

	public ElectionInfo getElectionInfo() {
		return electionInfo;
	}

	public void setElectionInfo(ElectionInfo electionInfo) {
		this.electionInfo = electionInfo;
	}

	public List<PaxosMember> getClusterMemberList() {
		return clusterMemberList;
	}

	public void setClusterMemberList(List<PaxosMember> clusterMemberList) {
		this.clusterMemberList = clusterMemberList;
	}

	public Integer getClusterNodesNum() {
		return clusterNodesNum;
	}

	public void setClusterNodesNum(Integer clusterNodesNum) {
		this.clusterNodesNum = clusterNodesNum;
	}

	public String getIpAndPort() {
		return ipAndPort;
	}

	public void setIpAndPort(String ipAndPort) {
		this.ipAndPort = ipAndPort;
	}

}
