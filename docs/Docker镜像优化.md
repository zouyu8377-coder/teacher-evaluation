# Docker 镜像优化文档

## 一、优化背景

项目初始构建时镜像体积较大：
- 前端：~95MB
- 后端：~432MB

优化目标：
- 后端：控制在 150MB 以内
- 前端：保持在 100MB 以内

## 二、已完成的优化

### 1. 后端优化 (backend/)

#### 新增 .dockerignore
文件位置：`backend/.dockerignore`

排除内容：
- Git 版本控制文件
- IDE 配置 (.idea, .vscode, *.iml)
- Maven 构建产物和缓存
- 日志文件
- 文档 (*.md)
- 测试代码 (src/test/)
- 环境变量文件

#### 优化 Dockerfile
文件位置：`backend/Dockerfile`

优化策略：
- 使用 Maven 镜像进行构建，充分利用层缓存
- 先复制 pom.xml 下载依赖，再复制源码，利用 Docker 层缓存
- 只复制最终的 jar 文件到运行镜像

```dockerfile
# 构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:17-jre-alpine
COPY --from=builder /app/target/teacher-evaluation-1.0.0.jar app.jar
```

### 2. 前端优化 (frontend/)

#### 完善 .dockerignore
文件位置：`frontend/.dockerignore`

排除内容：
- node_modules 及依赖锁文件
- 构建产物 (dist/, build/)
- Git 文件
- IDE 配置
- 文档
- 环境变量文件
- 测试文件
- Docker 配置

#### Dockerfile 现状
已采用多阶段构建，无需修改：
- 构建阶段：node:20-alpine + npm install + npm run build
- 运行阶段：nginx:alpine，只复制 dist 目录

## 三、构建优化说明

### 本地已有 jar 包时的优化

如果本地已经构建好 jar 包（target 目录下存在），可以使用简化的 Dockerfile 直接复制：

```dockerfile
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone
WORKDIR /app
COPY target/teacher-evaluation-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
```

当前项目已配置此简化版本，因为本地 target 目录已有构建产物。

### CI/CD 构建建议

在 CI/CD 环境中，建议使用多阶段构建以确保每次都是全新构建：

```dockerfile
# CI/CD 使用完整构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
# ... 运行阶段配置
```

## 四、进一步优化建议

如需进一步减小镜像体积，可考虑：

1. **使用更小的基础镜像**
   - 尝试 `bellsoft/liberica-openjre-alpine` (比 Temurin 更小)
   - 或使用 Distroless 镜像

2. **Spring Boot 分层构建**
   - 启用 Spring Boot 分层 jar 特性
   - 利用 Docker 构建缓存只重新构建变更的层

3. **JVM 内存优化**
   - 设置 `-XX:MaxRAMPercentage` 减少容器内存占用

4. **前端进一步优化**
   - 启用 gzip 压缩
   - 使用更小的 nginx 基础镜像

## 五、验证构建结果

构建后使用以下命令检查镜像大小：

```bash
# 查看镜像大小
docker images | grep teacher-eval

# 进入容器检查
docker run -it <image-id> /bin/sh
du -sh /app
```

## 六、相关文件清单

| 文件路径 | 说明 |
|----------|------|
| backend/Dockerfile | 后端 Docker 构建文件 |
| backend/.dockerignore | 后端 Docker 忽略配置 |
| frontend/Dockerfile | 前端 Docker 构建文件 |
| frontend/.dockerignore | 前端 Docker 忽略配置 |
| .claudeignore | Claude Code 专用忽略文件（不影响 Docker） |