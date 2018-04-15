package org.garden.remoting;

/**
 * @author wgt
 * @date 2018-04-12
 * @description 远程连接客户端
 **/
public interface RemotingClient {

	public boolean connect(String ip, Integer port);

	public void stop();
	
	/**
	 * 同步发送
	 * @param msg
	 * @return
	 */
	public Object sendSync(Object msg);
	
	/**
	 * 异步发送
	 * @param msg
	 */
	public void sendAsyncSync(Object msg);
	
	
	/**
	 * TODO 几个方法整合下
	 */
	public void send(Object msg);

}
