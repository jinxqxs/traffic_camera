<template>
  <div class="video-wall-container">
    <div class="video-grid">
      <div v-for="(item, index) in pagedCameras" :key="index" class="video-item" :class="{ 'alert-flash': alertState.alerts.has(item.cameraId) }" @click="openEnlarged(item)">
        <div class="video-player">
          <img
            :src="snapshotSrc(item)"
            class="mjpeg-img"
            @error="onImgError(item, $event)"
          />
        </div>
        <div class="video-footer">
          <span class="cam-label">{{ item.cameraName || '未知设备' }}</span>
          <el-button
            v-if="item.streamMode"
            class="mode-toggle-btn"
            :type="item.streamMode === 'yolo' ? 'warning' : 'success'"
            size="small"
            round
            @click.stop="toggleMode(item)"
          >
            {{ item.streamMode === 'yolo' ? 'YOLO' : '原画' }}
          </el-button>
        </div>
      </div>

      <div v-for="n in (pageSize - pagedCameras.length)" :key="'empty-' + n" class="video-item empty">
        <el-icon :size="32" color="#5a6280"><VideoCamera /></el-icon>
        <span>无信号</span>
      </div>
    </div>

    <div class="pagination-bar" v-if="totalPages > 1">
      <el-button size="small" :disabled="currentPage <= 1" @click="currentPage--">
        <el-icon><ArrowLeft /></el-icon>上一页
      </el-button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <el-button size="small" :disabled="currentPage >= totalPages" @click="currentPage++">
        下一页<el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>

    <!-- 放大 overlay -->
    <div v-if="enlargedCam" class="enlarged-overlay" @click="closeEnlarged">
      <div class="enlarged-close" @click.stop="closeEnlarged">
        <el-icon :size="28"><Close /></el-icon>
      </div>
      <img
        ref="enlargedImgRef"
        :src="enlargedCam.mjpegUrl"
        class="enlarged-img"
        :style="enlargedZoomStyle"
        @click.stop="handleEnlargedClick"
      />
      <div class="enlarged-label">{{ enlargedCam.cameraName || '未知设备' }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getVideoList } from '@/api/traffic/camera'
import { alertState } from '@/composables/alertState'

const allCameras = ref([])
const currentPage = ref(1)
const pageSize = 16
const enlargedCam = ref(null)
const enlargedZoomed = ref(false)
const enlargedImgRef = ref(null)
const refreshTick = ref(0)
let refreshTimerId = null

const totalPages = computed(() => Math.ceil(allCameras.value.length / pageSize))

const pagedCameras = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return allCameras.value.slice(start, start + pageSize)
})

const fetchPythonStreams = async () => {
  try {
    const res = await fetch('/ai/streams')
    return await res.json()
  } catch (e) {
    console.error('获取 Python 流信息失败：', e)
    return []
  }
}


const mergeStreamData = (cameras, streams) => {
  const streamMap = {}
  streams.forEach(s => {
    streamMap[s.camera_id] = {
      filename: s.filename,
      mode: s.mode,
      switchUrl: `/ai/switch_mode?name=${s.filename}`
    }
  })
  return cameras.map(cam => {
    const stream = streamMap[cam.cameraId]
    return {
      ...cam,
      // grid 用 snapshot 短连接（不占持久连接槽），overlay 用 MJPEG 实时流
      snapshotUrl: stream ? `/ai/snapshot/${stream.filename}` : null,
      mjpegUrl: stream ? `/ai/video_feed/${stream.filename}` : null,
      streamMode: stream ? stream.mode : null,
      streamName: stream ? stream.filename : null
    }
  })
}

const snapshotSrc = (item) => {
  return item.snapshotUrl ? `${item.snapshotUrl}?_t=${refreshTick.value}` : ''
}

const openEnlarged = (item) => {
  enlargedCam.value = item
  enlargedZoomed.value = false
}

const closeEnlarged = () => {
  // 先切断 MJPEG 流，防止浏览器 TCP 连接残留
  const img = enlargedImgRef.value
  if (img) img.src = ''
  enlargedCam.value = null
  enlargedZoomed.value = false
}

