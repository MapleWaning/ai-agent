package com.yupi.yuaiagent.exception;

import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.warn("BusinessException: code={}, message={}", e.getCode(), e.getMessage());
        return BaseResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<?> runtimeExceptionHandler(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return BaseResponse.error(ErrorCode.OPERATION_ERROR, "系统内部错误");
    }
}
