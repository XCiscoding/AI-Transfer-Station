<template>
  <div class="page-layout" :class="{ 'dark': themeStore.isDark }">
    <!-- 左侧导航栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <Logo :mode="themeStore.isDark ? 'light' : 'dark'" size="medium" />
          <span class="logo-text">AI调度中心</span>
        </div>
      </div>
      
      <el-menu
        :default-active="$route.path"
        class="sidebar-menu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <template #title>控制台首页</template>
        </el-menu-item>
        
        <el-menu-item index="/models">
          <el-icon><Grid /></el-icon>
          <template #title>模型广场</template>
        </el-menu-item>
        
        <el-menu-item index="/skills">
          <el-icon><MagicStick /></el-icon>
          <template #title>Skill超市</template>
        </el-menu-item>
        
        <el-menu-item index="/tokens">
          <el-icon><Key /></el-icon>
          <template #title>令牌管理</template>
        </el-menu-item>
        
        <el-menu-item index="/analytics">
          <el-icon><TrendCharts /></el-icon>
          <template #title>数据看板</template>
        </el-menu-item>
        
        <el-menu-item index="/logs">
          <el-icon><Document /></el-icon>
          <template #title>请求日志</template>
        </el-menu-item>
        
        <el-divider />
        
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <template #title>个人中心</template>
        </el-menu-item>
      </el-menu>
      
      <div class="sidebar-footer">
        <el-button
          type="text"
          class="collapse-btn"
          @click="isCollapse = !isCollapse"
        >
          <el-icon :size="18">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
        </el-button>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="main-container">
      <!-- 顶部栏 -->
      <header class="top-header">
        <div class="header-left">
          <breadcrumb />
        </div>
        <div class="header-right">
          <!-- 主题切换 -->
          <el-tooltip :content="themeStore.isDark ? '切换浅色模式' : '切换深色模式'">
            <el-button
              circle
              class="theme-toggle"
              @click="themeStore.toggleTheme"
            >
              <el-icon :size="18">
                <Sunny v-if="themeStore.isDark" />
                <Moon v-else />
              </el-icon>
            </el-button>
          </el-tooltip>
          
          <!-- 通知 -->
          <el-badge :value="3" class="notification-badge">
            <el-button circle class="glass-btn">
              <el-icon :size="18"><Bell /></el-icon>
            </el-button>
          </el-badge>
          
          <!-- 用户头像 -->
          <el-dropdown>
            <div class="user-info">
              <el-avatar :size="36" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="username">管理员</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人设置</el-dropdown-item>
                <el-dropdown-item>修改密码</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="main-content">
        <!-- 欢迎区域 - 非对称布局 -->
        <div class="welcome-section">
          <div class="welcome-content">
            <h1 class="welcome-title">欢迎回来，管理员</h1>
            <p class="welcome-subtitle">这是您今天的 AI 调度中心概览</p>
          </div>
          <div class="welcome-glow"></div>
        </div>

        <!-- 平台概览卡片 - 非对称布局 -->
        <div class="overview-section">
          <div class="stat-card stat-card-large">
            <div class="stat-glow"></div>
            <div class="stat-content">
              <div class="stat-icon-wrap cyan">
                <el-icon :size="28"><ChatDotRound /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">128,456</div>
                <div class="stat-label">今日调用次数</div>
                <div class="stat-trend up">
                  <el-icon><ArrowUp /></el-icon>
                  <span>+12.5%</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="stat-card">
            <div class="stat-glow"></div>
            <div class="stat-content">
              <div class="stat-icon-wrap green">
                <el-icon :size="24"><Coin /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">2.4M</div>
                <div class="stat-label">Token消耗量</div>
                <div class="stat-trend up">
                  <el-icon><ArrowUp /></el-icon>
                  <span>+8.3%</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="stat-card">
            <div class="stat-glow"></div>
            <div class="stat-content">
              <div class="stat-icon-wrap amber">
                <el-icon :size="24"><Wallet /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">¥1,280</div>
                <div class="stat-label">今日费用</div>
                <div class="stat-trend down">
                  <el-icon><ArrowDown /></el-icon>
                  <span>-5.2%</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="stat-card">
            <div class="stat-glow"></div>
            <div class="stat-content">
              <div class="stat-icon-wrap rose">
                <el-icon :size="24"><Timer /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">156ms</div>
                <div class="stat-label">平均响应时间</div>
                <div class="stat-trend up">
                  <el-icon><ArrowUp /></el-icon>
                  <span>+2.1%</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 快捷操作 & 流程引导 - 非对称布局 -->
        <div class="action-section">
          <div class="action-panel">
            <div class="panel-header">
              <div class="panel-icon">
                <el-icon><Pointer /></el-icon>
              </div>
              <span class="panel-title">快捷操作</span>
            </div>
            <div class="action-list">
              <div class="action-item glass" @click="$router.push('/tokens')">
                <div class="action-icon-wrap cyan">
                  <el-icon :size="20"><Plus /></el-icon>
                </div>
                <div class="action-text">
                  <div class="action-title">创建虚拟Key</div>
                  <div class="action-desc">快速生成新的API调用密钥</div>
                </div>
                <div class="action-arrow">
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
              <div class="action-item glass" @click="$router.push('/models')">
                <div class="action-icon-wrap green">
                  <el-icon :size="20"><Connection /></el-icon>
                </div>
                <div class="action-text">
                  <div class="action-title">接入新渠道</div>
                  <div class="action-desc">添加新的AI模型提供商</div>
                </div>
                <div class="action-arrow">
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
              <div class="action-item glass" @click="$router.push('/analytics')">
                <div class="action-icon-wrap violet">
                  <el-icon :size="20"><DataLine /></el-icon>
                </div>
                <div class="action-text">
                  <div class="action-title">查看数据报表</div>
                  <div class="action-desc">分析调用趋势和成本</div>
                </div>
                <div class="action-arrow">
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
            </div>
          </div>
          
          <div class="guide-panel">
            <div class="panel-header">
              <div class="panel-icon">
                <el-icon><Guide /></el-icon>
              </div>
              <span class="panel-title">流程引导</span>
            </div>
            <div class="guide-steps">
              <div class="step-item completed">
                <div class="step-indicator">
                  <el-icon><Check /></el-icon>
                </div>
                <div class="step-content">
                  <div class="step-title">接入渠道</div>
                  <div class="step-desc">配置AI模型提供商API密钥</div>
                </div>
              </div>
              <div class="step-line"></div>
              <div class="step-item completed">
                <div class="step-indicator">
                  <el-icon><Check /></el-icon>
                </div>
                <div class="step-content">
                  <div class="step-title">创建虚拟Key</div>
                  <div class="step-desc">生成用于调用的虚拟密钥</div>
                </div>
              </div>
              <div class="step-line"></div>
              <div class="step-item active">
                <div class="step-indicator pulse">
                  <el-icon><Loading /></el-icon>
                </div>
                <div class="step-content">
                  <div class="step-title">开始调用</div>
                  <div class="step-desc">使用虚拟Key调用AI模型</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useThemeStore } from '@/stores/theme.js'
