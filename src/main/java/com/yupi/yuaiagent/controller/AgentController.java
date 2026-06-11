package com.yupi.yuaiagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.model.dto.ChatRequest;
import com.yupi.yuaiagent.model.dto.RouteRequest;
import com.yupi.yuaiagent.model.entity.Chat;
import com.yupi.yuaiagent.model.vo.LoginUserVO;
import com.yupi.yuaiagent.model.vo.RouteResponse;
import com.yupi.yuaiagent.service.ChatHistoryService;
import com.yupi.yuaiagent.service.ChatService;
import com.yupi.yuaiagent.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/agent")
public class AgentController {

    private final RestClient restClient;
    private final String pythonBaseUrl;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ChatService chatService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private UserService userService;

    public AgentController(@Value("${python-agent.base-url:http://localhost:8000}") String pythonBaseUrl) {
        this.pythonBaseUrl = pythonBaseUrl;
        this.restClient = RestClient.builder()
                .baseUrl(pythonBaseUrl)
                .build();
    }

    /**
     * 路由决策：转发至 Python 服务的 /ai/chat/route
     */
    @PostMapping("/chat/route")
    public BaseResponse<RouteResponse> routing(@RequestBody RouteRequest request) {
        RouteResponse routeResponse = restClient.post()
                .uri("/ai/chat/route")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(RouteResponse.class);
        return BaseResponse.success(routeResponse);
    }

    /**
     * 创建会话
     */
    @PostMapping("/chat/create")
    public BaseResponse<Integer> createChat(HttpServletRequest httpRequest) {
        LoginUserVO loginUser = userService.getLoginUser(httpRequest);
        Chat chat = new Chat();
        chat.setUserId(loginUser.getUserId());
        chat.setTitle("新会话");
        chatService.save(chat);
        return BaseResponse.success(chat.getChatId());
    }

