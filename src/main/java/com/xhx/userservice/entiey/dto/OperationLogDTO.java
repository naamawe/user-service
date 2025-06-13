package com.xhx.userservice.entiey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class OperationLogDTO {
    private Long userId;
    private String action;
    private String ip;
    private String detail;


    public OperationLogDTO() {
    }

    public OperationLogDTO(Long userId, String action, String ip, String detail) {
        this.userId = userId;
        this.action = action;
        this.ip = ip;
        this.detail = detail;
    }

    /**
     * 获取
     * @return userId
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置
     * @param userId
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取
     * @return action
     */
    public String getAction() {
        return action;
    }

    /**
     * 设置
     * @param action
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 获取
     * @return ip
     */
    public String getIp() {
        return ip;
    }

    /**
     * 设置
     * @param ip
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * 获取
     * @return detail
     */
    public String getDetail() {
        return detail;
    }

    /**
     * 设置
     * @param detail
     */
    public void setDetail(String detail) {
        this.detail = detail;
    }

    @Override
    public String toString() {
        return "OperationLogDTO{userId = " + userId + ", action = " + action + ", ip = " + ip + ", detail = " + detail + "}";
    }
}
