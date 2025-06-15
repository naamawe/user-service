package com.xhx.userservice.entiey.vo;

import java.sql.Timestamp;

/**
 * @author master
 */
public class UserVO {
    private Long userId;
    private String username;
    private String email;
    private String phone;
    private Timestamp gmtCreate;

    public UserVO() {
    }

    public UserVO(Long userId, String username, String email, String phone, Timestamp gmtCreate) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.gmtCreate = gmtCreate;
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
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置
     * @param email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取
     * @return phone
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 设置
     * @param phone
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 获取
     * @return gmtCreate
     */
    public Timestamp getGmtCreate() {
        return gmtCreate;
    }

    /**
     * 设置
     * @param gmtCreate
     */
    public void setGmtCreate(Timestamp gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    @Override
    public String toString() {
        return "UserVO{userId = " + userId + ", username = " + username + ", email = " + email + ", phone = " + phone + ", gmtCreate = " + gmtCreate + "}";
    }
}
