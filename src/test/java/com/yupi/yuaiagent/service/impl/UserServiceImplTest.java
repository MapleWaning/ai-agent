package com.yupi.yuaiagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yuaiagent.common.ErrorCode;
import com.yupi.yuaiagent.constant.UserConstant;
import com.yupi.yuaiagent.exception.BusinessException;
import com.yupi.yuaiagent.mapper.UserMapper;
import com.yupi.yuaiagent.model.dto.UserLoginRequest;
import com.yupi.yuaiagent.model.dto.UserRegisterRequest;
import com.yupi.yuaiagent.model.entity.User;
import com.yupi.yuaiagent.model.vo.LoginUserVO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String VALID_PASSWORD = "123456789";

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        ReflectionTestUtils.setField(userService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(userService, "redisTtl", 3600L);
    }

    @Test
    void userRegister_passwordTooShort() {
        UserRegisterRequest request = buildRegisterRequest("testuser", "12345678", "12345678");

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.userRegister(request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void userRegister_passwordNotMatch() {
        UserRegisterRequest request = buildRegisterRequest("testuser", VALID_PASSWORD, VALID_PASSWORD + "0");

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.userRegister(request));

        assertEquals(ErrorCode.PASSWORD_NOT_MATCH.getCode(), exception.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void userRegister_userAlreadyExist() {
        UserRegisterRequest request = buildRegisterRequest("testuser", VALID_PASSWORD, VALID_PASSWORD);
        when(userMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.userRegister(request));

        assertEquals(ErrorCode.USER_ALREADY_EXIST.getCode(), exception.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void userRegister_success() {
        UserRegisterRequest request = buildRegisterRequest("testuser", VALID_PASSWORD, VALID_PASSWORD);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(100);
            return 1;
        });

        Integer userId = userService.userRegister(request);

        assertEquals(100, userId);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("testuser", savedUser.getUserName());
        assertEquals("user", savedUser.getRole());
        assertEquals(VALID_PASSWORD, savedUser.getPassword());
    }

    @Test
    void userLogin_userNotFound() {
        UserLoginRequest request = buildLoginRequest("missing", VALID_PASSWORD);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.userLogin(request, mock(HttpServletRequest.class), mock(HttpServletResponse.class)));

        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void userLogin_wrongPassword() {
        UserLoginRequest request = buildLoginRequest("testuser", VALID_PASSWORD);
        User user = buildUser(1, "testuser", "wrong-password");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(user);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.userLogin(request, mock(HttpServletRequest.class), mock(HttpServletResponse.class)));

        assertEquals(ErrorCode.PASSWORD_ERROR.getCode(), exception.getCode());
    }

    @Test
    void userLogin_success_writesSessionRedisAndCookie() throws Exception {
        UserLoginRequest request = buildLoginRequest("testuser", VALID_PASSWORD);
        User user = buildUser(1, "testuser", VALID_PASSWORD);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(user);

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(httpRequest.getSession(true)).thenReturn(session);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        LoginUserVO loginUserVO = userService.userLogin(request, httpRequest, httpResponse);

        assertNotNull(loginUserVO);
        assertEquals(1, loginUserVO.getUserId());
        assertEquals("testuser", loginUserVO.getUserName());
        assertEquals(UserConstant.DEFAULT_ROLE, loginUserVO.getRole());

        ArgumentCaptor<LoginUserVO> sessionCaptor = ArgumentCaptor.forClass(LoginUserVO.class);
        verify(session).setAttribute(eq(UserConstant.USER_LOGIN_STATE), sessionCaptor.capture());
        assertEquals(1, sessionCaptor.getValue().getUserId());

        ArgumentCaptor<String> redisKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> redisValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(redisKeyCaptor.capture(), redisValueCaptor.capture(), eq(Duration.ofSeconds(3600)));
        assertEquals(UserConstant.USER_LOGIN_STATE + ":1", redisKeyCaptor.getValue());
        assertEquals("{\"userId\":1,\"userName\":\"testuser\",\"role\":\"user\"}", redisValueCaptor.getValue());

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(httpResponse).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();
        assertEquals(UserConstant.USER_LOGIN_STATE, cookie.getName());
        assertEquals("1", cookie.getValue());
        assertEquals(3600, cookie.getMaxAge());
        assertEquals("/", cookie.getPath());
    }

    @Test
    void getLoginUser_noSession() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.getLoginUser(request));

        assertEquals(ErrorCode.NOT_LOGIN_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getLoginUser_success() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        LoginUserVO sessionUser = new LoginUserVO();
        sessionUser.setUserId(1);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(UserConstant.USER_LOGIN_STATE)).thenReturn(sessionUser);
        when(userMapper.selectById(1)).thenReturn(buildUser(1, "testuser", VALID_PASSWORD));

        LoginUserVO loginUser = userService.getLoginUser(request);

        assertEquals(1, loginUser.getUserId());
        assertEquals("testuser", loginUser.getUserName());
        assertEquals("user", loginUser.getRole());
    }

    private UserRegisterRequest buildRegisterRequest(String account, String password, String checkPassword) {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUserAccount(account);
        request.setUserPassword(password);
        request.setCheckPassword(checkPassword);
        return request;
    }

    private UserLoginRequest buildLoginRequest(String account, String password) {
        UserLoginRequest request = new UserLoginRequest();
        request.setUserAccount(account);
        request.setUserPassword(password);
        return request;
    }

    private User buildUser(Integer userId, String userName, String password) {
        User user = new User();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setPassword(password);
        user.setRole(UserConstant.DEFAULT_ROLE);
        return user;
    }
}
