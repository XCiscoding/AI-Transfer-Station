<template>
  <div class="analytics-page">
    <!-- 维度切换 bar -->
    <div class="dimension-bar glass-card-bar">
      <div class="dimension-row">
        <div class="dimension-item">
          <label class="dimension-label">分析维度</label>
          <el-select
            v-model="dimension"
            class="dim-select"
            @change="handleDimensionChange"
          >
            <el-option label="个人" value="personal" />
            <el-option label="团队" value="team" />
            <el-option label="项目" value="project" />
          </el-select>
        </div>
        <div class="dimension-item">
          <label class="dimension-label">{{ dimensionSecondLabel }}</label>
          <el-select
            v-model="dimensionSecondValue"
            class="dim-select"
            :loading="dimensionListLoading"
            placeholder="请选择"
          >
            <el-option
              v-for="item in dimensionList"
              :key="item.id"
              :label="dimension === 'personal' ? (item.username || item.nickname || String(item.id)) : (dimension === 'team' ? item.teamName : item.projectName)"
              :value="item.id"
            />
          </el-select>
        </div>
        <div class="dimension-actions">
          <el-button type="primary" class="dim-query-btn" :disabled="needsScopedSelection && !dimensionSecondValue" @click="handleDimensionQuery">查询</el-button>
        </div>
      </div>
      <div v-if="dimensionBlockedReason" class="dimension-tip">
        {{ dimensionBlockedReason }}
      </div>
    </div>

    <!-- 页面头部 - 非对称布局 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">数据看板</h1>
        <p class="page-subtitle">实时监控平台运行状态与数据分析</p>
      </div>
      <div class="header-glow"></div>
      <div class="header-actions">
        <div class="time-selector">
          <button 
            v-for="range in timeRanges" 
            :key="range.value"
            :class="['time-btn', { active: timeRange === range.value }]"
            @click="timeRange = range.value"
          >
            {{ range.label }}
          </button>
        </div>
        <button class="export-btn">
          <el-icon><Download /></el-icon>
          <span>导出报表</span>
        </button>
      </div>
    </div>

    <!-- 核心指标卡片 - 非对称布局 -->
    <div class="metrics-section">
      <div class="metric-card metric-card-featured">
        <div class="metric-glow"></div>
        <div class="metric-content">
          <div class="metric-header">
            <span class="metric-name">总调用次数</span>
            <div class="metric-icon-wrap cyan">
              <el-icon :size="20"><TrendCharts /></el-icon>
            </div>
          </div>
          <div class="metric-value">1.2M</div>
          <div class="metric-change up">
            <el-icon><ArrowUp /></el-icon>
            <span>+15.3% 较上期</span>
          </div>
        </div>
      </div>
      
      <div class="metric-card">
        <div class="metric-glow"></div>
        <div class="metric-content">
          <div class="metric-header">
            <span class="metric-name">成功率</span>
            <div class="metric-icon-wrap green">
              <el-icon :size="18"><CircleCheckFilled /></el-icon>
            </div>
          </div>
          <div class="metric-value">99.2%</div>
          <div class="metric-change up">
            <el-icon><ArrowUp /></el-icon>
            <span>+0.5% 较上期</span>
          </div>
        </div>
      </div>
      
      <div class="metric-card">
        <div class="metric-glow"></div>
        <div class="metric-content">
          <div class="metric-header">
            <span class="metric-name">平均延迟</span>
            <div class="metric-icon-wrap amber">
              <el-icon :size="18"><Timer /></el-icon>
            </div>
          </div>
          <div class="metric-value">142ms</div>
          <div class="metric-change down">
            <el-icon><ArrowDown /></el-icon>
            <span>-12ms 较上期</span>
          </div>
        </div>
      </div>
      
      <div class="metric-card">
        <div class="metric-glow"></div>
        <div class="metric-content">
          <div class="metric-header">
            <span class="metric-name">活跃渠道</span>
            <div class="metric-icon-wrap violet">
              <el-icon :size="18"><Connection /></el-icon>
            </div>
          </div>
          <div class="metric-value">8</div>
          <div class="metric-change neutral">
            <span class="status-dot"></span>
            <span>全部正常</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据趋势图表 -->
    <div class="chart-section">
      <div class="chart-panel">
        <div class="panel-header">
          <div class="panel-title-wrap">
            <div class="panel-icon">
              <el-icon><DataLine /></el-icon>
            </div>
            <span class="panel-title">调用趋势分析</span>
          </div>
          <div class="chart-type-selector">
            <button 
              v-for="type in chartTypes" 
              :key="type.value"
              :class="['type-btn', { active: chartType === type.value }]"
              @click="chartType = type.value"
            >
              {{ type.label }}
            </button>
          </div>
        </div>
        <div class="chart-content">
          <div class="chart-metrics">
            <div class="chart-metric">
              <span class="metric-label">总调用量</span>
              <span class="metric-number">128,456</span>
            </div>
            <div class="chart-metric">
              <span class="metric-label">峰值时段</span>
              <span class="metric-number">14:00</span>
            </div>
            <div class="chart-metric">
              <span class="metric-label">平均QPS</span>
              <span class="metric-number">89.2</span>
            </div>
          </div>
          <div class="chart-visual">
            <div class="chart-bars">
              <div v-for="(item, index) in trendData" :key="index" class="chart-bar-wrapper">
                <div class="bar-stack">
                  <div 
                    class="bar-success" 
                    :style="{ height: item.success + '%' }"
                  ></div>
                  <div 
                    class="bar-failed" 
                    :style="{ height: item.failed + '%' }"
                  ></div>
                </div>
                <span class="bar-label">{{ item.label }}</span>
              </div>
            </div>
            <div class="chart-axis-y">
              <span v-for="n in 5" :key="n">{{ (6 - n) * 20 }}k</span>
            </div>
          </div>
          <div class="chart-legend">
            <div class="legend-item">
              <span class="legend-dot success"></span>
              <span>成功调用</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot failed"></span>
              <span>失败调用</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 资源状态与公告 - 非对称布局 -->
    <div class="bottom-section">
      <div class="resource-panel">
        <div class="panel-header">
          <div class="panel-title-wrap">
            <div class="panel-icon">
              <el-icon><Monitor /></el-icon>
            </div>
            <span class="panel-title">渠道资源状态</span>
          </div>
          <div class="status-badge">
            <span class="status-dot success"></span>
            <span>8/8 正常</span>
          </div>
        </div>
        <div class="resource-list">
          <div class="resource-item" v-for="(channel, index) in channels" :key="index">
            <div class="resource-main">
              <div class="resource-icon" :class="channel.status">
                <el-icon v-if="channel.status === 'success'"><CircleCheckFilled /></el-icon>
                <el-icon v-else><WarningFilled /></el-icon>
              </div>
              <div class="resource-info">
                <span class="resource-name">{{ channel.name }}</span>
                <span class="resource-meta">{{ channel.meta }}</span>
              </div>
            </div>
            <div class="resource-status" :class="channel.status">
              {{ channel.statusText }}
            </div>
          </div>
        </div>
      </div>
      
      <div class="notice-panel">
        <div class="panel-header">
          <div class="panel-title-wrap">
            <div class="panel-icon">
              <el-icon><Notification /></el-icon>
            </div>
            <span class="panel-title">公告通知</span>
          </div>
          <button class="view-all-btn">查看全部</button>
        </div>
        <div class="notice-list">
          <div class="notice-item" v-for="(notice, index) in notices" :key="index">
            <div class="notice-badge" :class="notice.type">{{ notice.badge }}</div>
            <div class="notice-content">
              <div class="notice-title">{{ notice.title }}</div>
              <div class="notice-desc">{{ notice.desc }}</div>
              <div class="notice-time">{{ notice.time }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 模型使用排行 -->
    <div class="ranking-section">
      <div class="ranking-panel">
        <div class="panel-header">
          <div class="panel-title-wrap">
            <div class="panel-icon">
              <el-icon><Trophy /></el-icon>
            </div>
            <span class="panel-title">模型使用排行</span>
          </div>
        </div>
        <div class="ranking-list">
          <div v-for="(item, index) in modelRanking" :key="index" class="ranking-item">
            <div class="ranking-number" :class="{ 'top3': index < 3 }">{{ index + 1 }}</div>
            <div class="ranking-info">
              <span class="model-name">{{ item.name }}</span>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: item.percentage + '%', background: getProgressColor(index) }"></div>
              </div>
            </div>
            <div class="ranking-stats">
              <span class="call-count">{{ item.calls }}次</span>
              <span class="percentage">{{ item.percentage }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useThemeStore } from '@/stores/theme.js'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getUserInfo } from '@/api/auth'

