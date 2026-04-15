<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
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
      
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="login-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large">
            <template #prefix>
              <span class="material-symbols-outlined">person</span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" @keyup.enter="handleLogin">
            <template #prefix>
              <span class="material-symbols-outlined">key</span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%;" :loading="loading" @click="handleLogin" size="large">
            <span v-if="!loading">登录</span>
            <span v-else>登录中...</span>
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="tips">
        <p>测试账号：</p>
        <p>管理员: admin / admin123</p>
        <p>考核员: evaluator1 / eval123</p>
        <p>教师: teacher1 / teacher123</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form.username, form.password)
    if (res.code === 200) {
      userStore.setToken(res.data.token)
      userStore.setUser(res.data.user)
      ElMessage.success('登录成功')

      const role = res.data.user.role
      if (role === 'admin') router.push('/admin/dashboard')
      else if (role === 'evaluator') router.push('/evaluator/activities')
      else if (role === 'teacher') router.push('/teacher/dashboard')
      else router.push('/')
    } else {
      ElMessage.error(res.message || '用户名或密码错误，请重试')
    }
  } catch (e: any) {
    const errorMsg = e.response?.data?.message || e.message || '用户名或密码错误，请重试'
    ElMessage.error(errorMsg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f7f9fc;
  padding: 20px;
}

.login-card {
  background: #ffffff;
  border-radius: 24px;
  padding: 40px 30px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.login-card:hover {
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.login-header {
  margin-bottom: 32px;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: center;
}

.logo-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #8E2DE2 0%, #4A00E0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 24px rgba(74, 0, 224, 0.3);
}

.logo-icon .material-symbols-outlined {
  font-size: 28px;
  font-variation-settings: 'FILL' 1;
}

.logo-text h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  background: linear-gradient(135deg, #8E2DE2 0%, #4A00E0 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.logo-text p {
  font-size: 0.75rem;
  color: #94a3b8;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.login-form {
  margin-bottom: 24px;
}

.tips {
  margin-top: 20px;
  padding: 16px;
  background: #f2f4f7;
  border-radius: 12px;
  font-size: 12px;
  color: #64748b;
}

.tips p {
  margin: 4px 0;
}

/* Element Plus 样式覆盖 */
:deep(.el-input__wrapper) {
  border-radius: 12px;
  background: #f7f9fc;
  border: 1px solid #e2e8f0;
  box-shadow: none;
  transition: all 0.2s ease;
}

:deep(.el-input__wrapper:hover) {
  background: #f1f5f9;
  border-color: #c7d2fe;
}

:deep(.el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: #8E2DE2;
  box-shadow: 0 0 0 4px rgba(142, 45, 226, 0.1);
}

:deep(.el-button) {
  background: linear-gradient(135deg, #8E2DE2 0%, #4A00E0 100%);
  border: none;
  border-radius: 12px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(74, 0, 224, 0.3);
  transition: all 0.2s ease;
}

:deep(.el-button:hover) {
  background: linear-gradient(135deg, #a855f7 0%, #6366f1 100%);
  box-shadow: 0 8px 20px rgba(74, 0, 224, 0.4);
  transform: translateY(-1px);
}

:deep(.el-button:active) {
  transform: translateY(0);
}
</style>