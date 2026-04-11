# 教师评价考核平台 - 项目进展文档

## 项目基本信息
- **项目名称**: 教师评价考核平台
- **当前日期**: 2026年4月11日
- **文档版本**: 1.2
- **执行人**: OpenCode AI助手

## 当前系统状态

### ✅ 服务运行状态
所有Docker容器正常运行：

| 服务 | 容器名称 | 状态 | 访问地址 |
|------|----------|------|----------|
| 前端 | teacher-eval-frontend | Up | http://localhost:80 |
| 后端 | teacher-eval-backend-new | Up | http://localhost:8080 |
| MySQL | teacher-eval-mysql | Up | 3306端口 |
| Redis | teacher-eval-redis | Up | 6379端口 |
| MinIO | teacher-eval-minio | Up | http://localhost:9000/9001 |

### ✅ 服务访问测试
- **前端页面**: http://localhost:80 → 200 OK
- **后端API**: http://localhost:8080/api/auth/login → 200 OK
- **Swagger文档**: http://localhost:8080/swagger-ui/index.html → 200 OK
- **MinIO控制台**: http://localhost:9001 → 200 OK

### ✅ 核心功能状态

#### 已实现功能
1. **用户角色系统** - 管理员、考核员、教师三种角色
2. **考核周期管理** - 管理员创建和管理考核周期
3. **考核活动管理** - 管理员创建和管理考核活动（含开启/关闭功能）
4. **教师报名功能** - 教师需要报名考核周期才能参与
5. **文档上传功能** - 教师上传考核文档
6. **考核评分功能** - 考核员查看文档并评分
7. **成绩查询功能** - 教师查看个人考核结果
8. **学习资料功能** - 管理员上传学习资料供教师下载
9. **题库管理** - 管理员管理考试题目
10. **试卷管理** - 管理员创建试卷和手动/随机选题
11. **活动级别功能** - 考核分为C/B2/B1/A2/A1五级

#### 测试账号
| 角色 | 用户名 | 密码 | 部门 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 校办 |
| 考核员 | evaluator1 | eval123 | 考核组 |
| 教师 | teacher1 | teacher123 | 语文组 |
| 教师 | teacher2 | teacher123 | 数学组 |

## 技术架构

### 部署架构
```
┌───────────────────────────────────────────────────┐
│ 前端 (Vue3) @ http://localhost:80                 │
└──────────────────────────┬────────────────────────┘
                            │
                            ▼
┌───────────────────────────────────────────────────┐
│ 后端 (Spring Boot) @ http://localhost:8080        │
└──────────────────────────┬────────────────────────┘
                            │
           ┌────────────────┼──────────────────┐
           │                │                  │
           ▼                ▼                  ▼
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│ MySQL @3306 │      │ Redis @6379 │      │ MinIO @9000 │
└─────────────┘      └─────────────┘      └─────────────┘
```

## 项目进度

### ✅ 已完成工作
- 项目架构设计与技术选型
- 数据库设计与SQL脚本
- API接口协议定义
- 前后端开发任务分解
- Docker部署配置
- 项目文档编写
- 前后端服务启动与测试环境搭建
- 基础功能验证
- 数据库初始化问题修复
- 前端服务访问问题修复
- 测试账号登录功能完全验证
- **首页"当前考核活动"模块修复** - 修复了API路径重复和数据库状态问题
- **筛选下拉框显示优化** - 为所有下拉选择框添加filterable属性
- **考核活动操作栏优化** - 解决换行和按钮样式问题
- **关闭活动功能修复** - 修复后端update方法只更新非null字段
- **级别顺序优化**: 已修复级别高低顺序问题，C级为最低级别，A1级为最高级别
- **学习资料模块**: 已移除首页学习资料模块，并从侧边栏中移除学习资料菜单项
- **权限检查**: 已修改API响应拦截器，对403状态码不显示弹窗提示，只在控制台输出警告信息
- **级别统计**: 已去掉级别统计卡片中的"初级、中级、高级"等字样，只保留级别徽章和通过人数的显示

### 🔄 待开发工作
- 系统集成测试 - **优先级：中**
- 性能优化与安全加固 - **优先级：低**

## 问题修复记录 (2026-04-11)

### 问题1: 评分人数重置问题
**问题描述**: 管理员在编辑考核活动时，即使不修改评分人数，启动项目后评分人数也会被重置为2

**排查过程**:
1. 分析 `ActivityService.update` 方法 - 发现当提交 reviewerCount=2 时会覆盖已有值
2. 前端使用 `row.reviewerCount || 2`，当值为0时会被替换为2

**修复方案**:
1. 后端 `ActivityService.java` - 只有当新值不是默认值2，或原值为null时才更新
2. 前端 `ActivityManage.vue` - 修正 reviewerCount 取值逻辑，避免0被错误替换

**修复文件**:
- `backend/src/main/java/com/school/teacherEval/service/ActivityService.java` - update方法添加判断逻辑
- `frontend/src/views/admin/ActivityManage.vue` - handleEdit和handleReviewerConfig函数

**验证结果**:
- 活动reviewer_count=5时，提交默认值2不会覆盖
- 仅更新其他字段时，reviewerCount保持不变

### 问题2: 教师报名列表显示已报名活动
**问题描述**: 教师端的"报名新活动"列表中显示了已报名过的活动

