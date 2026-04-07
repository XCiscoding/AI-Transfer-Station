import request from '@/utils/request'

export function getRealKeyList(params) {
  return request({
    url: '/real-keys',
    method: 'get',
    params
  })
}

export function getRealKeyDetail(id) {
  return request({
    url: `/real-keys/${id}`,
    method: 'get'
  })
}

export function createRealKey(data) {
  return request({
    url: '/real-keys',
    method: 'post',
    data
  })
}

export function updateRealKey(id, data) {
  return request({
    url: `/real-keys/${id}`,
    method: 'put',
    data
  })
}

export function toggleRealKeyStatus(id) {
  return request({
    url: `/real-keys/${id}/status`,
    method: 'put'
  })
}

export function deleteRealKey(id) {
  return request({
    url: `/real-keys/${id}`,
    method: 'delete'
  })
}
