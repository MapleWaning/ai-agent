package com.yupi.yuaiagent.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RouteType {

    NORMAL_CHAT("normal_chat"),
    REPORT("report"),
    RAG("rag"),
    MCP("mcp"),
    TOOL("tool"),
    WORKFLOW("workflow");

    private final String value;

    RouteType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static RouteType normalize(Object value) {
        if (value instanceof RouteType routeType) {
            return routeType;
        }
        if (value instanceof String str) {
            str = str.strip();
            for (RouteType routeType : values()) {
                if (str.equals(routeType.value) || str.equals(routeType.name())) {
                    return routeType;
                }
            }
        }
        return null;
    }

    @JsonCreator
    public static RouteType fromValue(Object value) {
        RouteType routeType = normalize(value);
        if (routeType != null) {
            return routeType;
        }
        throw new IllegalArgumentException("Unknown RouteType: " + value);
    }
}
