import { get, post, put, del } from '@/utils/request'

/**
 * 后台管理 API
 */

// ==================== 数据统计 ====================

/**
 * 获取统计数据
 */
export const getStatistics = () => {
  return get('/v1/admin/statistics')
}

/**
 * 获取交易趋势
 * @param {number} days 天数
 */
export const getTradeTrend = (days = 7) => {
  return get('/v1/admin/statistics/trade-trend', { days })
}

/**
 * 获取用户增长
 * @param {number} days 天数
 */
export const getUserGrowth = (days = 7) => {
  return get('/v1/admin/statistics/user-growth', { days })
}

// ==================== 用户管理 ====================

/**
 * 获取用户列表
 * @param {Object} params 查询参数 { page, size, keyword, role }
 */
export const getUsers = (params = {}) => {
  return get('/v1/admin/users', params)
}

/**
 * 获取用户详情
 * @param {number} userId 用户 ID
 */
export const getUserById = (userId) => {
  return get(`/v1/admin/users/${userId}`)
}

/**
 * 更新用户信息
 * @param {number} userId 用户 ID
 * @param {Object} user 用户信息
 */
export const updateUser = (userId, user) => {
  return put(`/v1/admin/users/${userId}`, user)
}

/**
 * 更新用户状态
 * @param {number} userId 用户 ID
 * @param {number} status 状态
 */
export const updateUserStatus = (userId, status) => {
  return put(`/v1/admin/users/${userId}/status`, null, { params: { status } })
}

/**
 * 删除用户
 * @param {number} userId 用户 ID
 */
export const deleteUser = (userId) => {
  return del(`/v1/admin/users/${userId}`)
}

// ==================== 饰品管理 ====================

/**
 * 获取所有饰品
 */
export const getAllItems = () => {
  return get('/v1/admin/items')
}

/**
 * 添加饰品
 * @param {Object} item 饰品信息
 */
export const addItem = (item) => {
  return post('/v1/admin/items', item)
}

/**
 * 更新饰品
 * @param {number} itemId 饰品 ID
 * @param {Object} item 饰品信息
 */
export const updateItem = (itemId, item) => {
  return put(`/v1/admin/items/${itemId}`, item)
}

/**
 * 删除饰品
 * @param {number} itemId 饰品 ID
 */
export const deleteItem = (itemId) => {
  return del(`/v1/admin/items/${itemId}`)
}

// ==================== 订单管理 ====================

/**
 * 获取所有订单
 */
export const getAllOrders = () => {
  return get('/v1/admin/orders')
}

/**
 * 根据状态获取订单
 * @param {number} status 订单状态
 */
export const getOrdersByStatus = (status) => {
  return get('/v1/admin/orders/by-status', { status })
}

/**
 * 取消订单
 * @param {number} orderId 订单 ID
 */
export const cancelOrder = (orderId) => {
  return post(`/v1/admin/orders/${orderId}/cancel`)
}

// ==================== 资讯管理 ====================

/**
 * 获取所有资讯
 */
export const getAllNews = () => {
  return get('/v1/admin/news')
}

/**
 * 创建资讯
 * @param {Object} news 资讯信息
 */
export const createNews = (news) => {
  return post('/v1/admin/news', news)
}

/**
 * 更新资讯
 * @param {number} newsId 资讯 ID
 * @param {Object} news 资讯信息
 */
export const updateNews = (newsId, news) => {
  return put(`/v1/admin/news/${newsId}`, news)
}

/**
 * 审核资讯
 * @param {number} newsId 资讯 ID
 * @param {number} status 审核状态
 * @param {string} reason 审核原因
 */
export const auditNews = (newsId, status, reason) => {
  return put(`/v1/admin/news/${newsId}/audit`, null, { params: { status, reason } })
}

/**
 * 删除资讯
 * @param {number} newsId 资讯 ID
 */
export const deleteNews = (newsId) => {
  return del(`/v1/admin/news/${newsId}`)
}

// ==================== 玩家秀管理 ====================

