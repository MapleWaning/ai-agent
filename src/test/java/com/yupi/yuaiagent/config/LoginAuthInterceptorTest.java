package com.yupi.yuaiagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yuaiagent.common.ErrorCode;
import com.yupi.yuaiagent.exception.BusinessException;
import com.yupi.yuaiagent.model.vo.LoginUserVO;
import com.yupi.yuaiagent.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAuthInterceptorTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private LoginAuthInterceptor loginAuthInterceptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginAuthInterceptor, "objectMapper", new ObjectMapper());
    }

    @Test
    void preHandle_optionsRequest_passThrough() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("OPTIONS");

        assertTrue(loginAuthInterceptor.preHandle(request, response, new Object()));

        verify(userService, never()).getLoginUser(request);
    }

    @Test
    void preHandle_notLogin_reject() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        java.io.StringWriter stringWriter = new java.io.StringWriter();
        java.io.PrintWriter printWriter = new java.io.PrintWriter(stringWriter);
        when(request.getMethod()).thenReturn("GET");
        when(userService.getLoginUser(request)).thenThrow(new BusinessException(ErrorCode.NOT_LOGIN_ERROR));
        when(response.getWriter()).thenReturn(printWriter);

        assertFalse(loginAuthInterceptor.preHandle(request, response, new Object()));

        printWriter.flush();
        String body = stringWriter.toString();
        assertTrue(body.contains("\"code\":40103"));
        assertTrue(body.contains("未登录"));
    }

    @Test
    void preHandle_loggedIn_allow() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("GET");
        when(userService.getLoginUser(request)).thenReturn(new LoginUserVO());

        assertTrue(loginAuthInterceptor.preHandle(request, response, new Object()));
    }
}
