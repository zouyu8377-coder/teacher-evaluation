<template>
  <div class="activity-detail">
    <!-- 返回按钮 -->
    <div class="page-header">
      <router-link to="/teacher/enrollment" class="back-link">
        <span class="material-symbols-outlined">arrow_back</span>
        返回我的考核
      </router-link>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 活动详情 -->
    <template v-else-if="activity">
      <!-- 基本信息卡片 -->
      <div class="info-card">
        <div class="activity-header">
          <div class="activity-level" :class="'level-' + activity.level">
            {{ activity.level }}级
          </div>
          <div class="activity-title">
            <h1>{{ activity.name }}</h1>
            <span class="activity-status" :class="'status-' + activityStatus">
              {{ statusText }}
            </span>
          </div>
        </div>
        <p class="activity-desc">{{ activity.description || '暂无描述' }}</p>

        <div class="info-grid">
          <div class="info-item">
            <span class="label">考核类型</span>
            <span class="value">
              <el-tag v-if="activity.hasExam" type="warning">需要考试</el-tag>
              <el-tag v-else type="success">需要上传文档</el-tag>
            </span>
          </div>
          <div class="info-item" v-if="activity.level === 'C'">
            <span class="label">考试时间</span>
            <span class="value">{{ formatDateTime(activity.examStart) }} ~ {{ formatDateTime(activity.examEnd) }}</span>
          </div>
          <div class="info-item" v-else>
            <span class="label">活动时间</span>
            <span class="value">{{ formatDateTime(activity.materialStart) }} ~ {{ formatDateTime(activity.materialEnd) }}</span>
          </div>
          <div class="info-item">
            <span class="label">报名时间</span>
            <span class="value">{{ formatDateTime(enrollmentInfo?.enrollmentStart) }} ~ {{ formatDateTime(enrollmentInfo?.enrollmentEnd) }}</span>
          </div>
          <div class="info-item">
            <span class="label">报名状态</span>
            <span class="value">
              <el-tag type="success">已报名</el-tag>
            </span>
          </div>
          <div class="info-item" v-if="activity.location">
            <span class="label">考核地点</span>
            <span class="value">{{ activity.location }}</span>
          </div>
        </div>
      </div>

      <!-- C级：考试区域 -->
      <div v-if="activity.level === 'C'" class="action-card">
        <h3 class="section-title">
          <span class="material-symbols-outlined">fact_check</span>
          考试环节
        </h3>

        <template v-if="!examRecord">
          <div class="action-prompt">
            <p class="prompt-text">{{ examWindowPrompt }}</p>
            <el-button type="primary" size="large" :disabled="!canStartOrContinueExam" @click="startExam">
              <span class="material-symbols-outlined" style="font-size: 18px; margin-right: 8px;">play_arrow</span>
              开始考试
            </el-button>
          </div>
        </template>

        <template v-else-if="examRecord.status === 'in_progress'">
          <div class="action-prompt">
            <p class="prompt-text">{{ canStartOrContinueExam ? '您有正在进行的考试' : examWindowPrompt }}</p>
            <el-button type="warning" size="large" :disabled="!canStartOrContinueExam" @click="continueExam">
              <span class="material-symbols-outlined" style="font-size: 18px; margin-right: 8px;">play_arrow</span>
              继续考试
            </el-button>
          </div>
        </template>

        <template v-else>
          <div class="exam-completed">
            <div class="exam-status">
              <span class="material-symbols-outlined" :style="{ color: scorePublished ? '#16a34a' : '#d97706' }">
                {{ scorePublished ? 'check_circle' : 'hourglass_empty' }}
              </span>
              <span>{{ scorePublished ? '考核已完成' : '考试已完成，等待成绩发布' }}</span>
            </div>
            <div class="exam-info">
              <div class="info-row">
                <span class="label">交卷时间</span>
                <span class="value">{{ formatDateTime(examRecord.submittedAt) }}</span>
              </div>
              <div class="info-row">
                <span class="label">考试得分</span>
                <span class="value exam-score">{{ examRecord.score }}分</span>
              </div>
              <div v-if="scorePublished" class="info-row">
                <span class="label">最终得分</span>
                <span class="value exam-score">{{ enrollment?.finalScore }}分</span>
              </div>
            </div>
            <div style="margin-top: 16px; text-align: center;">
              <el-button type="primary" @click="viewExamResult">
                <span class="material-symbols-outlined" style="font-size: 18px; margin-right: 8px;">visibility</span>
                成绩查询
              </el-button>
            </div>
          </div>
        </template>
      </div>

      <!-- 非C级：文档上传区域 -->
      <div v-else class="action-card">
        <h3 class="section-title">
          <span class="material-symbols-outlined">description</span>
          考核文档
        </h3>

        <template v-if="!myDocument">
          <div class="action-prompt">
            <p class="prompt-text">请在活动截止前上传您的作品材料（支持 doc, docx, pdf, txt 格式）</p>
            <el-button type="primary" size="large" :disabled="!canUploadDocument" @click="showUploadDialog = true">
              <span class="material-symbols-outlined" style="font-size: 18px; margin-right: 8px;">upload</span>
              上传文档
            </el-button>
          </div>
        </template>

        <template v-else-if="!scorePublished">
          <div class="document-uploaded">
            <div class="document-info">
              <span class="material-symbols-outlined doc-icon">description</span>
              <div class="doc-details">
                <h4>{{ myDocument.title }}</h4>
                <p>{{ myDocument.fileName }} · {{ formatFileSize(myDocument.fileSize) }}</p>
              </div>
            </div>
            <div class="document-actions">
              <el-button type="primary" link @click="downloadDocument(myDocument)">下载</el-button>
              <el-button v-if="canUploadDocument" type="warning" link @click="showUploadDialog = true">重新上传</el-button>
            </div>
          </div>
          <div class="waiting-score">
            <span class="material-symbols-outlined">hourglass_empty</span>
            <span>等待考核员评分中...</span>
          </div>
        </template>

        <template v-else-if="scorePublished">
          <div class="score-card">
            <div class="score-header">
              <span class="score-label">最终得分</span>
              <span class="score-value" :class="getScoreClass(enrollment?.finalScore, enrollment?.isPassed)">
                {{ enrollment?.finalScore }}
              </span>
            </div>
            <div class="score-detail">
              <div class="detail-item">
                <span class="label">上传文档</span>
                <span class="value">
                  <el-button type="primary" link @click="downloadDocument(myDocument)">
                    {{ myDocument.fileName }}
                  </el-button>
                </span>
              </div>
              <div v-if="enrollment?.comment" class="detail-item full-width">
                <span class="label">评语</span>
                <span class="value">{{ enrollment.comment }}</span>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- 学习资料 -->
      <div class="materials-card">
        <h3 class="section-title">
          <span class="material-symbols-outlined">menu_book</span>
          学习资料
        </h3>
        <el-table :data="materials" stripe v-loading="materialsLoading">
          <el-table-column prop="title" label="资料标题" />
          <el-table-column prop="fileName" label="文件名" show-overflow-tooltip />
          <el-table-column prop="fileSize" label="大小" width="100">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="上传时间" width="180" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="downloadMaterial(row)">下载</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="materials.length === 0 && !materialsLoading" description="暂无学习资料" :image-size="60" />
      </div>
    </template>

    <!-- 未找到 -->
    <div v-else class="empty-state">
      <span class="material-symbols-outlined">search_off</span>
      <p>未找到活动信息</p>
    </div>

    <!-- 上传文档对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传考核文档" width="500px">
      <el-form :model="uploadForm" ref="uploadFormRef" label-width="80px">
        <el-form-item label="标题" prop="title" :rules="[{ required: true, message: '请输入文档标题', trigger: 'blur' }]">
          <el-input v-model="uploadForm.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="文件" prop="file" :rules="[{ required: true, message: '请选择文件', trigger: 'change' }]">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            accept=".doc,.docx,.pdf,.txt"
          >
            <el-button>选择文件</el-button>
            <template #tip>
              <div class="upload-tip">支持 doc, docx, pdf, txt 格式，最大 100MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="uploadForm.description" type="textarea" :rows="3" placeholder="请输入文档描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivityById, getEnrollmentInfo } from '@/api/activity'
