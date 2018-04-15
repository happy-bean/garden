package org.garden.util;

import java.util.Random;

/**
 * @author wgt
 * @date 2018-04-12
 * @description 选举冲突工具
 **/
public class PaxosConflictionUtil {

    //默认间隔时间
    public static final long DEFALT_INTERVAL = 1000;

    //每轮选举间隔时间,之后有随机
    public static volatile long electionIntervalBetweenRound = DEFALT_INTERVAL;

    //最小每轮选举间隔时间 毫秒
    public static final long MIN_ELECTION_INTERVAL_BETWEEN_ROUND_MILLSECONDS = 30000;

    //最大每轮选举间隔时间 毫秒
    public static final long MAX_ELECTION_INTERVAL_BETWEEN_ROUND_MILLSECONDS = 60000;

    //随机最大选举间隔 秒
    public static final int RANDOM_MAX_ELECTION_INTERVAL_SECONDS = 30;

    //获取随机选举间隔时间
    public static long getRandomElectionIntervalTime() {
        Random random = new Random();
        long resInMillSeconds = random.nextInt(RANDOM_MAX_ELECTION_INTERVAL_SECONDS) * 1000
                + MIN_ELECTION_INTERVAL_BETWEEN_ROUND_MILLSECONDS;

        long res = resInMillSeconds;
        if (res > MAX_ELECTION_INTERVAL_BETWEEN_ROUND_MILLSECONDS) {
            res = MAX_ELECTION_INTERVAL_BETWEEN_ROUND_MILLSECONDS;
        }

        if (res < MIN_ELECTION_INTERVAL_BETWEEN_ROUND_MILLSECONDS) {
            res = MIN_ELECTION_INTERVAL_BETWEEN_ROUND_MILLSECONDS;
        }

        return res;
    }

}
