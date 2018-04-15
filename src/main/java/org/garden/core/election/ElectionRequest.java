package org.garden.core.election;

/**
 * @author wgt
 * @date 2018-03-26
 * @description 选举时传送的命令
 **/
public class ElectionRequest extends BaseElectionRequest{

	private static final long serialVersionUID = 1L;

	/**
	 * 选举阶段,1-prepair阶段,2-accept阶段
	 */
	private Integer phase;

	/**
	 * 提议号，集群保证唯一递增
	 */
	private Long num;

	/**
	 * 提议值
	 */
	private Object value;

	public ElectionRequest() {

	}

	public ElectionRequest(int phase, long num, Object value) {
		this.phase = phase;
		this.num = num;
		this.value = value;
	}

	public Integer getPhase() {
		return phase;
	}

	public void setPhase(Integer phase) {
		this.phase = phase;
	}

	public Long getNum() {
		return num;
	}

	public void setNum(Long num) {
		this.num = num;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

}