export default {
  name: 'Analytics',
  setup() {
    const themeStore = useThemeStore()
    const timeRange = ref('day')
    const chartType = ref('calls')

    // 维度切换
    const dimension = ref('personal')
    const dimensionSecondValue = ref(1)
    const dimensionList = ref([])
    const dimensionListLoading = ref(false)
    const currentUser = ref(null)
    const dimensionBlockedReason = ref('')

    const isSuperAdmin = computed(() => Boolean(currentUser.value?.isSuperAdmin || currentUser.value?.roles?.includes('SUPER_ADMIN')))
    const needsScopedSelection = computed(() => dimension.value !== 'personal')

    const dimensionSecondLabel = computed(() => {
      if (dimension.value === 'personal') return '用户'
      if (dimension.value === 'team') return '团队'
      return '项目'
    })

    const fetchCurrentUser = async () => {
      try {
        const res = await getUserInfo()
        currentUser.value = res.code === 200 ? res.data : null
      } catch {
        currentUser.value = null
      }
    }

    const fetchDimensionList = async () => {
      dimensionListLoading.value = true
      dimensionBlockedReason.value = ''
      try {
        if (dimension.value === 'personal') {
          dimensionList.value = []
          dimensionSecondValue.value = null
          return
        }

        const url = dimension.value === 'team' ? '/api/v1/teams' : '/api/v1/projects'
        const res = await request({ url, method: 'get' })
        if (res.code === 200) {
          const raw = res.data?.records || res.data?.list || res.data || []
          dimensionList.value = raw
          dimensionSecondValue.value = raw.length > 0 ? raw[0].id : null
          if (!raw.length) {
            dimensionBlockedReason.value = dimension.value === 'team'
              ? '当前账号没有可分析的团队数据。'
              : '当前账号没有可分析的项目数据。'
          }
        }
      } catch (e) {
        dimensionList.value = []
        dimensionSecondValue.value = null
        if (e?.response?.status === 401 || e?.response?.status === 403) {
          dimensionBlockedReason.value = dimension.value === 'team'
            ? '当前账号无权查看团队维度数据。'
            : '当前账号无权查看项目维度数据。'
        }
      } finally {
        dimensionListLoading.value = false
      }
    }

    const handleDimensionChange = () => {
      dimensionSecondValue.value = null
      fetchDimensionList()
    }

    const handleDimensionQuery = () => {
      if (dimensionBlockedReason.value) {
        ElMessage.warning(dimensionBlockedReason.value)
        return
      }
      if (needsScopedSelection.value && !dimensionSecondValue.value) {
        ElMessage.warning(`请先选择${dimensionSecondLabel.value}`)
        return
      }
      const dimLabel = { personal: '个人', team: '团队', project: '项目' }[dimension.value] || dimension.value
      ElMessage.success(`已切换到${dimLabel}维度，数据看板已更新`)
    }

    const timeRanges = [
      { label: '今日', value: 'day' },
      { label: '本周', value: 'week' },
      { label: '本月', value: 'month' }
    ]

    const chartTypes = [
      { label: '调用量', value: 'calls' },
      { label: 'Token消耗', value: 'tokens' },
      { label: '费用', value: 'cost' }
    ]

    // 趋势数据
    const trendData = ref([
      { label: '00:00', success: 45, failed: 5 },
      { label: '04:00', success: 32, failed: 3 },
      { label: '08:00', success: 78, failed: 8 },
      { label: '12:00', success: 92, failed: 6 },
      { label: '14:00', success: 88, failed: 7 },
      { label: '16:00', success: 85, failed: 5 },
      { label: '18:00', success: 75, failed: 4 },
      { label: '20:00', success: 68, failed: 6 },
      { label: '22:00', success: 55, failed: 4 },
      { label: '23:59', success: 42, failed: 3 }
    ])

    // 渠道数据
    const channels = ref([
      { name: 'OpenAI GPT-4', meta: '延迟: 145ms | 可用率: 99.8%', status: 'success', statusText: '正常' },
      { name: '通义千问 Qwen', meta: '延迟: 89ms | 可用率: 99.5%', status: 'success', statusText: '正常' },
      { name: '文心一言 ERNIE', meta: '延迟: 320ms | 可用率: 96.2%', status: 'warning', statusText: '延迟高' },
      { name: 'DeepSeek V3', meta: '延迟: 112ms | 可用率: 99.1%', status: 'success', statusText: '正常' },
      { name: 'Claude 3.5 Sonnet', meta: '延迟: 178ms | 可用率: 99.6%', status: 'success', statusText: '正常' }
    ])

    // 公告数据
    const notices = ref([
      { type: 'primary', badge: '新功能', title: 'Auto模式智能模型选择功能已上线', desc: '系统将根据您的使用场景自动选择最优模型', time: '2小时前' },
      { type: 'warning', badge: '维护', title: '系统将于今晚 02:00 进行例行维护', desc: '预计维护时间30分钟，期间服务可能短暂中断', time: '昨天' },
      { type: 'success', badge: '更新', title: '新增 Claude 3.5 Sonnet 模型支持', desc: '现在您可以在模型广场中使用 Claude 3.5 Sonnet', time: '3天前' },
      { type: 'info', badge: '提示', title: 'API 调用限额提醒', desc: '您本月的 API 调用量已达到配额的 85%', time: '5天前' }
    ])

    // 模型使用排行
    const modelRanking = ref([
      { name: 'GPT-4 Turbo', calls: '45,230', percentage: 35 },
      { name: 'Claude 3.5 Sonnet', calls: '32,156', percentage: 25 },
      { name: '通义千问 Qwen', calls: '28,432', percentage: 22 },
      { name: 'DeepSeek V3', calls: '15,678', percentage: 12 },
      { name: '文心一言 ERNIE', calls: '7,890', percentage: 6 }
    ])

    const getProgressColor = (index) => {
      const colors = ['#22C55E', '#06B6D4', '#F59E0B', '#64748B', '#94A3B8']
      return colors[index] || '#94A3B8'
    }

    onMounted(async () => {
      themeStore.initTheme()
      await fetchCurrentUser()
      fetchDimensionList()
    })

    watch(dimension, () => {
      fetchDimensionList()
    })

    return {
      themeStore,
      timeRange,
      chartType,
      timeRanges,
      chartTypes,
      trendData,
      channels,
      notices,
      modelRanking,
      getProgressColor,
      dimension,
      dimensionSecondValue,
      dimensionSecondLabel,
      dimensionList,
      dimensionListLoading,
      dimensionBlockedReason,
      needsScopedSelection,
      handleDimensionChange,
      handleDimensionQuery
    }
  }
}
</script>

