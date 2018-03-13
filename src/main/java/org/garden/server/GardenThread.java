package org.garden.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wgt
 * @date 2018-03-11
 * @description
 **/
public class GardenThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(GardenThread.class);

    //检测出某个由于未捕获的异常而终结的情况
    private UncaughtExceptionHandler uncaughtExceptionalHandler = new UncaughtExceptionHandler() {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            handleException(t.getName(), e);
        }
    };

    public GardenThread(String threadName) {
        super(threadName);
        setUncaughtExceptionHandler(uncaughtExceptionalHandler);
    }

    protected void handleException(String thName, Throwable e) {
        LOGGER.warn("Exception occurred from thread {}", thName, e);
    }
}
