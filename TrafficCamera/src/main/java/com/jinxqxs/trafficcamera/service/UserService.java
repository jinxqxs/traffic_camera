package com.jinxqxs.trafficcamera.service;

import com.jinxqxs.trafficcamera.pojo.User;

import java.util.List;

public interface UserService {
    User findByUsername(String username);
    List<User> findAll();
    int addUser(User user);
    int updateUser(User user);
    int deleteUser(Long id);
}
