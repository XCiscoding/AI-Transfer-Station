@echo off
chcp 65001 >nul 2>&1
title AI调度中心 - 数据库初始化脚本
color 0B

echo ╔═══════════════════════════════════════════════════════════╗
echo ║     AI调度中心 - 数据库初始化工具                           ║
echo ╚═══════════════════════════════════════════════════════════╝
echo.

set "PROJECT_ROOT=%~dp0"
set "DB_SCRIPTS=%PROJECT_ROOT%backend\src\main\resources\db"

where mysql >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误：未找到mysql命令行工具
    echo   请确保MySQL bin目录已加入系统PATH环境变量
    echo   通常路径：C:\Program Files\MySQL\MySQL Server 8.0\bin
    pause
    exit /b 1
)

echo [信息] MySQL客户端检测通过
echo.
echo 请选择操作：
echo   1. 完整初始化（删除旧库 → 建库 → 建表 → 导入数据）⚠️ 会清空所有数据
echo   2. 仅导入表结构（保留已有数据）
echo   3. 仅导入初始数据（跳过建表）
echo   4. 查看数据库状态
echo   0. 退出
echo.

set /p choice=请输入选项 (0-4):

if "%choice%"=="1" goto full_init
if "%choice%"=="2" goto schema_only
if "%choice%"=="3" goto data_only
if "%choice%"=="4" goto show_status
if "%choice%"=="0" goto end

echo ❌ 无效选项
pause
exit /b 1

:full_init
echo.
echo ⚠️  警告：此操作将删除现有数据库并重新创建！
set /p confirm=确认继续？(输入 YES 确认):
if not "%confirm%"=="YES" (
    echo 已取消操作
    pause
    exit /b 0
)

echo.
echo [1/4] 删除旧数据库...
mysql -u root -proot -e "DROP DATABASE IF EXISTS ai_key_management;" 2>nul
if %errorlevel% neq 0 (
    echo ❌ 删除数据库失败，请检查MySQL连接
    pause
    exit /b 1
)
echo ✅ 旧数据库已删除

echo [2/4] 创建新数据库...
mysql -u root -proot -e "CREATE DATABASE ai_key_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>nul
if %errorlevel% neq 0 (
    echo ❌ 创建数据库失败
    pause
    exit /b 1
)
echo ✅ 数据库 ai_key_management 已创建

echo [3/4] 执行 Flyway 迁移...
call mvn -f "%PROJECT_ROOT%backend\pom.xml" flyway:migrate >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ Flyway 迁移失败，请检查数据库连接和迁移脚本
    pause
    exit /b 1
)
echo ✅ Flyway 迁移完成

echo [4/4] 启动时将由 Flyway 负责表结构与初始数据

echo.
echo ════════════════════════════════════════════════════════════
echo   ✅ 数据库完整初始化成功！
echo   管理员账号：admin  /  admin123
echo ════════════════════════════════════════════════════════════
pause
goto end

:schema_only
echo.
echo [1/2] 执行 Flyway 迁移...
call mvn -f "%PROJECT_ROOT%backend\pom.xml" flyway:migrate >nul 2>nul
if %errorlevel% neq 0 (
    echo ⚠️  Flyway 迁移失败，请检查数据库连接或迁移状态
) else (
    echo ✅ Flyway 迁移完成
)

echo [2/2] 验证表数量...
for /f %%i in ('mysql -u root -proot -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ai_key_management';"') do set table_count=%%i
echo ✅ 当前数据库共有 %table_count% 张表
pause
goto end

:data_only
echo.
echo [1/1] 提示：初始数据已由 Flyway 管理，请执行完整迁移
call mvn -f "%PROJECT_ROOT%backend\pom.xml" flyway:migrate >nul 2>nul
if %errorlevel% neq 0 (
    echo ⚠️  Flyway 迁移失败，请检查数据库连接或迁移状态
) else (
    echo ✅ Flyway 迁移完成
    echo   管理员账号：admin / admin123
)
pause
goto end

:show_status
echo.
echo ════════════════════════════════════════════════════════════
echo   数据库状态报告
echo ════════════════════════════════════════════════════════════
echo.

mysql -u root -proot -e "
SELECT 
    'ai_key_management' AS '数据库名称',
    TABLE_SCHEMA,
    COUNT(*) AS '表数量',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS '大小(MB)'
FROM information_schema.tables 
WHERE table_schema = 'ai_key_management'
GROUP BY TABLE_SCHEMA;
" 2>nul

if %errorlevel% neq 0 (
    echo ⚠️  无法连接到数据库或数据库不存在
) else (
    echo.
    echo 📋 数据库表清单：
    mysql -u root -proot -Nse "SELECT table_name FROM information_schema.tables WHERE table_schema='ai_key_management' ORDER BY table_name;" 2>nul
    
    echo.
    echo 👤 用户列表：
    mysql -u root -proot ai_key_management -e "SELECT id, username, nickname, status FROM users LIMIT 10;" 2>nul
)

echo ════════════════════════════════════════════════════════════
pause
goto end

:end
exit /b 0
