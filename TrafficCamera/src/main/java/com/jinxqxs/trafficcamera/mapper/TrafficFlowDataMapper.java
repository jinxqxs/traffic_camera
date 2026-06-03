package com.jinxqxs.trafficcamera.mapper;

import java.util.List;
import java.util.Map;

import com.jinxqxs.trafficcamera.pojo.TrafficFlowData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrafficFlowDataMapper {

    int insertTrafficFlowData(TrafficFlowData trafficFlowData);

    // 核心：5分钟聚合查询
    List<Map<String, Object>> selectTrafficTrends5Min(
            @Param("cameraId") Long cameraId,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("intervalType") String intervalType);
}