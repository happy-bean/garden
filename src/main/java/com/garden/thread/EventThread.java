/*
package org.garden.thread;

import org.garden.enums.EventType;
import org.garden.enums.KeeperState;

import java.util.concurrent.LinkedBlockingQueue;

*/
/**
 * @author wgt
 * @date 2018-03-11
 * @description 事件处理线程
 **//*

public class EventThread extends Thread {
    //等待处理的事件
    private final LinkedBlockingQueue<Object> waitingEvents =
            new LinkedBlockingQueue<Object>();

    */
/**
     *这个是真正的排队会话的状态，知道事件处理线程真正处理事件并将其返回给监视器。
     **//*

    private volatile KeeperState sessionState = KeeperState.Disconnected;

    private volatile boolean wasKilled = false;
    private volatile boolean isRunning = false;

    EventThread() {
        // 构造一个线程名
        super(makeThreadName("-EventThread"));
        setUncaughtExceptionHandler(uncaughtExceptionHandler);
        // 设置为守护线程
        setDaemon(true);
    }
    //
    public void queueEvent(WatchedEvent event) {
        // 如果WatchedEvent的类型是None状态是sessionStat的值则不处理
        if (event.getType() == EventType.None
                && sessionState == event.getState()) {
            return;
        }
        // 获取事件的状态
        sessionState = event.getState();

        // 构建一个基于事件的监视器
        WatcherSetEventPair pair = new WatcherSetEventPair(
                watcher.materialize(event.getState(), event.getType(),
                        event.getPath()),
                event);
        // 排队pair，稍后处理
        waitingEvents.add(pair);
    }

    // 排队Packet
    public void queuePacket(Packet packet) {
        if (wasKilled) {
            synchronized (waitingEvents) {
                if (isRunning) {
                    waitingEvents.add(packet);
                }
                else processEvent(packet);
            }
        } else {
            waitingEvents.add(packet);
        }
    }

    public void queueEventOfDeath() {
        waitingEvents.add(eventOfDeath);
    }

    @Override
    public void run() {
        try {
            isRunning = true;
            while (true) {
                //从等待处理的事件队列中获取事件
                Object event = waitingEvents.take();

                if (event == eventOfDeath) {
                    wasKilled = true;
                } else {
                    processEvent(event);
                }
                if (wasKilled)
                    synchronized (waitingEvents) {
                        if (waitingEvents.isEmpty()) {
                            isRunning = false;
                            break;
                        }
                    }
            }
        } catch (InterruptedException e) {
            LOG.error("Event thread exiting due to interruption", e);
        }

        LOG.info("EventThread shut down");
    }

    // 真正处理事件的入口，主要是回调处理
    private void processEvent(Object event) {
        try {
            // 如果事件是WatcherSetEventPair
            if (event instanceof WatcherSetEventPair) {
                //每个监视器都会处理这个事件
                WatcherSetEventPair pair = (WatcherSetEventPair) event;
                for (Watcher watcher : pair.watchers) {
                    try {
                        watcher.process(pair.event);
                    } catch (Throwable t) {
                        LOG.error("Error while calling watcher ", t);
                    }
                }
            } else {
                Packet p = (Packet) event;
                int rc = 0;
                // 获取客户端路径
                String clientPath = p.clientPath;
                if (p.replyHeader.getErr() != 0) {
                    rc = p.replyHeader.getErr();
                }
                if (p.cb == null) {
                    LOG.warn("Somehow a null cb got to EventThread!");
                } else if (p.response instanceof ExistsResponse
                        || p.response instanceof SetDataResponse
                        || p.response instanceof SetACLResponse) {
                    // 获取回调对象
                    StatCallback cb = (StatCallback) p.cb;
                    // 如果处理成功回调方法会传入响应状态，否则响应状态为null
                    if (rc == 0) {
                        if (p.response instanceof ExistsResponse) {
                            cb.processResult(rc, clientPath, p.ctx,
                                    ((ExistsResponse) p.response)
                                            .getStat());
                        } else if (p.response instanceof SetDataResponse) {
                            cb.processResult(rc, clientPath, p.ctx,
                                    ((SetDataResponse) p.response)
                                            .getStat());
                        } else if (p.response instanceof SetACLResponse) {
                            cb.processResult(rc, clientPath, p.ctx,
                                    ((SetACLResponse) p.response)
                                            .getStat());
                        }
                    } else {
                        cb.processResult(rc, clientPath, p.ctx, null);
                    }
                } else if (p.response instanceof GetDataResponse) {
                    DataCallback cb = (DataCallback) p.cb;
                    GetDataResponse rsp = (GetDataResponse) p.response;
                    if (rc == 0) {
                        cb.processResult(rc, clientPath, p.ctx, rsp
                                .getData(), rsp.getStat());
                    } else {
                        cb.processResult(rc, clientPath, p.ctx, null,
                                null);
                    }
                } else if (p.response instanceof GetACLResponse) {
                    ACLCallback cb = (ACLCallback) p.cb;
                    GetACLResponse rsp = (GetACLResponse) p.response;
                    if (rc == 0) {
                        cb.processResult(rc, clientPath, p.ctx, rsp
                                .getAcl(), rsp.getStat());
                    } else {
                        cb.processResult(rc, clientPath, p.ctx, null,
                                null);
                    }
                } else if (p.response instanceof GetChildrenResponse) {
                    ChildrenCallback cb = (ChildrenCallback) p.cb;
                    GetChildrenResponse rsp = (GetChildrenResponse) p.response;
                    if (rc == 0) {
                        cb.processResult(rc, clientPath, p.ctx, rsp
                                .getChildren());
                    } else {
                        cb.processResult(rc, clientPath, p.ctx, null);
                    }
                } else if (p.response instanceof GetChildren2Response) {
                    Children2Callback cb = (Children2Callback) p.cb;
                    GetChildren2Response rsp = (GetChildren2Response) p.response;
                    if (rc == 0) {
                        cb.processResult(rc, clientPath, p.ctx, rsp
                                .getChildren(), rsp.getStat());
                    } else {
                        cb.processResult(rc, clientPath, p.ctx, null, null);
                    }
                } else if (p.response instanceof CreateResponse) {
                    StringCallback cb = (StringCallback) p.cb;
                    CreateResponse rsp = (CreateResponse) p.response;
                    if (rc == 0) {
                        cb.processResult(rc, clientPath, p.ctx,
                                (chrootPath == null
                                        ? rsp.getPath()
                                        : rsp.getPath()
                                        .substring(chrootPath.length())));
                    } else {
                        cb.processResult(rc, clientPath, p.ctx, null);
                    }
                } else if (p.cb instanceof VoidCallback) {
                    VoidCallback cb = (VoidCallback) p.cb;
                    cb.processResult(rc, clientPath, p.ctx);
                }
            }
        } catch (Throwable t) {
            LOG.error("Caught unexpected throwable", t);
        }
    }
}*/
