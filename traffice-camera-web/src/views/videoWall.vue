<template>
  <div class="video-wall-container">
    <el-row :gutter="10" class="control-bar">
      <el-button type="primary" size="mini" @click="getList">刷新监控</el-button>
      <span class="title">交通监控 4×5 实时大屏</span>
    </el-row>

    <!-- 4x5 网格布局 -->
    <div class="video-grid">
      <div v-for="(item, index) in cameraList" :key="index" class="video-item">
        <div class="video-header">{{ item.cameraName || '未知设备' }}</div>
        <div class="video-player">
          <!-- 实际项目中建议使用 video.js 或 EasyPlayer -->
          <video
            :id="'video-' + index"
            controls
            autoplay
            muted
            class="vjs-tech"
          >
            <!-- 假设后端返回的 videoUrl 是播放地址 -->
            <source :src="item.videoUrl" type="application/x-mpegURL">
            您的浏览器不支持视频播放
          </video>
        </div>
      </div>

      <!-- 如果设备不足20个，用空盒子补齐 -->
      <div v-for="n in (20 - cameraList.length)" :key="'empty-' + n" class="video-item empty">
        无信号
      </div>
    </div>
  </div>
</template>

<script>
import { getVideoList } from "@/api/traffic/camera";

export default {
  name: "VideoWall",
  data() {
    return {
      cameraList: [],
      queryParams: {
        status: '0' // 假设0是正常状态
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      getVideoList(this.queryParams).then(response => {
        this.cameraList = response.data;
      });
    }
  }
};
</script>

<style scoped>
.video-wall-container {
  padding: 10px;
  background-color: #001529; /* 深色背景符合监控室风格 */
  min-height: calc(100vh - 84px);
}

.control-bar {
  margin-bottom: 10px;
  color: white;
}

.title {
  margin-left: 20px;
  font-weight: bold;
}

/* 核心：4行5列的网格布局 */
.video-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr); /* 5列 */
  gap: 10px;                             /* 格子间距 */
  padding-bottom: 20px; /* 底部留点间距 */
  min-height: 80vh;
}

.video-item {
  background: #000;
  border: 1px solid #333;
  display: flex;
  flex-direction: column;
  aspect-ratio: 16 / 9;
  position: relative;
}

.video-header {
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 12px;
  padding: 2px 5px;
  position: absolute;
  top: 0;
  width: 100%;
  z-index: 10;
}

.video-player {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

video {
  width: 100%;
  height: 100%;
  object-fit: fill; /* 强制填满格子 */
}

.empty {
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}
</style>
