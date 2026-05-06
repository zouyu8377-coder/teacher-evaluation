<template>
  <div class="evaluation-form">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>评分 - {{ teacherName }}</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <!-- C级考核：显示考试信息（无需人工评分）-->
      <div v-if="currentActivity?.level === 'C'" class="exam-info-section">
        <el-alert type="success" :closable="false" class="mb-3">
          <template #title>
            <span>C级考核为客观题考试，最终成绩以考试得分为准，无需人工评分。</span>
          </template>
        </el-alert>

        <el-descriptions :column="3" border class="mb-3">
          <el-descriptions-item label="自动得分">{{ examRecord?.autoScore || examRecord?.score || '-' }}</el-descriptions-item>
          <el-descriptions-item label="调整分值">{{ examRecord?.manualAdjust || 0 }}</el-descriptions-item>
          <el-descriptions-item label="最终得分">{{ examRecord?.finalScore || examRecord?.score || '-' }}</el-descriptions-item>
          <el-descriptions-item label="正确题数">{{ examRecord?.correctCount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="错误题数">{{ examRecord?.wrongCount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="考试状态">
            <el-tag :type="examRecord?.status === 'submitted' ? 'success' : 'info'">
              {{ examRecord?.status === 'submitted' ? '已完成' : '进行中' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-button type="primary" link @click="viewExamDetail" v-if="examRecord">
          查看答卷详情
        </el-button>
      </div>

      <!-- 非C级考核：显示文档信息 -->
      <div v-else-if="currentActivity" class="document-info-section">
        <el-alert type="info" :closable="false" class="mb-3">
          <template #title>
            <span>该考核为文档形式，请先查看教师上传的作业文档</span>
          </template>
        </el-alert>

        <el-descriptions :column="2" border class="mb-3">
          <el-descriptions-item label="文档标题">{{ document?.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="文件名">{{ document?.fileName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ document?.createdAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ formatFileSize(document?.fileSize) }}</el-descriptions-item>
        </el-descriptions>

        <el-button type="primary" @click="downloadDocument" v-if="document">
          下载文档
        </el-button>
        <el-button type="primary" link @click="viewDocument" v-if="document">
          在线预览
        </el-button>
        <span v-if="!document" class="text-muted">教师尚未上传文档</span>
      </div>

      <!-- C级考核无需评分表单 -->
      <template v-if="currentActivity?.level !== 'C'">
        <el-divider />

        <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
          <el-form-item label="教师姓名">
            <el-input :value="teacherName" disabled />
          </el-form-item>

          <el-form-item label="考核活动" prop="activityId">
            <el-select v-model="form.activityId" placeholder="请选择考核活动" @change="onActivityChange">
              <el-option v-for="a in activities" :key="a.id" :label="`${a.name} (${a.level}级)`" :value="a.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="评分" prop="score">
            <el-input-number v-model="form.score" :min="0" :max="100" :precision="1" />
            <span style="margin-left: 10px;">分 (0-100)</span>
          </el-form-item>

          <el-form-item label="评语" prop="comment">
            <el-input v-model="form.comment" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="请输入评语" />
          </el-form-item>

          <el-form-item>
            <el-alert v-if="scoresPublished" type="warning" :closable="false" style="margin-bottom: 12px; width: 100%;">
              该活动成绩已发布，不可再修改评分。
            </el-alert>
            <el-button type="primary" :loading="loading" @click="handleSubmit" :disabled="scoresPublished">提交评分</el-button>
          </el-form-item>
        </el-form>

        <el-divider />

        <h3>历史评分</h3>
        <el-table :data="historyData" stripe>
          <el-table-column prop="activityName" label="活动" />
          <el-table-column prop="score" label="评分" />
          <el-table-column prop="comment" label="评语" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="评分时间" />
        </el-table>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { submitEvaluation, getEvaluationList } from '@/api/evaluation'
import { getActivityList, getActivityById } from '@/api/activity'
import { getTeachers } from '@/api/user'
import { getExamRecordsByActivity } from '@/api/exam'
import { getTeacherDocuments } from '@/api/document'

const route = useRoute()
const router = useRouter()
const teacherId = computed(() => Number(route.params.teacherId))
const activityId = computed(() => route.query.activityId ? Number(route.query.activityId) : null)
const teacherName = ref('')
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  activityId: null as number | null,
  score: 0,
  comment: ''
})

const rules = {
  activityId: [{ required: true, message: '请选择考核活动', trigger: 'change' }],
  score: [{ required: true, message: '请输入评分', trigger: 'blur' }],
  comment: [{ max: 2000, message: '评语不能超过2000字', trigger: 'blur' }]
}

const activities = ref<any[]>([])
const historyData = ref<any[]>([])
const currentActivity = ref<any>(null)
const examRecord = ref<any>(null)
const document = ref<any>(null)
const scoresPublished = ref(false)

const currentActivityId = computed(() => form.activityId || activityId.value)

const currentActivityLevel = computed(() => currentActivity.value?.level)

const onActivityChange = async () => {
  const activity = activities.value.find(a => a.id === form.activityId)
  currentActivity.value = activity
  if (activity) {
    if (activity.level === 'C') {
      await loadExamRecord()
    } else {
      await loadDocument()
    }
  }
}

const loadExamRecord = async () => {
  if (!currentActivityId.value) return
  const res = await getExamRecordsByActivity(currentActivityId.value, 1, 20)
  if (res.code === 200) {
    const record = res.data.records.find((r: any) => r.teacherId === teacherId.value)
    if (record) {
      examRecord.value = record
      // C级考核默认使用考试得分
      form.score = record.finalScore || record.score || 0
    }
  }
}

const loadDocument = async () => {
  if (!currentActivityId.value) return
  const res = await getTeacherDocuments(teacherId.value, currentActivityId.value)
  if (res.code === 200 && res.data.records.length > 0) {
    document.value = res.data.records[0]
  } else {
    document.value = null
  }
}

const viewExamDetail = () => {
  router.push(`/evaluator/exam-records/${teacherId.value}?activityId=${currentActivityId.value}`)
}

const viewDocument = () => {
  if (document.value) {
    router.push(`/evaluator/documents/${teacherId.value}?activityId=${currentActivityId.value}`)
  }
}

const downloadDocument = () => {
  if (document.value?.fileUrl) {
    window.open(document.value.fileUrl, '_blank')
  }
}

const formatFileSize = (bytes: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const loadActivities = async () => {
  const res = await getActivityList()
  if (res.code === 200) {
    activities.value = res.data
    if (activityId.value) {
      form.activityId = activityId.value
      // 设置当前活动并加载相关信息
      const activity = activities.value.find(a => a.id === activityId.value)
      currentActivity.value = activity
      if (activity) {
        // 检查成绩是否已发布
        try {
          const detailRes = await getActivityById(activity.id)
          if (detailRes.code === 200) {
            scoresPublished.value = detailRes.data.scoresPublished || false
          }
        } catch (e) {
          scoresPublished.value = false
        }
        if (activity.level === 'C') {
          await loadExamRecord()
        } else {
          await loadDocument()
        }
        // 加载当前考核员对该教师的已有评分（避免重置为默认值）
        await loadMyScore()
      }
    }
  }
}

const loadMyScore = async () => {
  if (!currentActivityId.value || !teacherId.value) return
  try {
    const res = await getEvaluationList({
      activityId: currentActivityId.value,
      teacherId: teacherId.value,
      page: 1,
      size: 100
    })
    if (res.code === 200 && res.data?.records) {
      const myId = userStore.user?.id
      const myEval = res.data.records.find((e: any) => e.evaluatorId === myId)
      if (myEval && myEval.score !== undefined && myEval.score !== null) {
        form.score = myEval.score
      }
    }
  } catch (e) {
    // 忽略加载失败，使用默认值
  }
}

const loadTeacherName = async () => {
  const res = await getTeachers()
  if (res.code === 200) {
    const teacher = res.data.find((t: any) => t.id === teacherId.value)
    if (teacher) {
      teacherName.value = teacher.realName
    }
  }
}

const loadHistory = async () => {
  const res = await getEvaluationList({
    activityId: undefined,
    teacherId: teacherId.value
  })
  if (res.code === 200) {
    historyData.value = res.data.records
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await submitEvaluation({
      teacherId: teacherId.value,
      activityId: form.activityId!,
      score: form.score,
      comment: form.comment
    })
    if (res.code === 200) {
      ElMessage.success('提交成功')
      router.push({ path: '/evaluator/activities', query: { activityId: form.activityId } })
    }
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadActivities()
  loadTeacherName()
  loadHistory()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.evaluation-form {
  max-width: 900px;
}

.mb-3 {
  margin-bottom: 16px;
}

.text-muted {
  color: #909399;
  font-size: 14px;
}
</style>