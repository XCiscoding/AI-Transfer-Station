<template>
  <div class="quota-flow-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">额度流水</h1>
        <p class="page-subtitle">查看各维度的额度变动记录与汇总信息</p>
      </div>
      <div class="header-glow"></div>
    </div>

    <!-- 维度切换区 -->
    <div class="glass-card filter-card">
      <div class="filter-row">
        <div class="filter-item">
          <label class="filter-label">分析维度</label>
          <el-select v-model="dimension" class="dark-select" @change="handleDimensionChange">
            <el-option label="虚拟Key" value="virtual_key" />
            <el-option label="团队" value="team" />
            <el-option label="项目" value="project" />
          </el-select>
        </div>
        <div class="filter-item">
          <label class="filter-label">{{ dimensionSecondLabel }}</label>
          <el-select
            v-model="dimensionSecondValue"
            class="dark-select"
            :loading="dimensionListLoading"
            placeholder="请选择"
          >
            <el-option
              v-for="item in dimensionList"
              :key="item.id"
              :label="dimension === 'virtual_key' ? (item.keyName || item.key_name || item.id) : (dimension === 'team' ? item.teamName : item.projectName)"
              :value="item.id"
            />
          </el-select>
        </div>
        <div class="filter-actions">
          <el-button type="primary" class="search-btn" @click="handleQuery">查询</el-button>
        </div>
      </div>
    </div>

    <!-- 摘要卡片区 -->
    <div class="summary-cards">
      <div class="summary-card glass-card">
        <div class="summary-icon-wrap blue">
          <span class="summary-icon">∞</span>
        </div>
        <div class="summary-info">
          <div class="summary-label">额度上限</div>
          <div class="summary-value">{{ formatQuota(quotaSummary.quotaLimit) }}</div>
          <div class="summary-unit">积分</div>
        </div>
      </div>
      <div class="summary-card glass-card">
        <div class="summary-icon-wrap red">
          <span class="summary-icon">↓</span>
        </div>
        <div class="summary-info">
          <div class="summary-label">已使用</div>
          <div class="summary-value used">{{ formatQuota(quotaSummary.quotaUsed) }}</div>
          <div class="summary-unit">积分</div>
        </div>
      </div>
      <div class="summary-card glass-card">
        <div class="summary-icon-wrap green">
          <span class="summary-icon">✓</span>
        </div>
        <div class="summary-info">
          <div class="summary-label">剩余积分</div>
          <div class="summary-value remaining">{{ formatQuota(quotaSummary.quotaRemaining) }}</div>
          <div class="summary-unit">积分</div>
        </div>
      </div>
    </div>

    <!-- 流水表格 -->
    <div class="glass-card table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%; background: transparent;"
        :header-cell-style="headerCellStyle"
        :row-style="rowStyle"
        :cell-style="cellStyle"
      >
        <el-table-column prop="createdAt" label="时间" width="160">
          <template #default="{ row }">
            <span class="time-cell">{{ row.createdAt || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="transactionType" label="变动类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getTransactionTypeTag(row.transactionType)" size="small">
              {{ getTransactionTypeLabel(row.transactionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="对象类型" width="100" align="center">
          <template #default="{ row }">
            <span class="target-type">{{ getTargetTypeLabel(row.targetType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="变动量" width="130" align="right">
          <template #default="{ row }">
            <span :class="['amount-cell', row.amount > 0 ? 'amount-positive' : 'amount-negative']">
              {{ row.amount > 0 ? '+' : '' }}{{ formatQuota(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="balanceBefore" label="变动前" width="130" align="right">
          <template #default="{ row }">
            <span class="balance-cell">{{ formatQuota(row.balanceBefore) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="balanceAfter" label="变动后" width="130" align="right">
          <template #default="{ row }">
            <span class="balance-cell">{{ formatQuota(row.balanceAfter) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="150" show-overflow-tooltip />
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          class="dark-pagination"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getQuotaSummary, getQuotaTransactions } from '@/api/quota'
import request from '@/utils/request'

// 维度切换
const dimension = ref('virtual_key')
const dimensionSecondValue = ref(1)
const dimensionList = ref([])
const dimensionListLoading = ref(false)

const dimensionSecondLabel = computed(() => {
  if (dimension.value === 'virtual_key') return '虚拟Key'
  if (dimension.value === 'team') return '团队'
  return '项目'
})

const fetchDimensionList = async () => {
  dimensionListLoading.value = true
  try {
    let url
    if (dimension.value === 'virtual_key') {
      url = '/virtual-keys'
    } else if (dimension.value === 'team') {
      url = '/teams'
    } else {
      url = '/projects'
    }
    const res = await request({ url, method: 'get' })
    if (res.code === 200) {
      const raw = res.data?.records || res.data?.list || res.data || []
      dimensionList.value = raw
      if (raw.length > 0) {
        dimensionSecondValue.value = raw[0].id
      }
    }
  } catch (e) {
    dimensionList.value = []
  } finally {
    dimensionListLoading.value = false
  }
}

const handleDimensionChange = () => {
  dimensionSecondValue.value = null
  fetchDimensionList()
}

// 摘要数据
const quotaSummary = reactive({
  quotaLimit: 0,
  quotaUsed: 0,
  quotaRemaining: 0
})

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 表格样式
const headerCellStyle = {
  background: 'rgba(30, 41, 59, 0.6)',
  color: 'rgba(248, 250, 252, 0.7)',
  borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
  fontSize: '13px',
  fontWeight: '600'
}

const rowStyle = {
  background: 'transparent',
  color: '#F8FAFC'
}

const cellStyle = {
  background: 'transparent',
  borderBottom: '1px solid rgba(255, 255, 255, 0.06)',
  color: '#F8FAFC',
  fontSize: '13px'
}

// 工具函数
const formatQuota = (val) => {
  if (val == null) return '0'
  return Number(val).toLocaleString()
}

const getTransactionTypeLabel = (type) => {
  const map = {
    consume: '消耗',
    recharge: '充值',
    adjust: '调整',
    reset: '重置'
  }
  return map[type] || type || '-'
}

const getTransactionTypeTag = (type) => {
  const map = {
    consume: 'danger',
    recharge: 'success',
    adjust: 'warning',
    reset: 'info'
  }
  return map[type] || ''
}

const getTargetTypeLabel = (type) => {
  const map = {
    virtual_key: 'Key',
    team: '团队',
    project: '项目'
  }
  return map[type] || type || '-'
}

// 构建请求参数
const buildParams = () => {
  const params = {
    page: pagination.page,
    size: pagination.size
  }
  if (dimension.value === 'virtual_key') {
    params.targetType = 'virtual_key'
    params.targetId = dimensionSecondValue.value
  } else if (dimension.value === 'team') {
    params.targetType = 'team'
    params.targetId = dimensionSecondValue.value
  } else if (dimension.value === 'project') {
    params.targetType = 'project'
    params.targetId = dimensionSecondValue.value
  }
  return params
}

const buildSummaryParams = () => {
  const params = {}
  if (dimension.value === 'virtual_key') {
    params.targetType = 'virtual_key'
    params.targetId = dimensionSecondValue.value
  } else if (dimension.value === 'team') {
    params.targetType = 'team'
    params.targetId = dimensionSecondValue.value
  } else if (dimension.value === 'project') {
    params.targetType = 'project'
    params.targetId = dimensionSecondValue.value
  }
  return params
}

// 拉取摘要
const fetchSummary = async () => {
  try {
    const res = await getQuotaSummary(buildSummaryParams())
    if (res.code === 200 && res.data) {
      quotaSummary.quotaLimit = res.data.quotaLimit ?? 0
      quotaSummary.quotaUsed = res.data.quotaUsed ?? 0
      quotaSummary.quotaRemaining = res.data.quotaRemaining ?? 0
    }
  } catch (e) {
    // 静默失败
  }
}

// 拉取流水列表
const fetchTransactions = async () => {
  loading.value = true
  try {
    const res = await getQuotaTransactions(buildParams())
    if (res.code === 200) {
      tableData.value = res.data.records || res.data.list || res.data || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('获取流水列表失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  pagination.page = 1
  fetchSummary()
  fetchTransactions()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchTransactions()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchTransactions()
}

onMounted(() => {
  fetchDimensionList()
  fetchSummary()
  fetchTransactions()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.quota-flow-page {
  padding: 24px;
  font-family: 'Inter', sans-serif;
  animation: fadeInUp 0.5s ease-out;
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

.page-header {
  position: relative;
  padding: 24px 0 20px;
  margin-bottom: 8px;
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

/* glass-card */
.glass-card {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.8) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.05) inset, 0 8px 32px rgba(0, 0, 0, 0.2);
}

/* 筛选区 */
.filter-card {
  padding: 16px 20px;
}

.filter-row {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 160px;
}

.filter-label {
  font-size: 12px;
  color: rgba(248, 250, 252, 0.6);
  font-weight: 500;
}

.filter-actions {
  display: flex;
  align-items: center;
  padding-bottom: 2px;
}

/* 摘要卡片 */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  margin-bottom: 0;
  transition: all 0.3s ease;
}

.summary-card:hover {
  transform: translateY(-2px);
  border-color: rgba(59, 130, 246, 0.25);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(59, 130, 246, 0.1) inset;
}

.summary-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.summary-icon-wrap.blue {
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.35);
}

.summary-icon-wrap.red {
  background: linear-gradient(135deg, #F43F5E 0%, #E11D48 100%);
  box-shadow: 0 6px 16px rgba(244, 63, 94, 0.35);
}

.summary-icon-wrap.green {
  background: linear-gradient(135deg, #22C55E 0%, #16A34A 100%);
  box-shadow: 0 6px 16px rgba(34, 197, 94, 0.35);
}

.summary-icon {
  font-size: 22px;
  font-weight: 700;
  color: white;
  line-height: 1;
}

.summary-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-label {
  font-size: 13px;
  color: rgba(248, 250, 252, 0.6);
  font-weight: 500;
}

.summary-value {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 28px;
  font-weight: 700;
  color: #F8FAFC;
  letter-spacing: -0.5px;
  line-height: 1.1;
}

.summary-value.used {
  color: #F87171;
}

.summary-value.remaining {
  color: #4ADE80;
}

.summary-unit {
  font-size: 12px;
  color: rgba(248, 250, 252, 0.4);
}

/* 按钮 */
.search-btn {
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%) !important;
  border: none !important;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

/* 深色 select */
:deep(.dark-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  box-shadow: none !important;
}

:deep(.dark-select .el-input__wrapper:hover) {
  border-color: rgba(59, 130, 246, 0.4) !important;
}

:deep(.dark-select .el-input__wrapper.is-focus) {
  border-color: #3B82F6 !important;
}

:deep(.dark-select .el-input__inner) {
  color: #F8FAFC !important;
  font-size: 13px;
}

/* 表格 */
.table-card {
  padding: 0;
  overflow: hidden;
}

:deep(.el-table) {
  background: transparent !important;
  color: #F8FAFC;
}

:deep(.el-table tr) {
  background: transparent !important;
}

:deep(.el-table__body tr:hover > td) {
  background: rgba(59, 130, 246, 0.08) !important;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

:deep(.el-table th.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

:deep(.el-table__empty-block) {
  background: transparent;
}

:deep(.el-table__empty-text) {
  color: rgba(248, 250, 252, 0.4);
}

.time-cell {
  font-size: 12px;
  color: rgba(248, 250, 252, 0.7);
  font-family: 'Courier New', monospace;
}

.target-type {
  font-size: 12px;
  color: rgba(248, 250, 252, 0.6);
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.amount-cell {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-weight: 600;
  font-size: 13px;
}

.amount-positive {
  color: #4ADE80;
}

.amount-negative {
  color: #F87171;
}

.balance-cell {
  font-size: 13px;
  color: rgba(248, 250, 252, 0.8);
}

/* 分页 */
.pagination-wrap {
  padding: 16px 20px;
  display: flex;
  justify-content: flex-end;
  background: rgba(15, 23, 42, 0.3);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

:deep(.dark-pagination .el-pagination__total) {
  color: rgba(248, 250, 252, 0.6);
}

:deep(.dark-pagination .el-pagination__jump) {
  color: rgba(248, 250, 252, 0.6);
}

:deep(.dark-pagination .el-pager li) {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(248, 250, 252, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  margin: 0 2px;
}

:deep(.dark-pagination .el-pager li:hover) {
  color: #3B82F6;
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.3);
}

:deep(.dark-pagination .el-pager li.is-active) {
  background: rgba(59, 130, 246, 0.2);
  color: #3B82F6;
  border-color: rgba(59, 130, 246, 0.4);
}

:deep(.dark-pagination .btn-prev),
:deep(.dark-pagination .btn-next) {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(248, 250, 252, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
}

:deep(.dark-pagination .btn-prev:hover),
:deep(.dark-pagination .btn-next:hover) {
  color: #3B82F6;
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.3);
}

:deep(.dark-pagination .el-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: none !important;
}

:deep(.dark-pagination .el-select .el-input__inner) {
  color: rgba(248, 250, 252, 0.7) !important;
}

:deep(.dark-pagination .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: none !important;
}

:deep(.dark-pagination .el-input__inner) {
  color: rgba(248, 250, 252, 0.7) !important;
}

/* Tag */
:deep(.el-tag--success) {
  background: rgba(34, 197, 94, 0.15) !important;
  border-color: rgba(34, 197, 94, 0.25) !important;
  color: #4ADE80 !important;
}

:deep(.el-tag--danger) {
  background: rgba(244, 63, 94, 0.15) !important;
  border-color: rgba(244, 63, 94, 0.25) !important;
  color: #F87171 !important;
}

:deep(.el-tag--warning) {
  background: rgba(245, 158, 11, 0.15) !important;
  border-color: rgba(245, 158, 11, 0.25) !important;
  color: #FCD34D !important;
}

:deep(.el-tag--info) {
  background: rgba(59, 130, 246, 0.15) !important;
  border-color: rgba(59, 130, 246, 0.25) !important;
  color: #93C5FD !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .summary-cards {
    grid-template-columns: 1fr;
  }
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