import Logo from '@/components/Logo.vue'

export default {
  name: 'Overview',
  components: {
    Logo
  },
  setup() {
    const themeStore = useThemeStore()
    const isCollapse = ref(false)

    onMounted(() => {
      themeStore.initTheme()
    })

    return {
      themeStore,
      isCollapse
    }
  }
}
</script>

<style scoped>
/* 导入字体 */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.page-layout {
  display: flex;
  width: 100%;
  height: 100vh;
  background: #0F172A;
  font-family: 'Inter', sans-serif;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

/* 内容区域淡入动画 */
.main-content > * {
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 交错动画延迟 */
.main-content > *:nth-child(1) { animation-delay: 0s; }
.main-content > *:nth-child(2) { animation-delay: 0.1s; }
.main-content > *:nth-child(3) { animation-delay: 0.2s; }

/* 侧边栏样式 - 玻璃拟态 */
.sidebar {
  width: 240px;
  background: 
    linear-gradient(180deg, rgba(30, 41, 59, 0.85) 0%, rgba(15, 23, 42, 0.9) 100%);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-right: 1px solid rgba(255, 255, 255, 0.15);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  box-shadow: 
    0 0 0 1px rgba(59, 130, 246, 0.1) inset,
    4px 0 24px rgba(0, 0, 0, 0.2);
}

.sidebar-header {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #60A5FA 0%, #3B82F6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.4);
}

.logo-text {
  font-family: 'Orbitron', 'Exo 2', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: #F8FAFC;
  white-space: nowrap;
  letter-spacing: 1px;
  text-transform: uppercase;
  background: linear-gradient(135deg, #F8FAFC 0%, #60A5FA 50%, #3B82F6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 0 30px rgba(59, 130, 246, 0.3);
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 12px 8px;
}

.sidebar-menu :deep(.el-menu-item) {
  color: rgba(248, 250, 252, 0.7);
  border-radius: 10px;
  margin: 4px 0;
  height: 44px;
  line-height: 44px;
  font-family: 'DM Sans', sans-serif;
  font-weight: 500;
  transition: all 0.3s ease;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #F8FAFC;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: rgba(59, 130, 246, 0.15);
  color: #3B82F6;
  font-weight: 600;
}

.sidebar-menu :deep(.el-icon) {
  color: inherit;
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.collapse-btn {
  width: 100%;
  color: rgba(248, 250, 252, 0.6);
  transition: color 0.2s ease;
}

.collapse-btn:hover {
  color: #F8FAFC;
}

/* 主容器样式 */
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 顶部栏样式 - 玻璃拟态 */
.top-header {
  height: 72px;
  background: 
    linear-gradient(180deg, rgba(30, 41, 59, 0.75) 0%, rgba(15, 23, 42, 0.85) 100%);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  box-shadow: 
    0 0 0 1px rgba(59, 130, 246, 0.08) inset,
    0 4px 24px rgba(0, 0, 0, 0.15);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.theme-toggle,
.glass-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(248, 250, 252, 0.8);
  transition: all 0.3s ease;
}

.theme-toggle:hover,
.glass-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.2);
  color: #F8FAFC;
  transform: translateY(-1px);
}

.notification-badge :deep(.el-badge__content) {
  top: 6px;
  right: 6px;
  background: #EF4444;
  border: none;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s ease;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.15);
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #F8FAFC;
}

/* 主内容区样式 */
.main-content {
  flex: 1;
  padding: 16px 20px;
  overflow-y: auto;
  background:
    /* 顶部蓝色光晕 */
    radial-gradient(ellipse 120% 80% at 50% 0%, rgba(59, 130, 246, 0.15) 0%, transparent 60%),
    /* 左下角蓝色光晕 */
    radial-gradient(ellipse 80% 60% at 0% 100%, rgba(59, 130, 246, 0.12) 0%, transparent 50%),
    /* 右下角紫色光晕 */
    radial-gradient(ellipse 70% 50% at 100% 100%, rgba(139, 92, 246, 0.08) 0%, transparent 50%),
    /* 中央微弱光晕 */
    radial-gradient(ellipse 50% 40% at 50% 50%, rgba(59, 130, 246, 0.05) 0%, transparent 70%),
    /* 主背景渐变 */
    linear-gradient(180deg, 
      rgba(30, 41, 59, 0.95) 0%, 
      rgba(15, 23, 42, 0.98) 30%,
      rgba(15, 23, 42, 1) 50%, 
      rgba(15, 23, 42, 0.98) 70%,
      rgba(30, 41, 59, 0.95) 100%);
}

/* 欢迎区域 - 非对称布局 */
.welcome-section {
  margin-bottom: 16px;
  position: relative;
  padding: 12px 0;
}

.welcome-content {
  position: relative;
  z-index: 1;
}

.welcome-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 36px;
  font-weight: 700;
  color: #F8FAFC;
  margin-bottom: 8px;
  letter-spacing: -1px;
  background: linear-gradient(135deg, #F8FAFC 0%, #94A3B8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-subtitle {
  font-size: 15px;
  color: rgba(248, 250, 252, 0.6);
  font-weight: 400;
}

.welcome-glow {
  position: absolute;
  top: 50%;
  left: 10%;
  transform: translateY(-50%);
  width: 300px;
  height: 150px;
  background: radial-gradient(ellipse, rgba(59, 130, 246, 0.15) 0%, transparent 70%);
  pointer-events: none;
}

/* 概览卡片 - 非对称网格布局 */
.overview-section {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  position: relative;
  background: 
    linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.8) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 16px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 
    0 0 0 1px rgba(59, 130, 246, 0.05) inset,
    0 8px 32px rgba(0, 0, 0, 0.2);
}

