<template>
  <div class="app-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增账号</el-button>
    </div>

    <el-table :data="tableData" border>
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="用户名" prop="username" />
      <el-table-column label="角色">
        <template #default="scope">
          <el-tag :type="scope.row.role === 'admin' ? 'danger' : 'success'">
            {{ scope.row.role === 'admin' ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
          <el-button
              type="danger"
              link
              @click="handleDelete(scope.row)"
              :disabled="scope.row.role === 'admin'"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isAdd ? '新增账号' : '编辑账号'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="!isAdd" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" :placeholder="isAdd ? '请输入密码' : '留空则不修改'" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const dialogVisible = ref(false)
const isAdd = ref(false)
const form = ref({})

const getList = async () => {
  const res = await request.get('/user/list')
  tableData.value = res.data
}

const handleAdd = () => {
  isAdd.value = true
  form.value = { role: 'user' }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isAdd.value = false
  form.value = { ...row, password: '' }
  dialogVisible.value = true
}

const submitForm = async () => {
  if (isAdd.value) {
    if (!form.value.username || !form.value.password) {
      ElMessage.warning('用户名和密码不能为空')
      return
    }
    await request.post('/user', form.value)
    ElMessage.success('新增成功')
  } else {
    await request.put('/user', form.value)
    ElMessage.success('修改成功')
  }
  dialogVisible.value = false
  getList()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await request.delete(`/user/${row.id}`)
  ElMessage.success('删除成功')
  getList()
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.app-container { padding: 20px; }
.toolbar { margin-bottom: 10px; }
</style>
