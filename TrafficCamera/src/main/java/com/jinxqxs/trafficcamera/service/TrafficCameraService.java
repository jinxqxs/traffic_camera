package com.jinxqxs.trafficcamera.service;

import java.util.List;
import com.jinxqxs.trafficcamera.pojo.TrafficCamera;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Service;

/**
 * 交通监控设备Service接口
 */

@Service
public interface TrafficCameraService {
    TrafficCamera selectTrafficCameraByCameraId(Long cameraId);
    List<TrafficCamera> selectTrafficCameraList(TrafficCamera trafficCamera);
    int insertTrafficCamera(TrafficCamera trafficCamera);
    int updateTrafficCamera(TrafficCamera trafficCamera);
    int deleteTrafficCameraByCameraIds(Long[] cameraIds);
    int deleteTrafficCameraByCameraId(Long cameraId);
}