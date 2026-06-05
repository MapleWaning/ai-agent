package com.yupi.yuaiagent.controller;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
class AgentControllerCreateChatTest {

    @Resource
    private AgentController agentController;

    @Test
    void testCreateChat() {
        Integer chatId = agentController.createChat("1");
        System.out.println("createChat 返回 chatId: " + chatId);
        Assertions.assertNotNull(chatId);
        Assertions.assertTrue(chatId > 0);
    }
}
