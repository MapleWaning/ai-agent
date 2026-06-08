package com.yupi.yuaiagent.model.dto;

import lombok.Data;

@Data
public class FileRequest {

    /**
     * 对话 ID
     */
    private Long chatId;

    /**
     * 文件名
     */
    private String fileName;
}
