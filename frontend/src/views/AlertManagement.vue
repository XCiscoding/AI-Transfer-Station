<template>
  <div class="alert-management">
    <el-tabs v-model="activeTab" class="main-tabs">

      <!-- 告警规则 Tab -->
      <el-tab-pane v-if="isSuperAdmin" label="告警规则" name="rules">
        <div class="filter-card glass-card">
          <div class="filter-header">
            <span class="filter-title">告警规则列表</span>
            <div style="display:flex;gap:8px">
              <el-button type="success" :loading="checking" @click="handleCheckNow">立即触发检测</el-button>
              <el-button type="primary" @click="openCreateDialog">
                <el-icon><Plus /></el-icon> 新建规则
              </el-button>
            </div>
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
      <el-tab-pane :label="isSuperAdmin ? '告警历史' : '告警通知'" name="histories">
        <div v-if="!isSuperAdmin" class="filter-card glass-card notice-scope">
          当前账号只接收自己所属团队相关的告警通知，不能设置告警规则。
        </div>
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
      v-if="isSuperAdmin"
      v-model="dialogVisible"
      :title="isEdit ? '编辑告警规则' : '新建告警规则'"
      width="600px"
      destroy-on-close>
      <el-form :model="form" label-width="110px" :rules="formRules" ref="formRef">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="输入规则名称" />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="form.ruleType" placeholder="选择规则类型" style="width:100%" @change="handleRuleTypeChange">
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
        <el-form-item label="触发条件(JSON)" prop="conditionConfig">
          <el-input v-model="form.conditionConfig" type="textarea" :rows="3"
            placeholder='选择规则类型后自动预填，或手动输入合法 JSON' />
          <div style="font-size:12px;color:#999;margin-top:4px">
            调用量异常测试用: <code>{"maxCount": -1}</code>&nbsp;（-1 = 无条件触发）
          </div>
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
import { ref, onMounted, computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAlertRules, createAlertRule, updateAlertRule,
  toggleAlertRule, deleteAlertRule, getAlertHistories,
  triggerCheckNow
} from '@/api/alert'
import { getUserInfo } from '@/api/auth'

function readStoredUserInfo() {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || 'null')
  } catch {
    return null
  }
}

const currentUser = ref(readStoredUserInfo())
const isSuperAdmin = computed(() => Boolean(currentUser.value?.isSuperAdmin || currentUser.value?.roles?.includes('SUPER_ADMIN')))
const activeTab = ref(isSuperAdmin.value ? 'rules' : 'histories')

async function fetchCurrentUser() {
  try {
    const res = await getUserInfo()
    const user = res.code === 200 ? res.data : null
    currentUser.value = user
    if (user) {
      localStorage.setItem('roles', JSON.stringify(user.roles || []))
      localStorage.setItem('username', user.username || '')
      localStorage.setItem('userInfo', JSON.stringify(user))
      window.dispatchEvent(new Event('storage'))
    }
  } catch (error) {
    currentUser.value = readStoredUserInfo()
    console.error('刷新当前用户信息失败:', error?.response?.data || error)
  }
}

// --- 规则列表 ---
const rules = ref([])
const rulesLoading = ref(false)
const rulesPage = ref(1)
const rulesSize = ref(20)
const rulesTotal = ref(0)

async function fetchRules() {
  if (!isSuperAdmin.value) return
  rulesLoading.value = true
  try {
    const res = await getAlertRules({ page: rulesPage.value, size: rulesSize.value })
    rules.value = res.data?.records || []
    rulesTotal.value = res.data?.total || 0
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
    histories.value = res.data?.records || []
    historiesTotal.value = res.data?.total || 0
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
const checking = ref(false)

const validateJson = (rule, value, callback) => {
  if (!value || value.trim() === '') return callback()
  try {
    JSON.parse(value)
    callback()
  } catch (e) {
    callback(new Error('不是合法 JSON，key 必须用双引号，如 {"maxCount": -1}'))
  }
}

const formRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  conditionConfig: [{ validator: validateJson, trigger: 'blur' }],
  actionConfig: [{ validator: validateJson, trigger: 'blur' }]
}

function resetForm() {
  form.value = {
    ruleName: '', ruleType: '', targetType: '', targetId: null,
    conditionConfig: '', actionConfig: '', notificationChannels: '', isEnabled: 1
  }
}

function openCreateDialog() {
  if (!isSuperAdmin.value) return
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  if (!isSuperAdmin.value) return
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

function handleRuleTypeChange(type) {
  const presets = {
    call_volume: '{"maxCount": -1}',
    quota_low: '{"threshold": 20}',
    error_rate: '{"maxRate": 0.5}',
    channel_down: '{"failThreshold": 5}'
  }
  if (!form.value.conditionConfig) {
    form.value.conditionConfig = presets[type] || ''
  }
  if (!form.value.actionConfig) {
    form.value.actionConfig = '{}'
  }
}

async function handleCheckNow() {
  if (!isSuperAdmin.value) {
    ElMessage.warning('仅企业管理员可手动触发告警检测')
    return
  }
  checking.value = true
  try {
    const res = await triggerCheckNow()
    ElMessage.success(res.message || '检测完成，请切换到「告警历史」查看结果')
    fetchHistories()
    fetchRules()
  } catch (e) {
    ElMessage.error('触发失败：' + (e?.message || '未知错误'))
  } finally {
    checking.value = false
  }
}

async function handleSubmit() {
  if (!isSuperAdmin.value) return
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
  if (!isSuperAdmin.value) return
  await toggleAlertRule(row.id)
  ElMessage.success(row.isEnabled === 1 ? '已禁用' : '已启用')
  fetchRules()
}

async function handleDelete(row) {
  if (!isSuperAdmin.value) return
  await ElMessageBox.confirm(`确定删除规则「${row.ruleName}」？`, '确认', { type: 'warning' })
  await deleteAlertRule(row.id)
  ElMessage.success('已删除')
  fetchRules()
}

onMounted(async () => {
  await fetchCurrentUser()
  if (!isSuperAdmin.value && activeTab.value === 'rules') {
    activeTab.value = 'histories'
  } else if (isSuperAdmin.value && activeTab.value !== 'rules') {
    activeTab.value = 'rules'
  }
  if (isSuperAdmin.value) fetchRules()
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
.notice-scope {
  padding: 14px 18px;
  margin-bottom: 16px;
  color: rgba(248, 250, 252, 0.72);
  font-size: 13px;
}
</style>
