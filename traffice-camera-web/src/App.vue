<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <Sidebar />

    <!-- 右侧主体 -->
    <div class="main-area">
      <!-- 顶部栏 -->
      <header class="top-bar">
        <span class="user-info">
          {{ username }}（{{ role === 'admin' ? '管理员' : '普通用户' }}）
        </span>
        <el-button type="danger" size="small" @click="handleLogout">
          退出登录
        </el-button>
      </header>

      <!-- 页面内容 -->
      <div class="page-body">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import Sidebar from '@/components/sidebar/index.vue'

const router = useRouter()

const username = ref(localStorage.getItem('username') || '')
const role = ref(localStorage.getItem('role') || '')

const handleLogout = async () => {
  const accessToken = localStorage.getItem('accessToken')
  try {
    // 通知后端将当前 Access Token 拉黑
    await axios.post('/api/logout', { accessToken })
  } catch (e) {
    // 忽略登出接口错误
  }
  // 清空本地存储并跳转登录
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
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  gap: 15px;
}
.user-info {
  font-size: 14px;
  color: #666;
}
.page-body {
  flex: 1;
  overflow: auto;
}
</style>