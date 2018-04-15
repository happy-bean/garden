package org.garden.core.constants;

/**
 * @author wgt
 * @date 2018-03-26
 * @description code信息
 **/
public class CodeInfo {

    public static final int MAX_MSG_SIZE = 1024 * 1024;

    //版本号
    public static final byte VERSION = 1;

    /**
     * 默认选举监听端口
     */
    public static final int DEFAULT_LISTENER_ELECTION_PORT = 20000;

    //请求与响应类型
    /**
     * 请求指令类型
     */
    public static final byte COMMAND_TYPE_REQ = 1;

    /**
     * 响应指令类型
     */
    public static final byte COMMAND_TYPE_RES = 2;

    // 具体业务请求与响应类型
    /**
     * 心跳的请求类型
     */
    public static final byte REQ_TYPE_HEART_BEAT = 1;

    /**
     * 一阶段选举提议请求类型
     */
    public static final byte REQ_TYPE_ELECTION_FIRST_PHASE = 2;

    /**
     * 二阶段选举accept请求类型
     */
    public static final byte REQ_TYPE_ELECTION_SECOND_PHASE = 3;

    /**
     * 选举完成后的广播请求
     */
    public static final byte REQ_TYPE_ELECTION_RESULT_TO_LEANER = 4;

    /**
     * 心跳的响应类型
     */
    public static final byte RES_TYPE_HEART_BEAT = 1;

    /**
     * 一阶段选举提议请求类型
     */
    public static final byte RES_TYPE_ELECTION_FIRST_PHASE = 2;

    /**
     * 二阶段选举accept请求类型
     */
    public static final byte RES_TYPE_ELECTION_SECOND_PHASE = 3;

    /**
     * 选举完成后的广播发给接受者，接受者返回的响应结果
     */
    public static final byte RES_TYPE_ELECTION_RESULT_TO_LEANER = 4;

    //具体选举业务请求与响应类型

    //查询信息
    public static final byte REQ_TYPE_SEARCH_INFO = 5;

    public static final byte RES_TYPE_SEARCH_INFO = 5;


    public static final String IP_AND_PORT_SPLIT = ":";

    //心跳请求确定
    public static final byte HEATBEAT_RES_OK = 1;

    public static final byte HEARTBEAT_RES_ERR = -1;

    /**
     * 默认连接保活超时时间，如果超过这个时间还连不通，则关闭当前连接，然后重连 5秒
     */
    public static final long DEFAULT_CHANNEL_LIVE_TIME_OUT_MILLISECONDS = 5000;

    //选举响应常量
    public static final String ACCEPT_CODE = "accept";

    //拒绝选举
    public static final String DENY_CODE = "deny";

    // 由于接受者已经处于选举完成状态，所以拒绝提议者
    public static final String DENY_CODE_FOR_HAS_LEADER = "deny_for_has_leader";

    // 代表发送时，通信异常或其它环境异常
    public static final String SEND_ERR_CODE = "error";

    public static final int FIRST_PHASE_SEQ = 1;

    //第二次选举
    public static final int SECOND_PHASE_SEQ = 2;

    //心跳常量
    public static final String HEART_BEAT_OK = "ok";

    //失败
    public static final int FAIL_CODE = -1;

    //成功
    public static final int SUCCESS_CODE = 1;

    public static final String UTF_8 = "UTF-8";

    // 初始选举相关值
    // 提议者提议次数初始值
    public static final long INIT_PROPOSAL_ROUND = 1;

    // 提议者提议号初始值
    public static final int INIT_PROPOSAL_NUM = 0;

    // 选举轮数初始值
    public static final long INIT_ELECTION_ROUND_NUM = 1;

    // 接收到的一阶段最大提议号
    public static final long INIT_MAX_ACCEPT_FIRST_PHASE_NUM = -1;

    // 接收到的二阶段最大提议号
    public static final long INIT_MAX_ACCEPT_SECOND_PHASE_NUM = -1;

}
