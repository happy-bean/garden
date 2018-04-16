package org.garden.core.election;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;
import org.garden.core.data.DataStoreInf;
import org.garden.core.data.FileDataStore;
import org.garden.core.paxos.DefaultPaxosStore;
import org.garden.core.paxos.PaxosCore;
import org.garden.core.paxos.PaxosMember;
import org.garden.core.paxos.PaxosStoreInf;
import org.garden.util.BizSerialAndDeSerialUtil;

/**
 * @author wgt
 * @date 2018-04-12
 * @description 选举信息装载
 **/
public class ElectionInfoLoader {

    private static final Logger LOGGER = Logger.getLogger(ElectionInfoLoader.class);;

    private DataStoreInf dataStore = new FileDataStore();

    private PaxosStoreInf paxosStore;

    public ElectionInfoLoader(PaxosCore paxosCore){
        paxosStore = new DefaultPaxosStore();
        paxosStore.setCurrentPaxosMember(paxosCore.getCurrentPaxosMember());
        paxosCore.setOtherPaxosMemberList(paxosCore.getOtherPaxosMemberList());
    }

    //加载选举信息
    public void loadElectionInfo() throws Exception {
        LOGGER.info("begin loader electionInfo from store");

        //从文件中加载
        byte[] resBytes = dataStore.read();
        if (resBytes == null) {
            LOGGER.info("not found electionInfo from store");
            return;
        }

        //将选举 byte 信息转为对象
        ElectionInfo electionInfoSaved = BizSerialAndDeSerialUtil.byteToObjectByJson(resBytes, ElectionInfo.class);
        LOGGER.info("found electionInfo from store,res,electionInfo[" + JSON.toJSONString(electionInfoSaved) + "]");
        if (electionInfoSaved == null) {
            return;
        }

        //获取当前成员
        PaxosMember currentMember = paxosStore.getCurrentPaxosMember();


        Long savedRealNum = electionInfoSaved.getRealNum();
        Object savedRealValue = electionInfoSaved.getRealValue();

        ElectionInfo oldElectionInfo = currentMember.getElectionInfo();

        String text = new String(resBytes);

        JSONObject jsonObject = JSON.parseObject(text);
        Long savedElectionRound = jsonObject.getLong("electionRound");

        if (savedElectionRound != null) {
            oldElectionInfo.setElectionRoundByValue(savedElectionRound);
        }

        if (savedRealNum != null) {
            oldElectionInfo.setRealNum(savedRealNum);
        }

        if (savedRealValue != null) {
            oldElectionInfo.setRealValue(savedRealValue);
        }
        return;
    }
}
