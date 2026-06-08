package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.constant.UserConstant;
import com.yupi.yuaiagent.model.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mock.web.MockHttpServletRequest;

final class TestHttpRequestFactory {

    private TestHttpRequestFactory() {
    }

    static HttpServletRequest loginRequest(Integer userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginUserVO loginUser = new LoginUserVO();
        loginUser.setUserId(userId);
        request.getSession(true).setAttribute(UserConstant.USER_LOGIN_STATE, loginUser);
        return request;
    }
}
