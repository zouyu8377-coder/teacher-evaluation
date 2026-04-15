<template>
  <div class="paper-questions">
    <el-page-header @back="goBack" content="试卷选题" />

    <el-card class="paper-info-card">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="试卷名称">{{ paperInfo.name }}</el-descriptions-item>
        <el-descriptions-item label="说明">{{ paperInfo.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ paperInfo.totalScore }}分</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>已选题目 ({{ selectedQuestions.length }}题)</span>
          <el-button type="primary" @click="showAddDialog = true">添加题目</el-button>
        </div>
      </template>

      <el-table :data="selectedQuestions" stripe v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="questionText" label="题目内容" show-overflow-tooltip />
        <el-table-column prop="questionType" label="题型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.questionType === 'single' ? 'primary' : 'success'">
              {{ row.questionType === 'single' ? '单选题' : '多选题' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="80" />
        <el-table-column prop="questionOrder" label="顺序" width="80" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link @click="removeQuestion(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="selectedQuestions.length === 0 && !loading" description="暂无题目，请添加题目" />
    </el-card>

    <!-- 添加题目对话框 -->
    <el-dialog v-model="showAddDialog" title="添加题目" width="900px">
      <el-form inline>
        <el-form-item label="题型">
          <el-select v-model="questionQuery.type" placeholder="全部" clearable filterable @change="loadQuestions" style="width: 120px;">
            <el-option label="单选题" value="single" />
            <el-option label="多选题" value="multiple" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="questionQuery.difficulty" placeholder="全部" clearable filterable @change="loadQuestions" style="width: 100px;">
            <el-option label="简单" :value="1" />
            <el-option label="中等" :value="2" />
            <el-option label="困难" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table ref="questionTableRef" :data="questionList" @selection-change="handleSelectionChange" stripe row-key="id" v-loading="questionLoading">
        <el-table-column type="selection" width="40" :selectable="isQuestionSelectable" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="questionText" label="题目内容" show-overflow-tooltip />
        <el-table-column prop="questionType" label="题型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.questionType === 'single' ? 'primary' : 'success'">
              {{ row.questionType === 'single' ? '单选' : '多选' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="60" />
        <el-table-column prop="difficulty" label="难度" width="60">
          <template #default="{ row }">
            <el-tag :type="row.difficulty === 1 ? 'success' : row.difficulty === 2 ? 'warning' : 'danger'">
              {{ row.difficulty === 1 ? '简单' : row.difficulty === 2 ? '中等' : '困难' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="selectedQuestionIds.includes(row.id)" type="success">已添加</el-tag>
            <el-tag v-else type="info">未添加</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="questionQuery.page"
        v-model:page-size="questionQuery.size"
        :total="questionTotal"
        layout="total, prev, pager, next"
        @current-change="loadQuestions"
      />

      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="addSelectedQuestions">添加选中题目</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPaperById, getPaperQuestions, setPaperQuestions, getQuestions } from '@/api/exam'

const route = useRoute()
const router = useRouter()

const paperId = Number(route.params.id)
const loading = ref(true)
const saving = ref(false)
const showAddDialog = ref(false)
const questionLoading = ref(false)

const paperInfo = ref<any>({})
const selectedQuestions = ref<any[]>([])
const questionList = ref<any[]>([])
const questionTotal = ref(0)
const selectedToAdd = ref<any[]>([])

const questionQuery = ref({
  activityId: null as number | null,
  type: '',
  difficulty: null as number | null,
  page: 1,
  size: 20
})

const questionTableRef = ref()

// 已选择的题目ID列表（用于去重和标记）
const selectedQuestionIds = computed(() => selectedQuestions.value.map(q => q.questionId))

const goBack = () => {
  router.push('/admin/papers')
}

const loadPaperInfo = async () => {
  loading.value = true
  try {
    const res = await getPaperById(paperId)
    if (res.code === 200) {
      paperInfo.value = res.data
      // 设置活动ID用于加载题目
      if (res.data.activityId) {
        questionQuery.value.activityId = res.data.activityId
      }
    }
  } finally {
    loading.value = false
  }
}

const loadSelectedQuestions = async () => {
  const res = await getPaperQuestions(paperId)
  if (res.code === 200 && res.data) {
    selectedQuestions.value = res.data.map((pq: any) => ({
      id: pq.id,
      questionId: pq.questionId,
      questionText: pq.question?.questionText || '',
      questionType: pq.question?.questionType || 'single',
      score: pq.question?.score || 0,
      questionOrder: pq.questionOrder
    }))
  }
}

const loadQuestions = async () => {
  if (!questionQuery.value.activityId) {
    ElMessage.warning('请先关联活动才能加载题目')
    return
  }
  questionLoading.value = true
  try {
    const res = await getQuestions({
      activityId: questionQuery.value.activityId,
      type: questionQuery.value.type || undefined,
      difficulty: questionQuery.value.difficulty || undefined,
      status: true,
      page: questionQuery.value.page,
      size: questionQuery.value.size
    })
    if (res.code === 200) {
      questionList.value = res.data.content
      questionTotal.value = res.data.totalElements
    }
  } finally {
    questionLoading.value = false
  }
}

const isQuestionSelectable = (row: any) => {
  return !selectedQuestionIds.value.includes(row.id)
}

const handleSelectionChange = (val: any[]) => {
  selectedToAdd.value = val
}

const removeQuestion = async (row: any) => {
  await ElMessageBox.confirm('确定要移除该题目吗？', '提示', { type: 'warning' })

  // 移除题目 = 重新设置所有题目ID（排除要移除的）
  const newQuestionIds = selectedQuestions.value
    .filter(q => q.questionId !== row.questionId)
    .map(q => q.questionId)

  try {
    await setPaperQuestions(paperId, newQuestionIds)
    ElMessage.success('移除成功')
    loadSelectedQuestions()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const addSelectedQuestions = async () => {
  if (selectedToAdd.value.length === 0) {
    ElMessage.warning('请选择要添加的题目')
    return
  }

  saving.value = true
  try {
    // 合并已有题目和新选题目
    const newQuestionIds = [
      ...selectedQuestionIds.value,
      ...selectedToAdd.value.map((q: any) => q.id)
    ]

    await setPaperQuestions(paperId, newQuestionIds)
    ElMessage.success('添加成功')
    showAddDialog.value = false
    selectedToAdd.value = []
    loadSelectedQuestions()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadPaperInfo()
  await loadSelectedQuestions()
  if (questionQuery.value.activityId) {
    await loadQuestions()
  }
})
</script>

<style scoped>
.paper-questions {
  padding: 20px;
}

.paper-info-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-pagination {
  margin-top: 20px;
  justify-content: center;
}
</style>