<style scoped>
/* 导入字体 */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.analytics-page {
  padding: 24px;
  font-family: 'Inter', sans-serif;
}

/* 页面头部 - 非对称布局 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;
  position: relative;
  padding: 24px 0;
}

.header-content {
  position: relative;
  z-index: 1;
}

.page-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 32px;
  font-weight: 700;
  color: #F8FAFC;
  margin-bottom: 8px;
  letter-spacing: -1px;
  background: linear-gradient(135deg, #F8FAFC 0%, #94A3B8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-subtitle {
  font-size: 15px;
  color: rgba(248, 250, 252, 0.6);
  font-weight: 400;
}

.header-glow {
  position: absolute;
  top: 50%;
  left: 5%;
  transform: translateY(-50%);
  width: 250px;
  height: 120px;
  background: radial-gradient(ellipse, rgba(59, 130, 246, 0.15) 0%, transparent 70%);
  pointer-events: none;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  z-index: 1;
}

.time-selector {
  display: flex;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  padding: 4px;
}

.time-btn {
  padding: 8px 16px;
  border: none;
  background: transparent;
  color: rgba(248, 250, 252, 0.6);
  font-size: 13px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Inter', sans-serif;
}

.time-btn:hover {
  color: #F8FAFC;
  background: rgba(255, 255, 255, 0.08);
}

.time-btn.active {
  background: rgba(59, 130, 246, 0.2);
  color: #3B82F6;
}

.export-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
  border: none;
  border-radius: 10px;
  color: white;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Inter', sans-serif;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

.export-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.45);
}

/* 核心指标卡片 - 非对称网格 */
.metrics-section {
  display: grid;
  grid-template-columns: 1.3fr 1fr 1fr 1fr;
  gap: 20px;
  margin-bottom: 28px;
}

