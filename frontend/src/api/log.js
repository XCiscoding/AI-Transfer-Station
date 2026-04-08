import request from '@/utils/request'

export function getLogList(params) {
  return request({ url: '/logs', method: 'get', params })
}
