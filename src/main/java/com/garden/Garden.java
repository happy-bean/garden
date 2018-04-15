package com.garden;

import org.apache.log4j.Logger;
import com.garden.server.GardenServerCenter;


/**
 * @author wgt
 * @date 2018-03-11
 * @description 主类
 * 从AutoCloseable的注释可知它的出现是为了更好的管理资源，
 * 准确说是资源的释放，当一个资源类实现了该接口close方法，
 * 在使用try-catch-resources语法创建的资源抛出异常后，JVM会自动调用close 方法进行资源释放
 **/
public class Garden implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(Garden.class);

    private GardenServerCenter gardenServerCenter = new GardenServerCenter();

    public Garden() {

    }

    /**
     * 初始化
     *
     * @param
     */
    public void init() {

    }

    /**
     * 将服务应用注册到 garden 服务管理上
     *
     * @param
     */
    public void register() throws InterruptedException {

        //先启动服务中心
        gardenServerCenter.runServer();
    }

    @Override
    public void close() throws Exception {
        //gardenServerCenter.close();
    }

    /**
     * Register a watcher for a particular path.
     */
  /* public abstract class WatchRegistration {
        private Watcher watcher;
        private String clientPath;

        public WatchRegistration(Watcher watcher, String clientPath) {
            this.watcher = watcher;
            this.clientPath = clientPath;
        }

        abstract protected Map<String, Set<Watcher>> getWatches(int rc);

        *//**
         * Register the watcher with the set of watches on path.
         *
         * @param rc the result code of the operation that attempted to
         *           add the watch on the path.
         *//*
        public void register(int rc) {
            if (shouldAddWatch(rc)) {
                Map<String, Set<Watcher>> watches = getWatches(rc);
                synchronized (watches) {
                    Set<Watcher> watchers = watches.get(clientPath);
                    if (watchers == null) {
                        watchers = new HashSet<Watcher>();
                        watches.put(clientPath, watchers);
                    }
                    watchers.add(watcher);
                }
            }
        }
    }*/
    public static void main(String[] args) throws InterruptedException {
     new Garden().register();
    }
}
