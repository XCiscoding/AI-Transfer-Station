<template>
  <div class="page-layout" :class="{ 'dark': themeStore.isDark }">
    <!-- 左侧导航栏 -->
    <aside class="sidebar" :class="{ 'collapsed': isCollapse }">
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

        <el-menu-item index="/channels">
          <el-icon><Share /></el-icon>
          <template #title>渠道管理</template>
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
          <el-breadcrumb separator="/" class="breadcrumb-nav">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
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

          <!-- 退出登录按钮 -->
          <el-tooltip content="退出登录">
            <el-button
              type="danger"
              plain
              circle
              class="logout-btn-header"
              @click="handleLogout"
            >
              <el-icon :size="18"><SwitchButton /></el-icon>
            </el-button>
          </el-tooltip>

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
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 页面内容区域（路由视图） -->
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useThemeStore } from '@/stores/theme.js'
import Logo from '@/components/Logo.vue'

const router = useRouter()
const route = useRoute()
const themeStore = useThemeStore()
const isCollapse = ref(false)

const currentPageTitle = computed(() => {
  return route.meta.title || '当前页面'
})

onMounted(() => {
  themeStore.initTheme()
})

function handleLogout() {
  ElMessageBox.confirm(
    '确定要退出登录吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('loginTime')
    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.page-layout {
  display: flex;
  width: 100%;
  height: 100vh;
  background: #0F172A;
  font-family: 'Inter', sans-serif;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

/* 侧边栏样式 */
.sidebar {
  width: 240px;
  background:
    linear-gradient(180deg, rgba(30, 41, 59, 0.85) 0%, rgba(15, 23, 42, 0.9) 100%);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-right: 1px solid rgba(255, 255, 255, 0.15);
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.1) inset,
    4px 0 24px rgba(0, 0, 0, 0.2);
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar.collapsed .logo-text {
  display: none;
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
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  justify-content: center;
  align-items: center;
}

.collapse-btn {
  width: 36px;
  height: 36px;
  padding: 0;
  color: rgba(248, 250, 252, 0.6);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.collapse-btn:hover {
  color: #F8FAFC;
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

/* 主容器样式 */
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* 顶部栏样式 */
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
  flex-shrink: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.breadcrumb-nav {
  margin-left: 0;
}

.breadcrumb-nav :deep(.el-breadcrumb__inner) {
  color: rgba(248, 250, 252, 0.7);
  font-weight: 500;
}

.breadcrumb-nav :deep(.el-breadcrumb__inner.is-link) {
  color: rgba(248, 250, 252, 0.7);
}

.breadcrumb-nav :deep(.el-breadcrumb__inner.is-link:hover) {
  color: #3B82F6;
}

.breadcrumb-nav :deep(.el-breadcrumb__separator) {
  color: rgba(248, 250, 252, 0.4);
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

.logout-btn-header {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #EF4444;
  transition: all 0.3s ease;
}

.logout-btn-header:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.5);
  color: #FCA5A5;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
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
  overflow-y: auto;
  background:
    radial-gradient(ellipse 120% 80% at 50% 0%, rgba(59, 130, 246, 0.15) 0%, transparent 60%),
    radial-gradient(ellipse 80% 60% at 0% 100%, rgba(59, 130, 246, 0.12) 0%, transparent 50%),
    radial-gradient(ellipse 70% 50% at 100% 100%, rgba(139, 92, 246, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse 50% 40% at 50% 50%, rgba(59, 130, 246, 0.05) 0%, transparent 70%),
    linear-gradient(180deg,
      rgba(30, 41, 59, 0.95) 0%,
      rgba(15, 23, 42, 0.98) 30%,
      rgba(15, 23, 42, 1) 50%,
      rgba(15, 23, 42, 0.98) 70%,
      rgba(30, 41, 59, 0.95) 100%);
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
    radial-gradient(ellipse 120% 80% at 50% 0%, rgba(59, 130, 246, 0.18) 0%, transparent 60%),
    radial-gradient(ellipse 80% 60% at 0% 100%, rgba(59, 130, 246, 0.15) 0%, transparent 50%),
    radial-gradient(ellipse 70% 50% at 100% 100%, rgba(139, 92, 246, 0.1) 0%, transparent 50%),
    radial-gradient(ellipse 50% 40% at 50% 50%, rgba(59, 130, 246, 0.06) 0%, transparent 70%),
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

.page-layout:not(.dark) .sidebar-footer {
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.page-layout:not(.dark) .collapse-btn {
  color: rgba(30, 41, 59, 0.6);
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.page-layout:not(.dark) .collapse-btn:hover {
  color: #1E293B;
  background: rgba(0, 0, 0, 0.08);
  border-color: rgba(0, 0, 0, 0.12);
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
    radial-gradient(ellipse 120% 80% at 50% 0%, rgba(59, 130, 246, 0.12) 0%, transparent 60%),
    radial-gradient(ellipse 80% 60% at 0% 100%, rgba(59, 130, 246, 0.1) 0%, transparent 50%),
    radial-gradient(ellipse 70% 50% at 100% 100%, rgba(139, 92, 246, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse 50% 40% at 50% 50%, rgba(59, 130, 246, 0.04) 0%, transparent 70%),
    linear-gradient(180deg,
      rgba(248, 250, 252, 0.95) 0%,
      rgba(241, 245, 249, 0.98) 30%,
      rgba(241, 245, 249, 1) 50%,
      rgba(241, 245, 249, 0.98) 70%,
      rgba(226, 232, 240, 0.95) 100%);
}

/* 响应式适配 */
@media (max-width: 768px) {
  .sidebar {
    width: 64px;
  }

  .logo-text {
    display: none;
  }

  .top-header {
    padding: 0 16px;
  }

  .username {
    display: none;
  }
}
</style>
