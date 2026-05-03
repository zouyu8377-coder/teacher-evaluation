<template>
  <div class="dashboard">
    <h1 class="page-title">数据概览</h1>
    
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card group">
        <div class="stat-icon purple">
          <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">person</span>
        </div>
        <div class="stat-info">
          <p class="stat-label">教师数量</p>
          <h3 class="stat-value">{{ stats.teacherCount }}</h3>
        </div>
      </div>

      <div class="stat-card group">
        <div class="stat-icon pink">
          <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">psychology</span>
        </div>
        <div class="stat-info">
          <p class="stat-label">考核员数量</p>
          <h3 class="stat-value">{{ stats.evaluatorCount }}</h3>
        </div>
      </div>

      <div class="stat-card group">
        <div class="stat-icon blue">
          <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">description</span>
        </div>
        <div class="stat-info">
          <p class="stat-label">文档总数</p>
          <h3 class="stat-value">{{ stats.documentCount }}</h3>
        </div>
      </div>

      <div class="stat-card group">
        <div class="stat-icon green">
          <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">task_alt</span>
        </div>
        <div class="stat-info">
          <p class="stat-label">评分记录</p>
          <h3 class="stat-value">{{ stats.evaluationCount }}</h3>
        </div>
      </div>
    </div>

    <!-- 考核活动列表 -->
    <div class="content-section">
      <div class="section-header">
        <h2 class="section-title">
          <span class="title-dot purple"></span>
          当前考核活动
        </h2>
        <router-link to="/admin/activities" class="view-all-link">
          查看全部
          <span class="material-symbols-outlined">arrow_forward</span>
        </router-link>
      </div>
      <div class="activity-list">
        <div v-if="activities.length === 0" class="empty-state">
          <span class="material-symbols-outlined">event_busy</span>
          <p>暂无考核活动</p>
        </div>
        <router-link 
          v-for="activity in activities" 
          :key="activity.id" 
          :to="`/admin/activities/${activity.id}`"
          class="activity-item"
        >
          <div class="activity-level" :class="'level-' + activity.level">
            {{ activity.level }}
          </div>
          <div class="activity-info">
            <h4 class="activity-name">{{ activity.name }}</h4>
            <p class="activity-desc">{{ activity.description || '暂无描述' }}</p>
          </div>
          <span class="activity-status" :class="'status-' + activity.status">
            {{ getStatusText(activity.status) }}
          </span>
        </router-link>
      </div>
    </div>

    <!-- 通过人数统计 -->
    <div class="content-section">
      <div class="section-header">
        <h2 class="section-title">
          <span class="title-dot blue"></span>
          通过人数统计
        </h2>
      </div>
      <div class="level-stats">
        <div class="level-stat level-a1">
          <span class="level-badge">A1</span>
          <span class="level-count">{{ levelStats.A1 || 0 }}</span>
        </div>
        <div class="level-stat level-a2">
          <span class="level-badge">A2</span>
          <span class="level-count">{{ levelStats.A2 || 0 }}</span>
        </div>
        <div class="level-stat level-b1">
          <span class="level-badge">B1</span>
          <span class="level-count">{{ levelStats.B1 || 0 }}</span>
        </div>
        <div class="level-stat level-b2">
          <span class="level-badge">B2</span>
          <span class="level-count">{{ levelStats.B2 || 0 }}</span>
        </div>
        <div class="level-stat level-c">
          <span class="level-badge">C</span>
          <span class="level-count">{{ levelStats.C || 0 }}</span>
        </div>
      </div>
    </div>


  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLevelPassedCount, getActiveActivities } from '@/api/stats'
import { getUserList } from '@/api/user'
import { getEvaluationList } from '@/api/evaluation'
import { getDocumentList } from '@/api/document'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const stats = ref({
  teacherCount: 0,
  evaluatorCount: 0,
  documentCount: 0,
  evaluationCount: 0
})

interface LevelStats {
  A1: number
  A2: number
  B1: number
  B2: number
  C: number
}

const activities = ref<any[]>([])

