package com.jinxqxs.trafficcamera.controller;

import com.jinxqxs.trafficcamera.security.RefreshTokenBlacklist;
import com.jinxqxs.trafficcamera.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Token 刷新接口 —— 使用 Refresh Token 换取新的 Access Token。
 * 采用 Refresh Token 轮转策略：每次刷新同时返回一对新的 Access + Refresh。
 * 旧的 Refresh Token 加入黑名单，防止重放攻击。
 */
@RestController
@RequiredArgsConstructor
public class TokenController {
    
    private final JwtUtil jwtUtil;
    private final RefreshTokenBlacklist blacklist;

    @PostMapping("/refresh")
    public Map<String, Object> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        Map<String, Object> result = new HashMap<>();

        // 1. 校验 Refresh Token 是否被拉黑
        if (refreshToken == null || blacklist.isBlacklisted(refreshToken)) {
            result.put("code", 401);
            result.put("msg", "Refresh Token 已失效，请重新登录");
            return result;
        }

        // 2. 校验 Refresh Token 本身是否合法
        if (!jwtUtil.validateToken(refreshToken)) {
            result.put("code", 401);
            result.put("msg", "Refresh Token 无效或已过期，请重新登录");
            return result;
        }

        // 3. 从 Refresh Token 中提取用户信息
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        String role = jwtUtil.getRoleFromToken(refreshToken);

        // 4. 将旧 Refresh Token 拉黑（防重放）
        blacklist.add(refreshToken, System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L);

        // 5. 颁发全新的双 Token（轮转，保留角色）
        String newAccessToken = jwtUtil.generateAccessToken(userId, username, role);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username, role);

        result.put("code", 200);
        result.put("msg", "Token 刷新成功");

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", newAccessToken);
        data.put("refreshToken", newRefreshToken);
        data.put("username", username);
        data.put("role", role);
        result.put("data", data);

        return result;
    }

    /**
     * 登出 —— 将当前 Access Token 加入黑名单
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestBody Map<String, String> body) {
        String accessToken = body.get("accessToken");
        Map<String, Object> result = new HashMap<>();

        if (accessToken != null) {
            // 将 Access Token 拉黑，防止退出后继续使用
            blacklist.add(accessToken, System.currentTimeMillis() + 30 * 60 * 1000L);
        }

        result.put("code", 200);
        result.put("msg", "已退出登录");
        return result;
    }
}
