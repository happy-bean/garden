package org.garden.nodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @author wgt
 * @date 2018-03-15
 * @description 记录当前服务器各种信息,然后是一个线程，不断地完成leader选举
 **/
public class ServerNode extends Thread implements NodeStats.Provider{

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerNode.class);

    public static class QuorumServer {
        public QuorumServer(long id, InetSocketAddress addr,
                            InetSocketAddress electionAddr) {
            this.id = id;
            this.addr = addr;
            this.electionAddr = electionAddr;
        }

        public QuorumServer(long id, InetSocketAddress addr) {
            this.id = id;
            this.addr = addr;
            this.electionAddr = null;
        }

        public QuorumServer(long id, InetSocketAddress addr,
                            InetSocketAddress electionAddr, LearnerType type) {
            this.id = id;
            this.addr = addr;
            this.electionAddr = electionAddr;
            this.type = type;
        }

        public InetSocketAddress addr;

        public InetSocketAddress electionAddr;

        public long id;

        public LearnerType type = LearnerType.PARTICIPANT;
    }

    public enum ServerState {
        LOOKING, FOLLOWING, LEADING, OBSERVING;
    }

    /*
     * A peer can either be participating, which implies that it is willing to
     * both vote in instances of consensus and to elect or become a Leader, or
     * it may be observing in which case it isn't.
     *
     * We need this distinction to decide which ServerState to move to when
     * conditions change (e.g. which state to become after LOOKING).
     */
    public enum LearnerType {
        PARTICIPANT, OBSERVER;
    }

    @Override
    public String[] getServerNodes() {
        return new String[0];
    }

    @Override
    public String getServerState() {
        return null;
    }


}
