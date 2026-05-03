# 教师评价考核平台 - 项目进展文档

> 最后更新: 2026-05-02 | 当前版本: 1.1.6

---

## 一、项目概述

| 项目 | 内容 |
|------|------|
| 系统名称 | 教师评价考核平台 |
| 技术栈 | Spring Boot 3.2 + Vue 3 + TypeScript + Vite + MySQL + Redis + MinIO |
| 部署方式 | Docker Compose |
| 角色 | 教师、考核员、管理员 |
| 级别结构 | C级 → B2/B1(并行) → A2/A1(并行) |

---

## 二、版本变更记录

### v1.1.7 (2026-05-03)

**第一阶段：类型安全与接口契约**
- 消灭 Controller 中的 `Map<String, Object>`，全面替换为强类型 VO：
  - 新建 `vo` 包，创建 `PageVO<T>`、`UserVO`、`CurrentUserVO`、`MyEnrollmentVO`、`ReviewerStatVO`、`ReviewProgressVO`、`EnrollmentInfoVO`、`EnrollmentTeacherVO`、`DocumentVO`、`EvaluationVO`、`EvaluationListVO`、`EvaluationSummaryVO`、`LearningMaterialVO`
  - `AuthController`：`getCurrentUser` 返回 `CurrentUserVO`
  - `UserController`：`getUsers` 返回 `PageVO<UserVO>`，`getTeachers`/`getEvaluators`/`getUserById` 返回 `UserVO`
  - `ActivityController`：`getMyEnrollments` 返回 `MyEnrollmentVO`，`getReviewProgress` 返回 `ReviewProgressVO`，`getEnrollmentInfo` 返回 `EnrollmentInfoVO`，`getEnrollments` 返回 `EnrollmentTeacherVO`
  - `DocumentController`：`getDocuments` 返回 `PageVO<DocumentVO>`，`getDocument` 返回 `DocumentVO`
  - `EvaluationController`：`getEvaluations` 返回 `PageVO<EvaluationVO>`，`getMyEvaluations` 返回 `List<EvaluationVO>`，`getTeacherActivityEvaluations` 返回 `EvaluationListVO`，`getActivitySummary` 返回 `EvaluationSummaryVO`，`getEvaluation` 返回 `EvaluationVO`
  - `LearningMaterialController`：`getMaterials` 返回 `PageVO<LearningMaterialVO>`，`getMaterial` 返回 `LearningMaterialVO`
- 修复编译类型不匹配：`Evaluation.finalScore`/`ExamRecord.score` 统一为 `BigDecimal`，`Activity.startDate`/`endDate` 统一为 `LocalDate`

**第二阶段：状态管理与前端工程化**
- 新建 `frontend/src/api/types.ts`：统一前端 VO 类型定义（`ApiResponse<T>`、`PageResponse<T>`、`UserVO`、`MyEnrollmentVO`、`EnrollmentTeacherVO`、`EnrollmentInfoVO`、`ReviewerStatVO`、`ReviewProgressVO`、`DocumentVO`、`EvaluationVO`、`EvaluationListVO`、`EvaluationSummaryVO`、`LearningMaterialVO`）
- 新建 `frontend/src/stores/activity.ts`：Pinia store 接管活动状态（`allActivities`、`availableActivities`、`myEnrollments`、`enrollmentInfoMap`、`reviewProgressMap`、`enrollmentTeachersMap`）
- 所有 API 模块（`activity.ts`、`evaluation.ts`、`document.ts`、`user.ts`、`auth.ts`、`learningMaterial.ts`）添加泛型类型，消灭裸 `api.get/post`
- `Enrollment.vue` 迁移至 `useActivityStore`，消除 `any[]` 和裸 API 调用

### v1.1.7 (2026-05-02)

**安全加固（P0）**
- 移除所有配置文件中的默认密码/密钥回退值：`DB_PASSWORD`、`MINIO_ACCESS_KEY`/`SECRET`、`JWT_SECRET` 不再提供弱默认值
- CORS 配置收紧：从 `*` 改为读取环境变量 `APP_CORS_ORIGINS`，默认仅允许 localhost
- JWT 过滤器增加用户状态校验：禁用用户（`status=0`）的 Token 立即失效
- 新增 Token 黑名单服务（`TokenBlacklistService`）：用户登出后 Token 立即失效，内存存储 + 定时清理（内部使用），集群环境建议替换为 Redis
- 文件上传增加白名单校验：扩展名 + MIME 类型双重校验，限制 50MB，文件名安全过滤（去除路径穿越字符）
- `DocumentService.deleteDocument` 修复静默吞异常：增加错误日志记录
- `AuthController.login` 修复错误捕获过宽：`catch (Exception)` 改为 `catch (BusinessException)`，避免系统异常被误报为 401