.metric-card {
  position: relative;
  background: 
    linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.8) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 24px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 
    0 0 0 1px rgba(59, 130, 246, 0.05) inset,
    0 8px 32px rgba(0, 0, 0, 0.2);
}

.metric-card:hover {
  transform: translateY(-4px);
  border-color: rgba(59, 130, 246, 0.25);
  box-shadow: 
    0 24px 48px rgba(0, 0, 0, 0.35),
    0 0 0 1px rgba(59, 130, 246, 0.15) inset,
    0 0 30px rgba(59, 130, 246, 0.1);
}

.metric-glow {
  position: absolute;
  top: -50%;
  right: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.12) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
}

.metric-card:hover .metric-glow {
  opacity: 1;
}

.metric-content {
  position: relative;
  z-index: 1;
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.metric-name {
  font-size: 14px;
  color: rgba(248, 250, 252, 0.6);
  font-weight: 500;
}

.metric-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.metric-icon-wrap.cyan {
  background: linear-gradient(135deg, #06B6D4 0%, #0891B2 100%);
  box-shadow: 0 6px 16px rgba(6, 182, 212, 0.35);
}

.metric-icon-wrap.green {
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.35);
}

.metric-icon-wrap.amber {
  background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
  box-shadow: 0 6px 16px rgba(245, 158, 11, 0.35);
}

.metric-icon-wrap.violet {
  background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%);
  box-shadow: 0 6px 16px rgba(139, 92, 246, 0.35);
}

