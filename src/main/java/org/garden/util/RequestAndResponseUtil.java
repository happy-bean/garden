package org.garden.util;

import com.alibaba.fastjson.JSON;
import org.garden.core.constants.CodeInfo;
import org.garden.core.election.*;
import org.garden.remoting.*;

/**
 * @author wgt
 * @date 2018-04-11
 * @description 请求响应工具
 **/
public class RequestAndResponseUtil {

    //构成查找请求体
    public static Command composeSearchInfoRequest(long reqId) {
        //远程通信请求指令类型
        Command remotingCommand = new Command(CodeInfo.COMMAND_TYPE_REQ);

        //请求模板
        Request remotingRequest = new Request(reqId);
        //查询信息
        remotingRequest.setType(CodeInfo.REQ_TYPE_SEARCH_INFO);
        //json格式
        remotingCommand.setBody(JSON.toJSONString(remotingRequest));

        return remotingCommand;
    }

    //构成查找响应体
    public static Command composeSearchInfoResponse(long reqId, SearchInfoResponse searchInfoResponse) {
        //远程通信响应指令类型
        Command remotingCommand = new Command(CodeInfo.COMMAND_TYPE_RES);

        //响应模板
        Response remotingResponse = new Response();
        //请求id
        remotingResponse.setReqId(reqId);
        remotingResponse.setData(JSON.toJSONString(searchInfoResponse));

        remotingCommand.setBody(JSON.toJSONString(remotingResponse));
        return remotingCommand;
    }

    /**
     * 构建选举结果,底层会分别根据业务类型及election结果构造<br/>
     * 目前支持：一阶段结果，二阶段结果，及接受选举结果返回的响应
     *
     * @param reqId
     * @param bizType
     * @param electionResponse
     * @return
     */
    public static Command composeElectionResponseRemotingCommand(long reqId, byte bizType, BaseElectionResponse electionResponse) {
        Command remotingCommand = null;
        //一阶段选举提议请求类型
        if (CodeInfo.REQ_TYPE_ELECTION_FIRST_PHASE == bizType) {
            remotingCommand = RequestAndResponseUtil.composeFirstPahseElectionResponseCommand(reqId, electionResponse);
        }
        //二阶段选举accept请求类型
        else if (CodeInfo.REQ_TYPE_ELECTION_SECOND_PHASE == bizType) {
            remotingCommand = RequestAndResponseUtil.composeSecondPahseElectionResponseCommand(reqId, electionResponse);
        }
        //选举完成后的广播请求
        else if (CodeInfo.REQ_TYPE_ELECTION_RESULT_TO_LEANER == bizType) {
            remotingCommand = composeElectionResultResponseCommand(reqId, electionResponse);
        } else {
            throw new IllegalArgumentException("不支持的选举阶段类型,type[" + bizType + "]");
        }
        return remotingCommand;
    }

    //组装第一次选举响应命令
    public static Command composeFirstPahseElectionResponseCommand(long reqId, BaseElectionResponse electionResponse) {
        //响应指令类型
        Command remotingCommand = new Command(CodeInfo.COMMAND_TYPE_RES);
        //请求模板
        Request remotingRequest = new Request(reqId);
        //一阶段选举提议请求类型
        remotingRequest.setType(CodeInfo.RES_TYPE_ELECTION_FIRST_PHASE);
        remotingRequest.setData(JSON.toJSONString(electionResponse));
        remotingCommand.setBody(JSON.toJSONString(remotingRequest));
        return remotingCommand;
    }

    //构建第二次选举响应命令
    public static Command composeSecondPahseElectionResponseCommand(long reqId, BaseElectionResponse electionResponse) {
        //响应指令类型
        Command remotingCommand = new Command(CodeInfo.COMMAND_TYPE_RES);
        Request remotingRequest = new Request(reqId);
        //二阶段选举accept请求类型
        remotingRequest.setType(CodeInfo.RES_TYPE_ELECTION_SECOND_PHASE);

        // 实际业务一阶段选举响应结果
        remotingRequest.setData(JSON.toJSONString(electionResponse));
        remotingCommand.setBody(JSON.toJSONString(remotingRequest));
        return remotingCommand;
    }

    /**
     * 构造选举请求
     *
     * @param round 选举轮数
     * @param phase 第几阶段,CodeInfo.FIRST_PHASE_SEQ或CodeInfo.SECOND_PHASE_SEQ
     * @param num
     * @param value
     * @return
     */
    public static Command composeElectionRequest(long round, int phase, long num, Object value) {
        String logStr = "phase[" + phase + "],num[" + num + "],value[" + value + "]";
        //第一 二次选举请求
        if (phase != CodeInfo.FIRST_PHASE_SEQ && phase != CodeInfo.SECOND_PHASE_SEQ) {
            throw new IllegalArgumentException("composeElectionRequest err,phase must be 1 or 2," + logStr);
        }
        //请求指令类型
        Command command = new Command(CodeInfo.COMMAND_TYPE_REQ);

        Request remotingRequest = new Request(Request.getAndIncreaseReq());
        ElectionRequest electionRequest = new ElectionRequest(phase, num, value);
        electionRequest.setElectionRound(round);
        remotingRequest.setData(JSON.toJSONString(electionRequest));

        //第一次选举
        if (phase == CodeInfo.FIRST_PHASE_SEQ) {
            //一阶段选举提议请求类型
            remotingRequest.setType(CodeInfo.REQ_TYPE_ELECTION_FIRST_PHASE);
        } else {
            //二阶段选举accept请求类型
            remotingRequest.setType(CodeInfo.REQ_TYPE_ELECTION_SECOND_PHASE);
        }

        command.setBody(JSON.toJSONString(remotingRequest));
        return command;
    }

