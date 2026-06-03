package com.jinxqxs.trafficcamera.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Refresh Token 黑名单 —— 轮转后被废弃的旧 Refresh Token 加入此表。
 * 生产环境建议用 Redis + TTL 替代本内存实现。
 */
@Component
public class RefreshTokenBlacklist {

    /**
     * key   = Refresh Token 的 JWT ID (或 Token 明文)
     * value = 失效时间戳（ms）
     */
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * 将指定 Refresh Token 加入黑名单，标记其失效时间
     */
    public void add(String refreshToken, long expiryMs) {
        blacklist.put(refreshToken, expiryMs);
        // 惰性清理已过期的条目
        blacklist.entrySet().removeIf(e -> e.getValue() < System.currentTimeMillis());
    }

    /**
     * 判断该 Refresh Token 是否已被拉黑
     */
    public boolean isBlacklisted(String refreshToken) {
        Long expiry = blacklist.get(refreshToken);
        if (expiry == null) {
            return false;
        }
        // 如果已超过原过期时间，自动清理
        if (expiry < System.currentTimeMillis()) {
            blacklist.remove(refreshToken);
            return false;
        }
        return true;
    }
}
