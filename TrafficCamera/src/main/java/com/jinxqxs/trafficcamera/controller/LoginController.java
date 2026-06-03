package com.jinxqxs.trafficcamera.controller;

import com.jinxqxs.trafficcamera.pojo.User;
import com.jinxqxs.trafficcamera.service.UserService;
import com.jinxqxs.trafficcamera.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    /**
     * 登录 → 查数据库校验 → 颁发 Access Token + Refresh Token
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        Map<String, Object> result = new HashMap<>();

        // 从数据库查询用户
        User user = userService.findByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            result.put("code", 401);
            result.put("msg", "账号或密码错误");
            return result;
        }

        // 生成双 Token（包含角色）
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());

        result.put("code", 200);
        result.put("msg", "登录成功");

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        result.put("data", data);

        return result;
    }
}