.stat-card:hover {
  transform: translateY(-4px);
  border-color: rgba(59, 130, 246, 0.25);
  box-shadow: 
    0 24px 48px rgba(0, 0, 0, 0.35),
    0 0 0 1px rgba(59, 130, 246, 0.15) inset,
    0 0 30px rgba(59, 130, 246, 0.1);
}

.stat-card-large {
  grid-row: span 1;
}

.stat-glow {
  position: absolute;
  top: -50%;
  right: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.1) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
}

.stat-card:hover .stat-glow {
  opacity: 1;
}

.stat-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.stat-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-icon-wrap.cyan {
  background: linear-gradient(135deg, #06B6D4 0%, #0891B2 100%);
  box-shadow: 0 8px 20px rgba(6, 182, 212, 0.35);
}

.stat-icon-wrap.green {
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.35);
}

.stat-icon-wrap.amber {
  background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
  box-shadow: 0 8px 20px rgba(245, 158, 11, 0.35);
}

.stat-icon-wrap.rose {
  background: linear-gradient(135deg, #F43F5E 0%, #E11D48 100%);
  box-shadow: 0 8px 20px rgba(244, 63, 94, 0.35);
}

.stat-icon-wrap.violet {
  background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%);
  box-shadow: 0 8px 20px rgba(139, 92, 246, 0.35);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 28px;
  font-weight: 700;
  color: #F8FAFC;
  margin-bottom: 4px;
  letter-spacing: -0.5px;
}

