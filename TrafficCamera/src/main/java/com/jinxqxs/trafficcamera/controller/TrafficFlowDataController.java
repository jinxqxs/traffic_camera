package com.jinxqxs.trafficcamera.controller;

import com.jinxqxs.trafficcamera.pojo.Result;
import com.jinxqxs.trafficcamera.mapper.TrafficFlowDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/traffic/analysis")
public class TrafficFlowDataController {

    @Autowired
    private TrafficFlowDataMapper flowDataMapper;

    @GetMapping("/trends")
    public Result<Object> getTrends(Long cameraId, String beginTime, String endTime,String intervalType) {
        // 默认值：使用你已经插入的测试数据时间
        if (cameraId == null) cameraId = 100L;
        if (beginTime == null) beginTime = "2025-12-28 00:00:00";
        if (endTime == null) endTime = "2025-12-28 00:30:00";

        // 打印参数，确认是否正确
        System.out.println("cameraId: " + cameraId);
        System.out.println("beginTime: " + beginTime);
        System.out.println("endTime: " + endTime);

        return Result.success(flowDataMapper.selectTrafficTrends5Min(cameraId, beginTime, endTime,intervalType));
    }
}