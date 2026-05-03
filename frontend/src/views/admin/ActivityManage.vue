<template>
  <div class="activity-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>活动管理</span>
          <el-button type="primary" @click="handleAdd">新增活动</el-button>
        </div>
      </template>

      <el-form inline>
        <el-form-item label="报名状态">
          <el-select v-model="enrollmentStatusFilter" placeholder="全部" clearable style="width: 150px;">
            <el-option label="未开始报名" value="pending" />
            <el-option label="报名中" value="active" />
            <el-option label="报名已截止" value="ended" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="filteredActivities" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="活动名称" />
        <el-table-column prop="level" label="级别">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)">{{ row.level }}级</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间安排" width="200">
          <template #default="{ row }">
            <div class="time-cell">
              <div>报名: {{ formatDateTime(row.enrollmentStart) }} ~ {{ formatDateTime(row.enrollmentEnd) }}</div>
              <div v-if="row.level === 'C'">考试: {{ formatDateTime(row.examStart) }} ~ {{ formatDateTime(row.examEnd) }}</div>
              <div v-else>上传: {{ formatDateTime(row.materialStart) }} ~ {{ formatDateTime(row.materialEnd) }}</div>
            </div>
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
            <div class="action-buttons">
              <el-button type="success" link @click="goToDetail(row)">详情</el-button>
              <el-button :type="row.status === 'active' ? 'warning' : 'success'" link @click="toggleStatus(row)">
                {{ row.status === 'active' ? '关闭' : '启用' }}
              </el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </div>
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
            <el-form-item label="报名开始" prop="enrollmentStart">
              <el-date-picker v-model="form.enrollmentStart" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报名截止" prop="enrollmentEnd">
              <el-date-picker v-model="form.enrollmentEnd" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="form.level === 'C'">
          <el-col :span="12">
            <el-form-item label="考试开始" prop="examStart">
              <el-date-picker v-model="form.examStart" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试时长" prop="examDurationMinutes">
              <el-input-number v-model="form.examDurationMinutes" :min="10" :max="480" />
              <span style="margin-left: 10px; color: #999;">分钟</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="总名额" prop="maxParticipants">
          <el-input-number v-model="form.maxParticipants" :min="0" :max="10000" placeholder="0表示不限制" />
          <span style="margin-left: 10px; color: #999;">0表示不限制</span>
        </el-form-item>
        <el-form-item label="评分人数" prop="reviewerCount">
          <el-input :value="form.selectedReviewers?.length || 0" disabled />
          <span style="margin-left: 10px; color: #999;">人在评分人配置中设置</span>
        </el-form-item>
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
      <el-alert type="info" :closable="false" class="mb-3">
        勾选评分人后，评分人数将自动同步为勾选的人数
      </el-alert>
      <el-form label-width="100px">
        <el-form-item label="评分人数">
          <el-input :value="reviewerConfig.selectedReviewers.length" disabled />
          <span style="margin-left: 10px; color: #999;">人（由勾选的评分人自动计算）</span>
        </el-form-item>
        <el-form-item label="选择评分人">
          <el-checkbox-group v-model="reviewerConfig.selectedReviewers">
            <el-checkbox v-for="e in evaluators" :key="e.id" :value="e.id">
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
import { reactive, ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
import { getActivityList, createActivity, updateActivity, deleteActivity, getEnrollmentInfo, updateReviewerConfig } from '@/api/activity'
import { getEvaluators } from '@/api/user'
import type { Activity } from '@/api/types'

const loading = ref(false)
const dialogVisible = ref(false)
const reviewerDialogVisible = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)
const currentActivityId = ref<number | null>(null)
const enrollmentStatusFilter = ref<string>('')

const tableData = ref<any[]>([])
const evaluators = ref<any[]>([])

const filteredActivities = computed(() => {
  if (!enrollmentStatusFilter.value) return tableData.value
  return tableData.value.filter(a => a.enrollmentStatus === enrollmentStatusFilter.value)
})

const form = reactive({
  name: '',
  level: '' as Activity['level'],
  maxParticipants: 0,
  reviewerCount: 2,
  selectedReviewers: [] as number[],
  enrollmentStart: '',
  enrollmentEnd: '',
  examStart: '',
  examDurationMinutes: 60,
  examEnd: '',
  description: ''
})

const reviewerConfig = reactive({
  reviewerCount: 0,
  selectedReviewers: [] as number[]
})

