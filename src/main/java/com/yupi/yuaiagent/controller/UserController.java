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
     * 新增用户
     */
    @PostMapping("/add")
    public BaseResponse<Boolean> addUser(@RequestBody User user) {
        return BaseResponse.success(userService.save(user));
    }

    /**
     * 根据 id 删除用户
     */
    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteUser(@PathVariable("id") Integer id) {
        return BaseResponse.success(userService.removeById(id));
    }

    /**
     * 更新用户
     */
    @PutMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody User user) {
        return BaseResponse.success(userService.updateById(user));
    }

    /**
     * 根据 id 查询用户
     */
    @GetMapping("/get/{id}")
    public BaseResponse<User> getUser(@PathVariable("id") Integer id) {
        return BaseResponse.success(userService.getById(id));
    }

    /**
     * 查询用户列表
     */
    @GetMapping("/list")
    public BaseResponse<List<User>> listUser() {
        return BaseResponse.success(userService.list());
    }
}
