<template>
  <div class="main-layout">
    <AppSidebar />
    <div class="main-content-wrapper">
      <AppHeader />
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'
</script>

<style scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
  background: #f7f9fc;
}

.main-content-wrapper {
  flex: 1;
  margin-left: 260px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f7f9fc;
}

.main-content {
  flex: 1;
  padding: 32px;
  overflow-x: hidden;
  background: #f7f9fc;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content-wrapper {
    margin-left: 0;
  }
  
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