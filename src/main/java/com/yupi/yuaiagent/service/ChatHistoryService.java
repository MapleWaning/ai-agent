package com.yupi.yuaiagent.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;

import com.yupi.yuaiagent.model.entity.ChatHistory;

public interface ChatHistoryService extends IService<ChatHistory> {

    void preload(String userId, String chatId, String UserMessage, String type);

    void saveAiResponse(String userId, String chatId, String AiResponse, String type, List<Map<String, Object>> streamEvents);

}
