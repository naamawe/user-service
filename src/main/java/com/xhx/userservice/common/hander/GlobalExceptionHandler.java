package com.xhx.userservice.common.hander;

import com.xhx.userservice.common.exception.*;
import entiey.pojo.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author master
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
//    @ExceptionHandler(RuntimeException.class)
//    public Result handleRuntimeException(RuntimeException e) {
//        return Result.fail("服务器异常");
//    }
    @ExceptionHandler(BindingException.class)
    public Result handleBusinessException(BindingException e) {
        return Result.fail(e.getMessage());
    }
    @ExceptionHandler(MessageException.class)
    public Result handleMessageException(MessageException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(NullUserException.class)
    public Result handleNullUserException(NullUserException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(PasswordErrorException.class)
    public Result handlePasswordErrorException(PasswordErrorException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result handleAccessDeniedException(AccessDeniedException e) {
        return Result.fail(e.getMessage());
    }
}
