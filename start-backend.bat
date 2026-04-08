@echo off
setlocal

echo ========================================
echo 教师评价考核平台 - 后端启动脚本
echo ========================================

cd /d "%~dp0backend"

echo [1/4] 停止可能运行的后端服务...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING"') do (
    echo 关闭端口 8080 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081 " ^| findstr "LISTENING"') do (
    echo 关闭端口 8081 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8082 " ^| findstr "LISTENING"') do (
    echo 关闭端口 8082 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8083 " ^| findstr "LISTENING"') do (
    echo 关闭端口 8083 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8084 " ^| findstr "LISTENING"') do (
    echo 关闭端口 8084 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8085 " ^| findstr "LISTENING"') do (
    echo 关闭端口 8085 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)

echo 关闭所有 java.exe 进程...
taskkill /F /IM java.exe >nul 2>&1

echo [2/4] 清理旧日志...
if exist "backend.log" del /F /Q "backend.log"

echo [3/4] 启动后端服务...
java -jar target\teacher-evaluation-1.0.0.jar --server.port=8083 --spring.datasource.url=jdbc:h2:mem:testdb --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=create --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

echo [4/4] 后端已启动 (http://localhost:8083)
pause