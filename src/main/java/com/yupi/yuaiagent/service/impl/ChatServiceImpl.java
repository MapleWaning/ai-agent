package com.yupi.yuaiagent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yuaiagent.mapper.ChatMapper;
import com.yupi.yuaiagent.model.entity.Chat;
import com.yupi.yuaiagent.service.ChatService;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat> implements ChatService {
}
