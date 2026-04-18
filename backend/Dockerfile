# =====================================================
# 构建阶段：使用Maven构建fat jar
# =====================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 先复制pom.xml和源码，利用Maven层缓存
COPY pom.xml .
# 下载依赖（如果pom.xml没变，会复用缓存）
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# =====================================================
# 运行阶段：精简JRE镜像
# =====================================================
FROM eclipse-temurin:17-jre-alpine

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

# 只复制jar文件
COPY --from=builder /app/target/teacher-evaluation-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]