package com.yupi.yuaiagent.service.impl;

import cn.hutool.core.util.StrUtil;
// import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yuaiagent.common.ErrorCode;
import com.yupi.yuaiagent.constant.UserConstant;
import com.yupi.yuaiagent.exception.BusinessException;
import com.yupi.yuaiagent.mapper.UserMapper;
import com.yupi.yuaiagent.model.dto.UserLoginRequest;
import com.yupi.yuaiagent.model.dto.UserRegisterRequest;
import com.yupi.yuaiagent.model.entity.User;
import com.yupi.yuaiagent.model.vo.LoginUserVO;
import com.yupi.yuaiagent.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Value("${spring.data.redis.ttl:3600}")
    private long redisTtl;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Integer userRegister(UserRegisterRequest request) {
        String userAccount = request.getUserAccount();
        String userPassword = request.getUserPassword();
        String checkPassword = request.getCheckPassword();

        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }
        if (userPassword.length() <= 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度必须大于8个字符");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        long count = this.count(new LambdaQueryWrapper<User>().eq(User::getUserName, userAccount));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXIST);
        }

        User user = new User();
        user.setUserName(userAccount);
        // user.setPassword(encryptPassword(userPassword));
        user.setPassword(userPassword);
        user.setRole("user");

        boolean saved = this.save(user);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败");
        }
        return user.getUserId();
    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String userAccount = request.getUserAccount();
        String userPassword = request.getUserPassword();

        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }

        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, userAccount));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // String encryptPassword = encryptPassword(userPassword);
        // if (!encryptPassword.equals(user.getPassword())) {
        if (!userPassword.equals(user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(UserConstant.USER_LOGIN_STATE, loginUserVO);

        String redisKey = buildLoginRedisKey(user.getUserId());
        try {
            stringRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(loginUserVO), Duration.ofSeconds(redisTtl));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "登录态持久化失败");
        }

        Cookie loginCookie = new Cookie(UserConstant.USER_LOGIN_STATE, String.valueOf(user.getUserId()));
        loginCookie.setMaxAge((int) redisTtl);
        loginCookie.setPath("/");
        httpResponse.addCookie(loginCookie);

        return loginUserVO;
    }

    @Override
    public LoginUserVO getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Object loginUserObj = session.getAttribute(UserConstant.USER_LOGIN_STATE);
        if (!(loginUserObj instanceof LoginUserVO loginUser) || loginUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        User user = this.getById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        LoginUserVO currentUser = new LoginUserVO();
        BeanUtils.copyProperties(user, currentUser);
        return currentUser;
    }

    // private String encryptPassword(String userPassword) {
    //     return DigestUtil.md5Hex(UserConstant.USER_PASSWORD_SALT + userPassword);
    // }

    private String buildLoginRedisKey(Integer userId) {
        return UserConstant.USER_LOGIN_STATE + ":" + userId;
    }
}
