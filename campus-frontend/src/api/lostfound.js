import request from './request'

// 失物招领相关API
export const lostFoundApi = {
  // 发布失物/招领信息
  publish(data) {
    return request({
      url: '/lostfound/publish',
      method: 'post',
      data
    })
  },

  // 列表（type=LOST 寻物公告 / type=FOUND 招领公告）
  getList(params) {
    return request({
      url: '/lostfound/list',
      method: 'get',
      params
    })
  },

  // 详情
  getDetail(id) {
    return request({
      url: `/lostfound/${id}`,
      method: 'get'
    })
  },

  // 认领
  claim(id, data) {
    return request({
      url: `/lostfound/${id}/claim`,
      method: 'post',
      data
    })
  },

  // 我的发布（全部，含 LOST 和 FOUND）
  getMyList() {
    return request({
      url: '/lostfound/my',
      method: 'get'
    })
  },

  // 我的失物（type=LOST）
  getMyLostList() {
    return request({
      url: '/lostfound/my',
      method: 'get',
      params: { type: 'LOST' }
    })
  },

  // 我的招领（type=FOUND）
  getMyFoundList() {
    return request({
      url: '/lostfound/my',
      method: 'get',
      params: { type: 'FOUND' }
    })
  },

  // 标记已解决（已找回 / 已归还）
  markResolved(id) {
    return request({
      url: `/lostfound/${id}/resolve`,
      method: 'post'
    })
  },

  // 删除
  deleteItem(id) {
    return request({
      url: `/lostfound/${id}`,
      method: 'delete'
    })
  },

  // 智能匹配（LOST -> FOUND，FOUND -> LOST）
  getSmartMatches(id, limit = 5) {
    return request({
      url: `/lostfound/${id}/matches`,
      method: 'get',
      params: { limit }
    })
  }
}