.stat-card-large .stat-value {
  font-size: 36px;
}

.stat-label {
  font-size: 13px;
  color: rgba(248, 250, 252, 0.55);
  margin-bottom: 10px;
  font-weight: 500;
}

.stat-trend {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.06);
}

.stat-trend.up {
  color: #3B82F6;
}

.stat-trend.down {
  color: #F43F5E;
}

/* 快捷操作和流程引导 - 非对称布局 */
.action-section {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
}

.action-panel,
.guide-panel {
  background: 
    linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.8) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 16px;
  transition: all 0.4s ease;
  box-shadow: 
    0 0 0 1px rgba(59, 130, 246, 0.05) inset,
    0 8px 32px rgba(0, 0, 0, 0.2);
}

.action-panel:hover,
.guide-panel:hover {
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 
    0 24px 48px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(59, 130, 246, 0.1) inset,
    0 0 30px rgba(59, 130, 246, 0.08);
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.panel-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3B82F6;
}

.panel-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 17px;
  font-weight: 600;
  color: #F8FAFC;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: 
    linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.action-item:hover {
  background: 
    linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(59, 130, 246, 0.05) 100%);
  border-color: rgba(59, 130, 246, 0.25);
  transform: translateX(4px);
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.15);
}

.action-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.action-icon-wrap.cyan {
  background: linear-gradient(135deg, #06B6D4 0%, #0891B2 100%);
}

.action-icon-wrap.green {
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
}

.action-icon-wrap.violet {
  background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%);
}

.action-text {
  flex: 1;
}

.action-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: #F8FAFC;
  margin-bottom: 4px;
}

.action-desc {
  font-size: 13px;
  color: rgba(248, 250, 252, 0.5);
}

.action-arrow {
  color: rgba(248, 250, 252, 0.4);
  transition: all 0.3s ease;
}

.action-item:hover .action-arrow {
  color: #3B82F6;
  transform: translateX(4px);
}

/* 流程引导步骤 */
.guide-steps {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 8px 0;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 0;
}

.step-indicator {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
  transition: all 0.3s ease;
}

.step-item.completed .step-indicator {
  background: rgba(59, 130, 246, 0.2);
  color: #3B82F6;
}

.step-item.active .step-indicator {
  background: rgba(59, 130, 246, 0.3);
  color: #3B82F6;
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.4);
}

.step-indicator.pulse {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(59, 130, 246, 0);
  }
}

.step-line {
  width: 2px;
  height: 24px;
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.3) 0%, rgba(59, 130, 246, 0.1) 100%);
  margin-left: 17px;
}

.step-content {
  flex: 1;
  padding-top: 6px;
}

.step-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: #F8FAFC;
  margin-bottom: 4px;
}

.step-desc {
  font-size: 13px;
  color: rgba(248, 250, 252, 0.5);
}

/* 深色模式适配 */
.dark .sidebar {
  background: rgba(15, 23, 42, 0.8);
}

.dark .top-header {
  background: rgba(15, 23, 42, 0.6);
}

.dark .main-content {
  background: 
    /* 顶部蓝色光晕 */
    radial-gradient(ellipse 120% 80% at 50% 0%, rgba(59, 130, 246, 0.18) 0%, transparent 60%),
    /* 左下角蓝色光晕 */
    radial-gradient(ellipse 80% 60% at 0% 100%, rgba(59, 130, 246, 0.15) 0%, transparent 50%),
    /* 右下角紫色光晕 */
    radial-gradient(ellipse 70% 50% at 100% 100%, rgba(139, 92, 246, 0.1) 0%, transparent 50%),
    /* 中央微弱光晕 */
    radial-gradient(ellipse 50% 40% at 50% 50%, rgba(59, 130, 246, 0.06) 0%, transparent 70%),
    /* 深色背景 */
    linear-gradient(180deg, 
      rgba(15, 23, 42, 0.98) 0%, 
      rgba(11, 17, 32, 1) 50%, 
      rgba(15, 23, 42, 0.98) 100%);
}

