import request from './request'

export const userApi = {
  login(data) {
    return request({
      url: '/user/login',
      method: 'post',
      data
    })
  },

  register(data) {
    return request({
      url: '/user/register',
      method: 'post',
      data
    })
  },

  getUserInfo() {
    return request({
      url: '/user/info',
      method: 'get'
    })
  },

  getUserInfoById(userId) {
    return request({
      url: `/user/${userId}`,
      method: 'get'
    })
  },

  updateProfile(data) {
    return request({
      url: '/user/profile',
      method: 'put',
      data
    })
  },

  changePassword(data) {
    return request({
      url: '/user/password',
      method: 'put',
      data
    })
  },

  getUserList(params) {
    return request({
      url: '/user/list',
      method: 'get',
      params
    })
  },

  getDormitory() {
    return request({
      url: '/user/dormitory',
      method: 'get'
    })
  },

  saveDormitory(data) {
    return request({
      url: '/user/dormitory',
      method: 'post',
      data
    })
  }
}
