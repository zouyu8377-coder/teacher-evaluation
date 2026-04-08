<template>
  <div class="activity-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>活动管理</span>
          <el-button type="primary" @click="handleAdd">新增活动</el-button>
        </div>
      </template>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="活动名称" />
        <el-table-column prop="level" label="级别">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)">{{ row.level }}级</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120">
          <template #default="{ row }">
            {{ row.startDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="endDate" label="结束日期" width="120">
          <template #default="{ row }">
            {{ row.endDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="maxParticipants" label="总名额">
          <template #default="{ row }">
            {{ row.maxParticipants || '不限制' }}
          </template>
        </el-table-column>
        <el-table-column prop="enrolledCount" label="已报名" width="80" />
        <el-table-column prop="remaining" label="余量" width="80">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.remaining === 0 }">
              {{ row.remaining === -1 ? '不限' : row.remaining }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="reviewerCount" label="评分人数" width="80" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : row.status === 'closed' ? 'danger' : 'info'">
              {{ row.status === 'active' ? '进行中' : row.status === 'closed' ? '已关闭' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报名时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.enrollmentStart) }} ~ {{ formatDateTime(row.enrollmentEnd) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="info" link @click="handleReviewerConfig(row)">评分人配置</el-button>
            <el-button :type="row.status === 'active' ? 'warning' : 'success'" link @click="toggleStatus(row)">
              {{ row.status === 'active' ? '关闭' : '启用' }}
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑活动' : '新增活动'" width="650px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" placeholder="如：2024学年第一学期C级考核" />
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-select v-model="form.level" placeholder="请选择">
            <el-option label="C级" value="C" />
            <el-option label="B2级" value="B2" />
            <el-option label="B1级" value="B1" />
            <el-option label="A2级" value="A2" />
            <el-option label="A1级" value="A1" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="考核开始">
              <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考核结束">
              <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="总名额" prop="maxParticipants">
          <el-input-number v-model="form.maxParticipants" :min="0" :max="10000" placeholder="0表示不限制" />
          <span style="margin-left: 10px; color: #999;">0表示不限制</span>
        </el-form-item>
        <el-form-item label="评分人数" prop="reviewerCount">
          <el-input-number v-model="form.reviewerCount" :min="1" :max="10" />
          <span style="margin-left: 10px; color: #999;">需要评分的考核员数量</span>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="报名开始">
              <el-date-picker v-model="form.enrollmentStart" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报名截止">
              <el-date-picker v-model="form.enrollmentEnd" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewerDialogVisible" title="评分人配置" width="500px">
      <el-form label-width="100px">
        <el-form-item label="评分人数">
          <el-input-number v-model="reviewerConfig.reviewerCount" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="选择评分人">
          <el-checkbox-group v-model="reviewerConfig.selectedReviewers">
            <el-checkbox v-for="e in evaluators" :key="e.id" :label="e.id">
              {{ e.realName }} ({{ e.department }})
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveReviewerConfig">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getActivityList, createActivity, updateActivity, deleteActivity, getEnrollmentInfo, updateReviewerConfig } from '@/api/activity'
import { getEvaluators } from '@/api/user'

const loading = ref(false)
const dialogVisible = ref(false)
const reviewerDialogVisible = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)
const currentActivityId = ref<number | null>(null)

const tableData = ref<any[]>([])
const evaluators = ref<any[]>([])

const form = reactive({
  name: '',
  level: '',
  startDate: '',
  endDate: '',
  maxParticipants: 0,
  reviewerCount: 2,
  enrollmentStart: '',
  enrollmentEnd: '',
  description: ''
})

const reviewerConfig = reactive({
  reviewerCount: 2,
  selectedReviewers: [] as number[]
})

const rules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择级别', trigger: 'change' }]
}

const getLevelType = (level: string) => {
  const types: Record<string, string> = {
    'C': 'info',
    'B2': 'primary',
    'B1': 'success',
    'A2': 'warning',
    'A1': 'danger'
  }
  return types[level] || 'info'
}

const formatDateTime = (datetime: string | null) => {
  if (!datetime) return '-'
  return datetime.slice(0, 16).replace('T', ' ')
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getActivityList()
    if (res.code === 200) {
      const activities = res.data || []
      for (const activity of activities) {
        try {
          const infoRes = await getEnrollmentInfo(activity.id)
          activity.enrolledCount = infoRes.data.enrolledCount
          activity.remaining = infoRes.data.remaining
        } catch {
          activity.enrolledCount = 0
          activity.remaining = -1
        }
      }
      tableData.value = activities
    }
  } finally {
    loading.value = false
  }
}

const loadEvaluators = async () => {
  const res = await getEvaluators()
  if (res.code === 200) {
    evaluators.value = res.data
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, {
    name: '',
    level: '',
    startDate: '',
    endDate: '',
    maxParticipants: 0,
    reviewerCount: 2,
    enrollmentStart: '',
    enrollmentEnd: '',
    description: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  editId.value = row.id
  Object.assign(form, {
    name: row.name,
    level: row.level,
    startDate: row.startDate,
    endDate: row.endDate,
    maxParticipants: row.maxParticipants || 0,
    reviewerCount: row.reviewerCount || 2,
    enrollmentStart: row.enrollmentStart,
    enrollmentEnd: row.enrollmentEnd,
    description: row.description
  })
  dialogVisible.value = true
}

const handleReviewerConfig = (row: any) => {
  currentActivityId.value = row.id
  reviewerConfig.reviewerCount = row.reviewerCount || 2
  reviewerConfig.selectedReviewers = row.reviewerIds ? JSON.parse(row.reviewerIds).map((id: number) => id) : []
  reviewerDialogVisible.value = true
}

const handleSaveReviewerConfig = async () => {
  if (!currentActivityId.value) return
  
  const reviewerIds = JSON.stringify(reviewerConfig.selectedReviewers)
  await updateReviewerConfig(currentActivityId.value, reviewerConfig.reviewerCount, reviewerIds)
  ElMessage.success('评分人配置已保存')
  reviewerDialogVisible.value = false
  loadData()
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    let res
    if (editId.value) {
      res = await updateActivity(editId.value, form)
    } else {
      res = await createActivity({
        ...form,
        status: 'draft'
      })
    }
    if (res.code === 200) {
      ElMessage.success('操作成功')
      dialogVisible.value = false
      loadData()
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

const toggleStatus = async (row: any) => {
  const newStatus = row.status === 'active' ? 'closed' : 'active'
  try {
    await updateActivity(row.id, { status: newStatus })
    ElMessage.success('状态已更新')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确定要删除该活动吗？', '提示', { type: 'warning' })
  try {
    await deleteActivity(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  loadData()
  loadEvaluators()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.text-danger {
  color: #f56c6c;
  font-weight: bold;
}
</style>