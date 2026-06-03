package com.jinxqxs.trafficcamera.controller;

import com.jinxqxs.trafficcamera.pojo.Result;
import com.jinxqxs.trafficcamera.pojo.User;
import com.jinxqxs.trafficcamera.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 用户列表 */
    @GetMapping("/list")
    public Result<List<User>> list() {
        return Result.success(userService.findAll());
    }

    /** 新增用户 */
    @PostMapping
    public Result<Void> add(@RequestBody User user) {
        // 检查用户名是否已存在
        if (userService.findByUsername(user.getUsername()) != null) {
            return Result.error("用户名已存在");
        }
        return Result.toAjax(userService.addUser(user));
    }

    /** 修改用户（改密码/角色） */
    @PutMapping
    public Result<Void> edit(@RequestBody User user) {
        return Result.toAjax(userService.updateUser(user));
    }

    /** 删除用户 */
    @DeleteMapping("/{userId}")
    public Result<Void> remove(@PathVariable Long userId) {
        return Result.toAjax(userService.deleteUser(userId));
    }
}
