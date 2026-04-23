<template>
  <div class="team-management">
    <div class="filter-card glass-card role-banner">
      <div>
        <div class="role-title">{{ roleTitle }}</div>
        <div class="role-subtitle">
          {{ roleSubtitle }}
        </div>
      </div>
      <div v-if="isTeamOwnerView" class="role-tip">模型分组由企业管理员统一配置</div>
      <div v-else-if="isMemberView" class="role-tip">普通用户只读查看所在团队</div>
    </div>

    <div class="filter-card glass-card">
      <div class="filter-header">
        <el-form :model="searchForm" inline class="filter-form">
          <el-form-item label="搜索">
            <el-input
              v-model="searchForm.keyword"
              :placeholder="isSuperAdmin ? '输入团队名称或编码' : isTeamOwnerView ? '输入成员用户名' : '输入团队名称'"
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
        <el-button
          v-if="isSuperAdmin"
          type="primary"
          class="add-btn"
          :disabled="!authReady"
          @click="handleAdd"
        >
          <el-icon><Plus /></el-icon>
          新增团队
        </el-button>
      </div>
    </div>

    <div v-if="isSuperAdmin || isMemberView" class="table-card glass-card">
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

        <el-table-column prop="ownerName" label="团队管理员" width="120" align="center" />

        <el-table-column label="可用分组" min-width="220">
          <template #default="{ row }">
            <div v-if="row.allowedGroups?.length" class="group-tags">
              <el-tag
                v-for="group in row.allowedGroups"
                :key="group.id"
                size="small"
                type="success"
                effect="plain"
              >
                {{ group.groupName }}
              </el-tag>
            </div>
            <span v-else class="empty-text">未配置</span>
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

        <el-table-column :width="isSuperAdmin ? 300 : 120" label="操作" align="center">
          <template #default="{ row }">
            <div v-if="isSuperAdmin" class="action-buttons">
              <el-button type="success" link size="small" @click="handleIssueKey(row)">
                <el-icon><Key /></el-icon>发放Key
              </el-button>
              <el-button v-if="canManageRow(row)" type="warning" link size="small" @click="openMemberWorkspace(row)">
                <el-icon><UserFilled /></el-icon>成员管理
              </el-button>
              <el-button v-if="canManageRow(row)" type="primary" link size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button v-if="isSuperAdmin" type="danger" link size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>
            <el-tag v-else size="small" type="info">只读</el-tag>
          </template>
        </el-table-column>
      </el-table>

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

    <div v-else-if="isTeamOwnerView && selectedTeam && canManageSelectedTeam" class="table-card glass-card team-owner-workspace">
      <div class="workspace-header">
        <div>
          <div class="workspace-title">团队成员管理</div>
          <div class="workspace-subtitle">当前团队：{{ selectedTeam.teamName }}（{{ selectedTeam.teamCode }}）</div>
        </div>
        <div class="workspace-actions">
          <el-button type="primary" @click="openAddMemberDialog">添加成员</el-button>
          <el-button @click="fetchTeamMembers(selectedTeam.id)">刷新</el-button>
        </div>
      </div>

      <el-row :gutter="16">
        <el-col :xs="24" :lg="14">
          <div class="workspace-panel">
            <div class="panel-title">成员列表</div>
            <el-table :data="filteredMemberList" v-loading="memberLoading" class="member-table" empty-text="暂无成员">
              <el-table-column prop="username" label="用户名" min-width="120" />
              <el-table-column prop="realName" label="姓名" min-width="100">
                <template #default="{ row }">{{ row.realName || '-' }}</template>
              </el-table-column>
              <el-table-column prop="role" label="角色" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.owner ? 'warning' : 'info'" size="small">
                    {{ row.owner ? '管理员' : '成员' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="joinedAt" label="加入时间" width="160" align="center">
                <template #default="{ row }">{{ formatTime(row.joinedAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="180" align="center">
                <template #default="{ row }">
                  <div class="action-buttons">
                    <el-button
                      v-if="canManageRow(selectedTeam) && !row.owner"
                      type="primary"
                      link
                      size="small"
                      @click="handleTransferOwner(row)"
                    >
                      转交管理员
                    </el-button>
                    <el-button
                      v-if="canManageRow(selectedTeam)"
                      type="danger"
                      link
                      size="small"
                      :disabled="row.owner"
                      @click="handleRemoveMember(row)"
                    >
                      移除
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
        <el-col :xs="24" :lg="10">
          <div class="workspace-panel">
            <div class="panel-title">团队内发 Key</div>
            <div class="panel-body">成员管理完成后，可继续从当前团队进入发 Key 流程。</div>
            <el-button type="success" plain class="workspace-key-btn" @click="handleIssueKey(selectedTeam)">
              进入当前团队发 Key
            </el-button>
          </div>
        </el-col>
      </el-row>
    </div>

    <div v-else class="table-card glass-card empty-workspace-card">
      <div class="empty-workspace-title">{{ emptyStateTitle }}</div>
      <div class="empty-workspace-text">{{ emptyStateText }}</div>
    </div>

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

        <el-row v-if="isSuperAdmin" :gutter="20">
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

        <el-form-item v-if="isEdit && isSuperAdmin" label="团队管理员" prop="ownerId">
          <el-select
            v-model="formData.ownerId"
            filterable
            clearable
            placeholder="请选择团队管理员"
            style="width: 100%"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="user.username"
              :value="user.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="isSuperAdmin" label="可用分组">
          <el-select
            v-model="formData.allowedGroupIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="配置团队可用模型分组"
            style="width: 100%"
          >
            <el-option
              v-for="group in allModelGroupOptions"
              :key="group.id"
              :label="group.groupName"
              :value="group.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-else label="可用分组">
          <div class="readonly-groups">
            <div v-if="readonlyAllowedGroups.length" class="group-tags">
              <el-tag
                v-for="group in readonlyAllowedGroups"
                :key="group.id"
                size="small"
                type="success"
                effect="plain"
              >
                {{ group.groupName }}
              </el-tag>
            </div>
            <span v-else class="empty-text">当前团队未配置可用模型分组</span>
          </div>
        </el-form-item>

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

    <el-dialog
      v-model="memberDialogVisible"
      title="添加团队成员"
      width="480px"
      :close-on-click-modal="false"
      class="team-dialog"
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="搜索用户">
          <el-input
            v-model="memberSearchKeyword"
            placeholder="输入用户名或姓名"
            clearable
            @input="debounceFetchCandidates"
          />
        </el-form-item>
        <el-form-item label="选择成员">
          <el-select
            v-model="selectedCandidateUserId"
            filterable
            placeholder="请选择要添加的成员"
            style="width: 100%"
            :loading="candidateLoading"
          >
            <el-option
              v-for="user in memberCandidates"
              :key="user.id"
              :label="user.realName ? `${user.username}（${user.realName}）` : user.username"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="memberDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="memberSubmitLoading" @click="handleAddMember">添加</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Delete, Key, UserFilled } from '@element-plus/icons-vue'
import {
  getTeamList,
  createTeam,
  updateTeam,
  deleteTeam,
  getTeamMembers,
  getTeamMemberCandidates,
  addTeamMember,
  removeTeamMember,
  transferTeamOwner
} from '@/api/team'
import { getModelGroupAll } from '@/api/modelgroup'
import { getUserInfo } from '@/api/auth'
import request from '@/utils/request'

const router = useRouter()
const tableData = ref([])
const loading = ref(false)
const authReady = ref(false)
const isSuperAdmin = ref(false)
const isTeamOwner = ref(false)
const currentUser = ref(null)
const userOptions = ref([])
const readonlyAllowedGroups = ref([])
const selectedTeam = ref(null)
const memberList = ref([])
const memberLoading = ref(false)
const memberDialogVisible = ref(false)
const memberCandidates = ref([])
const candidateLoading = ref(false)
const memberSubmitLoading = ref(false)
const selectedCandidateUserId = ref(null)
const memberSearchKeyword = ref('')

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
  ownerId: null,
  quotaLimit: 0,
  quotaWeight: 1.0,
  allowedGroupIds: []
})

