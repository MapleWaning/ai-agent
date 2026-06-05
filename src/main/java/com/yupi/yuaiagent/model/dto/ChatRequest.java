package com.yupi.yuaiagent.model.dto;

import com.yupi.yuaiagent.model.enums.RouteType;
import lombok.Data;

@Data
public class ChatRequest {

    private String message;

    private String userId;

    private String chatId;

    /**
     * 推荐的路由类型
     */
    private RouteType routeType;
}
