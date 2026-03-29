import request from '@/utils/request'

export const getSellOrdersByItemId = (itemId) => {
  return request.get(`/v1/sell-orders/item/${itemId}`)
}

export const createSellOrder = (data) => {
  return request.post('/v1/sell-orders', data)
}

export const getMySellOrders = () => {
  return request.get('/v1/sell-orders/my')
}

export const cancelSellOrder = (id) => {
  return request.post(`/v1/sell-orders/${id}/cancel`)
}

export const getMarketList = (params = {}) => {
  return request.get('/v1/sell-orders/market', { params })
}

export const getSellOrderDetail = (id) => {
  return request.get(`/v1/sell-orders/${id}`)
}
