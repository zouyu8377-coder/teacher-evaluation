<template>
  <div class="document-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的文档</span>
          <el-button type="primary" @click="$router.push('/teacher/upload')">上传文档</el-button>
        </div>
      </template>
      
      <el-form inline>
        <el-form-item label="活动">
          <el-select v-model="query.activityId" placeholder="请选择" clearable @change="loadData">
            <el-option v-for="p in activities" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe>
        <el-table-column prop="title" label="文档标题" />
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="activityName" label="活动" />
        <el-table-column prop="createdAt" label="上传时间" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDownload(row)">下载</el-button>
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
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDocumentList, deleteDocument, downloadDocument } from '@/api/document'
import { getActivityList } from '@/api/activity'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const query = reactive({
  page: 1,
  size: 10,
  activityId: null as number | null
})

const tableData = ref<any[]>([])
const total = ref(0)
const activities = ref<any[]>([])

const loadData = async () => {
  const res = await getDocumentList({
    page: query.page,
    size: query.size,
    activityId: query.activityId || undefined,
    userId: userStore.user?.id
  })
  if (res.code === 200) {
    tableData.value = res.data.records
    total.value = res.data.total
  }
}

const loadActivities = async () => {
  const res = await getActivityList()
  if (res.code === 200) {
    activities.value = res.data
  }
}

const handleDownload = async (row: any) => {
  const res = await downloadDocument(row.id)
  const blob = new Blob([res as any])
  const link = document.createElement('a')
  link.href = window.URL.createObjectURL(blob)
  link.download = row.fileName
  link.click()
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确定要删除该文档吗？', '提示', { type: 'warning' })
  const res = await deleteDocument(row.id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    loadData()
  }
}

onMounted(() => {
  loadData()
  loadActivities()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>