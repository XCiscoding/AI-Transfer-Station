import request from '@/utils/request'

export function getModelGroupAll() {
  return request({ url: '/api/v1/model-groups/all', method: 'get' })
}

export function getModelGroupList(params) {
  return request({ url: '/api/v1/model-groups', method: 'get', params })
}

export function createModelGroup(data) {
  return request({ url: '/api/v1/model-groups', method: 'post', data })
}

export function updateModelGroup(id, data) {
  return request({ url: `/api/v1/model-groups/${id}`, method: 'put', data })
}

export function deleteModelGroup(id) {
  return request({ url: `/api/v1/model-groups/${id}`, method: 'delete' })
}
