#Requires -Version 5.1

<#
.SYNOPSIS
    AI调度中心 - 数据库初始化工具
.DESCRIPTION
    提供数据库的完整初始化、表结构导入、数据导入和状态查看功能
.NOTES
    支持4种操作模式
#>

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "AI调度中心 - 数据库初始化工具"

# 颜色定义
$Green = "Green"
$Red = "Red"
$Yellow = "Yellow"
$Cyan = "Cyan"

function Write-Header {
    param([string]$Text)
    Write-Host "`n╔═══════════════════════════════════════════════════════════╗" -ForegroundColor $Cyan
    Write-Host "║ $Text" -ForegroundColor $Cyan -NoNewline
    $padding = 56 - $Text.Length
    Write-Host (" " * $padding) "║" -ForegroundColor $Cyan
    Write-Host "╚═══════════════════════════════════════════════════════════╝" -ForegroundColor $Cyan
}

function Test-Command {
    param([string]$Command)
    return [bool](Get-Command -Name $Command -ErrorAction SilentlyContinue)
}

# 获取项目路径
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Clear-Host
Write-Header "AI调度中心 - 数据库初始化工具"

# 检查mysql命令
if (-not (Test-Command "mysql")) {
    Write-Host "`n❌ 错误：未找到mysql命令行工具" -ForegroundColor $Red
    Write-Host "请确保MySQL bin目录已加入系统PATH环境变量" -ForegroundColor $Yellow
    Write-Host "通常路径：C:\Program Files\MySQL\MySQL Server 8.0\bin" -ForegroundColor $Cyan
    Read-Host "`n按 Enter 键退出"
    exit 1
}

Write-Host "`n[信息] MySQL客户端检测通过" -ForegroundColor $Green

# 显示菜单
Write-Host "`n请选择操作：" -ForegroundColor $Cyan
Write-Host "  1. 完整初始化（删除旧库 → 建库 → 建表 → 导入数据）" -NoNewline
Write-Host " ⚠️ 会清空所有数据" -ForegroundColor $Red
Write-Host "  2. 仅导入表结构（保留已有数据）"
Write-Host "  3. 仅导入初始数据（跳过建表）"
Write-Host "  4. 查看数据库状态"
Write-Host "  0. 退出"
Write-Host ""

$choice = Read-Host "请输入选项 (0-4)"

switch ($choice) {
    "1" {
        # 完整初始化
        Write-Host "`n⚠️  警告：此操作将删除现有数据库并重新创建！" -ForegroundColor $Yellow
        $confirm = Read-Host "确认继续？(输入 YES 确认)"
        
        if ($confirm -ne "YES") {
            Write-Host "`n已取消操作" -ForegroundColor $Cyan
            Read-Host "按 Enter 键退出"
            exit 0
        }
        
        Write-Host "`n[1/4] 删除旧数据库..." -NoNewline
        & mysql -u root -proot -e "DROP DATABASE IF EXISTS ai_key_management;" 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host " ✅ 旧数据库已删除" -ForegroundColor $Green
        }
        else {
            Write-Host " ❌ 删除数据库失败，请检查MySQL连接" -ForegroundColor $Red
            Read-Host "按 Enter 键退出"
            exit 1
        }
        
        Write-Host "[2/4] 创建新数据库..." -NoNewline
        & mysql -u root -proot -e "CREATE DATABASE ai_key_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host " ✅ 数据库 ai_key_management 已创建" -ForegroundColor $Green
        }
        else {
            Write-Host " ❌ 创建数据库失败" -ForegroundColor $Red
            Read-Host "按 Enter 键退出"
            exit 1
        }
        
        Write-Host "[3/4] 执行 Flyway 迁移..." -NoNewline
        & mvn -f "$ProjectRoot\backend\pom.xml" flyway:migrate 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host " ✅ Flyway 迁移完成" -ForegroundColor $Green
        }
        else {
            Write-Host " ❌ Flyway 迁移失败，请检查数据库连接和迁移脚本" -ForegroundColor $Red
            Read-Host "按 Enter 键退出"
            exit 1
        }

        Write-Host "[4/4] 启动时将由 Flyway 负责表结构与初始数据" -ForegroundColor $Green
        
        Write-Host "`n════════════════════════════════════════════════════════════" -ForegroundColor $Cyan
        Write-Host "  ✅ 数据库完整初始化成功！" -ForegroundColor $Green
        Write-Host "  管理员账号：admin / admin123" -ForegroundColor $Cyan
        Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor $Cyan
    }
    
    "2" {
        # 仅导入表结构
        Write-Host "`n[1/2] 导入表结构..." -NoNewline
        & mvn -f "$ProjectRoot\backend\pom.xml" flyway:migrate 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host " ✅ 表结构导入完成" -ForegroundColor $Green
        }
        else {
            Write-Host " ℹ️  表结构导入可能失败（表可能已存在）" -ForegroundColor $Yellow
        }
        
        Write-Host "[2/2] 验证表数量..." -NoNewline
        $tableCount = & mysql -u root -proot -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ai_key_management';" 2>&1
        Write-Host " ✅ 当前数据库共有 $tableCount 张表" -ForegroundColor $Green
    }
    
    "3" {
        # 仅导入初始数据
        Write-Host "`n[1/1] 导入初始数据..." -NoNewline
        $dataResult = Get-Content $DataFile -Raw | & mysql -u root -proot ai_key_management 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host " ✅ 初始数据导入完成" -ForegroundColor $Green
            Write-Host "  管理员账号：admin / admin123" -ForegroundColor $Cyan
        }
        else {
            Write-Host " ℹ️  数据导入可能失败（数据可能已存在）" -ForegroundColor $Yellow
        }
    }
    
    "4" {
        # 查看数据库状态
        Write-Host "`n════════════════════════════════════════════════════════════" -ForegroundColor $Cyan
        Write-Host "  数据库状态报告" -ForegroundColor $Cyan
        Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor $Cyan
        Write-Host ""
        
        $dbInfo = & mysql -u root -proot -e "
SELECT 
    'ai_key_management' AS '数据库名称',
    TABLE_SCHEMA,
    COUNT(*) AS '表数量',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS '大小(MB)'
FROM information_schema.tables 
WHERE table_schema = 'ai_key_management'
GROUP BY TABLE_SCHEMA;
" 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "⚠️  无法连接到数据库或数据库不存在" -ForegroundColor $Yellow
        }
        else {
            Write-Host $dbInfo
            Write-Host ""
            Write-Host "📋 数据库表清单：" -ForegroundColor $Cyan
            $tables = & mysql -u root -proot -Nse "SELECT table_name FROM information_schema.tables WHERE table_schema='ai_key_management' ORDER BY table_name;" 2>&1
            $tables | ForEach-Object { Write-Host "  • $_" }
            
            Write-Host ""
            Write-Host "👤 用户列表：" -ForegroundColor $Cyan
            $users = & mysql -u root -proot ai_key_management -e "SELECT id, username, nickname, status FROM users LIMIT 10;" 2>&1
            Write-Host $users
        }
        
        Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor $Cyan
    }
    
    "0" {
        exit 0
    }
    
    default {
        Write-Host "`n❌ 无效选项" -ForegroundColor $Red
    }
}

Read-Host "`n按 Enter 键退出"
exit 0
