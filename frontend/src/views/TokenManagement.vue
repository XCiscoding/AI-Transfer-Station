<template>
  <div class="token-management">
    <!-- Tab切换区域 -->
    <div class="tab-card glass-card">
      <div class="tab-header">
        <el-tabs v-model="activeTab" class="token-tabs" @tab-change="handleTabChange">
          <el-tab-pane label="真实Key管理" name="real">
            <template #label>
              <span class="tab-label">
                <el-icon><Key /></el-icon>
                真实Key管理
              </span>
            </template>
          </el-tab-pane>

          <el-tab-pane label="虚拟Key管理" name="virtual">
            <template #label>
              <span class="tab-label">
                <el-icon><Connection /></el-icon>
                虚拟Key管理
              </span>
            </template>
          </el-tab-pane>
        </el-tabs>
        <el-button type="primary" class="add-btn" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增{{ activeTab === 'real' ? '真实Key' : '虚拟Key' }}
        </el-button>
      </div>
    </div>

    <!-- ==================== Tab1: 真实Key管理 ==================== -->
    <div v-show="activeTab === 'real'">
      <!-- 搜索筛选区域 -->
      <div class="filter-card glass-card">
        <el-form :model="realSearchForm" inline class="filter-form">
          <el-form-item label="搜索">
            <el-input
              v-model="realSearchForm.keyword"
              placeholder="输入名称或Key掩码"
              clearable
              @clear="handleRealSearch"
              @input="debounceRealSearch"
              class="search-input"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="渠道">
            <el-select
              v-model="realSearchForm.channelId"
              placeholder="全部渠道"
              clearable
              @change="handleRealSearch"
              class="channel-select"
            >
              <el-option
                v-for="channel in channelOptions"
                :key="channel.id"
                :label="channel.channelName"
                :value="channel.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button @click="resetRealSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 数据表格区域 -->
      <div class="table-card glass-card">
        <el-table
          :data="realTableData"
          v-loading="realLoading"
          stripe
          border
          style="width: 100%"
          class="token-table real-key-table"
        >
          <el-table-column prop="id" label="ID" width="70" align="center" />

          <el-table-column prop="keyName" label="Key名称" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="key-name">{{ row.keyName }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="keyMask" label="Key值(掩码)" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="key-value-wrapper">
                <span class="key-mask-text">{{ row.keyMask || '***' }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="channelId" label="关联渠道" width="130" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ getChannelName(row.channelId) }}</el-tag>
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
                @change="(val) => handleRealStatusChange(row, val)"
                :loading="row.statusLoading"
              />
            </template>
          </el-table-column>

          <el-table-column prop="expireTime" label="过期时间" width="170" align="center">
            <template #default="{ row }">
              <div class="expire-cell">
                <span>{{ formatTime(row.expireTime) || '永不过期' }}</span>
                <el-tag
                  v-if="getExpireStatus(row.expireTime) === 'soon'"
                  size="small"
                  type="warning"
                  class="expire-tag"
                >
                  即将过期
                </el-tag>
                <el-tag
                  v-else-if="getExpireStatus(row.expireTime) === 'expired'"
                  size="small"
                  type="danger"
                  class="expire-tag"
                >
                  已过期
                </el-tag>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="usageCount" label="使用次数" width="100" align="center" sortable>
            <template #default="{ row }">
              <span class="usage-count">{{ row.usageCount || 0 }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="remark-text">{{ row.remark || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button type="primary" link size="small" @click="handleRealEdit(row)">
                  <el-icon><Edit /></el-icon>编辑
                </el-button>
                <el-button type="danger" link size="small" @click="handleRealDelete(row)">
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页组件 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="realPagination.page"
            v-model:page-size="realPagination.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="realPagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="handleRealSizeChange"
            @current-change="handleRealPageChange"
          />
        </div>
      </div>
    </div>

    <!-- ==================== Tab2: 虚拟Key管理 ==================== -->
    <div v-show="activeTab === 'virtual'">
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

    <!-- ==================== 真实Key新增/编辑弹窗 ==================== -->
    <el-dialog
      v-model="realDialogVisible"
      :title="realDialogTitle"
      width="620px"
      :close-on-click-modal="false"
      class="token-dialog real-key-dialog"
      destroy-on-close
    >
      <el-form
        ref="realFormRef"
        :model="realFormData"
        :rules="realFormRules"
        label-width="110px"
        label-position="right"
        class="token-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Key名称" prop="keyName">
              <el-input v-model="realFormData.keyName" placeholder="请输入Key名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联渠道" prop="channelId">
              <el-select v-model="realFormData.channelId" placeholder="请选择渠道" style="width: 100%">
                <el-option
                  v-for="channel in channelOptions"
                  :key="channel.id"
                  :label="channel.channelName"
                  :value="channel.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="API Key值" prop="keyValue">
          <el-input
            v-model="realFormData.keyValue"
            type="password"
            show-password
            :placeholder="isRealEdit ? '留空则不修改Key值' : '请输入真实的API Key值'"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch
                v-model="realFormData.status"
                :active-value="1"
                :inactive-value="0"
                active-color="#3B82F6"
                inactive-color="#94A3B8"
                active-text="启用"
                inactive-text="禁用"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="过期时间">
              <el-date-picker
                v-model="realFormData.expireTime"
                type="datetime"
                placeholder="选择过期时间（可选）"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注">
          <el-input
            v-model="realFormData.remark"
            type="textarea"
            :rows="3"
            placeholder="可选，添加备注信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="realDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="realSubmitLoading" @click="handleRealSubmit">
            {{ isRealEdit ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

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
  Key,
  Connection,
  DocumentCopy,
  Refresh
} from '@element-plus/icons-vue'
import {
  getRealKeyList,
  createRealKey,
  updateRealKey,
  toggleRealKeyStatus,
  deleteRealKey
} from '@/api/realkey'
import {
  getVirtualKeyList,
  createVirtualKey,
  updateVirtualKey,
  refreshVirtualKey,
  toggleVirtualKeyStatus,
  deleteVirtualKey
} from '@/api/virtualkey'
import { getChannelList } from '@/api/channel'

// ==================== 全局状态 ====================

const activeTab = ref('real')
const channelOptions = ref([])

// 获取当前登录用户ID（从 localStorage 中的 userInfo 或 token 解析）
function getCurrentUserId() {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return userInfo.userId || userInfo.id || 1
  } catch {
    return 1
  }
}

// ==================== Tab1: 真实Key相关数据 ====================

const realTableData = ref([])
const realLoading = ref(false)

const realSearchForm = reactive({
  keyword: '',
  channelId: ''
})

const realPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const realDialogVisible = ref(false)
const realDialogTitle = computed(() => isRealEdit.value ? '编辑真实Key' : '新增真实Key')
const isRealEdit = ref(false)
const currentRealEditId = ref(null)
const realSubmitLoading = ref(false)

const realFormRef = ref()
const realFormData = reactive({
  keyName: '',
  keyValue: '',
  channelId: '',
  status: 1,
  expireTime: null,
  remark: ''
})

const realFormRules = {
  keyName: [
    { required: true, message: '请输入Key名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在2到100个字符', trigger: 'blur' }
  ],
  channelId: [
    { required: true, message: '请选择关联渠道', trigger: 'change' }
  ],
  keyValue: [
    {
      validator: (rule, value, callback) => {
        if (!isRealEdit.value && !value) {
          callback(new Error('请输入API Key值'))
        } else if (value && value.length < 10) {
          callback(new Error('API Key长度至少10个字符'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

let realSearchTimer = null

// ==================== Tab2: 虚拟Key相关数据 ====================

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
  fetchChannelOptions()
  fetchRealKeyList()
})

// ==================== 公共方法 ====================

async function fetchChannelOptions() {
  try {
    const res = await getChannelList({ page: 1, size: 1000 })
    if (res.code === 200 && res.data) {
      channelOptions.value = res.data.records || []
    }
  } catch (error) {
    console.error('获取渠道列表失败:', error)
  }
}

function getChannelName(channelId) {
  const channel = channelOptions.value.find(c => c.id === channelId)
  return channel ? channel.channelName : '-'
}

// ==================== Tab1: 真实Key API方法 ====================

async function fetchRealKeyList() {
  realLoading.value = true
  try {
    const params = {
      page: realPagination.page,
      size: realPagination.size,
      keyword: realSearchForm.keyword || undefined,
      channelId: realSearchForm.channelId || undefined
    }

    const res = await getRealKeyList(params)

    if (res.code === 200 && res.data) {
      realTableData.value = (res.data.records || []).map(item => ({
        ...item,
        statusLoading: false
      }))
      realPagination.total = res.data.total || 0
    } else {
      ElMessage.error(res.message || '获取真实Key列表失败')
    }
  } catch (error) {
    console.error('获取真实Key列表失败:', error)
  } finally {
    realLoading.value = false
  }
}

async function handleRealCreate() {
  realSubmitLoading.value = true
  try {
    const data = { ...realFormData }
    const res = await createRealKey(data)

    if (res.code === 200) {
      ElMessage.success('真实Key创建成功')
      realDialogVisible.value = false
      fetchRealKeyList()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error) {
    console.error('创建真实Key失败:', error)
  } finally {
    realSubmitLoading.value = false
  }
}

async function handleRealUpdate() {
  if (!currentRealEditId.value) return

  realSubmitLoading.value = true
  try {
    const data = { ...realFormData }
    if (!data.keyValue) {
      delete data.keyValue
    }
    const res = await updateRealKey(currentRealEditId.value, data)

    if (res.code === 200) {
      ElMessage.success('真实Key更新成功')
      realDialogVisible.value = false
      fetchRealKeyList()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (error) {
    console.error('更新真实Key失败:', error)
  } finally {
    realSubmitLoading.value = false
  }
}

async function handleRealDeleteAction(id) {
  try {
    const res = await deleteRealKey(id)

    if (res.code === 200) {
      ElMessage.success('删除成功')
      if (realTableData.value.length === 1 && realPagination.page > 1) {
        realPagination.page--
      }
      fetchRealKeyList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    console.error('删除真实Key失败:', error)
  }
}

// ==================== Tab2: 虚拟Key API方法 ====================

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

// ==================== Tab1: 真实Key事件处理 ====================

function debounceRealSearch() {
  if (realSearchTimer) clearTimeout(realSearchTimer)
  realSearchTimer = setTimeout(() => {
    realPagination.page = 1
    fetchRealKeyList()
  }, 500)
}

function handleRealSearch() {
  realPagination.page = 1
  fetchRealKeyList()
}

function resetRealSearch() {
  realSearchForm.keyword = ''
  realSearchForm.channelId = ''
  realPagination.page = 1
  fetchRealKeyList()
}

function handleRealSizeChange(val) {
  realPagination.size = val
  realPagination.page = 1
  fetchRealKeyList()
}

function handleRealPageChange(val) {
  realPagination.page = val
  fetchRealKeyList()
}

function handleAdd() {
  if (activeTab.value === 'real') {
    isRealEdit.value = false
    currentRealEditId.value = null
    resetRealFormData()
    realDialogVisible.value = true
  } else {
    isVirtualEdit.value = false
    currentVirtualEditId.value = null
    resetVirtualFormData()
    virtualDialogVisible.value = true
  }
}

function handleRealEdit(row) {
  isRealEdit.value = true
  currentRealEditId.value = row.id
  Object.assign(realFormData, {
    keyName: row.keyName,
    keyValue: '',
    channelId: row.channelId,
    status: row.status,
    expireTime: row.expireTime || null,
    remark: row.remark || ''
  })
  realDialogVisible.value = true
}

function handleRealSubmit() {
  realFormRef.value?.validate((valid) => {
    if (valid) {
      if (isRealEdit.value) {
        handleRealUpdate()
      } else {
        handleRealCreate()
      }
    }
  })
}

function handleRealDelete(row) {
  ElMessageBox.confirm(
    `确定要删除真实Key「${row.keyName}」吗？删除后不可恢复。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(() => {
    handleRealDeleteAction(row.id)
  }).catch(() => {})
}

async function handleRealStatusChange(row, newStatus) {
  row.statusLoading = true
  try {
    const res = await toggleRealKeyStatus(row.id)

    if (res.code === 200) {
      ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    } else {
      // 回滚状态
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

// ==================== Tab2: 虚拟Key事件处理 ====================

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

// ==================== Tab切换 ====================

function handleTabChange(tabName) {
  if (tabName === 'real' && realTableData.value.length === 0) {
    fetchRealKeyList()
  } else if (tabName === 'virtual' && virtualTableData.value.length === 0) {
    fetchVirtualKeyList()
  }
}

// ==================== 工具方法 ====================

function resetRealFormData() {
  Object.assign(realFormData, {
    keyName: '',
    keyValue: '',
    channelId: '',
    status: 1,
    expireTime: null,
    remark: ''
  })
  realFormRef.value?.resetFields()
}

function resetVirtualFormData() {
  Object.assign(virtualFormData, {
    keyName: '',
    quotaType: 'token',
    quotaLimit: 100000,
    rateLimitQpm: 60,
    rateLimitQpd: 0,
    expireTime: null,
    allowedModels: '',
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

.token-tabs {
  --el-tabs-header-height: 48px;
}

.token-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
  border-bottom: none;
}

.token-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.token-tabs :deep(.el-tabs__item) {
  height: 44px;
  line-height: 44px;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(248, 250, 252, 0.65);
  transition: all 0.3s ease;
  border-radius: 10px;
  margin-right: 8px;
}

.token-tabs :deep(.el-tabs__item:hover) {
  color: rgba(248, 250, 252, 0.85);
  background: rgba(255, 255, 255, 0.05);
}

.token-tabs :deep(.el-tabs__item.is-active) {
  color: #3B82F6;
  background: rgba(59, 130, 246, 0.12);
  font-weight: 600;
}

.token-tabs :deep(.el-tabs__active-bar) {
  display: none;
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
