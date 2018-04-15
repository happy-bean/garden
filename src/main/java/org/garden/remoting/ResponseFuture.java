package org.garden.remoting;

/**
 * @author wgt
 * @date 2018-04-12
 * @description 结果future接口
 **/
public interface ResponseFuture {

    /**
     * 获取结果
     *
     * @return
     * @throws Exception
     */
     Object get() throws Exception;

    /**
     * 指定时间内，获取结果，超时抛异常
     *
     * @param timeout
     *            单位毫秒
     * @return
     * @throws Exception
     */
     Object get(long timeout) throws Exception;

}

