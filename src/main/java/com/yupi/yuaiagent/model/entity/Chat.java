package com.yupi.yuaiagent.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("chat")
public class Chat {

    @TableId(value = "chat_id", type = IdType.AUTO)
    private Integer chatId;

    private Integer userId;
}
