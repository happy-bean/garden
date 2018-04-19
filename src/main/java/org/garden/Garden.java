package org.garden;

import org.apache.log4j.Logger;
import org.garden.balance.server.BalanceServer;
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

    private boolean balance = false;

    private boolean paxos =false;

    public Garden() {

    }

    /**
     * 将服务应用注册到 garden 服务管理上
     *
     * @param
     */
    public void register() throws Exception {
        //负载均衡服务
        if(balance) {
            new BalanceServer("balance").start();
        }

        //初始化服务
        if(paxos) {
            new GardenManager().init();
        }
    }

    @Override
    public void close() throws Exception {
        //gardenServerCenter.close();
    }

    public boolean isBalance() {
        return balance;
    }

    public Garden setBalance(boolean balance) {
        this.balance = balance;
        return this;
    }

    public boolean isPaxos() {
        return paxos;
    }

    public Garden setPaxos(boolean paxos) {
        this.paxos = paxos;
        return this;
    }

    public static void main(String[] args) throws Exception {

        //true表示启动该服务
        new Garden().setBalance(true).setPaxos(true).register();
    }
}
