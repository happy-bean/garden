package org.garden.enums;

/**
 * @author wgt
 * @date 2018-03-28
 * @description 成员角色信息
 **/
public enum PaxosMemberRole {

    /**
     * 未选举完成时的角色是不确定的
     */
    LOOKING(0, "looking"),

    /**
     * 领导者
     */
    LEADER(1, "leader"),

    /**
     * 领导者（替补者）
     * */
    LEADER_S(3,"leader-s"),

    /**
     * 跟随者
     */
    FOLLOWER(2, "follower"),

    /**
     * 观察者 不参与投票，只是同步状态
     */
    OBSERVER(3, "observer");

    /**
     * 角色
     */
    private int role;

    /**
     * 描述
     */
    private String message;

    PaxosMemberRole(int role, String message) {
        this.role = role;
        this.message = message;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