**架构改进（P1）**
- `ActivityController` 移除直接依赖 `EvaluationRepository` / `DocumentRepository`，所有数据访问通过 Service 层
- `ActivityService` 修复 `delete()` 未调用 `validateDelete()` 的遗漏
- 抽取 `ActivityValidator` 组件：统一封装 C 级/非 C 级活动时间校验规则，消除 `create`/`update` 中 40+ 行重复代码
- `UserService` 引入 DTO：`createUser` 接收 `UserCreateDTO`，`updateUser` 接收 `UserUpdateDTO`，防止客户端传入不该更新的字段（如 `id`、`createdAt`）
- `UserController` 同步适配 DTO 入参

**前端改进**
- `ActivityManage.vue`：所有 `JSON.parse(row.reviewerIds)` 增加防御性 `try-catch`，防止后端返回异常数据时页面白屏
- `ActivityManage.vue`：`loadData` 中串行请求改为 `Promise.all` 并发加载，提升页面加载速度

**部署**
- 前后端镜像重新构建并重启

### v1.1.7 (2026-05-03)

**第一阶段：类型安全与接口契约**
- 消灭 Controller 中的 `Map<String, Object>`，全面替换为强类型 VO：
  - 新建 `vo` 包，创建 `PageVO<T>`、`UserVO`、`CurrentUserVO`、`MyEnrollmentVO`、`ReviewerStatVO`、`ReviewProgressVO`、`EnrollmentInfoVO`、`EnrollmentTeacherVO`、`DocumentVO`、`EvaluationVO`、`EvaluationListVO`、`EvaluationSummaryVO`、`LearningMaterialVO`
  - `AuthController`：`getCurrentUser` 返回 `CurrentUserVO`
  - `UserController`：`getUsers` 返回 `PageVO<UserVO>`，`getTeachers`/`getEvaluators`/`getUserById` 返回 `UserVO`
  - `ActivityController`：`getMyEnrollments` 返回 `MyEnrollmentVO`，`getReviewProgress` 返回 `ReviewProgressVO`，`getEnrollmentInfo` 返回 `EnrollmentInfoVO`，`getEnrollments` 返回 `EnrollmentTeacherVO`
  - `DocumentController`：`getDocuments` 返回 `PageVO<DocumentVO>`，`getDocument` 返回 `DocumentVO`
  - `EvaluationController`：`getEvaluations` 返回 `PageVO<EvaluationVO>`，`getMyEvaluations` 返回 `List<EvaluationVO>`，`getTeacherActivityEvaluations` 返回 `EvaluationListVO`，`getActivitySummary` 返回 `EvaluationSummaryVO`，`getEvaluation` 返回 `EvaluationVO`
  - `LearningMaterialController`：`getMaterials` 返回 `PageVO<LearningMaterialVO>`，`getMaterial` 返回 `LearningMaterialVO`
- 修复编译类型不匹配：`Evaluation.finalScore`/`ExamRecord.score` 统一为 `BigDecimal`，`Activity.startDate`/`endDate` 统一为 `LocalDate`

### v1.1.6 (2026-05-02)

**第二阶段：状态管理与前端工程化**
- 新建 `frontend/src/api/types.ts`，统一前后端类型契约（13个VO接口：Activity, MyEnrollmentVO, EnrollmentTeacherVO, EnrollmentInfoVO, ReviewerStatVO, ReviewProgressVO, DocumentVO, EvaluationVO, EvaluationListVO, EvaluationSummaryVO, LearningMaterialVO, UserVO, PageResponse）
- 更新所有 API 文件添加强类型返回注解：`activity.ts`, `evaluation.ts`, `document.ts`, `user.ts`, `auth.ts`, `learningMaterial.ts`
- 新建 `frontend/src/stores/activity.ts` Pinia store，集中管理活动状态、报名列表、加载状态和缓存（enrollmentInfo/reviewProgress/enrollmentTeachers）
- 重构 `teacher/Enrollment.vue`：
  - 从直接 API 调用迁移到 `useActivityStore`
  - 串行请求改为并行（`Promise.all`）
  - 移除组件内局部 `myActivities`/`loading`，改用 store 的 `myEnrollments`/`loading`
  - `handleEnroll` 使用 store 的 `doEnroll`，成功后自动刷新相关缓存

**后端**
- 修复教师端查不到C级考试记录（`ExamRecordService.getMyRecords` 改用 `findByTeacherId`）
- 修复调整考试分数覆盖考核员评分（`syncToEvaluation` 精确匹配 `SYSTEM_EVALUATOR_ID`）
- 修复教师看到的最终得分是原始分（`getEnrollmentInfo` 返回 `finalScore`）
- 修复系统参考评分被成绩发布误清理（发布清理和平均分计算均排除系统评分）
- 修复考试提交后端缺少时间截止校验（`submitExam` 增加 `examEnd` 校验）
- 修复单教师发布后仍可修改评分（`createOrUpdateEvaluation` 增加 `isLocked` 校验）
- 修复考核员仍可发布成绩（后端 `publishScores`/`adjustScore` 权限改为仅 `admin`）
- 修复 `getEvaluations` 双参数查询忽略 `teacherId`（改为精确分页查询）
- 修复考核员查看答卷看不到正确答案（`getExamDetail` 增加 `isEvaluatorOrAdmin` 参数）

