@echo off
setlocal

echo ========================================
echo 教师评价考核平台 - 后端启动脚本
echo ========================================

if "%APP_BACKEND_PORT%"=="" set APP_BACKEND_PORT=18083

cd /d "%~dp0backend"

netstat -ano | findstr ":%APP_BACKEND_PORT% " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo 端口 %APP_BACKEND_PORT% 已被占用，请设置 APP_BACKEND_PORT 后重试。
    exit /b 1
)

echo 启动后端服务: http://localhost:%APP_BACKEND_PORT%
mvn spring-boot:run -Dspring-boot.run.profiles=test

pause
