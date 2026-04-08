<template>
  <div class="team-management">
    <!-- 搜索筛选区域 -->
    <div class="filter-card glass-card">
      <div class="filter-header">
        <el-form :model="searchForm" inline class="filter-form">
          <el-form-item label="搜索">
            <el-input
              v-model="searchForm.keyword"
              placeholder="输入团队名称或编码"
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

          <el-form-item>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" class="add-btn" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增团队
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
        class="team-table"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column prop="teamName" label="团队名称" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="team-name">{{ row.teamName }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="teamCode" label="团队编码" width="130" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.teamCode }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="额度使用" width="200" align="center">
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

        <el-table-column prop="quotaWeight" label="倍率" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="warning">×{{ row.quotaWeight }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="memberCount" label="成员数" width="80" align="center" />

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

        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="success" link size="small" @click="handleIssueKey(row)">
                <el-icon><Key /></el-icon>发放Key
              </el-button>
              <el-button type="primary" link size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>编辑
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
          :page-sizes="[10, 20, 50]"
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
      :title="isEdit ? '编辑团队' : '新增团队'"
      width="560px"
      :close-on-click-modal="false"
      class="team-dialog"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="110px"
        label-position="right"
        class="team-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="团队名称" prop="teamName">
              <el-input v-model="formData.teamName" placeholder="请输入团队名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="团队编码" prop="teamCode">
              <el-input
                v-model="formData.teamCode"
                placeholder="请输入团队编码"
                :disabled="isEdit"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="额度上限" prop="quotaLimit">
              <el-input-number
                v-model="formData.quotaLimit"
                :min="0"
                :precision="2"
                style="width: 100%"
                placeholder="0表示无限制"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="额度倍率" prop="quotaWeight">
              <el-input-number
                v-model="formData.quotaWeight"
                :min="0.01"
                :max="10"
                :precision="2"
                :step="0.1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="可选，添加团队描述"
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

    <!-- 为组员发放Key弹窗 -->
    <el-dialog
      v-model="issueKeyDialogVisible"
      :title="`为「${issueKeyTeam?.teamName}」组员发放 Key`"
      width="600px"
      :close-on-click-modal="false"
      class="team-dialog"
      destroy-on-close
    >
      <el-form
        ref="issueKeyFormRef"
        :model="issueKeyFormData"
        :rules="issueKeyFormRules"
        label-width="110px"
        label-position="right"
        class="team-form"
      >
        <el-form-item label="Key名称" prop="keyName">
          <el-input v-model="issueKeyFormData.keyName" placeholder="如：张三的开发Key" />
        </el-form-item>

        <el-form-item label="分配给用户" prop="userId">
          <el-input
            v-model.number="issueKeyFormData.userId"
            type="number"
            placeholder="请输入用户ID"
          />
          <div style="font-size:12px;color:rgba(248,250,252,0.5);margin-top:4px">
            在用户管理中查看用户ID（后续会改为下拉选择）
          </div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="额度类型" prop="quotaType">
              <el-select v-model="issueKeyFormData.quotaType" style="width:100%">
                <el-option label="Token数量" value="token" />
                <el-option label="调用次数" value="count" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="额度上限" prop="quotaLimit">
              <el-input-number
                v-model="issueKeyFormData.quotaLimit"
                :min="1"
                :precision="0"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="模型分组">
          <el-select
            v-model="issueKeyFormData.allowedGroupIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择允许的模型分组（不选则不限）"
            style="width:100%"
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
            v-model="issueKeyFormData.remark"
            type="textarea"
            :rows="2"
            placeholder="可选，如：用于项目A的开发调试"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="issueKeyDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="issueKeyLoading" @click="handleIssueKeySubmit">
            生成并下发
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Delete, Key } from '@element-plus/icons-vue'
import { getTeamList, createTeam, updateTeam, deleteTeam } from '@/api/team'
import { createVirtualKey } from '@/api/virtualkey'
import { getModelGroupAll } from '@/api/modelgroup'

function getCurrentUserId() {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return userInfo.userId || userInfo.id || 1
  } catch {
    return 1
  }
}

const tableData = ref([])
const loading = ref(false)

const searchForm = reactive({ keyword: '' })

