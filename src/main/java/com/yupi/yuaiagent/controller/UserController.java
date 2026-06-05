package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.model.entity.User;
import com.yupi.yuaiagent.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 新增用户
     */
    @PostMapping("/add")
    public boolean addUser(@RequestBody User user) {
        return userService.save(user);
    }

    /**
     * 根据 id 删除用户
     */
    @DeleteMapping("/delete/{id}")
    public boolean deleteUser(@PathVariable("id") Integer id) {
        return userService.removeById(id);
    }

    /**
     * 更新用户
     */
    @PutMapping("/update")
    public boolean updateUser(@RequestBody User user) {
        return userService.updateById(user);
    }

    /**
     * 根据 id 查询用户
     */
    @GetMapping("/get/{id}")
    public User getUser(@PathVariable("id") Integer id) {
        return userService.getById(id);
    }

    /**
     * 查询用户列表
     */
    @GetMapping("/list")
    public List<User> listUser() {
        return userService.list();
    }
}
