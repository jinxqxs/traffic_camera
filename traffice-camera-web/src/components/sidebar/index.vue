<template>
  <div class="sidebar-wrapper">
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
          :default-active="activeMenu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          mode="vertical"
          router
      >
        <!-- 仅 admin 可见 -->
        <template v-if="role === 'admin'">
          <el-menu-item index="/user-management">
            <i class="el-icon-user"></i>
            <span>账号管理</span>
          </el-menu-item>

          <el-menu-item index="/traffic">
            <i class="el-icon-video-camera"></i>
            <span>交通监控设备</span>
          </el-menu-item>
        </template>

        <!-- admin 和 user 都可见 -->
        <el-menu-item index="/video-wall">
          <i class="el-icon-monitor"></i>
          <span>视频大屏</span>
        </el-menu-item>

        <el-menu-item index="/flow-analysis">
          <i class="el-icon-data-line"></i>
          <span>流量查询</span>
        </el-menu-item>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script>
export default {
  data() {
    return {
      role: localStorage.getItem('role') || ''
    }
  },
  computed: {
    activeMenu() {
      return this.$route.path
    }
  },
  // 每次路由变化时刷新角色（以防切换登录不同账号）
  watch: {
    '$route'() {
      this.role = localStorage.getItem('role') || ''
    }
  }
}
</script>

<style scoped>
.sidebar-wrapper {
  width: 210px;
  height: 100vh;
  background-color: #304156;
}
.scrollbar-wrapper {
  height: 100vh;
}
</style>