const filteredMemberList = computed(() => {
  const keyword = searchForm.keyword?.trim()
  if (!keyword || isSuperAdmin.value) {
    return memberList.value
  }
  return memberList.value.filter(member => member.username?.includes(keyword))
})

const isTeamOwnerView = computed(() => !isSuperAdmin.value && isTeamOwner.value)
const isMemberView = computed(() => !isSuperAdmin.value && !isTeamOwner.value)
const hasTeamManagementAccess = computed(() => isSuperAdmin.value || isTeamOwnerView.value || isMemberView.value)
const roleTitle = computed(() => {
  if (isSuperAdmin.value) return '企业管理员视角'
  if (isTeamOwnerView.value) return '团队管理员视角'
  return '普通用户视角'
})
const roleSubtitle = computed(() => {
  if (isSuperAdmin.value) {
    return '创建团队、配置团队资源、进入团队发放 Key。'
  }
  if (isTeamOwnerView.value) {
    return '查看自己负责的团队，管理成员并在团队上下文内发放 Key。'
  }
  return '查看自己所在团队及可用模型分组，不能修改团队配置。'
})
const emptyStateTitle = computed(() => {
  if (!hasTeamManagementAccess.value) return '当前账号没有团队管理权限'
  if (isTeamOwnerView.value) return '当前账号不是任何团队管理员'
  if (isMemberView.value) return '当前账号暂未加入任何团队'
  return '暂无可管理团队'
})
const emptyStateText = computed(() => {
  if (!hasTeamManagementAccess.value) return '当前账号既不是企业管理员，也不是任何团队的 Team.owner。'
  if (isTeamOwnerView.value) return '当前账号虽然具备团队管理员身份标识，但还没有可进入的 owner 团队。'
  if (isMemberView.value) return '请联系团队管理员先把该账号加入团队。'
  return '当前账号没有可进入的团队管理工作区。'
})

