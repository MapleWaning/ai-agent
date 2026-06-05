package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.model.dto.ChatRequest;
import com.yupi.yuaiagent.model.dto.RouteRequest;
import com.yupi.yuaiagent.model.enums.RouteType;
import com.yupi.yuaiagent.model.vo.RouteResponse;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

@SpringBootTest
@ActiveProfiles("local")
@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
class AgentControllerChatFlowTest {

    private static final String USER_ID = "1";
    private static final String MESSAGE = "你好，我是小明，我想了解历史上有什么有名的恋爱高手，帮我在网上搜下";

    @Resource
    private AgentController agentController;

    @Test
    void testCreateRouteAndStream() {
        // 1. /chat/create
        Integer chatId = agentController.createChat(USER_ID);
        System.out.println("createChat 返回 chatId: " + chatId);
        Assertions.assertNotNull(chatId);
        Assertions.assertTrue(chatId > 0);

        // 2. /chat/route
        RouteRequest routeRequest = new RouteRequest();
        routeRequest.setInitPrompt(MESSAGE);
        RouteResponse routeResponse = agentController.routing(routeRequest);
        System.out.println("routeType: " + routeResponse.getRouteType());
        System.out.println("enumName: " + routeResponse.getEnumName());
        System.out.println("reason: " + routeResponse.getReason());
        Assertions.assertNotNull(routeResponse.getRouteType());

        RouteType routeType = RouteType.fromValue(routeResponse.getRouteType());
        System.out.println("\n========== 路由结果 ==========");
        System.out.println("routeType: " + routeType.getValue());
        System.out.println("enumName: " + routeResponse.getEnumName());
        System.out.println("reason: " + routeResponse.getReason());
        // 3. /chat/stream
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setUserId(USER_ID);
        chatRequest.setChatId(String.valueOf(chatId));
        chatRequest.setMessage(MESSAGE);
        chatRequest.setRouteType(routeType);

        System.out.println("\n========== 流式对话 ==========");
        System.out.println("userId: " + USER_ID + ", chatId: " + chatId);
        System.out.println("routeType: " + routeType.getValue());
        System.out.println("用户: " + MESSAGE);
        System.out.print("AI: ");

        StringBuilder aiResponse = new StringBuilder();
        agentController.chatStream(chatRequest)
                .doOnNext(chunk -> {
                    if ("done".equalsIgnoreCase(chunk)) {
                        System.out.println("\n[done]");
                        return;
                    }
                    System.out.print(chunk);
                    aiResponse.append(chunk);
                })
                .blockLast(Duration.ofMinutes(5));

        System.out.println("完整回复: " + aiResponse);
        Assertions.assertFalse(aiResponse.isEmpty(), "AI 回复不应为空");
    }
}