const pagination = reactive({ page: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const currentEditId = ref(null)
const submitLoading = ref(false)
const formRef = ref()

const formData = reactive({
  teamName: '',
  teamCode: '',
  description: '',
  quotaLimit: 0,
  quotaWeight: 1.0
})

const formRules = {
  teamName: [{ required: true, message: '请输入团队名称', trigger: 'blur' }],
  teamCode: [
    { required: true, message: '请输入团队编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/, message: '以字母开头，仅允许字母、数字、下划线、连字符', trigger: 'blur' }
  ]
}

let searchTimer = null

onMounted(() => {
  fetchList()
  fetchModelGroups()
})

// 模型分组
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

// 发放Key
const issueKeyDialogVisible = ref(false)
const issueKeyTeam = ref(null)
const issueKeyLoading = ref(false)
const issueKeyFormRef = ref()
const issueKeyFormData = reactive({
  keyName: '',
  userId: null,
  quotaType: 'token',
  quotaLimit: 100000,
  allowedGroupIds: [],
  remark: ''
})
const issueKeyFormRules = {
  keyName: [{ required: true, message: '请输入Key名称', trigger: 'blur' }],
  userId: [{ required: true, message: '请输入用户ID', trigger: 'blur' }],
  quotaType: [{ required: true, message: '请选择额度类型', trigger: 'change' }],
  quotaLimit: [{ required: true, message: '请输入额度上限', trigger: 'blur' }]
}

function handleIssueKey(row) {
  issueKeyTeam.value = row
  Object.assign(issueKeyFormData, {
    keyName: '',
    userId: null,
    quotaType: 'token',
    quotaLimit: 100000,
    allowedGroupIds: [],
    remark: ''
  })
  issueKeyDialogVisible.value = true
}

function handleIssueKeySubmit() {
  issueKeyFormRef.value?.validate(async (valid) => {
    if (!valid) return
    issueKeyLoading.value = true
    try {
      const res = await createVirtualKey({
        keyName: issueKeyFormData.keyName,
        userId: issueKeyFormData.userId,
        teamId: issueKeyTeam.value.id,
        quotaType: issueKeyFormData.quotaType,
        quotaLimit: issueKeyFormData.quotaLimit,
        allowedGroupIds: issueKeyFormData.allowedGroupIds,
        remark: issueKeyFormData.remark,
        rateLimitQpm: 60,
        rateLimitQpd: 0
      })
      if (res.code === 200) {
        ElMessage.success(`Key已生成：${res.data?.keyValue}`)
        issueKeyDialogVisible.value = false
      } else {
        ElMessage.error(res.message || '生成失败')
      }
    } catch (e) {
      ElMessage.error(e.message || '生成失败')
    } finally {
      issueKeyLoading.value = false
    }
  })
}

async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword || undefined
    }
    const res = await getTeamList(params)
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      pagination.total = res.data.total || 0
    } else {
      ElMessage.error(res.message || '获取团队列表失败')
    }
  } catch (error) {
    console.error('获取团队列表失败:', error)
  } finally {
    loading.value = false
  }
}

function debounceSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    pagination.page = 1
    fetchList()
  }, 500)
}

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function resetSearch() {
  searchForm.keyword = ''
  pagination.page = 1
  fetchList()
}

function handleSizeChange(val) {
  pagination.size = val
  pagination.page = 1
  fetchList()
}

function handlePageChange(val) {
  pagination.page = val
  fetchList()
}

function handleAdd() {
  isEdit.value = false
  currentEditId.value = null
  Object.assign(formData, { teamName: '', teamCode: '', description: '', quotaLimit: 0, quotaWeight: 1.0 })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  currentEditId.value = row.id
  Object.assign(formData, {
    teamName: row.teamName,
    teamCode: row.teamCode,
    description: row.description || '',
    quotaLimit: Number(row.quotaLimit) || 0,
    quotaWeight: Number(row.quotaWeight) || 1.0
  })
  dialogVisible.value = true
}

