import { get, post, put, del } from '@/utils/request'

export function getPlayerShows() {
  return get('/v1/player-shows')
}

export function getPlayerShowById(id) {
  return get(`/v1/player-shows/${id}`)
}

export function getMyPlayerShows() {
  return get('/v1/player-shows/my')
}

export function createPlayerShow(data) {
  return post('/v1/player-shows', data)
}

export function updatePlayerShow(id, data) {
  return put(`/v1/player-shows/${id}`, data)
}

export function likePlayerShow(id) {
  return post(`/v1/player-shows/${id}/like`)
}

export function hasLikedPlayerShow(id) {
  return get(`/v1/player-shows/${id}/liked`)
}

export function deletePlayerShow(id) {
  return del(`/v1/player-shows/${id}`)
}

export function getPlayerShowComments(id) {
  return get(`/v1/player-shows/${id}/comments`)
}

export function addPlayerShowComment(id, content) {
  return post(`/v1/player-shows/${id}/comments`, { content })
}