const rules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择级别', trigger: 'change' }],
  enrollmentStart: [{ required: true, message: '请选择报名开始时间', trigger: 'change' }],
  enrollmentEnd: [{ required: true, message: '请选择报名截止时间', trigger: 'change' }],
  examStart: [{ required: true, message: '请选择考试开始时间', trigger: 'change' }],
  examEnd: [{ required: true, message: '请选择考试截止时间', trigger: 'change' }]
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
      const activities = (res.data || []) as any[]
      const now = new Date()

      // 先计算本地状态，再并发请求报名详情
      for (const activity of activities) {
        const enrollmentStart = activity.enrollmentStart ? new Date(activity.enrollmentStart) : null
        const enrollmentEnd = activity.enrollmentEnd ? new Date(activity.enrollmentEnd) : null

        if (enrollmentStart && now < enrollmentStart) {
          activity.enrollmentStatus = 'pending'
        } else if (enrollmentEnd && now > enrollmentEnd) {
          activity.enrollmentStatus = 'ended'
        } else {
          activity.enrollmentStatus = 'active'
        }
      }

      // 并发加载所有活动的报名详情
      await Promise.all(
        activities.map(async (activity: any) => {
          try {
            const infoRes = await getEnrollmentInfo(activity.id)
            activity.enrolledCount = infoRes.data.enrolledCount
            activity.remaining = infoRes.data.remaining
          } catch {
            activity.enrolledCount = 0
            activity.remaining = -1
          }
        })
      )

      // 排序：按报名开始时间从旧到新
      activities.sort((a: any, b: any) => {
        const aTime = a.enrollmentStart ? new Date(a.enrollmentStart).getTime() : 0
        const bTime = b.enrollmentStart ? new Date(b.enrollmentStart).getTime() : 0
        return aTime - bTime
      })

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
    maxParticipants: 0,
    reviewerCount: 2,
    enrollmentStart: '',
    enrollmentEnd: '',
    examStart: '',
    examDurationMinutes: 60,
    examEnd: '',
    description: ''
  })
  dialogVisible.value = true
}

const goToDetail = (row: any) => {
  router.push(`/admin/activities/${row.id}`)
}

const safeParseReviewerIds = (reviewerIds: string | null | undefined): number[] => {
  if (!reviewerIds) return []
  try {
    const parsed = JSON.parse(reviewerIds)
    if (Array.isArray(parsed)) {
      return parsed.map((id: unknown) => Number(id)).filter((id: number) => !isNaN(id))
    }
  } catch {
    // 解析失败返回空数组
  }
  return []
}

const handleEdit = (row: any) => {
  editId.value = row.id
  Object.assign(form, {
    name: row.name,
    level: row.level,
    maxParticipants: row.maxParticipants || 0,
    reviewerCount: row.reviewerCount !== undefined && row.reviewerCount !== null ? row.reviewerCount : 2,
    selectedReviewers: safeParseReviewerIds(row.reviewerIds),
    enrollmentStart: row.enrollmentStart,
    enrollmentEnd: row.enrollmentEnd,
    examStart: row.examStart,
    examDurationMinutes: row.examDurationMinutes || 60,
    examEnd: row.examEnd,
    description: row.description
  })
  dialogVisible.value = true
}

const handleReviewerConfig = (row: any) => {
  currentActivityId.value = row.id
  reviewerConfig.reviewerCount = row.reviewerCount !== undefined && row.reviewerCount !== null ? row.reviewerCount : 2
  reviewerConfig.selectedReviewers = safeParseReviewerIds(row.reviewerIds)
  reviewerDialogVisible.value = true
}

const handleSaveReviewerConfig = async () => {
  if (!currentActivityId.value) return

  // 自动同步评分人数为勾选的评分员数量
  const reviewerCount = reviewerConfig.selectedReviewers.length
  const reviewerIds = JSON.stringify(reviewerConfig.selectedReviewers)
  await updateReviewerConfig(currentActivityId.value, reviewerCount, reviewerIds)
  ElMessage.success('评分人配置已保存')
  reviewerDialogVisible.value = false
  loadData()
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  // 前端校验：考试时间必须在报名截止之后
  if (form.level === 'C' && form.enrollmentStart && form.enrollmentEnd && form.examStart) {
    const enrollmentEnd = new Date(form.enrollmentEnd)
    const examStart = new Date(form.examStart)
    if (examStart <= enrollmentEnd) {
      ElMessage.error('考试时间应在报名结束之后')
      return
    }
  }

  loading.value = true
  try {
    let res
    if (editId.value) {
      res = await updateActivity(editId.value, form as Partial<Activity>)
    } else {
      res = await createActivity({
        ...form,
        status: 'draft'
      } as Partial<Activity>)
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
.mb-3 {
  margin-bottom: 16px;
}
.action-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
}
.action-buttons .el-button {
  padding: 4px 8px;
  min-width: auto;
}
</style>