# =====================================================
# 运行阶段：精简JRE镜像（宿主机预编译jar直接打包）
# 前置要求：先在宿主机执行 mvn clean package -DskipTests
# =====================================================
FROM eclipse-temurin:17-jre-alpine

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

# 直接复制宿主机已编译的jar
COPY target/teacher-evaluation-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
