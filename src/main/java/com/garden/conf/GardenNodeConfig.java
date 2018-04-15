package com.garden.conf;

import com.garden.Garden;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import com.garden.nodes.Node;
import com.garden.nodes.NodesManager;
import org.garden.util.PropertiesUtil;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * @author wgt
 * @date 2018-03-14
 * @description 配置文件加载
 **/
public class GardenNodeConfig {

    private static final Logger LOGGER = Logger.getLogger(GardenNodeConfig.class);

    protected InetSocketAddress clientPortAddress;

    //默认超时时间10s
    protected long timeOut = 10000;

    //默认服务端口号
    protected int serverPort = 9020;

    //本节点名称
    protected String serverNodeName;

    //本节点权重值
    protected int serverWeights = 1;

    /**
     * 本机节点信息
     */
    protected static Node localNode;

    /**
     * 加载配置文件
     *
     * @param
     */
    public void parse() throws ConfigException {
        try {
            LOGGER.info("Reading configuration from: garden.properties");
            ClassLoader classLoader = Garden.class.getClassLoader();
            InputStream resourceAsStream = classLoader.getResourceAsStream("garden.properties");
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
    public void parseProperties(Map<String, Object> map) throws UnknownHostException {

        //集群节点信息
        String[] nodeHosts = new String[0];

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String value = entry.getValue().toString().trim();
            if (StringUtils.isNotBlank(value)) {
                switch (entry.getKey()) {
                    case "garden.timeout":
                        timeOut = Long.parseLong(value);
                        break;
                    case "garden.port":
                        serverPort = Integer.parseInt(value);
                        break;
                    case "garden.nodes.hosts":
                        nodeHosts = value
                                .replace("[", "")
                                .replace("]", "").split(",");
                        break;
                    case "garden.node.weights":
                        serverWeights = Integer.parseInt(value);
                        break;
                    default:
                        break;
                }
            }

            //获取本机inetAddress实例
            InetSocketAddress inetSocketAddress;

            localNode = new Node();
            inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost(), serverPort);

            localNode.setHealth(Node.Health.GREEN);
            if (serverNodeName != null) {
                localNode.setName(serverNodeName);
            } else {
                localNode.setName(inetSocketAddress.getHostName());
            }

            localNode.setNodeId(inetSocketAddress.hashCode());
            localNode.setTime(System.currentTimeMillis());
            localNode.setIdentity(Node.dentity.LOOKING_STATE);
            localNode.setInetSocketAddress(inetSocketAddress);

            //初始化本机节点
            NodesManager.initLocalNode(localNode);

            //初始化服务节点
            for(String address :nodeHosts) {
                Node node = new Node();

                InetAddress inetAddress
                        = InetAddress
                        .getByAddress(address.split(":")[0].getBytes());

                InetSocketAddress socketAddress
                        = new InetSocketAddress(inetAddress, Integer.parseInt(address.split(":")[1]));
                node.setInetSocketAddress(socketAddress);
                NodesManager.addNode(node);
            }

        }
    }

    public static class ConfigException extends Exception {
        public ConfigException(String msg) {
            super(msg);
        }

        public ConfigException(String msg, Exception e) {
            super(msg, e);
        }
    }

}
