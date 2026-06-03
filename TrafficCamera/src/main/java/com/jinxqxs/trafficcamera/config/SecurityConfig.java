package com.jinxqxs.trafficcamera.config;

import com.jinxqxs.trafficcamera.security.Http401UnauthorizedEntryPoint;
import com.jinxqxs.trafficcamera.security.JwtAuthenticationTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    private final Http401UnauthorizedEntryPoint unauthorizedEntryPoint;

    /**
     * 白名单：不需要认证即可访问的端点
     */
    private static final String[] WHITE_LIST = {
            "/login",        // 登录
            "/refresh",      // 刷新 Token
            "/logout",       // 登出
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 无状态
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                // 未认证 → 401
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(unauthorizedEntryPoint)
                )
                // 鉴权规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        // ===== 管理员专属 =====
                        .requestMatchers(HttpMethod.POST, "/traffic/camera").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/traffic/camera").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/traffic/camera/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").hasRole("ADMIN")
                        // ===== 其余接口只需认证 =====
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}