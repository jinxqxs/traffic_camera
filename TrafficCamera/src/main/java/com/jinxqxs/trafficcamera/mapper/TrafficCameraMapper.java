package com.jinxqxs.trafficcamera.mapper;


import java.util.List;
import com.jinxqxs.trafficcamera.pojo.TrafficCamera;
import org.apache.ibatis.annotations.Mapper;

/**
 * 交通监控设备Mapper接口
 */
@Mapper
public interface TrafficCameraMapper {
    TrafficCamera selectTrafficCameraByCameraId(Long cameraId);
    List<TrafficCamera> selectTrafficCameraList(TrafficCamera trafficCamera);
    int insertTrafficCamera(TrafficCamera trafficCamera);
    int updateTrafficCamera(TrafficCamera trafficCamera);
    int deleteTrafficCameraByCameraId(Long cameraId);
    int deleteTrafficCameraByCameraIds(Long[] cameraIds);
}