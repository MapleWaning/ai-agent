package com.yupi.yuaiagent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yuaiagent.common.ErrorCode;
import com.yupi.yuaiagent.constant.FileConstant;
import com.yupi.yuaiagent.exception.BusinessException;
import com.yupi.yuaiagent.mapper.ChatMapper;
import com.yupi.yuaiagent.model.dto.ChatRequest;
import com.yupi.yuaiagent.model.entity.Chat;
import com.yupi.yuaiagent.model.entity.ChatHistory;
import com.yupi.yuaiagent.model.vo.ChatVO;
import com.yupi.yuaiagent.service.ChatHistoryService;
import com.yupi.yuaiagent.service.ChatService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat> implements ChatService {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Override
    public List<ChatVO> listChats(Integer userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return lambdaQuery()
                .eq(Chat::getUserId, userId)
                .orderByDesc(Chat::getModifyTime)
                .list()
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public ChatVO getChatDetail(ChatRequest request) {
        return toVO(getChatByUser(request));
    }

    @Override
    public ChatVO updateChatTitle(ChatRequest request, String title) {
        if (StrUtil.isBlank(title)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题不能为空");
        }
        if (title.length() > 255) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题长度不能超过255个字符");
        }

        Chat chat = getChatByUser(request);
        chat.setTitle(title);
        boolean updated = updateById(chat);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新会话标题失败");
        }
        return toVO(getById(chat.getChatId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChat(ChatRequest request) {
        Chat chat = getChatByUser(request);
        Integer chatId = chat.getChatId();
        Integer userId = chat.getUserId();

        chatHistoryService.lambdaUpdate()
                .eq(ChatHistory::getChatId, chatId)
                .eq(ChatHistory::getUserId, userId)
                .remove();

        deleteChatWorkspace(userId, chatId);

        boolean removed = removeById(chatId);
        if (!removed) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除会话失败");
        }
    }

    private Chat getChatByUser(ChatRequest request) {
        Integer userId = parseUserId(request.getUserId());
        Integer chatId = parseChatId(request.getChatId());
        Chat chat = getById(chatId);
        if (chat == null || !userId.equals(chat.getUserId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "会话不存在");
        }
        return chat;
    }

    private ChatVO toVO(Chat chat) {
        ChatVO chatVO = new ChatVO();
        BeanUtils.copyProperties(chat, chatVO);
        return chatVO;
    }

    private void deleteChatWorkspace(Integer userId, Integer chatId) {
        Path chatDir = getChatDir(userId, chatId);
        if (!Files.exists(chatDir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(chatDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除会话文件失败");
                }
            });
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除会话文件失败");
        }
    }

    private Path getChatDir(Integer userId, Integer chatId) {
        Path tmpRoot = Paths.get(FileConstant.FILE_SAVE_DIR)
                .toAbsolutePath()
                .normalize();
        return tmpRoot
                .resolve(String.valueOf(userId))
                .resolve(String.valueOf(chatId))
                .normalize();
    }

    private Integer parseUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        try {
            return Integer.valueOf(userId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 非法");
        }
    }

    private Integer parseChatId(String chatId) {
        if (StrUtil.isBlank(chatId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "chatId 不能为空");
        }
        try {
            int id = Integer.parseInt(chatId);
            if (id <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "chatId 非法");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "chatId 非法");
        }
    }
}
