package com.jinxqxs.trafficcamera.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import com.alibaba.excel.EasyExcel;
import com.jinxqxs.trafficcamera.pojo.TrafficCamera;
import com.jinxqxs.trafficcamera.pojo.Result;
import com.jinxqxs.trafficcamera.pojo.PageResult;
import com.jinxqxs.trafficcamera.service.TrafficCameraService;
import com.jinxqxs.trafficcamera.service.TrafficFlowDataService;
import com.jinxqxs.trafficcamera.pojo.TrafficFlowData; // 假设你在pojo下有这个类

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
// 若使用EasyExcel导出，请引入依赖
// import com.alibaba.excel.EasyExcel;

/**
 * 交通监控设备Controller
 *
 * @author custom
 * @date 2026-04-08
 */
@Slf4j
@RestController
@RequestMapping("/traffic/camera")
public class TrafficCameraController {

    @Autowired
    private TrafficCameraService trafficCameraService;

    @Autowired
    private TrafficFlowDataService trafficFlowDataService;

    /**
     * 查询交通监控设备列表
     */
//    @PreAuthorize("hasAuthority('traffic:camera:list')") // 替换为标准Spring Security权限
    @GetMapping("/list")
    public Result<PageResult<TrafficCamera>> list(
            TrafficCamera trafficCamera,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 使用原生 PageHelper 开启分页
        PageHelper.startPage(pageNum, pageSize);
        List<TrafficCamera> list = trafficCameraService.selectTrafficCameraList(trafficCamera);
        PageInfo<TrafficCamera> pageInfo = new PageInfo<>(list);

        return Result.success(new PageResult<>(pageInfo.getTotal(), list));
    }

    /**
     * 导出交通监控设备列表 (推荐使用 EasyExcel 替代若依的 ExcelUtil)
     */
//    @PreAuthorize("hasAuthority('traffic:camera:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, TrafficCamera trafficCamera) throws IOException {
        List<TrafficCamera> list = trafficCameraService.selectTrafficCameraList(trafficCamera);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("交通监控设备数据", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), TrafficCamera.class)
                .sheet("交通监控设备数据")
                .doWrite(list);
    }

    /**
     * 获取交通监控设备详细信息
     */
//    @PreAuthorize("hasAuthority('traffic:camera:query')")
    @GetMapping(value = "/{cameraId}")
    public Result<TrafficCamera> getInfo(@PathVariable("cameraId") Long cameraId) {
        return Result.success(trafficCameraService.selectTrafficCameraByCameraId(cameraId));
    }

    /**
     * 新增交通监控设备
     */
//    @PreAuthorize("hasAuthority('traffic:camera:add')")
    @PostMapping
    public Result<Void> add(@RequestBody TrafficCamera trafficCamera) {
        return Result.toAjax(trafficCameraService.insertTrafficCamera(trafficCamera));
    }

    /**
     * 修改交通监控设备
     */
//    @PreAuthorize("hasAuthority('traffic:camera:edit')")
    @PutMapping
    public Result<Void> edit(@RequestBody TrafficCamera trafficCamera) {
        return Result.toAjax(trafficCameraService.updateTrafficCamera(trafficCamera));
    }

    /**
     * 删除交通监控设备
     */
//    @PreAuthorize("hasAuthority('traffic:camera:remove')")
    @DeleteMapping("/{cameraIds}")
    public Result<Void> remove(@PathVariable Long[] cameraIds) {
        return Result.toAjax(trafficCameraService.deleteTrafficCameraByCameraIds(cameraIds));
    }

    /**
     * 获取视频大屏设备列表（不分页）
     */
//    @PreAuthorize("hasAuthority('traffic:camera:query')")
    @GetMapping("/videoList")
    public Result<List<TrafficCamera>> videoList(TrafficCamera trafficCamera) {
        List<TrafficCamera> list = trafficCameraService.selectTrafficCameraList(trafficCamera);
        return Result.success(list);
    }

    /**
     * 接收yolo数据
     */
    @PostMapping("/report")
    public Result<Void> reportData(@RequestBody TrafficFlowData data) {
        // 1. 存入数据库
        trafficFlowDataService.insert(data);
        // 2. 调用报警检查逻辑
        checkAlarm(data);
        return Result.success();
    }

    /**
     * 校验车流量是否超过阈值并触发报警
     */
    private void checkAlarm(TrafficFlowData data) {
        TrafficCamera camera = trafficCameraService.selectTrafficCameraByCameraId(data.getCameraId());
        int threshold = 100;

        if (data.getCarCount() != null && data.getCarCount() > threshold) {
            // 使用标准的 slf4j 打印日志，替代 System.err
            log.warn("【报警通知】摄像头：{} 流量超标！当前值：{}", camera.getCameraName(), data.getCarCount());
            // TODO: 这里以后可以写：存入报警记录表、发送邮件、或者通过 WebSocket 推送前端
        }
    }


}