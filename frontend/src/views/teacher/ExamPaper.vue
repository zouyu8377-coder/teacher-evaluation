<template>
  <div class="exam-paper">
    <el-card v-if="!examStarted">
      <div class="start-container">
        <el-result icon="info" title="C级考试">
          <template #sub-title>
            <p>试卷：{{ paperInfo.name }}</p>
            <p>时长：{{ paperInfo.durationMinutes }}分钟</p>
            <p>总分：{{ paperInfo.totalScore }}分</p>
            <p>题目：{{ paperInfo.questionCount }}题</p>
          </template>
          <template #extra>
            <el-button type="primary" size="large" @click="handleStartExam" :loading="starting">
              开始考试
            </el-button>
          </template>
        </el-result>
      </div>
    </el-card>

    <el-card v-else-if="examSubmitted">
      <div class="result-container">
        <el-result icon="success" title="考试完成">
          <template #sub-title>
            <div class="score-info">
              <div class="score-item">
                <span class="label">最终得分：</span>
                <span class="value">{{ examResult.score }}</span>
              </div>
              <div class="score-item">
                <span class="label">正确题数：</span>
                <span class="value success">{{ examResult.correctCount }}</span>
              </div>
              <div class="score-item">
                <span class="label">错误题数：</span>
                <span class="value danger">{{ examResult.wrongCount }}</span>
              </div>
            </div>
          </template>
          <template #extra>
            <el-button v-if="canViewAnswerDetail" type="primary" @click="showAnswerDetail = true">查看答题详情</el-button>
          </template>
        </el-result>
      </div>
    </el-card>

    <el-card v-else>
      <template #header>
        <div class="exam-header">
          <div class="exam-info">
            <span class="exam-title">{{ paperInfo.name }}</span>
            <span class="exam-time">
              剩余时间：<span :class="{ 'time-warning': remainingTime <= 300 }">
                {{ formatTime(remainingTime) }}
              </span>
            </span>
          </div>
          <div class="exam-progress">
            已答：{{ answeredCount }} / {{ questions.length }}
          </div>
        </div>
      </template>

      <div class="questions-container">
        <div v-for="(q, idx) in questions" :key="q.id" class="question-item">
          <div class="question-header">
            <span class="question-order">第{{ q.order }}题</span>
            <el-tag :type="q.type === 'single' ? 'primary' : 'success'" size="small">
              {{ q.type === 'single' ? '单选题' : '多选题' }}
            </el-tag>
            <span class="question-score">({{ q.score }}分)</span>
          </div>
          <div class="question-text">{{ q.text }}</div>
          <div class="options-list">
            <div
              v-for="opt in q.options"
              :key="opt.id"
              class="option-item"
              :class="{ 
                selected: answers[q.order] === opt.id,
                'multi-selected': q.type === 'multiple' && isMultiSelected(q.order, opt.id)
              }"
              @click="selectAnswer(q, opt.id)"
            >
              <span class="option-id">{{ opt.id }}</span>
              <span class="option-text">{{ opt.text }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="exam-footer">
        <el-button @click="confirmSubmit" type="primary" size="large">
          提交试卷
        </el-button>
      </div>
    </el-card>

    <el-dialog v-model="showAnswerDetail" title="答题详情" width="800px" top="5vh">
      <div class="answer-detail">
        <div class="detail-summary">
          <el-descriptions :column="4" border>
            <el-descriptions-item label="得分">{{ examResult.score }}</el-descriptions-item>
            <el-descriptions-item label="正确">{{ examResult.correctCount }}</el-descriptions-item>
            <el-descriptions-item label="错误">{{ examResult.wrongCount }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ formatDateTime(examResult.submittedAt) }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <el-divider />
        <div v-for="q in questions" :key="q.id" class="detail-question" :class="{ wrong: !q.isCorrect }">
          <div class="detail-header">
            <span>第{{ q.order }}题</span>
            <el-tag :type="q.isCorrect ? 'success' : 'danger'" size="small">
              {{ q.isCorrect ? '正确' : '错误' }}
            </el-tag>
          </div>
          <div class="detail-text">{{ q.text }}</div>
          <div class="detail-options">
            <div v-for="opt in q.options" :key="opt.id" class="detail-option"
              :class="{
                'user-selected': answers[q.order] === opt.id,
                'correct-answer': q.correctAnswer && q.correctAnswer.includes(opt.id)
              }">
              {{ opt.id }}. {{ opt.text }}
            </div>
          </div>
          <div class="detail-result">
            <div>您的答案：<span :class="q.isCorrect ? 'text-success' : 'text-danger'">{{ answers[q.order] || '未答' }}</span></div>
            <div v-if="!q.isCorrect">正确答案：<span class="text-success">{{ q.correctAnswer }}</span></div>
          </div>
          <div v-if="q.explanation" class="detail-explanation">
            <strong>解析：</strong>{{ q.explanation }}
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { startExam, getExamRecord, saveAnswer, submitExam, getExamRecordDetail } from '@/api/exam'
import { getActivityById } from '@/api/activity'

