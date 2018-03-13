package org.garden.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wgt
 * @date 2018-01-11
 * @description 配置文件工具类
 **/
public class PropertiesUtil {

    /**
     * 去除配置文件中的注释，以";"或"#"开头
     *
     * @param source
     * @return
     */
    private static String removeIniComments(String source) {
        String result = source;

        if (result.contains(";")) {
            result = result.substring(0, result.indexOf(";"));
        }

        if (result.contains("#")) {
            result = result.substring(0, result.indexOf("#"));
        }

        return result.trim();
    }

    /**
     * 读取配置文件，存放到Map中
     * 支持以‘#’或‘;’开头的注释；
     * 支持行连接符（行末的'\'标记）；
     * 支持空行、name/value前后的空格；
     * 如果有重名，取最后一个
     *
     * @return
     */
    public static Map<String, Object> readPropertiesFile(InputStream inputStream) {
        Map<String, Object> result = new HashMap();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String str = null;
            boolean lineContinued = false;
            String tempStr = null;

            //一次读入一行（非空），直到读入null为文件结束
            while ((str = reader.readLine()) != null) {
                //去掉尾部的注释、去掉首尾空格
                str = removeIniComments(str).trim();
                if ("".equals(str) || str == null) {
                    continue;
                }

                //如果前一行包括了连接符'\'
                if (lineContinued == true) {
                    str = tempStr + str;
                }

                //处理行连接符'\'
                if (str.endsWith("\\")) {
                    lineContinued = true;
                    tempStr = str.substring(0, str.length() - 1);
                    continue;
                } else {
                    lineContinued = false;
                }

                //整理拆开name=value对，并存放到MAP中：
                if (str.contains("=")) {
                    int delimiterPos = str.indexOf("=");
                    result.put(str.substring(0, delimiterPos).trim(), str.substring(delimiterPos + 1, str.length()).trim());
                }
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return result;
    }

}
