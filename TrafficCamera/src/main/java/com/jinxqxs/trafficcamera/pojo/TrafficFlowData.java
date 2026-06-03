package com.jinxqxs.trafficcamera.pojo;

import java.time.LocalDateTime;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrafficFlowData {
    private Long cameraId;
    private Integer carCount;
    private Integer direction;
    private LocalDateTime startTime;
}