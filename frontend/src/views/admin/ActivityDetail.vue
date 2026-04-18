<template>
  <div class="activity-detail">
    <div class="page-header">
      <router-link to="/admin/activities" class="back-link">
        <span class="material-symbols-outlined">arrow_back</span>
        返回活动列表
      </router-link>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="10" animated />
    </div>

    <template v-else-if="activity">
      <!-- 基本信息卡片 -->
      <div class="info-card">
        <div class="activity-header">
          <div class="activity-level" :class="'level-' + activity.level">
            {{ activity.level }}
          </div>
          <div class="activity-title">
            <h1>{{ activity.name }}</h1>
            <span class="activity-status" :class="'status-' + activity.status">
              {{ getStatusText(activity.status) }}
            </span>
          </div>
          <div class="activity-actions">
            <el-switch
              v-model="activity.status"
              :active-value="'active'"
              :inactive-value="'closed'"
              active-text="进行中"
              inactive-text="已关闭"
              @change="handleToggleStatus"
            />
          </div>
        </div>
        <p class="activity-desc">{{ activity.description || '暂无描述' }}</p>
      </div>

      <!-- 统计信息 -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon purple">
            <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">group</span>
          </div>
          <div class="stat-info">
            <p class="stat-label">已报名人数</p>
            <h3 class="stat-value">{{ activity.enrolledCount || 0 }}</h3>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon blue">
            <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">event_available</span>
          </div>
          <div class="stat-info">
            <p class="stat-label">总名额</p>
            <h3 class="stat-value">{{ activity.maxParticipants || '不限' }}</h3>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon green">
            <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">how_to_reg</span>
          </div>
          <div class="stat-info">
            <p class="stat-label">评分人数</p>
            <h3 class="stat-value">{{ activity.reviewerCount || 0 }}</h3>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon pink">
            <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">inventory_2</span>
          </div>
          <div class="stat-info">
            <p class="stat-label">剩余名额</p>
            <h3 class="stat-value">{{ getRemaining() }}</h3>
          </div>
        </div>
      </div>

      <!-- 时间地点信息 -->
      <div class="time-card">
        <div class="section-header">
          <h3 class="section-title">
            <span class="material-symbols-outlined">schedule</span>
            时间地点安排
          </h3>
          <el-button type="primary" size="small" @click="openEditDialog">
            <span class="material-symbols-outlined" style="font-size: 16px; margin-right: 4px;">edit</span>
            编辑
          </el-button>
        </div>
        <div class="time-list">
          <div class="time-item">
            <span class="time-label">报名时间</span>
            <span class="time-value">{{ formatDateTime(activity.enrollmentStart) }} ~ {{ formatDateTime(activity.enrollmentEnd) }}</span>
          </div>
          <div class="time-item" v-if="activity.level === 'C'">
            <span class="time-label">考试时间</span>
            <span class="time-value">{{ formatDateTime(activity.examStart) }} ~ {{ formatDateTime(activity.examEnd) }}</span>
          </div>
          <div class="time-item" v-else>
            <span class="time-label">上传资料</span>
            <span class="time-value">{{ formatDateTime(activity.materialStart) }} ~ {{ formatDateTime(activity.materialEnd) }}</span>
          </div>
          <div class="time-item" v-if="activity.location">
            <span class="time-label">考核地点</span>
            <span class="time-value">{{ activity.location }}</span>
          </div>
        </div>
      </div>

      <!-- 评分配置 -->
      <div class="reviewer-card">
        <div class="section-header">
          <h3 class="section-title">
            <span class="material-symbols-outlined">group</span>
            评分配置
          </h3>
          <el-button type="primary" size="small" @click="openReviewerDialog">
            <span class="material-symbols-outlined" style="font-size: 16px; margin-right: 4px;">settings</span>
            配置
          </el-button>
        </div>
        <div class="reviewer-info">
          <el-tag type="info">参与教师：{{ reviewProgress.enrolledCount || 0 }} 人</el-tag>
          <el-tag type="info" style="margin-left: 8px;">评分人数：{{ reviewProgress.reviewerCount || 0 }} 人</el-tag>
          <span v-if="selectedReviewerNames.length > 0" style="margin-left: 12px; color: #64748b;">
            {{ selectedReviewerNames.join('、') }}
          </span>
        </div>
        <!-- 评分进度 -->
        <div v-if="reviewProgress.reviewerStats && reviewProgress.reviewerStats.length > 0" class="reviewer-stats">
          <el-divider />
          <div class="progress-item" v-for="stat in reviewProgress.reviewerStats" :key="stat.id">
            <span class="reviewer-name" style="display: inline-block; width: 80px;">{{ stat.realName }}</span>
            <el-progress
              :percentage="stat.totalRequired > 0 ? Math.round(stat.completedCount / stat.totalRequired * 100) : 0"
              :stroke-width="10"
              :format="() => stat.completedCount + '/' + stat.totalRequired"
              style="width: 180px; display: inline-block; vertical-align: middle; margin-right: 12px;">
            </el-progress>
            <el-tag :type="stat.completedCount >= stat.totalRequired ? 'success' : 'warning'" size="small">
              {{ stat.completedCount >= stat.totalRequired ? '已完成' : '进行中' }}
            </el-tag>
          </div>
          <el-divider />
          <div class="review-status">
            <el-tag v-if="reviewProgress.reviewStatus === '未配置'" type="info">待配置评分人</el-tag>
            <el-tag v-else-if="reviewProgress.reviewStatus === '待评分'" type="warning">待评分</el-tag>
            <el-tag v-else-if="reviewProgress.reviewStatus === '评分中'" type="warning">评分中</el-tag>
            <el-tag v-else-if="reviewProgress.reviewStatus === '评分完成' && !reviewProgress.scoresPublished" type="success">评分完成</el-tag>
            <el-tag v-else-if="reviewProgress.scoresPublished" type="success">成绩已发布</el-tag>
          </div>
          <!-- 评分完成且成绩未发布时显示发布按钮 -->
          <div v-if="reviewProgress.reviewStatus === '评分完成' && !reviewProgress.scoresPublished" style="margin-top: 12px;">
            <el-button type="primary" @click="handlePublishScores">发布成绩</el-button>
          </div>
        </div>
      </div>

      <!-- 试卷配置 (仅C级考核显示) -->
      <div v-if="activity.level === 'C'" class="config-card">
        <h3 class="section-title">
          <span class="material-symbols-outlined">quiz</span>
          试卷配置
        </h3>
        <el-form label-width="100px">
          <el-form-item label="选择试卷">
            <el-select v-model="selectedPaperId" placeholder="请选择试卷" @change="handlePaperChange" style="width: 300px;">
              <el-option label="不设置试卷" :value="null" />
              <el-option v-for="paper in papers" :key="paper.id" :label="paper.name" :value="paper.id">
                <span>{{ paper.name }}</span>
                <span style="color: #999; font-size: 12px; margin-left: 8px;">({{ paper.questionCount }}题/{{ paper.totalScore }}分)</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item v-if="selectedPaperInfo" label="试卷信息">
            <div class="paper-info">
              <el-tag type="success">已选择：{{ selectedPaperInfo.name }}</el-tag>
              <span style="margin-left: 12px; color: #64748b;">
                共{{ selectedPaperInfo.questionCount }}题，总分{{ selectedPaperInfo.totalScore }}分
              </span>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 学习资料 -->
      <div class="materials-card">
        <div class="section-header">
          <h3 class="section-title">
            <span class="material-symbols-outlined">menu_book</span>
            学习资料
          </h3>
          <el-button type="primary" size="small" @click="showUploadDialog = true">
            <span class="material-symbols-outlined" style="font-size: 16px; margin-right: 4px;">upload</span>
            上传资料
          </el-button>
        </div>
        <el-table :data="materials" stripe v-loading="materialsLoading">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="fileName" label="文件名" show-overflow-tooltip />
          <el-table-column prop="fileSize" label="大小" width="100">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="creatorName" label="上传人" width="100" />
          <el-table-column prop="createdAt" label="上传时间" width="180">
            <template #default="{ row }">
              {{ row.createdAt }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button type="primary" link @click="downloadMaterial(row)">下载</el-button>
              <el-button type="danger" link @click="handleDeleteMaterial(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="materials.length === 0 && !materialsLoading" description="暂无学习资料" :image-size="60" />
      </div>

      <!-- 已报名教师列表 -->
      <div class="enrolled-card">
        <h3 class="section-title">
          <span class="material-symbols-outlined">people</span>
          已报名教师
        </h3>
        <el-table :data="enrolledTeachers" stripe v-loading="enrolledLoading">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="realName" label="姓名" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="enrolledAt" label="报名时间">
            <template #default="{ row }">
              {{ formatDateTime(row.enrolledAt) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <div v-else class="empty-state">
      <span class="material-symbols-outlined">search_off</span>
      <p>未找到活动信息</p>
    </div>

    <!-- 上传资料对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传学习资料" width="500px">
      <el-form :model="uploadForm" ref="uploadFormRef" label-width="80px">
        <el-form-item label="标题" prop="title" :rules="[{ required: true, message: '请输入标题', trigger: 'blur' }]">
          <el-input v-model="uploadForm.title" placeholder="请输入资料标题" />
        </el-form-item>
        <el-form-item label="文件" prop="file" :rules="[{ required: true, message: '请选择文件', trigger: 'change' }]">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.zip,.rar"
          >
            <el-button>选择文件</el-button>
            <template #tip>
              <div class="upload-tip">支持 PDF、Word、PPT、Excel、TXT、ZIP 格式</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="uploadForm.description" type="textarea" :rows="3" placeholder="请输入资料描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 编辑活动对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑活动" width="650px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="editForm.name" placeholder="如：2024学年第一学期C级考核" />
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-select v-model="editForm.level" disabled placeholder="请选择">
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
              <el-date-picker v-model="editForm.enrollmentStart" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报名截止" prop="enrollmentEnd">
              <el-date-picker v-model="editForm.enrollmentEnd" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- C级：考试时间 -->
        <template v-if="editForm.level === 'C'">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="考试开始" prop="examStart">
                <el-date-picker v-model="editForm.examStart" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="考试时长" prop="examDurationMinutes">
                <el-input-number v-model="editForm.examDurationMinutes" :min="10" :max="480" />
                <span style="margin-left: 10px;">分钟</span>
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        <!-- 非C级：材料上传时间 -->
        <template v-else>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="上传开始" prop="materialStart">
                <el-date-picker v-model="editForm.materialStart" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="上传截止" prop="materialEnd">
                <el-date-picker v-model="editForm.materialEnd" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        <el-form-item label="总名额" prop="maxParticipants">
          <el-input-number v-model="editForm.maxParticipants" :min="0" :max="10000" placeholder="0表示不限制" />
          <span style="margin-left: 10px; color: #999;">0表示不限制</span>
        </el-form-item>
        <el-form-item label="考核地点">
          <el-input v-model="editForm.location" placeholder="非必填" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEditSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 评分人配置对话框 -->
    <el-dialog v-model="showReviewerDialog" title="评分人配置" width="500px">
      <el-alert type="info" :closable="false" class="mb-3">
        勾选评分人后，评分人数将自动同步为勾选的人数
      </el-alert>
      <el-form label-width="100px">
        <el-form-item label="评分人数">
          <el-input :value="reviewerForm.selectedReviewers.length" disabled />
          <span style="margin-left: 10px; color: #999;">人（由勾选的评分人自动计算）</span>
        </el-form-item>
        <el-form-item label="选择评分人">
          <el-checkbox-group v-model="reviewerForm.selectedReviewers">
            <el-checkbox v-for="e in evaluators" :key="e.id" :value="e.id">
              {{ e.realName }} ({{ e.department }})
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReviewerDialog = false">取消</el-button>
        <el-button type="primary" @click="handleReviewerSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getActivityById, getActivityEnrollments, updateActivity, updateReviewerConfig, getReviewProgress } from '@/api/activity'
import { publishEvaluationScores } from '@/api/evaluation'
import { getEvaluators } from '@/api/user'
import { getPapersByPeriod } from '@/api/exam'
import { getMaterialList, uploadMaterial, deleteMaterial, downloadMaterial as downloadApi } from '@/api/learningMaterial'

const route = useRoute()
const loading = ref(true)
const activity = ref<any>(null)
const enrolledTeachers = ref<any[]>([])
const enrolledLoading = ref(false)

// 试卷相关
const papers = ref<any[]>([])
const selectedPaperId = ref<number | null>(null)

const selectedPaperInfo = computed(() => {
  if (!selectedPaperId.value) return null
  return papers.value.find(p => p.id === selectedPaperId.value)
})

// 根据 reviewerIds 获取已选评分人名字
const selectedReviewerNames = computed(() => {
  if (!activity.value?.reviewerIds) return []
  try {
    const ids = JSON.parse(activity.value.reviewerIds)
    return ids.map((id: number) => {
      const evaluator = evaluators.value.find(e => e.id === id)
      return evaluator ? evaluator.realName : ''
    }).filter((name: string) => name)
  } catch {
    return []
  }
})

// 学习资料相关
const materials = ref<any[]>([])
const materialsLoading = ref(false)
const showUploadDialog = ref(false)
const uploading = ref(false)
const uploadForm = ref({
  title: '',
  description: ''
})
const uploadFormRef = ref()
const uploadRef = ref()
const fileList = ref<any[]>([])
const selectedFile = ref<File | null>(null)

// 编辑活动相关
const showEditDialog = ref(false)
const editLoading = ref(false)
const editFormRef = ref()
const editForm = ref({
  name: '',
  level: '',
  maxParticipants: 0,
  enrollmentStart: '',
  enrollmentEnd: '',
  examStart: '',
  examDurationMinutes: 60,
  materialStart: '',
  materialEnd: '',
  location: '',
  description: ''
})
const editRules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择级别', trigger: 'change' }],
  enrollmentStart: [{ required: true, message: '请选择报名开始时间', trigger: 'change' }],
  enrollmentEnd: [{ required: true, message: '请选择报名截止时间', trigger: 'change' }],
  examStart: [{ required: true, message: '请选择考试开始时间', trigger: 'change' }],
  examDurationMinutes: [{ required: true, message: '请输入考试时长', trigger: 'blur' }],
  materialStart: [{ required: true, message: '请选择材料上传开始时间', trigger: 'change' }],
  materialEnd: [{ required: true, message: '请选择材料上传截止时间', trigger: 'change' }]
}

// 评分人配置相关
const showReviewerDialog = ref(false)
const evaluators = ref<any[]>([])
const reviewerForm = ref({
  selectedReviewers: [] as number[]
})

// 评分进度相关
const reviewProgress = ref<any>({
  enrolledCount: 0,
  reviewerCount: 0,
  reviewerStats: [],
  totalCompleted: 0,
  totalRequired: 0,
  reviewStatus: '未开始',
  scoresPublished: false
})

const loadReviewProgress = async () => {
  if (!activity.value) return
  try {
    const res = await getReviewProgress(activity.value.id)
    if (res.code === 200) {
      reviewProgress.value = res.data
    }
  } catch (e) {
    console.error('获取评分进度失败', e)
  }
}

const handlePublishScores = async () => {
  if (!activity.value) return
  await ElMessageBox.confirm('确定要公布这次考核中所有已打分的成绩吗？公布后教师可查看。', '提示', { type: 'warning' })
  try {
    const res = await publishEvaluationScores(activity.value.id)
    if (res.code === 200) {
      ElMessage.success('成绩已公布')
      loadData()
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '发布失败')
  }
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    active: '进行中',
    draft: '草稿',
    closed: '已结束'
  }
  return map[status] || '未知'
}

const formatDateTime = (dateStr: string | null | undefined) => {
  if (!dateStr) return '未设置'
  // 只显示到分钟，去掉秒
  return dateStr.slice(0, 16).replace('T', ' ')
}

const getRemaining = () => {
  if (!activity.value?.maxParticipants) return '不限'
  const remaining = activity.value.maxParticipants - (activity.value.enrolledCount || 0)
  return remaining > 0 ? remaining : 0
}

const loadData = async () => {
  const activityId = route.params.id
  loading.value = true
  try {
    const res = await getActivityById(activityId as string)
    if (res.code === 200) {
      activity.value = res.data
      selectedPaperId.value = res.data.examPaperId || null
      // 加载已报名教师
      loadEnrollments(activityId as string)
      // 加载学习资料
      loadMaterials()
      // 加载评分进度
      loadReviewProgress()
      // 如果是C级考核，加载试卷列表
      if (res.data.level === 'C') {
        loadPapers()
      }
    } else {
      ElMessage.error('获取活动信息失败')
    }
  } catch (e) {
    ElMessage.error('获取活动信息失败')
  } finally {
    loading.value = false
  }
}

const loadPapers = async () => {
  try {
    const res = await getPapersByPeriod()
    if (res.code === 200) {
      papers.value = res.data?.content || []
    }
  } catch (e) {
    console.error('获取试卷列表失败', e)
  }
}

const handlePaperChange = async (paperId: number | null) => {
  if (!activity.value) return
  try {
    // 同时设置 hasExam 为 true（如果有试卷）或 false（如果没有试卷）
    const hasExam = paperId !== null
    await updateActivity(activity.value.id, { examPaperId: paperId, hasExam })
    ElMessage.success('试卷配置已保存')
    // 刷新活动数据
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

const loadEvaluators = async () => {
  try {
    const res = await getEvaluators()
    if (res.code === 200) {
      evaluators.value = res.data || []
    }
  } catch (e) {
    console.error('获取评分人列表失败', e)
  }
}

const openEditDialog = () => {
  if (!activity.value) return
  editForm.value = {
    name: activity.value.name,
    level: activity.value.level,
    status: activity.value.status,
    maxParticipants: activity.value.maxParticipants || 0,
    enrollmentStart: activity.value.enrollmentStart,
    enrollmentEnd: activity.value.enrollmentEnd,
    examStart: activity.value.examStart || '',
    examDurationMinutes: activity.value.examDurationMinutes || 60,
    materialStart: activity.value.materialStart || '',
    materialEnd: activity.value.materialEnd || '',
    location: activity.value.location || '',
    description: activity.value.description || ''
  }
  showEditDialog.value = true
}

const handleEditSubmit = async () => {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return

  // 前端校验：考试时间必须在报名截止之后
  if (editForm.value.level === 'C' && editForm.value.enrollmentStart && editForm.value.enrollmentEnd && editForm.value.examStart) {
    const enrollmentEnd = new Date(editForm.value.enrollmentEnd)
    const examStart = new Date(editForm.value.examStart)
    if (examStart <= enrollmentEnd) {
      ElMessage.error('考试时间应在报名结束之后')
      return
    }
  }

  editLoading.value = true
  try {
    const res = await updateActivity(activity.value.id, editForm.value)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      showEditDialog.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    editLoading.value = false
  }
}

const openReviewerDialog = () => {
  if (!activity.value) return
  reviewerForm.value.selectedReviewers = activity.value.reviewerIds ? JSON.parse(activity.value.reviewerIds).map((id: number) => id) : []
  showReviewerDialog.value = true
}

const handleReviewerSubmit = async () => {
  if (!activity.value) return
  try {
    const reviewerCount = reviewerForm.value.selectedReviewers.length
    const reviewerIds = JSON.stringify(reviewerForm.value.selectedReviewers)
    await updateReviewerConfig(activity.value.id, reviewerCount, reviewerIds)
    ElMessage.success('评分人配置已保存')
    showReviewerDialog.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

const handleToggleStatus = async (newStatus: string) => {
  if (!activity.value) return
  try {
    await updateActivity(activity.value.id, { status: newStatus })
    ElMessage.success(newStatus === 'active' ? '活动已启用' : '活动已关闭')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
    // 恢复原状态
    activity.value.status = activity.value.status === 'active' ? 'closed' : 'active'
  }
}

const loadEnrollments = async (activityId: string) => {
  enrolledLoading.value = true
  try {
    const res = await getActivityEnrollments(activityId)
    if (res.code === 200) {
      enrolledTeachers.value = res.data || []
    }
  } catch (e) {
    console.error('获取报名列表失败', e)
  } finally {
    enrolledLoading.value = false
  }
}

const loadMaterials = async () => {
  if (!activity.value?.id) return
  materialsLoading.value = true
  try {
    const res = await getMaterialList({ activityId: activity.value.id, size: 100 })
    if (res.code === 200) {
      materials.value = res.data?.records || []
    }
  } catch (e) {
    console.error('获取学习资料失败', e)
  } finally {
    materialsLoading.value = false
  }
}

const formatFileSize = (size: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
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
    formData.append('activityId', activity.value.id.toString())
    formData.append('title', uploadForm.value.title)
    if (uploadForm.value.description) {
      formData.append('description', uploadForm.value.description)
    }

    const res = await uploadMaterial(formData)
    if (res.code === 200) {
      ElMessage.success('上传成功')
      showUploadDialog.value = false
      uploadForm.value = { title: '', description: '' }
      selectedFile.value = null
      fileList.value = []
      loadMaterials()
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const downloadMaterial = async (row: any) => {
  try {
    const res = await downloadApi(row.id)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.download = row.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '下载失败')
  }
}

const handleDeleteMaterial = async (row: any) => {
  await ElMessageBox.confirm('确定要删除该学习资料吗？', '提示', { type: 'warning' })
  try {
    await deleteMaterial(row.id)
    ElMessage.success('删除成功')
    loadMaterials()
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
.activity-detail {
  max-width: 1200px;
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
  gap: 4px;
}

.loading-state {
  background: #fff;
  border-radius: 20px;
  padding: 32px;
}

.info-card {
  background: #fff;
  border-radius: 20px;
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
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

.activity-level.level-C { background: #dcfce7; color: #16a34a; }
.activity-level.level-B2 { background: #dbeafe; color: #2563eb; }
.activity-level.level-B1 { background: #fed7aa; color: #ea580c; }
.activity-level.level-A2 { background: #fce7f3; color: #db2777; }
.activity-level.level-A1 { background: #e0e7ff; color: #4f46e5; }

.activity-title {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.activity-title h1 {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 700;
  color: #1e293b;
}

.activity-status {
  padding: 6px 16px;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.activity-status.status-active { background: #dcfce7; color: #16a34a; }
.activity-status.status-draft { background: #f1f5f9; color: #64748b; }
.activity-status.status-closed { background: #fee2e2; color: #dc2626; }

.activity-desc {
  color: #64748b;
  font-size: 1rem;
  margin: 0;
  line-height: 1.6;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  border: 1px solid #e2e8f0;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-icon.purple { background: linear-gradient(135deg, #8E2DE2 0%, #4A00E0 100%); }
.stat-icon.blue { background: linear-gradient(135deg, #00c6ff 0%, #0072ff 100%); }
.stat-icon.green { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
.stat-icon.pink { background: linear-gradient(135deg, #FF512F 0%, #DD2476 100%); }

.stat-icon .material-symbols-outlined { font-size: 28px; }

.stat-info { flex: 1; }

.stat-label {
  font-size: 0.75rem;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0 0 4px 0;
  font-weight: 600;
}

.stat-value {
  font-family: 'Manrope', sans-serif;
  font-size: 1.5rem;
  font-weight: 800;
  color: #1e293b;
  margin: 0;
}

.time-card,
.config-card,
.materials-card,
.enrolled-card,
.reviewer-card {
  background: #fff;
  border-radius: 20px;
  padding: 28px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  border: 1px solid #e2e8f0;
}

.reviewer-info {
  display: flex;
  align-items: center;
  padding: 12px 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-header .section-title {
  margin: 0;
}

.upload-tip {
  font-size: 0.75rem;
  color: #999;
  margin-top: 8px;
}

.section-title {
  font-family: 'Manrope', sans-serif;
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

.time-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.time-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.time-label {
  width: 100px;
  color: #64748b;
  font-weight: 500;
}

.time-value {
  color: #1e293b;
  font-weight: 500;
}

.config-grid {
  display: flex;
  gap: 32px;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.config-label {
  color: #64748b;
  font-weight: 500;
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

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .activity-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .activity-title {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>