import { getMaterialList, downloadMaterial as downloadMaterialApi } from '@/api/learningMaterial'
import { getDocumentList, uploadDocument, downloadDocument as downloadDocApi } from '@/api/document'
import { getMyExamRecords } from '@/api/exam'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const activity = ref<any>(null)
const enrollment = ref<any>(null)
const enrollmentInfo = ref<any>(null)
const materials = ref<any[]>([])
const materialsLoading = ref(false)
const myDocument = ref<any>(null)
const examRecord = ref<any>(null)

// 上传相关
const showUploadDialog = ref(false)
const uploading = ref(false)
const uploadForm = ref({ title: '', description: '' })
const uploadFormRef = ref()
const uploadRef = ref()
const fileList = ref<any[]>([])
const selectedFile = ref<File | null>(null)

// 计算活动状态
const activityStatus = computed(() => {
  if (!enrollment.value) return 'unknown'

  // 已发布成绩
  if (enrollment.value.scorePublished && enrollment.value.finalScore !== undefined) {
    return 'completed'
  }

  // C级 - 考试
  if (activity.value?.level === 'C') {
    if (!examRecord.value) return 'pending-exam'
    if (examRecord.value.status === 'in_progress') return 'exam-in-progress'
    return 'exam-completed'
  }

  // 非C级 - 文档
  if (myDocument.value) return 'pending-score'
  return 'pending-upload'
})

