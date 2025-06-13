package com.xhx.userservice.common.exception;

public class MessageException extends RuntimeException{

    /**
     * 消息发送失败异常
     * @param message
     */
    public MessageException(String message){
        super(message);
    }
}
