package org.garden.balance.server;

import org.garden.Garden;
import org.garden.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author wgt
 * @date 2018-03-29
 * @description node节点配置
 **/
public class NodeProperties {
    public static Logger LOGGER = LoggerFactory.getLogger(NodeProperties.class);

    private String type;
    private List<String> servers = new ArrayList<String>();
    private List<String> ports = new ArrayList<String>();
    private List<Integer> weights = new ArrayList<Integer>();


    public NodeProperties() {
        init();
    }

    private void init() {
        try {
            LOGGER.info("reading configuration from: loadbalance.properties");
            ClassLoader classLoader = Garden.class.getClassLoader();
            InputStream resourceAsStream = classLoader.getResourceAsStream("loadbalance.properties");
            Map<String, Object> map = PropertiesUtil.readPropertiesFile(resourceAsStream);

            parseProperties(map);
        } catch (Exception e) {
            throw new ConfigException("Error processing garden.properties", e);
        }
    }

    /**
     * 初始化配置变量
     *
     * @param map
     */
    public void parseProperties(Map<String, Object> map) {
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            String key = entry.getKey().split("\\[")[0];
            switch (key) {
                case "loadbalance.type":
                    type = String.valueOf(entry.getValue());
                    break;
                case "loadbalance.servers":
                    servers.add(String.valueOf(entry.getValue()));
                    break;
                case "loadbalance.ports":
                    ports.add(String.valueOf(entry.getValue()));
                    break;
                case "loadbalance.weights":
                    weights.add(Integer.valueOf(String.valueOf(entry.getValue())));
                    break;
                default:
                    break;
            }
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public List<String> getPorts() {
        return ports;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    public List<Integer> getWeights() {
        return weights;
    }

    public void setWeights(List<Integer> weights) {
        this.weights = weights;
    }

    public static class ConfigException extends RuntimeException {
        public ConfigException(String msg) {
            super(msg);
        }

        public ConfigException(String msg, Exception e) {
            super(msg, e);
        }
    }
}
