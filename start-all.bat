@echo off
setlocal enabledelayedexpansion

echo ========================================
echo 教师评价考核平台 - 一键启动脚本
echo ========================================

set PROJECT_DIR=%~dp0
cd /d "%PROJECT_DIR%backend"

echo [1/6] 停止可能运行的后端服务 (端口 8080-8090)...
for /L %%p in (8080,1,8090) do (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%%p " ^| findstr "LISTENING"') do (
        echo   关闭端口 %%p (PID: %%a)
        taskkill /F /PID %%a >nul 2>&1
    )
)
echo   关闭所有 java.exe 进程...
taskkill /F /IM java.exe >nul 2>&1

cd /d "%PROJECT_DIR%frontend"

echo [2/6] 停止可能运行的前端服务 (端口 5171-5180)...
for /L %%p in (5171,1,5180) do (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%%p " ^| findstr "LISTENING"') do (
        echo   关闭端口 %%p (PID: %%a)
        taskkill /F /PID %%a >nul 2>&1
    )
)
echo   关闭 node.exe 进程...
taskkill /F /IM node.exe >nul 2>&1

echo [3/6] 等待端口释放...
timeout /t 3 /nobreak >nul

echo [4/6] 启动后端服务 (端口 8083)...
cd /d "%PROJECT_DIR%backend"
start "Backend" cmd /k "java -jar target\teacher-evaluation-1.0.0.jar --server.port=8083 --spring.datasource.url=jdbc:h2:mem:testdb --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=create --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"

echo [5/6] 等待后端启动...
timeout /t 15 /nobreak >nul

echo [6/6] 启动前端服务 (端口 5174)...
cd /d "%PROJECT_DIR%frontend"
start "Frontend" cmd /k "npx vite --host 0.0.0.0 --port 5174"

echo.
echo ========================================
echo 启动完成！
echo ========================================
echo 前端: http://localhost:5174
echo 后端: http://localhost:8083
echo Swagger: http://localhost:8083/swagger-ui/index.html
echo.
echo 测试账号:
echo   管理员: admin / admin123
echo   考核员: evaluator1 / eval123
echo   教师: teacher1 / teacher123
echo ========================================

pause