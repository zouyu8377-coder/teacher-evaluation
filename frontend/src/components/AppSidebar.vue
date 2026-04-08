<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="logo-container">
        <div class="logo-icon">
          <span class="material-symbols-outlined">auto_awesome</span>
        </div>
        <div class="logo-text">
          <h1 class="font-headline">教师评价</h1>
          <p>考核管理平台</p>
        </div>
      </div>
    </div>
    
    <nav class="sidebar-nav">
      <!-- 管理员菜单 -->
      <template v-if="userStore.user?.role === 'admin'">
        <router-link 
          v-for="item in adminMenus" 
          :key="item.path"
          :to="item.path" 
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          @click="navigate(item.path)"
        >
          <span class="material-symbols-outlined">{{ item.icon }}</span>
          <span class="nav-label">{{ item.label }}</span>
        </router-link>
      </template>
      
      <!-- 考核员菜单 -->
      <template v-else-if="userStore.user?.role === 'evaluator'">
        <router-link 
          v-for="item in evaluatorMenus" 
          :key="item.path"
          :to="item.path" 
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          @click="navigate(item.path)"
        >
          <span class="material-symbols-outlined">{{ item.icon }}</span>
          <span class="nav-label">{{ item.label }}</span>
        </router-link>
      </template>
      
      <!-- 教师菜单 -->
      <template v-else-if="userStore.user?.role === 'teacher'">
        <router-link 
          v-for="item in teacherMenus" 
          :key="item.path"
          :to="item.path" 
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          @click="navigate(item.path)"
        >
          <span class="material-symbols-outlined">{{ item.icon }}</span>
          <span class="nav-label">{{ item.label }}</span>
        </router-link>
      </template>
    </nav>
    
    <div class="sidebar-footer">
      <div class="user-info">
        <img :src="userAvatar" alt="用户头像" class="user-avatar" />
        <div class="user-details">
          <p class="user-name">{{ userStore.user?.realName || userStore.user?.username }}</p>
          <p class="user-role">{{ roleText }}</p>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 检查当前路由是否匹配菜单项
const isActive = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}

// 导航方法
const navigate = (path: string) => {
  router.push(path)
}

const teacherMenus = [
  { path: '/teacher/enrollment', label: '我的考核', icon: 'calendar_month' },
  { path: '/teacher/documents', label: '我的文档', icon: 'description' },
  { path: '/teacher/upload', label: '文档上传', icon: 'upload' },
  { path: '/teacher/scores', label: '我的成绩', icon: 'insights' }
]

const evaluatorMenus = [
  { path: '/evaluator/activities', label: '考核活动', icon: 'assessment' },
  { path: '/evaluator/exams', label: '考试记录', icon: 'fact_check' }
]

const adminMenus = [
  { path: '/admin/dashboard', label: '数据概览', icon: 'dashboard' },
  { path: '/admin/users', label: '用户管理', icon: 'group' },
  { path: '/admin/activities', label: '考核活动', icon: 'trending_up' },
  { path: '/admin/questions', label: '题目管理', icon: 'psychology' },
  { path: '/admin/papers', label: '试卷管理', icon: 'assessment' }
]

const roleText = computed(() => {
  const roleMap: Record<string, string> = {
    admin: '管理员',
    evaluator: '考核员',
    teacher: '教师'
  }
  return roleMap[userStore.user?.role || ''] || ''
})

const userAvatar = computed(() => {
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(userStore.user?.realName || 'U')}&background=4a00e0&color=fff`
})
</script>

<style scoped>
.sidebar {
  width: 260px;
  height: 100vh;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 100;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
}

.sidebar-header {
  padding: 24px 20px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #8E2DE2 0%, #4A00E0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(74, 0, 224, 0.3);
}

.logo-icon .material-symbols-outlined {
  font-size: 20px;
  font-variation-settings: 'FILL' 1;
}

.logo-text h1 {
  font-size: 1rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  background: linear-gradient(135deg, #8E2DE2 0%, #4A00E0 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.logo-text p {
  font-size: 0.625rem;
  color: #94a3b8;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  color: #64748b;
  text-decoration: none;
  transition: all 0.2s ease;
  margin-bottom: 4px;
  font-size: 0.875rem;
  font-weight: 500;
  position: relative;
}

.nav-item:hover {
  background: #f1f5f9;
  color: #4f46e5;
}

.nav-item.active {
  background: #eef2ff;
  color: #4f46e5;
  font-weight: 600;
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 24px;
  background: linear-gradient(135deg, #8E2DE2 0%, #4A00E0 100%);
  border-radius: 0 4px 4px 0;
}

.nav-icon {
  font-size: 20px;
}

.nav-label {
  font-family: 'Inter', sans-serif;
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid #c7d2fe;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 0.6875rem;
  color: #94a3b8;
  margin: 0;
}
</style>
