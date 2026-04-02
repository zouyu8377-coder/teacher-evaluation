<template>
  <div class="my-scores">
    <el-card>
      <template #header>
        <span>我的成绩</span>
      </template>
      
      <el-form inline>
        <el-form-item label="考核周期">
          <el-select v-model="query.periodId" placeholder="请选择" clearable @change="loadData">
            <el-option v-for="p in periods" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe>
        <el-table-column prop="periodName" label="考核周期" />
        <el-table-column prop="score" label="得分">
          <template #default="{ row }">
            <el-tag :type="row.score >= 90 ? 'success' : row.score >= 60 ? 'warning' : 'danger'">
              {{ row.score }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="evaluatorName" label="评分人" />
        <el-table-column prop="createdAt" label="评分时间" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button type="primary" link @click="showDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="评分详情" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="考核周期">{{ currentRow?.periodName }}</el-descriptions-item>
        <el-descriptions-item label="得分">{{ currentRow?.score }}</el-descriptions-item>
        <el-descriptions-item label="评分人">{{ currentRow?.evaluatorName }}</el-descriptions-item>
        <el-descriptions-item label="评分时间">{{ currentRow?.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="评语">{{ currentRow?.comment || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyScores } from '@/api/evaluation'
import { getPeriodList } from '@/api/period'

const query = reactive({
  periodId: null as number | null
})

const tableData = ref<any[]>([])
const periods = ref<any[]>([])
const dialogVisible = ref(false)
const currentRow = ref<any>(null)

const loadData = async () => {
  const res = await getMyScores(query.periodId || undefined)
  if (res.code === 200) {
    tableData.value = res.data
  }
}

const loadPeriods = async () => {
  const res = await getPeriodList()
  if (res.code === 200) {
    periods.value = res.data
  }
}

const showDetail = (row: any) => {
  currentRow.value = row
  dialogVisible.value = true
}

onMounted(() => {
  loadData()
  loadPeriods()
})
</script>

<style scoped>
.my-scores {
  max-width: 100%;
}
</style>