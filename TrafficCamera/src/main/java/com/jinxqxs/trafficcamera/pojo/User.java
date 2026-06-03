package com.jinxqxs.trafficcamera.pojo;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    /** admin / user */
    private String role;
    private String createTime;
}
