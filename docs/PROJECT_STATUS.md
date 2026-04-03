# 教师评价考核平台 - 项目现状

## 项目概述

- **项目名称**: 教师评价考核平台
- **项目目标**: 为学校搭建教师评价系统，实现教师文档上传、考核员评分、教师查询成绩三大核心功能
- **项目规模**: 200-500人（教师+考核员）
- **部署方式**: Docker容器化

---

## 技术架构

| 层级 | 技术方案 |
|------|----------|
| 前端 | Vue3 + Vite + TypeScript + Element Plus + Pinia + Axios |
| 后端 | Spring Boot 3.2.0 + Java 17 + JPA + Security + JWT |
| 数据库 | MySQL 8.0 |
| 文件存储 | MinIO (S3兼容对象存储) |
| 缓存 | Redis 7 |
| 部署 | Docker + Docker Compose |

### 服务地址
| 服务 | 地址 |
|------|------|
| 前端应用 | http://localhost:5174 |
| 后端API | http://localhost:8080 |
| Swagger文档 | http://localhost:8080/swagger-ui/index.html |
| MinIO控制台 | http://localhost:9001 (minioadmin/minioadmin) |

---

## 核心功能

| 角色 | 功能 |
|------|------|
| **教师** | 报名考核周期、上传考核文档、下载学习资料、查看个人成绩 |
| **考核员** | 查看已报名教师及其文档、评分评语、提交考核结果 |
| **管理员** | 用户管理、考核周期管理、学习资料上传、系统数据统计 |

### 业务流程
1. 管理员创建并启用考核周期
2. 教师报名考核周期（报名后方可使用后续功能）
3. 管理员上传学习资料供教师下载
4. 教师上传考核文档
5. 考核员查看文档并评分
6. 教师查看考核成绩

---

## 数据库表

- `users` - 用户表
- `evaluation_periods` - 考核周期表
- `period_enrollments` - 报名记录表
- `documents` - 文档表
- `evaluations` - 考核评分表
- `learning_materials` - 学习资料表
- `sys_config` - 系统配置表

---

## 测试账号

| 角色 | 用户名 | 密码 | 部门 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 校办 |
| 考核员 | evaluator1 | eval123 | 考核组 |
| 教师 | teacher1 | teacher123 | 语文组 |
| 教师 | teacher2 | teacher123 | 数学组 |

---

## 项目结构

```
TeacherEvaluation/
├── backend/           # Spring Boot后端
├── frontend/          # Vue3前端
├── docs/              # 项目文档
├── docker-compose.yml # 容器编排
└── README.md         # 启动指南
```

---

## 启动命令

```powershell
# 启动后端+数据库+缓存+文件存储
cd C:\TeacherEvaluation
docker-compose up -d

# 启动前端
cd C:\TeacherEvaluation\frontend
npm run dev
```

---

**最后更新**: 2026年4月3日
**文档版本**: 1.0