.metric-value {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 32px;
  font-weight: 700;
  color: #F8FAFC;
  margin-bottom: 12px;
  letter-spacing: -0.5px;
}

.metric-card-featured .metric-value {
  font-size: 40px;
}

.metric-change {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.06);
}

.metric-change.up {
  color: #3B82F6;
}

.metric-change.down {
  color: #F43F5E;
}

.metric-change.neutral {
  color: rgba(248, 250, 252, 0.6);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #3B82F6;
  box-shadow: 0 0 8px rgba(59, 130, 246, 0.6);
}

/* 图表区域 */
.chart-section {
  margin-bottom: 28px;
}

.chart-panel {
  background: 
    linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.8) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 24px;
  transition: all 0.4s ease;
  box-shadow: 
    0 0 0 1px rgba(59, 130, 246, 0.05) inset,
    0 8px 32px rgba(0, 0, 0, 0.2);
}

.chart-panel:hover {
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 
    0 24px 48px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(59, 130, 246, 0.1) inset,
    0 0 30px rgba(59, 130, 246, 0.08);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.panel-title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
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
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 17px;
  font-weight: 600;
  color: #F8FAFC;
}

.chart-type-selector {
  display: flex;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  padding: 4px;
}

.type-btn {
  padding: 8px 16px;
  border: none;
  background: transparent;
  color: rgba(248, 250, 252, 0.6);
  font-size: 13px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Inter', sans-serif;
}

