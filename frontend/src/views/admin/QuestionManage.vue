<template>
  <div class="question-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>题库管理</span>
          <div>
            <el-button type="primary" @click="downloadTemplate">下载模板</el-button>
            <el-button type="success" @click="showImport = true">Excel导入</el-button>
            <el-button type="primary" @click="handleAdd">新增题目</el-button>
          </div>
        </div>
      </template>
      
      <el-form inline>
        <el-form-item label="活动">
          <el-select v-model="query.activityId" placeholder="请选择" clearable filterable @change="loadData" style="width: 200px;">
            <el-option v-for="p in activities" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="query.type" placeholder="全部" clearable filterable @change="loadData" style="width: 120px;">
            <el-option label="单选题" value="single" />
            <el-option label="多选题" value="multiple" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable filterable @change="loadData" style="width: 100px;">
            <el-option label="启用" :value="true" />
            <el-option label="禁用" :value="false" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
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
            <el-rate v-model="row.difficulty" disabled :max="5" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="60">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'info'">
              {{ row.status ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑题目' : '新增题目'" width="700px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="题目内容" prop="questionText">
          <el-input v-model="form.questionText" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="题型" prop="questionType">
          <el-radio-group v-model="form.questionType">
            <el-radio value="single">单选题</el-radio>
            <el-radio value="multiple">多选题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="选项">
          <div class="options-container">
            <div v-for="(opt, idx) in form.options" :key="idx" class="option-row">
              <span class="option-id">{{ opt.id }}:</span>
              <el-input v-model="opt.text" placeholder="选项内容" style="flex:1" />
              <el-button type="danger" link @click="removeOption(idx)" v-if="form.options.length > 2">×</el-button>
            </div>
            <el-button type="primary" link @click="addOption" v-if="form.options.length < 6">
              + 添加选项({{ form.options.length }}/6)
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="正确答案" prop="correctAnswer">
          <el-input v-model="form.correctAnswer" :placeholder="form.questionType === 'single' ? '如: A' : '如: ABC'" />
          <span class="tip">{{ form.questionType === 'single' ? '单个字母' : '多个字母组合如ABC' }}</span>
        </el-form-item>
        <el-form-item label="分值" prop="score">
          <el-input-number v-model="form.score" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="难度">
          <el-rate v-model="form.difficulty" :max="5" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.explanation" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showImport" title="Excel导入题目" width="500px">
      <el-form>
        <el-form-item label="选择文件">
          <el-upload ref="uploadRef" :auto-upload="false" :limit="1" accept=".xlsx,.xls">
            <el-button>选择Excel文件</el-button>
            <template #tip>
              <div class="el-upload__tip">请先下载模板，按格式填写后上传</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showImport = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getQuestions, createQuestion, updateQuestion, deleteQuestion, importQuestions, downloadQuestionTemplate } from '@/api/exam'
import { getActivityList } from '@/api/activity'

const loading = ref(false)
const dialogVisible = ref(false)
const showImport = ref(false)
const importing = ref(false)
const editId = ref<number | null>(null)
const formRef = ref()
const uploadRef = ref()

const activities = ref<any[]>([])
const tableData = ref<any[]>([])
const total = ref(0)

const query = reactive({
  activityId: null as number | null,
  type: '' as '' | 'single' | 'multiple',
  status: null as boolean | null,
  page: 1,
  size: 10
})

const form = reactive({
  activityId: null as number | null,
  questionText: '',
  questionType: 'single' as 'single' | 'multiple',
  options: [
    { id: 'A', text: '' },
    { id: 'B', text: '' },
    { id: 'C', text: '' },
    { id: 'D', text: '' }
  ],
  correctAnswer: '',
  score: 5,
  difficulty: 1,
  explanation: '',
  status: true
})

const rules = {
  questionText: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  questionType: [{ required: true, message: '请选择题型', trigger: 'change' }],
  correctAnswer: [{ required: true, message: '请输入正确答案', trigger: 'blur' }]
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
    const res = await getQuestions({
      activityId: query.activityId,
      type: query.type || undefined,
      status: query.status ?? undefined,
      page: query.page,
      size: query.size
    })
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
    questionText: '',
    questionType: 'single',
    options: [
      { id: 'A', text: '' },
      { id: 'B', text: '' },
      { id: 'C', text: '' },
      { id: 'D', text: '' }
    ],
    correctAnswer: '',
    score: 5,
    difficulty: 1,
    explanation: '',
    status: true
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  editId.value = row.id
  let options = []
  try {
    options = JSON.parse(row.options)
  } catch {
    options = []
  }
  Object.assign(form, {
    activityId: row.activityId,
    questionText: row.questionText,
    questionType: row.questionType,
    options: options.length > 0 ? options : [
      { id: 'A', text: '' },
      { id: 'B', text: '' },
      { id: 'C', text: '' },
      { id: 'D', text: '' }
    ],
    correctAnswer: row.correctAnswer,
    score: row.score,
    difficulty: row.difficulty,
    explanation: row.explanation,
    status: row.status
  })
  dialogVisible.value = true
}

const addOption = () => {
  if (form.options.length < 6) {
    const ids = ['A', 'B', 'C', 'D', 'E', 'F']
    form.options.push({ id: ids[form.options.length], text: '' })
  }
}

const removeOption = (idx: number) => {
  if (form.options.length > 2) {
    form.options.splice(idx, 1)
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  
  const optionsJson = JSON.stringify(form.options.filter(o => o.text))
  
  const data = {
    activityId: query.activityId,
    questionText: form.questionText,
    questionType: form.questionType,
    options: optionsJson,
    correctAnswer: form.correctAnswer.toUpperCase(),
    score: form.score,
    difficulty: form.difficulty,
    explanation: form.explanation,
    status: form.status
  }
  
  loading.value = true
  try {
    let res
    if (editId.value) {
      res = await updateQuestion(editId.value, data)
    } else {
      res = await createQuestion(data)
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
  await ElMessageBox.confirm('确定要删除该题目吗？', '提示', { type: 'warning' })
  try {
    await deleteQuestion(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

const downloadTemplate = async () => {
  try {
    const res = await downloadQuestionTemplate()
    const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'C级题库导入模板.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('模板下载成功')
  } catch (e) {
    ElMessage.error('模板下载失败')
  }
}

const handleImport = async () => {
  const files = (uploadRef.value as any)?.uploadFiles
  if (!files || files.length === 0) {
    ElMessage.warning('请选择要导入的Excel文件')
    return
  }
  
  importing.value = true
  try {
    const res = await importQuestions(files[0].raw, query.activityId!)
    if (res.code === 200) {
      ElMessage.success(`导入成功，共${res.data}道题目`)
      showImport.value = false
      loadData()
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '导入失败')
  } finally {
    importing.value = false
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
.options-container {
  width: 100%;
}
.option-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  gap: 8px;
}
.option-id {
  width: 24px;
  font-weight: bold;
}
.tip {
  margin-left: 10px;
  color: #999;
  font-size: 12px;
}
</style>