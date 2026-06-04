<template>
  <div class="app-container">
    <el-card shadow="hover">
      <el-form :inline="true" label-width="100px">
        <el-form-item label="摄像头">
          <el-select v-model="queryParams.cameraId" placeholder="请选择摄像头" clearable style="width: 200px">
            <el-option
                v-for="item in cameraList"
                :key="item.cameraId"
                :label="item.cameraName + ' (' + item.cameraCode + ')'"
                :value="item.cameraId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="统计时间">
          <el-date-picker
              v-model="queryParams.dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              :default-time="defaultTime"
          />
        </el-form-item>

        <el-form-item label="统计间隔">
          <el-select v-model="queryParams.interval" placeholder="请选择统计间隔" style="width: 120px">
            <el-option label="5分钟" value="5min" />
            <el-option label="半小时" value="30min" />
            <el-option label="1小时" value="1hour" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询流量趋势</el-button>
          <el-button type="success" @click="handleExport" :disabled="!exportDataReady">
            导出Excel
          </el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" style="margin-top:20px; height:550px;">
      <div ref="chartRef" style="width:100%;height:500px;"></div>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import request from '@/utils/request'
import { getVideoList } from '@/api/traffic/camera'

export default {
  name: "FlowAnalysis",
  data() {
    return {
      cameraList: [],
      queryParams: {
        cameraId: "",
        dateRange: [],
        interval: "5min"
      },
      defaultTime: [
        new Date(2000, 1, 1, 0, 0, 0),
        new Date(2000, 1, 1, 23, 59, 59)
      ],
      myChart: null,
      currentFullData: [],
      exportDataReady: false
    };
  },
  created() {
    this.setDefaultTime();
    this.loadCameraList();
  },
  mounted() {
    this.myChart = echarts.init(this.$refs.chartRef);
    this.setEmptyOption();
  },
  beforeDestroy() {
    if (this.myChart) {
      this.myChart.dispose();
      this.myChart = null;
    }
  },
  methods: {
    //设置默认时间为最近30分钟
    setDefaultTime() {
      const end = new Date();
      const start = new Date();
      start.setMinutes(start.getMinutes() - 30);
      this.queryParams.dateRange = [this.formatDate(start), this.formatDate(end)];
    },
    // 加载摄像头列表
    async loadCameraList() {
      try {
        const res = await getVideoList({ status: '0' });
        this.cameraList = res.data;
      } catch (e) {
        console.error('加载摄像头列表失败：', e);
      }
    },
    fillMissingData(backendData, beginTime, endTime, interval) {
      const result = [];
      const start = new Date(beginTime);
      const end = new Date(endTime);

      // 计算时间间隔（毫秒）
      let stepMs;
      switch(interval) {
        case '5min':
          stepMs = 5 * 60 * 1000;
          break;
        case '30min':
          stepMs = 30 * 60 * 1000;
          break;
        case '1hour':
          stepMs = 60 * 60 * 1000;
          break;
        default:
          stepMs = 5 * 60 * 1000;
      }

      // 生成所有时间点
      let current = new Date(start);
      while (current <= end) {
        // 拿到格式化的时间，可能是带秒的
        const timeStr = this.formatDate(current);

        // 【修改这里】：都截取前16位进行比对 (YYYY-MM-DD HH:mm)
        const matchKey = timeStr.substring(0, 16);
        const dataPoint = backendData.find(item => {
          // 确保 item.time 存在，并且也只取前 16 位来比对
          return item.time && item.time.substring(0, 16) === matchKey;
        });

        // 补0 (保存到图表的 time 可以直接用截取后的 matchKey，让 X 轴更干净)
        result.push({
          time: matchKey,
          totalCount: dataPoint ? dataPoint.totalCount : 0
        });

        // 步进
        current = new Date(current.getTime() + stepMs);
      }

      return result;
    },

    formatDate(date) {
      const y = date.getFullYear();
      const m = String(date.getMonth() + 1).padStart(2, '0');
      const d = String(date.getDate()).padStart(2, '0');
      const h = String(date.getHours()).padStart(2, '0');
      const min = String(date.getMinutes()).padStart(2, '0');
      const s = String(date.getSeconds()).padStart(2, '0');
      return `${y}-${m}-${d} ${h}:${min}:${s}`;
    },

    setEmptyOption() {
      this.myChart.setOption({
        title: { text: "车流量" + (this.queryParams.interval === '5min' ? '5分钟' : this.queryParams.interval === '30min' ? '半小时' : '1小时') +
              "统计趋势", left: "center"  },
        tooltip: { trigger: "axis" },
        xAxis: {
          type: "category",
          name:"时间",
          data: [],
          axisLabel: { interval: 0, rotate: 45 }
        },
        yAxis: { type: "value" },
        series: [{
          type: "line",
          name: "车辆数(量)",
          smooth: true,
          data: []
        }]
      });
    },

    // 🌟 核心修改：改为 async 异步方法，请求真实后端接口
    async handleQuery() {
      if (!this.queryParams.cameraId) {
        this.$message.warning("请选择摄像头");
        return;
      }
      if (!this.queryParams.dateRange || this.queryParams.dateRange.length !== 2) {
        this.$message.warning("请选择统计时间");
        return;
      }

      // 提取开始时间和结束时间
      const beginTime = this.queryParams.dateRange[0];
      const endTime = this.queryParams.dateRange[1];

      try {
        // 请求后端接口
        const res = await request.get('traffic/analysis/trends',{
          params: {
            cameraId: this.queryParams.cameraId,
            beginTime: beginTime,
            endTime: endTime,
            intervalType: this.queryParams.interval
          }
        });

        // 兼容不同的后端返回格式
        let realData=res.data;
        if (res.data.data) {
          realData = res.data.data; // 格式: { code: 200, data: [...] }
        } else if (Array.isArray(res.data)) {
          realData = res.data;      // 格式: [...] 纯数组
        } else {
          throw new Error("后端返回数据格式不正确，无法解析");
        }

        // 数据为空时的提示
        if (realData.length === 0) {
          this.$message.info("该时间段内暂无数据");
          this.myChart.clear();
          this.setEmptyOption();
          this.currentFullData = [];
          this.exportDataReady = false;
          return;
        }
        const fullData = this.fillMissingData(realData, beginTime, endTime, this.queryParams.interval);
        this.currentFullData = fullData;
        this.exportDataReady = true;
        this.myChart.setOption({
          title: {
            text: "车流量" + (this.queryParams.interval === '5min' ? '5分钟' : this.queryParams.interval === '30min' ? '半小时' : '1小时') + "统计趋势",
            left: "center"
          },
          tooltip: { trigger: "axis" },
          xAxis: {
            type: "category",
            name: "时间",
            data: fullData.map(i => i.time),
            axisLabel: {
              interval: 0,
              rotate: 45,
              formatter: function (value) {
                return value && value.length > 10 ? value.substring(10, 16) : value;
              }
            }
          },
          yAxis: { type: "value",min: 0,scale: true },
          series: [{
            type: "line",
            name: "车辆数（辆）",
            smooth: true,
            data: fullData.map(i => i.totalCount)
          }]
        }, true);

        this.$message.success("查询成功");

      } catch (err) {
        console.error("❌ 请求失败，详情：", err);
        this.$message.error("查询失败：" + (err.response?.data?.message || err.message));
        this.exportDataReady = false;
      }
    },

    resetQuery() {
      this.queryParams.cameraId = "";
      this.setDefaultTime();
      this.myChart.clear();
      this.setEmptyOption();
      this.currentFullData = [];
      this.exportDataReady = false;
    },

    // 【完整新增方法】
    handleExport() {
      if (!this.exportDataReady || this.currentFullData.length === 0) {
        this.$message.warning("请先查询数据再导出");
        return;
      }
      let csvContent = "时间,车流量(辆),摄像头ID,统计间隔\n";
      this.currentFullData.forEach(item => {
        const time = item.time || "";
        const count = item.totalCount || 0;
        const cameraId = this.queryParams.cameraId;
        const interval = this.queryParams.interval === "5min" ? "5分钟" :
            this.queryParams.interval === "30min" ? "半小时" : "1小时";
        csvContent += `${time},${count},${cameraId},${interval}\n`;
      });
      const blob = new Blob([new Uint8Array([0xEF, 0xBB, 0xBF]), csvContent], {
        type: "text/csv;charset=utf-8;"
      });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `车流量统计_${this.queryParams.cameraId}_${new Date().getTime()}.csv`;
      a.click();
      URL.revokeObjectURL(url);
      this.$message.success("导出成功！文件已下载");
    },

  }
};
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>