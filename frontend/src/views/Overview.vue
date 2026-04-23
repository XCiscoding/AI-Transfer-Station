<template>
  <div class="overview-page">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-content">
        <h1 class="welcome-title">欢迎回来，管理员</h1>
        <p class="welcome-subtitle">这是您今天的 AI 调度中心概览</p>
      </div>
      <div class="welcome-glow"></div>
    </div>

    <!-- 平台概览卡片 -->
    <div class="overview-section">
      <div class="stat-card stat-card-large">
        <div class="stat-glow"></div>
        <div class="stat-content">
          <div class="stat-icon-wrap cyan">
            <el-icon :size="28"><ChatDotRound /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formattedCalls }}</div>
            <div class="stat-label">今日调用次数</div>
            <div :class="['stat-trend', trendClass(stats.todayCallsTrend)]">
              <el-icon v-if="isTrendAvailable(stats.todayCallsTrend)"><ArrowUp v-if="stats.todayCallsTrend >= 0" /><ArrowDown v-else /></el-icon>
              <span>{{ trendText(stats.todayCallsTrend) }}</span>
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
            <div class="stat-value">{{ formattedTokens }}</div>
            <div class="stat-label">Token消耗量</div>
            <div :class="['stat-trend', trendClass(stats.todayTokensTrend)]">
              <el-icon v-if="isTrendAvailable(stats.todayTokensTrend)"><ArrowUp v-if="stats.todayTokensTrend >= 0" /><ArrowDown v-else /></el-icon>
              <span>{{ trendText(stats.todayTokensTrend) }}</span>
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
            <div class="stat-value">{{ formattedCost }}</div>
            <div class="stat-label">今日费用</div>
            <div :class="['stat-trend', trendClass(stats.todayCostTrend)]">
              <el-icon v-if="isTrendAvailable(stats.todayCostTrend)"><ArrowUp v-if="stats.todayCostTrend >= 0" /><ArrowDown v-else /></el-icon>
              <span>{{ trendText(stats.todayCostTrend) }}</span>
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
            <div class="stat-value">{{ formattedResponseTime }}</div>
            <div class="stat-label">平均响应时间</div>
            <div :class="['stat-trend', trendClass(stats.avgResponseTimeTrend)]">
              <el-icon v-if="isTrendAvailable(stats.avgResponseTimeTrend)"><ArrowUp v-if="stats.avgResponseTimeTrend >= 0" /><ArrowDown v-else /></el-icon>
              <span>{{ trendText(stats.avgResponseTimeTrend) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷操作 & 流程引导 -->
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
          <div class="action-item glass" @click="$router.push('/channels')">
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useThemeStore } from '@/stores/theme.js'
import { getOverviewStats } from '@/api/dashboard.js'

const themeStore = useThemeStore()

const stats = ref({
  todayCalls: 0,
  todayTokens: 0,
  todayCost: 0,
  avgResponseTime: 0,
  todayCallsTrend: 0,
  todayTokensTrend: 0,
  todayCostTrend: 0,
  avgResponseTimeTrend: 0
})

const loading = ref(true)
const statsLoadFailed = ref(false)

const formattedCalls = computed(() => {
  if (isMetricUnavailable()) return '-'
  const n = stats.value.todayCalls
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
})

const formattedTokens = computed(() => {
  if (isMetricUnavailable()) return '-'
  const n = stats.value.todayTokens
  if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
})

const formattedCost = computed(() => {
  if (isMetricUnavailable()) return '-'
  return '¥' + Number(stats.value.todayCost).toFixed(2)
})

const formattedResponseTime = computed(() => {
  if (isMetricUnavailable()) return '-'
  return stats.value.avgResponseTime + 'ms'
})

function trendClass(val) {
  if (!isTrendAvailable(val)) return 'neutral'
  return val >= 0 ? 'up' : 'down'
}

function trendText(val) {
  if (!isTrendAvailable(val)) return '-'
  const sign = val >= 0 ? '+' : ''
  return sign + val + '%'
}

function isMetricUnavailable() {
  return loading.value || statsLoadFailed.value
}

function isTrendAvailable(val) {
  return !isMetricUnavailable() && Number.isFinite(Number(val))
}

async function loadStats() {
  try {
    loading.value = true
    statsLoadFailed.value = false
    const res = await getOverviewStats()
    if (res.code === 200 && res.data) {
      stats.value = res.data
    } else {
      statsLoadFailed.value = true
    }
  } catch (e) {
    statsLoadFailed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  themeStore.initTheme()
  loadStats()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.overview-page {
  padding: 24px;
  font-family: 'Inter', sans-serif;
}

/* 内容区域淡入动画 */
.overview-page > * {
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.overview-page > *:nth-child(1) { animation-delay: 0s; }
.overview-page > *:nth-child(2) { animation-delay: 0.1s; }
.overview-page > *:nth-child(3) { animation-delay: 0.2s; }

/* 欢迎区域 */
.welcome-section {
  margin-bottom: 20px;
  position: relative;
  padding: 12px 0;
}

.welcome-content {
  position: relative;
  z-index: 1;
}

.welcome-title {
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

/* 概览卡片 */
.overview-section {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
}

.stat-card {
  position: relative;
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.8) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 16px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.05) inset, 0 8px 32px rgba(0, 0, 0, 0.2);
}

.stat-card:hover {
  transform: translateY(-4px);
  border-color: rgba(59, 130, 246, 0.25);
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.35), 0 0 0 1px rgba(59, 130, 246, 0.15) inset, 0 0 30px rgba(59, 130, 246, 0.1);
}

.stat-glow {
  position: absolute;
  top: -50%; right: -50%;
  width: 200%; height: 200%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.1) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
}
.stat-card:hover .stat-glow { opacity: 1; }

.stat-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.stat-icon-wrap {
  width: 52px; height: 52px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
}
.stat-icon-wrap.cyan { background: linear-gradient(135deg, #06B6D4 0%, #0891B2 100%); box-shadow: 0 8px 20px rgba(6, 182, 212, 0.35); }
.stat-icon-wrap.green { background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%); box-shadow: 0 8px 20px rgba(59, 130, 246, 0.35); }
.stat-icon-wrap.amber { background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%); box-shadow: 0 8px 20px rgba(245, 158, 11, 0.35); }
.stat-icon-wrap.rose { background: linear-gradient(135deg, #F43F5E 0%, #E11D48 100%); box-shadow: 0 8px 20px rgba(244, 63, 94, 0.35); }
.stat-icon-wrap.violet { background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%); box-shadow: 0 8px 20px rgba(139, 92, 246, 0.35); }

.stat-info { flex: 1; }

.stat-value {
  font-size: 28px; font-weight: 700;
  color: #F8FAFC; margin-bottom: 4px; letter-spacing: -0.5px;
}
.stat-card-large .stat-value { font-size: 36px; }

.stat-label { font-size: 13px; color: rgba(248, 250, 252, 0.55); margin-bottom: 10px; font-weight: 500; }

.stat-trend {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 12px; font-weight: 600;
  padding: 4px 10px; border-radius: 20px;
  background: rgba(255, 255, 255, 0.06);
}
.stat-trend.up { color: #3B82F6; }
.stat-trend.down { color: #F43F5E; }
.stat-trend.neutral { color: rgba(248, 250, 252, 0.45); }

/* 快捷操作 */
.action-section {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
}

.action-panel, .guide-panel {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.8) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 16px;
  transition: all 0.4s ease;
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.05) inset, 0 8px 32px rgba(0, 0, 0, 0.2);
}

.panel-header {
  display: flex; align-items: center; gap: 12px; margin-bottom: 20px;
}
.panel-icon {
  width: 36px; height: 36px; border-radius: 10px;
  background: rgba(59, 130, 246, 0.15);
  display: flex; align-items: center; justify-content: center;
  color: #3B82F6;
}
.panel-title { font-size: 17px; font-weight: 600; color: #F8FAFC; }

.action-list { display: flex; flex-direction: column; gap: 12px; }

.action-item {
  display: flex; align-items: center; gap: 16px;
  padding: 16px; border-radius: 14px; cursor: pointer;
  transition: all 0.3s ease;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.action-item:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(59, 130, 246, 0.05) 100%);
  border-color: rgba(59, 130, 246, 0.25);
  transform: translateX(4px);
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.15);
}

.action-icon-wrap {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
}
.action-icon-wrap.cyan { background: linear-gradient(135deg, #06B6D4 0%, #0891B2 100%); }
.action-icon-wrap.green { background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%); }
.action-icon-wrap.violet { background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%); }

.action-text { flex: 1; }
.action-title { font-size: 15px; font-weight: 600; color: #F8FAFC; margin-bottom: 4px; }
.action-desc { font-size: 13px; color: rgba(248, 250, 252, 0.5); }
.action-arrow { color: rgba(248, 250, 252, 0.4); transition: all 0.3s ease; }
.action-item:hover .action-arrow { color: #3B82F6; transform: translateX(4px); }

/* 流程引导步骤 */
.guide-steps { display: flex; flex-direction: column; gap: 0; padding: 8px 0; }

.step-item { display: flex; align-items: flex-start; gap: 16px; padding: 16px 0; }

.step-indicator {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; font-size: 16px; transition: all 0.3s ease;
}
.step-item.completed .step-indicator { background: rgba(59, 130, 246, 0.2); color: #3B82F6; }
.step-item.active .step-indicator { background: rgba(59, 130, 246, 0.3); color: #3B82F6; box-shadow: 0 0 20px rgba(59, 130, 246, 0.4); }

.step-indicator.pulse { animation: pulse 2s infinite; }
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4); }
  50% { box-shadow: 0 0 0 10px rgba(59, 130, 246, 0); }
}

.step-line { width: 2px; height: 24px; background: linear-gradient(180deg, rgba(59, 130, 246, 0.3) 0%, rgba(59, 130, 246, 0.1) 100%); margin-left: 17px; }

.step-content { flex: 1; padding-top: 6px; }
.step-title { font-size: 15px; font-weight: 600; color: #F8FAFC; margin-bottom: 4px; }
.step-desc { font-size: 13px; color: rgba(248, 250, 252, 0.5); }

/* 响应式 */
@media (max-width: 1280px) {
  .overview-section { grid-template-columns: repeat(2, 1fr); }
  .stat-card-large { grid-column: span 2; }
  .action-section { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .overview-page { padding: 16px; }
  .welcome-title { font-size: 26px; }
  .overview-section { grid-template-columns: 1fr; }
  .stat-card-large { grid-column: span 1; }
  .stat-content { flex-direction: column; gap: 12px; }
  .action-section { grid-template-columns: 1fr; }
}
</style>
