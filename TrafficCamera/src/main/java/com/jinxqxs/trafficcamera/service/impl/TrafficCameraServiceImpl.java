package com.jinxqxs.trafficcamera.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jinxqxs.trafficcamera.mapper.TrafficCameraMapper;
import com.jinxqxs.trafficcamera.mapper.TrafficFlowDataMapper;
import com.jinxqxs.trafficcamera.pojo.TrafficCamera;
import com.jinxqxs.trafficcamera.service.TrafficCameraService;
import com.jinxqxs.trafficcamera.service.TrafficPyService;

/**
 * 交通监控设备Service业务层处理
 *
 * @author custom
 * @date 2026-04-08
 */
@Service
public class TrafficCameraServiceImpl implements TrafficCameraService
{
    @Autowired
    private TrafficCameraMapper trafficCameraMapper;

    @Autowired
    private TrafficFlowDataMapper trafficFlowDataMapper;

    @Autowired
    private TrafficPyService trafficPyService;

    /**
     * 查询交通监控设备
     *
     * @param cameraId 交通监控设备主键
     * @return 交通监控设备
     */
    @Override
    public TrafficCamera selectTrafficCameraByCameraId(Long cameraId)
    {
        return trafficCameraMapper.selectTrafficCameraByCameraId(cameraId);
    }

    /**
     * 查询交通监控设备列表
     *
     * @param trafficCamera 交通监控设备
     * @return 交通监控设备
     */
    @Override
    public List<TrafficCamera> selectTrafficCameraList(TrafficCamera trafficCamera)
    {
        return trafficCameraMapper.selectTrafficCameraList(trafficCamera);
    }

    /**
     * 新增交通监控设备
     *
     * @param trafficCamera 交通监控设备
     * @return 结果
     */
    @Override
    public int insertTrafficCamera(TrafficCamera trafficCamera)
    {
        int rows = trafficCameraMapper.insertTrafficCamera(trafficCamera);
        trafficPyService.reload();
        return rows;
    }

    /**
     * 修改交通监控设备
     *
     * @param trafficCamera 交通监控设备
     * @return 结果
     */
    @Override
    public int updateTrafficCamera(TrafficCamera trafficCamera)
    {
        return trafficCameraMapper.updateTrafficCamera(trafficCamera);
    }

    /**
     * 批量删除交通监控设备
     *
     * @param cameraIds 需要删除的交通监控设备主键
     * @return 结果
     */
    @Override
    public int deleteTrafficCameraByCameraIds(Long[] cameraIds)
    {
        trafficFlowDataMapper.deleteByCameraIds(cameraIds);
        int rows = trafficCameraMapper.deleteTrafficCameraByCameraIds(cameraIds);
        trafficPyService.reload();
        return rows;
    }

    /**
     * 删除交通监控设备信息
     *
     * @param cameraId 交通监控设备主键
     * @return 结果
     */
    @Override
    public int deleteTrafficCameraByCameraId(Long cameraId)
    {
        trafficFlowDataMapper.deleteByCameraIds(new Long[]{cameraId});
        int rows = trafficCameraMapper.deleteTrafficCameraByCameraId(cameraId);
        trafficPyService.reload();
        return rows;
    }
}