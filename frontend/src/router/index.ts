import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/',
    redirect: () => {
      const userStore = useUserStore()
      const role = userStore.user?.role
      if (role === 'admin') return '/admin/dashboard'
      if (role === 'evaluator') return '/evaluator/periods'
      if (role === 'teacher') return '/teacher/enrollment'
      return '/login'
    }
  },
  {
    path: '/teacher',
    component: () => import('@/views/layout/MainLayout.vue'),
    meta: { role: 'teacher' },
    children: [
      {
        path: 'documents',
        name: 'TeacherDocuments',
        component: () => import('@/views/teacher/DocumentList.vue')
      },
      {
        path: 'upload',
        name: 'TeacherUpload',
        component: () => import('@/views/teacher/DocumentUpload.vue')
      },
      {
        path: 'scores',
        name: 'TeacherScores',
        component: () => import('@/views/teacher/MyScores.vue')
      },
      {
        path: 'materials',
        name: 'TeacherMaterials',
        component: () => import('@/views/teacher/MaterialList.vue')
      },
      {
        path: 'enrollment',
        name: 'TeacherEnrollment',
        component: () => import('@/views/teacher/Enrollment.vue')
      }
    ]
  },
  {
    path: '/evaluator',
    component: () => import('@/views/layout/MainLayout.vue'),
    meta: { role: 'evaluator' },
    children: [
      {
        path: 'periods',
        name: 'EvaluatorPeriods',
        component: () => import('@/views/evaluator/PeriodManage.vue')
      },
      {
        path: 'materials',
        name: 'EvaluatorMaterials',
        component: () => import('@/views/evaluator/MaterialList.vue')
      },
      {
        path: 'documents/:teacherId',
        name: 'EvaluatorDocuments',
        component: () => import('@/views/evaluator/DocumentView.vue')
      },
      {
        path: 'evaluate/:teacherId',
        name: 'EvaluatorForm',
        component: () => import('@/views/evaluator/EvaluationForm.vue')
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/views/layout/MainLayout.vue'),
    meta: { role: 'admin' },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue')
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManage.vue')
      },
      {
        path: 'periods',
        name: 'AdminPeriods',
        component: () => import('@/views/admin/PeriodManage.vue')
      },
      {
        path: 'materials',
        name: 'AdminMaterials',
        component: () => import('@/views/admin/MaterialManage.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  userStore.initUser()
  
  if (to.path === '/login') {
    if (userStore.token) {
      next('/')
    } else {
      next()
    }
    return
  }
  
  if (!userStore.token) {
    next('/login')
    return
  }
  
  const requiredRole = to.meta.role as string
  if (requiredRole && userStore.user?.role !== requiredRole) {
    next('/')
    return
  }
  
  next()
})

export default router