/**
 * 获取所有玩家秀
 */
export const getAllPlayerShows = () => {
  return get('/v1/admin/player-shows')
}

/**
 * 删除玩家秀
 * @param {number} showId 玩家秀 ID
 */
export const deletePlayerShow = (showId) => {
  return del(`/v1/admin/player-shows/${showId}`)
}

// ==================== 提现管理 ====================

/**
 * 获取提现申请列表
 * @param {number} status 审核状态
 */
export const getWithdrawals = (status) => {
  return get('/v1/admin/withdrawals', { status })
}

/**
 * 审核提现申请
 * @param {number} withdrawalId 提现 ID
 * @param {number} status 审核状态
 * @param {string} reason 审核原因
 */
export const auditWithdrawal = (withdrawalId, status, reason) => {
  return post(`/v1/admin/withdrawals/${withdrawalId}/audit`, null, { params: { status, reason } })
}

// ==================== 出售订单管理 ====================

/**
 * 获取所有出售订单
 * @param {number} status 订单状态
 */
export const getSellOrders = (status) => {
  return get('/v1/admin/sell-orders', { status })
}

/**
 * 获取出售订单详情
 * @param {number} orderId 订单 ID
 */
export const getSellOrderById = (orderId) => {
  return get(`/v1/admin/sell-orders/${orderId}`)
}

/**
 * 取消出售订单
 * @param {number} orderId 订单 ID
 */
export const cancelSellOrder = (orderId) => {
  return post(`/v1/admin/sell-orders/${orderId}/cancel`)
}

// ==================== 数据清理 ====================

/**
 * 清理已取消的出售订单
 */
export const cleanCancelledSellOrders = () => {
  return del('/v1/admin/clean/sell-orders/cancelled')
}

/**
 * 清理已取消的交易订单
 */
export const cleanCancelledTradeOrders = () => {
  return del('/v1/admin/clean/trade-orders/cancelled')
}

/**
 * 清理过期的出售订单
 */
export const cleanExpiredSellOrders = () => {
  return del('/v1/admin/clean/sell-orders/expired')
}

/**
 * 清理旧的已完成交易订单
 * @param {number} days 天数
 */
export const cleanOldCompletedOrders = (days = 30) => {
  return del('/v1/admin/clean/trade-orders/old-completed', { params: { days } })
}

/**
 * 清理已售出的库存记录
 */
export const cleanSoldInventory = () => {
  return del('/v1/admin/clean/inventory/sold')
}

/**
 * 一键清理所有无用数据
 */
export const cleanAllUselessData = () => {
  return del('/v1/admin/clean/all')
}

// ==================== 轮播图管理 ====================

/**
 * 获取轮播图列表
 */
export const getBanners = () => {
  return get('/v1/admin/banners')
}

/**
 * 添加轮播图
 * @param {Object} banner 轮播图信息
 */
export const addBanner = (banner) => {
  return post('/v1/admin/banners', banner)
}

/**
 * 更新轮播图
 * @param {number} bannerId 轮播图 ID
 * @param {Object} banner 轮播图信息
 */
export const updateBanner = (bannerId, banner) => {
  return put(`/v1/admin/banners/${bannerId}`, banner)
}

/**
 * 删除轮播图
 * @param {number} bannerId 轮播图 ID
 */
export const deleteBanner = (bannerId) => {
  return del(`/v1/admin/banners/${bannerId}`)
}

/**
 * 更新轮播图状态
 * @param {number} bannerId 轮播图 ID
 * @param {number} status 状态
 */
export const updateBannerStatus = (bannerId, status) => {
  return put(`/v1/admin/banners/${bannerId}/status`, null, { params: { status } })
}

/**
 * 更新轮播图排序
 * @param {number} bannerId 轮播图 ID
 * @param {number} sortOrder 排序
 */
export const updateBannerSort = (bannerId, sortOrder) => {
  return put(`/v1/admin/banners/${bannerId}/sort`, null, { params: { sortOrder } })
}
