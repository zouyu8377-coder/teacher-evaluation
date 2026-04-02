<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon teacher">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.teacherCount }}</div>
            <div class="stat-label">教师数量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon evaluator">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.evaluatorCount }}</div>
            <div class="stat-label">考核员数量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon document">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.documentCount }}</div>
            <div class="stat-label">文档总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon evaluation">
            <el-icon><Finished /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.evaluationCount }}</div>
            <div class="stat-label">评分记录</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>当前活跃考核周期</span>
          </template>
          <div v-if="activePeriod" class="period-info">
            <h3>{{ activePeriod.name }}</h3>
            <p>时间范围：{{ activePeriod.startDate }} ~ {{ activePeriod.endDate }}</p>
            <p>{{ activePeriod.description }}</p>
          </div>
          <el-empty v-else description="暂无活跃周期" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>考核进度</span>
          </template>
          <div class="progress-info">
            <div class="progress-item">
              <span>已完成评分</span>
              <el-progress :percentage="progress.completed" :stroke-width="20" />
            </div>
            <div class="progress-item">
              <span>待评分</span>
              <el-progress :percentage="progress.pending" :stroke-width="20" status="exception" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { User, DataAnalysis, Document, Finished } from '@element-plus/icons-vue'
import { getUserList, getTeachers } from '@/api/user'
import { getDocumentList } from '@/api/document'
import { getEvaluationList } from '@/api/evaluation'
import { getActivePeriod } from '@/api/period'

const stats = ref({
  teacherCount: 0,
  evaluatorCount: 0,
  documentCount: 0,
  evaluationCount: 0
})

const activePeriod = ref<any>(null)
const progress = ref({
  completed: 0,
  pending: 0
})

const loadStats = async () => {
  const [teacherRes, evaluatorRes, docRes, evalRes] = await Promise.all([
    getUserList({ role: 'teacher', size: 1 }),
    getUserList({ role: 'evaluator', size: 1 }),
    getDocumentList({ size: 1 }),
    getEvaluationList({ size: 1 })
  ])

  stats.value = {
    teacherCount: teacherRes.data?.total || 0,
    evaluatorCount: evaluatorRes.data?.total || 0,
    documentCount: docRes.data?.total || 0,
    evaluationCount: evalRes.data?.total || 0
  }

  const total = stats.value.teacherCount
  if (total > 0) {
    progress.value.completed = Math.round((stats.value.evaluationCount / total) * 100)
    progress.value.pending = 100 - progress.value.completed
  }
}

const loadActivePeriod = async () => {
  const res = await getActivePeriod()
  if (res.code === 200 && res.data) {
    activePeriod.value = res.data
  }
}

onMounted(() => {
  loadStats()
  loadActivePeriod()
})
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
  margin-right: 15px;
}

.stat-icon.teacher {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.evaluator {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.document {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.evaluation {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.period-info h3 {
  margin: 0 0 10px;
  color: #409EFF;
}

.period-info p {
  margin: 5px 0;
  color: #606266;
}

.progress-info {
  padding: 10px 0;
}

.progress-item {
  margin-bottom: 20px;
}

.progress-item span {
  display: block;
  margin-bottom: 8px;
  color: #606266;
}
</style>