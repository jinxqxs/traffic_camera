<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <el-icon :size="20" color="#00d4ff"><VideoCamera /></el-icon>
      <h2>交通监控设备</h2>
    </div>

    <!-- 搜索栏 -->
    <div class="search-box">
      <el-form :model="queryParams" inline>
        <el-form-item label="设备名称">
          <el-input v-model="queryParams.cameraName" />
        </el-form-item>
        <el-form-item label="设备编号">
          <el-input v-model="queryParams.cameraCode" />
        </el-form-item>
        <el-form-item label="安装地点">
          <el-input v-model="queryParams.location" />
        </el-form-item>
        <el-form-item label="流量阈值">
          <el-input v-model.number="queryParams.threshold" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getList">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作按钮 -->
    <div class="toolbar" style="margin: 10px 0;">
      <el-button type="primary" @click="handleAdd" v-if="isAdmin">
        <el-icon><Plus /></el-icon>新增
      </el-button>
      <el-button type="warning" @click="handleExport">
        <el-icon><Download /></el-icon>导出
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table
      ref="tableRef"
      :data="tableData"
      border
    >
      <el-table-column label="ID" prop="cameraId" width="80" />
      <el-table-column label="设备名称" prop="cameraName" />
      <el-table-column label="设备编号" prop="cameraCode" />
      <el-table-column label="安装地点" prop="location" />
      <el-table-column label="流量阈值" prop="threshold" />
      <el-table-column label="状态" prop="status">
        <template #default="scope">
          <el-tag :type="scope.row.status == 0 ? 'success' : 'danger'" effect="dark" size="small">
            {{ scope.row.status == 0 ? '正常' : '异常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button v-if="isAdmin" type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
          <el-button v-if="isAdmin" type="danger" link @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      :current-page="queryParams.pageNum"
      :page-size="queryParams.pageSize"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isAdd ? '新增设备' : '编辑设备'" width="600px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="设备名称" prop="cameraName">
          <el-input v-model="form.cameraName" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="设备编号" prop="cameraCode">
          <el-input v-model="form.cameraCode" placeholder="请输入设备编号" />
        </el-form-item>
        <el-form-item label="安装地点">
          <el-input v-model="form.location" placeholder="请输入安装地点" />
        </el-form-item>
        <el-form-item label="流量阈值">
          <el-input v-model.number="form.threshold" placeholder="请输入流量阈值" />
        </el-form-item>
        <el-form-item label="视频流地址">
          <el-input v-model="form.videoUrl" placeholder="请输入视频流地址" />
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

const isAdmin = localStorage.getItem('role') === 'admin'

const tableRef = ref(null)
const tableData = ref([])
const total = ref(0)
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  cameraName: '',
  cameraCode: '',
  location: '',
  direction: '',
  threshold: null
})
const multiple = ref(false)
const selectedIds = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const form = ref({})
const isAdd = ref(false)

const formRules = {
  cameraName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  cameraCode: [{ required: true, message: '请输入设备编号', trigger: 'blur' }]
}

const getList = async () => {
  const res = await request.get('/traffic/camera/list', { params: queryParams.value })
  tableData.value = res.data.rows
  total.value = res.data.total
}

const resetQuery = () => {
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    cameraName: '',
    cameraCode: '',
    location: '',
    direction: '',
    threshold: null
  }
  getList()
}

const handleSizeChange = (val) => {
  queryParams.value.pageSize = val
  getList()
}
const handleCurrentChange = (val) => {
  queryParams.value.pageNum = val
  getList()
}

const handleSelectionChange = (val) => {
  multiple.value = val.length > 0
  selectedIds.value = val.map(item => item.cameraId)
}

const handleAdd = () => {
  isAdd.value = true
  form.value = {}
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

const handleEdit = (row) => {
  if (!row && selectedIds.value.length !== 1) {
    ElMessage.warning('请选择一条数据进行修改')
    return
  }
  isAdd.value = false
  form.value = row ? { ...row } : { ...tableData.value.find(item => item.cameraId === selectedIds.value[0]) }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

const submitForm = async () => {
  await formRef.value.validate()
  if (isAdd.value) {
    await request.post('/traffic/camera', form.value)
    ElMessage.success('新增成功')
  } else {
    await request.put('/traffic/camera', form.value)
    ElMessage.success('修改成功')
  }
  dialogVisible.value = false
  getList()
}

const handleDelete = async (row) => {
  const ids = row ? [row.cameraId] : selectedIds.value
  await ElMessageBox.confirm('确定删除选中数据？', '提示', { type: 'warning' })
  await request.delete(`/traffic/camera/${ids.join(',')}`)
  ElMessage.success('删除成功')
  getList()
}

const handleExport = async () => {
  try {
    const res = await request({
      url: '/traffic/camera/export',
      method: 'post',
      params: queryParams.value,
      responseType: 'blob'
    })
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '交通监控设备.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  getList()
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

.search-box {
  background: var(--bg-card);
  padding: 20px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.toolbar {
  margin: 10px 0;
}
</style>
