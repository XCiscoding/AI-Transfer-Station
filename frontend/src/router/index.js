import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import MainLayout from '@/components/MainLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录', requiresAuth: false }
  },
  // 所有需要 layout（侧边栏+顶栏）的页面都放到 MainLayout 下
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Overview',
        component: () => import('@/views/Overview.vue'),
        meta: { title: '控制台首页', requiresAuth: true }
      },
      {
        path: 'models',
        name: 'Models',
        component: () => import('@/views/Placeholder.vue'),
        meta: { title: '模型广场', requiresAuth: true }
      },
      {
        path: 'skills',
        name: 'Skills',
        component: () => import('@/views/Placeholder.vue'),
        meta: { title: 'Skill超市', requiresAuth: true }
      },
      {
        path: 'tokens',
        name: 'Tokens',
        component: () => import('@/views/TokenManagement.vue'),
        meta: { title: '令牌管理', requiresAuth: true }
      },
      {
        path: 'channels',
        name: 'Channels',
        component: () => import('@/views/ChannelManagement.vue'),
        meta: { title: '渠道管理', requiresAuth: true }
      },
      {
        path: 'teams',
        name: 'Teams',
        component: () => import('@/views/TeamManagement.vue'),
        meta: { title: '团队管理', requiresAuth: true }
      },
      {
        path: 'projects',
        name: 'Projects',
        component: () => import('@/views/ProjectManagement.vue'),
        meta: { title: '项目管理', requiresAuth: true }
      },
      {
        path: 'analytics',
        name: 'Analytics',
        component: () => import('@/views/Analytics.vue'),
        meta: { title: '数据看板', requiresAuth: true }
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/RequestLog.vue'),
        meta: { title: '请求日志', requiresAuth: true }
      },
      {
        path: 'quota-flow',
        name: 'QuotaFlow',
        component: () => import('@/views/QuotaFlow.vue'),
        meta: { title: '额度流水', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Placeholder.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

function getStoredRoles() {
  try {
    return JSON.parse(localStorage.getItem('roles') || '[]')
  } catch {
    return []
  }
}

// 路由守卫：未登录跳转到登录页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const roles = getStoredRoles()

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    void roles
    next()
  }
})

export default router
