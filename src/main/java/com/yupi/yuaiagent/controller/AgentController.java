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
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

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
        chatService.save(chat);
        return BaseResponse.success(chat.getChatId());
    }

    /**
     * 流式对话：预加载记忆 → 转发 Python /ai/chat/stream → 落库 AI 回复
     * SSE 流式响应，保持 Flux 直出，不使用 BaseResponse 包装
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        LoginUserVO loginUser = userService.getLoginUser(httpRequest);
        request.setUserId(String.valueOf(loginUser.getUserId()));

        chatHistoryService.preload(request.getUserId(), request.getChatId(), request.getMessage());

        final String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            return Flux.error(new IllegalStateException("Failed to serialize ChatRequest", e));
        }

        return Flux.defer(() -> Flux.<String>create(sink -> {
                    try {
                        StringBuilder aiResponse = new StringBuilder();
                        HttpClient httpClient = HttpClient.newHttpClient();
                        HttpRequest pythonHttpRequest = HttpRequest.newBuilder()
                                .uri(URI.create(pythonBaseUrl + "/ai/chat/stream"))
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .header("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                .build();

                        HttpResponse<InputStream> response = httpClient.send(
                                pythonHttpRequest, HttpResponse.BodyHandlers.ofInputStream());
                        if (response.statusCode() != 200) {
                            sink.error(new IllegalStateException(
                                    "Python chat stream failed: HTTP " + response.statusCode()));
                            return;
                        }

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data: ")) {
                                    continue;
                                }
                                String data = line.substring(6);
                                if ("done".equalsIgnoreCase(data) || "[DONE]".equals(data)) {
                                    sink.next("done");
                                    break;
                                }
                                aiResponse.append(data);
                                sink.next(data);
                            }
                        }

                        chatHistoryService.saveAiResponse(
                                request.getUserId(), request.getChatId(), aiResponse.toString());
                        sink.complete();
                    } catch (Exception e) {
                        sink.error(e);
                    }
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
