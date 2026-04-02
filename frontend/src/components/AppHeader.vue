<template>
  <div class="header">
    <div class="header-left">
      <h2 class="title">教师评价考核系统</h2>
    </div>
    <div class="header-right">
      <el-dropdown @command="handleCommand">
        <span class="user-info">
          <el-icon><User /></el-icon>
          <span>{{ userStore.user?.realName }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/auth'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await logout()
    } catch (e) {
      // ignore
    }
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #606266;
}

.user-info:hover {
  color: #409EFF;
}
</style>