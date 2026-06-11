package com.yupi.yuaiagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuaiagent.model.dto.UserLoginRequest;
import com.yupi.yuaiagent.model.dto.UserRegisterRequest;
import com.yupi.yuaiagent.model.entity.User;
import com.yupi.yuaiagent.model.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface UserService extends IService<User> {

    /**
     * 用户注册，仅落库不登录
     */
    Integer userRegister(UserRegisterRequest request);

    /**
     * 用户登录，写入 Session、Redis、Cookie
     */
    LoginUserVO userLogin(UserLoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * 获取当前登录用户，未登录则抛异常
     */
    LoginUserVO getLoginUser(HttpServletRequest request);

    /**
     * 校验当前用户为管理员
     */
    void checkAdmin(HttpServletRequest request);

    /**
     * 退出登录，清除 Session、Redis、Cookie
     */
    void userLogout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);
}