const enlargedZoomStyle = computed(() => {
  if (!enlargedZoomed.value) return { transition: 'transform 0.25s ease, transform-origin 0.25s ease' }
  return {
    transform: 'scale(2)',
    transformOrigin: `${enlargedZoomed.value.x}% ${enlargedZoomed.value.y}%`,
    transition: 'transform 0.25s ease, transform-origin 0.25s ease'
  }
})

const handleEnlargedClick = (event) => {
  if (enlargedZoomed.value) {
    enlargedZoomed.value = false
  } else {
    const rect = event.currentTarget.getBoundingClientRect()
    const x = ((event.clientX - rect.left) / rect.width * 100).toFixed(1)
    const y = ((event.clientY - rect.top) / rect.height * 100).toFixed(1)
    enlargedZoomed.value = { x, y }
  }
}

const toggleMode = async (item) => {
  const newMode = item.streamMode === 'yolo' ? 'raw' : 'yolo'
  try {
    await fetch(`/ai/switch_mode?name=${item.streamName}&mode=${newMode}`)
    item.streamMode = newMode
    ElMessage.success(`已切换至 ${newMode === 'yolo' ? 'YOLO识别' : '原画'} 模式`)
  } catch (e) {
    ElMessage.error('模式切换失败')
  }
}

const onImgError = (item, event) => {
  console.warn(`摄像头 ${item.cameraName || item.cameraId} 快照加载失败，1秒后重试`)
  if (event?.target) {
    setTimeout(() => { event.target.src = snapshotSrc(item) }, 1000)
  }
}

const handleKeydown = (e) => {
  if (e.key === 'Escape') {
    if (enlargedZoomed.value) {
      enlargedZoomed.value = false
    } else if (enlargedCam.value) {
      closeEnlarged()
    }
  }
}

const getList = async () => {
  try {
    const [camRes, streams] = await Promise.all([
      getVideoList({ status: '0' }),
      fetchPythonStreams()
    ])
    allCameras.value = mergeStreamData(camRes.data, streams)
  } catch (err) {
    console.error('获取视频列表失败：', err)
    ElMessage.error('获取视频列表失败')
  }
}

onMounted(() => {
  getList()
  window.addEventListener('keydown', handleKeydown)
  refreshTimerId = setInterval(() => { refreshTick.value++ }, 200)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  if (refreshTimerId) { clearInterval(refreshTimerId); refreshTimerId = null }
})
</script>

<style scoped>
.video-wall-container {
  padding: 10px;
  background-color: var(--bg-primary);
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 4x4 网格 可伸缩 */
.video-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  flex: 1;
  align-content: start;
}

.video-item {
  background: #000;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  min-width: 360px;
  aspect-ratio: 16 / 9;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}
.video-item:hover {
  border-color: var(--accent-cyan);
  box-shadow: 0 0 12px rgba(0, 212, 255, 0.15);
}
.video-item.alert-flash {
  border-color: #ff3d71;
  animation: alert-border-blink 0.8s ease-in-out infinite;
}
@keyframes alert-border-blink {
  0%, 100% { border-color: #ff3d71; box-shadow: 0 0 8px rgba(255, 61, 113, 0.3); }
  50% { border-color: rgba(255, 61, 113, 0.3); box-shadow: 0 0 18px rgba(255, 61, 113, 0.6); }
}

.video-footer {
  background: rgba(0, 0, 0, 0.85);
  color: #c0c7d0;
  font-size: 12px;
  padding: 4px 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.cam-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
.mode-toggle-btn {
  flex-shrink: 0;
  margin-left: 6px;
  font-size: 10px;
  padding: 2px 10px;
  height: 22px;
}

.video-player {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
}
.mjpeg-img {
  width: 100%;
  height: 100%;
  object-fit: fill;
}

.empty {
  color: var(--text-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 12px 0;
}
.page-info {
  font-size: 14px;
  color: var(--text-secondary);
}

.enlarged-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}
.enlarged-close {
  position: absolute;
  top: 20px;
  right: 20px;
  color: #fff;
  cursor: pointer;
  opacity: 0.7;
  z-index: 1;
}
.enlarged-close:hover {
  opacity: 1;
}
.enlarged-img {
  width: 80vw;
  height: 60vh;
  max-width: 90vw;
  max-height: 86vh;
  object-fit: contain;
  cursor: pointer;
  background: #111;
}
.enlarged-label {
  color: #c0c7d0;
  font-size: 14px;
  margin-top: 12px;
}
</style>
