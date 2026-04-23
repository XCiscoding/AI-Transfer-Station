<template>
  <div class="login-page">
    <!-- 背景装饰层 -->
    <div class="bg-decoration">
      <div class="glow glow-top"></div>
      <div class="glow glow-left"></div>
      <div class="glow glow-right"></div>
    </div>

    <!-- 登录卡片容器 -->
    <div class="login-container">
      <div class="login-card glass-effect">
        <!-- Logo和标题区域 -->
        <div class="login-header">
          <Logo mode="light" size="large" />
          <h1 class="title">AI调度中心</h1>
          <p class="subtitle">欢迎回来</p>
        </div>

        <!-- 登录表单 -->
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <!-- 用户名输入框 -->
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
              clearable
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <!-- 密码输入框 -->
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <!-- 登录按钮 -->
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-button"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 底部提示 -->
        <div class="login-footer">
          <p class="footer-text">安全登录 · 数据加密传输</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login, getUserInfo } from '@/api/auth'
import Logo from '@/components/Logo.vue'

export default {
  name: 'Login',
  components: {
    Logo
  },
  setup() {
    const router = useRouter()
    const loginFormRef = ref(null)
    const loading = ref(false)
    const showPassword = ref(false)

    // 表单数据
    const loginForm = reactive({
      username: '',
      password: ''
    })

    // 表单验证规则
    const loginRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度在3到20个字符之间', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 30, message: '密码长度在6到30个字符之间', trigger: 'blur' }
      ]
    }

    // 登录处理函数
    const handleLogin = async () => {
      // 表单验证
      if (!loginFormRef.value) return

      try {
        await loginFormRef.value.validate()
      } catch (error) {
        return
      }

      // 开始加载状态
      loading.value = true

      try {
        // 调用登录API
        // 注意：request.js拦截器已将response.data解包，所以这里直接使用response
        const response = await login({
          username: loginForm.username,
          password: loginForm.password
        })

        // 检查响应数据
        // request.js响应拦截器已解包response.data，返回结构为 {code:200, data:{token, userId, ...}}
        if (response && response.data && response.data.token) {
          // 存储token到localStorage
          localStorage.setItem('token', response.data.token)

          let userInfo = {
            userId: response.data.userId,
            username: response.data.username || loginForm.username,
            email: response.data.email,
            roles: response.data.roles || [],
            isSuperAdmin: Boolean(response.data.isSuperAdmin || response.data.roles?.includes('SUPER_ADMIN')),
            isTeamOwner: Boolean(response.data.isTeamOwner),
            isTeamMember: Boolean(response.data.isTeamMember),
            status: response.data.status
          }

          try {
            const meResponse = await getUserInfo()
            if (meResponse?.data) {
              userInfo = {
                ...userInfo,
                ...meResponse.data,
                roles: meResponse.data.roles || userInfo.roles,
                isSuperAdmin: Boolean(meResponse.data.isSuperAdmin || meResponse.data.roles?.includes('SUPER_ADMIN')),
                isTeamOwner: Boolean(meResponse.data.isTeamOwner),
                isTeamMember: Boolean(meResponse.data.isTeamMember)
              }
            }
          } catch (userInfoError) {
            console.warn('获取当前用户信息失败，回退使用登录响应:', userInfoError)
          }

          localStorage.setItem('username', userInfo.username || loginForm.username)
          localStorage.setItem('roles', JSON.stringify(userInfo.roles || []))
          localStorage.setItem('userInfo', JSON.stringify(userInfo))

          // 显示成功提示
          ElMessage.success('登录成功')

          // 跳转到首页
          router.push('/')
        } else {
          ElMessage.error('登录失败，请检查用户名和密码')
        }
      } catch (error) {
        // 错误处理
        console.error('登录错误:', error)

        if (error.response) {
          // 服务器返回的错误
          const errorMessage = error.response.data?.message || error.response.data?.msg || '登录失败'
          ElMessage.error(errorMessage)
        } else if (error.request) {
          // 请求已发出但没有收到响应
          ElMessage.error('网络连接异常，请检查网络')
        } else {
          // 其他错误
          ElMessage.error('登录失败，请稍后重试')
        }
      } finally {
        // 结束加载状态
        loading.value = false
      }
    }

    return {
      loginFormRef,
      loginForm,
      loginRules,
      loading,
      showPassword,
      User,
      Lock,
      handleLogin
    }
  }
}
</script>

<style scoped>
/* 导入字体 */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Space+Grotesk:wght@500;600;700&display=swap');

