package org.garden.nodes;

/**
 * @author wgt
 * @date 2018-03-15
 * @description 服务器运行时的统计器
 **/
public class NodeStats {

    private final Provider provider;

    public interface Provider {
        static public final String UNKNOWN_STATE = "unknown";
        static public final String LOOKING_STATE = "leaderelection";
        static public final String LEADING_STATE = "leading";
        static public final String FOLLOWING_STATE = "following";
        static public final String OBSERVING_STATE = "observing";

        public String[] getServerNodes();

        public String getServerState();
    }

    protected NodeStats(Provider provider) {
        this.provider = provider;
    }

    public String getServerState() {
        return provider.getServerState();
    }

    /**
     * 获取服务器集群节点信息
     */
    public String[] getServerNodes() {
        return provider.getServerNodes();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        String state = getServerState();
        if (state.equals(Provider.LEADING_STATE)) {
            sb.append("Followers:");
            for (String f : getServerNodes()) {
                sb.append(" ").append(f);
            }
            sb.append("\n");
        } else if (state.equals(Provider.FOLLOWING_STATE)
                || state.equals(Provider.OBSERVING_STATE)) {
            sb.append("Leader: ");
            String[] ldr = getServerNodes();
            if (ldr.length > 0) {
                sb.append(ldr[0]);
            } else {
                sb.append("not connected");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
