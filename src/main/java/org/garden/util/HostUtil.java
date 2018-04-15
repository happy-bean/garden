package org.garden.util;

import org.apache.log4j.Logger;

public class HostUtil {

    private static final Logger LOGGER = Logger.getLogger(HostUtil.class);

    /**
     * 端口号转int
     *
     * @param portStr 端口号
     * @return
     */
    public static int parsePort(String portStr) throws Exception {
        try {
            int port = Integer.valueOf(portStr);
            return port;
        } catch (Exception e) {
            LOGGER.error("parse port err,port:" + portStr);
            throw e;
        }
    }

    /**
     * IP地址转换成十进制整数
     *
     * @param strIp ip
     * @return
     */
    public static long ipToLong(String strIp) {
        long[] ip = new long[4];
        // 先找到IP地址字符串中.的位置
        int position1 = strIp.indexOf(".");
        int position2 = strIp.indexOf(".", position1 + 1);
        int position3 = strIp.indexOf(".", position2 + 1);
        // 将每个.之间的字符串转换成整型
        ip[0] = Long.parseLong(strIp.substring(0, position1));
        ip[1] = Long.parseLong(strIp.substring(position1 + 1, position2));
        ip[2] = Long.parseLong(strIp.substring(position2 + 1, position3));
        ip[3] = Long.parseLong(strIp.substring(position3 + 1));
        return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
    }


    /**
     * 将十进制整数形式转换成ip地址
     *
     * @param longIp ip地址
     * @return
     */
    public static String longToIP(long longIp) {
        StringBuffer sb = new StringBuffer("");
        // 直接右移24位
        sb.append(String.valueOf((longIp >>> 24)));
        sb.append(".");
        // 将高8位置0，然后右移16位
        sb.append(String.valueOf((longIp & 0x00FFFFFF) >>> 16));
        sb.append(".");
        // 将高16位置0，然后右移8位
        sb.append(String.valueOf((longIp & 0x0000FFFF) >>> 8));
        sb.append(".");
        // 将高24位置0
        sb.append(String.valueOf((longIp & 0x000000FF)));
        return sb.toString();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String ipStr = "192.168.0.1";
        long longIp = HostUtil.ipToLong(ipStr);
        System.out.println("192.168.0.1 的整数形式为：" + longIp);
        System.out.println("整数" + longIp + "转化成字符串IP地址：" + HostUtil.longToIP(longIp));

        String ipStr2 = "192.168.14.1";
        long longIp2 = HostUtil.ipToLong(ipStr2);
        System.out.println("192.168.14.1 的整数形式为：" + longIp2);
        System.out.println("整数" + longIp2 + "转化成字符串IP地址：" + HostUtil.longToIP(longIp2));
    }
}
