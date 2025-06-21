package com.xhx.userservice.common.exception;

/**
 * @author master
 */
public class PasswordErrorException extends RuntimeException{
    /**
     * 密码错误异常
     * @param message 错误信息
     */
    public PasswordErrorException(String message){
        super(message);
    }
}
