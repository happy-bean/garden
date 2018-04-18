package org.garden.balance.tactics;

import java.util.List;

/**
 * @author wgt
 * @date 2018-04-17
 * @description 随机（Random）法
 **/
public class Random {
	public synchronized static String getServer(List<String> serverList,List<String> portList)
	{
        java.util.Random random = new java.util.Random();  
        int randomPos = random.nextInt(serverList.size());
        return serverList.get(randomPos)+":"+portList.get(randomPos);
    }  
}
