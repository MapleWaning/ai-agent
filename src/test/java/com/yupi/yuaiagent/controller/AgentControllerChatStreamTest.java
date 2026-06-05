package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.model.dto.ChatRequest;
import com.yupi.yuaiagent.model.enums.RouteType;
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
class AgentControllerChatStreamTest {

    private static final String USER_ID = "1";

    /**
     * 可通过 JVM 参数指定：-Dchat.id=101
     */
    private static final String CHAT_ID = System.getProperty("chat.id", "1");

    @Resource
    private AgentController agentController;

    @Test
    void testChatStream() {
        String[] messages = {
                "你好，我是小明，我想咨询一下恋爱问题",
                "你还记得我是谁吗"
        };

        for (int i = 0; i < messages.length; i++) {
            ChatRequest request = new ChatRequest();
            request.setUserId(USER_ID);
            request.setChatId(CHAT_ID);
            request.setMessage(messages[i]);
            request.setRouteType(RouteType.NORMAL_CHAT);

            System.out.println("\n========== 第 " + (i + 1) + " 条消息 ==========");
            System.out.println("userId: " + USER_ID + ", chatId: " + CHAT_ID);
            System.out.println("用户: " + messages[i]);
            System.out.print("AI: ");

            StringBuilder aiResponse = new StringBuilder();
            agentController.chatStream(request)
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
            Assertions.assertFalse(aiResponse.isEmpty(), "第 " + (i + 1) + " 条消息 AI 回复不应为空");
        }
    }
}