const canManageSelectedTeam = computed(() => canManageRow(selectedTeam.value))

const formRules = computed(() => ({
  teamName: [{ required: true, message: '请输入团队名称', trigger: 'blur' }],
  teamCode: [
    { required: true, message: '请输入团队编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/, message: '以字母开头，仅允许字母、数字、下划线、连字符', trigger: 'blur' }
  ]
}))

let searchTimer = null
let candidateTimer = null

onMounted(async () => {
  await initPage()
})

const allModelGroupOptions = ref([])

async function initPage() {
  authReady.value = false
  await fetchCurrentUser()

  if (hasTeamManagementAccess.value) {
    await fetchList()
  } else {
    tableData.value = []
    pagination.total = 0
    selectedTeam.value = null
    readonlyAllowedGroups.value = []
    memberList.value = []
  }

  if (isSuperAdmin.value) {
    loadSuperAdminResources()
  } else {
    allModelGroupOptions.value = []
    userOptions.value = []
  }

  authReady.value = true
}

async function fetchCurrentUser() {
  try {
    const res = await getUserInfo()
    const user = res.code === 200 ? res.data : null
    currentUser.value = user
    isSuperAdmin.value = Boolean(user?.isSuperAdmin || user?.roles?.includes('SUPER_ADMIN'))
    isTeamOwner.value = Boolean(user?.isTeamOwner)
    if (user) {
      localStorage.setItem('roles', JSON.stringify(user.roles || []))
      localStorage.setItem('username', user.username || '')
      localStorage.setItem('userInfo', JSON.stringify(user))
      window.dispatchEvent(new Event('storage'))
    }
  } catch (error) {
    currentUser.value = null
    isSuperAdmin.value = false
    isTeamOwner.value = false
    localStorage.removeItem('userInfo')
    window.dispatchEvent(new Event('storage'))
    console.error('获取当前用户信息失败:', error?.response?.data || error)
    ElMessage.error('获取当前用户信息失败')
  }
}

async function fetchUsers() {
  if (!isSuperAdmin.value) {
    userOptions.value = []
    return
  }
  try {
    const res = await request({ url: '/users', method: 'get' })
    userOptions.value = res.code === 200 && Array.isArray(res.data) ? res.data : []
  } catch (error) {
    userOptions.value = []
  }
}

async function fetchAllModelGroups() {
  try {
    const res = await getModelGroupAll()
    allModelGroupOptions.value = res.code === 200 && res.data ? res.data : []
  } catch (e) {
    allModelGroupOptions.value = []
    console.warn('获取全量模型分组失败:', e)
  }
}

function canManageRow(row) {
  if (!row) {
    return false
  }
  if (isSuperAdmin.value) {
    return true
  }
  const currentUserId = currentUser.value?.userId
  const currentUsername = currentUser.value?.username
  return row?.ownerId === currentUserId || row?.ownerName === currentUsername
}

function handleIssueKey(row) {
  if (!isSuperAdmin.value && !canManageRow(row)) {
    ElMessage.warning('仅团队管理员可在当前团队发放 Key')
    return
  }
  if (!row.allowedGroupIds || row.allowedGroupIds.length === 0) {
    ElMessage.warning('当前团队未配置可用模型分组')
    return
  }

  router.push({
    path: '/tokens',
    query: {
      teamId: String(row.id),
      teamName: row.teamName,
      autoCreate: '1'
    }
  })
}

async function fetchList() {
  if (!hasTeamManagementAccess.value) {
    tableData.value = []
    pagination.total = 0
    selectedTeam.value = null
    readonlyAllowedGroups.value = []
    memberList.value = []
    return
  }

  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: isSuperAdmin.value ? (searchForm.keyword || undefined) : undefined
    }
    const res = await getTeamList(params)
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      pagination.total = res.data.total || 0
      if (selectedTeam.value) {
        const latestTeam = tableData.value.find(item => item.id === selectedTeam.value.id)
        if (latestTeam) {
          selectedTeam.value = latestTeam
        }
      }
      if (isTeamOwnerView.value) {
        const manageableTeams = tableData.value.filter(item => canManageRow(item))
        const nextSelectedTeam = selectedTeam.value && manageableTeams.some(item => item.id === selectedTeam.value.id)
          ? manageableTeams.find(item => item.id === selectedTeam.value.id)
          : manageableTeams[0] || null
        selectedTeam.value = nextSelectedTeam
        readonlyAllowedGroups.value = nextSelectedTeam?.allowedGroups || []
        if (nextSelectedTeam) {
          await fetchTeamMembers(nextSelectedTeam.id)
        } else {
          memberList.value = []
        }
      }
    } else {
      ElMessage.error(res.message || '获取团队列表失败')
    }
  } catch (error) {
    console.error('获取团队列表失败:', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '团队列表加载失败')
    tableData.value = []
    pagination.total = 0
    selectedTeam.value = null
    readonlyAllowedGroups.value = []
    memberList.value = []
  } finally {
    loading.value = false
  }
}

