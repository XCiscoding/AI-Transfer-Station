import { createRouter, createWebHistory } from 'vue-router'
import Overview from '@/views/Overview.vue'
import Analytics from '@/views/Analytics.vue'

const routes = [
  {
    path: '/',
    name: 'Overview',
    component: Overview,
    meta: { title: '控制台首页' }
  },
  {
    path: '/models',
    name: 'Models',
    component: () => import('@/views/Placeholder.vue'),
    meta: { title: '模型广场' }
  },
  {
    path: '/skills',
    name: 'Skills',
    component: () => import('@/views/Placeholder.vue'),
    meta: { title: 'Skill超市' }
  },
  {
    path: '/tokens',
    name: 'Tokens',
    component: () => import('@/views/Placeholder.vue'),
    meta: { title: '令牌管理' }
  },
  {
    path: '/analytics',
    name: 'Analytics',
    component: Analytics,
    meta: { title: '数据看板' }
  },
  {
    path: '/logs',
    name: 'Logs',
    component: () => import('@/views/Placeholder.vue'),
    meta: { title: '请求日志' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Placeholder.vue'),
    meta: { title: '个人中心' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
