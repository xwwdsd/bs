import request from '@/utils/request'

export function getMessages(type, page = 1, size = 20) {
  if (type === 'unread-count') {
    return request.get('/v1/messages/unread-count')
  }
  return request.get('/v1/messages', { 
    params: { type, page, size } 
  })
}

export function markAsRead(ids) {
  if (Array.isArray(ids)) {
    return request.post('/v1/messages/batch-read', { ids })
  }
  return request.post(`/v1/messages/${ids}/read`)
}

export function markAllRead(type) {
  return request.post('/v1/messages/read-all', { type })
}

export function deleteMessages(ids) {
  return request.post('/v1/messages/batch-delete', { ids })
}

export function getUnreadCount() {
  return request.get('/v1/messages/unread-count')
}
