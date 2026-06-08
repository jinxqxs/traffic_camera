<template>
  <div class="layout-container">
    <Sidebar v-if="!isLoginPage" v-show="!sidebarCollapsed" />
    <div class="main-area">
      <header v-if="!isLoginPage" class="top-bar">
        <div class="top-bar-left">
          <el-button
            class="sidebar-toggle"
            text
            @click="sidebarCollapsed = !sidebarCollapsed"
          >
            <el-icon :size="20">
              <Fold v-if="!sidebarCollapsed" />
              <Expand v-else />
            </el-icon>
          </el-button>
          <el-icon :size="20"><Monitor /></el-icon>
          <span class="app-title">交通监控管理系统</span>
        </div>
        <div class="top-bar-right">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ username }}（{{ role === 'admin' ? '管理员' : '普通用户' }}）
          </span>
          <el-button type="danger" size="small" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </header>
      <TopAlertBar v-if="!isLoginPage" />
      <div class="page-body">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import Sidebar from '@/components/sidebar/index.vue'
import TopAlertBar from '@/components/TopAlertBar.vue'
import { useTrafficAlert } from '@/composables/useTrafficAlert'

// 全局流量异常预警（登录后自动启用）
useTrafficAlert()

const route = useRoute()
const sidebarCollapsed = ref(false)
const isLoginPage = computed(() => route.path === '/login')

const router = useRouter()
const username = ref(localStorage.getItem('username') || '')
const role = ref(localStorage.getItem('role') || '')

watch(() => route.path, () => {
  username.value = localStorage.getItem('username') || ''
  role.value = localStorage.getItem('role') || ''
})

const handleLogout = async () => {
  const accessToken = localStorage.getItem('accessToken')
  try {
    await axios.post('/api/logout', { accessToken })
  } catch (e) { /* ignore */ }
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('username')
  localStorage.removeItem('role')
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
}
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.top-bar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
}
.top-bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--accent-cyan);
}
.sidebar-toggle {
  color: var(--text-secondary);
  margin-right: 4px;
}
.sidebar-toggle:hover {
  color: var(--accent-cyan);
}
.app-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 1px;
}
.top-bar-right {
  display: flex;
  align-items: center;
  gap: 15px;
}
.user-info {
  font-size: 14px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.page-body {
  flex: 1;
  overflow: auto;
  background: var(--bg-primary);
}
</style>
