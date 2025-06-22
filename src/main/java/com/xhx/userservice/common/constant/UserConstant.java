package com.xhx.userservice.common.constant;

/**
 * @author master
 */
public class UserConstant {
    public static final String USER_SERVICE_GROUP = "user-service-tx-group";
    public static final String USER_REGISTER = "user_register";
    public static final String USER_LOGIN = "user_login";
    public static final String USER_CHECK = "user_check";
    public static final String USER_UPDATE = "user_update";
    public static final String USER_RESET_PASSWORD = "user_reset_password";

    public static final String USER_REGISTER_SUCCESS = "用户注册成功";
    public static final String USER_LOGIN_SUCCESS = "用户登录成功";
    public static final String USER_CHECK_SUCCESS = "查询用户信息";
    public static final String USER_UPDATE_SUCCESS = "更新用户信息";
    public static final String USER_RESET_PASSWORD_SUCCESS = "重置用户密码";

    public static final String USER_ROLE_USER = "user";
    public static final String USER_ROLE_ADMIN = "admin";
    public static final String USER_ROLE_SUPER_ADMIN = "super_admin";

    public static final String NOT_NULL_USERNAME = "用户名不能为空";
    public static final String NOT_NULL_PASSWORD = "密码不能为空";
    public static final String EMAIL_FORMAT_ERROR = "邮箱格式不正确";
    public static final String PHONE_FORMAT_ERROR = "手机号格式不正确";
    public static final String ONLY_SUPER_ADMIN_CAN_UPDATE_ROLE = "只有超级管理员可以修改用户权限";
    public static final String UNSUPPORT_TARGET_ROLE = "不支持的目标角色";

}
