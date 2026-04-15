<template>
  <div class="evaluator-activity">
    <el-card>
      <template #header>
        <div class="header-wrapper">
          <span>考核活动</span>
          <el-button v-if="selectedActivity" @click="selectedActivity = null">返回列表</el-button>
        </div>
      </template>

      <!-- 一级：考核活动列表 -->
      <template v-if="!selectedActivity">
        <el-form inline>
          <el-form-item label="级别筛选">
            <el-select v-model="levelFilter" placeholder="全部级别" clearable style="width: 150px;">
              <el-option label="C级" value="C" />
              <el-option label="B2级" value="B2" />
              <el-option label="B1级" value="B1" />
              <el-option label="A2级" value="A2" />
              <el-option label="A1级" value="A1" />
            </el-select>
          </el-form-item>
        </el-form>

        <el-table :data="filteredActivities" stripe v-loading="loading">
          <el-table-column prop="name" label="活动名称" />
          <el-table-column prop="level" label="级别" width="100">
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.level)">{{ row.level }}级</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="考核类型" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.hasExam" type="warning">考试</el-tag>
              <el-tag v-else type="success">文档</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startDate" label="开始日期" width="120" />
          <el-table-column prop="endDate" label="结束日期" width="120" />
          <el-table-column prop="enrolledCount" label="报名人数" width="100" align="center" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button type="primary" link @click="selectActivity(row)">
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="filteredActivities.length === 0 && !loading" description="暂无考核活动" :image-size="60" />
      </template>

      <!-- 二级：参与教师列表 -->
      <template v-else>
        <div class="activity-info">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="活动名称">{{ selectedActivity.name }}</el-descriptions-item>
            <el-descriptions-item label="级别">
              <el-tag :type="getLevelType(selectedActivity.level)">{{ selectedActivity.level }}级</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="考核类型">
              <el-tag v-if="selectedActivity.hasExam" type="warning">考试</el-tag>
              <el-tag v-else type="success">文档</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <el-table :data="enrolledTeachers" stripe v-loading="teacherLoading">
          <el-table-column prop="realName" label="姓名" />
          <el-table-column prop="department" label="所属教研组" />
          <el-table-column label="提交时间" width="180">
            <template #default="{ row }">
              <span v-if="row.submittedAt">{{ formatDateTime(row.submittedAt) }}</span>
              <el-tag v-else type="info" size="small">未提交</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="已打分" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.score !== undefined && row.score !== null" type="success">{{ row.score }}</el-tag>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <div class="action-buttons">
                <template v-if="selectedActivity.level === 'C'">
                  <el-button type="primary" link @click="viewExam(row)" v-if="row.submissionStatus === 'submitted'">查看答卷</el-button>
                </template>
                <template v-else>
                  <el-button type="primary" link @click="viewDocuments(row)" v-if="row.submittedAt">查看文档</el-button>
                </template>
                <el-button type="success" link @click="goEvaluate(row)">打分</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="enrolledTeachers.length === 0 && !teacherLoading" description="暂无报名教师" :image-size="60" />

        <el-divider />

        <div class="actions">
          <el-button type="primary" @click="publishScores" :loading="publishLoading">公布成绩</el-button>
        </div>
      </template>
    </el-card>

    <el-dialog v-model="showExamDetail" title="考试详情" width="95%" top="2vh" :close-on-click-modal="false">
      <div v-if="examDetailRecord" class="detail-content">
        <el-descriptions :column="4" border class="mb-3">
          <el-descriptions-item label="教师">{{ examDetailRecord.teacherName }}</el-descriptions-item>
          <el-descriptions-item label="得分">{{ examDetailRecord.score }}</el-descriptions-item>
          <el-descriptions-item label="正确">{{ examDetailRecord.correctCount }}</el-descriptions-item>
          <el-descriptions-item label="错误">{{ examDetailRecord.wrongCount }}</el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <div class="questions-list">
          <div v-for="q in examDetailQuestions" :key="q.order" class="question-item" :class="{ wrong: !q.isCorrect }">
            <div class="question-header">
              <span>第{{ q.order }}题</span>
              <el-tag :type="q.isCorrect ? 'success' : 'danger'" size="small">
                {{ q.isCorrect ? '正确' : '错误' }}
              </el-tag>
            </div>
            <div class="question-text">{{ q.text }}</div>
            <div class="options">
              <div v-for="opt in q.options" :key="opt.id" class="option"
                :class="{
                  'user-selected': q.userAnswer && q.userAnswer.includes(opt.id),
                  'correct': q.correctAnswer && q.correctAnswer.includes(opt.id)
                }">
                {{ opt.id }}. {{ opt.text }}
              </div>
            </div>
            <div class="result-info">
              <span>答案: {{ q.userAnswer || '未答' }} → {{ q.correctAnswer }}</span>
            </div>
            <div v-if="q.explanation" class="explanation">
              解析: {{ q.explanation }}
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getActiveActivities, getActivityEnrollments } from '@/api/activity'
import { getExamRecordDetail } from '@/api/exam'
import { getTeacherActivityEvaluations, publishEvaluationScores } from '@/api/evaluation'

const router = useRouter()
const loading = ref(false)
const teacherLoading = ref(false)
const publishLoading = ref(false)
const activities = ref<any[]>([])
const enrolledTeachers = ref<any[]>([])
const selectedActivity = ref<any>(null)
const levelFilter = ref<string>('')
const showExamDetail = ref(false)
const examDetailRecord = ref<any>(null)
const examDetailQuestions = ref<any[]>([])

