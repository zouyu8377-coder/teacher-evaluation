<template>
  <div class="main-layout">
    <AppSidebar v-if="userStore.user" />
    <div class="main-content-wrapper">
      <AppHeader />
      <main class="main-content">
        <router-view v-if="userStore.user" />
        <div v-else class="loading-container">
          <el-loading text="用户信息加载中..." />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
  background: #f7f9fc;
}

.main-content-wrapper {
  flex: 1;
  margin-left: 260px; /* 永远保持左边距等于侧边栏宽度，内容不会跑到侧边栏下面 */
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f7f9fc;
  min-width: 800px; /* 最小宽度保证内容不会过度压缩，按钮可见 */
}

.main-content {
  flex: 1;
  padding: 32px;
  overflow-x: auto; /* 内容超出时显示横向滚动条，不会压缩 */
  background: #f7f9fc;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    padding: 24px;
  }
}

@media (max-width: 640px) {
  .main-content {
    padding: 16px;
  }
}
</style>