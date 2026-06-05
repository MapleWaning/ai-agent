package com.yupi.yuaiagent.model.dto;

import lombok.Data;

@Data
public class RouteRequest {

    /**
     * 用户初始输入，用于路由决策
     */
    private String initPrompt;
}
