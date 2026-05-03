<template>
  <div class="exam-records">
    <el-card>
      <template #header>
        <div class="header-wrapper">
          <span>{{ singleMode ? '考试详情' : 'C级考试管理' }}</span>
          <el-button v-if="singleMode" @click="$router.back()">返回列表</el-button>
        </div>
      </template>

      <el-form v-if="!singleMode" inline>
        <el-form-item label="活动">
          <el-select v-model="query.activityId" placeholder="请选择" clearable filterable @change="loadData" style="width: 250px;">
            <el-option v-for="a in activities" :key="a.id" :label="`${a.level}级 - ${a.name}`" :value="a.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="teacherName" label="教师" />
        <el-table-column prop="department" label="部门" />
        <el-table-column label="考试状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="80">
          <template #default="{ row }">
            <span v-if="row.status === 'submitted'">{{ row.score }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="autoScore" label="自动分" width="80">
          <template #default="{ row }">
            <span v-if="row.status === 'submitted'">{{ row.autoScore || row.score }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="manualAdjust" label="调整分" width="80">
          <template #default="{ row }">
            <span v-if="row.status === 'submitted'">{{ row.manualAdjust || 0 }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="correctCount" label="正确" width="60">
          <template #default="{ row }">
            <span v-if="row.status === 'submitted'" class="text-success">{{ row.correctCount }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="wrongCount" label="错误" width="60">
          <template #default="{ row }">
            <span v-if="row.status === 'submitted'" class="text-danger">{{ row.wrongCount }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)" v-if="row.status === 'submitted'">
              查看详情
            </el-button>
            <el-button type="warning" link @click="openAdjust(row)" v-if="isAdmin && row.status === 'submitted'">
              调整分数
            </el-button>
            <el-button type="success" link @click="publishScore(row)" v-if="isAdmin && row.status === 'submitted' && !row.isPublished">
              公布成绩
            </el-button>
            <el-tag type="success" v-if="row.isPublished">已公布</el-tag>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </el-card>

    <el-dialog v-model="showDetail" title="考试详情" width="95%" top="2vh" :close-on-click-modal="false">
      <div v-if="currentRecord" class="detail-content">
        <el-descriptions :column="4" border class="mb-3">
          <el-descriptions-item label="教师">{{ currentRecord.teacherName }}</el-descriptions-item>
          <el-descriptions-item label="得分">{{ currentRecord.score }}</el-descriptions-item>
          <el-descriptions-item label="正确">{{ currentRecord.correctCount }}</el-descriptions-item>
          <el-descriptions-item label="错误">{{ currentRecord.wrongCount }}</el-descriptions-item>
        </el-descriptions>
        
        <el-divider />
        
        <div class="questions-list">
          <div v-for="q in detailQuestions" :key="q.order" class="question-item" :class="{ wrong: !q.isCorrect }">
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

    <el-dialog v-model="showAdjust" title="调整分数" width="400px">
      <el-form label-width="100px">
        <el-form-item label="当前得分">
          <span>{{ currentRecord?.score }}</span>
        </el-form-item>
        <el-form-item label="自动得分">
          <span>{{ currentRecord?.autoScore || currentRecord?.score }}</span>
        </el-form-item>
        <el-form-item label="调整分值">
          <el-input-number v-model="adjustValue" :min="-20" :max="20" :step="1" />
        </el-form-item>
        <el-form-item label="调整后得分">
          <span class="text-primary">{{ (currentRecord?.autoScore || currentRecord?.score || 0) + adjustValue }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdjust = false">取消</el-button>
        <el-button type="primary" @click="confirmAdjust" :loading="adjusting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getExamRecordsByActivity, getExamRecordDetail, adjustScore } from '@/api/exam'
import { getActivityList, getActivitiesByPeriod } from '@/api/activity'
import { publishEvaluationScores } from '@/api/evaluation'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const showDetail = ref(false)
const showAdjust = ref(false)
const adjusting = ref(false)
const singleMode = ref(false)
const isAdmin = computed(() => userStore.user?.role === 'admin')

const activities = ref<any[]>([])
const tableData = ref<any[]>([])
const total = ref(0)
const currentRecord = ref<any>(null)
const detailQuestions = ref<any[]>([])
const adjustValue = ref(0)

const query = reactive({
  activityId: null as number | null,
  page: 1,
  size: 20
})

const loadActivities = async () => {
  const res = await getActivityList()
  if (res.code === 200) {
    activities.value = res.data.filter((p: any) => p.status === 'active' && p.level === 'C')
  }
}

const loadData = async () => {
  if (!query.activityId) return
  loading.value = true
  try {
    const res = await getExamRecordsByActivity(query.activityId, query.page, query.size)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const loadSingleRecord = async () => {
  const activityId = Number(route.query.activityId)
  const teacherId = Number(route.params.teacherId)
  if (activityId && teacherId) {
    singleMode.value = true
    query.activityId = activityId
    loading.value = true
    try {
      const res = await getExamRecordsByActivity(activityId, 1, 20)
      if (res.code === 200) {
        const record = res.data.records.find((r: any) => r.teacherId === teacherId)
        if (record) {
          await viewDetail(record)
        }
      }
    } finally {
      loading.value = false
    }
  }
}

const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    'not_started': 'info',
    'in_progress': 'warning',
    'submitted': 'success'
  }
  return types[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    'not_started': '未开始',
    'in_progress': '进行中',
    'submitted': '已完成'
  }
  return texts[status] || status
}

const viewDetail = async (row: any) => {
  const res = await getExamRecordDetail(row.id)
  if (res.code === 200) {
    currentRecord.value = { ...row, ...res.data.record }
    detailQuestions.value = res.data.questions
    showDetail.value = true
  }
}

const openAdjust = (row: any) => {
  currentRecord.value = row
  adjustValue.value = row.manualAdjust || 0
  showAdjust.value = true
}

const confirmAdjust = async () => {
  if (!currentRecord.value) return
  adjusting.value = true
  try {
    const res = await adjustScore(currentRecord.value.id, adjustValue.value)
    if (res.code === 200) {
      ElMessage.success('分数调整成功')
      showAdjust.value = false
      loadData()
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '调整失败')
  } finally {
    adjusting.value = false
  }
}

const publishScore = async (row: any) => {
  try {
    const res = await publishEvaluationScores(query.activityId!, row.teacherId)
    if (res.code === 200) {
      ElMessage.success('成绩公布成功')
      loadData()
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '公布失败')
  }
}

onMounted(async () => {
  // 检查是否单条记录模式（从ActivityManage跳转）
  const activityId = route.query.activityId
  const teacherId = route.params.teacherId
  if (activityId && teacherId) {
    await loadActivities()
    await loadSingleRecord()
  } else {
    await loadActivities()
  }
})
</script>

<style scoped>
.header-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mb-3 {
  margin-bottom: 20px;
}
.text-success {
  color: #67c23a;
  font-weight: bold;
}
.text-danger {
  color: #f56c6c;
  font-weight: bold;
}
.text-primary {
  color: #409eff;
  font-weight: bold;
  font-size: 18px;
}
.detail-content {
  max-height: 70vh;
  overflow-y: auto;
}
.questions-list {
  max-height: 50vh;
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