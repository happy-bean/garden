package org.garden.enums;

/**
 * @author wgt
 * @date 2018-03-28
 * @description 选举成员状态
 **/
public enum PaxosMemberStatus {

    /**
     * 初始状态
     */
    INIT(0, "init"),

    /**
     * 选举中
     */
    ELECTIONING(1, "in the election"),

    /**
     * 选举完成正常状态
     */
    NORMAL(2, "end of election");

    /**
     * 状态
     */
    private int status;

    /**
     * 描述
     */
    private String message;

    PaxosMemberStatus(int status, String message) {
        this.status = status;
        this.message = message;
    }


    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
