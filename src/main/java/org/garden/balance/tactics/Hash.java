package org.garden.balance.tactics;

import java.util.List;

/**
 * @author wgt
 * @date 2018-04-17
 * @description 源地址哈希（Hash）法
 **/
public class Hash 
{
	public synchronized static String getServer(List<String> serverList,List<String> portList,int hashCode)
    {
        int serverListSize = serverList.size();
        int serverPos =   Math.abs(hashCode) % serverListSize;
        return serverList.get(serverPos)+":"+portList.get(serverPos);
    }
}
