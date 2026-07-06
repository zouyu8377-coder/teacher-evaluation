@echo off
setlocal

echo ========================================
echo 教师评价考核平台 - 前端启动脚本
echo ========================================

if "%APP_FRONTEND_PORT%"=="" set APP_FRONTEND_PORT=15188
if "%APP_BACKEND_PORT%"=="" set APP_BACKEND_PORT=18083
if "%APP_BACKEND_URL%"=="" set APP_BACKEND_URL=http://localhost:%APP_BACKEND_PORT%

cd /d "%~dp0frontend"

netstat -ano | findstr ":%APP_FRONTEND_PORT% " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo 端口 %APP_FRONTEND_PORT% 已被占用，请设置 APP_FRONTEND_PORT 后重试。
    exit /b 1
)

echo 启动前端服务: http://localhost:%APP_FRONTEND_PORT%
npm run dev

pause
