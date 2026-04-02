# 教师评价考核平台 - 项目说明文档

## 一、项目概述

### 1.1 项目背景
为学校搭建教师评价考核平台一期，主要实现：教师文档上传、考核员评分、教师查询成绩三大核心功能。

### 1.2 项目规模
- 用户数：200-500人（教师+考核员）
- 部署方式：Windows 本地测试环境 + Docker 容器化

---

## 二、技术架构

### 2.1 技术栈

| 层级 | 技术方案 |
|------|----------|
| 前端 | Vue3 + Vite + Element Plus + Axios |
| 后端 | Spring Boot 3.x + Java 17 |
| 数据库 | MySQL 8.0 |
| 文件存储 | MinIO (开源S3兼容对象存储) |
| 缓存 | Redis 7 |
| 部署 | Docker + Docker Compose |

### 2.2 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                      前端 (Vue3)                                │
│                   http://localhost:5173                         │
└─────────────────────────────┬───────────────────────────────────┘
                              │ HTTP/REST
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     后端 (Spring Boot)                          │
│                   http://localhost:8080                         │
│                   Swagger: http://localhost:8080/swagger-ui     │
└─────────────────────────────┬───────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│    MySQL      │    │    Redis      │    │    MinIO      │
│   localhost   │    │   localhost   │    │   localhost   │
│    :3306      │    │    :6379      │    │ :9000/:9001   │
└───────────────┘    └───────────────┘    └───────────────┘
```

---

## 三、数据库设计

### 3.1 核心表结构

```sql
-- 用户表
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    real_name VARCHAR(50) COMMENT '真实姓名',
    role ENUM('teacher', 'evaluator', 'admin') NOT NULL COMMENT '角色',
    department VARCHAR(100) COMMENT '部门/教研组',
    avatar VARCHAR(500) COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 考核周期表
CREATE TABLE evaluation_periods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '周期名称 如: 2024学年第一学期',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    description TEXT COMMENT '考核说明',
    status ENUM('draft', 'active', 'closed') DEFAULT 'draft' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 教师文档表
CREATE TABLE documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '上传教师ID',
    period_id BIGINT NOT NULL COMMENT '考核周期ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    file_path VARCHAR(500) NOT NULL COMMENT 'MinIO存储路径',
    file_name VARCHAR(200) NOT NULL COMMENT '原始文件名',
    file_size BIGINT COMMENT '文件大小(字节)',
    file_type VARCHAR(100) COMMENT 'MIME类型',
    description VARCHAR(500) COMMENT '文档描述',
    is_deleted TINYINT DEFAULT 0 COMMENT '软删除: 0否 1是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (period_id) REFERENCES evaluation_periods(id)
);

-- 考核评分表
CREATE TABLE evaluations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    evaluator_id BIGINT NOT NULL COMMENT '考核员ID',
    teacher_id BIGINT NOT NULL COMMENT '被考核教师ID',
    period_id BIGINT NOT NULL COMMENT '考核周期ID',
    score DECIMAL(5,2) COMMENT '评分 0-100',
    comment TEXT COMMENT '评语',
    attachments VARCHAR(1000) COMMENT '评分附件JSON',
    status ENUM('draft', 'submitted') DEFAULT 'draft' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (evaluator_id) REFERENCES users(id),
    FOREIGN KEY (teacher_id) REFERENCES users(id),
    FOREIGN KEY (period_id) REFERENCES evaluation_periods(id),
    UNIQUE KEY uk_teacher_period (teacher_id, period_id)
);

-- 系统配置表
CREATE TABLE sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value VARCHAR(500),
    description VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 3.2 初始化数据

```sql
-- 插入测试用户 (密码均为 BCrypt 加密后的 demo123)
-- admin / demo123 (管理员)
-- evaluator1 / demo123 (考核员)
-- teacher1 / demo123 (教师)
-- teacher2 / demo123 (教师)

INSERT INTO users (username, password, real_name, role, department) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'admin', '校办'),
('evaluator1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张考核', 'evaluator', '考核组'),
('teacher1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李老师', 'teacher', '语文组'),
('teacher2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '王老师', 'teacher', '数学组');

-- 插入考核周期
INSERT INTO evaluation_periods (name, start_date, end_date, description, status) VALUES
('2024学年第一学期', '2024-09-01', '2025-01-31', '2024学年第一学期教师考核', 'active');
```

