package com.yupi.yuaiagent.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    USER_ALREADY_EXIST(40001, "用户名已存在"),
    USER_NOT_FOUND(40400, "用户不存在"),
    PASSWORD_ERROR(40100, "密码错误"),
    NOT_LOGIN_ERROR(40103, "未登录"),
    PASSWORD_NOT_MATCH(40002, "两次密码不一致"),
    OPERATION_ERROR(50000, "操作失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
