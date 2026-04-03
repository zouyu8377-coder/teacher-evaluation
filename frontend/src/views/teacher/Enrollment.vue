<template>
  <div class="enrollment">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的考核</span>
        </div>
      </template>
      
      <el-tabs v-model="activeTab">
        <el-tab-pane label="已报名周期" name="enrolled">
          <el-table :data="myPeriods" stripe v-loading="loading">
            <el-table-column prop="name" label="考核周期" />
            <el-table-column prop="startDate" label="开始日期" />
            <el-table-column prop="endDate" label="结束日期" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ row.status === 'active' ? '进行中' : row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button type="primary" link @click="goToUpload(row)">上传文档</el-button>
                <el-button type="primary" link @click="goToMaterials(row)">学习资料</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="myPeriods.length === 0 && !loading" description="暂无已报名周期" />
        </el-tab-pane>
        
        <el-tab-pane label="报名新周期" name="available">
          <el-table :data="availablePeriods" stripe v-loading="loading">
            <el-table-column prop="name" label="考核周期" />
            <el-table-column prop="startDate" label="开始日期" />
            <el-table-column prop="endDate" label="结束日期" />
            <el-table-column prop="description" label="说明" show-overflow-tooltip />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" @click="handleEnroll(row)">报名</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="availablePeriods.length === 0 && !loading" description="暂无可报名周期" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAvailablePeriods, getMyEnrollments, enrollPeriod } from '@/api/period'

const router = useRouter()
const activeTab = ref('enrolled')
const loading = ref(false)
const myPeriods = ref<any[]>([])
const availablePeriods = ref<any[]>([])

const loadMyEnrollments = async () => {
  loading.value = true
  try {
    const res = await getMyEnrollments()
    if (res.code === 200) {
      myPeriods.value = res.data
    }
  } finally {
    loading.value = false
  }
}

const loadAvailablePeriods = async () => {
  loading.value = true
  try {
    const res = await getAvailablePeriods()
    if (res.code === 200) {
      availablePeriods.value = res.data
    }
  } finally {
    loading.value = false
  }
}

const handleEnroll = async (row: any) => {
  try {
    const res = await enrollPeriod(row.id)
    if (res.code === 200) {
      ElMessage.success('报名成功')
      loadMyEnrollments()
      loadAvailablePeriods()
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '报名失败')
  }
}

const goToUpload = (row: any) => {
  router.push({ path: '/teacher/upload', query: { periodId: row.id } })
}

const goToMaterials = (row: any) => {
  router.push('/teacher/materials')
}

onMounted(() => {
  loadMyEnrollments()
  loadAvailablePeriods()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>