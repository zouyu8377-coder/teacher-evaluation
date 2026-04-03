<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(() => {
  userStore.initUser()
})
</script>

<style scoped>
/* 应用特定的样式可以放在这里 */
</style>