<template>
  <div class="token-management">
    <!-- 标题区域 -->
    <div class="tab-card glass-card">
      <div class="tab-header">
        <span class="tab-label page-title">
          <el-icon><Connection /></el-icon>
          虚拟Key管理
        </span>
        <el-button type="primary" class="add-btn" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增虚拟Key
        </el-button>
      </div>
    </div>

    <!-- ==================== 虚拟Key管理 ==================== -->
    <div>
      <!-- 搜索筛选区域 -->
      <div class="filter-card glass-card">
        <el-form :model="virtualSearchForm" inline class="filter-form">
          <el-form-item label="搜索">
            <el-input
              v-model="virtualSearchForm.keyword"
              placeholder="输入虚拟Key名称"
              clearable
              @clear="handleVirtualSearch"
              @input="debounceVirtualSearch"
              class="search-input"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="状态">
            <el-select
              v-model="virtualSearchForm.status"
              placeholder="全部状态"
              clearable
              @change="handleVirtualSearch"
              class="status-select"
            >
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button @click="resetVirtualSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 数据表格区域 -->
      <div class="table-card glass-card">
        <el-table
          :data="virtualTableData"
          v-loading="virtualLoading"
          stripe
          border
          style="width: 100%"
          class="token-table virtual-key-table"
        >
          <el-table-column prop="id" label="ID" width="70" align="center" />

          <el-table-column prop="keyName" label="Key名称" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="key-name">{{ row.keyName }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="keyValue" label="虚拟Key值" min-width="280" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="key-value-wrapper">
                <span class="virtual-key-code">{{ row.keyValue }}</span>
                <el-button
                  type="primary"
                  link
                  size="small"
                  class="copy-btn"
                  @click="handleCopyVirtualKey(row)"
                >
                  <el-icon><DocumentCopy /></el-icon>
                  复制
                </el-button>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="quotaType" label="额度类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="getQuotaTypeTagType(row.quotaType)">
                {{ getQuotaTypeLabel(row.quotaType) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="额度使用" width="180" align="center">
            <template #default="{ row }">
              <div class="quota-cell">
                <el-progress
                  :percentage="getQuotaPercent(row)"
                  :color="getQuotaColor(row)"
                  :stroke-width="6"
                  style="width: 120px"
                />
                <span class="quota-text">{{ row.quotaUsed }} / {{ row.quotaLimit }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                active-color="#3B82F6"
                inactive-color="#94A3B8"
                @change="(val) => handleVirtualStatusChange(row, val)"
                :loading="row.statusLoading"
              />
            </template>
          </el-table-column>

          <el-table-column prop="expireTime" label="过期时间" width="160" align="center">
            <template #default="{ row }">
              {{ formatTime(row.expireTime) || '永不过期' }}
            </template>
          </el-table-column>

          <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="200" align="center">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button type="primary" link size="small" @click="handleVirtualEdit(row)">
                  <el-icon><Edit /></el-icon>编辑
                </el-button>
                <el-button type="warning" link size="small" @click="handleVirtualRefresh(row)">
                  <el-icon><Refresh /></el-icon>刷新
                </el-button>
                <el-button type="danger" link size="small" @click="handleVirtualDelete(row)">
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页组件 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="virtualPagination.page"
            v-model:page-size="virtualPagination.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="virtualPagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="handleVirtualSizeChange"
            @current-change="handleVirtualPageChange"
          />
        </div>
      </div>
    </div>

    <!-- ==================== 虚拟Key新增/编辑弹窗 ==================== -->
    <el-dialog
      v-model="virtualDialogVisible"
      :title="virtualDialogTitle"
      width="680px"
      :close-on-click-modal="false"
      class="token-dialog virtual-key-dialog"
      destroy-on-close
    >
      <el-form
        ref="virtualFormRef"
        :model="virtualFormData"
        :rules="virtualFormRules"
        label-width="110px"
        label-position="right"
        class="token-form"
      >
        <el-form-item label="Key名称" prop="keyName">
          <el-input v-model="virtualFormData.keyName" placeholder="请输入Key名称" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="额度类型" prop="quotaType">
              <el-select v-model="virtualFormData.quotaType" placeholder="请选择额度类型" style="width: 100%">
                <el-option label="Token数量" value="token" />
                <el-option label="调用次数" value="count" />
                <el-option label="金额(元)" value="amount" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="额度上限" prop="quotaLimit">
              <el-input-number
                v-model="virtualFormData.quotaLimit"
                :min="1"
                :precision="0"
                style="width: 100%"
                placeholder="请输入额度上限"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="每分钟限制">
              <el-input-number
                v-model="virtualFormData.rateLimitQpm"
                :min="0"
                :max="10000"
                style="width: 100%"
                placeholder="0表示不限制"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="每日限制">
              <el-input-number
                v-model="virtualFormData.rateLimitQpd"
                :min="0"
                style="width: 100%"
                placeholder="0表示不限制"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="过期时间">
              <el-date-picker
                v-model="virtualFormData.expireTime"
                type="datetime"
                placeholder="选择过期时间（可选）"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="允许模型">
              <el-input
                v-model="virtualFormData.allowedModels"
                placeholder='如: ["gpt-4"] 空表示不限'
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="模型分组">
          <el-select
            v-model="virtualFormData.allowedGroupIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择允许的模型分组（可多选，不选则不限）"
            style="width: 100%"
          >
            <el-option
              v-for="group in modelGroupOptions"
              :key="group.id"
              :label="group.groupName"
              :value="group.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="virtualFormData.remark"
            type="textarea"
            :rows="3"
            placeholder="可选，添加备注信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="virtualDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="virtualSubmitLoading" @click="handleVirtualSubmit">
            {{ isVirtualEdit ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Search,
  Edit,
  Delete,
  Connection,
  DocumentCopy,
  Refresh
} from '@element-plus/icons-vue'
import {
  getVirtualKeyList,
  createVirtualKey,
  updateVirtualKey,
  refreshVirtualKey,
  toggleVirtualKeyStatus,
  deleteVirtualKey
} from '@/api/virtualkey'
import { getModelGroupAll } from '@/api/modelgroup'

// ==================== 全局状态 ====================

// 模型分组列表
const modelGroupOptions = ref([])

async function fetchModelGroups() {
  try {
    const res = await getModelGroupAll()
    if (res.code === 200 && res.data) {
      modelGroupOptions.value = res.data
    } else {
      modelGroupOptions.value = []
    }
  } catch (e) {
    modelGroupOptions.value = []
    console.warn('获取模型分组失败:', e)
  }
}

// 获取当前登录用户ID（从 localStorage 中的 userInfo 或 token 解析）
function getCurrentUserId() {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return userInfo.userId || userInfo.id || 1
  } catch {
    return 1
  }
}

// ==================== 虚拟Key相关数据 ====================

const virtualTableData = ref([])
const virtualLoading = ref(false)

const virtualSearchForm = reactive({
  keyword: '',
  status: ''
})

const virtualPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const virtualDialogVisible = ref(false)
const virtualDialogTitle = computed(() => isVirtualEdit.value ? '编辑虚拟Key' : '新增虚拟Key')
const isVirtualEdit = ref(false)
const currentVirtualEditId = ref(null)
const virtualSubmitLoading = ref(false)

const virtualFormRef = ref()
const virtualFormData = reactive({
  keyName: '',
  quotaType: 'token',
  quotaLimit: 100000,
  rateLimitQpm: 60,
  rateLimitQpd: 0,
  expireTime: null,
  allowedModels: '',
  allowedGroupIds: [],
  remark: ''
})

const virtualFormRules = {
  keyName: [
    { required: true, message: '请输入Key名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在2到100个字符', trigger: 'blur' }
  ],
  quotaType: [
    { required: true, message: '请选择额度类型', trigger: 'change' }
  ],
  quotaLimit: [
    { required: true, message: '请输入额度上限', trigger: 'blur' }
  ]
}

let virtualSearchTimer = null

// ==================== 生命周期 ====================

onMounted(() => {
  fetchVirtualKeyList()
  fetchModelGroups()
})

// ==================== 虚拟Key API方法 ====================

async function fetchVirtualKeyList() {
  virtualLoading.value = true
  try {
    const params = {
      page: virtualPagination.page,
      size: virtualPagination.size,
      keyword: virtualSearchForm.keyword || undefined,
      status: virtualSearchForm.status !== '' ? virtualSearchForm.status : undefined
    }

    const res = await getVirtualKeyList(params)

    if (res.code === 200 && res.data) {
      virtualTableData.value = (res.data.records || []).map(item => ({
        ...item,
        statusLoading: false
      }))
      virtualPagination.total = res.data.total || 0
    } else {
      ElMessage.error(res.message || '获取虚拟Key列表失败')
    }
  } catch (error) {
    console.error('获取虚拟Key列表失败:', error)
  } finally {
    virtualLoading.value = false
  }
}

async function handleVirtualCreate() {
  virtualSubmitLoading.value = true
  try {
    const data = {
      ...virtualFormData,
      userId: getCurrentUserId()
    }
    const res = await createVirtualKey(data)

    if (res.code === 200) {
      ElMessage.success('虚拟Key创建成功')
      virtualDialogVisible.value = false
      fetchVirtualKeyList()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error) {
    console.error('创建虚拟Key失败:', error)
  } finally {
    virtualSubmitLoading.value = false
  }
}

async function handleVirtualUpdate() {
  if (!currentVirtualEditId.value) return

  virtualSubmitLoading.value = true
  try {
    const data = { ...virtualFormData }
    const res = await updateVirtualKey(currentVirtualEditId.value, data)

    if (res.code === 200) {
      ElMessage.success('虚拟Key更新成功')
      virtualDialogVisible.value = false
      fetchVirtualKeyList()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (error) {
    console.error('更新虚拟Key失败:', error)
  } finally {
    virtualSubmitLoading.value = false
  }
}

async function handleVirtualDeleteAction(id) {
  try {
    const res = await deleteVirtualKey(id)

    if (res.code === 200) {
      ElMessage.success('删除成功')
      if (virtualTableData.value.length === 1 && virtualPagination.page > 1) {
        virtualPagination.page--
      }
      fetchVirtualKeyList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    console.error('删除虚拟Key失败:', error)
  }
}

// ==================== 虚拟Key事件处理 ====================

function handleAdd() {
  isVirtualEdit.value = false
  currentVirtualEditId.value = null
  resetVirtualFormData()
  virtualDialogVisible.value = true
}

function debounceVirtualSearch() {
  if (virtualSearchTimer) clearTimeout(virtualSearchTimer)
  virtualSearchTimer = setTimeout(() => {
    virtualPagination.page = 1
    fetchVirtualKeyList()
  }, 500)
}

function handleVirtualSearch() {
  virtualPagination.page = 1
  fetchVirtualKeyList()
}

function resetVirtualSearch() {
  virtualSearchForm.keyword = ''
  virtualSearchForm.status = ''
  virtualPagination.page = 1
  fetchVirtualKeyList()
}

function handleVirtualSizeChange(val) {
  virtualPagination.size = val
  virtualPagination.page = 1
  fetchVirtualKeyList()
}

function handleVirtualPageChange(val) {
  virtualPagination.page = val
  fetchVirtualKeyList()
}

function handleVirtualEdit(row) {
  isVirtualEdit.value = true
  currentVirtualEditId.value = row.id
  Object.assign(virtualFormData, {
    keyName: row.keyName,
    quotaType: row.quotaType,
    quotaLimit: Number(row.quotaLimit),
    rateLimitQpm: row.rateLimitQpm ?? 60,
    rateLimitQpd: row.rateLimitQpd ?? 0,
    expireTime: row.expireTime || null,
    allowedModels: row.allowedModels || '',
    allowedGroupIds: row.allowedGroupIds || [],
    remark: row.remark || ''
  })
  virtualDialogVisible.value = true
}

function handleVirtualSubmit() {
  virtualFormRef.value?.validate((valid) => {
    if (valid) {
      if (isVirtualEdit.value) {
        handleVirtualUpdate()
      } else {
        handleVirtualCreate()
      }
    }
  })
}

async function handleVirtualRefresh(row) {
  try {
    await ElMessageBox.confirm(
      `刷新后，旧Key值「${row.keyValue}」立即失效，确定继续？`,
      '刷新Key值',
      {
        confirmButtonText: '确定刷新',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await refreshVirtualKey(row.id)
    if (res.code === 200) {
      ElMessage.success('Key值已刷新')
      fetchVirtualKeyList()
    } else {
      ElMessage.error(res.message || '刷新失败')
    }
  } catch {
    // 用户取消
  }
}

function handleVirtualDelete(row) {
  ElMessageBox.confirm(
    `确定要删除虚拟Key「${row.keyName}」吗？删除后不可恢复。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(() => {
    handleVirtualDeleteAction(row.id)
  }).catch(() => {})
}

async function handleVirtualStatusChange(row, newStatus) {
  row.statusLoading = true
  try {
    const res = await toggleVirtualKeyStatus(row.id)

    if (res.code === 200) {
      ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    } else {
      row.status = newStatus === 1 ? 0 : 1
      ElMessage.error(res.message || '状态更新失败')
    }
  } catch (error) {
    console.error('状态更新失败:', error)
    row.status = newStatus === 1 ? 0 : 1
    ElMessage.error(error.message || '状态更新失败')
  } finally {
    row.statusLoading = false
  }
}

// ==================== 工具方法 ====================

function resetVirtualFormData() {
  Object.assign(virtualFormData, {
    keyName: '',
    quotaType: 'token',
    quotaLimit: 100000,
    rateLimitQpm: 60,
    rateLimitQpd: 0,
    expireTime: null,
    allowedModels: '',
    allowedGroupIds: [],
    remark: ''
  })
  virtualFormRef.value?.resetFields()
}

async function handleCopyVirtualKey(row) {
  try {
    await navigator.clipboard.writeText(row.keyValue || '')
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

function getExpireStatus(expireTime) {
  if (!expireTime) return 'none'
  const now = new Date()
  const expire = new Date(expireTime)
  const diffDays = Math.ceil((expire - now) / (1000 * 60 * 60 * 24))
  if (diffDays < 0) return 'expired'
  if (diffDays <= 7) return 'soon'
  return 'normal'
}

function getQuotaPercent(row) {
  if (!row.quotaLimit || Number(row.quotaLimit) === 0) return 0
  return Math.min(Math.round((Number(row.quotaUsed) / Number(row.quotaLimit)) * 100), 100)
}

function getQuotaColor(row) {
  const pct = getQuotaPercent(row)
  if (pct >= 90) return '#EF4444'
  if (pct >= 70) return '#F59E0B'
  return '#3B82F6'
}

function getQuotaTypeLabel(type) {
  const map = { token: 'Token', count: '次数', amount: '金额' }
  return map[type] || type
}

function getQuotaTypeTagType(type) {
  const map = { token: 'primary', count: 'success', amount: 'warning' }
  return map[type] || 'info'
}
</script>

<style scoped>
.token-management {
  padding: 0;
}

.add-btn {
  height: 36px;
  padding: 0 18px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 8px;
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
  border: none;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.4);
  transition: all 0.3s ease;
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.5);
}

.tab-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.glass-card {
  background:
    linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.8) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  transition: all 0.3s ease;
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.05) inset,
    0 8px 32px rgba(0, 0, 0, 0.2);
  animation: fadeInUp 0.5s ease-out;
  animation-delay: 0.1s;
  animation-fill-mode: backwards;
}

.glass-card:hover {
  border-color: rgba(59, 130, 246, 0.18);
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.08) inset,
    0 12px 40px rgba(0, 0, 0, 0.25);
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

.tab-card {
  animation-delay: 0.05s;
  padding: 4px 20px 16px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #F8FAFC;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-label .el-icon {
  font-size: 16px;
}

.filter-card {
  animation-delay: 0.1s;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 0;
}

.filter-form :deep(.el-form-item__label) {
  color: rgba(248, 250, 252, 0.7);
  font-weight: 500;
  font-size: 13px;
}

.search-input {
  width: 280px;
}

.channel-select,
.status-select {
  width: 160px;
}

.filter-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: none;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.filter-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(255, 255, 255, 0.08);
}

.filter-form :deep(.el-input__wrapper.is-focus) {
  border-color: #3B82F6;
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.filter-form :deep(.el-input__inner) {
  color: #F8FAFC;
}

.filter-form :deep(.el-input__inner::placeholder) {
  color: rgba(248, 250, 252, 0.35);
}

.filter-form :deep(.el-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
}

.table-card {
  animation-delay: 0.2s;
  padding: 0;
  overflow: hidden;
}

.token-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(30, 41, 59, 0.6);
  --el-table-row-hover-bg-color: rgba(59, 130, 246, 0.08);
  --el-table-border-color: rgba(255, 255, 255, 0.08);
  --el-table-header-text-color: rgba(248, 250, 252, 0.85);
  --el-table-text-color: #F8FAFC;
  --el-font-size-base: 13px;
}

.token-table :deep(.el-table__header th) {
  font-weight: 600;
  letter-spacing: 0.3px;
  text-transform: uppercase;
  font-size: 12px;
  background: rgba(30, 41, 59, 0.7) !important;
  border-bottom: 2px solid rgba(59, 130, 246, 0.2);
}

.token-table :deep(.el-table__body tr) {
  transition: all 0.2s ease;
}

.token-table :deep(.el-table__body tr:hover > td) {
  background: rgba(59, 130, 246, 0.06) !important;
}

.token-table :deep(.el-table__body td) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  padding: 12px 0;
}

.token-table :deep(.el-table__body tr.el-table__row--striped td) {
  background: rgba(255, 255, 255, 0.02) !important;
}

.key-name {
  font-weight: 600;
  color: #F8FAFC;
  font-size: 14px;
}

.key-value-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.key-mask-text {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  color: rgba(248, 250, 252, 0.6);
  background: rgba(255, 255, 255, 0.04);
  padding: 4px 10px;
  border-radius: 6px;
  display: inline-block;
  flex: 1;
}

.copy-btn {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  flex-shrink: 0;
}

.virtual-key-code {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  color: #A78BFA;
  background: rgba(167, 139, 250, 0.1);
  padding: 4px 10px;
  border-radius: 6px;
  display: inline-block;
  font-weight: 500;
  flex: 1;
  word-break: break-all;
}

.expire-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.expire-tag {
  font-size: 11px;
  padding: 0 6px;
}

.usage-count {
  font-weight: 600;
  color: #3B82F6;
  font-size: 14px;
}

.remark-text {
  color: rgba(248, 250, 252, 0.55);
  font-size: 13px;
}

.quota-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.quota-text {
  font-size: 11px;
  color: rgba(248, 250, 252, 0.55);
}

.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
}

.action-buttons .el-button {
  font-size: 12px;
  font-weight: 500;
  padding: 4px 6px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.pagination-wrapper :deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: rgba(248, 250, 252, 0.7);
  --el-pagination-button-bg-color: rgba(255, 255, 255, 0.06);
  --el-pagination-hover-color: #3B82F6;
}

.pagination-wrapper :deep(.el-pager li) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  margin: 0 3px;
  color: rgba(248, 250, 252, 0.7);
  transition: all 0.2s ease;
}

.pagination-wrapper :deep(.el-pager li:hover) {
  background: rgba(59, 130, 246, 0.15);
  border-color: rgba(59, 130, 246, 0.3);
  color: #3B82F6;
}

.pagination-wrapper :deep(.el-pager li.is-active) {
  background: #3B82F6;
  border-color: #3B82F6;
  color: white;
  font-weight: 600;
}

.token-dialog :deep(.el-dialog) {
  background:
    linear-gradient(135deg, rgba(30, 41, 59, 0.95) 0%, rgba(15, 23, 42, 0.98) 100%);
  backdrop-filter: blur(30px) saturate(180%);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.1) inset,
    0 24px 64px rgba(0, 0, 0, 0.5);
}

.token-dialog :deep(.el-dialog__header) {
  padding: 24px 24px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.token-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: #F8FAFC;
}

.token-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 65vh;
  overflow-y: auto;
}

.token-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.token-form :deep(.el-form-item__label) {
  color: rgba(248, 250, 252, 0.75);
  font-weight: 500;
  font-size: 13px;
}

.token-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: none;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.token-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(255, 255, 255, 0.08);
}

.token-form :deep(.el-input__wrapper.is-focus) {
  border-color: #3B82F6;
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.token-form :deep(.el-input__inner) {
  color: #F8FAFC;
}

.token-form :deep(.el-input__inner::placeholder) {
  color: rgba(248, 250, 252, 0.35);
}

.token-form :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #F8FAFC;
  border-radius: 8px;
}

.token-form :deep(.el-textarea__inner::placeholder) {
  color: rgba(248, 250, 252, 0.35);
}

.token-form :deep(.el-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
}

.token-form :deep(.el-date-editor) {
  --el-input-bg-color: rgba(255, 255, 255, 0.06);
  --el-input-border-color: rgba(255, 255, 255, 0.12);
  --el-input-text-color: #F8FAFC;
}

.token-form :deep(.el-date-editor .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
}

.token-form :deep(.el-input-number .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
}

.token-table :deep(.el-switch.is-checked .el-switch__core) {
  background: #3B82F6;
  border-color: #3B82F6;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-footer .el-button {
  padding: 10px 24px;
  border-radius: 8px;
  font-weight: 500;
  font-size: 14px;
}

@media (max-width: 1280px) {
  .search-input {
    width: 240px;
  }

  .channel-select,
  .status-select {
    width: 140px;
  }
}

@media (max-width: 768px) {
  .filter-form {
    flex-direction: column;
  }

  .search-input,
  .channel-select,
  .status-select {
    width: 100%;
  }

  .token-dialog :deep(.el-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }

  .token-tabs :deep(.el-tabs__item) {
    padding: 0 16px;
    font-size: 13px;
  }
}
</style>
