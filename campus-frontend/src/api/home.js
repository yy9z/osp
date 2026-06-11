import request from './request'

export const homeApi = {
  getHomeData() {
    return request({
      url: '/v1/home/data',
      method: 'get'
    })
  },

  getProactiveTips() {
    return request({
      url: '/v1/home/proactive-tips',
      method: 'get'
    })
  }
}
