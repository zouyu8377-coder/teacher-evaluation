<template>
  <div class="paper-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>试卷管理</span>
          <el-button type="primary" @click="handleAdd">新增试卷</el-button>
        </div>
      </template>
      
      <el-form inline>
        <el-form-item label="活动">
          <el-select v-model="query.activityId" placeholder="请选择" clearable filterable @change="loadData" style="width: 200px;">
            <el-option v-for="p in activities" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="试卷名称" />
        <el-table-column prop="description" label="说明" show-overflow-tooltip />
        <el-table-column prop="questionCount" label="题目数" width="80" />
        <el-table-column prop="totalScore" label="总分" width="60" />
        <el-table-column prop="durationMinutes" label="时长(分)" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : row.status === 'closed' ? 'danger' : 'info'">
              {{ row.status === 'active' ? '启用' : row.status === 'closed' ? '已关闭' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
              <el-button type="success" link @click="openQuestionSelector(row)">选题</el-button>
              <el-button type="warning" link @click="openGenerator(row)">组卷</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </div>
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

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑试卷' : '新增试卷'" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="活动" prop="activityId">
          <el-select v-model="form.activityId" placeholder="请选择">
            <el-option v-for="p in activities" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="试卷名称" prop="name">
          <el-input v-model="form.name" placeholder="如：C级考试卷一" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="考试时长">
          <el-input-number v-model="form.durationMinutes" :min="10" :max="180" />
          <span style="margin-left: 10px;">分钟</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="draft">草稿</el-radio>
            <el-radio value="active">启用</el-radio>
            <el-radio value="closed">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showQuestionSelector" title="选择题目" width="900px">
      <el-form inline>
        <el-form-item label="题型">
          <el-select v-model="questionQuery.type" placeholder="全部" clearable filterable @change="loadQuestions" style="width: 120px;">
            <el-option label="单选题" value="single" />
            <el-option label="多选题" value="multiple" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-table ref="questionTableRef" :data="questionList" @selection-change="handleSelectionChange" stripe row-key="id">
        <el-table-column type="selection" width="40" />
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
      </el-table>
      <el-pagination
        v-model:current-page="questionQuery.page"
        v-model:page-size="questionQuery.size"
        :total="questionTotal"
        layout="total, prev, pager, next"
        @current-change="loadQuestions"
      />
      <template #footer>
        <el-button @click="showQuestionSelector = false">取消</el-button>
        <el-button type="primary" @click="confirmSelectQuestions">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showGenerator" title="随机组卷" width="400px">
      <el-form label-width="100px">
        <el-form-item label="单选题数量">
          <el-input-number v-model="generator.singleCount" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="多选题数量">
          <el-input-number v-model="generator.multiCount" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="可用题数">
          <span>单选: {{ questionStats.single }} | 多选: {{ questionStats.multi }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGenerator = false">取消</el-button>
        <el-button type="primary" @click="confirmGenerate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPapers, createPaper, updatePaper, deletePaper, getPaperQuestions, setPaperQuestions, generatePaper, getQuestions } from '@/api/exam'
import { getActivityList } from '@/api/activity'

const loading = ref(false)
const dialogVisible = ref(false)
const showQuestionSelector = ref(false)
const showGenerator = ref(false)
const editId = ref<number | null>(null)
const formRef = ref()
const questionTableRef = ref()
const selectedPaper = ref<any>(null)
const selectedQuestions = ref<any[]>([])

const activities = ref<any[]>([])
const query = reactive({
  activityId: null as number | null,
  page: 1,
  size: 10
})

const form = reactive({
  activityId: null as number | null,
  name: '',
  description: '',
  durationMinutes: 60,
  status: 'draft'
})

const questionList = ref<any[]>([])
const questionTotal = ref(0)
const questionQuery = reactive({
  type: '' as '' | 'single' | 'multiple',
  page: 1,
  size: 20
})

const generator = reactive({
  singleCount: 10,
  multiCount: 10
})

const questionStats = reactive({
  single: 0,
  multi: 0
})

const rules = {
  activityId: [{ required: true, message: '请选择活动', trigger: 'change' }],
  name: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }]
}

const loadActivities = async () => {
  const res = await getActivityList()
  if (res.code === 200) {
    activities.value = res.data
    if (activities.value.length > 0 && !query.activityId) {
      query.activityId = activities.value[0].id
      loadData()
    }
  }
}

const loadData = async () => {
  if (!query.activityId) return
  loading.value = true
  try {
    const res = await getPapers({ activityId: query.activityId, page: query.page, size: query.size })
    if (res.code === 200) {
      tableData.value = res.data.content
      total.value = res.data.totalElements
    }
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, {
    activityId: query.activityId,
    name: '',
    description: '',
    durationMinutes: 60,
    status: 'draft'
  })
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  editId.value = row.id
  Object.assign(form, {
    activityId: row.activityId,
    name: row.name,
    description: row.description,
    durationMinutes: row.durationMinutes,
    status: row.status
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  try {
    let res
    if (editId.value) {
      res = await updatePaper(editId.value, form)
    } else {
      res = await createPaper(form)
    }
    if (res.code === 200) {
      ElMessage.success('操作成功')
      dialogVisible.value = false
      loadData()
    }
  } finally {
    loading.value = false
  }
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确定要删除该试卷吗？', '提示', { type: 'warning' })
  try {
    await deletePaper(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

const openQuestionSelector = async (row: any) => {
  selectedPaper.value = row
  selectedQuestions.value = []
  
  // 加载已有题目
  const res = await getPaperQuestions(row.id)
  if (res.code === 200 && res.data) {
    selectedQuestions.value = res.data.map((pq: any) => pq.questionId)
  }
  
  await loadQuestions()
  showQuestionSelector.value = true
}

const loadQuestions = async () => {
  if (!query.activityId) return
  const res = await getQuestions({
    activityId: query.activityId,
    type: questionQuery.type || undefined,
    status: true,
    page: questionQuery.page,
    size: questionQuery.size
  })
  if (res.code === 200) {
    questionList.value = res.data.content
    questionTotal.value = res.data.totalElements
    
    // 统计可用题目
    questionStats.single = res.data.content.filter((q: any) => q.questionType === 'single').length
    questionStats.multi = res.data.content.filter((q: any) => q.questionType === 'multiple').length
    
    // 设置已选题目预选中
    nextTick(() => {
      if (questionTableRef.value && selectedQuestions.value.length > 0) {
        questionList.value.forEach((q: any) => {
          if (selectedQuestions.value.includes(q.id)) {
            questionTableRef.value.toggleRowSelection(q, true)
          }
        })
      }
    })
  }
}

const handleSelectionChange = (val: any[]) => {
  selectedQuestions.value = val.map(q => q.id)
}

const confirmSelectQuestions = async () => {
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请选择题目')
    return
  }
  try {
    await setPaperQuestions(selectedPaper.value.id, selectedQuestions.value)
    ElMessage.success('选题成功')
    showQuestionSelector.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const openGenerator = (row: any) => {
  selectedPaper.value = row
  generator.singleCount = 10
  generator.multiCount = 10
  showGenerator.value = true
}

const confirmGenerate = async () => {
  try {
    await generatePaper(selectedPaper.value.id, generator.singleCount, generator.multiCount)
    ElMessage.success('组卷成功')
    showGenerator.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '组卷失败')
  }
}

onMounted(() => {
  loadActivities()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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