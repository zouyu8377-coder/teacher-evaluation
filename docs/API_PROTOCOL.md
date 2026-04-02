# API 联调协议

## 一、基础协议

### 1.1 基础信息

| 项目 | 值 |
|------|-----|
| 基础URL | http://localhost:8080 |
| 前端代理 | http://localhost:5173 → 代理到 8080 |
| 协议 | HTTP REST |
| 字符编码 | UTF-8 |
| Content-Type | application/json |

### 1.2 请求头 (Headers)

```http
Content-Type: application/json
Authorization: Bearer <token>
```

### 1.3 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 1.4 错误码定义

| code | message | 说明 |
|------|---------|------|
| 200 | success | 成功 |
| 400 | Bad Request | 请求参数错误 |
| 401 | Unauthorized | 未登录或token过期 |
| 403 | Forbidden | 无权限 |
| 404 | Not Found | 资源不存在 |
| 500 | Internal Server Error | 服务器内部错误 |

---

## 二、认证接口

### 2.1 登录

**请求**
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "demo123"
}
```

**响应 (成功)**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "realName": "系统管理员",
      "role": "admin",
      "department": "校办"
    }
  }
}
```

**响应 (失败)**
```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

### 2.2 获取当前用户

**请求**
```http
GET /api/auth/current
Authorization: Bearer <token>
```

**响应**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "系统管理员",
    "role": "admin",
    "department": "校办"
  }
}
```

### 2.3 登出

**请求**
```http
POST /api/auth/logout
Authorization: Bearer <token>
```

**响应**
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 三、用户管理

### 3.1 用户列表

**请求**
```http
GET /api/users?page=1&size=10&role=teacher&keyword=张
Authorization: Bearer <token>
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页条数，默认10 |
| role | string | 否 | 角色筛选: teacher/evaluator/admin |
| keyword | string | 否 | 搜索关键词(用户名/姓名) |

**响应**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 3,
        "username": "teacher1",
        "realName": "李老师",
        "role": "teacher",
        "department": "语文组",
        "status": 1,
        "createdAt": "2024-01-01 10:00:00"
      }
    ],
    "total": 50,
    "page": 1,
    "size": 10
  }
}
```

### 3.2 创建用户

**请求**
```http
POST /api/users
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "teacher3",
  "password": "demo123",
  "realName": "王老师",
  "role": "teacher",
  "department": "数学组"
}
```

**响应**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 4,
    "username": "teacher3"
  }
}
```

### 3.3 教师列表（考核员使用）

**请求**
```http
GET /api/users/teachers
Authorization: Bearer <token>
```

**响应**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 3,
      "realName": "李老师",
      "department": "语文组"
    },
    {
      "id": 4,
      "realName": "王老师", 
      "department": "数学组"
    }
  ]
}
```

---

## 四、考核周期

### 4.1 周期列表

**请求**
```http
GET /api/periods
Authorization: Bearer <token>
```

**响应**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "2024学年第一学期",
      "startDate": "2024-09-01",
      "endDate": "2025-01-31",
      "description": "2024学年第一学期教师考核",
      "status": "active"
    }
  ]
}
```

### 4.2 当前活跃周期

**请求**
```http
GET /api/periods/active
Authorization: Bearer <token>
```

---

## 五、文档管理

### 5.1 文档列表

**请求**
```http
GET /api/documents?page=1&size=10&periodId=1
Authorization: Bearer <token>
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |
| periodId | long | 否 | 考核周期ID |
| userId | long | 否 | 教师ID (考核员查看指定教师的文档) |

**响应**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "userId": 3,
        "realName": "李老师",
        "periodId": 1,
        "periodName": "2024学年第一学期",
        "title": "教学工作计划",
        "fileName": "plan.docx",
        "fileSize": 1024000,
        "fileType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "description": "本学期教学工作计划",
        "createdAt": "2024-09-15 10:30:00"
      }
    ],
    "total": 20,
    "page": 1,
    "size": 10
  }
}
```

### 5.2 上传文档

**请求**
```http
POST /api/documents
Authorization: Bearer <token>
Content-Type: multipart/form-data

