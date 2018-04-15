package com.garden.server;

/**
 * @author wgt
 * @date 2018-03-12
 * @description
 **/
public class GardenServerCenter {

    public void runServer(){

        //启动通讯服务
        new GardenSocketServer().start();
    }
}