async function fetchTeamMembers(teamId) {
  memberLoading.value = true
  try {
    const res = await getTeamMembers(teamId)
    memberList.value = res.code === 200 && Array.isArray(res.data) ? res.data : []
  } catch (error) {
    memberList.value = []
  } finally {
    memberLoading.value = false
  }
}

async function openMemberWorkspace(row) {
  if (!canManageRow(row)) {
    ElMessage.warning('仅团队管理员可进入当前团队工作区')
    return
  }
  selectedTeam.value = row
  readonlyAllowedGroups.value = row.allowedGroups || []
  await fetchTeamMembers(row.id)
}

async function fetchMemberCandidates() {
  if (!selectedTeam.value) return
  candidateLoading.value = true
  try {
    const res = await getTeamMemberCandidates(selectedTeam.value.id, {
      keyword: memberSearchKeyword.value || undefined
    })
    memberCandidates.value = res.code === 200 && Array.isArray(res.data) ? res.data : []
  } catch (error) {
    memberCandidates.value = []
  } finally {
    candidateLoading.value = false
  }
}

function debounceFetchCandidates() {
  if (candidateTimer) clearTimeout(candidateTimer)
  candidateTimer = setTimeout(() => {
    fetchMemberCandidates()
  }, 400)
}

function openAddMemberDialog() {
  if (!selectedTeam.value) return
  selectedCandidateUserId.value = null
  memberSearchKeyword.value = ''
  memberDialogVisible.value = true
  fetchMemberCandidates()
}

