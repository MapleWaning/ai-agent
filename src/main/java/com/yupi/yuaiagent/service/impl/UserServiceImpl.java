package com.yupi.yuaiagent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yuaiagent.mapper.UserMapper;
import com.yupi.yuaiagent.model.entity.User;
import com.yupi.yuaiagent.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