const levelStats = ref<LevelStats>({
  A1: 0,
  A2: 0,
  B1: 0,
  B2: 0,
  C: 0
})

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    active: '进行中',
    draft: '草稿',
    closed: '已结束'
  }
  return map[status] || '未知'
}

const loadData = async () => {
  try {
    // 检查用户角色
    if (userStore.user?.role !== 'admin') {
      console.warn('无权限访问此页面')
      return
    }
    
    const [userRes, evalRes, docRes, levelRes, activitiesRes] = await Promise.all([
      getUserList({ role: 'teacher', size: 1 }).catch(() => ({ code: 500, message: '', data: null } as any)),
      getEvaluationList({ size: 1 }).catch(() => ({ code: 500, message: '', data: null } as any)),
      getDocumentList({ size: 1 }).catch(() => ({ code: 500, message: '', data: null } as any)),
      getLevelPassedCount().catch(() => ({ code: 500, data: null } as any)),
      getActiveActivities().catch(() => ({ code: 500, data: null } as any))
    ])
    
    if (userRes.code === 200) {
      stats.value.teacherCount = userRes.data?.total || 0
    }
    if (evalRes.code === 200) {
      stats.value.evaluationCount = evalRes.data?.total || 0
    }
    if (docRes.code === 200) {
      stats.value.documentCount = docRes.data?.total || 0
    }
    if (levelRes.code === 200) {
      levelStats.value = levelRes.data || { A1: 0, A2: 0, B1: 0, B2: 0, C: 0 }
    }
    if (activitiesRes.code === 200) {
      activities.value = activitiesRes.data || []
    }
    
    const evalRes2 = await getUserList({ role: 'evaluator', size: 1 }).catch(() => ({ code: 500, message: '', data: null } as any))
    if (evalRes2.code === 200) {
      stats.value.evaluatorCount = evalRes2.data?.total || 0
    }
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard {
  max-width: 1400px;
}

.page-title {
  font-family: 'Manrope', sans-serif;
  font-size: 2rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 32px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-bottom: 32px;
}

.stat-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 28px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  transition: all 0.3s ease;
  border: 1px solid #e2e8f0;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #8E2DE2 0%, #4A00E0 100%);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.stat-card:hover::before {
  transform: scaleX(1);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0,0,0,0.12);
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-icon.purple {
  background: linear-gradient(135deg, #8E2DE2 0%, #4A00E0 100%);
}

.stat-icon.pink {
  background: linear-gradient(135deg, #FF512F 0%, #DD2476 100%);
}

.stat-icon.blue {
  background: linear-gradient(135deg, #00c6ff 0%, #0072ff 100%);
}

.stat-icon.green {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.stat-icon .material-symbols-outlined {
  font-size: 32px;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 0.75rem;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0 0 4px 0;
  font-weight: 600;
}

.stat-value {
  font-family: 'Manrope', sans-serif;
  font-size: 2rem;
  font-weight: 800;
  color: #1e293b;
  margin: 0;
}

.content-section {
  background: #ffffff;
  border-radius: 20px;
  padding: 28px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  border: 1px solid #e2e8f0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  font-family: 'Manrope', sans-serif;
  font-size: 1.25rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-dot {
  width: 4px;
  height: 24px;
  border-radius: 2px;
}

.title-dot.purple {
  background: linear-gradient(135deg, #8E2DE2 0%, #4A00E0 100%);
}

.title-dot.blue {
  background: linear-gradient(135deg, #00c6ff 0%, #0072ff 100%);
}

.title-dot.green {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.view-all-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #8E2DE2;
  font-weight: 600;
  font-size: 0.875rem;
  text-decoration: none;
  transition: all 0.2s ease;
}

.view-all-link:hover {
  color: #4A00E0;
  gap: 8px;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #f8fafc;
  border-radius: 16px;
  transition: all 0.3s ease;
  border: 1px solid #e2e8f0;
  text-decoration: none;
  color: inherit;
}

.activity-item:hover {
  background: #f1f5f9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.activity-level {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.25rem;
  flex-shrink: 0;
}

.activity-level.level-C {
  background: #dcfce7;
  color: #16a34a;
}

.activity-level.level-B2 {
  background: #dbeafe;
  color: #2563eb;
}

.activity-level.level-B1 {
  background: #fed7aa;
  color: #ea580c;
}

.activity-level.level-A2 {
  background: #fce7f3;
  color: #db2777;
}

.activity-level.level-A1 {
  background: #e0e7ff;
  color: #4f46e5;
}

.activity-info {
  flex: 1;
}

.activity-name {
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px 0;
  font-size: 1rem;
}

.activity-desc {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0;
}

.activity-status {
  padding: 8px 16px;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  flex-shrink: 0;
}

.activity-status.status-active {
  background: #dcfce7;
  color: #16a34a;
}

.activity-status.status-draft {
  background: #f1f5f9;
  color: #64748b;
}

.activity-status.status-closed {
  background: #fee2e2;
  color: #dc2626;
}

.empty-state {
  text-align: center;
  padding: 60px 40px;
  color: #94a3b8;
}

.empty-state .material-symbols-outlined {
  font-size: 64px;
  margin-bottom: 12px;
}

.level-stats {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}

.level-stat {
  padding: 24px 16px;
  border-radius: 16px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  transition: all 0.3s ease;
  border: 1px solid #e2e8f0;
  position: relative;
  overflow: hidden;
}

.level-stat::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.level-stat:hover::before {
  opacity: 1;
}

.level-stat:hover {
  transform: scale(1.04);
  box-shadow: 0 8px 20px rgba(0,0,0,0.08);
}

.level-stat.level-c {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.1) 0%, rgba(34, 197, 94, 0.05) 100%);
  border-left: 4px solid #22c55e;
}

.level-stat.level-b2 {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(59, 130, 246, 0.05) 100%);
  border-left: 4px solid #3b82f6;
}

.level-stat.level-b1 {
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.1) 0%, rgba(249, 115, 22, 0.05) 100%);
  border-left: 4px solid #f97316;
}

.level-stat.level-a2 {
  background: linear-gradient(135deg, rgba(236, 72, 153, 0.1) 0%, rgba(236, 72, 153, 0.05) 100%);
  border-left: 4px solid #ec4899;
}

.level-stat.level-a1 {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(99, 102, 241, 0.05) 100%);
  border-left: 4px solid #6366f1;
}

.level-badge {
  font-family: 'Manrope', sans-serif;
  font-size: 1.75rem;
  font-weight: 800;
}

.level-stat.level-c .level-badge { color: #16a34a; }
.level-stat.level-b2 .level-badge { color: #2563eb; }
.level-stat.level-b1 .level-badge { color: #ea580c; }
.level-stat.level-a2 .level-badge { color: #db2777; }
.level-stat.level-a1 .level-badge { color: #4f46e5; }

.level-name {
  font-size: 0.75rem;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 600;
}

.level-count {
  font-family: 'Manrope', sans-serif;
  font-size: 1.75rem;
  font-weight: 800;
}

.level-stat.level-c .level-count { color: #16a34a; }
.level-stat.level-b2 .level-count { color: #2563eb; }
.level-stat.level-b1 .level-count { color: #ea580c; }
.level-stat.level-a2 .level-count { color: #db2777; }
.level-stat.level-a1 .level-count { color: #4f46e5; }

.materials-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.material-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  transition: all 0.2s ease;
  border: 1px solid #e2e8f0;
}

.material-item:hover {
  background: #f1f5f9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.material-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  flex-shrink: 0;
}

.material-icon .material-symbols-outlined {
  font-size: 24px;
}

.material-info {
  flex: 1;
}

.material-name {
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.material-desc {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0;
}

.download-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  border: none;
  background: #e2e8f0;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.download-btn:hover {
  background: #d1d5db;
  color: #4b5563;
  transform: scale(1.05);
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .level-stats {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .level-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .view-all-link {
    align-self: flex-end;
  }
}
</style>