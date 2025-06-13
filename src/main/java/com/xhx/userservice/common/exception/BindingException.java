package com.xhx.userservice.common.exception;

public class BindingException extends RuntimeException{
    /**
     * 绑定角色失败异常
     * @param message
     */
    public BindingException(String message) {
        super(message);
    }
}
