import { get, post, put, del } from '@/utils/request'

/**
 * 饰品相关API
 */

/**
 * 获取饰品列表
 * @param {Object} params 查询参数 { category, exterior, quality, keyword, sortField, sortOrder, page, size }
 * @returns {Promise}
 */
export const getItemList = (params = {}) => {
  return get('/v1/items', params)
}

/**
 * 获取饰品详情
 * @param {number} id 饰品ID
 * @returns {Promise}
 */
export const getItemById = (id) => {
  return get(`/v1/items/${id}`)
}

export const getItemMarketPanel = (id) => {
  return get(`/v1/items/${id}/market-panel`)
}

/**
 * 获取所有启用的饰品
 * @returns {Promise}
 */
export const getAllItems = () => {
  return get('/v1/items/all')
}

/**
 * 根据分类获取饰品
 * @param {string} category 分类
 * @returns {Promise}
 */
export const getItemsByCategory = (category) => {
  return get(`/v1/items/category/${category}`)
}

/**
 * 搜索饰品
 * @param {string} keyword 关键词
 * @returns {Promise}
 */
export const searchItems = (keyword) => {
  return get('/v1/items/search', { keyword })
}

export const getRecommendations = (limit = 8) => {
  return get('/v1/items/recommendations', { limit }, { timeout: 45000 })
}

/**
 * 获取所有分类
 * @returns {Promise}
 */
export const getCategories = () => {
  return get('/v1/items/categories')
}

/**
 * 获取所有外观类型
 * @returns {Promise}
 */
export const getExteriors = () => {
  return get('/v1/items/exteriors')
}

/**
 * 获取所有品质
 * @returns {Promise}
 */
export const getQualities = () => {
  return get('/v1/items/qualities')
}

/**
 * 从 Steam API 同步饰品数据（异步）
 * @returns {Promise}
 */
export const syncItemsFromSteam = () => {
  return post('/v1/items/sync-steam/async')
}

/**
 * 获取 Steam 饰品同步状态
 * @returns {Promise}
 */
export const getSteamSyncStatus = () => {
  return get('/v1/items/sync-steam/status')
}