const filteredActivities = computed(() => {
  if (!levelFilter.value) return activities.value
  return activities.value.filter(a => a.level === levelFilter.value)
})

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
  return datetime.slice(0, 19).replace('T', ' ')
}

const loadActivities = async () => {
  loading.value = true
  try {
    const res = await getActiveActivities()
    if (res.code === 200) {
      // 获取每个活动的报名人数并排序
      const activitiesWithCount = await Promise.all(
        res.data.map(async (activity: any) => {
          // 计算报名状态
          const now = new Date()
          const enrollmentStart = activity.enrollmentStart ? new Date(activity.enrollmentStart) : null
          const enrollmentEnd = activity.enrollmentEnd ? new Date(activity.enrollmentEnd) : null

          if (enrollmentStart && now < enrollmentStart) {
            activity.enrollmentStatus = 'pending'
          } else if (enrollmentEnd && now > enrollmentEnd) {
            activity.enrollmentStatus = 'ended'
          } else {
            activity.enrollmentStatus = 'active'
          }

          try {
            const enrollRes = await getActivityEnrollments(activity.id)
            return {
              ...activity,
              enrolledCount: enrollRes.code === 200 ? enrollRes.data.length : 0
            }
          } catch {
            return { ...activity, enrolledCount: 0 }
          }
        })
      )

      // 排序：按报名开始时间从旧到新，已过报名截止的放在最后
      const statusOrder = { active: 0, pending: 1, ended: 2 }
      activitiesWithCount.sort((a, b) => {
        // 先按状态排序（报名中 > 未开始 > 已结束）
        const statusDiff = statusOrder[a.enrollmentStatus] - statusOrder[b.enrollmentStatus]
        if (statusDiff !== 0) return statusDiff
        // 同状态内按报名开始时间排序
        const aTime = a.enrollmentStart ? new Date(a.enrollmentStart).getTime() : 0
        const bTime = b.enrollmentStart ? new Date(b.enrollmentStart).getTime() : 0
        return aTime - bTime
      })

      activities.value = activitiesWithCount
    }
  } finally {
    loading.value = false
  }
}

const selectActivity = async (activity: any) => {
  selectedActivity.value = activity
  teacherLoading.value = true
  try {
    const res = await getActivityEnrollments(activity.id)
    if (res.code === 200) {
      // 获取每个教师的评分
      const teachers = res.data
      for (const teacher of teachers) {
        const scoreRes = await getTeacherActivityEvaluations(activity.id, teacher.id)
        if (scoreRes.code === 200 && scoreRes.data.records && scoreRes.data.records.length > 0) {
          teacher.score = scoreRes.data.records[0].score
        }
      }
      enrolledTeachers.value = teachers
    }
  } finally {
    teacherLoading.value = false
  }
}

const viewDocuments = (row: any) => {
  router.push(`/evaluator/documents/${row.id}?activityId=${selectedActivity.value.id}`)
}

const viewExam = async (row: any) => {
  // 获取考试记录ID
  const activityId = selectedActivity.value.id
  const res = await getActivityEnrollments(activityId)
  if (res.code === 200) {
    const teacher = res.data.find((t: any) => t.id === row.id)
    if (teacher && teacher.examRecordId) {
      const detailRes = await getExamRecordDetail(teacher.examRecordId)
      if (detailRes.code === 200) {
        examDetailRecord.value = { ...teacher, ...detailRes.data.record }
        examDetailQuestions.value = detailRes.data.questions
        showExamDetail.value = true
      }
    } else {
      ElMessage.warning('找不到该教师的考试记录')
    }
  }
}

const goEvaluate = (row: any) => {
  router.push(`/evaluator/evaluate/${row.id}?activityId=${selectedActivity.value.id}`)
}

const publishScores = async () => {
  if (!selectedActivity.value) {
    ElMessage.warning('请先选择考核活动')
    return
  }

  await ElMessageBox.confirm('确定要公布这次考核中所有已打分的成绩吗？公布后教师可查看。', '提示', { type: 'warning' })

  publishLoading.value = true
  try {
    const res = await publishEvaluationScores(selectedActivity.value.id)
    if (res.code === 200) {
      ElMessage.success('成绩已公布')
    }
  } finally {
    publishLoading.value = false
  }
}

onMounted(() => {
  loadActivities()
})
</script>

<style scoped>
.header-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.action-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
}
.activity-info {
  margin-bottom: 20px;
}
.text-muted {
  color: #909399;
}
.mb-3 {
  margin-bottom: 16px;
}
.actions {
  display: flex;
  justify-content: flex-end;
}
.detail-content {
  max-height: 75vh;
  overflow-y: auto;
}
.questions-list {
  max-height: 60vh;
  overflow-y: auto;
}
.question-item {
  padding: 15px;
  margin-bottom: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}
.question-item.wrong {
  background: #fef0f0;
}
.question-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-weight: bold;
}
.question-text {
  margin-bottom: 10px;
}
.options {
  margin-bottom: 10px;
}
.option {
  padding: 5px 10px;
  margin: 3px 0;
  background: #fff;
  border-radius: 3px;
}
.option.user-selected {
  background: #409eff;
  color: white;
}
.option.correct {
  background: #67c23a;
  color: white;
}
.result-info {
  margin: 10px 0;
  font-weight: bold;
}
.explanation {
  margin-top: 10px;
  padding: 10px;
  background: #fff;
  border-radius: 4px;
  color: #666;
}
</style>