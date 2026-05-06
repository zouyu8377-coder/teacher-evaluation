# 教师评价考核平台 —— 项目简要说明

> 本文档由 Claude 根据当前代码库自动生成，用于快速了解项目全貌，便于后续开发维护。

---

## 一、项目基本信息

| 项目 | 值 |
|------|-----|
| 项目名称 | 教师评价考核平台 |
| 当前版本 | v1.1.9（前端 package.json / 后端 pom.xml / 部署镜像均已同步） |
| 项目路径 | `C:\TeacherEvaluation` |
| 前端路径 | `C:\TeacherEvaluation\frontend` |
| 后端路径 | `C:\TeacherEvaluation\backend` |
| 部署方式 | Docker Compose（本地 + 云服务器） |
| 生产服务器 | 124.174.17.44 |
| 镜像仓库 | `teacher-eval-cn-beijing.cr.volces.com`（火山引擎容器镜像服务） |

---

## 二、技术架构

### 2.1 后端（Backend）

| 技术 | 版本 / 说明 |
|------|------------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.2.0 |
| ORM | Spring Data JPA (Hibernate) |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 |
| 对象存储 | MinIO |
| 安全 | Spring Security + JWT (jjwt 0.12.3) |
| API 文档 | Swagger / OpenAPI (springdoc 2.3.0) |
| Excel 处理 | Apache POI 5.2.5 |
| 构建工具 | Maven |
| 源码文件数 | ~71 个 Java 文件 |

**主要包结构：**
- `config` — 配置类（安全、MinIO、Web、数据初始化）
- `controller` — REST API 控制器（10 个）
- `service` — 业务逻辑层
- `entity` — JPA 实体（11 个）
- `repository` — Spring Data JPA 仓库
- `dto` / `vo` — 数据传输对象
- `security` — JWT、认证过滤器等
- `exception` / `schedule` — 全局异常、定时任务

### 2.2 前端（Frontend）

| 技术 | 版本 / 说明 |
|------|------------|
| 框架 | Vue 3 + Vite |
| 语言 | TypeScript |
| UI 组件库 | Element Plus |
| 样式 | Tailwind CSS 3.4 |
| 状态管理 | Pinia 3 |
| 路由 | Vue Router 5 |
| HTTP 客户端 | Axios |
| 源码文件数 | ~50 个文件 |

**主要目录：**
- `views/` — 页面视图（按角色分：admin / evaluator / teacher / layout / Login.vue）
- `api/` — 接口封装（按模块拆分）
- `stores/` — Pinia 状态管理
- `router/` — 路由配置
- `components/` — 公共组件
- `types/` — TypeScript 类型定义

---

## 三、核心业务模块

平台围绕教师职称/级别晋升考核，支持 **C → B2/B1 → A2/A1** 五级晋升体系。

### 3.1 角色与权限

| 角色 | 权限概述 |
|------|---------|
| **admin（管理员）** | 用户管理、活动管理、题库管理、试卷管理、看板统计 |
| **evaluator（考核员）** | 活动评审、在线阅卷、材料审核、评分打分 |
| **teacher（教师）** | 报名活动、在线考试、提交材料、查看成绩、学习资料 |

### 3.2 核心功能模块

1. **活动/考核期管理（Activity）**
   - 创建不同级别（C / B2 / B1 / A2 / A1）的考核活动
   - 设置报名起止时间、考试时间、材料提交时间
   - 配置考核员、最大参与人数、考试时长
   - 活动状态控制（启用/禁用）、时间状态自动计算

2. **报名注册（Enrollment）**
   - 教师根据当前级别报名符合条件的下一级别考核
   - 级别晋升有前置要求（如考 B 需先通过 C）
   - 支持并列级别处理（B2/B1 同级、A2/A1 同级）

3. **在线考试（Exam）**
   - 试卷管理（关联题库）
   - 在线答题、自动计时、交卷
   - 成绩发布控制（发布后才显示答案）
   - 防止重复考试

