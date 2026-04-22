<template>
  <div class="alert-management">
    <el-tabs v-model="activeTab" class="main-tabs">

      <!-- 告警规则 Tab -->
      <el-tab-pane label="告警规则" name="rules">
        <div class="filter-card glass-card">
          <div class="filter-header">
            <span class="filter-title">告警规则列表</span>
            <el-button type="primary" @click="openCreateDialog">
              <el-icon><Plus /></el-icon> 新建规则
            </el-button>
          </div>
        </div>

        <div class="table-card glass-card">
          <el-table :data="rules" v-loading="rulesLoading" stripe>
            <el-table-column prop="ruleName" label="规则名称" min-width="150" />
            <el-table-column prop="ruleType" label="类型" width="120" />
            <el-table-column prop="targetType" label="目标类型" width="100" />
            <el-table-column prop="targetId" label="目标ID" width="90" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.isEnabled === 1 ? 'success' : 'info'" size="small">
                  {{ row.isEnabled === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="triggerCount" label="触发次数" width="90" />
            <el-table-column label="最后触发" width="170">
              <template #default="{ row }">{{ row.lastTriggeredTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
                <el-button size="small" :type="row.isEnabled === 1 ? 'warning' : 'success'"
                  @click="handleToggle(row)">
                  {{ row.isEnabled === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="rulesPage"
              v-model:page-size="rulesSize"
              :total="rulesTotal"
              layout="total, prev, pager, next"
              @current-change="fetchRules"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 告警历史 Tab -->
      <el-tab-pane label="告警历史" name="histories">
        <div class="table-card glass-card">
          <el-table :data="histories" v-loading="historiesLoading" stripe>
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ row.createdAt }}</template>
            </el-table-column>
            <el-table-column prop="alertLevel" label="级别" width="80">
              <template #default="{ row }">
                <el-tag
                  :type="row.alertLevel === 'critical' ? 'danger' : row.alertLevel === 'warning' ? 'warning' : 'info'"
                  size="small">
                  {{ row.alertLevel }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="alertType" label="类型" width="120" />
            <el-table-column prop="alertTitle" label="标题" min-width="200" />
            <el-table-column prop="alertContent" label="内容" min-width="250" show-overflow-tooltip />
            <el-table-column label="通知状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.notificationStatus === 'sent' ? 'success' : row.notificationStatus === 'failed' ? 'danger' : 'info'" size="small">
                  {{ row.notificationStatus }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="historiesPage"
              v-model:page-size="historiesSize"
              :total="historiesTotal"
              layout="total, prev, pager, next"
              @current-change="fetchHistories"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑告警规则' : '新建告警规则'"
      width="600px"
      destroy-on-close>
      <el-form :model="form" label-width="110px" :rules="formRules" ref="formRef">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="输入规则名称" />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="form.ruleType" placeholder="选择规则类型" style="width:100%">
            <el-option label="额度不足告警" value="quota_low" />
            <el-option label="错误率告警" value="error_rate" />
            <el-option label="调用量异常" value="call_volume" />
            <el-option label="渠道不可用" value="channel_down" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标类型">
          <el-select v-model="form.targetType" placeholder="全局/项目/用户" clearable style="width:100%">
            <el-option label="全局" value="global" />
            <el-option label="项目" value="project" />
            <el-option label="用户" value="user" />
            <el-option label="渠道" value="channel" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标ID">
          <el-input-number v-model="form.targetId" :min="1" placeholder="留空表示全局" style="width:100%" controls-position="right" />
        </el-form-item>
        <el-form-item label="触发条件(JSON)">
          <el-input v-model="form.conditionConfig" type="textarea" :rows="3"
            placeholder='如: {"threshold": 100, "unit": "yuan"}' />
        </el-form-item>
        <el-form-item label="动作配置(JSON)">
          <el-input v-model="form.actionConfig" type="textarea" :rows="3"
            placeholder='如: {"action": "notify"}' />
        </el-form-item>
        <el-form-item label="通知渠道">
          <el-input v-model="form.notificationChannels" placeholder="如: email,webhook" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.isEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAlertRules, createAlertRule, updateAlertRule,
  toggleAlertRule, deleteAlertRule, getAlertHistories
} from '@/api/alert'

const activeTab = ref('rules')

// --- 规则列表 ---
const rules = ref([])
const rulesLoading = ref(false)
const rulesPage = ref(1)
const rulesSize = ref(20)
const rulesTotal = ref(0)

async function fetchRules() {
  rulesLoading.value = true
  try {
    const res = await getAlertRules({ page: rulesPage.value, size: rulesSize.value })
    rules.value = res.records || []
    rulesTotal.value = res.total || 0
  } finally {
    rulesLoading.value = false
  }
}

// --- 历史列表 ---
const histories = ref([])
const historiesLoading = ref(false)
const historiesPage = ref(1)
const historiesSize = ref(20)
const historiesTotal = ref(0)

async function fetchHistories() {
  historiesLoading.value = true
  try {
    const res = await getAlertHistories({ page: historiesPage.value, size: historiesSize.value })
    histories.value = res.records || []
    historiesTotal.value = res.total || 0
  } finally {
    historiesLoading.value = false
  }
}

// --- 对话框 ---
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)
const formRef = ref(null)
const form = ref({
  ruleName: '',
  ruleType: '',
  targetType: '',
  targetId: null,
  conditionConfig: '',
  actionConfig: '',
  notificationChannels: '',
  isEnabled: 1
})
const formRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }]
}

function resetForm() {
  form.value = {
    ruleName: '', ruleType: '', targetType: '', targetId: null,
    conditionConfig: '', actionConfig: '', notificationChannels: '', isEnabled: 1
  }
}

function openCreateDialog() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.value = {
    ruleName: row.ruleName,
    ruleType: row.ruleType,
    targetType: row.targetType || '',
    targetId: row.targetId || null,
    conditionConfig: row.conditionConfig || '',
    actionConfig: row.actionConfig || '',
    notificationChannels: row.notificationChannels || '',
    isEnabled: row.isEnabled
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...form.value }
    if (isEdit.value) {
      await updateAlertRule(editId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createAlertRule(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchRules()
  } finally {
    submitting.value = false
  }
}

async function handleToggle(row) {
  await toggleAlertRule(row.id)
  ElMessage.success(row.isEnabled === 1 ? '已禁用' : '已启用')
  fetchRules()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除规则「${row.ruleName}」？`, '确认', { type: 'warning' })
  await deleteAlertRule(row.id)
  ElMessage.success('已删除')
  fetchRules()
}

onMounted(() => {
  fetchRules()
  fetchHistories()
})
</script>

<style scoped>
.alert-management {
  padding: 20px;
}
.filter-card {
  padding: 16px 20px;
  margin-bottom: 16px;
  border-radius: 12px;
}
.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.filter-title {
  font-size: 16px;
  font-weight: 600;
}
.table-card {
  padding: 16px;
  border-radius: 12px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
