import request from './request'

export const dormitoryApi = {
  getList(params) {
    return request({
      url: '/dormitory/list',
      method: 'get',
      params
    })
  },

  getDetail(id) {
    return request({
      url: `/dormitory/${id}`,
      method: 'get'
    })
  },

  getMyDormitory() {
    return request({
      url: '/dormitory/my',
      method: 'get'
    })
  },

  getMembers(id) {
    return request({
      url: `/dormitory/${id}/members`,
      method: 'get'
    })
  },

  getBuildings() {
    return request({
      url: '/dormitory/buildings',
      method: 'get'
    })
  },

  createBuilding(data) {
    return request({
      url: '/dormitory/buildings',
      method: 'post',
      data
    })
  },

  updateBuilding(id, data) {
    return request({
      url: `/dormitory/buildings/${id}`,
      method: 'put',
      data
    })
  },

  assignBuildingManager(id, data) {
    return request({
      url: `/dormitory/buildings/${id}/manager`,
      method: 'put',
      data
    })
  },

  getMyBuilding() {
    return request({
      url: '/dormitory/building/my',
      method: 'get'
    })
  },

  getAssignableUsers(params) {
    return request({
      url: '/dormitory/assignable-users',
      method: 'get',
      params
    })
  },

  assignDormitory(data) {
    return request({
      url: '/dormitory/assign',
      method: 'post',
      data
    })
  },

  checkoutDormitory(userId) {
    return request({
      url: `/dormitory/checkout/${userId}`,
      method: 'put'
    })
  },

  createRepair(data) {
    return request({
      url: '/dormitory/repair',
      method: 'post',
      data
    })
  },

  getMyRepairs(params) {
    return request({
      url: '/dormitory/repair/my',
      method: 'get',
      params
    })
  },

  getRepairDetail(id) {
    return request({
      url: `/dormitory/repair/${id}`,
      method: 'get'
    })
  },

  getRepairList(params) {
    return request({
      url: '/dormitory/repair/list',
      method: 'get',
      params
    })
  },

  getRepairStats(building) {
    return request({
      url: '/dormitory/repair/stats',
      method: 'get',
      params: { building }
    })
  },

  handleRepair(id, data) {
    return request({
      url: `/dormitory/repair/${id}/handle`,
      method: 'put',
      data
    })
  },

  closeRepair(id) {
    return request({
      url: `/dormitory/repair/${id}/close`,
      method: 'put'
    })
  },

  cancelRepair(id) {
    return request({
      url: `/dormitory/repair/${id}/cancel`,
      method: 'put'
    })
  },

  updateRepair(id, data) {
    return request({
      url: `/dormitory/repair/${id}`,
      method: 'put',
      data
    })
  },

  reapplyRepair(id) {
    return request({
      url: `/dormitory/repair/${id}/reapply`,
      method: 'put'
    })
  },

  deleteRepair(id) {
    return request({
      url: `/dormitory/repair/${id}`,
      method: 'delete'
    })
  },

  processRepair(id, status, remark) {
    return request({
      url: `/dormitory/repair/${id}`,
      method: 'put',
      params: { status, remark }
    })
  },

  startRepair(id) {
    return request({
      url: `/dormitory/repair/${id}/start`,
      method: 'post'
    })
  },

  completeRepair(id, data) {
    return request({
      url: `/dormitory/repair/${id}/complete`,
      method: 'post',
      data
    })
  }
}