const route = useRoute()
const router = useRouter()

const activityId = Number(route.query.activityId)

const starting = ref(false)
const examStarted = ref(false)
const examSubmitted = ref(false)
const showAnswerDetail = ref(false)
const canViewAnswerDetail = ref(false)

const paperInfo = reactive({
  id: 0,
  name: '',
  durationMinutes: 60,
  totalScore: 0,
  questionCount: 0
})

const recordId = ref(0)
const questions = ref<any[]>([])
const answers = reactive<Record<string, string>>({})
const remainingTime = ref(0)
let timer: number | null = null

const answeredCount = computed(() => Object.keys(answers).length)

const examResult = reactive({
  score: 0,
  correctCount: 0,
  wrongCount: 0,
  submittedAt: ''
})

const isMultiSelected = (order: string, optId: string) => {
  const ans = answers[order]
  if (!ans) return false
  return ans.includes(optId)
}

const selectAnswer = (question: any, optionId: string) => {
  if (examSubmitted.value) return
  
  if (question.type === 'single') {
    answers[question.order] = optionId
  } else {
    // 多选题toggle
    const current = answers[question.order] || ''
    if (current.includes(optionId)) {
      answers[question.order] = current.replace(optionId, '')
    } else {
      answers[question.order] = current + optionId
    }
    // 排序
    const arr = answers[question.order].split('').sort()
    answers[question.order] = arr.join('')
  }
  
  // 自动保存
  saveAnswer(recordId.value, { [question.order]: answers[question.order] }).catch(() => {})
}

const handleStartExam = async () => {
  starting.value = true
  try {
    const res = await startExam(activityId)
    if (res.code === 200) {
      recordId.value = res.data.id
      examStarted.value = true
      loadExamData()
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '开始考试失败')
  } finally {
    starting.value = false
  }
}

