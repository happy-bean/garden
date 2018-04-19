package org.garden.balance.server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wgt
 * @date 2018-04-18
 * @description 节点健康状态
 **/
public class NodeHealth {

    private static List<String> timeList;

    private static Map<String, LinkedHashMap<String, Integer>> concurrentHashMap = new ConcurrentHashMap<>();

    public static synchronized List<String> getTimeList() {
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

    public static Map<String, LinkedHashMap<String, Integer>> getConcurrentHashMap() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        Iterator<Map.Entry<String, LinkedHashMap<String, Integer>>> iterator = concurrentHashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LinkedHashMap<String, Integer>> next = iterator.next();
            Iterator<Map.Entry<String, Integer>> iterator1 = next.getValue().entrySet().iterator();
            while (iterator1.hasNext()) {
                Map.Entry<String, Integer> next1 = iterator1.next();
                try {
                    Date begin = format.parse(next1.getKey());
                    long between = (date.getTime() - begin.getTime());
                    long day = between / (24 * 60 * 60 * 1000);
                    long hour = (between / (60 * 60 * 1000) - day * 24);
                    long min = ((between / (60 * 1000)) - day * 24 * 60 - hour * 60);
                    long s = (between / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);

                    if (s > 7) {
                        concurrentHashMap.get(next.getKey()).put(next1.getKey(),0);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        return concurrentHashMap;
    }

    public static void addConcurrent(String ip) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String timeKey = format.format(date);

        try {
            if (concurrentHashMap.get(ip) != null) {
                Integer tmp = concurrentHashMap.get(ip).get(timeKey);
                if (tmp != null) {
                    tmp++;
                    concurrentHashMap.get(ip).put(timeKey, tmp);
                } else {
                    if (concurrentHashMap.get(ip).size() < 7) {
                        concurrentHashMap.get(ip).put(timeKey, 1);
                    } else {
                        Iterator it = concurrentHashMap.get(ip).entrySet().iterator();
                        if (it.hasNext()) {
                            Map.Entry entity = (Map.Entry) it.next();
                            concurrentHashMap.get(ip).remove(entity.getKey());
                        }
                        concurrentHashMap.get(ip).put(timeKey, 1);
                    }
                }
            } else {
                LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<>();
                linkedHashMap.put(timeKey, 1);
                concurrentHashMap.put(ip, linkedHashMap);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
