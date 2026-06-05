package com.yupi.yuaiagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yuaiagent.model.dto.ChatRequest;
import com.yupi.yuaiagent.model.dto.RouteRequest;
import com.yupi.yuaiagent.model.entity.Chat;
import com.yupi.yuaiagent.model.vo.RouteResponse;
import com.yupi.yuaiagent.service.ChatHistoryService;
import com.yupi.yuaiagent.service.ChatService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public RouteResponse routing(@RequestBody RouteRequest request) {
        return restClient.post()
                .uri("/ai/chat/route")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(RouteResponse.class);
    }

    /**
     * 创建会话
     */
    @PostMapping("/chat/create")
    public Integer createChat(@RequestParam String userId) {
        Chat chat = new Chat();
        chat.setUserId(Integer.valueOf(userId));
        chatService.save(chat);
        return chat.getChatId();
    }

    /**
     * 流式对话：预加载记忆 → 转发 Python /ai/chat/stream → 落库 AI 回复
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
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
                        HttpRequest httpRequest = HttpRequest.newBuilder()
                                .uri(URI.create(pythonBaseUrl + "/ai/chat/stream"))
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .header("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                .build();

                        HttpResponse<InputStream> response = httpClient.send(
                                httpRequest, HttpResponse.BodyHandlers.ofInputStream());
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