.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background:
    /* 顶部蓝色光晕 */
    radial-gradient(ellipse 120% 80% at 50% 0%, rgba(59, 130, 246, 0.15) 0%, transparent 60%),
    /* 左下角蓝色光晕 */
    radial-gradient(ellipse 80% 60% at 0% 100%, rgba(59, 130, 246, 0.12) 0%, transparent 50%),
    /* 右下角紫色光晕 */
    radial-gradient(ellipse 70% 50% at 100% 100%, rgba(139, 92, 246, 0.08) 0%, transparent 50%),
    /* 主背景渐变 */
    linear-gradient(180deg,
      rgba(30, 41, 59, 0.95) 0%,
      rgba(15, 23, 42, 0.98) 30%,
      rgba(15, 23, 42, 1) 50%,
      rgba(15, 23, 42, 0.98) 70%,
      rgba(30, 41, 59, 0.95) 100%);
}

/* 背景装饰层 */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: hidden;
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  animation: floatGlow 8s ease-in-out infinite;
}

.glow-top {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.3) 0%, transparent 70%);
  top: -200px;
  left: 50%;
  transform: translateX(-50%);
  animation-delay: 0s;
}

.glow-left {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.25) 0%, transparent 70%);
  bottom: -150px;
  left: -100px;
  animation-delay: 2s;
}

.glow-right {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.2) 0%, transparent 70%);
  bottom: 10%;
  right: -100px;
  animation-delay: 4s;
}

@keyframes floatGlow {
  0%, 100% {
    transform: translateY(0) scale(1);
    opacity: 0.4;
  }
  50% {
    transform: translateY(-20px) scale(1.05);
    opacity: 0.5;
  }
}

/* 登录容器 */
.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 20px;
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 登录卡片 - 玻璃拟态效果 */
.login-card {
  border-radius: 24px;
  padding: 48px 40px 40px;
  background:
    linear-gradient(135deg, rgba(30, 41, 59, 0.8) 0%, rgba(15, 23, 42, 0.9) 100%);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.08) inset,
    0 16px 64px rgba(0, 0, 0, 0.3),
    0 0 40px rgba(59, 130, 246, 0.1);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.login-card:hover {
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.12) inset,
    0 20px 72px rgba(0, 0, 0, 0.35),
    0 0 50px rgba(59, 130, 246, 0.15);
}

/* Logo和标题区域 */
.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 28px;
  font-weight: 700;
  color: #F8FAFC;
  margin: 20px 0 8px;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #F8FAFC 0%, #60A5FA 50%, #3B82F6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: 15px;
  color: rgba(248, 250, 252, 0.55);
  font-weight: 400;
  margin: 0;
}

/* 表单样式 */
.login-form {
  margin-top: 32px;
}

.login-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  box-shadow: none;
  transition: all 0.3s ease;
  padding: 4px 12px;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(255, 255, 255, 0.08);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #3B82F6;
  background: rgba(59, 130, 246, 0.08);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1), 0 0 20px rgba(59, 130, 246, 0.1);
}

.login-form :deep(.el-input__inner) {
  color: #F8FAFC;
  font-size: 15px;
  font-family: 'Inter', sans-serif;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: rgba(248, 250, 252, 0.35);
}

.login-form :deep(.el-input__prefix .el-icon) {
  color: rgba(248, 250, 252, 0.45);
  font-size: 18px;
}

.login-form :deep(.el-input__suffix .el-icon) {
  color: rgba(248, 250, 252, 0.45);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-form :deep(.el-form-item__error) {
  color: #EF4444;
  font-size: 12px;
  padding-top: 4px;
}

/* 登录按钮 */
.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  font-family: 'Space Grotesk', sans-serif;
  letter-spacing: 2px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
  border: none;
  transition: all 0.3s ease;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.35);
  margin-top: 8px;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(59, 130, 246, 0.45);
  background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
}

.login-button:active {
  transform: translateY(0);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.35);
}

.login-button.is-loading {
  opacity: 0.85;
  transform: none;
}

/* 底部提示 */
.login-footer {
  text-align: center;
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.footer-text {
  font-size: 13px;
  color: rgba(248, 250, 252, 0.35);
  margin: 0;
  letter-spacing: 0.5px;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .login-container {
    max-width: 90%;
    padding: 16px;
  }

  .login-card {
    padding: 36px 28px 32px;
    border-radius: 20px;
  }

  .title {
    font-size: 24px;
  }

  .subtitle {
    font-size: 14px;
  }

  .login-button {
    height: 44px;
    font-size: 15px;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 28px 20px 24px;
    border-radius: 16px;
  }

  .title {
    font-size: 22px;
  }

  .login-form {
    margin-top: 24px;
  }
}
</style>
