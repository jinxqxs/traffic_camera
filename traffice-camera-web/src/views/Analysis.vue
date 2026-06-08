<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <el-icon :size="20" color="#00d4ff"><TrendCharts /></el-icon>
      <h2>流量统计分析</h2>
    </div>

    <el-card shadow="never">
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
          <el-button type="success" @click="handleExport" :disabled="!exportDataReady">导出Excel</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top:16px; height:550px;">
      <div ref="chartRef" style="width:100%;height:500px;"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getVideoList } from '@/api/traffic/camera'

const cameraList = ref([])
const queryParams = ref({
  cameraId: '',
  dateRange: [],
  interval: '5min'
})
const defaultTime = [
  new Date(2000, 1, 1, 0, 0, 0),
  new Date(2000, 1, 1, 23, 59, 59)
]
const chartRef = ref(null)
let myChart = null
const currentFullData = ref([])
const exportDataReady = ref(false)

const formatDate = (date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}:${s}`
}

const setDefaultTime = () => {
  const end = new Date()
  const start = new Date()
  start.setMinutes(start.getMinutes() - 30)
  queryParams.value.dateRange = [formatDate(start), formatDate(end)]
}

const loadCameraList = async () => {
  try {
    const res = await getVideoList({ status: '0' })
    cameraList.value = res.data
  } catch (e) {
    console.error('加载摄像头列表失败：', e)
  }
}

const fillMissingData = (backendData, beginTime, endTime, interval) => {
  const result = []
  const start = new Date(beginTime)
  const end = new Date(endTime)
  let stepMs
  switch (interval) {
    case '5min': stepMs = 5 * 60 * 1000; break
    case '30min': stepMs = 30 * 60 * 1000; break
    case '1hour': stepMs = 60 * 60 * 1000; break
    default: stepMs = 5 * 60 * 1000
  }

  // 构建按时间排序的数据副本，用于区间匹配
  const sortedData = [...backendData].filter(d => d.time).sort((a, b) => (a.time || '').localeCompare(b.time || ''))
  console.log('[fillMissingData] sortedData:', sortedData)
  console.log('[fillMissingData] 第一条 time 类型:', typeof sortedData[0]?.time, '值:', sortedData[0]?.time)

  let current = new Date(start)
  while (current < end) {
    const bucketStart = current.getTime()
    const bucketEnd = bucketStart + stepMs

    // 将落在 [bucketStart, bucketEnd) 区间内的数据点求和
    let sum = 0
    for (const item of sortedData) {
      const itemTime = new Date(item.time).getTime()
      if (itemTime >= bucketStart && itemTime < bucketEnd) {
        sum += item.totalCount || 0
      }
    }

    const timeStr = formatDate(current)
    const bucketLabel = timeStr.substring(0, 16)
    if (sum > 0) {
      console.log(`[fillMissingData] 桶 ${bucketLabel}: sum=${sum}, bucketStart=${new Date(bucketStart).toISOString()}, bucketEnd=${new Date(bucketEnd).toISOString()}`)
    }
    result.push({
      time: timeStr.substring(0, 16),
      totalCount: sum
    })
    current = new Date(bucketEnd)
  }
  return result
}

const setEmptyOption = () => {
  myChart.setOption({
    backgroundColor: 'transparent',
    title: {
      text: '车流量统计趋势',
      left: 'center',
      textStyle: { color: '#e8eaf0', fontSize: 16 }
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(28,34,72,0.95)',
      borderColor: '#2a3060',
      textStyle: { color: '#e8eaf0' }
    },
    xAxis: {
      type: 'category',
      name: '时间',
      data: [],
      axisLabel: { interval: 0, rotate: 45, color: '#8892b0' },
      axisLine: { lineStyle: { color: '#2a3060' } },
      axisTick: { lineStyle: { color: '#2a3060' } },
      splitLine: { show: false },
      nameTextStyle: { color: '#8892b0' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#8892b0' },
      axisLine: { lineStyle: { color: '#2a3060' } },
      splitLine: { lineStyle: { color: '#2a3060', type: 'dashed' } },
      nameTextStyle: { color: '#8892b0' }
    },
    series: [{
      type: 'line',
      name: '车辆数(辆)',
      smooth: true,
      data: [],
      lineStyle: {
        width: 2,
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#00d4ff' },
          { offset: 1, color: '#409eff' }
        ])
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0,212,255,0.25)' },
          { offset: 1, color: 'rgba(64,158,255,0.02)' }
        ])
      },
      itemStyle: { color: '#00d4ff' },
      symbol: 'circle',
      symbolSize: 4
    }]
  })
}

const handleQuery = async () => {
  if (!queryParams.value.cameraId) {
    ElMessage.warning('请选择摄像头')
    return
  }
  if (!queryParams.value.dateRange || queryParams.value.dateRange.length !== 2) {
    ElMessage.warning('请选择统计时间')
    return
  }

  const beginTime = queryParams.value.dateRange[0]
  const endTime = queryParams.value.dateRange[1]

  try {
    const res = await request.get('traffic/analysis/trends', {
      params: {
        cameraId: queryParams.value.cameraId,
        beginTime: beginTime,
        endTime: endTime,
        intervalType: queryParams.value.interval
      }
    })

    let realData = res.data
    if (res.data.data) {
      realData = res.data.data
    } else if (Array.isArray(res.data)) {
      realData = res.data
    } else {
      throw new Error('后端返回数据格式不正确，无法解析')
    }

    console.log('[Analysis] 原始返回 res:', res)
    console.log('[Analysis] 解析后 realData:', realData)
    console.log('[Analysis] beginTime:', beginTime, 'endTime:', endTime)

    if (realData.length === 0) {
      ElMessage.info('该时间段内暂无数据')
      myChart.clear()
      setEmptyOption()
      currentFullData.value = []
      exportDataReady.value = false
      return
    }

    const fullData = fillMissingData(realData, beginTime, endTime, queryParams.value.interval)
    console.log('[Analysis] fillMissingData 结果:', fullData)
    currentFullData.value = fullData
    exportDataReady.value = true

    myChart.setOption({
      backgroundColor: 'transparent',
      title: {
        text: '车流量' + (queryParams.value.interval === '5min' ? '5分钟' : queryParams.value.interval === '30min' ? '半小时' : '1小时') + '统计趋势',
        left: 'center',
        textStyle: { color: '#e8eaf0', fontSize: 16 }
      },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(28,34,72,0.95)',
        borderColor: '#2a3060',
        textStyle: { color: '#e8eaf0' }
      },
      xAxis: {
        type: 'category',
        name: '时间',
        data: fullData.map(i => i.time),
        axisLabel: {
          interval: 0,
          rotate: 45,
          color: '#8892b0',
          formatter: (value) => value && value.length > 10 ? value.substring(10, 16) : value
        },
        axisLine: { lineStyle: { color: '#2a3060' } },
        axisTick: { lineStyle: { color: '#2a3060' } },
        splitLine: { show: false },
        nameTextStyle: { color: '#8892b0' }
      },
      yAxis: {
        type: 'value',
        min: 0,
        scale: true,
        axisLabel: { color: '#8892b0' },
        axisLine: { lineStyle: { color: '#2a3060' } },
        splitLine: { lineStyle: { color: '#2a3060', type: 'dashed' } },
        nameTextStyle: { color: '#8892b0' }
      },
      series: [{
        type: 'line',
        name: '车辆数（辆）',
        smooth: true,
        data: fullData.map(i => i.totalCount),
        lineStyle: {
          width: 2,
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#00d4ff' },
            { offset: 1, color: '#409eff' }
          ])
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,212,255,0.25)' },
            { offset: 1, color: 'rgba(64,158,255,0.02)' }
          ])
        },
        itemStyle: { color: '#00d4ff' },
        symbol: 'circle',
        symbolSize: 4
      }]
    }, true)

    ElMessage.success('查询成功')
  } catch (err) {
    console.error('请求失败，详情：', err)
    ElMessage.error('查询失败：' + (err.response?.data?.message || err.message))
    exportDataReady.value = false
  }
}

const resetQuery = () => {
  queryParams.value.cameraId = ''
  setDefaultTime()
  myChart.clear()
  setEmptyOption()
  currentFullData.value = []
  exportDataReady.value = false
}

const handleExport = () => {
  if (!exportDataReady.value || currentFullData.value.length === 0) {
    ElMessage.warning('请先查询数据再导出')
    return
  }
  let csvContent = '时间,车流量(辆),摄像头ID,统计间隔\n'
  currentFullData.value.forEach(item => {
    const time = item.time || ''
    const count = item.totalCount || 0
    const cameraId = queryParams.value.cameraId
    const interval = queryParams.value.interval === '5min' ? '5分钟' :
      queryParams.value.interval === '30min' ? '半小时' : '1小时'
    csvContent += `${time},${count},${cameraId},${interval}\n`
  })
  const blob = new Blob([new Uint8Array([0xEF, 0xBB, 0xBF]), csvContent], {
    type: 'text/csv;charset=utf-8;'
  })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `车流量统计_${queryParams.value.cameraId}_${new Date().getTime()}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功！文件已下载')
}

onMounted(async () => {
  setDefaultTime()
  await loadCameraList()
  await nextTick()
  myChart = echarts.init(chartRef.value, null, { backgroundColor: 'transparent' })
  setEmptyOption()
})

onBeforeUnmount(() => {
  if (myChart) {
    myChart.dispose()
    myChart = null
  }
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}
.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}
</style>
