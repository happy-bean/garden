package org.garden.balance.tactics;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wgt
 * @date 2018-04-17
 * @description 加权源地址哈希（Weight Hash）法
 **/
public class WeightHash {
    /**
     * @param hashCode 在Web应用中可通过HttpServlet的getRemoteIp方法获取
     * @return
     */
    public synchronized static String getServer(List<String> serverList, List<String> portList, List<Integer> weightList, int hashCode) {
        List<String> newServerList = new ArrayList<String>();
        for (int i = 0; i < serverList.size(); i++) {
            int weight = weightList.get(i);
            for (int j = 0; j < weight; j++) {
                newServerList.add(serverList.get(i) + ":" + portList.get(i));
            }
        }
        int serverListSize = serverList.size();
        int serverPos = Math.abs(hashCode) % serverListSize;
        return serverList.get(serverPos);
    }
}
