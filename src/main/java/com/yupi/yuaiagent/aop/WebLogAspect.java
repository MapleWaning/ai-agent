package com.yupi.yuaiagent.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 接口请求日志切面：统一打印 controller 层每个请求的入参、出参、耗时以及异常信息。
 */
@Slf4j
@Aspect
@Component
public class WebLogAspect {

    private final ObjectMapper objectMapper;

    /**
     * 是否在请求日志中打印发送参数（入参），由 application.yml 中 yu.log.show-request-args 控制
     */
    @Value("${yu.log.show-request-args:true}")
    private boolean showRequestArgs;

    public WebLogAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 拦截 controller 包下所有方法
     */
    @Pointcut("execution(* com.yupi.yuaiagent.controller..*(..))")
    public void controllerMethods() {
    }

    @Around("controllerMethods()")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        String httpMethod = "-";
        String uri = "-";
        String queryString = null;
        String clientIp = "-";
        HttpServletRequest request = currentRequest();
        if (request != null) {
            httpMethod = request.getMethod();
            uri = request.getRequestURI();
            queryString = request.getQueryString();
            clientIp = resolveClientIp(request);
        }

        String fullUri = queryString == null ? uri : uri + "?" + queryString;
        if (showRequestArgs) {
            log.info("[{}] >>> 请求开始 {} {} | 方法={} | IP={} | 参数={}",
                    requestId, httpMethod, fullUri, methodName, clientIp, formatArgs(joinPoint.getArgs()));
        } else {
            log.info("[{}] >>> 请求开始 {} {} | 方法={} | IP={}",
                    requestId, httpMethod, fullUri, methodName, clientIp);
        }

        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - startTime;
            log.info("[{}] <<< 请求结束 {} {} | 方法={} | 耗时={}ms | 返回={}",
                    requestId, httpMethod, fullUri, methodName, cost, formatResult(result));
            return result;
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("[{}] !!! 请求异常 {} {} | 方法={} | 耗时={}ms | 异常={}: {}",
                    requestId, httpMethod, fullUri, methodName, cost,
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        try {
            String serialized = Arrays.stream(args)
                    .filter(this::isSerializable)
                    .map(this::safeToJson)
                    .collect(Collectors.joining(", "));
            return "[" + serialized + "]";
        } catch (Exception e) {
            return "[参数序列化失败]";
        }
    }

    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }
        // 流式响应、文件等不做序列化，避免阻塞或大对象刷屏
        String typeName = result.getClass().getSimpleName();
        if (result instanceof reactor.core.publisher.Flux
                || result instanceof org.springframework.http.ResponseEntity
                || result instanceof org.springframework.core.io.Resource) {
            return "<" + typeName + ">";
        }
        return safeToJson(result);
    }

    private boolean isSerializable(Object arg) {
        return !(arg instanceof HttpServletRequest)
                && !(arg instanceof jakarta.servlet.http.HttpServletResponse)
                && !(arg instanceof MultipartFile);
    }

    private String safeToJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
