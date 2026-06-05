package com.yupi.yuaiagent.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("chat_history")
public class ChatHistory {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer chatId;

    private Integer userId;

    private String content;
}
