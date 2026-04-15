FROM eclipse-temurin:17-jre-alpine

# 设置时区为上海
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

COPY target/teacher-evaluation-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]