<template>
  <div class="enrollment">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的考核</span>
        </div>
      </template>
      
      <el-tabs v-model="activeTab">
        <el-tab-pane label="已报名活动" name="enrolled">
          <el-table :data="myActivities" stripe v-loading="loading">
            <el-table-column prop="activityName" label="活动名称" />
            <el-table-column prop="level" label="级别">
              <template #default="{ row }">
                <el-tag :type="getLevelType(row.level)">{{ row.level }}级</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="enrolledAt" label="报名时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.enrolledAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="250">
              <template #default="{ row }">
                <template v-if="row.level === 'C' && row.hasExam">
                  <el-button type="primary" link @click="goToExam(row)">开始考试</el-button>
                </template>
                <template v-else>
                  <el-button type="primary" link @click="goToUpload(row)">上传文档</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="myActivities.length === 0 && !loading" description="暂未报名任何活动" />
        </el-tab-pane>
        
        <el-tab-pane label="报名新活动" name="available">
          <el-table :data="availableActivities" stripe v-loading="loading">
            <el-table-column prop="name" label="活动名称" />
            <el-table-column prop="level" label="级别">
              <template #default="{ row }">
                <el-tag :type="getLevelType(row.level)">{{ row.level }}级</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" show-overflow-tooltip />
            <el-table-column label="考核时间" width="180">
              <template #default="{ row }">
                {{ row.startDate || '-' }} ~ {{ row.endDate || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="报名余量" width="100">
              <template #default="{ row }">
                <span v-if="row.enrollmentInfo" :class="{ 'text-danger': row.enrollmentInfo.remaining === 0 }">
                  {{ row.enrollmentInfo.remaining === -1 ? '不限' : `${row.enrollmentInfo.remaining}/${row.enrollmentInfo.maxParticipants}` }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="报名时间" width="180">
              <template #default="{ row }">
                <span v-if="row.enrollmentInfo">
                  {{ formatDateTime(row.enrollmentInfo.enrollmentStart) }} ~ {{ formatDateTime(row.enrollmentInfo.enrollmentEnd) }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button 
                  type="primary" 
                  @click="handleEnroll(row)" 
                  :disabled="!row.canEnroll || (row.enrollmentInfo && row.enrollmentInfo.remaining === 0)"
                >
                  报名
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="availableActivities.length === 0 && !loading" description="暂无可报名活动" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyEnrollments, enrollActivity, getAvailableActivitiesList, canEnrollActivity, getEnrollmentInfo } from '@/api/activity'

const router = useRouter()
const activeTab = ref('enrolled')
const loading = ref(false)
const availableActivities = ref<any[]>([])
const myActivities = ref<any[]>([])

const formatDateTime = (datetime: string | null) => {
  if (!datetime) return '-'
  return datetime.slice(0, 16).replace('T', ' ')
}

const getLevelType = (level: string) => {
  const types: Record<string, string> = {
    'C': 'info',
    'B2': 'primary',
    'B1': 'success',
    'A2': 'warning',
    'A1': 'danger'
  }
  return types[level] || 'info'
}

const loadAvailableActivities = async () => {
  loading.value = true
  try {
    const res = await getAvailableActivitiesList()
    if (res.code === 200) {
      const activities = res.data || []
      for (const activity of activities) {
        try {
          const checkRes = await canEnrollActivity(activity.id)
          activity.canEnroll = checkRes.data
        } catch {
          activity.canEnroll = false
        }
        try {
          const infoRes = await getEnrollmentInfo(activity.id)
          activity.enrollmentInfo = infoRes.data
        } catch {
          activity.enrollmentInfo = null
        }
      }
      availableActivities.value = activities
    }
  } finally {
    loading.value = false
  }
}

const loadMyActivities = async () => {
  const res = await getMyEnrollments()
  if (res.code === 200) {
    myActivities.value = res.data || []
  }
}

const handleEnroll = async (row: any) => {
  try {
    const res = await enrollActivity(row.id)
    if (res.code === 200) {
      ElMessage.success('报名成功！您现在可以上传考核文档和查看学习资料。')
      loadAvailableActivities()
      loadMyActivities()
      activeTab.value = 'enrolled'
    }
  } catch (e: any) {
    const msg = e.response?.data?.message || '报名失败'
    ElMessage.error(msg)
  }
}

const goToUpload = (row: any) => {
  router.push({ path: '/teacher/upload', query: { activityId: row.activityId } })
}

const goToExam = (row: any) => {
  router.push({ path: '/teacher/exam', query: { activityId: row.activityId } })
}

onMounted(async () => {
  await loadMyActivities()
  await loadAvailableActivities()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.text-danger {
  color: #f56c6c;
  font-weight: bold;
}
</style>