<template>
  <div class="model-market">
    <!-- 搜索筛选区域 -->
    <div class="filter-card glass-card">
      <div class="filter-header">
        <el-form :model="searchForm" inline class="filter-form">
          <el-form-item label="搜索">
            <el-input
              v-model="searchForm.keyword"
              placeholder="输入模型名称或编码"
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
              v-model="searchForm.modelType"
              placeholder="全部类型"
              clearable
              @change="handleSearch"
              class="type-select"
            >
              <el-option label="对话" value="chat" />
              <el-option label="Embedding" value="embedding" />
              <el-option label="图像" value="image" />
              <el-option label="语音" value="audio" />
            </el-select>
          </el-form-item>

          <el-form-item label="渠道">
            <el-select
              v-model="searchForm.channelId"
              placeholder="全部渠道"
              clearable
              @change="handleSearch"
              class="channel-select"
            >
              <el-option
                v-for="c in channelOptions"
                :key="c.id"
                :label="c.channelName"
                :value="c.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" class="add-btn" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增模型
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
        class="model-table"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column prop="modelName" label="模型名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="model-name-cell">
              <span class="model-name">{{ row.modelName }}</span>
              <el-tag v-if="row.modelAlias" size="small" type="info" class="alias-tag">{{ row.modelAlias }}</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="modelCode" label="模型编码" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" class="model-code-tag">{{ row.modelCode }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="modelType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.modelType)" size="small">{{ getTypeLabel(row.modelType) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="channelName" label="所属渠道" min-width="130" show-overflow-tooltip />

        <el-table-column prop="maxTokens" label="Max Tokens" width="120" align="center">
          <template #default="{ row }">
            <span class="token-count">{{ row.maxTokens?.toLocaleString() }}</span>
          </template>
        </el-table-column>

        <el-table-column label="价格(¥/M tokens)" width="160" align="center">
          <template #default="{ row }">
            <div class="price-cell">
              <span class="price-in">入: {{ formatPrice(row.inputPrice) }}</span>
              <span class="price-out">出: {{ formatPrice(row.outputPrice) }}</span>
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
              @change="(val) => handleStatusChange(row, val)"
              :loading="row.statusLoading"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
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
          class="pagination"
        />
      </div>
    </div>

    <!-- 新增模型对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增模型"
      width="560px"
      class="model-dialog"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        class="model-form"
      >
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="formData.modelName" placeholder="如 GPT-4 Turbo" />
        </el-form-item>
        <el-form-item label="模型编码" prop="modelCode">
          <el-input v-model="formData.modelCode" placeholder="如 gpt-4-turbo" />
        </el-form-item>
        <el-form-item label="模型别名">
          <el-input v-model="formData.modelAlias" placeholder="可选" />
        </el-form-item>
        <el-form-item label="所属渠道" prop="channelId">
          <el-select v-model="formData.channelId" placeholder="选择渠道" style="width:100%">
            <el-option
              v-for="c in channelOptions"
              :key="c.id"
              :label="c.channelName"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="模型类型" prop="modelType">
          <el-select v-model="formData.modelType" placeholder="选择类型" style="width:100%">
            <el-option label="对话" value="chat" />
            <el-option label="Embedding" value="embedding" />
            <el-option label="图像" value="image" />
            <el-option label="语音" value="audio" />
          </el-select>
        </el-form-item>
        <el-form-item label="Max Tokens">
          <el-input-number v-model="formData.maxTokens" :min="1" :max="2000000" style="width:100%" />
        </el-form-item>
        <el-form-item label="输入价格">
          <el-input-number v-model="formData.inputPrice" :min="0" :precision="6" :step="0.001" style="width:100%" />
          <span class="price-unit">¥/M tokens</span>
        </el-form-item>
        <el-form-item label="输出价格">
          <el-input-number v-model="formData.outputPrice" :min="0" :precision="6" :step="0.001" style="width:100%" />
          <span class="price-unit">¥/M tokens</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModelList, createModel, toggleModelStatus, deleteModel } from '@/api/model'
import { getChannelList } from '@/api/channel'

const loading = ref(false)
const tableData = ref([])
const channelOptions = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const searchForm = reactive({ keyword: '', modelType: '', channelId: null })
const pagination = reactive({ page: 1, size: 20, total: 0 })

const formData = reactive({
  modelName: '',
  modelCode: '',
  modelAlias: '',
  channelId: null,
  modelType: '',
  maxTokens: 4096,
  inputPrice: 0,
  outputPrice: 0
})

const formRules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  modelCode: [{ required: true, message: '请输入模型编码', trigger: 'blur' }],
  channelId: [{ required: true, message: '请选择渠道', trigger: 'change' }],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }]
}

let debounceTimer = null
function debounceSearch() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(handleSearch, 400)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getModelList({
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword || undefined,
      modelType: searchForm.modelType || undefined,
      channelId: searchForm.channelId || undefined
    })
    tableData.value = (res.data?.records || []).map(r => ({ ...r, statusLoading: false }))
    pagination.total = res.data?.total || 0
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function fetchChannels() {
  try {
    const res = await getChannelList({ page: 1, size: 100 })
    channelOptions.value = res.data?.records || []
  } catch (e) {}
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function resetSearch() {
  searchForm.keyword = ''
  searchForm.modelType = ''
  searchForm.channelId = null
  handleSearch()
}

function handlePageChange(page) {
  pagination.page = page
  fetchData()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  fetchData()
}

async function handleStatusChange(row, val) {
  row.statusLoading = true
  try {
    await toggleModelStatus(row.id)
    ElMessage.success(val === 1 ? '已启用' : '已下线')
  } catch (e) {
    row.status = val === 1 ? 0 : 1
  } finally {
    row.statusLoading = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除模型「${row.modelName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteModel(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
}

function handleAdd() {
  Object.assign(formData, {
    modelName: '', modelCode: '', modelAlias: '',
    channelId: null, modelType: '', maxTokens: 4096,
    inputPrice: 0, outputPrice: 0
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createModel({ ...formData })
    ElMessage.success('创建成功')
    dialogVisible.value = false
    fetchData()
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

function getTypeTagType(type) {
  const map = { chat: 'primary', embedding: 'success', image: 'warning', audio: 'info' }
  return map[type] || 'info'
}

function getTypeLabel(type) {
  const map = { chat: '对话', embedding: 'Embedding', image: '图像', audio: '语音' }
  return map[type] || type
}

function formatPrice(price) {
  if (!price && price !== 0) return '-'
  return Number(price) === 0 ? '免费' : `¥${Number(price).toFixed(4)}`
}

onMounted(() => {
  fetchData()
  fetchChannels()
})
</script>

<style scoped>
.model-market {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.glass-card {
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 20px 24px;
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  margin: 0;
}

.search-input { width: 220px; }
.type-select, .channel-select { width: 140px; }

.add-btn {
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
  border: none;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

.model-table :deep(.el-table__row) {
  background: transparent;
}

.model-name-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.model-name {
  font-weight: 500;
  color: #F8FAFC;
}

.alias-tag {
  font-size: 11px;
  opacity: 0.7;
}

.model-code-tag {
  font-family: monospace;
  font-size: 12px;
}

.token-count {
  font-family: monospace;
  font-size: 13px;
  color: #94A3B8;
}

.price-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 12px;
}

.price-in { color: #60A5FA; }
.price-out { color: #34D399; }

.price-unit {
  margin-left: 8px;
  font-size: 12px;
  color: #94A3B8;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.model-form :deep(.el-form-item__label) {
  color: rgba(248, 250, 252, 0.8);
}
</style>
