# Agent 1: 后端服务开发任务

## 任务目标
开发教师评价考核平台后端服务，提供完整的 RESTful API。

---

## 一、技术栈

- Spring Boot 3.x
- Java 17
- Spring Data JPA (MySQL)
- Spring Security + JWT
- Spring Data Redis
- MinIO SDK (文件存储)
- Maven

---

## 二、必须完成的开发任务

### 1. 项目初始化 (Day 1)

- [ ] 创建 Spring Boot 项目，pom.xml 包含以下依赖：
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - spring-boot-starter-security
  - spring-boot-starter-data-redis
  - mysql-connector-j
  - minio
  - jjwt-api (JWT)
  - lombok
  - springdoc-openapi (Swagger文档)
  
- [ ] 创建 application.yml 和 application-dev.yml 配置文件
- [ ] 配置 MySQL、Redis、MinIO 连接参数

### 2. 实体类开发 (Day 1-2)

创建以下 Entity 类：
- [ ] User.java - 用户实体
- [ ] EvaluationPeriod.java - 考核周期实体
- [ ] Document.java - 文档实体
- [ ] Evaluation.java - 考核评分实体
- [ ] SysConfig.java - 系统配置实体

### 3. Repository 开发 (Day 2)

为每个 Entity 创建对应的 Repository 接口：
- [ ] UserRepository
- [ ] EvaluationPeriodRepository
- [ ] DocumentRepository
- [ ] EvaluationRepository

### 4. Service 层开发 (Day 2-4)

- [ ] UserService - 用户管理
- [ ] AuthService - 认证服务 (登录/JWT)
- [ ] DocumentService - 文档上传/下载/删除
- [ ] EvaluationPeriodService - 考核周期管理
- [ ] EvaluationService - 考核评分

### 5. Controller 层开发 (Day 4-6)

实现以下 API：

**认证接口**
- [ ] POST /api/auth/login - 用户登录
- [ ] POST /api/auth/logout - 退出登录
- [ ] GET /api/auth/current - 获取当前用户

**用户管理**
- [ ] GET /api/users - 用户列表
- [ ] POST /api/users - 创建用户
- [ ] PUT /api/users/{id} - 更新用户
- [ ] DELETE /api/users/{id} - 删除用户
- [ ] GET /api/users/teachers - 教师列表

**考核周期**
- [ ] GET /api/periods - 周期列表
- [ ] POST /api/periods - 创建周期
- [ ] PUT /api/periods/{id} - 更新周期
- [ ] DELETE /api/periods/{id} - 删除周期
- [ ] GET /api/periods/active - 当前活跃周期

**文档管理**
- [ ] GET /api/documents - 文档列表
- [ ] POST /api/documents - 上传文档
- [ ] GET /api/documents/{id} - 文档详情
- [ ] PUT /api/documents/{id} - 更新文档
- [ ] DELETE /api/documents/{id} - 删除文档
- [ ] GET /api/documents/{id}/download - 下载文档

**考核评分**
- [ ] GET /api/evaluations - 评分列表
- [ ] POST /api/evaluations - 提交评分
- [ ] GET /api/evaluations/{id} - 评分详情
- [ ] GET /api/evaluations/teacher/{teacherId} - 教师成绩

### 6. 安全配置 (Day 3)

- [ ] SecurityConfig - Spring Security 配置
- [ ] JwtAuthenticationFilter - JWT 过滤器
- [ ] JwtUtil - JWT 工具类
- [ ] 配置 JWT secret: `teacher-eval-secret-key-2024`

### 7. 部署配置 (Day 6-7)

- [ ] 编写 Dockerfile
- [ ] 编写 docker-compose.yml
- [ ] 编写数据库初始化 SQL (docs/SQL/init.sql)

---

## 三、API 接口规范（必须遵守）

### 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 错误码规范
- 200: 成功
- 400: 请求参数错误
- 401: 未授权 (未登录)
- 403: 禁止访问 (权限不足)
- 404: 资源不存在
- 500: 服务器内部错误

### 文件上传
- 使用 MinIO 存储
- 支持最大 100MB 文件
- 返回文件访问路径

---

## 四、数据库表结构（必须实现）

参考 PROJECT_SPEC.md 中的 SQL 脚本，确保：
- 主键自增
- 软删除 (is_deleted 字段)
- 时间戳自动更新
- 考核评分唯一约束 (teacher_id + period_id)

---

## 五、测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | demo123 |
| 考核员 | evaluator1 | demo123 |
| 教师 | teacher1 | demo123 |
| 教师 | teacher2 | demo123 |

> 注：密码需使用 BCrypt 加密存储

---

## 六、交付要求

1. 完整的 Spring Boot 项目代码
2. 可运行的 jar 包
3. 数据库初始化 SQL 脚本
4. Docker 部署配置
5. API 接口文档 (Swagger)

---

## 七、协作约定

1. 每天与 Agent 2 同步进度
2. API 有变更时及时通知 Agent 2
3. 代码提交时使用清晰的英文描述
4. 遇到问题及时沟通

---

## 八、启动命令

```powershell
cd backend
mvn clean package -DskipTests
java -jar target/teacher-eval-1.0.0.jar --spring.profiles.active=dev
```

后端启动后访问：
- API: http://localhost:8080/api
- Swagger: http://localhost:8080/swagger-ui.html