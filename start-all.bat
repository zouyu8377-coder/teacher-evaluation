@echo off
setlocal

echo ========================================
echo 教师评价考核平台 - 一键启动脚本
echo ========================================

set PROJECT_DIR=%~dp0
if "%APP_BACKEND_PORT%"=="" set APP_BACKEND_PORT=18083
if "%APP_FRONTEND_PORT%"=="" set APP_FRONTEND_PORT=15188
set APP_BACKEND_URL=http://localhost:%APP_BACKEND_PORT%

netstat -ano | findstr ":%APP_BACKEND_PORT% " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo 后端端口 %APP_BACKEND_PORT% 已被占用。
    exit /b 1
)
netstat -ano | findstr ":%APP_FRONTEND_PORT% " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo 前端端口 %APP_FRONTEND_PORT% 已被占用。
    exit /b 1
)

start "TeacherEvaluation Backend" /D "%PROJECT_DIR%backend" cmd /k call "%PROJECT_DIR%start-backend.bat"
timeout /t 12 /nobreak >nul
start "TeacherEvaluation Frontend" /D "%PROJECT_DIR%frontend" cmd /k call "%PROJECT_DIR%start-frontend.bat"

echo.
echo ========================================
echo 启动完成！
echo ========================================
echo 前端: http://localhost:%APP_FRONTEND_PORT%
echo 后端: http://localhost:%APP_BACKEND_PORT%
echo Swagger: http://localhost:%APP_BACKEND_PORT%/swagger-ui/index.html
echo ========================================

pause
