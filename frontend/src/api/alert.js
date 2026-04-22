import request from '@/utils/request'

export function getAlertRules(params) {
  return request({ url: '/alert-rules', method: 'get', params })
}

export function createAlertRule(data) {
  return request({ url: '/alert-rules', method: 'post', data })
}

export function updateAlertRule(id, data) {
  return request({ url: `/alert-rules/${id}`, method: 'put', data })
}

export function toggleAlertRule(id) {
  return request({ url: `/alert-rules/${id}/toggle`, method: 'patch' })
}

export function deleteAlertRule(id) {
  return request({ url: `/alert-rules/${id}`, method: 'delete' })
}

export function getAlertHistories(params) {
  return request({ url: '/alert-rules/histories', method: 'get', params })
}

export function getAlertUnreadCount() {
  return request({ url: '/alert-rules/unread-count', method: 'get' })
}

export function getRecentAlerts() {
  return request({ url: '/alert-rules/recent', method: 'get' })
}
