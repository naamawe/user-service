package com.xhx.userservice.common.exception;

/**
 * @author master
 */
public class AccessDeniedException extends RuntimeException{
    /**
     * 无权限访问异常
     * @param message 异常提示信息
     */
    public AccessDeniedException(String message) {
        super(message);
    }
}
