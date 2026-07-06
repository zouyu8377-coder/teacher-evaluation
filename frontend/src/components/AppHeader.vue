<template>
  <header class="app-header">
    <div class="header-search">
      <span class="material-symbols-outlined search-icon">search</span>
      <input
        type="text"
        class="search-input"
        placeholder="搜索教师、考核记录..."
        v-model="searchKeyword"
      />
    </div>
    
    <div class="header-actions">
      <button class="icon-btn">
        <span class="material-symbols-outlined">notifications</span>
        <span class="notification-dot"></span>
      </button>
      <button class="icon-btn">
        <span class="material-symbols-outlined">help_outline</span>
      </button>
      
      <div class="header-divider"></div>
      
      <el-dropdown @command="handleCommand" trigger="click">
        <div class="user-dropdown">
          <div class="user-info">
            <p class="user-name">{{ userStore.user?.realName }}</p>
            <p class="user-role">{{ roleText }}</p>
          </div>
          <img :src="userAvatar" alt="头像" class="user-avatar" />
          <span class="material-symbols-outlined dropdown-arrow">expand_more</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu class="header-dropdown">
            <el-dropdown-item command="change-password">
              <span class="material-symbols-outlined">lock_reset</span>
              修改密码
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <span class="material-symbols-outlined">logout</span>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>

  <el-dialog v-model="passwordDialogVisible" title="修改密码" width="420px" @closed="resetPasswordForm">
    <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="96px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="passwordChanging" @click="submitPasswordChange">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { changePassword, logout } from '@/api/auth'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()
const searchKeyword = ref('')
const passwordDialogVisible = ref(false)
const passwordChanging = ref(false)
const passwordFormRef = ref()

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '新密码长度需为6-64位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

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

const handleCommand = async (command: string) => {
  if (command === 'change-password') {
    passwordDialogVisible.value = true
    return
  }

  if (command === 'logout') {
    try {
      await logout()
    } catch (e) {
      console.warn('登出API调用失败:', e)
    }
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

const submitPasswordChange = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  passwordChanging.value = true
  try {
    const res = await changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    if (res.code === 200) {
      ElMessage.success('密码修改成功')
      passwordDialogVisible.value = false
    } else {
      ElMessage.error(res.message || '密码修改失败')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || e.message || '密码修改失败')
  } finally {
    passwordChanging.value = false
  }
}
</script>

<style scoped>
.app-header {
  height: 64px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  border-bottom: 1px solid #e2e8f0;
  position: sticky;
  top: 0;
  z-index: 50;
}

.header-search {
  position: relative;
  width: 100%;
  max-width: 400px;
}

.search-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 20px;
}

.search-input {
  width: 100%;
  height: 40px;
  background: #f1f5f9;
  border: none;
  border-radius: 9999px;
  padding: 0 16px 0 48px;
  font-size: 0.875rem;
  color: #1e293b;
  outline: none;
  transition: all 0.2s ease;
}

.search-input::placeholder {
  color: #94a3b8;
}

.search-input:focus {
  background: #e2e8f0;
  box-shadow: 0 0 0 2px #c7d2fe;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.2s ease;
}

.icon-btn:hover {
  background: #f1f5f9;
  color: #4f46e5;
}

.notification-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 8px;
  height: 8px;
  background: #ec4899;
  border-radius: 50%;
  border: 2px solid white;
}

.header-divider {
  width: 1px;
  height: 32px;
  background: #e2e8f0;
  margin: 0 8px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-dropdown:hover {
  background: #f1f5f9;
}

.user-info {
  text-align: right;
}

.user-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.user-role {
  font-size: 0.6875rem;
  color: #94a3b8;
  margin: 0;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid #c7d2fe;
}

.dropdown-arrow {
  color: #94a3b8;
  font-size: 20px;
}

:deep(.header-dropdown) {
  padding: 8px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

:deep(.header-dropdown .el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 0.875rem;
}

:deep(.header-dropdown .el-dropdown-menu__item .material-symbols-outlined) {
  font-size: 18px;
  color: #64748b;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .app-header {
    padding: 0 20px;
  }

  .header-search {
    max-width: 300px;
  }
}

@media (max-width: 640px) {
  .app-header {
    padding: 0 16px;
  }
  
  .header-search {
    max-width: 200px;
  }
  
  .search-input {
    font-size: 0.75rem;
    padding: 0 12px 0 40px;
  }
  
  .icon-btn {
    width: 36px;
    height: 36px;
  }
}
</style>
