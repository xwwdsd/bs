import request from '@/utils/request'

export function getFavorites(type) {
  return request({
    url: '/v1/favorites',
    method: 'get',
    params: { type }
  })
}

export function addFavorite(targetId, type) {
  return request({
    url: '/v1/favorites',
    method: 'post',
    data: { targetId, type }
  })
}

export function removeFavorite(targetId, type) {
  return request({
    url: `/v1/favorites/${type}/${targetId}`,
    method: 'delete'
  })
}

export function checkFavorite(targetId, type) {
  return request({
    url: `/v1/favorites/${type}/${targetId}/check`,
    method: 'get'
  })
}
