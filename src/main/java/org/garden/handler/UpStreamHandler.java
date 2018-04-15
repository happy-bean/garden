package org.garden.handler;

import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.garden.core.constants.CodeInfo;
import org.garden.core.election.BaseElectionResponse;
import org.garden.core.paxos.PaxosCore;
import org.garden.core.paxos.PaxosCoreComponent;
import org.garden.exchange.ExchangeClient;
import org.garden.remoting.Command;
import org.garden.remoting.DefaultFuture;
import org.garden.remoting.Request;
import org.garden.remoting.Response;
import org.garden.util.RequestAndResponseUtil;

import java.util.Date;

/**
 * @author wgt
 * @date 2018-03-26
 * @description 结点接收到远程通信指令处理类
 **/
public class UpStreamHandler extends SimpleChannelInboundHandler<Command> {

    private static final Logger LOGGER = Logger.getLogger(UpStreamHandler.class);

    //paxos核心
    private PaxosCore paxosCore;

    public UpStreamHandler(PaxosCore paxosCoreComponent) {
        this.paxosCore = paxosCoreComponent;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
        LOGGER.debug("recieve msg,msg[" + JSON.toJSONString(msg) + "]");

        //检查msg有效性
        if(msg==null){
            return;
        }

        //查询结果
        String bizReq = msg.getBody();

        //处理指令, 请求指令类型
        if (msg.getCommandType() == CodeInfo.COMMAND_TYPE_REQ) {
            //处理远程通信请求指令
            Request remotingRequest = Request.parseCommandToRemotingRequest(bizReq);
            this.processCommandReq(ctx, remotingRequest);
        } else if (msg.getCommandType() == CodeInfo.COMMAND_TYPE_RES) {
            //响应指令类型
            //处理远程通信响应指令
            Response remotingResponse = Request.parseCommandToRemotingResponse(bizReq);
            this.processCommandResponse(ctx, remotingResponse);
        } else {
            throw new IllegalArgumentException("不支持的command类型,parma[" + JSON.toJSONString(msg) + "]");
        }

    }

    private void processCommandResponse(ChannelHandlerContext ctx, Response remotingResponse) {
        String msgLog = JSON.toJSONString(remotingResponse);
        LOGGER.debug("begin processCommandResponse,remotingResponse[" + msgLog + "]");

        //处理响应
        if (remotingResponse.getType() == CodeInfo.RES_TYPE_HEART_BEAT && !StringUtils.isEmpty(remotingResponse.getData())) {
            //如果是心跳响应则更新对应exchangeClient的心跳保活时间
            ExchangeClient exchangeClient = DefaultFuture.getExchangeClientByReqId(remotingResponse.getReqId());
            if (exchangeClient == null) {
                LOGGER.error("cannot found exchangeClient,remotingResponse[" + msgLog + "]");
            } else {
                exchangeClient.setLastLiveTime(System.currentTimeMillis());
            }
        }

        //设置异步结果，并清除defaultFuture中的请求上下文
        DefaultFuture.recieve(remotingResponse);
        LOGGER.debug("end processCommandResponse succ,remotingResponse[" + msgLog + "]");
    }

    private void processCommandReq(ChannelHandlerContext ctx, Request remotingRequest) {
        String logStr = JSON.toJSONString(remotingRequest);
        LOGGER.debug("begin processCommandReq,request[" + logStr + "]");

        //心跳处理
        if (CodeInfo.REQ_TYPE_HEART_BEAT == remotingRequest.getType()) {
            Command responseCommand = RequestAndResponseUtil.composeHeartbeatResponseCommand(remotingRequest.getReqId());
            LOGGER.debug("send heartbeat response,reponse[" + logStr + "]");
            ChannelFuture channelFuture = ctx.channel().writeAndFlush(responseCommand);
            LOGGER.debug("send response succFlag[" + channelFuture.isSuccess() + "]");
            return;
        }

        //其它为选举业务处理，交给业务组件
        String requestData = remotingRequest.getData();
        byte bizType = remotingRequest.getType();
        long reqId = remotingRequest.getReqId();
        boolean sendResponseFlag;
        Command resCommand;

        BaseElectionResponse electionResponse = paxosCore.processElectionRequestForAcceptor(bizType, requestData);
        resCommand = RequestAndResponseUtil.composeElectionResultResponseCommand(reqId, electionResponse);
        sendResponseFlag = this.secondRemotingCommand(ctx, resCommand);
        if (!sendResponseFlag) {
            LOGGER.error("processCommandReq send responseRes err," + logStr);
        }
        LOGGER.debug("end processCommandReq succ,request[" + logStr + "]");
    }

    private boolean secondRemotingCommand(ChannelHandlerContext ctx,Command remotingCommand) {
        String logStr = JSON.toJSONString(remotingCommand);
        try {
            ChannelFuture sendRes = ctx.writeAndFlush(remotingCommand);
            if (!sendRes.isSuccess()) {
                LOGGER.error("secondRemotingCommand err," + logStr);
                return false;
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("secondRemotingCommand err,param[" + logStr + "]");
            return false;
        }
    }

    public PaxosCore getPaxosCoreComponent() {
        return paxosCore;
    }

    public void setPaxosCoreComponent(PaxosCoreComponent paxosCoreComponent) {
        this.paxosCore = paxosCoreComponent;
    }
}