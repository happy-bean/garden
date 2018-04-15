package org.garden.util;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.garden.core.constants.CodeInfo;
import org.garden.core.election.ElectionRequest;
import org.garden.core.election.ElectionResultRequest;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * @author wgt
 * @date 2018-04-06
 * @description 序列化工具
 **/
public class BizSerialAndDeSerialUtil {

    private static final Logger LOGGER = Logger.getLogger(BizSerialAndDeSerialUtil.class);

    //对象 json 转 byte
    public static byte[] objectToBytesByByJson(Object object) {
        return JSON.toJSONString(object).getBytes(Charset.forName(CodeInfo.UTF_8));
    }

    //byte 转 json
    public static <T> T byteToObjectByJson(byte[] res, Class<T> clazz) {
        try {
            String resStr = new String(res, CodeInfo.UTF_8);
            return JSON.parseObject(resStr, clazz);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("UnsupportedEncodingException",e);
        }
        return null;
    }

    public static ElectionRequest parseElectionRequest(String requestData) {
        if (StringUtils.isEmpty(requestData)) {
            LOGGER.error("parseElectionRequest requestData is null," + requestData);
            return null;
        }

        try {
            return JSON.parseObject(requestData, ElectionRequest.class);
        } catch (Exception e) {
            LOGGER.error("parseElectionRequest err," + requestData, e);
            return null;
        }
    }

    /**
     * 格式化选举结果
     * */
    public static ElectionResultRequest parseElectionResultRequest(String requestData) {
        if (StringUtils.isEmpty(requestData)) {
            LOGGER.error("parseElectionResultRequest requestData is null," + requestData);
            return null;
        }

        try {
            return JSON.parseObject(requestData, ElectionResultRequest.class);
        } catch (Exception e) {
            LOGGER.error("parseElectionResultRequest err," + requestData, e);
            return null;
        }
    }

}

