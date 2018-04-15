package org.garden.remoting;

import com.alibaba.fastjson.JSON;
import org.garden.core.constants.CodeInfo;
import org.garden.exchange.ExchangeClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author wgt
 * @date 2018-04-12
 * @description
 **/
public class DefaultFuture implements ResponseFuture {

    private Response response;

    private Long id;

    private static final Map<Long, DefaultFuture> futureMap = new ConcurrentHashMap<Long, DefaultFuture>();

    private static final Map<Long, ExchangeClient> exchangeClientMap = new ConcurrentHashMap<Long, ExchangeClient>();

    private volatile boolean done;

    private Lock lock = new ReentrantLock();

    private Condition lockCondition = lock.newCondition();

    private static final int DEFAULT_TIME_OUT_MILLISECONDS = 6000;

    public DefaultFuture(Command command, ExchangeClient exchangeClient) {
        done = false;
        if (command.getCommandType() != CodeInfo.COMMAND_TYPE_REQ) {
            throw new IllegalArgumentException("DefaultFuture,reqType must be COMMAND_TYPE_REQ");
        }

        String body = command.getBody();
        Request remotingRequest = JSON.parseObject(body, Request.class);
        if (remotingRequest == null || remotingRequest.getReqId() == null) {
            throw new IllegalArgumentException("reqId cannot be null");
        }
        id = remotingRequest.getReqId();
        futureMap.put(id, this);
        exchangeClientMap.put(id, exchangeClient);
    }

    private boolean isDone() {
        return this.response != null;
    }

    @Override
    public Object get() throws Exception {
        lock.lock();
        long oldTime = System.currentTimeMillis();
        try {
            if (!done) {
                while (!this.isDone()) {
                    /**
                     * 超时就退出
                     */
                    if ((System.currentTimeMillis() - oldTime) > DEFAULT_TIME_OUT_MILLISECONDS) {
                        break;
                    }
                    lockCondition.await(DEFAULT_TIME_OUT_MILLISECONDS, TimeUnit.MILLISECONDS);
                }

            }

            if (!done) {
                throw new TimeoutException("get timeout");
            }
        } finally {
            lock.unlock();
        }

        return response;
    }

    @Override
    public Object get(long timeout) throws Exception {
        throw new UnsupportedOperationException("暂不支持暂时获取get操作");
    }

    public static void recieve(Response response) {
        long reqId = response.getReqId();
        try {
            DefaultFuture defaultFuture = futureMap.remove(reqId);
            if (defaultFuture != null) {
                defaultFuture.doRecieve(response);
            }
        } finally {
            exchangeClientMap.remove(reqId);
        }
    }

    private void doRecieve(Response res) {
        lock.lock();
        try {
            this.response = res;
            this.lockCondition.signalAll();
            this.done = true;
        } finally {
            lock.unlock();
        }
    }

    public static ExchangeClient getExchangeClientByReqId(long reqId) {
        return exchangeClientMap.get(reqId);

    }

}