function handleSubmit() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      let res
      if (isEdit.value) {
        res = await updateTeam(currentEditId.value, {
          teamName: formData.teamName,
          description: formData.description,
          quotaLimit: formData.quotaLimit,
          quotaWeight: formData.quotaWeight
        })
      } else {
        res = await createTeam({
          ...formData,
          ownerId: getCurrentUserId()
        })
      }
      if (res.code === 200) {
        ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
        dialogVisible.value = false
        fetchList()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(
    `确定要删除团队「${row.teamName}」吗？`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    try {
      const res = await deleteTeam(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        if (tableData.value.length === 1 && pagination.page > 1) pagination.page--
        fetchList()
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

function formatTime(timeStr) {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
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
</script>

<style scoped>
.team-management { padding: 0; }

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

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.glass-card {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.8) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  transition: all 0.3s ease;
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.05) inset, 0 8px 32px rgba(0, 0, 0, 0.2);
  animation: fadeInUp 0.5s ease-out;
  animation-fill-mode: backwards;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.filter-card { animation-delay: 0.1s; }
.table-card { animation-delay: 0.2s; padding: 0; overflow: hidden; }

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.filter-form :deep(.el-form-item) { margin-bottom: 0; margin-right: 0; }
.filter-form :deep(.el-form-item__label) { color: rgba(248, 250, 252, 0.7); font-weight: 500; font-size: 13px; }
.search-input { width: 280px; }

.filter-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: none;
  border-radius: 8px;
  transition: all 0.3s ease;
}
.filter-form :deep(.el-input__inner) { color: #F8FAFC; }
.filter-form :deep(.el-input__inner::placeholder) { color: rgba(248, 250, 252, 0.35); }

.team-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(30, 41, 59, 0.6);
  --el-table-row-hover-bg-color: rgba(59, 130, 246, 0.08);
  --el-table-border-color: rgba(255, 255, 255, 0.08);
  --el-table-header-text-color: rgba(248, 250, 252, 0.85);
  --el-table-text-color: #F8FAFC;
  --el-font-size-base: 13px;
}

.team-table :deep(.el-table__header th) {
  font-weight: 600;
  font-size: 12px;
  background: rgba(30, 41, 59, 0.7) !important;
  border-bottom: 2px solid rgba(59, 130, 246, 0.2);
}

.team-table :deep(.el-table__body td) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  padding: 12px 0;
}

.team-table :deep(.el-table__body tr:hover > td) {
  background: rgba(59, 130, 246, 0.06) !important;
}

.team-table :deep(.el-table__body tr.el-table__row--striped td) {
  background: rgba(255, 255, 255, 0.02) !important;
}

.team-name { font-weight: 600; color: #F8FAFC; font-size: 14px; }

.quota-cell { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.quota-text { font-size: 11px; color: rgba(248, 250, 252, 0.55); }

.action-buttons { display: flex; justify-content: center; align-items: center; gap: 4px; }
.action-buttons .el-button { font-size: 12px; font-weight: 500; padding: 4px 6px; }

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

.pagination-wrapper :deep(.el-pager li.is-active) {
  background: #3B82F6;
  border-color: #3B82F6;
  color: white;
  font-weight: 600;
}

.team-dialog :deep(.el-dialog) {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.95) 0%, rgba(15, 23, 42, 0.98) 100%);
  backdrop-filter: blur(30px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.1) inset, 0 24px 64px rgba(0, 0, 0, 0.5);
}

.team-dialog :deep(.el-dialog__header) {
  padding: 24px 24px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.team-dialog :deep(.el-dialog__title) { font-size: 18px; font-weight: 700; color: #F8FAFC; }
.team-dialog :deep(.el-dialog__body) { padding: 24px; max-height: 65vh; overflow-y: auto; }
.team-dialog :deep(.el-dialog__footer) { padding: 16px 24px 24px; border-top: 1px solid rgba(255, 255, 255, 0.08); }

.team-form :deep(.el-form-item__label) { color: rgba(248, 250, 252, 0.75); font-weight: 500; font-size: 13px; }

.team-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: none;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.team-form :deep(.el-input__wrapper.is-focus) {
  border-color: #3B82F6;
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.team-form :deep(.el-input__inner) { color: #F8FAFC; }
.team-form :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #F8FAFC;
  border-radius: 8px;
}
.team-form :deep(.el-input-number .el-input__wrapper) { background: rgba(255, 255, 255, 0.06); }

.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; }
.dialog-footer .el-button { padding: 10px 24px; border-radius: 8px; font-weight: 500; font-size: 14px; }
</style>
