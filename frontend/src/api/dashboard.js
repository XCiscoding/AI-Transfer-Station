import request from '@/utils/request'

export function getDashboardOverview() {
  return request({ url: '/dashboard/overview', method: 'get' })
}