---

## 四、API 接口规范

### 4.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}

# 错误响应
{
  "code": 400,
  "message": "错误描述",
  "data": null
}
```

### 4.2 认证接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/auth/login | POST | 用户登录 | 公开 |
| /api/auth/logout | POST | 退出登录 | 登录用户 |
| /api/auth/current | GET | 获取当前用户 | 登录用户 |

### 4.3 用户管理

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/users | GET | 用户列表 | 管理员 |
| /api/users | POST | 创建用户 | 管理员 |
| /api/users/{id} | PUT | 更新用户 | 管理员 |
| /api/users/{id} | DELETE | 删除用户 | 管理员 |
| /api/users/teachers | GET | 教师列表 | 考核员/管理员 |

### 4.4 考核周期

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/periods | GET | 周期列表 | 登录用户 |
| /api/periods | POST | 创建周期 | 管理员 |
| /api/periods/{id} | PUT | 更新周期 | 管理员 |
| /api/periods/{id} | DELETE | 删除周期 | 管理员 |
| /api/periods/active | GET | 当前 active 周期 | 登录用户 |

### 4.5 文档管理

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/documents | GET | 文档列表(可筛选) | 登录用户 |
| /api/documents | POST | 上传文档 | 教师 |
| /api/documents/{id} | GET | 文档详情 | 上传者/考核员/管理员 |
| /api/documents/{id} | PUT | 更新文档信息 | 上传者 |
| /api/documents/{id} | DELETE | 删除文档 | 上传者/管理员 |
| /api/documents/{id}/download | GET | 下载文档 | 上传者/考核员/管理员 |

### 4.6 考核评分

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/evaluations | GET | 评分列表 | 考核员/管理员 |
| /api/evaluations | POST | 提交评分 | 考核员 |
| /api/evaluations/{id} | GET | 评分详情 | 考核员/管理员 |
| /api/evaluations/teacher/{teacherId} | GET | 教师成绩 | 教师本人/考核员/管理员 |
| /api/evaluations/teacher/{teacherId}/period/{periodId} | GET | 教师某周期成绩 | 教师本人/考核员/管理员 |

### 4.7 统计报表

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/stats/overview | GET | 整体数据概览 | 管理员 |
| /api/stats/period/{periodId} | GET | 某周期统计 | 管理员/考核员 |

---

## 五、Agent 分工说明

### Agent 1: 后端服务开发

#### 职责范围
1. **项目初始化**
   - 创建 Spring Boot 项目结构
   - 配置 Maven 依赖 (Spring Web, Spring Data JPA, Spring Security, MySQL, Redis, MinIO)
   - 配置 application.yml 多环境配置

2. **基础设施**
   - MySQL 数据库连接配置
   - Redis 缓存配置
   - MinIO 文件存储配置
   - JWT 认证配置
   - CORS 跨域配置

3. **核心模块开发**
   - 用户模块 (User Entity, Repository, Service, Controller)
   - 考核周期模块 (EvaluationPeriod)
   - 文档上传模块 (Document + MinIO 集成)
   - 考核评分模块 (Evaluation)
   - 认证授权模块 (Security + JWT Filter)

4. **部署配置**
   - 编写 Dockerfile
   - 编写 docker-compose.yml
   - 编写数据库初始化 SQL 脚本

#### 交付物
```
backend/
├── src/main/java/com/school/teacherEval/
│   ├── config/          # 配置类
│   ├── controller/      # 控制器
│   ├── service/         # 业务逻辑
│   ├── repository/      # 数据访问
│   ├── entity/          # 实体类
│   ├── dto/             # 数据传输对象
│   ├── security/        # 安全配置
│   └── TeacherEvalApplication.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-prod.yml
├── pom.xml
├── Dockerfile
└── target/              # 编译输出
```

#### 开发规范
- 使用 RESTful API 设计规范
- 分层架构：Controller → Service → Repository
- 所有接口返回统一 ResponseBody
- 文件上传使用 MinIO SDK
- 密码使用 BCrypt 加密存储

---

### Agent 2: 前端界面开发

#### 职责范围
1. **项目初始化**
   - 创建 Vue3 + Vite 项目
   - 安装 Element Plus、Axios、Vue Router、Pinia
   - 配置 Vite 代理 (对接后端 8080 端口)

2. **公共组件**
   - 请求封装 (Axios + 拦截器)
   - 路由守卫 (权限校验)
   - 状态管理 (Pinia 用户状态)
   - 统一布局 (Header + Sidebar + Content)

3. **页面开发**
   - 登录页
   - 教师端：我的文档、文档上传、我的成绩
   - 考核员端：教师列表、文档查看、在线评分
   - 管理员端：用户管理、考核周期管理、数据概览

4. **UI 规范**
   - 使用 Element Plus 组件
   - 主色调：蓝色 (#409EFF)
   - 响应式布局

#### 交付物
```
frontend/
├── src/
│   ├── api/             # API 请求封装
│   │   ├── index.js     # Axios 实例
│   │   ├── auth.js      # 认证相关
│   │   ├── user.js      # 用户管理
│   │   ├── document.js  # 文档管理
│   │   └── evaluation.js# 考核评分
│   ├── views/           # 页面组件
│   │   ├── Login.vue
│   │   ├── layout/      # 布局组件
│   │   ├── teacher/     # 教师端
│   │   ├── evaluator/   # 考核员端
│   │   └── admin/       # 管理员端
│   ├── router/          # 路由配置
│   ├── stores/          # Pinia 状态
│   ├── App.vue
│   └── main.js
├── package.json
└── vite.config.js
```

---

## 六、协作约定

### 6.1 接口联调

| 阶段 | 说明 |
|------|------|
| Phase 1 | Agent 1 完成基础框架后，输出 API 接口文档给 Agent 2 |
| Phase 2 | Agent 2 按接口文档开发前端，后端 API 同步开发 |
| Phase 3 | 双方对照接口文档联调，修复不匹配问题 |

### 6.2 代码规范

- 后端：遵循 Google Java Style Guide
- 前端：遵循 Vue 3 官方风格指南
- 命名：驼峰命名法 (camelCase)
- 提交：使用英文描述提交信息

### 6.3 测试数据

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | demo123 |
| 考核员 | evaluator1 | demo123 |
| 教师 | teacher1 | demo123 |
| 教师 | teacher2 | demo123 |

---

## 七、启动指南

### 7.1 前置条件

已在 Windows 安装：
- Docker Desktop
- JDK 17+
- Maven 3.8+
- Node.js 18+

### 7.2 启动步骤

```powershell
# 1. 启动 Docker 服务
docker network create teacher-eval-network
docker run -d --name mysql --network teacher-eval-network -e MYSQL_ROOT_PASSWORD=root123 -e MYSQL_DATABASE=teacher_eval -p 3306:3306 mysql:8.0
docker run -d --name redis --network teacher-eval-network -p 6379:6379 redis:7-alpine
docker run -d --name minio --network teacher-eval-network -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin -p 9000:9000 -p 9001:9001 minio/minio server /data --console-address ":9001"

# 2. 导入数据库
# (Agent 1 提供 SQL 脚本后执行)

# 3. 启动后端 (Agent 1)
cd backend
mvn clean package
java -jar target/teacher-eval-1.0.0.jar --spring.profiles.active=dev

# 4. 启动前端 (Agent 2)
cd frontend
npm install
npm run dev
```

### 7.3 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端 | http://localhost:8080 |
| MinIO | http://localhost:9001 |

---

## 八、注意事项

1. **密码加密**：Demo 密码均为 BCrypt 加密，示例密码 `demo123`
2. **文件大小**：限制单文件最大 100MB
3. **状态管理**：使用 Pinia 持久化用户登录状态
4. **安全**：生产环境需修改 JWT secret 和数据库密码
5. **协作**：两 Agent 保持每日沟通，确保接口匹配

---

## 九、联系人

- 项目负责人：[待填写]
- 后端开发：Agent 1
- 前端开发：Agent 2