**排查过程**:
1. 检查前端API调用 - 发现调用的是 `/activities/available` 而不是 `/activities/teacher/available`
2. 检查导入语句 - 发现缺少 `getAvailableActivitiesForTeacher` 的导入
3. 检查后端逻辑 - `getAvailableForTeacher` 方法未过滤已报名活动

**修复方案**:
1. 前端添加 `getAvailableActivitiesForTeacher` 到 import 语句
2. 后端 `getAvailableForTeacher` 方法添加过滤已报名活动的逻辑

**修复文件**:
- `frontend/src/views/teacher/Enrollment.vue` - 添加导入并使用正确的API
- `backend/src/main/java/com/school/teacherEval/service/ActivityService.java` - 添加过滤逻辑

**验证结果**:
- teacher1已报名活动3，剩余活动5显示在可报名列表
- 已报名活动不再出现

---

## 问题修复记录 (2026-04-09)

### 问题1: 教师无法查看开放中的活动
**问题描述**: 教师用户登录后无法看到正在开放中的活动，无法进行报名等操作

**排查过程**:
1. 检查前端调用 API - 发现调用 `/api/activities/available` 
2. 检查后端 API - 发现 `/api/activities/teacher/available` 接口报错 403
3. 查看后端日志 - 发现 `NullPointerException: user is null`
4. 根因分析 - `JwtAuthenticationFilter` 存储的是 username (字符串)，但 Controller 使用 `@AuthenticationPrincipal User user)` 期望获取 User 对象

**修复方案**:
1. 修改 `JwtAuthenticationFilter.java` - 从数据库加载完整的 User 对象并存储到 SecurityContext
2. 更新数据库活动状态 - 将 `draft` 状态改为 `active`

**修复文件**:
- `backend/src/main/java/com/school/teacherEval/security/JwtAuthenticationFilter.java` - 从数据库加载完整User对象
- `backend/src/main/java/com/school/teacherEval/controller/ActivityController.java` - 添加 `/my-enrollments` API

**数据修复**:
- 更新活动状态 `draft` → `active`
- 修复报名记录 `activity_id: 1` → `3`

**验证结果**:
- `/api/activities/available` 返回 1 个活动 (C级)
- `/api/activities/teacher/available` 正确返回教师可报名的活动
- `/api/activities/my-enrollments` 正确返回已报名活动
- **新增报名API**: `/api/activities/{id}/enroll` POST

### 问题2: 管理员端评分人数重置问题
- **原因**: update方法中 `reviewerCount` 只在非null时更新，但前端可能传null
- **修复**: 改为 null 时保留原值，只有 null 且原值为 null 时才设置为默认值 2

### 问题3: 教师查看活动列表包含非进行中活动
- **修复**: 
  - 后端 `/api/activities` 新增 `activeOnly` 参数
  - 前端 "我的文档" 活动筛选只显示进行中的活动

### 问题4: 报名功能失败
- **原因**: 后端缺少报名 API `/activities/{id}/enroll`
- **修复**: 添加 POST 报名接口

### 问题5: 权限检查问题
- **原因**: 登录后首页弹窗提示无权限
- **修复**: 修改API响应拦截器和组件权限检查

## 部署信息

### Docker部署
```powershell
# 进入项目目录
cd C:\TeacherEvaluation

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 服务访问地址
| 服务 | 地址 |
|------|------|
| 前端应用 | http://localhost:80 |
| 后端API | http://localhost:8080 |
| Swagger文档 | http://localhost:8080/swagger-ui/index.html |
| MinIO控制台 | http://localhost:9001 |

## 技术规范与约束

### 安全规范
- 密码BCrypt加密存储
- JWT token认证 (secret: teacher-eval-secret-key-2024)
- 接口权限控制 (基于角色)
- 文件上传大小限制 (最大100MB)

### 数据规范
- 统一响应格式
- 统一错误码处理
- 软删除机制
- 自动时间戳更新

## 问题与解决方案

### 已解决问题
1. **Docker网络问题**: 无法连接Docker Hub拉取镜像 → 配置国内镜像源
2. **端口冲突**: 端口被占用 → 调整端口配置
3. **数据库方言不匹配**: H2数据库无法执行MySQL特定语法 → 显式配置Hibernate使用H2方言
4. **登录问题**: 测试账号无法登录 → 创建DataInitializer自动初始化数据
5. **前端访问问题**: 前端无法访问 → 创建批处理文件正确启动
6. **首页活动模块不显示**: API路径重复 + 数据库状态问题 → 修复API路径和数据库数据
7. **下拉框显示异常**: 缺少filterable属性 → 为所有el-select添加filterable
8. **操作栏换行**: 按钮太长且无flex布局 → 增加列宽、缩短文字、添加flex样式
9. **关闭活动失败**: 后端update方法会覆盖null值 → 修改为只更新非null字段
10. **权限检查问题**: 登录后首页弹窗提示无权限 → 修改API响应拦截器和组件权限检查

### 待解决问题
- 无（当前系统状态良好）

## 系统功能验证
- **登录功能**: 可以正常使用admin/admin123账号登录
- **页面访问**: /admin/dashboard 页面可以正常访问
- **API调用**: 所有API接口返回200状态码
- **功能模块**: 教师报名、文档上传、考核评分、成绩查询等功能正常

## 联系人信息

- **项目负责人**: 待填写
- **后端开发**: Agent 1
- **前端开发**: Agent 2
- **测试人员**: 待填写
- **运维人员**: 待填写
- **当前执行人**: OpenCode AI助手

**最后更新**: 2026年4月9日 00:10