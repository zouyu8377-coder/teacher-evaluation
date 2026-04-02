<template>
  <div class="evaluation-form">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>评分 - {{ teacherName }}</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="教师姓名">
          <el-input :value="teacherName" disabled />
        </el-form-item>
        
        <el-form-item label="考核周期" prop="periodId">
          <el-select v-model="form.periodId" placeholder="请选择考核周期">
            <el-option v-for="p in periods" :key="p.id" :label="p.name" :value="p.id" />
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
          <el-button type="primary" :loading="loading" @click="handleSubmit">提交评分</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <h3>历史评分</h3>
      <el-table :data="historyData" stripe>
        <el-table-column prop="periodName" label="考核周期" />
        <el-table-column prop="score" label="评分" />
        <el-table-column prop="comment" label="评语" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="评分时间" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitEvaluation, getEvaluationList } from '@/api/evaluation'
import { getPeriodList } from '@/api/period'
import { getTeachers } from '@/api/user'

const route = useRoute()
const router = useRouter()
const teacherId = computed(() => Number(route.params.teacherId))
const teacherName = ref('')

const formRef = ref()
const loading = ref(false)

const form = reactive({
  periodId: null as number | null,
  score: 0,
  comment: ''
})

const rules = {
  periodId: [{ required: true, message: '请选择考核周期', trigger: 'change' }],
  score: [{ required: true, message: '请输入评分', trigger: 'blur' }],
  comment: [{ max: 2000, message: '评语不能超过2000字', trigger: 'blur' }]
}

const periods = ref<any[]>([])
const historyData = ref<any[]>([])

const loadPeriods = async () => {
  const res = await getPeriodList()
  if (res.code === 200) {
    periods.value = res.data
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
    periodId: undefined,
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
      periodId: form.periodId!,
      score: form.score,
      comment: form.comment
    })
    if (res.code === 200) {
      ElMessage.success('提交成功')
      router.push('/evaluator/teachers')
    }
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPeriods()
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
  max-width: 700px;
}
</style>