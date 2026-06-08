package com.yupi.yuaiagent.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebAuthConfig implements WebMvcConfigurer {

    @Resource
    private LoginAuthInterceptor loginAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginAuthInterceptor)
                .addPathPatterns("/user/**", "/agent/**", "/chatHistory/**", "/chat/file/**")
                .excludePathPatterns("/user/login", "/user/register");
    }
}
