package org.garden.balance.tactics;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @author wgt
 * @date 2018-04-19
 * @description 一致性hash
 **/
public class ConsistencyHash {
    private static TreeMap<Long, String> nodes = null;
    //真实服务器节点信息
    private static List<String> shards = new ArrayList();
    //设置虚拟节点数目
    private static int VIRTUAL_NUM = 4;

    /**
     * 初始化一致环
     */
    private static void init() {
        nodes = new TreeMap<Long, String>();
        for (int i = 0; i < shards.size(); i++) {
            String shardInfo = shards.get(i);
            for (int j = 0; j < VIRTUAL_NUM; j++) {
                nodes.put(hash(computeMd5("SHARD-" + i + "-NODE-" + j), j), shardInfo);
            }
        }
    }

    public synchronized static String getServer(List<String> serverList, List<String> portList) {
        shards = serverList;
        init();
        java.util.Random random = new java.util.Random();
        String ip = getShardInfo(hash(computeMd5(String.valueOf(random.nextInt())), random.nextInt(VIRTUAL_NUM)));
        int i=0;
        for(;i<serverList.size();i++){
            if(ip.equals(serverList.get(i))){
                break;
            }
        }
        return ip+":"+portList.get(i);
    }

    /**
     * 根据key的hash值取得服务器节点信息
     *
     * @param hash
     * @return
     */
    private static String getShardInfo(long hash) {
        Long key = hash;
        SortedMap<Long, String> tailMap = nodes.tailMap(key);
        if (tailMap.isEmpty()) {
            key = nodes.firstKey();
        } else {
            key = tailMap.firstKey();
        }
        return nodes.get(key);
    }

    /**
     * 根据2^32把节点分布到圆环上面。
     *
     * @param digest
     * @param nTime
     * @return
     */
    private static long hash(byte[] digest, int nTime) {
        long rv = ((long) (digest[3 + nTime * 4] & 0xFF) << 24)
                | ((long) (digest[2 + nTime * 4] & 0xFF) << 16)
                | ((long) (digest[1 + nTime * 4] & 0xFF) << 8)
                | (digest[0 + nTime * 4] & 0xFF);

        return rv & 0xffffffffL; /* Truncate to 32-bits */
    }

    /**
     * Get the md5 of the given key.
     * 计算MD5值
     */
    private static byte[] computeMd5(String k) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        md5.reset();
        byte[] keyBytes = null;
        try {
            keyBytes = k.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown string :" + k, e);
        }

        md5.update(keyBytes);
        return md5.digest();
    }
}