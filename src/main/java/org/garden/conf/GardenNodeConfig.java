package org.garden.conf;

import org.apache.commons.lang3.StringUtils;
import org.garden.Garden;
import org.garden.nodes.Node;
import org.garden.nodes.ServerNode;
import org.garden.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wgt
 * @date 2018-03-14
 * @description 配置文件加载
 **/
public class GardenNodeConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(GardenNodeConfig.class);

    protected InetSocketAddress clientPortAddress;

    //默认超时时间10s
    protected long timeOut = 10000;

    //默认服务端口号
    protected int serverPort = 9020;

    //本节点名称
    protected String serverNode = "node-";

    //本节点权重值
    protected int serverWeights = 1;

    //默认数据文件路径
    protected String dataDir = "/tmp/garden/data/";

    //默认日志文件路径
    protected String logDir = "/tmp/garden/log/";

    protected final HashMap<Long, ServerNode.QuorumServer> servers =
            new HashMap<>();
    protected final HashMap<Long, ServerNode.QuorumServer> observers =
            new HashMap<>();


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
        String[] hosts;

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
                        hosts = value
                                .replace("[", "")
                                .replace("]", "").split(",");
                        break;
                    case "garden.node.weights":

                }
            }

            //获取本机inetAddress实例
            InetAddress inetAddress = InetAddress.getLocalHost();

            System.out.println("计算机名：" + inetAddress.getHostName());
            System.out.println("IP地址：" + inetAddress.getHostAddress());
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
