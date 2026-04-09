<template>
  <div class="evaluator-activity">
    <el-card>
      <template #header>
        <div class="header-wrapper">
          <span>考核活动</span>
          <el-button type="primary" @click="publishScores" :loading="publishLoading">公布成绩</el-button>
        </div>
      </template>

      <el-form inline>
        <el-form-item label="选择考核活动">
          <el-select v-model="selectedActivity" placeholder="请选择活动" @change="onActivityChange" clearable filterable style="width: 280px;">
            <el-option v-for="a in activities" :key="a.id" :label="`${a.name} (${a.level}级)`" :value="a.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="department" label="所属教研组" />
        <el-table-column prop="activityName" label="考核活动" />
        <el-table-column prop="activityLevel" label="级别">
          <template #default="{ row }">
            <el-tag>{{ row.activityLevel }}级</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="得分">
          <template #default="{ row }">
            {{ row.score !== null ? row.score : '未评分' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link @click="viewDocuments(row)">查看文档</el-button>
              <el-button type="success" link @click="goEvaluate(row)">打分</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!selectedActivity" description="请先选择考核活动" />
      <el-empty v-else-if="tableData.length === 0 && !loading" description="暂无已报名老师" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getActiveActivities, getActivityEnrollments } from '@/api/activity'
import { publishEvaluationScores } from '@/api/evaluation'

const router = useRouter()
const loading = ref(false)
const publishLoading = ref(false)
const tableData = ref<any[]>([])
const activities = ref<any[]>([])
const selectedActivity = ref<number | null>(null)

const loadActivities = async () => {
  const res = await getActiveActivities()
  if (res.code === 200) {
    activities.value = res.data
  }
}

const onActivityChange = async () => {
  tableData.value = []
  if (selectedActivity.value) {
    loading.value = true
    try {
      const res = await getActivityEnrollments(selectedActivity.value)
      if (res.code === 200) {
        const activity = activities.value.find(a => a.id === selectedActivity.value)
        tableData.value = res.data.map((t: any) => ({
          ...t,
          activityName: activity?.name,
          activityLevel: activity?.level
        }))
      }
    } finally {
      loading.value = false
    }
  }
}

const viewDocuments = (row: any) => {
  router.push(`/evaluator/documents/${row.id}?activityId=${selectedActivity.value}`)
}

const goEvaluate = (row: any) => {
  router.push(`/evaluator/evaluate/${row.id}?activityId=${selectedActivity.value}`)
}

const publishScores = async () => {
  if (!selectedActivity.value) {
    ElMessage.warning('请先选择考核活动')
    return
  }
  
  await ElMessageBox.confirm('确定要公布这次考核中所有已打分的成绩吗？公布后教师可查看。', '提示', { type: 'warning' })
  
  publishLoading.value = true
  try {
    const res = await publishEvaluationScores(selectedActivity.value)
    if (res.code === 200) {
      ElMessage.success('成绩已公布')
    }
  } finally {
    publishLoading.value = false
  }
}

onMounted(() => {
  loadActivities()
})
</script>

<style scoped>
.header-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.action-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>