package org.garden.balance.server;


import org.garden.balance.tactics.*;

import java.util.List;

/**
 * @author wgt
 * @date 2018-03-29
 * @description 负载均衡
 **/
public class LoadBalance {
    /*
     * 注入配置文件类
     */
    private static NodeProperties nodeProperties = new NodeProperties();

    public synchronized String getServer(int hashCode) {
        String type = nodeProperties.getType();
        List<String> serverList = nodeProperties.getServers();
        List<String> portList = nodeProperties.getPorts();
        List<Integer> weightList = nodeProperties.getWeights();
        String server = null;
        if ("hash".equals(type)) {
            server = Hash.getServer(serverList, portList, hashCode);
        } else if ("random".equals(type)) {
            server = Random.getServer(serverList, portList);
        } else if ("roundRobin".equals(type)) {
            server = RoundRobin.getServer(serverList, portList);
        } else if ("weightHash".equals(type)) {
            server = WeightHash.getServer(serverList, portList, weightList, hashCode);
        } else if ("weightRandom".equals(type)) {
            server = WeightRandom.getServer(serverList, portList, weightList);
        } else if ("weightRoundRobin".equals(type)) {
            server = WeightRoundRobin.getServer(serverList, portList, weightList);
        } else if ("consistencyHash".equals(type)) {
            server = ConsistencyHash.getServer(serverList, portList);
        } else {
            server = Random.getServer(serverList, portList);
        }
        return server;
    }
}