/* 浅色模式适配 */
.page-layout:not(.dark) {
  background: #F1F5F9;
}

.page-layout:not(.dark) .sidebar {
  background: rgba(255, 255, 255, 0.8);
  border-right: 1px solid rgba(0, 0, 0, 0.08);
}

.page-layout:not(.dark) .logo-text {
  background: linear-gradient(135deg, #1E293B 0%, #3B82F6 50%, #2563EB 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 0 20px rgba(59, 130, 246, 0.2);
}

.page-layout:not(.dark) .sidebar-menu :deep(.el-menu-item) {
  color: rgba(30, 41, 59, 0.7);
}

.page-layout:not(.dark) .sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(0, 0, 0, 0.04);
  color: #1E293B;
}

.page-layout:not(.dark) .sidebar-menu :deep(.el-menu-item.is-active) {
  background: rgba(59, 130, 246, 0.1);
  color: #2563EB;
}

.page-layout:not(.dark) .top-header {
  background: rgba(255, 255, 255, 0.7);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.page-layout:not(.dark) .theme-toggle,
.page-layout:not(.dark) .glass-btn {
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.08);
  color: rgba(30, 41, 59, 0.7);
}

.page-layout:not(.dark) .user-info {
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.page-layout:not(.dark) .username {
  color: #1E293B;
}

.page-layout:not(.dark) .main-content {
  background:
    /* 顶部蓝色光晕 */
    radial-gradient(ellipse 120% 80% at 50% 0%, rgba(59, 130, 246, 0.12) 0%, transparent 60%),
    /* 左下角蓝色光晕 */
    radial-gradient(ellipse 80% 60% at 0% 100%, rgba(59, 130, 246, 0.1) 0%, transparent 50%),
    /* 右下角紫色光晕 */
    radial-gradient(ellipse 70% 50% at 100% 100%, rgba(139, 92, 246, 0.06) 0%, transparent 50%),
    /* 中央微弱光晕 */
    radial-gradient(ellipse 50% 40% at 50% 50%, rgba(59, 130, 246, 0.04) 0%, transparent 70%),
    /* 主背景渐变 */
    linear-gradient(180deg, 
      rgba(248, 250, 252, 0.95) 0%, 
      rgba(241, 245, 249, 0.98) 30%,
      rgba(241, 245, 249, 1) 50%, 
      rgba(241, 245, 249, 0.98) 70%,
      rgba(226, 232, 240, 0.95) 100%);
}

.page-layout:not(.dark) .welcome-title {
  background: linear-gradient(135deg, #1E293B 0%, #475569 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-layout:not(.dark) .welcome-subtitle {
  color: rgba(30, 41, 59, 0.6);
}

.page-layout:not(.dark) .stat-card,
.page-layout:not(.dark) .action-panel,
.page-layout:not(.dark) .guide-panel {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.page-layout:not(.dark) .stat-value,
.page-layout:not(.dark) .panel-title,
.page-layout:not(.dark) .action-title,
.page-layout:not(.dark) .step-title {
  color: #1E293B;
}

.page-layout:not(.dark) .stat-label,
.page-layout:not(.dark) .action-desc,
.page-layout:not(.dark) .step-desc {
  color: rgba(30, 41, 59, 0.55);
}

.page-layout:not(.dark) .action-item {
  background: 
    linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0.6) 100%);
  border: 1px solid rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.page-layout:not(.dark) .action-item:hover {
  background: 
    linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(59, 130, 246, 0.03) 100%);
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.1);
}

.page-layout:not(.dark) .action-arrow {
  color: rgba(30, 41, 59, 0.4);
}

.page-layout:not(.dark) .step-item.completed .step-indicator {
  background: rgba(59, 130, 246, 0.15);
}

/* 响应式适配 */
@media (max-width: 1280px) {
  .overview-section {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stat-card-large {
    grid-column: span 2;
  }
  
  .action-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .sidebar {
    width: 64px;
  }
  
  .logo-text {
    display: none;
  }
  
  .main-content {
    padding: 16px;
  }
  
  .welcome-title {
    font-size: 26px;
  }
  
  .overview-section {
    grid-template-columns: 1fr;
  }
  
  .stat-card-large {
    grid-column: span 1;
  }
  
  .stat-content {
    flex-direction: column;
    gap: 12px;
  }
  
  .action-section {
    grid-template-columns: 1fr;
  }
}
</style>
