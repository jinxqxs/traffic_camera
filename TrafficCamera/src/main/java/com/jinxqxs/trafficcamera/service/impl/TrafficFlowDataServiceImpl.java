package com.jinxqxs.trafficcamera.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.jinxqxs.trafficcamera.mapper.TrafficFlowDataMapper;
import com.jinxqxs.trafficcamera.pojo.TrafficFlowData;
import com.jinxqxs.trafficcamera.service.TrafficFlowDataService;

@Service
public class TrafficFlowDataServiceImpl implements TrafficFlowDataService {

    @Autowired
    private TrafficFlowDataMapper trafficFlowDataMapper;

    @Override
    public int insert(TrafficFlowData trafficFlowData) {
        return trafficFlowDataMapper.insertTrafficFlowData(trafficFlowData);
    }
}