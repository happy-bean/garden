package com.garden;

import java.io.IOException;

/**
 * @author wgt
 * @date 2018-03-11
 * @description
 **/
public interface Record {
    public void serialize(OutputArchive archive, String tag)
            throws IOException;
    public void deserialize(InputArchive archive, String tag)
            throws IOException;
}