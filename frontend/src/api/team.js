import request from '@/utils/request'

export function getTeamList(params) {
  return request({ url: '/teams', method: 'get', params })
}

export function createTeam(data) {
  return request({ url: '/teams', method: 'post', data })
}

export function updateTeam(id, data) {
  return request({ url: `/teams/${id}`, method: 'put', data })
}

export function deleteTeam(id) {
  return request({ url: `/teams/${id}`, method: 'delete' })
}
