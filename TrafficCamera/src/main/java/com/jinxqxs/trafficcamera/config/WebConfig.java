package com.jinxqxs.trafficcamera.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 保留 WebMvcConfigurer 用于日后扩展（如静态资源映射）。
 * 鉴权逻辑已完全迁移至 Spring Security + JwtAuthenticationTokenFilter。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}