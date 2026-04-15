<template>
  <div class="user-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAdd">新增用户</el-button>
        </div>
      </template>

      <el-form inline>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="请选择" clearable filterable @change="loadData" style="width: 120px;">
            <el-option label="教师" value="teacher" />
            <el-option label="考核员" value="evaluator" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="搜索用户名/姓名" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="role" label="角色">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'teacher'" type="success">教师</el-tag>
            <el-tag v-else-if="row.role === 'evaluator'" type="warning">考核员</el-tag>
            <el-tag v-else type="danger">管理员</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
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
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!editId" />
        </el-form-item>
        <el-form-item v-if="!editId" label="密码" prop="password">
          <el-input v-model="form.password" type="password" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role">
            <el-option label="教师" value="teacher" />
            <el-option label="考核员" value="evaluator" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="form.department" />
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
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, createUser, updateUser, deleteUser } from '@/api/user'

const query = reactive({
  page: 1,
  size: 10,
  role: '',
  keyword: ''
})

const tableData = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref()
const loading = ref(false)
const editId = ref<number | null>(null)

const form = reactive({
  username: '',
  password: '',
  realName: '',
  role: 'teacher',
  department: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  department: [{ required: true, message: '请输入部门', trigger: 'blur' }]
}

const loadData = async () => {
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
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, { username: '', password: '', realName: '', role: 'teacher', department: '' })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  editId.value = row.id
  Object.assign(form, {
    username: row.username,
    password: '',
    realName: row.realName,
    role: row.role,
    department: row.department
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    if (editId.value) {
      const res = await updateUser(editId.value, {
        realName: form.realName,
        role: form.role,
        department: form.department,
        status: 1
      })
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
    loading.value = false
  }
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确定要删除该用户吗？', '提示', { type: 'warning' })
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

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>