{
  "file": <binary>,
  "periodId": 1,
  "title": "教学工作计划",
  "description": "本学期教学工作计划"
}
```

**说明**
- 文件通过 form-data 上传，字段名为 `file`
- 其他字段为 form-data 字符串字段

**响应**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "教学工作计划",
    "fileName": "plan.docx",
    "fileSize": 1024000
  }
}
```

### 5.3 删除文档

**请求**
```http
DELETE /api/documents/{id}
Authorization: Bearer <token>
```

**响应**
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

### 5.4 下载文档

**请求**
```http
GET /api/documents/{id}/download
Authorization: Bearer <token>
```

**响应**
- 返回文件流，Content-Type 为文件的 MIME 类型
- Content-Disposition: attachment; filename="xxx.docx"

---

## 六、考核评分

### 6.1 评分列表

**请求**
```http
GET /api/evaluations?page=1&size=10&periodId=1
Authorization: Bearer <token>
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| periodId | long | 否 | 考核周期ID |
| teacherId | long | 否 | 教师ID |

**响应**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "evaluatorId": 2,
        "evaluatorName": "张考核",
        "teacherId": 3,
        "teacherName": "李老师",
        "periodId": 1,
        "periodName": "2024学年第一学期",
        "score": 95.5,
        "comment": "教学计划详细，可操作性强",
        "status": "submitted",
        "createdAt": "2024-10-01 15:30:00"
      }
    ],
    "total": 10,
    "page": 1,
    "size": 10
  }
}
```

### 6.2 提交评分

**请求**
```http
POST /api/evaluations
Authorization: Bearer <token>
Content-Type: application/json

{
  "teacherId": 3,
  "periodId": 1,
  "score": 95.5,
  "comment": "教学计划详细，可操作性强"
}
```

**说明**
- score 为数字，支持小数，如 95.5
- comment 为评语，最多 2000 字符

**响应**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "status": "submitted"
  }
}
```

### 6.3 教师查看自己的成绩

**请求**
```http
GET /api/evaluations/teacher/me?periodId=1
Authorization: Bearer <token>
```

**响应**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "periodName": "2024学年第一学期",
      "score": 95.5,
      "comment": "教学计划详细，可操作性强",
      "evaluatorName": "张考核",
      "createdAt": "2024-10-01 15:30:00"
    }
  ]
}
```

---

## 七、通用约定

### 7.1 分页响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

### 7.2 列表响应格式（不分页）

```json
{
  "code": 200,
  "message": "success",
  "data": [
    { ... },
    { ... }
  ]
}
```

### 7.3 树启动

- 开发环境：dev
- 生产环境：prod

---

## 八、前端调用示例

### 8.1 Axios 配置 (src/api/index.js)

```javascript
import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器 - 添加token
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 - 处理错误
api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
```

### 8.2 登录示例

```javascript
import api from './index'

// 登录
const login = async (username, password) => {
  const res = await api.post('/auth/login', { username, password })
  if (res.code === 200) {
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data.user))
  }
  return res
}

// 获取当前用户
const getCurrentUser = () => {
  return api.get('/auth/current')
}
```

---

## 九、测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | demo123 |
| 考核员 | evaluator1 | demo123 |
| 教师 | teacher1 | demo123 |
| 教师 | teacher2 | demo123 |

---

## 十、注意事项

1. **文件上传**：使用 FormData 格式，不要用 JSON
2. **文件下载**：返回二进制流，前端需要处理 blob
3. **权限控制**：后端会根据 role 返回 403，前端需跳转
4. **Token**：前端存储在 localStorage，退出时清除
5. **时间格式**：统一使用 "YYYY-MM-DD HH:mm:ss"