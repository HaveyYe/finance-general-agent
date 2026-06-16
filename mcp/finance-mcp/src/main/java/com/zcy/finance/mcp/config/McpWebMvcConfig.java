package com.zcy.finance.mcp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class McpWebMvcConfig implements WebMvcConfigurer {

    private final McpAuthInterceptor authInterceptor;

    public McpWebMvcConfig(McpAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/mcp/**");
    }
}
