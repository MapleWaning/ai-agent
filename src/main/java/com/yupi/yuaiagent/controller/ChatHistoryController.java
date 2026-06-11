package com.yupi.yuaiagent.controller;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.yupi.yuaiagent.common.BaseResponse;

import com.yupi.yuaiagent.common.ErrorCode;

import com.yupi.yuaiagent.exception.BusinessException;

import com.yupi.yuaiagent.model.entity.Chat;

import com.yupi.yuaiagent.model.entity.ChatHistory;

import com.yupi.yuaiagent.model.vo.LoginUserVO;

import com.yupi.yuaiagent.service.ChatHistoryService;

import com.yupi.yuaiagent.service.ChatService;

import com.yupi.yuaiagent.service.UserService;

import jakarta.annotation.Resource;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController

@RequestMapping("/chatHistory")

public class ChatHistoryController {



    @Resource

    private ChatHistoryService chatHistoryService;



    @Resource

    private ChatService chatService;



    @Resource

    private UserService userService;



    /**

     * 按会话 ID 游标分页查询对话历史（id DESC，最新在前）

     * 首次加载不传 lastId；加载更早历史时传上一页最后一条的 id

     */

    @GetMapping("/chat/{chatId}")

    public BaseResponse<Page<ChatHistory>> listChatHistory(@PathVariable Integer chatId,

                                                           @RequestParam(defaultValue = "10") Integer pageSize,

                                                           @RequestParam(required = false) Integer lastId,

                                                           HttpServletRequest request) {

        if (chatId == null || chatId <= 0) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话ID不能为空");

        }

        if (pageSize < 1 || pageSize > 50) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");

        }



        LoginUserVO loginUser = userService.getLoginUser(request);

        validateChatOwnership(loginUser.getUserId(), chatId);



        LambdaQueryWrapper<ChatHistory> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(ChatHistory::getChatId, chatId);

        queryWrapper.eq(ChatHistory::getUserId, loginUser.getUserId());

        if (lastId != null) {

            queryWrapper.lt(ChatHistory::getId, lastId);

        }

        queryWrapper.orderByDesc(ChatHistory::getId);



        Page<ChatHistory> page = chatHistoryService.page(new Page<>(1, pageSize), queryWrapper);

        return BaseResponse.success(page);

    }



    private void validateChatOwnership(Integer userId, Integer chatId) {

        Chat chat = chatService.getById(chatId);

        if (chat == null || !userId.equals(chat.getUserId())) {

            throw new BusinessException(ErrorCode.OPERATION_ERROR, "会话不存在");

        }

    }

}


