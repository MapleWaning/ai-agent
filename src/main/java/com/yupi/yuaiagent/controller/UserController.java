package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.model.dto.UserLoginRequest;
import com.yupi.yuaiagent.model.dto.UserRegisterRequest;
import com.yupi.yuaiagent.model.entity.User;
import com.yupi.yuaiagent.model.vo.LoginUserVO;
import com.yupi.yuaiagent.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册，仅落库不自动登录
     */
    @PostMapping("/register")
    public BaseResponse<Integer> register(@RequestBody UserRegisterRequest request) {
        return BaseResponse.success(userService.userRegister(request));
    }

    /**
     * 用户登录，写入 Session、Redis、Cookie
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginRequest request,
                                           HttpServletRequest httpRequest,
                                           HttpServletResponse httpResponse) {
        return BaseResponse.success(userService.userLogin(request, httpRequest, httpResponse));
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/current")
    public BaseResponse<LoginUserVO> getCurrentUser(HttpServletRequest httpRequest) {
        return BaseResponse.success(userService.getLoginUser(httpRequest));
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest httpRequest,
                                        HttpServletResponse httpResponse) {
        userService.userLogout(httpRequest, httpResponse);
        return BaseResponse.success(true);
    }

    /**
     * 新增用户（仅管理员）
     */
    @PostMapping("/add")
    public BaseResponse<Boolean> addUser(@RequestBody User user, HttpServletRequest httpRequest) {
        userService.checkAdmin(httpRequest);
        return BaseResponse.success(userService.save(user));
    }

    /**
     * 根据 id 删除用户（仅管理员）
     */
    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteUser(@PathVariable("id") Integer id,
                                            HttpServletRequest httpRequest) {
        userService.checkAdmin(httpRequest);
        return BaseResponse.success(userService.removeById(id));
    }

    /**
     * 更新用户（仅管理员）
     */
    @PutMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody User user, HttpServletRequest httpRequest) {
        userService.checkAdmin(httpRequest);
        return BaseResponse.success(userService.updateById(user));
    }

    /**
     * 根据 id 查询用户（仅管理员）
     */
    @GetMapping("/get/{id}")
    public BaseResponse<User> getUser(@PathVariable("id") Integer id, HttpServletRequest httpRequest) {
        userService.checkAdmin(httpRequest);
        return BaseResponse.success(userService.getById(id));
    }

    /**
     * 查询用户列表（仅管理员）
     */
    @GetMapping("/list")
    public BaseResponse<List<User>> listUser(HttpServletRequest httpRequest) {
        userService.checkAdmin(httpRequest);
        return BaseResponse.success(userService.list());
    }
}
