<template>
  <div class="sidebar">
    <div class="logo">
      <h1>考核系统</h1>
    </div>
    <el-menu
      :default-active="activeMenu"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409EFF"
      router
    >
      <template v-if="userStore.user?.role === 'teacher'">
        <el-menu-item index="/teacher/documents">
          <el-icon><Document /></el-icon>
          <span>我的文档</span>
        </el-menu-item>
        <el-menu-item index="/teacher/upload">
          <el-icon><Upload /></el-icon>
          <span>文档上传</span>
        </el-menu-item>
        <el-menu-item index="/teacher/scores">
          <el-icon><DataAnalysis /></el-icon>
          <span>我的成绩</span>
        </el-menu-item>
      </template>
      <template v-else-if="userStore.user?.role === 'evaluator'">
        <el-menu-item index="/evaluator/teachers">
          <el-icon><User /></el-icon>
          <span>教师列表</span>
        </el-menu-item>
      </template>
      <template v-else-if="userStore.user?.role === 'admin'">
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/periods">
          <el-icon><Calendar /></el-icon>
          <span>考核周期</span>
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Document, Upload, DataAnalysis, User, Calendar } from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
</script>

<style scoped>
.sidebar {
  height: 100vh;
  background-color: #304156;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo h1 {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.el-menu {
  border: none;
}
</style>