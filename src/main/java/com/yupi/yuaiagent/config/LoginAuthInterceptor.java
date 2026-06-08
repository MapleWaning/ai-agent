package com.yupi.yuaiagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.common.ErrorCode;
import com.yupi.yuaiagent.exception.BusinessException;
import com.yupi.yuaiagent.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
public class LoginAuthInterceptor implements HandlerInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        try {
            userService.getLoginUser(request);
            return true;
        } catch (BusinessException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(BaseResponse.error(e.getCode(), e.getMessage())));
            return false;
        }
    }
}
