<template>
  <div class="user-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div class="header-actions">
            <el-button @click="handleDownloadTemplate">下载模板</el-button>
            <el-button type="success" @click="showImportDialog = true">导入用户</el-button>
            <el-button type="warning" @click="handleExport">导出用户</el-button>
            <el-button type="primary" @click="handleAdd">新增用户</el-button>
          </div>
        </div>
      </template>

      <el-form inline>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="请选择" clearable filterable @change="loadData" style="width: 120px;">
            <el-option label="教师" value="teacher" />
            <el-option label="评分员" value="evaluator" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="搜索用户名、姓名或部门" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'teacher'" type="success">教师</el-tag>
            <el-tag v-else-if="row.role === 'evaluator'" type="warning">评分员</el-tag>
            <el-tag v-else type="danger">管理员</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" min-width="120" />
        <el-table-column prop="teacherLevel" label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelTagType(row.teacherLevel || 'NONE')">
              {{ row.teacherLevel || 'NONE' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.role === 'teacher'" type="warning" link @click="handleChangeLevel(row)">修改等级</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="88px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!editId" />
        </el-form-item>
        <el-form-item v-if="!editId" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <template v-if="editId">
          <el-form-item label="重置密码">
            <el-checkbox v-model="resetPassword">重置密码</el-checkbox>
          </el-form-item>
          <el-form-item v-if="resetPassword" label="新密码" prop="newPassword">
            <el-input v-model="form.newPassword" type="password" show-password placeholder="请输入新密码" />
          </el-form-item>
        </template>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role">
            <el-option label="教师" value="teacher" />
            <el-option label="评分员" value="evaluator" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="form.department" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showImportDialog" title="导入用户" width="560px" @closed="resetImport">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="ID 为空时新增用户；ID 命中现有用户时，仅更新表格中非空字段。密码留空表示不修改密码。"
      />
      <el-upload
        class="import-upload"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :file-list="importFileList"
        :on-change="handleImportFileChange"
        :on-remove="handleImportFileRemove"
      >
        <div class="upload-text">拖拽 Excel 到此处，或点击选择文件</div>
        <template #tip>
          <div class="el-upload__tip">请使用“下载模板”生成的字段格式，支持 .xlsx / .xls。</div>
        </template>
      </el-upload>
      <div v-if="importResult" class="import-result">
        <el-tag type="success">新增 {{ importResult.createdCount }}</el-tag>
        <el-tag type="warning">更新 {{ importResult.updatedCount }}</el-tag>
        <el-tag type="info">跳过 {{ importResult.skippedCount }}</el-tag>
        <ul v-if="importResult.errors?.length">
          <li v-for="error in importResult.errors" :key="error">{{ error }}</li>
        </ul>
      </div>
      <template #footer>
        <el-button @click="showImportDialog = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">开始导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="levelDialogVisible" title="修改教师等级" width="400px">
      <el-form label-width="88px">
        <el-form-item label="当前等级">
          <el-tag :type="getLevelTagType(levelForm.currentLevel || 'NONE')">
            {{ levelForm.currentLevel || 'NONE' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新等级">
          <el-select v-model="levelForm.newLevel" placeholder="请选择等级">
            <el-option label="无等级" value="NONE" />
            <el-option label="C级" value="C" />
            <el-option label="B级" value="B" />
            <el-option label="A级" value="A" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="levelLoading" @click="handleLevelSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUserList,
  createUser,
  updateUser,
  deleteUser,
  updateTeacherLevel,
  downloadUserTemplate,
  exportUsers,
  importUsers,
  type UserImportResult
} from '@/api/user'

const query = reactive({
  page: 1,
  size: 10,
  role: '',
  keyword: ''
})

const tableData = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const submitting = ref(false)
const editId = ref<number | null>(null)
const resetPassword = ref(false)

const form = reactive({
  username: '',
  password: '',
  newPassword: '',
  realName: '',
  role: 'teacher',
  department: '',
  status: 1
})

const showImportDialog = ref(false)
const importing = ref(false)
const importFile = ref<File | null>(null)
const importFileList = ref<any[]>([])
const importResult = ref<UserImportResult | null>(null)

const levelDialogVisible = ref(false)
const levelLoading = ref(false)
const levelForm = reactive({
  teacherId: null as number | null,
  currentLevel: '',
  newLevel: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度需为 3-50 位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_.-]+$/, message: '用户名仅支持字母、数字、下划线、短横线和点', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需为 6-64 位', trigger: 'blur' }
  ],
  newPassword: [
    { min: 6, max: 64, message: '密码长度需为 6-64 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  department: [{ required: true, message: '请输入部门', trigger: 'blur' }]
}

const saveBlob = (blob: Blob, filename: string) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserList({
      page: query.page,
      size: query.size,
      role: query.role || undefined,
      keyword: query.keyword || undefined
    })
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const handleDownloadTemplate = async () => {
  const blob = await downloadUserTemplate()
  saveBlob(blob, '用户导入模板.xlsx')
}

const handleExport = async () => {
  const blob = await exportUsers({
    role: query.role || undefined,
    keyword: query.keyword || undefined
  })
  saveBlob(blob, '用户数据.xlsx')
}

const handleAdd = () => {
  editId.value = null
  resetPassword.value = false
  Object.assign(form, {
    username: '',
    password: '',
    newPassword: '',
    realName: '',
    role: 'teacher',
    department: '',
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  editId.value = row.id
  resetPassword.value = false
  Object.assign(form, {
    username: row.username,
    password: '',
    newPassword: '',
    realName: row.realName,
    role: row.role,
    department: row.department,
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (editId.value) {
      const payload: any = {
        realName: form.realName,
        role: form.role,
        department: form.department,
        status: form.status
      }
      if (resetPassword.value && form.newPassword) {
        payload.password = form.newPassword
      }
      const res = await updateUser(editId.value, payload)
      if (res.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        loadData()
      }
    } else {
      const res = await createUser(form)
      if (res.code === 200) {
        ElMessage.success('创建成功')
        dialogVisible.value = false
        loadData()
      }
    }
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(`确定要删除用户“${row.realName || row.username}”吗？`, '提示', { type: 'warning' })
  loading.value = true
  try {
    const res = await deleteUser(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } finally {
    loading.value = false
  }
}

const handleImportFileChange = (file: any) => {
  const name = file.name || file.raw?.name || ''
  if (!name.endsWith('.xlsx') && !name.endsWith('.xls')) {
    ElMessage.warning('请选择 Excel 文件')
    importFile.value = null
    importFileList.value = []
    return
  }
  importFile.value = file.raw
  importResult.value = null
}

const handleImportFileRemove = () => {
  importFile.value = null
}

const handleImport = async () => {
  if (!importFile.value) {
    ElMessage.warning('请选择要导入的 Excel 文件')
    return
  }
  importing.value = true
  try {
    const res = await importUsers(importFile.value)
    if (res.code === 200) {
      importResult.value = res.data
      ElMessage.success(`导入完成：新增 ${res.data.createdCount}，更新 ${res.data.updatedCount}`)
      await loadData()
    }
  } finally {
    importing.value = false
  }
}

const resetImport = () => {
  importFile.value = null
  importFileList.value = []
  importResult.value = null
}

const getLevelTagType = (level: string) => {
  const typeMap: Record<string, any> = {
    NONE: 'info',
    C: 'success',
    B: 'warning',
    A: 'danger'
  }
  return typeMap[level] || 'info'
}

const handleChangeLevel = (row: any) => {
  levelForm.teacherId = row.id
  levelForm.currentLevel = row.teacherLevel || 'NONE'
  levelForm.newLevel = row.teacherLevel || 'NONE'
  levelDialogVisible.value = true
}

const handleLevelSubmit = async () => {
  if (!levelForm.newLevel) {
    ElMessage.warning('请选择新等级')
    return
  }
  levelLoading.value = true
  try {
    const res = await updateTeacherLevel(levelForm.teacherId!, levelForm.newLevel)
    if (res.code === 200) {
      ElMessage.success('修改成功')
      levelDialogVisible.value = false
      loadData()
    }
  } finally {
    levelLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.import-upload {
  margin-top: 16px;
}

.upload-text {
  color: #606266;
  line-height: 80px;
}

.import-result {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.import-result ul {
  flex-basis: 100%;
  max-height: 160px;
  overflow: auto;
  margin: 8px 0 0;
  padding-left: 20px;
  color: #b45309;
  line-height: 1.7;
}
</style>
