import request from '@/utils/request'

export function getLogList(params) {
  return request({ url: '/api/v1/logs', method: 'get', params })
}
