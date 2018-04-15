package org.garden;

import com.garden.server.GardenServerCenter;
import org.apache.log4j.Logger;
import org.garden.bootstrap.GardenManager;


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
     * 将服务应用注册到 garden 服务管理上
     *
     * @param
     */
    public void register() throws Exception {

        new GardenManager().init();
        //先启动服务中心
        //gardenServerCenter.runServer();
    }

    @Override
    public void close() throws Exception {
        //gardenServerCenter.close();
    }

    public static void main(String[] args) throws Exception {
     new Garden().register();
    }
}
