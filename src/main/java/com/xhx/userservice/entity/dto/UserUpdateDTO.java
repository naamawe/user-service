package com.xhx.userservice.entity.dto;

/**
 * @author master
 */
public class UserUpdateDTO {
    private String username;
    private String email;
    private String phone;
    private String role;

    public UserUpdateDTO() {
    }

    public UserUpdateDTO(String username, String email, String phone, String role) {
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.role = role;
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
     * @return role
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置
     * @param role
     */
    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "UserUpdateDTO{username = " + username + ", email = " + email + ", phone = " + phone + ", role = " + role + "}";
    }
}
