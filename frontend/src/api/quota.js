import request from '@/utils/request'

export function getQuotaSummary(params) {
  return request({ url: '/api/v1/quota/summary', method: 'get', params })
}

export function getQuotaTransactions(params) {
  return request({ url: '/api/v1/quota/transactions', method: 'get', params })
}

export function rechargeQuota(data) {
  return request({ url: '/api/v1/quota/recharge', method: 'post', data })
}

export function adjustQuota(data) {
  return request({ url: '/api/v1/quota/adjust', method: 'post', data })
}

export function resetQuota(data) {
  return request({ url: '/api/v1/quota/reset', method: 'post', data })
}
