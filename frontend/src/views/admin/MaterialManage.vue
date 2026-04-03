<template>
  <div class="material-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>学习资料管理</span>
          <el-button v-if="isAdmin" type="primary" @click="handleAdd">上传资料</el-button>
        </div>
      </template>

      <el-form inline>
        <el-form-item label="考核周期">
          <el-select v-model="query.periodId" placeholder="全部" clearable @change="loadData">
            <el-option v-for="p in periods" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="资料标题" show-overflow-tooltip />
        <el-table-column prop="fileName" label="文件名" show-overflow-tooltip />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="periodName" label="考核周期" width="150" />
        <el-table-column prop="creatorName" label="上传人" width="100" />
        <el-table-column prop="createdAt" label="上传时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDownload(row)">下载</el-button>
            <el-button v-if="isAdmin" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="isAdmin" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑资料' : '上传资料'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="考核周期" prop="periodId">
          <el-select v-model="form.periodId" placeholder="请选择考核周期">
            <el-option v-for="p in periods" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="资料标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入资料标题" />
        </el-form-item>
        <el-form-item label="资料描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入资料描述" />
        </el-form-item>
        <el-form-item v-if="!editId" label="选择文件" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getMaterialList, uploadMaterial, updateMaterial, deleteMaterial, downloadMaterial } from '@/api/learningMaterial'
import { getPeriodList } from '@/api/period'

const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}')
  } catch {
    return {}
  }
})

const isAdmin = computed(() => userInfo.value?.role === 'admin')

const tableData = ref<any[]>([])
const periods = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const uploadRef = ref()
const editId = ref<number | null>(null)
const fileList = ref<any[]>([])

const query = reactive({
  page: 1,
  size: 10,
  periodId: null as number | null
})

const form = reactive({
  periodId: null as number | null,
  title: '',
  description: '',
  file: null as File | null
})

const rules = {
  periodId: [{ required: true, message: '请选择考核周期', trigger: 'change' }],
  title: [{ required: true, message: '请输入资料标题', trigger: 'blur' }]
}

const formatSize = (bytes: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

const loadPeriods = async () => {
  const res = await getPeriodList()
  if (res.code === 200) {
    periods.value = res.data
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMaterialList(query)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, { periodId: null, title: '', description: '', file: null })
  fileList.value = []
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  editId.value = row.id
  Object.assign(form, {
    periodId: row.periodId,
    title: row.title,
    description: row.description,
    file: null
  })
  dialogVisible.value = true
}

const handleFileChange = (uploadFile: any, uploadFiles: any[]) => {
  if (uploadFile?.raw) {
    form.file = uploadFile.raw
    fileList.value = [uploadFile]
  } else if (uploadFiles?.length > 0 && uploadFiles[0].raw) {
    form.file = uploadFiles[0].raw
    fileList.value = uploadFiles
  }
}

const handleFileRemove = () => {
  form.file = null
  fileList.value = []
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (!editId.value && !form.file) {
    ElMessage.warning('请选择文件')
    return
  }

  loading.value = true
  try {
    if (editId.value) {
      const res = await updateMaterial(editId.value, {
        title: form.title,
        description: form.description
      })
      if (res.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        loadData()
      }
    } else {
      const formData = new FormData()
      formData.append('file', form.file!)
      formData.append('periodId', String(form.periodId))
      formData.append('title', form.title)
      formData.append('description', form.description)
      
      const res = await uploadMaterial(formData)
      if (res.code === 200) {
        ElMessage.success('上传成功')
        dialogVisible.value = false
        loadData()
      }
    }
  } finally {
    loading.value = false
  }
}

const handleDownload = async (row: any) => {
  try {
    const res = await downloadMaterial(row.id)
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.download = row.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确定要删除该资料吗？', '提示', { type: 'warning' })
  const res = await deleteMaterial(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    loadData()
  }
}

const total = ref(0)

onMounted(() => {
  loadPeriods()
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-icon--upload {
  font-size: 67px;
  color: #409EFF;
  margin-bottom: 16px;
}

.el-upload__text {
  color: #606266;
  font-size: 14px;
}
</style>