import request from './request'

export const secondhandApi = {
  getList(params) {
    return request({
      url: '/secondhand/list',
      method: 'get',
      params
    })
  },

  getMyList(params) {
    return request({
      url: '/secondhand/my',
      method: 'get',
      params
    })
  },

  getDetail(id) {
    return request({
      url: `/secondhand/${id}`,
      method: 'get'
    })
  },

  publish(data) {
    return request({
      url: '/secondhand/publish',
      method: 'post',
      data
    })
  },

  update(id, data) {
    return request({
      url: `/secondhand/${id}`,
      method: 'put',
      data
    })
  },

  delete(id) {
    return request({
      url: `/secondhand/${id}`,
      method: 'delete'
    })
  },

  remove(id, reason) {
    return request({
      url: `/secondhand/${id}/remove`,
      method: 'put',
      data: { reason }
    })
  },

  markAsSold(id) {
    return request({
      url: `/secondhand/${id}/sold`,
      method: 'put'
    })
  },

  relist(id) {
    return request({
      url: `/secondhand/${id}/relist`,
      method: 'put'
    })
  },

  favorite(id) {
    return request({
      url: `/secondhand/${id}/favorite`,
      method: 'post'
    })
  },

  unfavorite(id) {
    return request({
      url: `/secondhand/${id}/favorite`,
      method: 'delete'
    })
  },

  getFavorites(params) {
    return request({
      url: '/secondhand/favorites',
      method: 'get',
      params
    })
  },

  getFavoriteStatus(id) {
    return request({
      url: `/secondhand/${id}/favorite/status`,
      method: 'get'
    })
  },

  createSubscription(data) {
    return request({
      url: '/secondhand/subscriptions',
      method: 'post',
      data
    })
  },

  getSubscriptions() {
    return request({
      url: '/secondhand/subscriptions',
      method: 'get'
    })
  },

  deleteSubscription(id) {
    return request({
      url: `/secondhand/subscriptions/${id}`,
      method: 'delete'
    })
  },

  getMeetupRecommendation(id) {
    return request({
      url: `/secondhand/${id}/meetup-recommendation`,
      method: 'get'
    })
  },

  getBargainTemplate(id, targetPrice) {
    return request({
      url: `/secondhand/${id}/bargain-template`,
      method: 'get',
      params: targetPrice ? { targetPrice } : {}
    })
  },

  getSellerTrust(sellerId) {
    return request({
      url: `/secondhand/seller/${sellerId}/trust`,
      method: 'get'
    })
  },

  isFavorited(id) {
    return this.getFavoriteStatus(id)
  }
}