4. **材料提交（Document）**
   - 教师上传考核相关材料（论文、证书等）
   - 支持 MinIO 对象存储
   - 考核员在线查看、审核

5. **评分评价（Evaluation）**
   - 考核员对教师进行多维度评分
   - 支持多人评审（可配置评审人数）

6. **学习资料（Learning Material）**
   - 资料上传与下载

7. **统计报表（Stats / Dashboard）**
   - 管理员看板、教师个人成绩、考核员评审记录

---

## 四、实体关系概览

| 实体 | 说明 |
|------|------|
| `User` | 用户（教师/考核员/管理员） |
| `Activity` | 考核活动/期次 |
| `PeriodEnrollment` | 报名记录（教师-活动关联） |
| `ExamPaper` | 试卷 |
| `ExamQuestion` | 试题（单选/多选/判断/填空/简答） |
| `PaperQuestion` | 试卷-试题关联（含分值、排序） |
| `ExamRecord` | 考试记录/成绩 |
| `Document` | 教师提交的材料文档 |
| `LearningMaterial` | 学习资料 |
| `Evaluation` | 评分评价记录 |

---

## 五、部署架构

### 5.1 容器组成

```
┌─────────────────────────────────────────────┐
│           Docker Compose Network            │
│  ┌─────────┐  ┌─────────┐  ┌─────────────┐ │
│  │  MySQL  │  │  Redis  │  │    MinIO    │ │
│  │  :3306  │  │  :6379  │  │ :9000/:9001 │ │
│  └────┬────┘  └────┬────┘  └──────┬──────┘ │
│       └─────────────┴──────────────┘        │
│                     │                       │
│              ┌──────┴──────┐               │
│              │   Backend   │               │
│              │   :8080     │               │
│              └──────┬──────┘               │
│                     │                       │
│              ┌──────┴──────┐               │
│              │   Frontend  │               │
│              │    :80      │               │
│              └─────────────┘               │
└─────────────────────────────────────────────┘
```

### 5.2 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Frontend | 80 | Nginx 托管的 Vue 静态资源 |
| Backend | 8080 | Spring Boot REST API |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| MinIO API | 9000 | 对象存储 API |
| MinIO Console | 9001 | 管理控制台 |

### 5.3 生产访问地址

- 前端：`http://124.174.17.44/`
- 后端 API：`http://124.174.17.44:8080`
- Swagger 文档：`http://124.174.17.44:8080/swagger-ui.html`

---

## 六、关键配置文件

| 文件 | 说明 |
|------|------|
| `backend/pom.xml` | Maven 依赖与构建配置 |
| `backend/src/main/resources/application.yml` | Spring Boot 应用配置（开发环境） |
| `frontend/package.json` | npm 依赖与脚本 |
| `frontend/vite.config.ts` | Vite 构建配置 |
| `frontend/tailwind.config.js` | Tailwind CSS 配置 |
| `docker-compose.yml` | 本地开发 Docker Compose（使用 GitHub Packages 镜像） |
| `deploy/docker-compose.yml` | 生产部署 Docker Compose（使用火山引擎镜像，带健康检查） |
| `docs/部署配置.md` | 详细部署操作手册（含 PowerShell 命令、SSH 密钥路径） |

---

## 七、测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 考核员 | evaluator1 | eval123 |
| 教师 | teacher1 | teacher123 |
| 教师 | teacher2 | teacher123 |

---

## 八、本地开发启动

### 后端
```bash
cd C:\TeacherEvaluation\backend
mvn clean spring-boot:run
```

### 前端
```bash
cd C:\TeacherEvaluation\frontend
npm install
npm run dev
```

### Docker 全栈启动
```bash
cd C:\TeacherEvaluation
docker-compose up -d
```

---

## 九、近期版本变更（根据 git 日志）

| 版本 | 主要变更 |
|------|---------|
| v1.1.7 | 前后端类型安全重构 |
| v1.1.8+ | 考核功能完善、多处问题优化、评分人数重置修复、教师报名列表过滤优化 |

---

*文档生成时间：2026-05-03*
*对应代码版本：v1.1.9*
