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
          <el-select v-model="query.periodId" placeholder="请选择" clearable filterable @change="loadData" style="width: 200px;">
            <!-- 注意：query 保留 periodId，但 UI 显示为活动 -->
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
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
              <el-button type="success" link @click="goToQuestionSelector(row)">选题</el-button>
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPapers, createPaper, updatePaper, deletePaper } from '@/api/exam'
import { getActivityList } from '@/api/activity'
import type { ExamPaper } from '@/api/exam'

const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const formRef = ref()

const activities = ref<any[]>([])
const query = reactive({
  periodId: null as number | null,
  page: 1,
  size: 10
})

const form = reactive({
  activityId: null as number | null,
  name: '',
  description: '',
  status: 'draft' as ExamPaper['status']
})

const rules = {
  activityId: [{ required: true, message: '请选择活动', trigger: 'change' }],
  name: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }]
}

const loadActivities = async () => {
  const res = await getActivityList()
  if (res.code === 200) {
    activities.value = res.data
    if (activities.value.length > 0 && !query.periodId) {
      query.periodId = activities.value[0].id
      loadData()
    }
  }
}

const loadData = async () => {
  if (!query.periodId) return
  loading.value = true
  try {
    const res = await getPapers({ periodId: query.periodId, page: query.page, size: query.size })
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, {
    activityId: query.periodId,
    name: '',
    description: '',
    status: 'draft' as ExamPaper['status']
  })
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  editId.value = row.id
  Object.assign(form, {
    activityId: row.periodId,
    name: row.name,
    description: row.description,
    status: row.status as ExamPaper['status']
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!form.activityId) {
    ElMessage.error('请选择活动')
    return
  }

  loading.value = true
  try {
    const payload = { ...form, periodId: form.activityId } as Partial<ExamPaper>
    let res
    if (editId.value) {
      res = await updatePaper(editId.value, payload)
    } else {
      res = await createPaper(payload)
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

const goToQuestionSelector = (row: any) => {
  router.push(`/admin/papers/${row.id}/questions`)
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