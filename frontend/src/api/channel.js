import request from '@/utils/request'

export function getChannelList(params) {
  return request({
    url: '/channels',
    method: 'get',
    params
  })
}

export function getChannelDetail(id) {
  return request({
    url: `/channels/${id}`,
    method: 'get'
  })
}

export function createChannel(data) {
  return request({
    url: '/channels',
    method: 'post',
    data
  })
}

export function updateChannel(id, data) {
  return request({
    url: `/channels/${id}`,
    method: 'put',
    data
  })
}

export function deleteChannel(id) {
  return request({
    url: `/channels/${id}`,
    method: 'delete'
  })
}

export function testChannel(id) {
  return request({
    url: `/channels/${id}/test`,
    method: 'post'
  })
}
