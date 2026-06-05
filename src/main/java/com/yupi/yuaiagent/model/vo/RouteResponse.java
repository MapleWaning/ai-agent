package com.yupi.yuaiagent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {

    private String routeType;

    private String enumName;

    private String reason;
}
