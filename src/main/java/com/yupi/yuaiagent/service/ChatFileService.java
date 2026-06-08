package com.yupi.yuaiagent.service;

import com.yupi.yuaiagent.model.vo.ChatFileVO;
import org.springframework.core.io.Resource;

import java.util.List;

public interface ChatFileService {

    /**
     * 获取某个用户某个对话的文件列表
     */
    List<ChatFileVO> listFiles(Long userId, Long chatId);

    /**
     * 加载指定文件资源
     */
    Resource loadFileAsResource(Long userId, Long chatId, String fileName);
}
