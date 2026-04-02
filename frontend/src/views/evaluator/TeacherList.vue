<template>
  <div class="teacher-list">
    <el-card>
      <template #header>
        <span>教师列表</span>
      </template>
      
      <el-table :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="department" label="所属教研组" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDocuments(row)">查看文档</el-button>
            <el-button type="success" link @click="goEvaluate(row)">打分</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTeachers } from '@/api/user'

const router = useRouter()
const tableData = ref<any[]>([])

const loadData = async () => {
  const res = await getTeachers()
  if (res.code === 200) {
    tableData.value = res.data
  }
}

const viewDocuments = (row: any) => {
  router.push(`/evaluator/documents/${row.id}`)
}

const goEvaluate = (row: any) => {
  router.push(`/evaluator/evaluate/${row.id}`)
}

onMounted(() => {
  loadData()
})
</script>