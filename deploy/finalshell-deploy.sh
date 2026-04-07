#!/bin/bash
# AI调度中心 - FinalShell 一键部署脚本
# 使用方法: 在FinalShell中连接到服务器，然后执行: bash finalshell-deploy.sh

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  AI调度中心 - FinalShell 一键部署${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 配置变量
PROJECT_NAME="aikey"
INSTALL_DIR="/opt/${PROJECT_NAME}"
GITHUB_REPO="https://github.com/yourusername/ai-key-management.git"  # 请修改为你的仓库地址

# 检查是否以root运行
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}请使用 root 用户运行此脚本${NC}"
    exit 1
fi

# ==========================================
# 步骤1: 安装Docker
# ==========================================
echo -e "${YELLOW}[1/6] 检查并安装 Docker...${NC}"

if command -v docker &> /dev/null; then
    echo -e "${GREEN}Docker 已安装: $(docker --version)${NC}"
else
    echo "正在安装 Docker..."
    curl -fsSL https://get.docker.com | sh
    systemctl start docker
    systemctl enable docker
    echo -e "${GREEN}Docker 安装完成${NC}"
fi

# 检查docker compose
if docker compose version &> /dev/null; then
    echo -e "${GREEN}Docker Compose 已安装${NC}"
else
    echo -e "${YELLOW}安装 Docker Compose 插件...${NC}"
    # 新版Docker通常已内置compose插件
    # 如果未安装，使用独立版本
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
fi

# ==========================================
# 步骤2: 创建目录
# ==========================================
echo -e "${YELLOW}[2/6] 创建项目目录...${NC}"

mkdir -p ${INSTALL_DIR}
cd ${INSTALL_DIR}

# ==========================================
# 步骤3: 下载项目代码
# ==========================================
echo -e "${YELLOW}[3/6] 下载项目代码...${NC}"

if [ -d "${INSTALL_DIR}/.git" ]; then
    echo "检测到已有代码，执行更新..."
    git pull
else
    echo "克隆项目代码..."
    echo -e "${YELLOW}提示: 请先将代码推送到GitHub/GitLab，然后修改脚本中的 GITHUB_REPO 变量${NC}"
    # git clone ${GITHUB_REPO} .
    
    # 如果没有Git仓库，提示用户上传代码
    echo -e "${YELLOW}请通过 FinalShell 的文件管理器上传项目代码到: ${INSTALL_DIR}${NC}"
    echo -e "${YELLOW}上传完成后按回车继续...${NC}"
    read
fi

# ==========================================
# 步骤4: 配置环境变量
# ==========================================
echo -e "${YELLOW}[4/6] 配置环境变量...${NC}"

cd ${INSTALL_DIR}/deploy

if [ ! -f ".env" ]; then
    cp .env.example .env
    
    # 生成随机密码
    DB_ROOT_PASS=$(openssl rand -base64 12 2>/dev/null || date +%s | sha256sum | base64 | head -c 16)
    DB_USER_PASS=$(openssl rand -base64 12 2>/dev/null || date +%s | sha256sum | base64 | head -c 16)
    
    # 更新密码
    sed -i "s/MYSQL_ROOT_PASSWORD=.*/MYSQL_ROOT_PASSWORD=${DB_ROOT_PASS}/" .env
    sed -i "s/MYSQL_PASSWORD=.*/MYSQL_PASSWORD=${DB_USER_PASS}/" .env
    
    echo -e "${GREEN}环境变量已配置${NC}"
    echo -e "${YELLOW}数据库root密码: ${DB_ROOT_PASS}${NC}"
    echo -e "${YELLOW}数据库用户密码: ${DB_USER_PASS}${NC}"
    echo -e "${YELLOW}请记录以上密码，或查看 .env 文件${NC}"
else
    echo -e "${GREEN}环境变量已存在，跳过配置${NC}"
fi

# ==========================================
# 步骤5: 启动服务
# ==========================================
echo -e "${YELLOW}[5/6] 构建并启动服务...${NC}"
echo "首次构建可能需要5-10分钟，请耐心等待..."

docker compose -f docker-compose.all-in-one.yml down 2>/dev/null || true
docker compose -f docker-compose.all-in-one.yml up -d --build

# 等待服务启动
echo "等待服务启动..."
sleep 10

# ==========================================
# 步骤6: 检查状态
# ==========================================
echo -e "${YELLOW}[6/6] 检查服务状态...${NC}"

# 检查容器状态
RUNNING_CONTAINERS=$(docker ps --filter "name=aikey" --format "table {{.Names}}\t{{.Status}}" | wc -l)

if [ "$RUNNING_CONTAINERS" -ge 4 ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  部署成功！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "服务状态:"
    docker ps --filter "name=aikey" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    echo ""
    echo -e "访问地址: ${GREEN}http://$(curl -s ifconfig.me 2>/dev/null || echo '你的服务器IP')${NC}"
    echo ""
    echo "常用命令:"
    echo "  查看日志: docker compose -f docker-compose.all-in-one.yml logs -f"
    echo "  停止服务: docker compose -f docker-compose.all-in-one.yml down"
    echo "  重启服务: docker compose -f docker-compose.all-in-one.yml restart"
    echo ""
    echo -e "${YELLOW}数据库密码保存在: ${INSTALL_DIR}/deploy/.env${NC}"
else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}  部署可能有问题，请检查日志${NC}"
    echo -e "${RED}========================================${NC}"
    echo ""
    echo "查看日志:"
    docker compose -f docker-compose.all-in-one.yml logs
fi

echo ""
echo -e "${GREEN}部署脚本执行完成！${NC}"
