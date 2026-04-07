import request from '@/utils/request'

export function getModelList(params) {
  return request({
    url: '/models',
    method: 'get',
    params
  })
}

export function createModel(data) {
  return request({
    url: '/models',
    method: 'post',
    data
  })
}

export function toggleModelStatus(id) {
  return request({
    url: `/models/${id}/status`,
    method: 'put'
  })
}

export function deleteModel(id) {
  return request({
    url: `/models/${id}`,
    method: 'delete'
  })
}
