<template>
  <div class="teacher-dashboard">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <h1>欢迎回来，{{ dashboard?.userInfo?.realName || '教师' }}</h1>
        <p>{{ currentDate }}</p>
      </div>
      <div class="welcome-decoration"></div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>

    <template v-else>
      <!-- 主要信息区域 -->
      <div class="dashboard-grid">
        <!-- 左侧列 -->
        <div class="dashboard-left">
          <!-- 用户信息卡片 -->
          <div class="info-card user-card">
            <div class="card-header">
              <span class="material-symbols-outlined">person</span>
              <h3>基本信息</h3>
            </div>
            <div class="card-body">
              <div class="info-row">
                <span class="info-label">用户名</span>
                <span class="info-value">{{ dashboard?.userInfo?.username }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">姓名</span>
                <span class="info-value">{{ dashboard?.userInfo?.realName }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">部门</span>
                <span class="info-value">{{ dashboard?.userInfo?.department || '未设置' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">注册时间</span>
                <span class="info-value">{{ formatDate(dashboard?.userInfo?.createdAt) }}</span>
              </div>
            </div>
          </div>

          <!-- 当前级别卡片 -->
          <div class="info-card level-card">
            <div class="card-header">
              <span class="material-symbols-outlined">military_tech</span>
              <h3>当前级别</h3>
            </div>
            <div class="card-body">
              <div class="level-display">
                <div class="level-badge" :class="getLevelClass(dashboard?.currentLevel?.level)">
                  {{ dashboard?.currentLevel?.level || '无' }}
                </div>
                <div class="level-info">
                  <span class="level-name">{{ dashboard?.currentLevel?.levelName || '无级别' }}</span>
                  <span class="level-status" :class="{ passed: dashboard?.currentLevel?.hasPassed }">
                    {{ dashboard?.currentLevel?.hasPassed ? '已通过' : '未通过' }}
                  </span>
                </div>
              </div>
              <div v-if="dashboard?.currentLevel?.hasPassed && dashboard?.currentLevel?.bestScore" class="best-score">
                <span>最高成绩</span>
                <strong>{{ dashboard.currentLevel.bestScore }}分</strong>
              </div>
              <div v-if="dashboard?.currentLevel?.passedAt" class="pass-date">
                <span>通过时间</span>
                <strong>{{ formatDate(dashboard.currentLevel.passedAt) }}</strong>
              </div>
              <div v-if="dashboard?.currentLevel?.canEnrollNext" class="next-level-hint">
                <span class="material-symbols-outlined">arrow_forward</span>
                可报考: {{ dashboard?.currentLevel?.nextLevel }}级
              </div>
              <div v-else-if="!dashboard?.currentLevel?.hasPassed" class="next-level-hint">
                <span class="material-symbols-outlined">info</span>
                C级可直接报名
              </div>
            </div>
          </div>

          <!-- 考核历史 -->
          <div class="info-card history-card">
            <div class="card-header">
              <span class="material-symbols-outlined">history</span>
              <h3>考核历史</h3>
            </div>
            <div class="card-body">
              <template v-if="dashboard?.historyRecords?.length">
                <div
                  v-for="record in dashboard.historyRecords"
                  :key="record.activityId + record.completedAt"
                  class="history-item"
                  :class="{ passed: record.isPassed, failed: !record.isPassed }"
                >
                  <div class="history-icon">
                    <span class="material-symbols-outlined">
                      {{ record.isPassed ? 'check_circle' : 'cancel' }}
                    </span>
                  </div>
                  <div class="history-content">
                    <span class="history-name">{{ record.activityName }}</span>
                    <span class="history-level">{{ record.levelName }}</span>
                    <span class="history-score" v-if="record.finalScore">
                      {{ record.finalScore }}分
                    </span>
                  </div>
                  <div class="history-date">
                    {{ formatDate(record.completedAt) }}
                  </div>
                </div>
              </template>
              <div v-else class="empty-state small">
                <span class="material-symbols-outlined">history</span>
                <p>暂无历史记录</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧列 -->
        <div class="dashboard-right">
          <!-- 待办事项 -->
          <div class="info-card todo-card">
            <div class="card-header">
              <span class="material-symbols-outlined">task_alt</span>
              <h3>待办事项</h3>
              <el-badge v-if="dashboard?.todoItems?.length" :value="dashboard.todoItems.length" type="danger" />
            </div>
            <div class="card-body">
              <template v-if="dashboard?.todoItems?.length">
                <div
                  v-for="(todo, index) in dashboard.todoItems"
                  :key="index"
                  class="todo-item"
                  :class="todo.type"
                  @click="handleTodoClick(todo)"
                >
                  <div class="todo-icon">
                    <span class="material-symbols-outlined">
                      {{ getTodoIcon(todo.type) }}
                    </span>
                  </div>
                  <div class="todo-content">
                    <span class="todo-title">{{ todo.title }}</span>
                    <span class="todo-desc">{{ todo.description }}</span>
                    <span v-if="todo.deadline" class="todo-deadline">
                      <span class="material-symbols-outlined">schedule</span>
                      截止: {{ formatDate(todo.deadline) }}
                    </span>
                  </div>
                  <span class="material-symbols-outlined arrow">chevron_right</span>
                </div>
              </template>
              <div v-else class="empty-state">
                <span class="material-symbols-outlined">check_circle</span>
                <p>暂无待办事项</p>
              </div>
            </div>
          </div>

          <!-- 当前报考 -->
          <div class="info-card enrollment-card">
            <div class="card-header">
              <span class="material-symbols-outlined">school</span>
              <h3>当前报考</h3>
            </div>
            <div class="card-body">
              <template v-if="dashboard?.currentEnrollments?.length">
                <div
                  v-for="enrollment in dashboard.currentEnrollments"
                  :key="enrollment.enrollmentId"
                  class="enrollment-item"
                >
                  <div class="enrollment-header">
                    <span class="enrollment-name">{{ enrollment.activityName }}</span>
                    <el-tag :type="getEnrollmentStatusType(enrollment.status)" size="small">
                      {{ enrollment.status === 'enrolled' ? '已报名' : enrollment.status }}
                    </el-tag>
                  </div>
                  <div class="enrollment-meta">
                    <span class="level-tag">{{ enrollment.levelName }}</span>
                    <span class="enroll-date">报名于 {{ formatDate(enrollment.enrolledAt) }}</span>
                  </div>
                  <!-- 显示时间窗口 -->
                  <div class="time-window">
                    <span class="time-label">{{ enrollment.hasExam ? '考试时间' : '上传窗口' }}：</span>
                    <span class="time-value">{{ getWindowText(enrollment) }}</span>
                  </div>

                  <div v-if="enrollment.hasExam" class="exam-status">
                    <!-- 未参加考试或未开始 -->
                    <template v-if="!enrollment.examStatus || enrollment.examStatus === 'not_started'">
                      <template v-if="isInWindow(enrollment)">
                        <el-button type="primary" size="small" @click="startExam(enrollment)">
                          开始考试
                        </el-button>
                        <span class="exam-hint">时长: {{ enrollment.examDurationMinutes }}分钟</span>
                      </template>
                      <template v-else>
                        <el-tag type="info" size="small">未开放</el-tag>
                      </template>
                    </template>
                    <!-- 考试进行中 -->
                    <template v-else-if="enrollment.examStatus === 'in_progress'">
                      <el-button type="warning" size="small" @click="continueExam(enrollment)">
                        继续考试
                      </el-button>
                      <span class="exam-hint">考试进行中</span>
                    </template>
                    <!-- 考试已完成（未评分或已评分） -->
                    <template v-else-if="enrollment.examStatus === 'completed'">
                      <template v-if="!enrollment.scorePublished">
                        <el-tag type="info" size="small">考试完成，等待评分</el-tag>
                      </template>
                      <template v-else-if="enrollment.isPassed">
                        <el-tag type="success" size="small">已通过</el-tag>
                        <span class="score-display">得分: {{ enrollment.finalScore }}</span>
                      </template>
                      <template v-else>
                        <el-tag type="danger" size="small">未通过</el-tag>
                        <span class="score-display">得分: {{ enrollment.finalScore }}</span>
                      </template>
                    </template>
                  </div>
                  <!-- 非C级：文档上传入口 -->
                  <div v-else class="exam-status">
                    <template v-if="isInWindow(enrollment)">
                      <el-button type="primary" size="small" @click="goToUpload(enrollment)">
                        上传文档
                      </el-button>
                    </template>
                    <template v-else>
                      <el-tag type="info" size="small">未开放</el-tag>
                    </template>
                  </div>
                </div>
              </template>
              <div v-else class="empty-state">
                <span class="material-symbols-outlined">event_busy</span>
                <p>暂无进行中的报考</p>
                <el-button type="primary" @click="$router.push('/teacher/enrollment')">
                  前往报名
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTeacherDashboard, type TeacherDashboard, type TodoItem, type EnrollmentInfo } from '@/api/teacher'

const router = useRouter()

const loading = ref(true)
const error = ref(false)
const dashboard = ref<TeacherDashboard | null>(null)

const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})

const loadData = async () => {
  loading.value = true
  error.value = false
  try {
    const res = await getTeacherDashboard()
    dashboard.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
    error.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

const formatDate = (dateStr: string | null | undefined) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const formatDateTime = (dateStr: string | null | undefined) => {
  if (!dateStr) return '未设置'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 检查当前时间是否在考试/上传窗口期内
const isInWindow = (enrollment: EnrollmentInfo) => {
  const now = new Date()
  if (enrollment.hasExam) {
    const start = enrollment.examStartTime ? new Date(enrollment.examStartTime) : null
    const end = enrollment.examEndTime ? new Date(enrollment.examEndTime) : null
    if (start && now < start) return false
    if (end && now > end) return false
    return true
  } else {
    const start = enrollment.materialStartTime ? new Date(enrollment.materialStartTime) : null
    const end = enrollment.materialEndTime ? new Date(enrollment.materialEndTime) : null
    if (start && now < start) return false
    if (end && now > end) return false
    return true
  }
}

// 获取时间窗口描述
const getWindowText = (enrollment: EnrollmentInfo) => {
  if (enrollment.hasExam) {
    return `${formatDateTime(enrollment.examStartTime)} ~ ${formatDateTime(enrollment.examEndTime)}`
  } else {
    return `${formatDateTime(enrollment.materialStartTime)} ~ ${formatDateTime(enrollment.materialEndTime)}`
  }
}

const getLevelClass = (level: string | undefined) => {
  if (!level) return 'level-c'
  const levelOrder: Record<string, number> = { C: 0, B2: 1, B1: 2, A2: 3, A1: 4 }
  const order = levelOrder[level] ?? 0
  return `level-${['c', 'b2', 'b1', 'a2', 'a1'][order]}`
}

const getTodoIcon = (type: string) => {
  const iconMap: Record<string, string> = {
    exam: 'quiz',
    enrollment: 'how_to_reg',
    evaluation: 'rate_review'
  }
  return iconMap[type] || 'task'
}

const handleTodoClick = (todo: TodoItem) => {
  if (todo.type === 'exam') {
    router.push(`/teacher/exam?activityId=${todo.relatedId}`)
  } else if (todo.type === 'enrollment') {
    router.push('/teacher/enrollment')
  }
}

const getEnrollmentStatusType = (status: string) => {
  const typeMap: Record<string, any> = {
    enrolled: 'success',
    removed: 'info'
  }
  return typeMap[status] || 'info'
}

const startExam = (enrollment: EnrollmentInfo) => {
  router.push(`/teacher/exam?activityId=${enrollment.activityId}`)
}

const continueExam = (enrollment: EnrollmentInfo) => {
  router.push(`/teacher/exam?activityId=${enrollment.activityId}&recordId=${enrollment.examRecordId}`)
}
</script>

<style scoped>
.teacher-dashboard {
  max-width: 1400px;
  margin: 0 auto;
}

/* 欢迎横幅 */
.welcome-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}

.welcome-content {
  position: relative;
  z-index: 1;
  color: white;
}

.welcome-content h1 {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 8px 0;
}

.welcome-content p {
  margin: 0;
  opacity: 0.9;
}

.welcome-decoration {
  position: absolute;
  right: -50px;
  top: -50px;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

/* 加载状态 */
.loading-container {
  background: white;
  border-radius: 16px;
  padding: 24px;
}

/* 网格布局 */
.dashboard-grid {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 24px;
}

@media (max-width: 1024px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

/* 卡片通用样式 */
.info-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  background: #f8fafc;
}

.card-header .material-symbols-outlined {
  color: #6366f1;
  font-size: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 0.9375rem;
  font-weight: 600;
  color: #1e293b;
  flex: 1;
}

.card-body {
  padding: 20px;
}

/* 用户信息卡片 */
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  color: #64748b;
  font-size: 0.875rem;
}

.info-value {
  color: #1e293b;
  font-weight: 500;
  font-size: 0.875rem;
}

/* 级别卡片 */
.level-display {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.level-badge {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
}

.level-badge.level-c { background: linear-gradient(135deg, #10b981, #059669); }
.level-badge.level-b2 { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.level-badge.level-b1 { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }
.level-badge.level-a2 { background: linear-gradient(135deg, #f59e0b, #d97706); }
.level-badge.level-a1 { background: linear-gradient(135deg, #ef4444, #dc2626); }

.level-info {
  display: flex;
  flex-direction: column;
}

.level-name {
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
}

.level-status {
  font-size: 0.75rem;
  color: #94a3b8;
}

.level-status.passed {
  color: #10b981;
}

.best-score, .pass-date {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 0.875rem;
}

.best-score span, .pass-date span {
  color: #64748b;
}

.best-score strong, .pass-date strong {
  color: #1e293b;
}

.next-level-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 10px;
  background: #f0f9ff;
  border-radius: 8px;
  color: #0369a1;
  font-size: 0.8125rem;
}

.next-level-hint .material-symbols-outlined {
  font-size: 16px;
}

/* 待办事项 */
.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.todo-item:hover {
  border-color: #6366f1;
  background: #f8faff;
}

.todo-item:last-child {
  margin-bottom: 0;
}

.todo-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
}

.todo-item.exam .todo-icon { background: #fef3c7; color: #d97706; }
.todo-item.enrollment .todo-icon { background: #dbeafe; color: #2563eb; }

.todo-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.todo-title {
  font-weight: 600;
  color: #1e293b;
  font-size: 0.875rem;
}

.todo-desc {
  color: #64748b;
  font-size: 0.8125rem;
  margin-top: 2px;
}

.todo-deadline {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #94a3b8;
  font-size: 0.75rem;
  margin-top: 4px;
}

.todo-deadline .material-symbols-outlined {
  font-size: 14px;
}

.todo-item .arrow {
  color: #94a3b8;
}

/* 报考卡片 */
.enrollment-item {
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  margin-bottom: 12px;
}

.enrollment-item:last-child {
  margin-bottom: 0;
}

.enrollment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.enrollment-name {
  font-weight: 600;
  color: #1e293b;
}

.enrollment-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.level-tag {
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  color: #64748b;
}

.enroll-date {
  font-size: 0.75rem;
  color: #94a3b8;
}

.exam-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.exam-hint {
  font-size: 0.75rem;
  color: #94a3b8;
}

/* 历史记录 */
.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}

.history-item:last-child {
  border-bottom: none;
}

.history-icon .material-symbols-outlined {
  font-size: 24px;
}

.history-item.passed .history-icon { color: #10b981; }
.history-item.failed .history-icon { color: #ef4444; }

.history-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.history-name {
  font-weight: 500;
  color: #1e293b;
  font-size: 0.875rem;
}

.history-level {
  font-size: 0.75rem;
  color: #64748b;
}

.history-score {
  font-size: 0.875rem;
  font-weight: 600;
  color: #6366f1;
}

.history-date {
  font-size: 0.75rem;
  color: #94a3b8;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px;
  color: #94a3b8;
}

.empty-state .material-symbols-outlined {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-state p {
  margin: 0 0 16px 0;
}

.empty-state.small {
  padding: 20px;
}

.empty-state.small .material-symbols-outlined {
  font-size: 32px;
}
</style>