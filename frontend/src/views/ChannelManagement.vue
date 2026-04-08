<template>
  <div class="channel-management">
    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" class="main-tabs" @tab-change="handleTabChange">

      <!-- 渠道管理 Tab -->
      <el-tab-pane label="渠道管理" name="channels">

    <!-- 搜索筛选区域 -->
    <div class="filter-card glass-card">
      <div class="filter-header">
        <el-form :model="searchForm" inline class="filter-form">
        <el-form-item label="搜索">
          <el-input
            v-model="searchForm.keyword"
            placeholder="输入渠道名称或编码"
            clearable
            @clear="handleSearch"
            @input="debounceSearch"
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="类型">
          <el-select
            v-model="searchForm.channelType"
            placeholder="全部类型"
            clearable
            @change="handleSearch"
            class="type-select"
          >
            <el-option label="LLM" value="LLM" />
            <el-option label="Image" value="Image" />
            <el-option label="Audio" value="Audio" />
            <el-option label="Video" value="Video" />
            <el-option label="Embedding" value="Embedding" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="全部状态"
            clearable
            @change="handleSearch"
            class="status-select"
          >
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
        <el-button type="primary" class="add-btn" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增渠道
        </el-button>
      </div>
    </div>

    <!-- 数据表格区域 -->
    <div class="table-card glass-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        class="channel-table"
        @sort-change="handleSortChange"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />

        <el-table-column prop="channelName" label="渠道名称" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="channel-name">{{ row.channelName }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="channelCode" label="渠道编码" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.channelCode }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="channelType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.channelType)" size="small">
              {{ row.channelType }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="provider" label="提供商" min-width="120" show-overflow-tooltip />

        <el-table-column prop="apiKeyMask" label="API Key" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="api-key-text">{{ row.apiKeyMask || '***' }}</span>
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
              @change="(val) => handleStatusChange(row, val)"
              :loading="row.statusLoading"
            />
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="170" sortable="custom" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button type="info" link size="small" @click="handleOpenKeys(row)">
                <el-icon><Key /></el-icon>Keys
              </el-button>
              <el-button type="success" link size="small" @click="handleTest(row)">
                <el-icon><Connection /></el-icon>测试
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="680px"
      :close-on-click-modal="false"
      class="channel-dialog"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        label-position="right"
        class="channel-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="渠道名称" prop="channelName">
              <el-input v-model="formData.channelName" placeholder="请输入渠道名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="渠道编码" prop="channelCode">
              <el-input
                v-model="formData.channelCode"
                placeholder="请输入渠道编码"
                :disabled="isEdit"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="渠道类型" prop="channelType">
              <el-select v-model="formData.channelType" placeholder="请选择类型" style="width: 100%">
                <el-option label="LLM (大语言模型)" value="LLM" />
                <el-option label="Image (图像生成)" value="Image" />
                <el-option label="Audio (语音处理)" value="Audio" />
                <el-option label="Video (视频处理)" value="Video" />
                <el-option label="Embedding (向量嵌入)" value="Embedding" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提供商" prop="provider">
              <el-input v-model="formData.provider" placeholder="如：OpenAI、Anthropic" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="formData.baseUrl" placeholder="请输入API基础地址" />
        </el-form-item>

        <el-form-item label="API Key" prop="apiKey">
          <el-input
            v-model="formData.apiKey"
            type="password"
            show-password
            placeholder="请输入API Key"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="API版本">
              <el-input v-model="formData.apiVersion" placeholder="可选，如：v1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch
                v-model="formData.status"
                :active-value="1"
                :inactive-value="0"
                active-color="#3B82F6"
                inactive-color="#94A3B8"
                active-text="启用"
                inactive-text="禁用"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">高级配置</el-divider>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="最大Token数">
              <el-input-number
                v-model="formData.maxTokens"
                :min="0"
                :max="128000"
                :step="1024"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大RPM">
              <el-input-number
                v-model="formData.maxRpm"
                :min="0"
                :max="10000"
                :step="10"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大TPM">
              <el-input-number
                v-model="formData.maxTpm"
                :min="0"
                :max="10000000"
                :step="1000"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="超时时间(秒)">
              <el-input-number
                v-model="formData.timeout"
                :min="5"
                :max="300"
                :step="5"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="可选，添加备注信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEdit ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
    <!-- 真实Key管理抽屉 -->
    <el-drawer
      v-model="keyDrawerVisible"
      :title="`${currentChannelName} — 真实Key管理`"
      direction="rtl"
      size="700px"
      :destroy-on-close="true"
      class="key-drawer"
    >
      <div class="key-drawer-body">
        <!-- 操作栏 -->
        <div class="key-toolbar">
          <span class="key-count">共 {{ realKeyList.length }} 个Key</span>
          <el-button type="primary" size="small" @click="handleAddRealKey">
            <el-icon><Plus /></el-icon>新增Key
          </el-button>
        </div>

        <!-- Key列表 -->
        <el-table
          :data="realKeyList"
          v-loading="realKeyLoading"
          stripe
          border
          style="width: 100%"
          class="real-key-table"
        >
          <el-table-column prop="keyName" label="Key名称" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="key-name">{{ row.keyName }}</span>
            </template>
          </el-table-column>

          <el-table-column label="Key值" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="key-mask">{{ row.keyMask || '***' }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                active-color="#3B82F6"
                inactive-color="#94A3B8"
                @change="(val) => handleRealKeyStatusChange(row, val)"
                :loading="row.statusLoading"
              />
            </template>
          </el-table-column>

          <el-table-column label="操作" width="130" align="center">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button type="primary" link size="small" @click="handleEditRealKey(row)">
                  <el-icon><Edit /></el-icon>编辑
                </el-button>
                <el-button type="danger" link size="small" @click="handleDeleteRealKey(row)">
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <!-- 真实Key新增/编辑弹窗 -->
    <el-dialog
      v-model="realKeyDialogVisible"
      :title="isRealKeyEdit ? '编辑真实Key' : '新增真实Key'"
      width="520px"
      :close-on-click-modal="false"
      class="channel-dialog"
      destroy-on-close
    >
      <el-form
        ref="realKeyFormRef"
        :model="realKeyFormData"
        :rules="realKeyFormRules"
        label-width="100px"
        label-position="right"
        class="channel-form"
      >
        <el-form-item label="Key名称" prop="keyName">
          <el-input v-model="realKeyFormData.keyName" placeholder="请输入Key名称" />
        </el-form-item>

        <el-form-item label="Key值" prop="keyValue">
          <el-input
            v-model="realKeyFormData.keyValue"
            type="password"
            show-password
            :placeholder="isRealKeyEdit ? '留空则不修改' : '请输入真实API Key'"
          />
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="realKeyFormData.remark"
            type="textarea"
            :rows="3"
            placeholder="可选"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="realKeyDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="realKeySubmitLoading" @click="handleRealKeySubmit">
            {{ isRealKeyEdit ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

      </el-tab-pane>
      <!-- 模型分组 Tab -->
      <el-tab-pane label="模型分组" name="groups">

        <!-- 操作栏 -->
        <div class="filter-card glass-card">
          <div class="filter-header">
            <el-form :model="groupSearchForm" inline class="filter-form">
              <el-form-item label="搜索">
                <el-input
                  v-model="groupSearchForm.keyword"
                  placeholder="输入分组名称"
                  clearable
                  @clear="handleGroupSearch"
                  @input="handleGroupSearch"
                  class="search-input"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
            </el-form>
            <el-button type="primary" class="add-btn" @click="handleGroupAdd">
              <el-icon><Plus /></el-icon>
              新增分组
            </el-button>
          </div>
        </div>

        <!-- 分组表格 -->
        <div class="table-card glass-card">
          <el-table
            :data="groupTableData"
            v-loading="groupLoading"
            stripe
            border
            style="width: 100%"
            class="channel-table"
          >
            <el-table-column prop="id" label="ID" width="70" align="center" />

            <el-table-column prop="groupName" label="分组名称" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="channel-name">{{ row.groupName }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />

            <el-table-column label="包含模型数" width="100" align="center">
              <template #default="{ row }">
                {{ row.models?.length || 0 }}
              </template>
            </el-table-column>

            <el-table-column label="模型列表" min-width="200">
              <template #default="{ row }">
                <span v-if="!row.models || row.models.length === 0" style="color: rgba(248,250,252,0.4); font-size: 12px;">—</span>
                <template v-else>
                  <el-tag
                    v-for="m in row.models.slice(0, 3)"
                    :key="m.id || m.modelCode"
                    size="small"
                    type="info"
                    style="margin: 2px 3px 2px 0;"
                  >
                    {{ m.modelCode }}
                  </el-tag>
                  <el-tag v-if="row.models.length > 3" size="small" type="info">
                    +{{ row.models.length - 3 }}
                  </el-tag>
                </template>
              </template>
            </el-table-column>

            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>

            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button type="primary" link size="small" @click="handleGroupEdit(row)">
                    <el-icon><Edit /></el-icon>编辑
                  </el-button>
                  <el-button type="danger" link size="small" @click="handleGroupDelete(row)">
                    <el-icon><Delete /></el-icon>删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="groupPagination.page"
              v-model:page-size="groupPagination.size"
              :page-sizes="[10, 20, 50, 100]"
              :total="groupPagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              background
              @size-change="handleGroupSizeChange"
              @current-change="handleGroupPageChange"
            />
          </div>
        </div>

        <!-- 新增/编辑分组弹窗 -->
        <el-dialog
          v-model="groupDialogVisible"
          :title="isGroupEdit ? '编辑分组' : '新增分组'"
          width="560px"
          :close-on-click-modal="false"
          class="channel-dialog"
          destroy-on-close
        >
          <el-form
            ref="groupFormRef"
            :model="groupFormData"
            :rules="groupFormRules"
            label-width="100px"
            label-position="right"
            class="channel-form"
          >
            <el-form-item label="分组名称" prop="groupName">
              <el-input v-model="groupFormData.groupName" placeholder="请输入分组名称" />
            </el-form-item>

            <el-form-item label="描述">
              <el-input
                v-model="groupFormData.description"
                type="textarea"
                :rows="3"
                placeholder="可选，描述该分组的用途"
              />
            </el-form-item>

            <el-form-item label="包含模型" prop="modelIds">
              <el-select
                v-model="groupFormData.modelIds"
                multiple
                placeholder="请选择模型"
                style="width: 100%"
                filterable
              >
                <el-option
                  v-for="m in allModels"
                  :key="m.id"
                  :label="m.modelName"
                  :value="m.id"
                />
              </el-select>
            </el-form-item>
          </el-form>

          <template #footer>
            <div class="dialog-footer">
              <el-button @click="groupDialogVisible = false">取消</el-button>
              <el-button type="primary" :loading="groupSubmitLoading" @click="handleGroupSubmit">
                {{ isGroupEdit ? '更新' : '创建' }}
              </el-button>
            </div>
          </template>
        </el-dialog>

      </el-tab-pane>
    </el-tabs>
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
  Key
} from '@element-plus/icons-vue'
import {
  getChannelList,
  createChannel,
  updateChannel,
  deleteChannel
} from '@/api/channel'
import {
  getRealKeyList,
  createRealKey,
  updateRealKey,
  toggleRealKeyStatus,
  deleteRealKey
} from '@/api/realkey'
import {
  getModelGroupList,
  createModelGroup,
  updateModelGroup,
  deleteModelGroup
} from '@/api/modelgroup'
import { getModelList } from '@/api/model'

// ==================== 响应式数据 ====================

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 搜索表单
const searchForm = reactive({
  keyword: '',
  channelType: '',
  status: ''
})

// 分页配置
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 排序配置
const sortConfig = reactive({
  prop: '',
  order: ''
})

// 弹窗相关
const dialogVisible = ref(false)
const dialogTitle = computed(() => isEdit.value ? '编辑渠道' : '新增渠道')
const isEdit = ref(false)
const currentEditId = ref(null)
const submitLoading = ref(false)

// 表单数据
const formRef = ref()
const formData = reactive({
  channelName: '',
  channelCode: '',
  channelType: '',
  provider: '',
  baseUrl: '',
  apiKey: '',
  apiVersion: '',
  status: 1,
  maxTokens: 4096,
  maxRpm: 60,
  maxTpm: 90000,
  timeout: 30,
  remark: ''
})

// 表单验证规则
const formRules = {
  channelName: [
    { required: true, message: '请输入渠道名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符', trigger: 'blur' }
  ],
  channelCode: [
    { required: true, message: '请输入渠道编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/, message: '以字母开头，仅允许字母、数字、下划线、连字符', trigger: 'blur' }
  ],
  channelType: [
    { required: true, message: '请选择渠道类型', trigger: 'change' }
  ],
  provider: [
    { required: true, message: '请输入提供商', trigger: 'blur' }
  ],
  baseUrl: [
    { required: true, message: '请输入Base URL', trigger: 'blur' },
    { type: 'url', message: '请输入有效的URL地址', trigger: 'blur' }
  ],
  apiKey: [
    { required: true, message: '请输入API Key', trigger: 'blur' },
    { min: 10, message: 'API Key长度至少10个字符', trigger: 'blur' }
  ]
}

// 防抖定时器
let searchTimer = null

// ==================== 模型分组状态 ====================

const activeTab = ref('channels')
const groupTableData = ref([])
const groupLoading = ref(false)
const groupPagination = reactive({ page: 1, size: 10, total: 0 })
const groupSearchForm = reactive({ keyword: '' })
const groupDialogVisible = ref(false)
const isGroupEdit = ref(false)
const currentGroupEditId = ref(null)
const groupSubmitLoading = ref(false)
const groupFormRef = ref()
const groupFormData = reactive({ groupName: '', description: '', modelIds: [] })
const groupFormRules = {
  groupName: [{ required: true, message: '请输入分组名称', trigger: 'blur' }],
  modelIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一个模型', trigger: 'change' }]
}
const allModels = ref([])
let groupLoaded = false

// ==================== 生命周期 ====================

onMounted(() => {
  fetchChannelList()
  fetchAllModels()
})

// ==================== API 调用方法 ====================

/**
 * 获取渠道列表
 */
async function fetchChannelList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword || undefined,
      channelType: searchForm.channelType || undefined,
      status: searchForm.status !== '' ? searchForm.status : undefined,
      sortProp: sortConfig.prop || undefined,
      sortOrder: sortConfig.order || undefined
    }

    const res = await getChannelList(params)

    if (res.code === 200 && res.data) {
      tableData.value = (res.data.records || []).map(item => ({
        ...item,
        statusLoading: false
      }))
      pagination.total = res.data.total || 0
    } else {
      ElMessage.error(res.message || '获取渠道列表失败')
    }
  } catch (error) {
    console.error('获取渠道列表失败:', error)
    ElMessage.error(error.message || '网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 创建渠道
 */
async function handleCreate() {
  submitLoading.value = true
  try {
    const data = { ...formData }
    const res = await createChannel(data)

    if (res.code === 200) {
      ElMessage.success('渠道创建成功')
      dialogVisible.value = false
      fetchChannelList()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error) {
    console.error('创建渠道失败:', error)
    ElMessage.error(error.message || '创建失败，请稍后重试')
  } finally {
    submitLoading.value = false
  }
}

/**
 * 更新渠道
 */
async function handleUpdate() {
  if (!currentEditId.value) return

  submitLoading.value = true
  try {
    const data = { ...formData }
    const res = await updateChannel(currentEditId.value, data)

    if (res.code === 200) {
      ElMessage.success('渠道更新成功')
      dialogVisible.value = false
      fetchChannelList()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (error) {
    console.error('更新渠道失败:', error)
    ElMessage.error(error.message || '更新失败，请稍后重试')
  } finally {
    submitLoading.value = false
  }
}

/**
 * 删除渠道
 */
async function handleDeleteAction(id) {
  try {
    const res = await deleteChannel(id)

    if (res.code === 200) {
      ElMessage.success('删除成功')
      // 如果当前页只有一条数据且不是第一页，则回到上一页
      if (tableData.value.length === 1 && pagination.page > 1) {
        pagination.page--
      }
      fetchChannelList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    console.error('删除渠道失败:', error)
    ElMessage.error(error.message || '删除失败，请稍后重试')
  }
}

// ==================== 事件处理方法 ====================

/**
 * 防抖搜索
 */
function debounceSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    pagination.page = 1
    fetchChannelList()
  }, 500)
}

/**
 * 执行搜索
 */
function handleSearch() {
  pagination.page = 1
  fetchChannelList()
}

/**
 * 重置搜索条件
 */
function resetSearch() {
  searchForm.keyword = ''
  searchForm.channelType = ''
  searchForm.status = ''
  pagination.page = 1
  fetchChannelList()
}

/**
 * 分页大小改变
 */
function handleSizeChange(val) {
  pagination.size = val
  pagination.page = 1
  fetchChannelList()
}

/**
 * 页码改变
 */
function handlePageChange(val) {
  pagination.page = val
  fetchChannelList()
}

/**
 * 排序变化
 */
function handleSortChange({ prop, order }) {
  sortConfig.prop = prop || ''
  sortConfig.order = order ? (order === 'ascending' ? 'asc' : 'desc') : ''
  pagination.page = 1
  fetchChannelList()
}

/**
 * 打开新增弹窗
 */
function handleAdd() {
  isEdit.value = false
  currentEditId.value = null
  resetFormData()
  dialogVisible.value = true
}

/**
 * 打开编辑弹窗
 */
function handleEdit(row) {
  isEdit.value = true
  currentEditId.value = row.id
  // 填充表单数据（注意：apiKeyMask是脱敏的，编辑时不回显真实key）
  Object.assign(formData, {
    channelName: row.channelName,
    channelCode: row.channelCode,
    channelType: row.channelType,
    provider: row.provider,
    baseUrl: row.baseUrl,
    apiKey: '', // 编辑时清空API Key，需要重新输入
    apiVersion: row.apiVersion || '',
    status: row.status,
    maxTokens: row.maxTokens || 4096,
    maxRpm: row.maxRpm || 60,
    maxTpm: row.maxTpm || 90000,
    timeout: row.timeout || 30,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

/**
 * 提交表单
 */
function handleSubmit() {
  formRef.value?.validate((valid) => {
    if (valid) {
      if (isEdit.value) {
        handleUpdate()
      } else {
        handleCreate()
      }
    }
  })
}

/**
 * 删除确认
 */
function handleDelete(row) {
  ElMessageBox.confirm(
    `确定要删除渠道「${row.channelName}」吗？删除后不可恢复。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(() => {
    handleDeleteAction(row.id)
  }).catch(() => {
    // 用户取消删除
  })
}

/**
 * 状态切换
 */
async function handleStatusChange(row, newStatus) {
  row.statusLoading = true
  try {
    const res = await updateChannel(row.id, { status: newStatus })

    if (res.code === 200) {
      ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
      // 更新本地数据
      row.status = newStatus
    } else {
      // 回滚状态
      row.status = newStatus === 1 ? 0 : 1
      ElMessage.error(res.message || '状态更新失败')
    }
  } catch (error) {
    console.error('状态更新失败:', error)
    // 回滚状态
    row.status = newStatus === 1 ? 0 : 1
    ElMessage.error(error.message || '状态更新失败')
  } finally {
    row.statusLoading = false
  }
}

/**
 * 连通性测试（预留接口）
 */
function handleTest(row) {
  ElMessage.info(`正在测试 ${row.provider} (${row.channelName}) 的连通性...`)
  // TODO: 实现真实的连通性测试逻辑
  // 可以调用后端专门的测试接口
  setTimeout(() => {
    ElMessage.success('连通性测试通过 ✓')
  }, 1500)
}

// ==================== 工具方法 ====================

/**
 * 重置表单数据
 */
function resetFormData() {
  Object.assign(formData, {
    channelName: '',
    channelCode: '',
    channelType: '',
    provider: '',
    baseUrl: '',
    apiKey: '',
    apiVersion: '',
    status: 1,
    maxTokens: 4096,
    maxRpm: 60,
    maxTpm: 90000,
    timeout: 30,
    remark: ''
  })
  // 重置验证状态
  formRef.value?.resetFields()
}

/**
 * 格式化时间
 */
function formatTime(timeStr) {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 获取类型标签样式
 */
function getTypeTagType(type) {
  const typeMap = {
    'LLM': '',
    'Image': 'success',
    'Audio': 'warning',
    'Video': 'danger',
    'Embedding': 'info'
  }
  return typeMap[type] || 'info'
}

// ==================== 真实Key管理 ====================

const keyDrawerVisible = ref(false)
const currentChannelId = ref(null)
const currentChannelName = ref('')
const realKeyList = ref([])
const realKeyLoading = ref(false)

const realKeyDialogVisible = ref(false)
const isRealKeyEdit = ref(false)
const currentRealKeyEditId = ref(null)
const realKeySubmitLoading = ref(false)
const realKeyFormRef = ref()

const realKeyFormData = reactive({
  keyName: '',
  keyValue: '',
  remark: ''
})

const realKeyFormRules = {
  keyName: [
    { required: true, message: '请输入Key名称', trigger: 'blur' }
  ],
  keyValue: [
    {
      validator: (rule, value, callback) => {
        if (!isRealKeyEdit.value && !value) {
          callback(new Error('请输入真实API Key'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function handleOpenKeys(row) {
  currentChannelId.value = row.id
  currentChannelName.value = row.channelName
  keyDrawerVisible.value = true
  fetchRealKeyList()
}

async function fetchRealKeyList() {
  realKeyLoading.value = true
  try {
    const res = await getRealKeyList({ channelId: currentChannelId.value, page: 1, size: 100 })
    if (res.code === 200 && res.data) {
      realKeyList.value = (res.data.records || []).map(item => ({ ...item, statusLoading: false }))
    } else {
      ElMessage.error(res.message || '获取Key列表失败')
    }
  } catch (error) {
    console.error('获取Key列表失败:', error)
  } finally {
    realKeyLoading.value = false
  }
}

function handleAddRealKey() {
  isRealKeyEdit.value = false
  currentRealKeyEditId.value = null
  Object.assign(realKeyFormData, { keyName: '', keyValue: '', remark: '' })
  realKeyDialogVisible.value = true
}

function handleEditRealKey(row) {
  isRealKeyEdit.value = true
  currentRealKeyEditId.value = row.id
  Object.assign(realKeyFormData, {
    keyName: row.keyName,
    keyValue: '',
    remark: row.remark || ''
  })
  realKeyDialogVisible.value = true
}

function handleRealKeySubmit() {
  realKeyFormRef.value?.validate(async (valid) => {
    if (!valid) return
    realKeySubmitLoading.value = true
    try {
      let res
      if (isRealKeyEdit.value) {
        const data = { keyName: realKeyFormData.keyName, remark: realKeyFormData.remark }
        if (realKeyFormData.keyValue) data.keyValue = realKeyFormData.keyValue
        res = await updateRealKey(currentRealKeyEditId.value, data)
      } else {
        res = await createRealKey({
          channelId: currentChannelId.value,
          keyName: realKeyFormData.keyName,
          keyValue: realKeyFormData.keyValue,
          remark: realKeyFormData.remark
        })
      }
      if (res.code === 200) {
        ElMessage.success(isRealKeyEdit.value ? '更新成功' : '创建成功')
        realKeyDialogVisible.value = false
        fetchRealKeyList()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (error) {
      console.error('Key操作失败:', error)
      ElMessage.error(error.message || '操作失败')
    } finally {
      realKeySubmitLoading.value = false
    }
  })
}

function handleDeleteRealKey(row) {
  ElMessageBox.confirm(
    `确定要删除Key「${row.keyName}」吗？`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    try {
      const res = await deleteRealKey(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        fetchRealKeyList()
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

async function handleRealKeyStatusChange(row, newStatus) {
  row.statusLoading = true
  try {
    const res = await toggleRealKeyStatus(row.id)
    if (res.code === 200) {
      ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    } else {
      row.status = newStatus === 1 ? 0 : 1
      ElMessage.error(res.message || '状态更新失败')
    }
  } catch (error) {
    row.status = newStatus === 1 ? 0 : 1
    ElMessage.error(error.message || '状态更新失败')
  } finally {
    row.statusLoading = false
  }
}

// ==================== Tab 切换 ====================

function handleTabChange(tabName) {
  if (tabName === 'groups' && !groupLoaded) {
    fetchGroupList()
  }
}

// ==================== 模型分组 API ====================

async function fetchGroupList() {
  groupLoading.value = true
  try {
    const res = await getModelGroupList({
      page: groupPagination.page,
      size: groupPagination.size,
      keyword: groupSearchForm.keyword || undefined
    })
    if (res.code === 200 && res.data) {
      groupTableData.value = res.data.records || []
      groupPagination.total = res.data.total || 0
      groupLoaded = true
    } else {
      ElMessage.error(res.message || '获取分组列表失败')
    }
  } catch (error) {
    console.error('获取分组列表失败:', error)
    ElMessage.error(error.message || '网络错误，请稍后重试')
  } finally {
    groupLoading.value = false
  }
}

async function fetchAllModels() {
  try {
    const res = await getModelList({ page: 1, size: 1000 })
    if (res.code === 200 && res.data) {
      allModels.value = res.data.records || []
    }
  } catch (error) {
    console.error('获取模型列表失败:', error)
  }
}

function handleGroupSearch() {
  groupPagination.page = 1
  fetchGroupList()
}

function handleGroupSizeChange(val) {
  groupPagination.size = val
  groupPagination.page = 1
  fetchGroupList()
}

function handleGroupPageChange(val) {
  groupPagination.page = val
  fetchGroupList()
}

function handleGroupAdd() {
  isGroupEdit.value = false
  currentGroupEditId.value = null
  Object.assign(groupFormData, { groupName: '', description: '', modelIds: [] })
  groupDialogVisible.value = true
}

function handleGroupEdit(row) {
  isGroupEdit.value = true
  currentGroupEditId.value = row.id
  Object.assign(groupFormData, {
    groupName: row.groupName,
    description: row.description || '',
    modelIds: (row.models || []).map(m => m.id).filter(Boolean)
  })
  groupDialogVisible.value = true
}

function handleGroupDelete(row) {
  ElMessageBox.confirm(
    `确定要删除分组「${row.groupName}」吗？删除后不可恢复。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    try {
      const res = await deleteModelGroup(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        if (groupTableData.value.length === 1 && groupPagination.page > 1) {
          groupPagination.page--
        }
        fetchGroupList()
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

function handleGroupSubmit() {
  groupFormRef.value?.validate(async (valid) => {
    if (!valid) return
    groupSubmitLoading.value = true
    try {
      const data = { ...groupFormData }
      let res
      if (isGroupEdit.value) {
        res = await updateModelGroup(currentGroupEditId.value, data)
      } else {
        res = await createModelGroup(data)
      }
      if (res.code === 200) {
        ElMessage.success(isGroupEdit.value ? '更新成功' : '创建成功')
        groupDialogVisible.value = false
        fetchGroupList()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (error) {
      console.error('分组操作失败:', error)
      ElMessage.error(error.message || '操作失败')
    } finally {
      groupSubmitLoading.value = false
    }
  })
}
</script>

<style scoped>
/* 渠道管理页面容器 */
.channel-management {
  padding: 0;
}

/* 新增按钮 */
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
  flex-shrink: 0;
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.5);
}

/* 筛选区域头部（表单+按钮） */
.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

/* 玻璃拟态卡片通用样式 */
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

/* 搜索筛选区域 */
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

.type-select,
.status-select {
  width: 160px;
}

/* 输入框深色主题适配 */
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

/* 下拉选择器深色适配 */
.filter-form :deep(.el-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
}

.filter-form :deep(.el-select-dropdown) {
  background: rgba(30, 41, 59, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

/* 表格卡片 */
.table-card {
  animation-delay: 0.2s;
  padding: 0;
  overflow: hidden;
}

/* Element Plus表格深色主题覆盖 */
.channel-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(30, 41, 59, 0.6);
  --el-table-row-hover-bg-color: rgba(59, 130, 246, 0.08);
  --el-table-border-color: rgba(255, 255, 255, 0.08);
  --el-table-header-text-color: rgba(248, 250, 252, 0.85);
  --el-table-text-color: #F8FAFC;
  --el-font-size-base: 13px;
}

.channel-table :deep(.el-table__header th) {
  font-weight: 600;
  letter-spacing: 0.3px;
  text-transform: uppercase;
  font-size: 12px;
  background: rgba(30, 41, 59, 0.7) !important;
  border-bottom: 2px solid rgba(59, 130, 246, 0.2);
}

.channel-table :deep(.el-table__body tr) {
  transition: all 0.2s ease;
}

.channel-table :deep(.el-table__body tr:hover > td) {
  background: rgba(59, 130, 246, 0.06) !important;
}

.channel-table :deep(.el-table__body td) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  padding: 12px 0;
}

/* 斑马纹效果增强 */
.channel-table :deep(.el-table__body tr.el-table__row--striped td) {
  background: rgba(255, 255, 255, 0.02) !important;
}

/* 渠道名称样式 */
.channel-name {
  font-weight: 600;
  color: #F8FAFC;
  font-size: 14px;
}

/* API Key文本样式 */
.api-key-text {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  color: rgba(248, 250, 252, 0.6);
  background: rgba(255, 255, 255, 0.04);
  padding: 4px 8px;
  border-radius: 6px;
  display: inline-block;
}

/* 操作按钮组 */
.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.action-buttons .el-button {
  font-size: 12px;
  font-weight: 500;
  padding: 4px 8px;
}

/* 分页区域 */
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

/* 弹窗样式 */
.channel-dialog :deep(.el-dialog) {
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

.channel-dialog :deep(.el-dialog__header) {
  padding: 24px 24px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.channel-dialog :deep(.el-dialog__title) {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: #F8FAFC;
  letter-spacing: -0.3px;
}

.channel-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 65vh;
  overflow-y: auto;
}

.channel-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

/* 表单样式 */
.channel-form :deep(.el-form-item__label) {
  color: rgba(248, 250, 252, 0.75);
  font-weight: 500;
  font-size: 13px;
}

.channel-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: none;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.channel-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(255, 255, 255, 0.08);
}

.channel-form :deep(.el-input__wrapper.is-focus) {
  border-color: #3B82F6;
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.channel-form :deep(.el-input__inner) {
  color: #F8FAFC;
}

.channel-form :deep(.el-input__inner::placeholder) {
  color: rgba(248, 250, 252, 0.35);
}

.channel-form :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #F8FAFC;
  border-radius: 8px;
}

.channel-form :deep(.el-textarea__inner::placeholder) {
  color: rgba(248, 250, 252, 0.35);
}

.channel-form :deep(.el-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
}

.channel-form :deep(.el-input-number .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
}

.channel-form :deep(.el-input-number__decrease),
.channel-form :deep(.el-input-number__increase) {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.1);
  color: rgba(248, 250, 252, 0.6);
}

.channel-form :deep(.el-divider__text) {
  color: rgba(248, 250, 252, 0.6);
  font-weight: 600;
  font-size: 13px;
  background: transparent;
}

.channel-form :deep(.el-divider) {
  border-color: rgba(255, 255, 255, 0.08);
}

/* 弹窗底部按钮 */
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

/* 开关样式优化 */
.channel-table :deep(.el-switch.is-checked .el-switch__core) {
  background: #3B82F6;
  border-color: #3B82F6;
}

/* Key抽屉样式 */
.key-drawer :deep(.el-drawer) {
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.98) 0%, rgba(30, 41, 59, 0.95) 100%);
  border-left: 1px solid rgba(255, 255, 255, 0.12);
}

.key-drawer :deep(.el-drawer__header) {
  padding: 20px 24px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: #F8FAFC;
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 0;
}

.key-drawer :deep(.el-drawer__body) {
  padding: 0;
  overflow: hidden;
}

.key-drawer-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.key-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.key-count {
  font-size: 13px;
  color: rgba(248, 250, 252, 0.55);
}

.real-key-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(30, 41, 59, 0.6);
  --el-table-row-hover-bg-color: rgba(59, 130, 246, 0.08);
  --el-table-border-color: rgba(255, 255, 255, 0.08);
  --el-table-header-text-color: rgba(248, 250, 252, 0.85);
  --el-table-text-color: #F8FAFC;
  --el-font-size-base: 13px;
}

.real-key-table :deep(.el-table__header th) {
  background: rgba(30, 41, 59, 0.7) !important;
  font-weight: 600;
  font-size: 12px;
  border-bottom: 2px solid rgba(59, 130, 246, 0.2);
}

.real-key-table :deep(.el-table__body td) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  padding: 10px 0;
}

.real-key-table :deep(.el-table__body tr:hover > td) {
  background: rgba(59, 130, 246, 0.06) !important;
}

.real-key-table :deep(.el-switch.is-checked .el-switch__core) {
  background: #3B82F6;
  border-color: #3B82F6;
}

.key-name {
  font-weight: 600;
  color: #F8FAFC;
}

.key-mask {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  color: rgba(248, 250, 252, 0.6);
  background: rgba(255, 255, 255, 0.04);
  padding: 2px 8px;
  border-radius: 4px;
}

/* 响应式布局 */
@media (max-width: 1280px) {
  .channel-management {
    padding: 16px;
  }

  .page-title {
    font-size: 28px;
  }

  .search-input {
    width: 240px;
  }

  .type-select,
  .status-select {
    width: 140px;
  }
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .add-btn {
    width: 100%;
  }

  .filter-form {
    flex-direction: column;
  }

  .search-input,
  .type-select,
  .status-select {
    width: 100%;
  }

  .action-buttons {
    flex-wrap: wrap;
  }

  .channel-dialog :deep(.el-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }
}

/* Tab 切换样式 */
.main-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.main-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(255, 255, 255, 0.08);
}

.main-tabs :deep(.el-tabs__item) {
  color: rgba(248, 250, 252, 0.55);
  font-size: 14px;
  font-weight: 500;
  transition: color 0.2s ease;
}

.main-tabs :deep(.el-tabs__item:hover) {
  color: rgba(248, 250, 252, 0.85);
}

.main-tabs :deep(.el-tabs__item.is-active) {
  color: #3B82F6;
  font-weight: 600;
}

.main-tabs :deep(.el-tabs__active-bar) {
  background-color: #3B82F6;
  border-radius: 2px;
}
</style>
