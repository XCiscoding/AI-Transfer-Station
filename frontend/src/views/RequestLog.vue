<template>
  <div class="request-log-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">请求日志</h1>
        <p class="page-subtitle">查看所有 API 请求的详细记录与状态</p>
      </div>
      <div class="header-glow"></div>
    </div>

    <!-- 筛选区 -->
    <div class="glass-card filter-card">
      <div class="filter-row">
        <div class="filter-item filter-item-wide">
          <label class="filter-label">时间范围</label>
          <el-date-picker
            v-model="searchForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            class="dark-picker"
          />
        </div>
        <div class="filter-item">
          <label class="filter-label">模型名称</label>
          <el-input
            v-model="searchForm.modelName"
            placeholder="模型名称（模糊）"
            class="dark-input"
            clearable
          />
        </div>
        <div class="filter-item">
          <label class="filter-label">状态</label>
          <el-select v-model="searchForm.status" placeholder="全部" class="dark-select" clearable>
            <el-option label="全部" :value="null" />
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </div>
        <div class="filter-item">
          <label class="filter-label">渠道ID</label>
          <el-input
            v-model="searchForm.channelId"
            placeholder="渠道ID（可选）"
            class="dark-input"
            clearable
            type="number"
          />
        </div>
        <div class="filter-actions">
          <el-button type="primary" class="search-btn" @click="handleSearch">查询</el-button>
          <el-button class="reset-btn" @click="handleReset">重置</el-button>
          <el-button class="export-btn" :loading="exporting" @click="handleExport">导出 CSV</el-button>
        </div>
      </div>
    </div>

    <!-- 表格区 -->
    <div class="glass-card table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%; background: transparent;"
        :header-cell-style="headerCellStyle"
        :row-style="rowStyle"
        :cell-style="cellStyle"
        @row-mouseenter="handleRowEnter"
        @row-mouseleave="handleRowLeave"
      >
        <el-table-column prop="createdAt" label="时间" width="160">
          <template #default="{ row }">
            <span class="time-cell">{{ formatTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="requestModel" label="请求模型" min-width="140" show-overflow-tooltip />
        <el-table-column prop="modelName" label="实际模型" min-width="140" show-overflow-tooltip />
        <el-table-column prop="channelName" label="渠道" min-width="120" show-overflow-tooltip />
        <el-table-column prop="promptTokens" label="Prompt Tokens" width="120" align="center" />
        <el-table-column prop="completionTokens" label="Completion Tokens" width="140" align="center" />
        <el-table-column prop="totalTokens" label="Total Tokens" width="110" align="center" />
        <el-table-column prop="responseTime" label="响应时间" width="100" align="center">
          <template #default="{ row }">
            <span class="response-time">{{ row.responseTime != null ? row.responseTime + 'ms' : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="weightedAmount" label="加权积分" width="110" align="right">
          <template #default="{ row }">
            <span class="weighted-credits">{{ row.weightedAmount != null ? formatCredits(row.weightedAmount) : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              class="detail-btn"
              @click="handleDetail(row)"
            >详情</el-button>
          </template>
        </el-table-column>
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

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="请求日志详情"
      width="600px"
      class="dark-dialog"
      :close-on-click-modal="false"
    >
      <el-descriptions :column="1" border class="dark-descriptions">
        <el-descriptions-item label="Trace ID">{{ currentLog.traceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户端 IP">{{ currentLog.clientIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求模型">{{ currentLog.requestModel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际模型">{{ currentLog.modelName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ currentLog.channelName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'" size="small">
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="错误码">{{ currentLog.errorCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="错误信息">
          <span class="error-message">{{ currentLog.errorMessage || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="Prompt Tokens">{{ currentLog.promptTokens ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="Completion Tokens">{{ currentLog.completionTokens ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="Total Tokens">{{ currentLog.totalTokens ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="响应时间">{{ currentLog.responseTime != null ? currentLog.responseTime + 'ms' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="自动选模">{{ currentLog.isAutoMode === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="选中模型">{{ currentLog.selectedModel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="调度策略">{{ currentLog.selectionStrategy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatTime(currentLog.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false" class="cancel-btn">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getLogList, exportLogs } from '@/api/log'

// 搜索表单
const searchForm = reactive({
  timeRange: null,
  modelName: '',
  status: null,
  channelId: ''
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

// 导出
const exporting = ref(false)

// 详情弹窗
const detailVisible = ref(false)
const currentLog = ref({})

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

const handleRowEnter = (row, column, event) => {
  event.currentTarget.style.background = 'rgba(59, 130, 246, 0.08)'
}

const handleRowLeave = (row, column, event) => {
  event.currentTarget.style.background = 'transparent'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return time
}

// 格式化积分
const formatCredits = (val) => {
  if (val == null) return '-'
  return Number(val).toLocaleString()
}

// 获取日志列表
const toIsoDateTime = (value) => value?.replace(' ', 'T')

const fetchLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }
    if (searchForm.timeRange && searchForm.timeRange.length === 2) {
      params.startTime = toIsoDateTime(searchForm.timeRange[0])
      params.endTime = toIsoDateTime(searchForm.timeRange[1])
    }
    if (searchForm.modelName) params.modelName = searchForm.modelName
    if (searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
    if (searchForm.channelId) params.channelId = searchForm.channelId

    const res = await getLogList(params)
    if (res.code === 200) {
      tableData.value = res.data.records || res.data.list || res.data || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('获取请求日志失败')
  } finally {
    loading.value = false
  }
}

// 查询
const handleSearch = () => {
  pagination.page = 1
  fetchLogs()
}

// 重置
const handleReset = () => {
  searchForm.timeRange = null
  searchForm.modelName = ''
  searchForm.status = null
  searchForm.channelId = ''
  pagination.page = 1
  fetchLogs()
}

// 分页
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchLogs()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchLogs()
}

// 详情
const handleDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

// 导出 CSV
const handleExport = async () => {
  exporting.value = true
  try {
    const params = {}
    if (searchForm.timeRange && searchForm.timeRange.length === 2) {
      params.startTime = toIsoDateTime(searchForm.timeRange[0])
      params.endTime = toIsoDateTime(searchForm.timeRange[1])
    }
    if (searchForm.modelName) params.modelName = searchForm.modelName
    if (searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
    if (searchForm.channelId) params.channelId = searchForm.channelId

    const blob = await exportLogs(params)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `call_logs_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  fetchLogs()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.request-log-page {
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

.filter-item-wide {
  min-width: 340px;
}

.filter-label {
  font-size: 12px;
  color: rgba(248, 250, 252, 0.6);
  font-weight: 500;
}

.filter-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  padding-bottom: 2px;
}

/* 深色输入框 */
:deep(.dark-input .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  box-shadow: none !important;
}

:deep(.dark-input .el-input__wrapper:hover) {
  border-color: rgba(59, 130, 246, 0.4) !important;
}

:deep(.dark-input .el-input__wrapper.is-focus) {
  border-color: #3B82F6 !important;
}

:deep(.dark-input .el-input__inner) {
  color: #F8FAFC !important;
  font-size: 13px;
}

:deep(.dark-input .el-input__inner::placeholder) {
  color: rgba(248, 250, 252, 0.35);
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

:deep(.dark-select .el-input__inner) {
  color: #F8FAFC !important;
  font-size: 13px;
}

/* 深色 date picker */
:deep(.dark-picker .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  box-shadow: none !important;
}

:deep(.dark-picker .el-input__wrapper:hover) {
  border-color: rgba(59, 130, 246, 0.4) !important;
}

:deep(.dark-picker .el-input__inner) {
  color: #F8FAFC !important;
  font-size: 13px;
}

:deep(.dark-picker .el-range-separator) {
  color: rgba(248, 250, 252, 0.5) !important;
}

:deep(.dark-picker .el-range-input::placeholder) {
  color: rgba(248, 250, 252, 0.35);
}

/* 查询/重置按钮 */
.search-btn {
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%) !important;
  border: none !important;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

.reset-btn {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  color: rgba(248, 250, 252, 0.7) !important;
}

.reset-btn:hover {
  background: rgba(255, 255, 255, 0.1) !important;
  color: #F8FAFC !important;
}

.export-btn {
  background: rgba(16, 185, 129, 0.12) !important;
  border: 1px solid rgba(16, 185, 129, 0.3) !important;
  color: #10b981 !important;
}

.export-btn:hover {
  background: rgba(16, 185, 129, 0.2) !important;
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

:deep(.el-table__header-wrapper) {
  background: rgba(30, 41, 59, 0.6);
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

.response-time {
  color: #06B6D4;
  font-weight: 500;
  font-size: 12px;
}

.weighted-credits {
  color: #A78BFA;
  font-weight: 600;
  font-size: 12px;
  font-family: 'Plus Jakarta Sans', sans-serif;
}

.detail-btn {
  color: #3B82F6 !important;
  font-size: 12px;
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

/* 弹窗 */
:deep(.dark-dialog .el-dialog) {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.95) 0%, rgba(15, 23, 42, 0.98) 100%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.5);
}

:deep(.dark-dialog .el-dialog__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding: 20px 24px 16px;
}

:deep(.dark-dialog .el-dialog__title) {
  color: #F8FAFC;
  font-size: 16px;
  font-weight: 600;
}

:deep(.dark-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: rgba(248, 250, 252, 0.5);
}

:deep(.dark-dialog .el-dialog__headerbtn:hover .el-dialog__close) {
  color: #F8FAFC;
}

:deep(.dark-dialog .el-dialog__body) {
  padding: 20px 24px;
}

:deep(.dark-dialog .el-dialog__footer) {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding: 16px 24px;
}

/* 描述列表 */
:deep(.dark-descriptions .el-descriptions__body) {
  background: transparent;
}

:deep(.dark-descriptions .el-descriptions__label) {
  background: rgba(30, 41, 59, 0.5) !important;
  color: rgba(248, 250, 252, 0.6) !important;
  font-weight: 500;
  font-size: 13px;
}

:deep(.dark-descriptions .el-descriptions__content) {
  background: rgba(15, 23, 42, 0.3) !important;
  color: #F8FAFC !important;
  font-size: 13px;
}

:deep(.dark-descriptions .el-descriptions__cell) {
  border-color: rgba(255, 255, 255, 0.08) !important;
}

.error-message {
  color: #F87171;
  word-break: break-all;
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  color: rgba(248, 250, 252, 0.7) !important;
}

.cancel-btn:hover {
  background: rgba(255, 255, 255, 0.1) !important;
  color: #F8FAFC !important;
}

/* Tag 样式覆盖 */
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
</style>
