<template>
  <div class="evaluator-period">
    <el-card>
      <template #header>
        <span>考核周期</span>
      </template>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expanded-content">
              <el-table :data="row.teachers" stripe v-if="row.teachers && row.teachers.length > 0">
                <el-table-column prop="id" label="ID" width="60" />
                <el-table-column prop="realName" label="姓名" />
                <el-table-column prop="department" label="所属教研组" />
                <el-table-column label="操作" width="200">
                  <template #default="{ row: teacher }">
                    <el-button type="primary" link @click="viewDocuments(row, teacher)">查看文档</el-button>
                    <el-button type="success" link @click="goEvaluate(row, teacher)">打分</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂无已报名老师" :image-size="60" />
            </div>
          </template>
        </el-table-column>
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
        <el-table-column prop="enrolledCount" label="已报名人数" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getEnrolledTeachers } from '@/api/period'

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getEnrolledTeachers()
    if (res.code === 200) {
      tableData.value = res.data
    }
  } finally {
    loading.value = false
  }
}

const viewDocuments = (period: any, teacher: any) => {
  router.push(`/evaluator/documents/${teacher.id}?periodId=${period.id}`)
}

const goEvaluate = (period: any, teacher: any) => {
  router.push(`/evaluator/evaluate/${teacher.id}?periodId=${period.id}`)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.expanded-content {
  padding: 10px 20px;
}
</style>