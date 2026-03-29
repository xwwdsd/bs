package com.cs2trade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 配置静态资源映射等
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-04
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.path:./uploads/}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射上传文件的访问路径
        // 将 /uploads/** 映射到本地文件系统
        String resourcePath = "file:" + uploadPath;
        // 如果是相对路径，需要处理一下，不过 file:./uploads/ 在Spring Boot中通常能工作
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourcePath);
    }
}
