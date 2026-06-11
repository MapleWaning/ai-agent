package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.model.dto.ChatRequest;
import com.yupi.yuaiagent.model.dto.ChatUpdateRequest;
import com.yupi.yuaiagent.model.vo.ChatVO;
import com.yupi.yuaiagent.model.vo.LoginUserVO;
import com.yupi.yuaiagent.service.ChatService;
import com.yupi.yuaiagent.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/agent/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private UserService userService;

    /**
     * 查询当前用户会话列表
     */
    @GetMapping("/list")
    public BaseResponse<List<ChatVO>> listChats(HttpServletRequest request) {
        LoginUserVO loginUser = userService.getLoginUser(request);
        return BaseResponse.success(chatService.listChats(loginUser.getUserId()));
    }

    /**
     * 查询单个会话详情
     */
    @GetMapping("/{chatId}")
    public BaseResponse<ChatVO> getChat(@PathVariable String chatId,
                                        HttpServletRequest request) {
        LoginUserVO loginUser = userService.getLoginUser(request);
        ChatRequest chatRequest = buildChatRequest(loginUser.getUserId(), chatId);
        return BaseResponse.success(chatService.getChatDetail(chatRequest));
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/{chatId}")
    public BaseResponse<ChatVO> updateChat(@PathVariable String chatId,
                                           @RequestBody ChatUpdateRequest updateRequest,
                                           HttpServletRequest request) {
        LoginUserVO loginUser = userService.getLoginUser(request);
        ChatRequest chatRequest = buildChatRequest(loginUser.getUserId(), chatId);
        return BaseResponse.success(chatService.updateChatTitle(chatRequest, updateRequest.getTitle()));
    }

    /**
     * 删除会话（级联清理 chat_history 与会话文件目录）
     */
    @DeleteMapping("/{chatId}")
    public BaseResponse<Boolean> deleteChat(@PathVariable String chatId,
                                            HttpServletRequest request) {
        LoginUserVO loginUser = userService.getLoginUser(request);
        ChatRequest chatRequest = buildChatRequest(loginUser.getUserId(), chatId);
        chatService.deleteChat(chatRequest);
        return BaseResponse.success(true);
    }

    private ChatRequest buildChatRequest(Integer userId, String chatId) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setUserId(String.valueOf(userId));
        chatRequest.setChatId(chatId);
        return chatRequest;
    }
}
