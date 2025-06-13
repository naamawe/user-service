package com.xhx.userservice.common.exception;

/**
 * @author master
 */
public class NullUserException extends RuntimeException{
    /**
     * 用户不存在异常
     * @param message
     */
    public NullUserException(String message) {
        super(message);
    }
}