const statusText = computed(() => {
  if (enrollment.value?.statusText) {
    return enrollment.value.statusText
  }
  const map: Record<string, string> = {
    'completed': '已完成',
    'exam-completed': '待评分',
    'pending-exam': '待考试',
    'pending-upload': '待上传',
    'pending-score': '待评分'
  }
  return map[activityStatus.value] || ''
})

const scorePublished = computed(() => {
  return enrollment.value?.scorePublished && enrollment.value?.finalScore !== undefined
})

const examWindowState = computed(() => {
  const now = new Date()
  const start = activity.value?.examStart ? new Date(activity.value.examStart) : null
  const end = activity.value?.examEnd ? new Date(activity.value.examEnd) : null
  if (!start || !end) return 'not_configured'
  if (start && now < start) return 'pending'
  if (end && now > end) return 'ended'
  return 'open'
})

const isExamWindowOpen = computed(() => examWindowState.value === 'open')
const canStartOrContinueExam = computed(() => {
  const actions = enrollment.value?.availableActions
  if (actions) {
    return actions.includes('start_exam') || actions.includes('continue_exam')
  }
  return isExamWindowOpen.value
})
const canUploadDocument = computed(() => {
  const actions = enrollment.value?.availableActions
  if (actions) {
    return actions.includes('upload_document') || actions.includes('replace_document')
  }
  if (!activity.value || activity.value.level === 'C') return false
  const now = new Date()
  const start = activity.value.materialStart ? new Date(activity.value.materialStart) : null
  const end = activity.value.materialEnd ? new Date(activity.value.materialEnd) : null
  if (start && now < start) return false
  if (end && now > end) return false
  return true
})

const examWindowPrompt = computed(() => {
  if (examWindowState.value === 'pending') return '考试尚未开始，请在考试时间内参加'
  if (examWindowState.value === 'ended') return '考试时间已结束'
  return '您需要参加在线考试来完成本次考核'
})

const formatDateTime = (datetime: string | null | undefined) => {
  if (!datetime) return '未设置'
  return datetime.slice(0, 16).replace('T', ' ')
}

