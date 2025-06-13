package com.xhx.userservice.entiey.vo;

/**
 * @author master
 */
public class UserLoginVO {
    private Long userId;
    private String username;
    private String token;


    public UserLoginVO() {
    }

    public UserLoginVO(Long userId, String username, String token) {
        this.userId = userId;
        this.username = username;
        this.token = token;
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
     * @return token
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置
     * @param token
     */
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "UserLoginVO{userId = " + userId + ", username = " + username + ", token = " + token + "}";
    }
}
