<template>
  <div class="period-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>考核周期管理</span>
          <el-button type="primary" @click="handleAdd">新增周期</el-button>
        </div>
      </template>

      <el-table :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="周期名称" />
        <el-table-column prop="startDate" label="开始日期" />
        <el-table-column prop="endDate" label="结束日期" />
        <el-table-column prop="description" label="说明" show-overflow-tooltip />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'">
              {{ row.status === 'active' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button 
              :type="row.status === 'active' ? 'warning' : 'success'" 
              link 
              @click="toggleStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
            <el-button type="info" link @click="showEnrollments(row)">报名({{ row.enrolledCount || 0 }})</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑周期' : '新增周期'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="周期名称" prop="name">
          <el-input v-model="form.name" placeholder="如：2024学年第一学期" />
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="enrollmentDialogVisible" :title="`已报名老师 - ${currentPeriod?.name}`" width="600px">
      <el-table :data="enrolledTeachers" v-loading="enrollmentLoading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="department" label="所属教研组" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" link @click="handleRemoveTeacher(row)">踢出</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="enrolledTeachers.length === 0 && !enrollmentLoading" description="暂无已报名老师" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPeriodList, createPeriod, updatePeriod, deletePeriod, getPeriodEnrollments, removeEnrollment } from '@/api/period'

const tableData = ref<any[]>([])
const dialogVisible = ref(false)
const formRef = ref()
const loading = ref(false)
const editId = ref<number | null>(null)

const enrollmentDialogVisible = ref(false)
const enrollmentLoading = ref(false)
const currentPeriod = ref<any>(null)
const enrolledTeachers = ref<any[]>([])

const form = reactive({
  name: '',
  startDate: '',
  endDate: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入周期名称', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }]
}

const loadData = async () => {
  const res = await getPeriodList()
  if (res.code === 200) {
    tableData.value = res.data
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, { name: '', startDate: '', endDate: '', description: '' })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  editId.value = row.id
  Object.assign(form, {
    name: row.name,
    startDate: row.startDate,
    endDate: row.endDate,
    description: row.description
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    let res
    if (editId.value) {
      res = await updatePeriod(editId.value, form)
    } else {
      res = await createPeriod(form)
    }
    if (res.code === 200) {
      ElMessage.success('操作成功')
      dialogVisible.value = false
      loadData()
    }
  } finally {
    loading.value = false
  }
}

const toggleStatus = async (row: any) => {
  const newStatus = row.status === 'active' ? 'inactive' : 'active'
  const res = await updatePeriod(row.id, { status: newStatus })
  if (res.code === 200) {
    ElMessage.success('状态已更新')
    loadData()
  }
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确定要删除该考核周期吗？', '提示', { type: 'warning' })
  const res = await deletePeriod(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    loadData()
  }
}

const showEnrollments = async (row: any) => {
  currentPeriod.value = row
  enrollmentDialogVisible.value = true
  enrollmentLoading.value = true
  
  try {
    const res = await getPeriodEnrollments(row.id)
    if (res.code === 200) {
      enrolledTeachers.value = res.data
    }
  } finally {
    enrollmentLoading.value = false
  }
}

const handleRemoveTeacher = async (teacher: any) => {
  await ElMessageBox.confirm('确定要踢出该老师吗？', '提示', { type: 'warning' })
  const res = await removeEnrollment(currentPeriod.value.id, teacher.id)
  if (res.code === 200) {
    ElMessage.success('已踢出')
    showEnrollments(currentPeriod.value)
    loadData()
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