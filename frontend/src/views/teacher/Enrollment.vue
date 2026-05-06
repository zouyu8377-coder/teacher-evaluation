<template>
  <div class="enrollment">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的考核</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 已报名活动 - 卡片展示 -->
        <el-tab-pane label="已报名活动" name="enrolled">
          <div v-if="myEnrollments.length === 0 && !loading" class="empty-container">
            <el-empty description="暂未报名任何考核活动" />
          </div>

          <div v-else class="activity-grid">
            <div
              v-for="item in myEnrollments"
              :key="item.id"
              class="activity-card"
              @click="goToActivityDetail(item)"
            >
              <div class="card-top">
                <div class="activity-level" :class="'level-' + item.level">
                  {{ item.level }}级
                </div>
                <div class="activity-status" :class="'status-' + getActivityStatus(item)">
                  {{ getStatusText(item) }}
                </div>
              </div>

              <h3 class="activity-name">{{ item.activityName }}</h3>

              <div class="activity-info">
                <div class="info-item">
                  <span class="label">报名时间</span>
                  <span class="value">{{ formatDateTime(item.enrolledAt) }}</span>
                </div>
                <div class="info-item">
                  <span class="label">{{ item.level === 'C' ? '考试时间' : '上传窗口' }}</span>
                  <span class="value">{{ item.level === 'C' ? getExamWindowText(item) : getMaterialWindowText(item) }}</span>
                </div>
              </div>

              <!-- C级：显示考试状态 -->
              <div v-if="item.level === 'C'" class="card-action">
                <template v-if="item.examStatus === 'in_progress'">
                  <template v-if="isExamWindowOpen(item)">
                    <el-button type="primary" @click.stop="goToExam(item)">继续考试</el-button>
                  </template>
                  <template v-else>
                    <el-button type="info" disabled>考试时间已结束</el-button>
                  </template>
                </template>
                <template v-else-if="item.examRecordId">
                  <template v-if="!item.scorePublished">
                    <el-button type="info" disabled>考试完成，等待成绩发布</el-button>
                  </template>
                  <template v-else>
                    <div class="score-display">
                      <span class="score-label">得分</span>
                      <span class="score-value" :class="getScoreClass(item.finalScore, item.isPassed)">
                        {{ item.finalScore }}
                      </span>
                    </div>
                  </template>
                  <el-button type="primary" size="small" @click.stop="viewExamDetail(item)" style="margin-left: 8px;">
                    查看详情
                  </el-button>
                </template>
                <template v-else>
                  <template v-if="isExamWindowOpen(item)">
                    <el-button type="primary" @click.stop="goToExam(item)">开始考试</el-button>
                  </template>
                  <template v-else>
                    <el-button type="info" disabled>未开放</el-button>
                  </template>
                </template>
              </div>

              <!-- 非C级：显示文档上传状态 -->
              <div v-else class="card-action">
                <template v-if="!item.documentId">
                  <template v-if="isMaterialWindowOpen(item)">
                    <el-button type="primary" @click.stop="goToUpload(item)">上传文档</el-button>
                  </template>
                  <template v-else>
                    <el-button type="info" disabled>未开放</el-button>
                  </template>
                </template>
                <template v-else-if="!item.scorePublished">
                  <el-button type="info" disabled>已上传，等待评分</el-button>
                </template>
                <template v-else>
                  <div class="score-display">
                    <span class="score-label">得分</span>
                    <span class="score-value" :class="getScoreClass(item.finalScore, item.isPassed)">
                      {{ item.finalScore }}
                    </span>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 报名新活动 -->
        <el-tab-pane label="报名新活动" name="available">
          <el-table :data="availableActivities" stripe v-loading="loading">
            <el-table-column prop="name" label="活动名称" />
            <el-table-column prop="level" label="级别">
              <template #default="{ row }">
                <el-tag :type="getLevelType(row.level)">{{ row.level }}级</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" show-overflow-tooltip />
            <el-table-column label="考核类型" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.hasExam" type="warning">考试</el-tag>
                <el-tag v-else type="success">文档</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="考核时间" width="220">
              <template #default="{ row }">
                <span v-if="row.level === 'C'">
                  {{ formatDateTimeFull(row.examStart) }} ~ {{ formatDateTimeFull(row.examEnd) }}
                </span>
                <span v-else>
                  {{ formatDateTimeFull(row.materialStart) }} ~ {{ formatDateTimeFull(row.materialEnd) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="报名余量" width="100">
              <template #default="{ row }">
                <span v-if="row.enrollmentInfo" :class="{ 'text-danger': row.enrollmentInfo.remaining === 0 }">
                  {{ row.enrollmentInfo.remaining === -1 ? '不限' : `${row.enrollmentInfo.remaining}/${row.enrollmentInfo.maxParticipants}` }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="报名时间" width="180">
              <template #default="{ row }">
                <span v-if="row.enrollmentInfo">
                  {{ formatDateTime(row.enrollmentInfo.enrollmentStart) }} ~ {{ formatDateTime(row.enrollmentInfo.enrollmentEnd) }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="location" label="考核地点" width="120" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.location || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  @click="handleEnroll(row)"
                  :disabled="row.enrollmentStatus !== 'active' || !row.canEnroll || (row.enrollmentInfo && row.enrollmentInfo.remaining === 0)"
                >
                  {{ row.enrollmentStatus === 'ended' ? '报名已截止' : row.enrollmentStatus === 'pending' ? '未开始' : '报名' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="availableActivities.length === 0 && !loading" description="暂无可报名活动" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { useActivityStore } from '@/stores/activity'
import type { Activity } from '@/api/types'

const router = useRouter()
const route = useRoute()
const activityStore = useActivityStore()
const { myEnrollments, loading } = storeToRefs(activityStore)

const activeTab = ref((route.query.tab as string) || 'enrolled')
const availableActivities = ref<(Activity & { enrollmentStatus?: string; canEnroll?: boolean; enrollmentInfo?: any })[]>([])

const formatDateTime = (datetime: string | null) => {
  if (!datetime) return '-'
  return datetime.slice(0, 16).replace('T', ' ')
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

// 获取活动状态
const getActivityStatus = (item: any) => {
  // 已评分发布成绩
  if (item.scorePublished && item.finalScore !== undefined) {
    return 'completed'
  }
  // C级 - 考试相关
  if (item.level === 'C') {
    // 有考试记录且状态为submitted表示已完成考试
    if (item.examRecordId && item.examStatus === 'submitted') {
      return 'exam-completed'  // 考试完成，等待评分
    }
    if (item.examRecordId && item.examStatus === 'in_progress') {
      return 'exam-in-progress'  // 考试进行中
    }
    return 'pending-exam'  // 待考试
  }
  // 非C级 - 文档相关
  if (item.documentId) {
    return 'pending-score'  // 已上传，等待评分
  }
  return 'pending-upload'  // 待上传文档
}

// 获取状态文本
const getStatusText = (item: any) => {
  const status = getActivityStatus(item)
  const statusMap: Record<string, string> = {
    'completed': '已完成',
    'exam-completed': '待评分',
    'exam-in-progress': '考试中',
    'pending-exam': '待考试',
    'pending-upload': '待上传',
    'pending-score': '待评分'
  }
  return statusMap[status] || ''
}

const getScoreClass = (score: number | null, isPassed?: boolean | null) => {
  if (isPassed === true) return 'score-high'
  if (isPassed === false) return 'score-low'
  if (score === null) return 'score-low'
  if (score >= 90) return 'score-high'
  if (score >= 60) return 'score-mid'
  return 'score-low'
}

const formatDateTimeFull = (dateStr: string | null) => {
  if (!dateStr) return '-'
  return dateStr.slice(0, 16).replace('T', ' ')
}

const getExamWindowText = (item: any) => {
  return `${formatDateTimeFull(item.examStart)} ~ ${formatDateTimeFull(item.examEnd)}`
}

const getMaterialWindowText = (item: any) => {
  return `${formatDateTimeFull(item.materialStart)} ~ ${formatDateTimeFull(item.materialEnd)}`
}

const isExamWindowOpen = (item: any) => {
  const now = new Date()
  const start = item.examStart ? new Date(item.examStart) : null
  const end = item.examEnd ? new Date(item.examEnd) : null
  if (start && now < start) return false
  if (end && now > end) return false
  return true
}

const isMaterialWindowOpen = (item: any) => {
  const now = new Date()
  const start = item.materialStart ? new Date(item.materialStart) : null
  const end = item.materialEnd ? new Date(item.materialEnd) : null
  if (start && now < start) return false
  if (end && now > end) return false
  return true
}

const loadAvailableActivities = async () => {
  const activities = await activityStore.loadAvailableActivities()
  const now = new Date()

  const enhanced = await Promise.all(
    activities.map(async (activity) => {
      const enrollmentStart = activity.enrollmentStart ? new Date(activity.enrollmentStart) : null
      const enrollmentEnd = activity.enrollmentEnd ? new Date(activity.enrollmentEnd) : null
      let enrollmentStatus = 'active'
      if (enrollmentStart && now < enrollmentStart) {
        enrollmentStatus = 'pending'
      } else if (enrollmentEnd && now > enrollmentEnd) {
        enrollmentStatus = 'ended'
      }

      const [canEnroll, enrollmentInfo] = await Promise.all([
        activityStore.checkCanEnroll(activity.id).catch(() => false),
        activityStore.loadEnrollmentInfo(activity.id).catch(() => null)
      ])

      return {
        ...activity,
        enrollmentStatus,
        canEnroll,
        enrollmentInfo
      }
    })
  )

  const statusOrder = { active: 0, pending: 1, ended: 2 }
  enhanced.sort((a, b) => {
    const statusDiff = statusOrder[a.enrollmentStatus as keyof typeof statusOrder] - statusOrder[b.enrollmentStatus as keyof typeof statusOrder]
    if (statusDiff !== 0) return statusDiff
    const aTime = a.enrollmentStart ? new Date(a.enrollmentStart).getTime() : 0
    const bTime = b.enrollmentStart ? new Date(b.enrollmentStart).getTime() : 0
    return aTime - bTime
  })

  availableActivities.value = enhanced
}

const loadMyActivities = async () => {
  await activityStore.loadMyEnrollments()
}

const handleEnroll = async (row: any) => {
  try {
    const res = await activityStore.doEnroll(row.id)
    if (res.code === 200) {
      const levelText = row.level === 'C' ? '考试' : '上传考核文档'
      ElMessage.success(`报名成功！请等待报名时间开始后进行${row.level}级${levelText}。`)
      await loadAvailableActivities()
      activeTab.value = 'enrolled'
    }
  } catch (e: any) {
    const msg = e.response?.data?.message || '报名失败'
    ElMessage.error(msg)
  }
}

const goToUpload = (row: any) => {
  router.push({ path: '/teacher/upload', query: { activityId: row.activityId } })
}

const goToExam = (row: any) => {
  router.push({ path: '/teacher/exam', query: { activityId: row.activityId } })
}

const viewExamDetail = (item: any) => {
  router.push({ path: '/teacher/exam', query: { recordId: item.examRecordId } })
}

const goToActivityDetail = (item: any) => {
  router.push(`/teacher/activities/${item.activityId}`)
}

onMounted(async () => {
  await loadMyActivities()
  await loadAvailableActivities()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-container {
  padding: 40px 0;
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.activity-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.3s ease;
}

.activity-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.activity-level {
  padding: 4px 12px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.875rem;
}

.activity-level.level-C { background: #e0e7ff; color: #4f46e5; }
.activity-level.level-B2 { background: #dbeafe; color: #2563eb; }
.activity-level.level-B1 { background: #fed7aa; color: #ea580c; }
.activity-level.level-A2 { background: #fce7f3; color: #db2777; }
.activity-level.level-A1 { background: #fee2e2; color: #dc2626; }

.activity-status {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 500;
}

.activity-status.status-completed { background: #dcfce7; color: #16a34a; }
.activity-status.status-exam-completed { background: #fef3c7; color: #d97706; }
.activity-status.status-exam-in-progress { background: #e0e7ff; color: #4f46e5; }
.activity-status.status-pending-exam { background: #e0e7ff; color: #4f46e5; }
.activity-status.status-pending-upload { background: #fef3c7; color: #d97706; }
.activity-status.status-pending-score { background: #f1f5f9; color: #64748b; }

.activity-name {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px 0;
}

.activity-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
}

.info-item .label {
  color: #64748b;
}

.info-item .value {
  color: #1e293b;
  font-weight: 500;
}

.card-action {
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: center;
}

.score-display {
  display: flex;
  align-items: center;
  gap: 12px;
}

.score-label {
  font-size: 0.875rem;
  color: #64748b;
}

.score-value {
  font-size: 1.5rem;
  font-weight: 700;
}

.score-high { color: #16a34a; }
.score-mid { color: #d97706; }
.score-low { color: #dc2626; }

.text-danger {
  color: #f56c6c;
  font-weight: bold;
}
</style>