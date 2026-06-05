package com.yupi.yuaiagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuaiagent.model.entity.ChatHistory;

public interface ChatHistoryService extends IService<ChatHistory> {

    void preload(String userId, String chatId,String UserMessage);

    void saveAiResponse(String userId, String chatId,String AiResponse);

}