.type-btn:hover {
  color: #F8FAFC;
  background: rgba(255, 255, 255, 0.08);
}

.type-btn.active {
  background: rgba(59, 130, 246, 0.2);
  color: #3B82F6;
}

.chart-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.chart-metrics {
  display: flex;
  gap: 48px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.chart-metric {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metric-label {
  font-size: 12px;
  color: rgba(248, 250, 252, 0.5);
  font-weight: 500;
}

.metric-number {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 22px;
  font-weight: 700;
  color: #F8FAFC;
}

.chart-visual {
  display: flex;
  gap: 20px;
  height: 280px;
  position: relative;
}

.chart-bars {
  flex: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 12px;
  padding: 0 50px;
}

.chart-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.bar-stack {
  width: 100%;
  max-width: 36px;
  height: 240px;
  display: flex;
  flex-direction: column-reverse;
  border-radius: 6px;
  overflow: hidden;
}

.bar-success {
  background: linear-gradient(180deg, #3B82F6 0%, #2563EB 100%);
  transition: height 0.5s ease;
}

.bar-failed {
  background: linear-gradient(180deg, #F43F5E 0%, #E11D48 100%);
  transition: height 0.5s ease;
}

.bar-label {
  font-size: 12px;
  color: rgba(248, 250, 252, 0.5);
  font-weight: 500;
}

.chart-axis-y {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 30px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  font-size: 12px;
  color: rgba(248, 250, 252, 0.4);
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 32px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: rgba(248, 250, 252, 0.6);
  font-weight: 500;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.legend-dot.success {
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
}

.legend-dot.failed {
  background: linear-gradient(135deg, #F43F5E 0%, #E11D48 100%);
}

/* 底部区域 - 非对称布局 */
.bottom-section {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 24px;
  margin-bottom: 28px;
}

.resource-panel,
.notice-panel {
  background: 
    linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.8) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 24px;
  transition: all 0.4s ease;
  box-shadow: 
    0 0 0 1px rgba(59, 130, 246, 0.05) inset,
    0 8px 32px rgba(0, 0, 0, 0.2);
}

.resource-panel:hover,
.notice-panel:hover {
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 
    0 24px 48px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(59, 130, 246, 0.1) inset,
    0 0 30px rgba(59, 130, 246, 0.08);
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(59, 130, 246, 0.15);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  color: #3B82F6;
}

.view-all-btn {
  padding: 6px 14px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  color: rgba(248, 250, 252, 0.7);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Inter', sans-serif;
}

.view-all-btn:hover {
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.3);
  color: #3B82F6;
}

.resource-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resource-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.resource-item:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(59, 130, 246, 0.03) 100%);
  border-color: rgba(59, 130, 246, 0.2);
  transform: translateX(4px);
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.1);
}

.resource-main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.resource-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.resource-icon.success {
  background: rgba(59, 130, 246, 0.15);
  color: #3B82F6;
}

.resource-icon.warning {
  background: rgba(245, 158, 11, 0.15);
  color: #F59E0B;
}

.resource-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.resource-name {
  font-size: 14px;
  font-weight: 600;
  color: #F8FAFC;
}

.resource-meta {
  font-size: 12px;
  color: rgba(248, 250, 252, 0.5);
}

.resource-status {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.resource-status.success {
  background: rgba(59, 130, 246, 0.15);
  color: #3B82F6;
}

.resource-status.warning {
  background: rgba(245, 158, 11, 0.15);
  color: #F59E0B;
}

/* 公告列表 */
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.notice-item {
  display: flex;
  gap: 14px;
  padding: 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.notice-item:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(59, 130, 246, 0.03) 100%);
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.1);
}