const loadExamData = async () => {
  try {
    const res = await getExamRecord(recordId.value)
    if (res.code === 200) {
      const data = res.data as any
      Object.assign(paperInfo, data.paper)
      questions.value = data.questions

      // 加载已有答案
      for (const q of data.questions) {
        if (q.userAnswer) {
          answers[q.order] = q.userAnswer
        }
      }

      // 考试进行中时校验时间窗口
      if (data.record.status === 'in_progress') {
        const activityRes = await getActivityById(data.record.activityId)
        if (activityRes.code === 200) {
          const activity = activityRes.data
          const now = new Date()
          const examEnd = activity.examEnd ? new Date(activity.examEnd) : null
          if (examEnd && now > examEnd) {
            ElMessage.error('考试时间已结束，无法继续作答')
            examStarted.value = false
            router.push('/teacher/enrollment')
            return
          }
        }

        const started = new Date(data.record.startedAt).getTime()
        const used = Math.floor((Date.now() - started) / 1000)
        remainingTime.value = paperInfo.durationMinutes * 60 - used
        if (remainingTime.value <= 0) {
          handleAutoSubmit()
        } else {
          startTimer()
        }
      }
      
      if (data.record.status === 'submitted') {
        examSubmitted.value = true
        examResult.score = data.record.score
        examResult.correctCount = data.record.correctCount
        examResult.wrongCount = data.record.wrongCount
        examResult.submittedAt = data.record.submittedAt

        // 加载答题详情
        const detailRes = await getExamRecordDetail(recordId.value)
        if (detailRes.code === 200) {
          const detailData = detailRes.data as any
          questions.value = detailData.questions
          for (const q of detailData.questions) {
            if (q.userAnswer) {
              answers[q.order] = q.userAnswer
            }
          }
        }

        // 判断是否可以查看答题详情（活动结束后才开放）
        const activityRes = await getActivityById(data.record.activityId)
        if (activityRes.code === 200) {
          const act = activityRes.data
          const now = new Date()
          const examEnd = act.examEnd ? new Date(act.examEnd) : null
          canViewAnswerDetail.value = !examEnd || now >= examEnd
        }
      }
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载考试数据失败')
  }
}

const startTimer = () => {
  if (timer) clearInterval(timer)
  timer = window.setInterval(() => {
    remainingTime.value--
    if (remainingTime.value <= 0) {
      handleAutoSubmit()
    }
  }, 1000)
}

const confirmSubmit = async () => {
  const unanswered = questions.value.length - answeredCount.value
  if (unanswered > 0) {
    await ElMessageBox.confirm(`还有${unanswered}题未作答，确定要提交吗？`, '提示', {
      confirmButtonText: '确定提交',
      cancelButtonText: '继续作答',
      type: 'warning'
    })
  }
  await handleSubmit()
}

const handleAutoSubmit = async () => {
  ElMessage.warning('考试时间到，自动提交')
  await handleSubmit()
}

const handleSubmit = async () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  try {
    const res = await submitExam(recordId.value)
    if (res.code === 200) {
      examSubmitted.value = true
      examResult.score = res.data.score
      examResult.correctCount = res.data.correctCount
      examResult.wrongCount = res.data.wrongCount
      examResult.submittedAt = res.data.submittedAt
      
      // 刷新详情
      const detailRes = await getExamRecordDetail(recordId.value)
      if (detailRes.code === 200) {
        questions.value = detailRes.data.questions
        for (const q of detailRes.data.questions) {
          if (q.userAnswer) {
            answers[q.order] = q.userAnswer
          }
        }
      }
      
      ElMessage.success('提交成功')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  }
}

const formatTime = (seconds: number) => {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

const formatDateTime = (datetime: string) => {
  if (!datetime) return '-'
  return datetime.replace('T', ' ').slice(0, 19)
}

onMounted(async () => {
  const routeRecordId = Number(route.query.recordId)
  if (routeRecordId) {
    recordId.value = routeRecordId
    examStarted.value = true
    loadExamData()
    return
  }

  if (!activityId) {
    ElMessage.error('参数错误')
    router.push('/teacher/enrollment')
    return
  }

  // 检查活动是否有试卷及是否在考试窗口内
  try {
    const res = await getActivityById(activityId)
    if (res.code === 200) {
      const activity = res.data
      if (!activity.hasExam || !activity.examPaperId) {
        ElMessage.error('该活动没有关联试卷')
        router.push('/teacher/enrollment')
        return
      }
      const now = new Date()
      const examStart = activity.examStart ? new Date(activity.examStart) : null
      const examEnd = activity.examEnd ? new Date(activity.examEnd) : null
      if (examStart && now < examStart) {
        ElMessage.error('考试尚未开始')
        router.push('/teacher/enrollment')
        return
      }
      if (examEnd && now > examEnd) {
        ElMessage.error('考试时间已结束')
        router.push('/teacher/enrollment')
        return
      }
    }
  } catch (e) {
    // 忽略
  }
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.start-container, .result-container {
  max-width: 600px;
  margin: 50px auto;
}
.score-info {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-top: 20px;
}
.score-item .label {
  color: #666;
}
.score-item .value {
  font-size: 24px;
  font-weight: bold;
}
.score-item .value.success {
  color: #67c23a;
}
.score-item .value.danger {
  color: #f56c6c;
}
.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.exam-info {
  display: flex;
  align-items: center;
  gap: 20px;
}
.exam-title {
  font-weight: bold;
  font-size: 16px;
}
.exam-time {
  color: #666;
}
.time-warning {
  color: #f56c6c;
  font-weight: bold;
}
.exam-progress {
  color: #409eff;
}
.questions-container {
  max-height: 60vh;
  overflow-y: auto;
  padding: 10px 0;
}
.question-item {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px dashed #eee;
}
.question-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.question-order {
  font-weight: bold;
  color: #409eff;
}
.question-score {
  color: #999;
}
.question-text {
  margin-bottom: 15px;
  line-height: 1.6;
}
.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.option-item {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}
.option-item:hover {
  border-color: #409eff;
}
.option-item.selected {
  background-color: #ecf5ff;
  border-color: #409eff;
}
.option-item.multi-selected {
  background-color: #ecf5ff;
  border-color: #409eff;
}
.option-id {
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  background: #f0f0f0;
  border-radius: 50%;
  margin-right: 12px;
  font-weight: bold;
}
.option-item.selected .option-id,
.option-item.multi-selected .option-id {
  background: #409eff;
  color: white;
}
.exam-footer {
  margin-top: 20px;
  text-align: center;
}
.answer-detail {
  max-height: 70vh;
  overflow-y: auto;
}
.detail-summary {
  margin-bottom: 20px;
}
.detail-question {
  padding: 15px;
  margin-bottom: 15px;
  border-radius: 4px;
  background: #f5f7fa;
}
.detail-question.wrong {
  background: #fef0f0;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}
.detail-text {
  margin-bottom: 10px;
}
.detail-options {
  margin-bottom: 10px;
}
.detail-option {
  padding: 5px 10px;
  margin: 3px 0;
  border-radius: 3px;
}
.detail-option.user-selected {
  background: #409eff;
  color: white;
}
.detail-option.correct-answer {
  background: #67c23a;
  color: white;
}
.detail-result {
  margin: 10px 0;
}
.text-success {
  color: #67c23a;
  font-weight: bold;
}
.text-danger {
  color: #f56c6c;
  font-weight: bold;
}
.detail-explanation {
  margin-top: 10px;
  padding: 10px;
  background: #fff;
  border-radius: 4px;
  color: #666;
}
</style>