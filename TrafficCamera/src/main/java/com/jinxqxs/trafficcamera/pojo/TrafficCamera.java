package com.jinxqxs.trafficcamera.pojo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 交通监控设备对象 traffic_camera
 *
 * @author custom
 * @date 2026-04-08
 */
@Data
public class TrafficCamera implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    @ExcelIgnore // 导出时忽略主键
    private Long cameraId;

    /** 设备名称 */
    @ExcelProperty("设备名称")
    private String cameraName;

    /** 设备编号/序列号 */
    @ExcelProperty("设备编号/序列号")
    private String cameraCode;

    /** 视频流地址(RTSP/RTMP/HTTP) */
    @ExcelProperty("视频流地址(RTSP/RTMP/HTTP)")
    private String videoUrl;

    /** 安装地点 */
    @ExcelProperty("安装地点")
    private String location;


    /** 设备状态（0正常 1停用） */
    @ExcelProperty("设备状态")
    private String status;

    /** 流量报警阈值(单位:辆/小时) */
    @ExcelProperty("流量报警阈值(单位:辆/小时)")
    private Long threshold;


    /** 备注 */
    @ExcelProperty("备注")
    private String remark;
}