    /**
     * 流式对话：预加载记忆 → 转发 Python /ai/chat/stream → 落库 AI 回复
     * SSE 流式响应，保持 Flux 直出，不使用 BaseResponse 包装
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        SseEmitter emitter = new SseEmitter(0L);

        final String requestBody;
        final String userId;
        final String chatId;
        final String type;

        try {
            // 建议放在异步线程外，避免异步线程中继续依赖 HttpServletRequest
            LoginUserVO loginUser = userService.getLoginUser(httpRequest);
            userId = String.valueOf(loginUser.getUserId());

            request.setUserId(userId);

            chatId = request.getChatId();

            type = request.getRouteType() != null
                    ? request.getRouteType().getValue()
                    : null;

            chatHistoryService.preload(
                    request.getUserId(),
                    request.getChatId(),
                    request.getMessage(),
                    type
            );

            requestBody = objectMapper.writeValueAsString(request);

        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        CompletableFuture.runAsync(() -> {
            StringBuilder aiResponse = new StringBuilder();

            // 只保存需要历史回放的事件，例如 workflow_step/tool_start/tool_end/file
            List<Map<String, Object>> streamEvents = new ArrayList<>();

            try {
                HttpClient httpClient = HttpClient.newHttpClient();

                HttpRequest pythonHttpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(pythonBaseUrl + "/ai/chat/stream"))
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .header("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<InputStream> response = httpClient.send(
                        pythonHttpRequest,
                        HttpResponse.BodyHandlers.ofInputStream()
                );

                if (response.statusCode() != 200) {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("Python chat stream failed: HTTP " + response.statusCode()));

                    emitter.completeWithError(new IllegalStateException(
                            "Python chat stream failed: HTTP " + response.statusCode()
                    ));
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {

                    readPythonSseAndForward(
                            reader,
                            emitter,
                            aiResponse,
                            streamEvents
                    );
                }

                // 如果你暂时不想改 service，可以先继续用旧方法：
                // chatHistoryService.saveAiResponse(userId, chatId, aiResponse.toString(), type);

                // 推荐后续改成支持 events 的方法
                chatHistoryService.saveAiResponse(
                        userId,
                        chatId,
                        aiResponse.toString(),
                        type,
                        streamEvents
                );

                emitter.complete();

            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(e.getMessage()));
                } catch (Exception ignored) {
                }

                emitter.completeWithError(e);
            }
        });

        return emitter;
    }




    private boolean handlePythonSseEvent(
        String eventName,
        String rawData,
        SseEmitter emitter,
        StringBuilder aiResponse,
        List<Map<String, Object>> streamEvents
    ) throws IOException {
        Object dataObj = parseSseData(rawData);

        // 兼容旧格式：没有 event，只靠 data=done 结束
        if ("done".equals(eventName)
                || "done".equalsIgnoreCase(String.valueOf(dataObj))
                || "[DONE]".equals(String.valueOf(dataObj))) {

            emitter.send(SseEmitter.event()
                    .name("done")
                    .data("done"));

            return false;
        }

        switch (eventName) {
            case "message" -> {
                String text = String.valueOf(dataObj);

                // 只有正文 token 才拼进最终 AI 回复
                aiResponse.append(text);

                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(text));
            }

            case "route" -> {
                // route 只转发给前端，不拼进正文
                emitter.send(SseEmitter.event()
                        .name("route")
                        .data(dataObj));
            }

            case "status" -> {
                // status 通常是临时状态，默认不落库
                emitter.send(SseEmitter.event()
                        .name("status")
                        .data(dataObj));
            }

            case "workflow_step", "tool_start", "tool_end", "tool_error", "file" -> {
                // 这些事件对历史回放有价值，可以保存
                Map<String, Object> eventRecord = new LinkedHashMap<>();
                eventRecord.put("event", eventName);
                eventRecord.put("data", dataObj);
                streamEvents.add(eventRecord);

                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(dataObj));
            }

            case "error" -> {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(dataObj));

                // 这里不一定要 return false，看你是否希望错误后立刻终止
                return false;
            }

            default -> {
                // 未知事件：转发给前端，但默认不拼进正文
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(dataObj));
            }
        }

        return true;
    }

    private void readPythonSseAndForward(
        BufferedReader reader,
        SseEmitter emitter,
        StringBuilder aiResponse,
        List<Map<String, Object>> streamEvents
    ) throws IOException {
    String eventName = "message";
    StringBuilder dataBuilder = new StringBuilder();

    String line;

    while ((line = reader.readLine()) != null) {
        // 空行表示一个完整 SSE 事件结束
        if (line.isEmpty()) {
            if (dataBuilder.length() > 0) {
                boolean shouldContinue = handlePythonSseEvent(
                        eventName,
                        dataBuilder.toString(),
                        emitter,
                        aiResponse,
                        streamEvents
                );

                if (!shouldContinue) {
                    break;
                }
            }

            // 重置，准备解析下一个 SSE event
            eventName = "message";
            dataBuilder.setLength(0);
            continue;
        }

        if (line.startsWith("event:")) {
            eventName = line.substring(6).trim();
            continue;
        }

        if (line.startsWith("data:")) {
            String raw = line.substring(5);

            if (raw.startsWith(" ")) {
                raw = raw.substring(1);
            }

            // SSE 允许多行 data，这里兼容一下
            if (dataBuilder.length() > 0) {
                dataBuilder.append("\n");
            }

            dataBuilder.append(raw);
        }
    }

    // 兼容最后一个事件没有空行结束的情况
    if (dataBuilder.length() > 0) {
        handlePythonSseEvent(
                eventName,
                dataBuilder.toString(),
                emitter,
                aiResponse,
                streamEvents
        );
    }
    }

    private Object parseSseData(String rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return "";
        }
    
        try {
            return objectMapper.readValue(rawData, Object.class);
        } catch (Exception e) {
            return rawData;
        }
    }

}