async function handleAddMember() {
  if (!selectedTeam.value || !selectedCandidateUserId.value) {
    ElMessage.warning('请先选择要添加的成员')
    return
  }
  memberSubmitLoading.value = true
  try {
    const res = await addTeamMember(selectedTeam.value.id, { userId: selectedCandidateUserId.value })
    if (res.code === 200) {
      ElMessage.success('添加成员成功')
      memberDialogVisible.value = false
      memberList.value = Array.isArray(res.data) ? res.data : []
      await fetchList()
    }
  } catch (error) {
    ElMessage.error(error.message || '添加成员失败')
  } finally {
    memberSubmitLoading.value = false
  }
}

async function handleRemoveMember(row) {
  if (!selectedTeam.value) return
  try {
    await ElMessageBox.confirm(`确定移除成员「${row.username}」吗？`, '移除确认', {
      confirmButtonText: '确定移除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await removeTeamMember(selectedTeam.value.id, row.userId)
    if (res.code === 200) {
      ElMessage.success('移除成员成功')
      memberList.value = Array.isArray(res.data) ? res.data : []
      await fetchList()
    }
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  }
}

async function handleTransferOwner(row) {
  if (!selectedTeam.value) return
  try {
    await ElMessageBox.confirm(`确定将团队管理员转交给「${row.username}」吗？`, '转交确认', {
      confirmButtonText: '确定转交',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await transferTeamOwner(selectedTeam.value.id, { newOwnerId: row.userId })
    if (res.code === 200) {
      ElMessage.success('团队管理员已转交')
      memberList.value = Array.isArray(res.data) ? res.data : []
      await fetchList()
      await fetchCurrentUser()
    }
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
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

function resetFormData() {
  Object.assign(formData, {
    teamName: '',
    teamCode: '',
    description: '',
    ownerId: currentUser.value?.userId || null,
    quotaLimit: 0,
    quotaWeight: 1.0,
    allowedGroupIds: []
  })
  readonlyAllowedGroups.value = []
}

function buildCreatePayload() {
  return {
    teamName: formData.teamName,
    teamCode: formData.teamCode,
    description: formData.description,
    quotaLimit: formData.quotaLimit,
    quotaWeight: formData.quotaWeight
  }
}

async function loadSuperAdminResources() {
  try {
    await Promise.all([fetchAllModelGroups(), fetchUsers()])
  } catch (error) {
    console.error('加载企业管理员资源失败:', error)
  }
}

function handleAdd() {
  if (!authReady.value) {
    ElMessage.warning('正在确认当前用户权限，请稍后再试')
    return
  }
  if (!isSuperAdmin.value) {
    ElMessage.warning('前端判断：仅企业管理员可创建团队')
    console.warn('team-create blocked in handleAdd', {
      authReady: authReady.value,
      isSuperAdmin: isSuperAdmin.value,
      currentUser: currentUser.value
    })
    return
  }
  isEdit.value = false
  currentEditId.value = null
  resetFormData()
  dialogVisible.value = true
}

function handleEdit(row) {
  if (!canManageRow(row)) {
    ElMessage.warning('仅团队管理员可编辑当前团队')
    return
  }
  isEdit.value = true
  currentEditId.value = row.id
  Object.assign(formData, {
    teamName: row.teamName,
    teamCode: row.teamCode,
    description: row.description || '',
    ownerId: row.ownerId || null,
    quotaLimit: Number(row.quotaLimit) || 0,
    quotaWeight: Number(row.quotaWeight) || 1.0,
    allowedGroupIds: row.allowedGroupIds || []
  })
  readonlyAllowedGroups.value = row.allowedGroups || []
  dialogVisible.value = true
}

function buildUpdatePayload() {
  const payload = {
    teamName: formData.teamName,
    description: formData.description
  }
  if (isSuperAdmin.value) {
    payload.ownerId = formData.ownerId
    payload.quotaLimit = formData.quotaLimit
    payload.quotaWeight = formData.quotaWeight
    payload.allowedGroupIds = formData.allowedGroupIds
  }
  return payload
}

function handleSubmit() {
  if (submitLoading.value) return
  if (!authReady.value) {
    ElMessage.warning('正在确认当前用户权限，请稍后再试')
    return
  }
  if (!isEdit.value && !isSuperAdmin.value) {
    ElMessage.warning('前端判断：当前身份不是企业管理员，已阻止创建请求')
    console.warn('team-create blocked before submit', {
      authReady: authReady.value,
      isSuperAdmin: isSuperAdmin.value,
      currentUser: currentUser.value,
      formData: { ...formData }
    })
    return
  }
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      let res
      if (isEdit.value) {
        res = await updateTeam(currentEditId.value, buildUpdatePayload())
      } else {
        res = await createTeam(buildCreatePayload())
      }
      if (res.code === 200) {
        ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
        dialogVisible.value = false
        fetchList()
      } else {
        console.error('team-submit non-200 response:', res)
        ElMessage.error(res.message || '操作失败')
      }
    } catch (error) {
      console.error('team-submit request failed:', error?.response?.data || error)
      ElMessage.error(error.response?.data?.message || error.message || '操作失败')
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

.role-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.role-title {
  font-size: 18px;
  font-weight: 700;
  color: #F8FAFC;
}

.role-subtitle,
.role-tip,
.empty-text {
  color: rgba(248, 250, 252, 0.65);
  font-size: 13px;
}

.group-tags,
.readonly-groups {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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

.team-owner-workspace {
  padding: 20px;
}

.empty-workspace-card {
  padding: 32px 20px;
  text-align: center;
}

.empty-workspace-title {
  font-size: 16px;
  font-weight: 700;
  color: #F8FAFC;
}

.empty-workspace-text {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(248, 250, 252, 0.65);
}

.workspace-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}

.workspace-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.workspace-title {
  font-size: 16px;
  font-weight: 700;
  color: #F8FAFC;
}

.workspace-subtitle,
.panel-body {
  font-size: 13px;
  color: rgba(248, 250, 252, 0.65);
}

.workspace-panel {
  min-height: 140px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(15, 23, 42, 0.35);
}

.panel-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #F8FAFC;
}

.workspace-key-btn {
  margin-top: 16px;
}

.member-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(30, 41, 59, 0.45);
  --el-table-row-hover-bg-color: rgba(59, 130, 246, 0.08);
  --el-table-border-color: rgba(255, 255, 255, 0.06);
  --el-table-header-text-color: rgba(248, 250, 252, 0.85);
  --el-table-text-color: #F8FAFC;
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

.action-buttons { display: flex; justify-content: center; align-items: center; gap: 4px; flex-wrap: wrap; }
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

.team-form :deep(.el-input__wrapper),
.team-form :deep(.el-select__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: none;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.team-form :deep(.el-input__wrapper.is-focus),
.team-form :deep(.el-select__wrapper.is-focused) {
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
