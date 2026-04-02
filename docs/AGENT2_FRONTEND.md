# Agent 2: 前端界面开发任务

## 任务目标
开发教师评价考核平台前端界面，基于 Vue3 + Element Plus。

---

## 一、技术栈

- Vue 3 (Composition API)
- Vite (构建工具)
- Element Plus (UI 组件库)
- Axios (HTTP 请求)
- Vue Router (路由管理)
- Pinia (状态管理)
- VueUse (工具函数)

---

## 二、必须完成的开发任务

### 1. 项目初始化 (Day 1)

- [ ] 创建 Vue3 + Vite 项目
- [ ] 安装依赖：
  ```bash
  npm install element-plus axios vue-router pinia @vueuse/core
  ```
- [ ] 配置 vite.config.js 代理 (代理到 localhost:8080)
- [ ] 配置 main.js 引入 Element Plus

### 2. 公共模块 (Day 1-2)

**API 封装 (src/api/)**
- [ ] index.js - Axios 实例、拦截器配置
- [ ] auth.js - 登录/登出/当前用户 API
- [ ] user.js - 用户管理 API
- [ ] document.js - 文档管理 API
- [ ] evaluation.js - 考核评分 API
- [ ] period.js - 考核周期 API

**路由配置 (src/router/)**
- [ ] index.js - 路由配置
- [ ] 路由守卫 - 登录校验、权限校验

**状态管理 (src/stores/)**
- [ ] user.js - 用户登录状态、权限信息

**通用组件**
- [ ] layout/MainLayout.vue - 整体布局 (Header + Sidebar)
- [ ] components/AppHeader.vue - 顶部导航
- [ ] components/AppSidebar.vue - 侧边栏菜单

### 3. 登录页面 (Day 2)

- [ ] Login.vue - 登录表单
  - 用户名输入框
  - 密码输入框
  - 登录按钮
  - 错误提示

### 4. 教师端 (Day 2-4)

**我的文档**
- [ ] teacher/DocumentList.vue
  - 文档列表表格 (标题、上传时间、状态)
  - 删除按钮
  - 下载按钮
  - 筛选功能 (按考核周期)

**文档上传**
- [ ] teacher/DocumentUpload.vue
  - 文件选择 (支持拖拽)
  - 文件列表展示
  - 进度条
  - 标题、描述输入
  - 考核周期选择

**我的成绩**
- [ ] teacher/MyScores.vue
  - 我的成绩列表
  - 考核周期筛选
  - 成绩详情弹窗 (评分、评语)

### 5. 考核员端 (Day 4-6)

**教师列表**
- [ ] evaluator/TeacherList.vue
  - 教师列表表格
  - 所属教研组
  - 查看文档按钮
  - 打分按钮

**文档查看**
- [ ] evaluator/DocumentView.vue
  - 查看教师上传的文档列表
  - 下载文档

**在线评分**
- [ ] evaluator/EvaluationForm.vue
  - 被打分教师信息
  - 分数输入 (0-100，支持小数)
  - 评语 textarea
  - 提交按钮
  - 历史评分查看

### 6. 管理员端 (Day 6-8)

**用户管理**
- [ ] admin/UserManage.vue
  - 用户列表表格
  - 新增用户按钮
  - 编辑用户
  - 删除用户
  - 重置密码

**考核周期管理**
- [ ] admin/PeriodManage.vue
  - 周期列表
  - 新增周期 (名称、开始/结束日期、说明)
  - 编辑周期
  - 删除周期
  - 启用/禁用周期

**数据概览**
- [ ] admin/Dashboard.vue
  - 用户统计 (教师数、考核员数)
  - 文档统计 (总上传数)
  - 考核统计 (已完成/待评分)
  - 数据卡片展示

---

## 三、页面结构

```
src/views/
├── Login.vue                    # 登录页
├── layout/
│   ├── MainLayout.vue          # 主布局
│   ├── AppHeader.vue           # 顶部导航
│   └── AppSidebar.vue          # 侧边栏
├── teacher/
│   ├── DocumentList.vue        # 我的文档
│   ├── DocumentUpload.vue      # 文档上传
│   └── MyScores.vue            # 我的成绩
├── evaluator/
│   ├── TeacherList.vue         # 教师列表
│   ├── DocumentView.vue        # 查看文档
│   └── EvaluationForm.vue      # 评分表单
└── admin/
    ├── UserManage.vue          # 用户管理
    ├── PeriodManage.vue        # 考核周期管理
    └── Dashboard.vue           # 数据概览
```

---

## 四、UI 规范

- **主色调**: 蓝色 #409EFF (Element Plus 默认)
- **布局**: 左侧固定 sidebar，右侧内容区
- **表格**: 使用 el-table，斑马纹
- **表单**: 使用 el-form，标签居右
- **按钮**: 主按钮 primary，次按钮 default
- **提示**: 使用 el-message 组件

---

## 五、权限控制

| 路由 | 角色 | 说明 |
|------|------|------|
| /login | 全部 | 登录页 |
| /teacher/* | teacher | 教师端 |
| /evaluator/* | evaluator | 考核员端 |
| /admin/* | admin | 管理员端 |
| / | 全部 | 重定向到对应角色首页 |

---

## 六、协作约定

1. 严格按照 Agent 1 提供的 API 接口开发
2. 如有接口不匹配，及时沟通调整
3. 使用中文界面文字
4. 代码提交清晰描述

---

## 七、交付要求

1. 完整的 Vue3 前端项目代码
2. 可运行的前端页面
3. 与后端 API 对接正常

---

## 八、启动命令

```powershell
cd frontend
npm install
npm run dev
```

前端启动后访问：http://localhost:5173

---

## 九、测试流程

1. 使用 admin/demo123 登录管理员端
2. 创建考核周期
3. 使用 teacher1/demo123 登录教师端，上传文档
4. 使用 evaluator1/demo123 登录考核员端，查看文档并打分
5. 使用 teacher1/demo123 登录查看成绩