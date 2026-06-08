<template>
  <div class="sidebar-wrapper">
    <!-- Logo 区域 -->
    <div class="sidebar-logo">
      <el-icon :size="24" color="#00d4ff"><VideoCamera /></el-icon>
      <span class="logo-text">监控中心</span>
    </div>

    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        background-color="#111633"
        text-color="#8892b0"
        active-text-color="#00d4ff"
        mode="vertical"
        router
      >
        <template v-if="role === 'admin'">
          <el-menu-item index="/user-management">
            <el-icon><UserFilled /></el-icon>
            <span>账号管理</span>
          </el-menu-item>
          <el-menu-item index="/traffic">
            <el-icon><VideoCamera /></el-icon>
            <span>交通监控设备</span>
          </el-menu-item>
        </template>

        <el-menu-item index="/video-wall">
          <el-icon><Monitor /></el-icon>
          <span>视频大屏</span>
        </el-menu-item>

        <el-menu-item index="/flow-analysis">
          <el-icon><TrendCharts /></el-icon>
          <span>流量查询</span>
        </el-menu-item>
      </el-menu>
    </el-scrollbar>

    <!-- 底部状态 -->
    <div class="sidebar-footer">
      <span class="status-dot"></span>
      <span class="status-text">系统运行中</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const role = ref(localStorage.getItem('role') || '')

const activeMenu = computed(() => route.path)

watch(() => route.path, () => {
  role.value = localStorage.getItem('role') || ''
})
</script>

<style scoped>
.sidebar-wrapper {
  width: 210px;
  height: 100vh;
  background-color: #111633;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-color);
}

.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-color);
}
.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 1px;
}

.scrollbar-wrapper {
  flex: 1;
  overflow: auto;
}

.el-menu {
  border-right: none !important;
}

.el-menu-item {
  font-size: 14px;
}
.el-menu-item:hover {
  background-color: var(--bg-elevated) !important;
}
.el-menu-item.is-active {
  background-color: rgba(0, 212, 255, 0.08) !important;
}

.sidebar-footer {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-top: 1px solid var(--border-color);
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent-green);
  box-shadow: 0 0 6px var(--accent-green);
  animation: statusPulse 2s ease-in-out infinite;
}
@keyframes statusPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
.status-text {
  font-size: 12px;
  color: var(--text-muted);
}
</style>