.notice-badge {
  padding: 5px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.notice-badge.primary {
  background: rgba(6, 182, 212, 0.15);
  color: #06B6D4;
}

.notice-badge.warning {
  background: rgba(245, 158, 11, 0.15);
  color: #F59E0B;
}

.notice-badge.success {
  background: rgba(59, 130, 246, 0.15);
  color: #3B82F6;
}

.notice-badge.info {
  background: rgba(148, 163, 184, 0.15);
  color: #94A3B8;
}

.notice-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notice-title {
  font-size: 14px;
  font-weight: 600;
  color: #F8FAFC;
}

.notice-desc {
  font-size: 12px;
  color: rgba(248, 250, 252, 0.5);
}

.notice-time {
  font-size: 11px;
  color: rgba(248, 250, 252, 0.35);
  margin-top: 2px;
}

/* 排行区域 */
.ranking-section {
  margin-bottom: 28px;
}

.ranking-panel {
  background: 
    linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.8) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 24px;
  transition: all 0.4s ease;
  box-shadow: 
    0 0 0 1px rgba(59, 130, 246, 0.05) inset,
    0 8px 32px rgba(0, 0, 0, 0.2);
}

.ranking-panel:hover {
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 
    0 24px 48px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(59, 130, 246, 0.1) inset,
    0 0 30px rgba(59, 130, 246, 0.08);
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.ranking-item:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(59, 130, 246, 0.03) 100%);
  border-color: rgba(59, 130, 246, 0.2);
  transform: translateX(4px);
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.1);
}

.ranking-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: rgba(248, 250, 252, 0.5);
  background: rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.ranking-number.top3 {
  color: #fff;
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.ranking-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.model-name {
  font-size: 15px;
  font-weight: 600;
  color: #F8FAFC;
}

.progress-bar {
  height: 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.ranking-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 120px;
}

.call-count {
  font-size: 14px;
  color: rgba(248, 250, 252, 0.5);
}

.percentage {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 15px;
  font-weight: 700;
  color: #F8FAFC;
  min-width: 40px;
  text-align: right;
}

/* 维度切换 bar */
.glass-card-bar {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.8) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.05) inset, 0 8px 32px rgba(0, 0, 0, 0.2);
}

.dimension-row {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.dimension-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 160px;
}

.dimension-label {
  font-size: 12px;
  color: rgba(248, 250, 252, 0.6);
  font-weight: 500;
}

.dimension-actions {
  display: flex;
  align-items: center;
  padding-bottom: 2px;
}

.dim-query-btn {
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%) !important;
  border: none !important;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

:deep(.dim-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  box-shadow: none !important;
}

:deep(.dim-select .el-input__wrapper:hover) {
  border-color: rgba(59, 130, 246, 0.4) !important;
}

:deep(.dim-select .el-input__wrapper.is-focus) {
  border-color: #3B82F6 !important;
}

:deep(.dim-select .el-input__inner) {
  color: #F8FAFC !important;
  font-size: 13px;
}

/* 响应式适配 */
@media (max-width: 1280px) {
  .metrics-section {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .metric-card-featured {
    grid-column: span 2;
  }
  
  .bottom-section {
    grid-template-columns: 1fr;
  }
  
  .page-header {
    flex-direction: column;
    gap: 20px;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .page-title {
    font-size: 26px;
  }
  
  .metrics-section {
    grid-template-columns: 1fr;
  }
  
  .metric-card-featured {
    grid-column: span 1;
  }
  
  .chart-metrics {
    flex-wrap: wrap;
    gap: 20px;
  }
  
  .chart-visual {
    height: 220px;
  }
  
  .chart-bars {
    padding: 0 40px;
  }
  
  .ranking-stats {
    flex-direction: column;
    gap: 4px;
    align-items: flex-end;
  }
}
</style>
