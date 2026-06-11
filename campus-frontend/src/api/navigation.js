/**
 * 校园导航 API 接口
 */
import request from './request'

export const navigationApi = {
  getPois() {
    return request({
      url: '/user/navigation/all',
      method: 'get'
    })
  },

  getPoisByCategory(category) {
    return request({
      url: `/user/navigation/category/${category}`,
      method: 'get'
    })
  },

  getNearby(params) {
    return request({
      url: '/user/navigation/nearby',
      method: 'get',
      params: {
        lat: params.lat,
        lng: params.lng,
        radius: params.radius || 500
      }
    })
  },

  resolvePlace(data) {
    return request({
      url: '/user/navigation/resolve',
      method: 'post',
      data
    })
  },

  calculateRoute(data) {
    return request({
      url: '/user/navigation/route',
      method: 'post',
      data
    })
  },

  calculateRouteAmap(data) {
    return request({
      url: '/user/navigation/route/amap',
      method: 'post',
      data
    })
  },

  getRegions() {
    return request({
      url: '/user/navigation/regions',
      method: 'get'
    })
  },

  getPoiDetail(id) {
    return request({
      url: `/user/navigation/places/${id}`,
      method: 'get'
    })
  },

  searchPois(keyword) {
    return request({
      url: '/user/navigation/places',
      method: 'get',
      params: { keyword }
    })
  }
}

export default navigationApi
