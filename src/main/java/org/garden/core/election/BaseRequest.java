package org.garden.core.election;

import java.io.Serializable;

/**
 * @author wgt
 * @date 2018-03-26
 * @description 请求基类
 **/
public class BaseRequest implements Serializable {

	private static final long serialVersionUID = 8961245632L;

	/**
	 * 选举轮数
	 */
	private Long electionRound;

	public Long getElectionRound() {
		return electionRound;
	}

	public void setElectionRound(Long electionRound) {
		this.electionRound = electionRound;
	}

}
