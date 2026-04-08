<template>
  <div class="document-upload">
    <el-card>
      <template #header>
        <span>文档上传</span>
      </template>
      
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="活动" prop="activityId">
          <el-select v-model="form.activityId" placeholder="请选择活动">
            <el-option v-for="a in activities" :key="a.id" :label="`${a.level}级 - ${a.name}`" :value="a.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="文档标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文档标题" />
        </el-form-item>
        
        <el-form-item label="文档描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入文档描述" />
        </el-form-item>
        
        <el-form-item label="选择文件" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">支持 doc, docx, pdf, txt 格式</div>
            </template>
          </el-upload>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">提交</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadDocument } from '@/api/document'
import { getActivityList, getAvailableActivitiesForTeacher } from '@/api/activity'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const uploadRef = ref()
const loading = ref(false)

const form = reactive({
  activityId: null as number | null,
  title: '',
  description: '',
  file: null as File | null
})

const rules = {
  activityId: [{ required: true, message: '请选择活动', trigger: 'change' }],
  title: [{ required: true, message: '请输入文档标题', trigger: 'blur' }],
  file: [{ required: true, message: '请选择文件', trigger: 'change' }]
}

const activities = ref<any[]>([])
const fileList = ref<any[]>([])

const loadActivities = async () => {
  const res = await getAvailableActivitiesForTeacher()
  if (res.code === 200) {
    activities.value = res.data
    if (route.query.activityId) {
      form.activityId = Number(route.query.activityId)
    }
  }
}

const handleFileChange = (uploadFile: any, uploadFiles: any[]) => {
  if (uploadFile && uploadFile.raw) {
    form.file = uploadFile.raw
    fileList.value = [uploadFile]
  } else if (uploadFiles && uploadFiles.length > 0 && uploadFiles[0].raw) {
    form.file = uploadFiles[0].raw
    fileList.value = uploadFiles
  }
}

const handleFileRemove = () => {
  form.file = null
  fileList.value = []
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (!form.file) {
    ElMessage.warning('请选择文件')
    return
  }

  loading.value = true
  try {
    const formData = new FormData()
    formData.append('file', form.file)
    formData.append('activityId', String(form.activityId))
    formData.append('title', form.title)
    formData.append('description', form.description)

    const res = await uploadDocument(formData)
    if (res.code === 200) {
      ElMessage.success('上传成功')
      router.push('/teacher/documents')
    }
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadActivities()
})
</script>

<style scoped>
.document-upload {
  max-width: 600px;
}

.el-icon--upload {
  font-size: 67px;
  color: #409EFF;
  margin-bottom: 16px;
}

.el-upload__text {
  color: #606266;
  font-size: 14px;
}

.el-upload__tip {
  color: #909399;
  font-size: 12px;
  margin-top: 7px;
}
</style>