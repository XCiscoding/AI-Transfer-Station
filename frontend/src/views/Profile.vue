<template>
  <div class="profile-page">
    <div class="profile-hero glass-card">
      <div class="avatar-block">
        <el-avatar :size="64" class="profile-avatar">
          {{ avatarText }}
        </el-avatar>
        <div>
          <h2>{{ displayName }}</h2>
          <div class="role-row">
            <el-tag :type="roleTagType" effect="dark" round>{{ roleLabel }}</el-tag>
            <el-tag v-if="userInfo?.isTeamMember" type="success" effect="plain" round>团队成员</el-tag>
          </div>
        </div>
      </div>
      <el-button :loading="loading" type="primary" class="refresh-btn" @click="refreshProfile">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="profile-grid">
      <div class="glass-card profile-card">
        <div class="card-title">
          <el-icon><User /></el-icon>
          账号信息
        </div>
        <div class="info-list">
          <div class="info-row">
            <span>用户ID</span>
            <strong>{{ userInfo?.userId || '-' }}</strong>
          </div>
          <div class="info-row">
            <span>用户名</span>
            <strong>{{ userInfo?.username || '-' }}</strong>
          </div>
          <div class="info-row">
            <span>邮箱</span>
            <strong>{{ userInfo?.email || '-' }}</strong>
          </div>
          <div class="info-row">
            <span>账号状态</span>
            <el-tag :type="userInfo?.status === 1 ? 'success' : 'info'" size="small">
              {{ userInfo?.status === 1 ? '启用' : '未知' }}
            </el-tag>
          </div>
        </div>
      </div>

      <div class="glass-card profile-card">
        <div class="card-title">
          <el-icon><Lock /></el-icon>
          权限范围
        </div>
        <div class="scope-list">
          <div class="scope-item" :class="{ active: isSuperAdmin }">
            <span>企业管理员</span>
            <el-tag size="small" :type="isSuperAdmin ? 'warning' : 'info'">{{ isSuperAdmin ? '已启用' : '未启用' }}</el-tag>
          </div>
          <div class="scope-item" :class="{ active: userInfo?.isTeamOwner }">
            <span>团队管理员</span>
            <el-tag size="small" :type="userInfo?.isTeamOwner ? 'success' : 'info'">{{ userInfo?.isTeamOwner ? '已启用' : '未启用' }}</el-tag>
          </div>
          <div class="scope-item" :class="{ active: userInfo?.isTeamMember }">
            <span>普通成员</span>
            <el-tag size="small" :type="userInfo?.isTeamMember ? 'primary' : 'info'">{{ userInfo?.isTeamMember ? '已加入' : '未加入' }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <div class="glass-card demo-card">
      <div class="card-title">
        <el-icon><Key /></el-icon>
        预设账号
      </div>
      <div class="demo-account-grid">
        <div v-for="account in demoAccounts" :key="account.username" class="demo-account">
          <div>
            <div class="demo-role">{{ account.role }}</div>
            <div class="demo-desc">{{ account.desc }}</div>
          </div>
          <div class="credential-block">
            <code>{{ account.username }}</code>
            <code>{{ account.password }}</code>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Key, Lock, Refresh, User } from '@element-plus/icons-vue'
import { getUserInfo } from '@/api/auth'

function readStoredUserInfo() {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || 'null')
  } catch {
    return null
  }
}

const loading = ref(false)
const userInfo = ref(readStoredUserInfo())

const demoAccounts = [
  { role: '企业管理员', username: 'enterprise_admin', password: 'admin123', desc: '完整管理后台权限' },
  { role: '团队管理员', username: 'team_admin', password: 'team123456', desc: '管理本团队项目和密钥' },
  { role: '普通用户', username: 'demo_user', password: 'user123456', desc: '查看团队、项目并领取密钥' }
]

const roles = computed(() => userInfo.value?.roles || [])
const isSuperAdmin = computed(() => Boolean(userInfo.value?.isSuperAdmin || roles.value.includes('SUPER_ADMIN')))
const roleLabel = computed(() => {
  if (isSuperAdmin.value) return '企业管理员'
  if (userInfo.value?.isTeamOwner) return '团队管理员'
  return '普通用户'
})
const roleTagType = computed(() => {
  if (isSuperAdmin.value) return 'warning'
  if (userInfo.value?.isTeamOwner) return 'success'
  return 'info'
})
const displayName = computed(() => userInfo.value?.realName || userInfo.value?.username || '当前用户')
const avatarText = computed(() => (displayName.value || 'U').slice(0, 1).toUpperCase())

async function refreshProfile() {
  loading.value = true
  try {
    const res = await getUserInfo()
    if (res.code === 200 && res.data) {
      userInfo.value = res.data
      localStorage.setItem('username', res.data.username || '')
      localStorage.setItem('roles', JSON.stringify(res.data.roles || []))
      localStorage.setItem('userInfo', JSON.stringify(res.data))
      ElMessage.success('个人信息已刷新')
    } else {
      ElMessage.error(res.message || '获取个人信息失败')
    }
  } catch (error) {
    console.error('获取个人信息失败:', error)
    ElMessage.error('获取个人信息失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!userInfo.value) {
    refreshProfile()
  }
})
</script>

<style scoped>
.profile-page {
  padding: 20px;
}

.glass-card {
  background:
    linear-gradient(135deg, rgba(30, 41, 59, 0.72) 0%, rgba(15, 23, 42, 0.84) 100%);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.05) inset,
    0 8px 32px rgba(0, 0, 0, 0.2);
}

.profile-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px;
  margin-bottom: 16px;
}

.avatar-block {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.profile-avatar {
  flex-shrink: 0;
  color: #f8fafc;
  background: linear-gradient(135deg, #2563eb 0%, #14b8a6 100%);
  font-size: 24px;
  font-weight: 700;
}

h2 {
  margin: 0 0 10px;
  color: #f8fafc;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0;
}

.role-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.refresh-btn {
  flex-shrink: 0;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #2563eb 0%, #0f766e 100%);
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.profile-card,
.demo-card {
  padding: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  color: #f8fafc;
  font-size: 16px;
  font-weight: 700;
}

.info-list,
.scope-list {
  display: grid;
  gap: 12px;
}

.info-row,
.scope-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.34);
}

.info-row span,
.scope-item span {
  color: rgba(248, 250, 252, 0.62);
  font-size: 13px;
}

.info-row strong {
  min-width: 0;
  color: #f8fafc;
  font-size: 14px;
  font-weight: 600;
  text-align: right;
  overflow-wrap: anywhere;
}

.scope-item.active {
  border-color: rgba(20, 184, 166, 0.36);
  background: rgba(20, 184, 166, 0.08);
}

.demo-account-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.demo-account {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.38);
}

.demo-role {
  margin-bottom: 6px;
  color: #f8fafc;
  font-size: 15px;
  font-weight: 700;
}

.demo-desc {
  color: rgba(248, 250, 252, 0.58);
  font-size: 12px;
  line-height: 1.6;
}

.credential-block {
  display: grid;
  gap: 8px;
}

code {
  display: block;
  padding: 8px 10px;
  border-radius: 6px;
  color: #bfdbfe;
  background: rgba(2, 6, 23, 0.68);
  border: 1px solid rgba(96, 165, 250, 0.16);
  font-size: 12px;
  overflow-wrap: anywhere;
}

@media (max-width: 980px) {
  .profile-grid,
  .demo-account-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .profile-page {
    padding: 12px;
  }

  .profile-hero {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
