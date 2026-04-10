import request from '@/utils/request'

export function getTeamList(params) {
  return request({ url: '/teams', method: 'get', params })
}

export function getTeamModelGroups(id) {
  return request({ url: `/teams/${id}/model-groups`, method: 'get' })
}

export function getTeamMembers(teamId) {
  return request({ url: `/teams/${teamId}/members`, method: 'get' })
}

export function getTeamMemberCandidates(teamId, params) {
  return request({ url: `/teams/${teamId}/member-candidates`, method: 'get', params })
}

export function addTeamMember(teamId, data) {
  return request({ url: `/teams/${teamId}/members`, method: 'post', data })
}

export function removeTeamMember(teamId, userId) {
  return request({ url: `/teams/${teamId}/members/${userId}`, method: 'delete' })
}

export function transferTeamOwner(teamId, data) {
  return request({ url: `/teams/${teamId}/owner`, method: 'put', data })
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
