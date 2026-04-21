import request, { get } from '@/utils/request'

/**
 * 库存相关API
 */

/**
 * 获取用户库存列表
 * @returns {Promise}
 */
export const getInventory = () => {
  return request.get('/v1/inventory', { timeout: 15000 })
}

/**
 * 同步Steam库存
 * @returns {Promise}
 */
export const syncInventory = () => {
  return request.post('/v1/inventory/sync', {}, { timeout: 45000 })
}

/**
 * 获取可交易的库存
 * @returns {Promise}
 */
export const getMarketableInventory = () => {
  return request.get('/v1/inventory/marketable', { timeout: 15000 })
}

export const getInventoryAnalysis = () => {
  return request.get('/v1/inventory/analysis', { timeout: 15000 })
}

/**
 * 获取用户库存（用于出售）
 * @returns {Promise}
 */
export const getUserInventory = () => {
  return request.get('/v1/inventory', { timeout: 15000 })
}
