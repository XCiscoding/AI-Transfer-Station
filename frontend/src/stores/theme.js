import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  // 主题状态：'light' | 'dark'
  const theme = ref(localStorage.getItem('theme') || 'light')

  // 是否为深色模式
  const isDark = computed(() => theme.value === 'dark')

  // 切换主题
  const toggleTheme = () => {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    localStorage.setItem('theme', theme.value)
    applyTheme()
  }

  // 设置主题
  const setTheme = (newTheme) => {
    theme.value = newTheme
    localStorage.setItem('theme', theme.value)
    applyTheme()
  }

  // 应用主题到文档
  const applyTheme = () => {
    const html = document.documentElement
    if (theme.value === 'dark') {
      html.classList.add('dark')
    } else {
      html.classList.remove('dark')
    }
  }

  // 初始化主题
  const initTheme = () => {
    applyTheme()
  }

  return {
    theme,
    isDark,
    toggleTheme,
    setTheme,
    initTheme
  }
})
