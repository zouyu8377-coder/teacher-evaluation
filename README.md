# 教师评价考核平台 - 启动指南

## 环境要求

安装以下软件：
1. JDK 17 - 已安装或从 https://adoptium.net/ 下载
2. Maven - 从 https://maven.apache.org/download.cgi 下载
3. Docker Desktop - 从 https://www.docker.com/products/docker-desktop/ 下载

## 启动步骤

### 方式一：Docker Compose 一键启动（推荐）

```powershell
# 1. 启动 Docker Desktop

# 2. 进入项目目录
cd C:\TeacherEvaluation

# 3. 启动所有服务
docker-compose up -d
```

### 方式二：本地开发启动

```powershell
# 1. 启动 MySQL、Redis、MinIO
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=root123 -e MYSQL_DATABASE=teacher_eval -p 3306:3306 mysql:8.0
docker run -d --name redis -p 6379:6379 redis:7-alpine
docker run -d --name minio -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin -p 9000:9000 -p 9001:9001 minio/minio server /data --console-address ":9001"

# 2. 编译后端
cd C:\TeacherEvaluation\backend
mvn clean package -DskipTests

# 3. 启动后端
java -jar target/teacher-evaluation-1.0.0.jar --spring.profiles.active=dev

# 4. 启动前端（另开终端）
cd C:\TeacherEvaluation\frontend
npm install
npm run dev
```

## 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端API | http://localhost:8080 |
| Swagger文档 | http://localhost:8080/swagger-ui.html |
| MinIO控制台 | http://localhost:9001 |

## 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 考核员 | evaluator1 | eval123 |
| 教师 | teacher1 | teacher123 |