@echo off
setlocal

echo ========================================
echo 教师评价考核平台 - 前端启动脚本
echo ========================================

cd /d "%~dp0frontend"

echo [1/3] 停止可能运行的前端服务...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173 " ^| findstr "LISTENING"') do (
    echo 关闭端口 5173 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5174 " ^| findstr "LISTENING"') do (
    echo 关闭端口 5174 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5175 " ^| findstr "LISTENING"') do (
    echo 关闭端口 5175 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000 " ^| findstr "LISTENING"') do (
    echo 关闭端口 3000 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)

echo 关闭 node.exe 进程...
taskkill /F /IM node.exe >nul 2>&1

echo [2/3] 等待端口释放...
timeout /t 2 /nobreak >nul

echo [3/3] 启动前端服务...
npx vite --host 0.0.0.0 --port 5174

echo 前端已启动 (http://localhost:5174)
pause