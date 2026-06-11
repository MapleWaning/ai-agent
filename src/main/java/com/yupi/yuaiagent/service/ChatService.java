package com.yupi.yuaiagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuaiagent.model.dto.ChatRequest;
import com.yupi.yuaiagent.model.entity.Chat;
import com.yupi.yuaiagent.model.vo.ChatVO;

import java.util.List;

public interface ChatService extends IService<Chat> {

    /**
     * 查询当前用户的会话列表
     */
    List<ChatVO> listChats(Integer userId);

    /**
     * 查询单个会话详情
     */
    ChatVO getChatDetail(ChatRequest request);

    /**
     * 更新会话标题
     */
    ChatVO updateChatTitle(ChatRequest request, String title);

    /**
     * 删除会话（级联清理 chat_history 与会话文件目录）
     */
    void deleteChat(ChatRequest request);
}
