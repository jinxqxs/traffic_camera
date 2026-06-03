package com.jinxqxs.trafficcamera.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Access Token 过期时间：30 分钟（毫秒）
     */
    private static final long ACCESS_EXPIRATION =30*10* 1000L;

    /**
     * Refresh Token 过期时间：7 天（毫秒）
     */
    private static final long REFRESH_EXPIRATION = 24 * 60 * 60 * 1000L;

    /**
     * 按 Base64 解码密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Access Token（短时效）
     */
    public String generateAccessToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, ACCESS_EXPIRATION);
    }

    /**
     * 生成 Refresh Token（长时效）
     */
    public String generateRefreshToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, REFRESH_EXPIRATION);
    }

    /**
     * 通用 Token 生成
     */
    private String generateToken(Long userId, String username, String role, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 中获取角色
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * 解析 Token 中的所有声明
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 校验 Token 是否有效（不区分 Access / Refresh）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
