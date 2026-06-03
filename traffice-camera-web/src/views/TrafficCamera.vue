<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <div class="search-box">
      <el-form :model="queryParams" inline>
        <el-form-item label="设备名称">
          <el-input v-model="queryParams.cameraName" placeholder="设备名称" />
        </el-form-item>
        <el-form-item label="设备编号">
          <el-input v-model="queryParams.cameraCode" placeholder="设备编号" />
        </el-form-item>
        <el-form-item label="安装地点">
          <el-input v-model="queryParams.location" placeholder="安装地点" />
        </el-form-item>
<!--        <el-form-item label="监控方向">-->
<!--          <el-input v-model="queryParams.direction" placeholder="监控方向" />-->
<!--        </el-form-item>-->
        <el-form-item label="流量阈值">
          <el-input v-model.number="queryParams.threshold" placeholder="流量阈值" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getList">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作按钮 -->
    <div class="toolbar" style="margin: 10px 0;">
      <el-button type="primary" icon="el-icon-plus" @click="handleAdd" v-if="isAdmin">新增</el-button>
      <el-button type="warning" icon="el-icon-download" @click="handleExport">导出</el-button>
    </div>

    <!-- 表格 -->
    <el-table
        ref="tableRef"
        :data="tableData"
        border
        @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column label="ID" prop="cameraId" width="80" />
      <el-table-column label="设备名称" prop="cameraName" />
      <el-table-column label="设备编号" prop="cameraCode" />
      <el-table-column label="安装地点" prop="location" />
<!--      <el-table-column label="监控方向" prop="direction" />-->
      <el-table-column label="流量阈值" prop="threshold" />
      <el-table-column label="状态" prop="status" />
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
    <el-dialog v-model="dialogVisible" title="编辑设备" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="设备名称">
          <el-input v-model="form.cameraName" />
        </el-form-item>
        <el-form-item label="设备编号">
          <el-input v-model="form.cameraCode" />
        </el-form-item>
        <el-form-item label="安装地点">
          <el-input v-model="form.location" />
        </el-form-item>
<!--        <el-form-item label="监控方向">-->
<!--          <el-input v-model="form.direction" />-->
<!--        </el-form-item>-->
        <el-form-item label="流量阈值">
          <el-input v-model.number="form.threshold" />
        </el-form-item>
        <el-form-item label="视频流地址">
          <el-input v-model="form.videoUrl" />
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
const form = ref({})
const isAdd = ref(false)

// 获取列表
const getList = async () => {
  const res = await request.get('/traffic/camera/list', { params: queryParams.value })
  console.log("接口返回的完整数据：", res) // 关键！看控制台打印结果
  tableData.value = res.data.rows
  total.value = res.data.total
}

// 重置
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

// 分页
const handleSizeChange = (val) => {
  queryParams.value.pageSize = val
  getList()
}
const handleCurrentChange = (val) => {
  queryParams.value.pageNum = val
  getList()
}

// 多选
const handleSelectionChange = (val) => {
  multiple.value = val.length > 0
  selectedIds.value = val.map(item => item.cameraId)
}

// 新增
const handleAdd = () => {
  isAdd.value = true
  form.value = {}
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  if (!row && selectedIds.value.length !== 1) {
    ElMessage.warning('请选择一条数据进行修改')
    return
  }
  isAdd.value = false
  form.value = row ? { ...row } : { ...tableData.value.find(item => item.cameraId === selectedIds.value[0]) }
  dialogVisible.value = true
}

// 提交表单
const submitForm = async () => {
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

// 删除
const handleDelete = async (row) => {
  const ids = row ? [row.cameraId] : selectedIds.value
  await ElMessageBox.confirm('确定删除选中数据？', '提示', { type: 'warning' })
  await request.delete(`/traffic/camera/${ids.join(',')}`)
  ElMessage.success('删除成功')
  getList()
}

// 导出
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