**前端**
- 修复考试结束后仍可通过URL加载题目（`loadExamData` 增加时间窗口校验）
- 修复直接URL访问绕过时间窗口（`onMounted` 增加 `examStart`/`examEnd` 校验）
- 优化考试已结束但 `in_progress` 时的显示（按钮文案改为"考试时间已结束"）
- 修复考核员已有评分被重置为考试分（`EvaluationForm` 加载当前考核员的已有评分）
- 去除考核员页面"发布成绩"和"调整分数"按钮（仅管理员保留）
- 明确C级评分入口职责（`EvaluationForm` 提示文案优化）
- 修复报名按钮未校验报名时间窗口（`Enrollment.vue` 增加 `enrollmentStatus` 判断，报名截止/未开始时禁用并显示对应文案）

**部署**
- 优化 Docker 构建：宿主机预编译 + Docker 仅打包，构建速度从数分钟降至秒级
- 前后端镜像版本统一更新至 1.1.6

### v1.1.5 (2026-05-02)

**后端**
- `EvaluationService.publishScores()` 发布成绩前，自动清理不在当前 `reviewerIds` 配置中的非授权评分记录
- 批量发布成绩时，若报名时间与考试时间/材料提交时间均已结束，自动将活动状态设为 `closed`
- 评分提交接口加强权限校验，拒绝非配置考核员的评分请求

**部署**
- 前后端镜像版本统一更新至 1.1.5
- 修复后端 Docker 构建过慢问题（改用宿主机预编译 JAR 直接打包）

### v1.1.4 (2026-05-02)

**前端**
- 修复考核员活动列表获取评分数据的字段名：`res.data.records` → `res.data.evaluations`
- 修复教师查看自己已完成试卷时的答题正误展示权限（不再受 `isPublished` 限制）

### v1.1.3 (2026-05-02)

**前端**
- 管理员活动详情页（`admin/activities/:id`）已报名教师模块增加"各评分员打分"和"平均分"展示
- 考核员页面去除"发布成绩"按钮，仅管理员保留发布权限
- 成绩发布后，考核员评分表单提交按钮禁用并显示警告提示
- 教师 Dashboard 得分显示改为"发布后的评分员平均分"
- 教师 Dashboard 新增"对X题 / 错X题"统计（来源 `ExamRecord.correctCount` / `wrongCount`）

**后端**
- `TeacherDashboardDTO` / `TeacherDashboardService` 增加 `correctCount` / `wrongCount` 字段与计算逻辑

### v1.1.2 (2026-05-02)

**前端**
- 考核员提交评分后正确回退至 `/evaluator/activities` 列表页，保留 `activityId` 上下文参数
- 考核员活动列表仅展示**自己**的评分情况和提交时间
- 更新打分后列表状态实时同步（显示"已打分"和更新时间）

---

## 三、核心功能完成情况

| 模块 | 功能 | 状态 |
|------|------|------|
| 认证 | JWT + Spring Security 登录/鉴权 | ✅ |
| 用户管理 | 增删改查、角色分配 | ✅ |
| 考核活动 | 创建/编辑/发布/关闭 | ✅ |
| 报名系统 | 资格校验、名额限制、时间窗口 | ✅ |
| 考试系统 | 题库、组卷、自动判分、断点续考 | ✅ |
| 评分系统 | 多评分人、平均分计算、成绩发布 | ✅ |
| 评分权限 | 仅配置考核员可评分、发布后锁定 | ✅ |
| 数据清理 | 发布时自动清理非授权评分记录 | ✅ |
| 教师首页 | 考核进度、考试成绩、对/错题数 | ✅ |
| 文档管理 | MinIO 上传/下载 | ✅ |
| 等级证书 | 自动生成证书 | ⏳ 待开发 |

---

## 四、已知问题与优化项

| 优先级 | 事项 | 说明 |
|--------|------|------|
| P1 | 生成等级证书 | 活动结束后为达标教师生成 PDF 证书 |
| P1 | 敏感信息环境变量化 | JWT Secret、数据库密码等应走 `.env` |
| P2 | 用户编辑密码逻辑 | 当前逻辑需优化校验流程 |
| P2 | 前端 Docker 构建内存 | 需 Docker Desktop 内存 ≥ 6GB，否则 `npm install` 易 OOM |
| P2 | 构建优化 | Dockerfile 中 `npm install` 可改为 `npm ci` 减少内存占用 |
| P2 | Maven 依赖缓存 | 后端 Dockerfile 构建时容器内无 Maven 缓存，首次极慢 |

---

## 五、项目路径

```
C:\TeacherEvaluation
├── backend/          # Spring Boot 后端源码
├── frontend/         # Vue 3 前端源码
├── deploy/           # Docker Compose 部署配置
└── docs/             # 项目文档
```

---

## 六、部署状态

| 服务 | 版本 | 状态 |
|------|------|------|
| backend | 1.1.6 | healthy |
| frontend | 1.1.6 | healthy |
| mysql | 8.0 | healthy |
| redis | 7-alpine | healthy |
| minio | RELEASE.2024-01-16 | healthy |
