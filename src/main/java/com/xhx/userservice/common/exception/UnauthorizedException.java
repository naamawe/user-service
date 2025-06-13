package com.xhx.userservice.common.exception;


/**
 * @author master
 */
public class UnauthorizedException extends RuntimeException {

    private final int status;

    public UnauthorizedException(String message) {
        super(message);
        this.status = 401;
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
        this.status = 401;
    }

    public UnauthorizedException(Throwable cause) {
        super(cause);
        this.status = 401;
    }

    public int getStatus() {
        return status;
    }
}
