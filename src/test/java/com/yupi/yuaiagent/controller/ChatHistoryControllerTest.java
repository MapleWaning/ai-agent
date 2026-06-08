package com.yupi.yuaiagent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.common.ErrorCode;
import com.yupi.yuaiagent.exception.BusinessException;
import com.yupi.yuaiagent.model.entity.ChatHistory;
import com.yupi.yuaiagent.service.ChatHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatHistoryControllerTest {

    @Mock
    private ChatHistoryService chatHistoryService;

    @InjectMocks
    private ChatHistoryController chatHistoryController;

    private Page<ChatHistory> mockPage;

    @BeforeEach
    void setUp() {
        mockPage = new Page<>(1, 10);
        ChatHistory history = new ChatHistory();
        history.setId(100);
        history.setChatId(1);
        history.setUserId(1);
        history.setContent("hello");
        mockPage.setRecords(List.of(history));
        mockPage.setTotal(1);
    }

    @Test
    void listChatHistory_invalidChatId() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> chatHistoryController.listChatHistory(0, 10, null));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("会话ID不能为空", exception.getMessage());
    }

    @Test
    void listChatHistory_invalidPageSize() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> chatHistoryController.listChatHistory(1, 51, null));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("页面大小必须在1-50之间", exception.getMessage());
    }

    @Test
    void listChatHistory_firstPage() {
        when(chatHistoryService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        BaseResponse<Page<ChatHistory>> response = chatHistoryController.listChatHistory(1, 10, null);

        assertEquals(0, response.getCode());
        assertEquals(1, response.getData().getRecords().size());
        assertEquals(100, response.getData().getRecords().get(0).getId());
        verify(chatHistoryService).page(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void listChatHistory_withLastId() {
        when(chatHistoryService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        BaseResponse<Page<ChatHistory>> response = chatHistoryController.listChatHistory(1, 10, 100);

        assertEquals(0, response.getCode());
        assertEquals(1, response.getData().getRecords().size());
    }
}
