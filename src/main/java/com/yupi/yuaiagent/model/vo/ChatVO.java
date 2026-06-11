package com.yupi.yuaiagent.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatVO {

    /**
     * 会话 ID
     */
    private Integer chatId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime modifyTime;
}
