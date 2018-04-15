package org.garden.conf;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.garden.Garden;
import org.garden.util.PropertiesUtil;

import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * @author wgt
 * @date 2018-03-26
 * @description 加载 garden 配置文件
 **/
public class GardenConfig {

    public static final Logger LOGGER = Logger.getLogger(GardenConfig.class);

    //默认超时时间10s
    public static long timeOut = 10000;

    //本节点ip
    public static String serverIp;

    //默认服务选举端口号
    public static int serverPort = 9020;

    //心跳端口=选举端口+1
    public static int heartPort = 9021;

    //本节点名称
    public static String serverNodeName;

    //本节点权重值
    public static int serverWeights = 1;

    //节点角色
    public static String nodeRole = "looking";

    //集群节点
    public static String nodeHosts;

    //数据存放文件
    public static String dataPath = "/data/garden/garden.data";

    static {
        try {
            parse();
        } catch (ConfigException e) {
            LOGGER.error("Error processing garden.properties", e);
        }
    }

    /**
     * 加载配置文件
     *
     * @param
     */
    private static void parse() throws GardenConfig.ConfigException {
        try {
            LOGGER.info("Reading configuration from: garden.properties");
            ClassLoader classLoader = Garden.class.getClassLoader();
            InputStream resourceAsStream = classLoader.getResourceAsStream("garden.properties");
            Map<String, Object> map = PropertiesUtil.readPropertiesFile(resourceAsStream);

            parseProperties(map);
        } catch (Exception e) {
            throw new GardenConfig.ConfigException("Error processing garden.properties", e);
        }
    }

    /**
     * 初始化配置变量
     *
     * @param map
     */
    private static void parseProperties(Map<String, Object> map) throws UnknownHostException {

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String value = entry.getValue().toString().trim();
            if (StringUtils.isNotBlank(value)) {
                switch (entry.getKey()) {
                    case "garden.node.name":
                        serverNodeName = value;
                        break;
                    case "garden.timeout":
                        timeOut = Long.parseLong(value);
                        break;
                    case "garden.port":
                        serverPort = Integer.parseInt(value);
                        break;
                    case "garden.nodes.hosts":
                        nodeHosts = value;
                        setServerIp(nodeHosts);
                        break;
                    case "garden.node.weights":
                        serverWeights = Integer.parseInt(value);
                        break;
                    case "garden.dataPath":
                        dataPath = value;
                        break;
                    case "garden.nodeRole":
                        nodeRole = value;
                        break;
                    default:
                        break;
                }
            }


        }
    }


    private static class ConfigException extends Exception {
        public ConfigException(String msg) {
            super(msg);
        }

        public ConfigException(String msg, Exception e) {
            super(msg, e);
        }
    }

    /**
     * 设置本节点ip
     */
    private static void setServerIp(String nodeHosts) {
        String ipAndPort = nodeHosts.replace("[", "").replace("]", "")
                .split(",")[0];
        serverIp = ipAndPort.split(":")[0];
    }
}
