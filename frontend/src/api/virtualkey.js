import request from '@/utils/request'

export function getVirtualKeyList(params) {
  return request({
    url: '/virtual-keys',
    method: 'get',
    params
  })
}

export function getVirtualKeyDetail(id) {
  return request({
    url: `/virtual-keys/${id}`,
    method: 'get'
  })
}

export function createVirtualKey(data) {
  return request({
    url: '/virtual-keys',
    method: 'post',
    data
  })
}

export function updateVirtualKey(id, data) {
  return request({
    url: `/virtual-keys/${id}`,
    method: 'put',
    data
  })
}

export function refreshVirtualKey(id) {
  return request({
    url: `/virtual-keys/${id}/refresh`,
    method: 'put'
  })
}

export function toggleVirtualKeyStatus(id) {
  return request({
    url: `/virtual-keys/${id}/status`,
    method: 'put'
  })
}

export function deleteVirtualKey(id) {
  return request({
    url: `/virtual-keys/${id}`,
    method: 'delete'
  })
}