    //构成选举结果请求
    public static Command composeElectionResultRequest(long electionRound, long realNum, Object realValue) {
        //请求指令类型
        Command command = new Command(CodeInfo.COMMAND_TYPE_REQ);
        Request remotingRequest = new Request(Request.getAndIncreaseReq());
        //选举完成后的广播请求
        remotingRequest.setType(CodeInfo.REQ_TYPE_ELECTION_RESULT_TO_LEANER);

        // 业务发送选举结果对象
        ElectionResultRequest electionResultRequest = new ElectionResultRequest();
        electionResultRequest.setNum(realNum);
        electionResultRequest.setValue(realValue);
        electionResultRequest.setElectionRound(electionRound);
        remotingRequest.setData(JSON.toJSONString(electionResultRequest));

        command.setBody(JSON.toJSONString(remotingRequest));
        return command;
    }

    /**
     * 构建learner接收到选举结果请求处理后，返回给提议者的结果值
     *
     * @param code
     * @return
     */
    public static Command composeElectionResultResponseCommand(long reqId, int code) {
        //响应指令类型
        Command command = new Command(CodeInfo.COMMAND_TYPE_RES);
        Request remotingRequest = new Request(reqId);
        //选举完成后的广播发给接受者，接受者返回的响应结果
        remotingRequest.setType(CodeInfo.RES_TYPE_ELECTION_RESULT_TO_LEANER);

        // 业务发送选举结果对象
        ElectionResultResponse electionResultResponse = composeElectionResultResponse(code);
        remotingRequest.setData(JSON.toJSONString(electionResultResponse));
        command.setBody(JSON.toJSONString(remotingRequest));
        return command;
    }

    //构成选举结果响应命令
    public static Command composeElectionResultResponseCommand(long reqId, BaseElectionResponse electionResultResponse) {
        //响应指令类型
        Command command = new Command(CodeInfo.COMMAND_TYPE_RES);
        Request remotingRequest = new Request(reqId);
        //选举完成后的广播发给接受者，接受者返回的响应结果
        remotingRequest.setType(CodeInfo.RES_TYPE_ELECTION_RESULT_TO_LEANER);

        // 业务发送选举结果对象
        remotingRequest.setData(JSON.toJSONString(electionResultResponse));
        command.setBody(JSON.toJSONString(remotingRequest));
        return command;
    }

    //构成选举结果响应体
    public static ElectionResultResponse composeElectionResultResponse(int code) {
        // 业务发送选举结果对象
        ElectionResultResponse electionResultResponse = new ElectionResultResponse();
        electionResultResponse.setCode(code);
        return electionResultResponse;
    }

    //构成第二次选举提议响应体
    public static ElectionResponse composeSecondPahseElectionResponse(String responseCode, Long maxAcceptFirstPhaseNum,
                                                                      Long maxAcceptSecondPhaseNum, Object maxAcceptSecondPhaseValue) {
        // 实际业务一阶段选举响应结果
        return new ElectionResponse(responseCode, maxAcceptFirstPhaseNum, maxAcceptSecondPhaseNum, maxAcceptSecondPhaseValue);
    }

    //组成第一次选举提议响应
    public static ElectionResponse composeFirstPahseElectionResponse(String responseCode, Long maxAcceptFirstPhaseNum,
                                                                     Long maxAcceptSecondPhaseNum, Object maxAcceptSecondPhaseValue, Long realNum, Object realValue, Long electionFinishRound) {
        // 实际业务一阶段选举响应结果
        ElectionResponse electionResponse = new ElectionResponse(responseCode, maxAcceptFirstPhaseNum, maxAcceptSecondPhaseNum,
                maxAcceptSecondPhaseValue);
        electionResponse.setRealNum(realNum);
        electionResponse.setRealValue(realValue);
        electionResponse.setElectionRound(electionFinishRound);
        return electionResponse;
    }

    //组成心跳响应命令
    public static Command composeHeartbeatResponseCommand(long reqId) {
        // 心跳请求
        Command responseCommand = new Command();
        //版本号
        responseCommand.setVersion(CodeInfo.VERSION);
        //响应指令类型
        responseCommand.setCommandType(CodeInfo.COMMAND_TYPE_RES);

        Response remotingResponse = new Response();
        remotingResponse.setReqId(reqId);
        //心跳的响应类型
        remotingResponse.setType(CodeInfo.RES_TYPE_HEART_BEAT);

        // 心跳返回对象
        HeartbeatReponse heartbeatReponse = new HeartbeatReponse();
        //心跳请求确定
        heartbeatReponse.setStatus(CodeInfo.HEATBEAT_RES_OK);
        remotingResponse.setData(JSON.toJSONString(heartbeatReponse));

        responseCommand.setBody(JSON.toJSONString(remotingResponse));
        return responseCommand;
    }

    //组装心跳请求命令
    public static Command composeHeartbeatRequestCommand() {
        Command heartbeatRequestCommand = new Command();
        //版本号
        heartbeatRequestCommand.setVersion(CodeInfo.VERSION);
        //请求指令类型
        heartbeatRequestCommand.setCommandType(CodeInfo.COMMAND_TYPE_REQ);

        // 心跳对象
        Request remotingRequest = new Request(Request.getAndIncreaseReq());
        //心跳的请求类型
        remotingRequest.setType(CodeInfo.REQ_TYPE_HEART_BEAT);
        String data = "1";
        remotingRequest.setData(JSON.toJSONString(data));

        heartbeatRequestCommand.setBody(JSON.toJSONString(remotingRequest));
        return heartbeatRequestCommand;
    }
}
