import request from '@/utils/request'

export const createBuyOrder = (data) => {
  return request.post('/v1/buy-orders', data)
}

export const getMyBuyOrders = () => {
  return request.get('/v1/buy-orders/my')
}

export const cancelBuyOrder = (id) => {
  return request.post(`/v1/buy-orders/${id}/cancel`)
}

export const getBuyOrdersByItemId = (itemId) => {
  return request.get(`/v1/buy-orders/item/${itemId}`)
}

export const getBuyOrderMarketList = (params = {}) => {
  return request.get('/v1/buy-orders/market', { params })
}

export const respondBuyOrder = (id, data) => {
  return request.post(`/v1/buy-orders/${id}/respond`, data)
}
