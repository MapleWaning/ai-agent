package com.yupi.yuaiagent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yuaiagent.mapper.ChatHistoryMapper;
import com.yupi.yuaiagent.model.entity.ChatHistory;
import com.yupi.yuaiagent.service.ChatHistoryService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Value("${redis.KEY_PREFIX:message_store:}")
    private String redisKeyPrefix;

    @Value("${spring.data.redis.ttl:3600}")
    private long redisTtl;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void preload(String userId, String chatId, String UserMessage) {
        String sessionId = buildSessionId(userId, chatId);
        String redisKey = redisKeyPrefix + sessionId;

        // 1. 删除 Redis 中该会话的历史（与 Python RedisChatMessageHistory.clear 一致）
        stringRedisTemplate.delete(redisKey);

        // 2. 从 MySQL 加载已有历史到 Redis（LPUSH + 逆序写入，与 LangChain RedisChatMessageHistory 一致）
        Integer uid = Integer.valueOf(userId);
        Integer cid = Integer.valueOf(chatId);
        List<ChatHistory> histories = lambdaQuery()
                .eq(ChatHistory::getUserId, uid)
                .eq(ChatHistory::getChatId, cid)
                .orderByAsc(ChatHistory::getId)
                .list();

        for (int i = histories.size() - 1; i >= 0; i--) {
            stringRedisTemplate.opsForList().leftPush(redisKey, histories.get(i).getContent());
        }
        if (redisTtl > 0 && !histories.isEmpty()) {
            stringRedisTemplate.expire(redisKey, Duration.ofSeconds(redisTtl));
        }

        // 3. 将当前用户消息以 LangChain HumanMessage 格式写入 MySQL
        ChatHistory record = new ChatHistory();
        record.setUserId(uid);
        record.setChatId(cid);
        record.setContent(buildHumanMessageJson(UserMessage));
        save(record);
    }

    private String buildSessionId(String userId, String chatId) {
        return userId + "_" + chatId;
    }

    /**
     * 构造 LangChain message_to_dict(HumanMessage) 格式 JSON
     */
    private String buildHumanMessageJson(String content) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", content);
        data.put("additional_kwargs", Collections.emptyMap());
        data.put("response_metadata", Collections.emptyMap());
        data.put("type", "human");
        data.put("name", null);
        data.put("id", null);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "human");
        message.put("data", data);

        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize LangChain HumanMessage", e);
        }
    }

    @Override
    public void saveAiResponse(String userId, String chatId, String AiResponse) {
        ChatHistory record = new ChatHistory();
        record.setUserId(Integer.valueOf(userId));
        record.setChatId(Integer.valueOf(chatId));
        record.setContent(buildAiMessageJson(AiResponse));
        save(record);
    }

    /**
     * 构造 LangChain message_to_dict(AIMessage) 格式 JSON
     */
    private String buildAiMessageJson(String content) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", content);
        data.put("additional_kwargs", Collections.emptyMap());
        data.put("response_metadata", Collections.emptyMap());
        data.put("type", "ai");
        data.put("name", null);
        data.put("id", null);
        data.put("tool_calls", Collections.emptyList());
        data.put("invalid_tool_calls", Collections.emptyList());
        data.put("usage_metadata", null);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "ai");
        message.put("data", data);

        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize LangChain AIMessage", e);
        }
    }

}
