package com.garden.thread;

import org.apache.log4j.Logger;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.File;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * @author wgt
 * @date 2018-03-24
 * @description
 **/
public class DirCleanUpManager extends Thread {

    private static final Logger LOGGER = Logger.getLogger(DirCleanUpManager.class);

    //默认数据文件路径
    private String dataDir = "/tmp/garden/data/";

    //默认日志文件路径
    private String logDir = "/tmp/garden/log/";

    @Override
    public void run() {
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            JobDetail job = JobBuilder.newJob(DirClean.class)
                    .withIdentity("job", "dirclean-group").build();

            // 休眠时长可指定时间单位，此处使用秒作为单位（withIntervalInSeconds）
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger", "dirclean-group").startNow()
                    .withSchedule(simpleSchedule().withIntervalInHours(24).repeatForever())
                    .build();

            scheduler.scheduleJob(job, trigger);

        } catch (SchedulerException se) {
            LOGGER.error("data dir clean job run failed ...", se);
        }
    }


    class DirClean implements Job {

        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

            File logFile = new File(logDir);
            dirExists(logFile);
            cleanUpLog(logFile);

            File dataFile = new File(dataDir);
            dirExists(dataFile);
            cleanData(dataFile);
        }

        /**
         * 清除除了今天的日志
         */
        private void cleanUpLog(File logDir) {

            String todayLogName = "";

            if (logDir.isDirectory()) {
                //获取文件夹中的文件集合
                File[] logs = logDir.listFiles();
                //遍历集合
                for (File log : logs) {

                    if (!todayLogName.equals(log.getName())) {
                        //执行删除方法
                        log.delete();
                    }

                }
            }
        }

        /**
         * 清除除了今天的数据
         */
        private void cleanData(File dataDir) {
            String todayDataName = "";

            if (dataDir.isDirectory()) {
                //获取文件夹中的文件集合
                File[] datas = dataDir.listFiles();
                //遍历集合
                for (File data : datas) {

                    if (!todayDataName.equals(data.getName())) {
                        //执行删除方法
                        data.delete();
                    }

                }
            }
        }


        /**
         * 判断文件夹是否存在
         */
        public void dirExists(File file) {

            if (file.exists()) {
                if (file.isDirectory()) {
                } else {
                    LOGGER.warn("the same name file exists, can not create dir");
                }
            } else {
                LOGGER.warn("dir not exists, create it ...");
                file.mkdir();
            }

        }

    }

}
