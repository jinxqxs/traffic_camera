<template>
  <div class="login-wrapper">
    <!-- 浮动动画圆形 -->
    <div class="bg-circle circle-1"></div>
    <div class="bg-circle circle-2"></div>
    <div class="bg-circle circle-3"></div>

    <div class="login-card">
      <div class="login-header">
        <el-icon :size="36" color="#00d4ff"><VideoCamera /></el-icon>
        <h2>交通监控管理系统</h2>
        <p class="subtitle">Traffic Monitor Control Center</p>
      </div>

      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="请输入账号"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%; height: 44px" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const form = ref({
  username: 'admin',
  password: '123456'
})

const handleLogin = async () => {
  try {
    const res = await request.post('/login', form.value)
    if (res.code === 200) {
      localStorage.setItem('accessToken', res.data.accessToken)
      localStorage.setItem('refreshToken', res.data.refreshToken)
      localStorage.setItem('username', res.data.username)
      localStorage.setItem('role', res.data.role)
      router.push('/traffic')
    } else {
      ElMessage.error(res.msg || '登录失败')
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('登录失败：' + err.message)
  }
}
</script>

<style scoped>
.login-wrapper {
  position: relative;
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #0a0e27 0%, #111633 50%, #0a0e27 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

/* 浮动动画圆形 */
.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.06;
  filter: blur(40px);
}
.circle-1 {
  width: 500px; height: 500px;
  background: var(--accent-cyan);
  top: -10%; left: -5%;
  animation: floatCircle1 8s ease-in-out infinite;
}
.circle-2 {
  width: 400px; height: 400px;
  background: var(--accent-blue);
  bottom: -8%; right: -3%;
  animation: floatCircle2 10s ease-in-out infinite;
}
.circle-3 {
  width: 300px; height: 300px;
  background: var(--accent-green);
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  animation: floatCircle3 12s ease-in-out infinite;
}
@keyframes floatCircle1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(30px, -30px) scale(1.1); }
}
@keyframes floatCircle2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-20px, 25px) scale(1.08); }
}
@keyframes floatCircle3 {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  50% { transform: translate(-50%, -50%) scale(1.15); }
}

/* 毛玻璃卡片 */
.login-card {
  position: relative;
  z-index: 10;
  width: 420px;
  padding: 40px;
  background: rgba(22, 27, 58, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(42, 48, 96, 0.5);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}
.login-header h2 {
  margin-top: 12px;
  font-size: 22px;
  color: var(--text-primary);
  letter-spacing: 2px;
}
.subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 1px;
}
</style>
