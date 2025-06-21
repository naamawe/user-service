package com.xhx.userservice.common.exception;

/**
 * @author master
 */
public class BindingException extends RuntimeException{
    /**
     * 绑定角色失败异常
     * @param message 错误信息
     */
    public BindingException(String message) {
        super(message);
    }
}
