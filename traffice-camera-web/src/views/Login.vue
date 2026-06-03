<template>
  <div class="login-container">
    <el-card class="login-box">
      <h2>登录</h2>
      <el-form @submit.prevent="handleLogin">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
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
      // 存储双 Token + 角色
      localStorage.setItem('accessToken', res.data.accessToken)
      localStorage.setItem('refreshToken', res.data.refreshToken)
      localStorage.setItem('username', res.data.username)
      localStorage.setItem('role', res.data.role)

      router.push('/traffic')
    } else {
      alert(res.msg || '登录失败')
    }
  } catch (err) {
    console.error(err)
    alert('登录失败：' + err.message)
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}
.login-box {
  width: 400px;
  padding: 20px;
}
</style>