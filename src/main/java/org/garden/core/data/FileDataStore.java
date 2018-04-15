package org.garden.core.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.garden.conf.GardenConfig;
import org.garden.core.constants.CodeInfo;

import java.io.*;

/**
 * @author wgt
 * @date 2018-04-06
 * @description 文件储存持久化实现
 **/
public class FileDataStore implements DataStoreInf {

    private static final Logger LOGGER = Logger.getLogger(FileDataStore.class);

    //创建文件路径 如果不存在的情况
    private boolean createFilePathIfNotExist(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return false;
        }

        File file = new File(filePath);
        boolean resFlag = true;
        if (!file.exists()) {
            resFlag = file.mkdirs();
            if (!resFlag) {
                LOGGER.error("cannot create filePath in writeToStore,filePath:" + filePath);
                return false;
            }
        }
        return resFlag;
    }

    //创建文件
    private boolean createFileIfNotExist(String filePath, String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return false;
        }

        String fullPath = this.getFullName(filePath, fileName);
        File file = new File(fullPath);
        boolean resFlag = true;
        if (!file.exists()) {
            try {
                resFlag = file.createNewFile();
                if (!resFlag) {
                    LOGGER.error(">cannot create fileName in writeToStore,fileName:" + fileName);
                }
            } catch (IOException e) {
                LOGGER.error(">create file err,fileName:" + fileName, e);
                resFlag = false;
            }

        }
        return resFlag;
    }

    //将指定字节写入到存储
    @Override
    public boolean writeToStore(byte[] data) {
        String dataStorePath = GardenConfig.dataPath;
        String fileName = "garden.data";

        String logStr = "dataStorePath[" + dataStorePath + "],fileName[" + fileName + "]";

        LOGGER.info(logStr);

        if (StringUtils.isEmpty(dataStorePath)) {
            throw new IllegalArgumentException("dataStorePath cannot be null");
        }

        /**
         * 目录处理
         */
        boolean createFilePathRes = this.createFilePathIfNotExist(dataStorePath);
        if (!createFilePathRes) {
            LOGGER.error("filePath not found," + logStr);
            return false;
        }

        /**
         * 文件处理
         */
        boolean createFileRes = this.createFileIfNotExist(dataStorePath, fileName);
        if (!createFileRes) {
            LOGGER.error("file not found," + logStr);
            return false;
        }

        String fullPath = this.getFullName(dataStorePath, fileName);
        File file = new File(fullPath);
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            fout.write(data);
            return true;
        } catch (FileNotFoundException e) {
            LOGGER.error("not found filePath when writeToStore," + logStr, e);
            return false;
        } catch (IOException e) {
            LOGGER.error("not found filePath when IOException," + logStr, e);
            return false;
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    LOGGER.error(">close file err");
                }
            }
        }
    }

    private String getFullName(String path, String fileName) {
        String fileNameRes = fileName + "_" + GardenConfig.serverIp + "_" + GardenConfig.serverPort;
        return path + File.separator + fileNameRes;
    }

    @Override
    public boolean writeToStore(String data) {
        throw new UnsupportedOperationException("not support");
    }

    //读取写入的数据
    @Override
    public byte[] read() {
        String dataStorePath = GardenConfig.dataPath;
        String fileName = "garden.data";
        String logStr = "dataStorePath[" + dataStorePath + "],fileName[" + fileName + "]";
        LOGGER.info(logStr);

        if (StringUtils.isEmpty(dataStorePath)) {
            throw new IllegalArgumentException("dataStorePath cannot be null");
        }

        String fullPath = this.getFullName(dataStorePath, fileName);

        // 读取数据
        BufferedReader breader = null;

        StringBuilder buf = new StringBuilder(48);
        String temp = null;

        try {
            breader = new BufferedReader(new FileReader(fullPath));
            while ((temp = breader.readLine()) != null) {
                buf.append(temp);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("not found file when read,fullPath[" + fullPath + "]");
        } catch (IOException e) {
            LOGGER.error("not found file when read,fullPath[" + fullPath + "]", e);
        } finally {
            if (breader != null) {
                try {
                    breader.close();
                } catch (IOException e) {
                    LOGGER.error("close breader err");
                }
            }
        }

        try {
            byte[] res = buf.toString().getBytes(CodeInfo.UTF_8);
            return res;
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("UnsupportedEncodingException,fullPath[" + fullPath + "]", e);
            return null;
        }
    }
}

