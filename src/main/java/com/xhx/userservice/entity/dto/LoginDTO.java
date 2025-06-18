package com.xhx.userservice.entity.dto;

public class LoginDTO {
    private Long userId;
    private String username;
    private String password;


    public LoginDTO() {
    }

    public LoginDTO(Long userId, String username, String password) {
        this.userId = userId;
        this.username = username;
        this.password = password;
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
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginDTO{userId = " + userId + ", username = " + username + ", password = " + password + "}";
    }
}