const formatFileSize = (bytes: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

const getScoreClass = (score: number, isPassed?: boolean | null) => {
  if (isPassed === true) return 'score-high'
  if (isPassed === false) return 'score-low'
  if (score >= 90) return 'score-high'
  if (score >= 60) return 'score-mid'
  return 'score-low'
}

const loadData = async () => {
  const activityId = route.params.id as string
  loading.value = true

  try {
    // 获取活动详情
    const activityRes = await getActivityById(Number(activityId))
    if (activityRes.code === 200) {
      activity.value = activityRes.data
    }

    // 获取报名信息
    const enrollRes = await getEnrollmentInfo(parseInt(activityId))
    if (enrollRes.code === 200) {
      enrollmentInfo.value = enrollRes.data
      // 从 enrollmentInfo 中获取更多报名详情
      enrollment.value = enrollRes.data
    }

    // 获取学习资料
    loadMaterials(parseInt(activityId))

    // 获取我的文档
    loadMyDocument(parseInt(activityId))

    // 获取我的考试记录
    const examRes = await getMyExamRecords()
    if (examRes.code === 200) {
      const records = examRes.data || []
      examRecord.value = records.find((r: any) => r.activityId === parseInt(activityId)) || null
    }

  } catch (e) {
    console.error('获取活动信息失败', e)
    ElMessage.error('获取活动信息失败')
  } finally {
    loading.value = false
  }
}

const loadMaterials = async (activityId: number) => {
  materialsLoading.value = true
  try {
    const res = await getMaterialList({ activityId, size: 100 })
    if (res.code === 200) {
      materials.value = res.data?.records || []
    }
  } catch (e) {
    console.error('获取学习资料失败', e)
  } finally {
    materialsLoading.value = false
  }
}

const loadMyDocument = async (activityId: number) => {
  try {
    const userStore = await import('@/stores/user')
    const userId = userStore.useUserStore().user?.id

    // 查询该活动下的文档
    const res = await getDocumentList({ activityId, size: 10 })
    if (res.code === 200 && res.data?.records?.length > 0) {
      myDocument.value = res.data.records[0]
    }
  } catch (e) {
    console.error('获取文档失败', e)
  }
}

const startExam = () => {
  if (!canStartOrContinueExam.value) {
    ElMessage.warning(enrollment.value?.statusText || examWindowPrompt.value)
    return
  }
  router.push({ path: '/teacher/exam', query: { activityId: route.params.id } })
}

const continueExam = () => {
  if (!canStartOrContinueExam.value) {
    ElMessage.warning(enrollment.value?.statusText || examWindowPrompt.value)
    return
  }
  router.push({ path: '/teacher/exam', query: { activityId: route.params.id, recordId: examRecord.value.id } })
}

const viewExamResult = () => {
  router.push({ path: '/teacher/exam', query: { recordId: examRecord.value.id } })
}

const downloadDocument = async (doc: any) => {
  try {
    const res = await downloadDocApi(doc.id)
    const url = window.URL.createObjectURL(new Blob([res as unknown as BlobPart]))
    const link = document.createElement('a')
    link.href = url
    link.download = doc.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

const downloadMaterial = async (row: any) => {
  try {
    const res = await downloadMaterialApi(row.id)
    const url = window.URL.createObjectURL(new Blob([res as unknown as BlobPart]))
    const link = document.createElement('a')
    link.href = url
    link.download = row.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

const handleFileChange = (file: any) => {
  selectedFile.value = file.raw
}

const handleFileRemove = () => {
  selectedFile.value = null
}

const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  if (!uploadForm.value.title) {
    ElMessage.warning('请输入标题')
    return
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('activityId', route.params.id as string)
    formData.append('title', uploadForm.value.title)
    if (uploadForm.value.description) {
      formData.append('description', uploadForm.value.description)
    }

    const res = await uploadDocument(formData)
    if (res.code === 200) {
      ElMessage.success('上传成功')
      showUploadDialog.value = false
      uploadForm.value = { title: '', description: '' }
      selectedFile.value = null
      fileList.value = []
      // 刷新文档列表
      loadMyDocument(parseInt(route.params.id as string))
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.activity-detail {
  max-width: 1000px;
}

.page-header {
  margin-bottom: 24px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.2s ease;
}

.back-link:hover {
  color: #8E2DE2;
}

.loading-state {
  background: #fff;
  border-radius: 20px;
  padding: 32px;
}

.info-card,
.action-card,
.materials-card {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
}

.activity-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
}

.activity-level {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.5rem;
  flex-shrink: 0;
}

.activity-level.level-C { background: #e0e7ff; color: #4f46e5; }
.activity-level.level-B2 { background: #dbeafe; color: #2563eb; }
.activity-level.level-B1 { background: #fed7aa; color: #ea580c; }
.activity-level.level-A2 { background: #fce7f3; color: #db2777; }
.activity-level.level-A1 { background: #fee2e2; color: #dc2626; }

.activity-title {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.activity-title h1 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
}

.activity-status {
  padding: 6px 16px;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.activity-status.status-completed { background: #dcfce7; color: #16a34a; }
.activity-status.status-exam-completed { background: #fef3c7; color: #d97706; }
.activity-status.status-pending-exam { background: #e0e7ff; color: #4f46e5; }
.activity-status.status-pending-upload { background: #fef3c7; color: #d97706; }
.activity-status.status-pending-score { background: #f1f5f9; color: #64748b; }

.activity-desc {
  color: #64748b;
  margin: 0 0 20px 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item .label {
  font-size: 0.75rem;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-item .value {
  font-size: 0.875rem;
  color: #1e293b;
  font-weight: 500;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 20px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title .material-symbols-outlined {
  color: #8E2DE2;
}

.action-prompt {
  text-align: center;
  padding: 32px 0;
}

.prompt-text {
  color: #64748b;
  margin-bottom: 20px;
  font-size: 1rem;
}

.exam-completed,
.document-uploaded {
  background: #f8fafc;
  border-radius: 12px;
  padding: 20px;
}

.exam-status,
.waiting-score {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #d97706;
  margin-bottom: 16px;
}

.exam-info {
  display: flex;
  gap: 32px;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-row .label {
  font-size: 0.75rem;
  color: #94a3b8;
}

.info-row .value {
  font-weight: 600;
  color: #1e293b;
}

.exam-score {
  font-size: 1.25rem;
  color: #4f46e5;
}

.document-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.doc-icon {
  font-size: 40px;
  color: #4f46e5;
}

.doc-details h4 {
  margin: 0;
  font-size: 1rem;
  color: #1e293b;
}

.doc-details p {
  margin: 4px 0 0;
  font-size: 0.875rem;
  color: #64748b;
}

.document-actions {
  margin-top: 16px;
}

.waiting-score {
  margin-top: 16px;
  color: #64748b;
}

.score-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #bae6fd;
}

.score-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.score-label {
  font-size: 0.875rem;
  color: #0369a1;
}

.score-value {
  font-size: 2rem;
  font-weight: 800;
}

.score-high { color: #16a34a; }
.score-mid { color: #d97706; }
.score-low { color: #dc2626; }

.score-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.detail-item .label {
  width: 80px;
  color: #64748b;
  font-size: 0.875rem;
}

.detail-item .value {
  color: #1e293b;
  font-weight: 500;
}

.detail-item.full-width {
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.detail-item.full-width .label {
  width: auto;
}

.detail-item.full-width .value {
  line-height: 1.6;
}

.empty-state {
  text-align: center;
  padding: 80px 40px;
  color: #94a3b8;
  background: #fff;
  border-radius: 20px;
}

.empty-state .material-symbols-outlined {
  font-size: 64px;
  margin-bottom: 12px;
}

.upload-tip {
  font-size: 0.75rem;
  color: #999;
  margin-top: 8px;
}
</style>
