package org.garden.balance.server;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wgt
 * @date 2018-04-18
 * @description 节点健康状态
 **/
public class NodeHealth {

    private List<String> timeList;

    private Map<String, List<Integer>> concurrentHashMap = new ConcurrentHashMap<>();

    public synchronized List<String> getTimeList() {
        timeList = new ArrayList<>(7);
        SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        for (int i = 0; i >= -6; i--) {
            calendar.add(Calendar.SECOND, -1);
            Date date1 = calendar.getTime();
            calendar.setTime(date1);
            String dataStr = format.format(date1);
            timeList.add(dataStr);
        }
        Collections.reverse(timeList);
        return timeList;
    }

    public void setTimeList(List<String> timeList) {
        this.timeList = timeList;
    }

    public Map<String, List<Integer>> getConcurrentHashMap() {
        return concurrentHashMap;
    }

    public synchronized void addConcurrent(String ip) {
        concurrentHashMap.get(ip);
    }

    public static void main(String[] args) {

    }


}
