<template>
  <div class="material-list">
    <el-card>
      <template #header>
        <span>学习资料</span>
      </template>

      <el-form inline>
        <el-form-item label="活动">
          <el-select v-model="query.activityId" placeholder="全部" clearable filterable @change="loadData" style="width: 200px;">
            <el-option v-for="p in activities" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="title" label="资料标题" show-overflow-tooltip />
        <el-table-column prop="fileName" label="文件名" show-overflow-tooltip />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="periodName" label="考核周期" width="150" />
        <el-table-column prop="creatorName" label="上传人" width="100" />
        <el-table-column prop="createdAt" label="上传时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDownload(row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMaterialList, downloadMaterial } from '@/api/learningMaterial'
import { getActivityList } from '@/api/activity'

const tableData = ref<any[]>([])
const activities = ref<any[]>([])
const loading = ref(false)
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  activityId: null as number | null
})

const formatSize = (bytes: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

const loadActivities = async () => {
  const res = await getActivityList()
  if (res.code === 200) {
    activities.value = res.data
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMaterialList(query)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const handleDownload = async (row: any) => {
  try {
    const res = await downloadMaterial(row.id)
    const url = window.URL.createObjectURL(new Blob([res as unknown as BlobPart]))
    const link = document.createElement('a')
    link.href = url
    link.download = row.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

onMounted(() => {
  loadActivities()
  loadData()
})
</script>

<style scoped>
</style>